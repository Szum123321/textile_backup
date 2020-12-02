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
import net.szum123321.textile_backup.core.create.BackupContext;
import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.compress.parallel.InputStreamSupplier;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

/*
	This part of code is based on:
	https://stackoverflow.com/questions/54624695/how-to-implement-parallel-zip-creation-with-scatterzipoutputstream-with-zip64-su
	answer by:
	https://stackoverflow.com/users/2987755/dkb
*/
public class ParallelZipCompressor extends ZipCompressor {
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
			CRC32 sum = new CRC32();
			sum.update(Files.readAllBytes(file.toPath()));
			entry.setCrc(sum.getValue());
		} else entry.setMethod(ZipEntry.DEFLATED);

		entry.setTime(System.currentTimeMillis());

		scatterZipCreator.addArchiveEntry(entry, new FileInputStreamSupplier(file));
	}

	@Override
	protected void finish(OutputStream arc) throws InterruptedException, ExecutionException, IOException {
		scatterZipCreator.writeTo((ZipArchiveOutputStream) arc);
	}

	static class FileInputStreamSupplier implements InputStreamSupplier {
		private final File sourceFile;

		FileInputStreamSupplier(File sourceFile) {
			this.sourceFile = sourceFile;
		}

		public InputStream get() {
			try {
				return new FileInputStream(sourceFile);
			} catch (IOException e) {
				Statics.LOGGER.error("An exception occurred while trying to create input stream from file: {}!", sourceFile.getName(), e);
			}

			return null;
		}
	}
}
