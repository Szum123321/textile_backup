/*
 * A simple backup mod for Fabric
 * Copyright (C)  2022   Szum123321
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

import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.core.NoSpaceLeftOnDeviceException;
import net.szum123321.textile_backup.core.create.ExecutableBackup;
import net.szum123321.textile_backup.core.create.InputSupplier;
import org.apache.commons.compress.archivers.zip.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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
	private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

	//These fields are used to discriminate against the issue #51
	private final static SimpleStackTraceElement[] STACKTRACE_NO_SPACE_ON_LEFT_ON_DEVICE = {
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
	protected OutputStream createArchiveOutputStream(OutputStream stream, ExecutableBackup ctx, int coreLimit) {
		scatterZipCreator = new ParallelScatterZipCreator(Executors.newFixedThreadPool(coreLimit));
		return super.createArchiveOutputStream(stream, ctx, coreLimit);
	}

	@Override
	protected void addEntry(InputSupplier input, OutputStream arc) throws IOException {
		ZipArchiveEntry entry;
		if(input.getPath().isEmpty()) {
			entry = new ZipArchiveEntry(input.getName());
			entry.setMethod(ZipEntry.STORED);
			entry.setSize(input.size());
		} else {
			Path file = input.getPath().get();
			entry = (ZipArchiveEntry) ((ZipArchiveOutputStream) arc).createArchiveEntry(file, input.getName());
			if (ZipCompressor.isDotDat(file.toString())) {
				entry.setMethod(ZipEntry.STORED);
				entry.setSize(Files.size(file));
				entry.setCompressedSize(Files.size(file));
				entry.setCrc(getCRC(file));
			} else entry.setMethod(ZipEntry.DEFLATED);
		}

		entry.setTime(System.currentTimeMillis());

		scatterZipCreator.addArchiveEntry(entry, input);
	}

	@Override
	protected void finish(OutputStream arc) throws InterruptedException, IOException, ExecutionException {
		/*
			This is perhaps the most dreadful line of this whole mess
			This line causes the infamous Out of space error (#20 and #80)
		*/
		try {
			scatterZipCreator.writeTo((ZipArchiveOutputStream) arc);
		} catch (ExecutionException e) {
			Throwable cause;
			if((cause = e.getCause()).getClass().equals(IOException.class)) {
				//The out of space exception is thrown at sun.nio.ch.FileDispatcherImpl.write0(Native Method)
				boolean match = (cause.getStackTrace().length >= STACKTRACE_NO_SPACE_ON_LEFT_ON_DEVICE.length);
				if(match) {
					for(int i = 0; i < STACKTRACE_NO_SPACE_ON_LEFT_ON_DEVICE.length && match; i++)
						if(!STACKTRACE_NO_SPACE_ON_LEFT_ON_DEVICE[i].matches(cause.getStackTrace()[i])) match = false;

					//For clarity's sake let's not throw the ExecutionException itself rather only the cause, as the EE is just the wrapper
					if(match) throw new NoSpaceLeftOnDeviceException(cause);
				}
			}

			throw e;
		}
	}

	private record SimpleStackTraceElement (
			String className,
			String methodName,
			boolean isNative
	) {
		public boolean matches(StackTraceElement o) {
			return (isNative == o.isNativeMethod()) && Objects.equals(className, o.getClassName()) && Objects.equals(methodName, o.getMethodName());
		}
	}
}
