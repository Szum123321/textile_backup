/*
 * A simple backup mod for Fabric
 * Copyright (C) 2020  Szum123321
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.szum123321.textile_backup.core.create.compressors;

import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.NoSpaceLeftOnDeviceException;
import net.szum123321.textile_backup.core.create.BackupContext;
import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.compress.parallel.InputStreamSupplier;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;

/*
	This part of code is based on:
	https://stackoverflow.com/questions/54624695/how-to-implement-parallel-zip-creation-with-scatterzipoutputstream-with-zip64-su
	answer by:
	https://stackoverflow.com/users/2987755/dkb
*/
public class ParallelZipCompressor extends ZipCompressor {
	//These fields are used to discriminate against the issue #51
	private final static SimpleStackTraceElement[] STACKTRACE = {
			new SimpleStackTraceElement("sun.nio.ch.FileDispatcherImpl", "write0", true),
			new SimpleStackTraceElement("sun.nio.ch.FileDispatcherImpl", "write", false),
			new SimpleStackTraceElement("sun.nio.ch.IOUtil", "writeFromNativeBuffer", false),
			new SimpleStackTraceElement("sun.nio.ch.IOUtil", "write", false),
			new SimpleStackTraceElement("sun.nio.ch.FileChannelImpl", "write", false),
			new SimpleStackTraceElement("java.nio.channels.Channels", "writeFullyImpl", false),
			new SimpleStackTraceElement("java.nio.channels.Channels", "writeFully", false),
			new SimpleStackTraceElement("java.nio.channels.Channels$1", "write", false),
			new SimpleStackTraceElement("org.apache.commons.compress.parallel.FileBasedScatterGatherBackingStore", "writeOut", false)
	};

	private ParallelScatterZipCreator scatterZipCreator;

	public static ParallelZipCompressor getInstance() {
		return new ParallelZipCompressor();
	}

	@Override
	protected OutputStream createArchiveOutputStream(OutputStream stream, BackupContext ctx, int coreLimit) {
		scatterZipCreator = new ParallelScatterZipCreator(Executors.newFixedThreadPool(coreLimit));
		return super.createArchiveOutputStream(stream, ctx, coreLimit);
	}

	@Override
	protected void addEntry(File file, String entryName, OutputStream arc) throws IOException {
		ZipArchiveEntry entry = (ZipArchiveEntry)((ZipArchiveOutputStream)arc).createArchiveEntry(file, entryName);

		if(ZipCompressor.isDotDat(file.getName())) {
			entry.setMethod(ZipArchiveOutputStream.STORED);
			entry.setSize(file.length());
			entry.setCompressedSize(file.length());
			entry.setCrc(getCRC(file));
		} else entry.setMethod(ZipEntry.DEFLATED);

		entry.setTime(System.currentTimeMillis());

		scatterZipCreator.addArchiveEntry(entry, new FileInputStreamSupplier(file));
	}

	@Override
	protected void finish(OutputStream arc) throws InterruptedException, IOException, ExecutionException {
		/*
			This is perhaps the most dreadful line of this whole mess
			This line causes the infamous Out of space error
		*/
		try {
			scatterZipCreator.writeTo((ZipArchiveOutputStream) arc);
		} catch (ExecutionException e) {
			Throwable cause;
			if((cause = e.getCause()).getClass().equals(IOException.class)) {
				//The out of space exception is thrown at sun.nio.ch.FileDispatcherImpl.write0(Native Method)
				boolean match = (cause.getStackTrace().length >= STACKTRACE.length);
				if(match) {
					for(int i = 0; i < STACKTRACE.length && match; i++)
						if(!STACKTRACE[i].equals(cause.getStackTrace()[i])) {
							//Statics.LOGGER.error("Mismatch at: {}, classname: {}, methodname: {}, {}", i, cause.getStackTrace()[i].getClassName(), cause.getStackTrace()[i].getMethodName());
							match = false;
						}

					//For clarity sake let's not throw the ExecutionException itself rather only the cause, as the EE is just the wrapper
					if(match) throw new NoSpaceLeftOnDeviceException(cause);
				}
			}

			throw e;
		}
	}

	private static record SimpleStackTraceElement (
			String className,
			String methodName,
			boolean isNative
	) {
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null) return false;
			if(o.getClass() == StackTraceElement.class) {
				StackTraceElement that = (StackTraceElement) o;
				return (isNative == that.isNativeMethod()) && Objects.equals(className, that.getClassName()) && Objects.equals(methodName, that.getMethodName());
			}
			if(getClass() != o.getClass()) return false;
			SimpleStackTraceElement that = (SimpleStackTraceElement) o;
			return isNative == that.isNative && Objects.equals(className, that.className) && Objects.equals(methodName, that.methodName);
		}
	}

	record FileInputStreamSupplier(File sourceFile) implements InputStreamSupplier {
		public InputStream get() {
			try {
				return new FileInputStream(sourceFile);
			} catch (IOException e) {
				Statics.LOGGER.error("An exception occurred while trying to create an input stream from file: {}!", sourceFile.getName(), e);
			}

			return null;
		}
	}
}
