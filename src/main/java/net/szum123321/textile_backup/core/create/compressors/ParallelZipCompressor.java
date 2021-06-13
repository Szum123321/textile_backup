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
import net.szum123321.textile_backup.core.create.compressors.ParallelZipCompressor.FileInputStreamSupplier;
import net.szum123321.textile_backup.core.create.compressors.parallel_zip_fix.FailsafeScatterGatherBackingStore;
import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.compress.parallel.ScatterGatherBackingStore;
import org.apache.commons.compress.parallel.ScatterGatherBackingStoreSupplier;
import sun.security.action.GetPropertyAction;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;

/*
	This part of code is based on:
	https://stackoverflow.com/questions/54624695/how-to-implement-parallel-zip-creation-with-scatterzipoutputstream-with-zip64-su
	answer by:
	https://stackoverflow.com/users/2987755/dkb
*/
public class ParallelZipCompressor extends ZipCompressor {
	private ParallelScatterZipCreator scatterZipCreator;
	private ScatterZipOutputStream dirs;

	public static ParallelZipCompressor getInstance() {
		return new ParallelZipCompressor();
	}

	@Override
	protected OutputStream createArchiveOutputStream(OutputStream stream, BackupContext ctx, int coreLimit) throws IOException {
		dirs = ScatterZipOutputStream.fileBased(File.createTempFile("scatter-dirs", "tmp"));
		scatterZipCreator = new ParallelScatterZipCreator(Executors.newFixedThreadPool(coreLimit), new CatchingBackingStoreSupplier());

		return super.createArchiveOutputStream(stream, ctx, coreLimit);
	}

	@Override
	protected void addEntry(File file, String entryName, OutputStream arc) throws IOException {
		ZipArchiveEntry entry = (ZipArchiveEntry)((ZipArchiveOutputStream)arc).createArchiveEntry(file, entryName);

		if(entry.isDirectory() && !entry.isUnixSymlink()) {
			dirs.addArchiveEntry(
					ZipArchiveEntryRequest.createZipArchiveEntryRequest(entry, new FileInputStreamSupplier(file))
			);
		} else {
			if (ZipCompressor.isDotDat(file.getName())) {
				entry.setMethod(ZipArchiveOutputStream.STORED);
				entry.setCompressedSize(entry.getSize());
				entry.setCrc(getCRC(file));
			} else entry.setMethod(ZipEntry.DEFLATED);

			scatterZipCreator.addArchiveEntry(entry, new FileInputStreamSupplier(file));
		}
	}

	@Override
	protected void finish(OutputStream arc) throws InterruptedException, ExecutionException, IOException {
		dirs.writeTo((ZipArchiveOutputStream) arc);
		dirs.close();
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
				Statics.LOGGER.error("An exception occurred while trying to create an input stream from file: {}!", sourceFile.getName(), e);
			}

			return null;
		}
	}

	private static class CatchingBackingStoreSupplier implements ScatterGatherBackingStoreSupplier {
		final AtomicInteger storeNum = new AtomicInteger(0);

		@Override
		public ScatterGatherBackingStore get() throws IOException {
			//final File tempFile = File.createTempFile("catchngparallelscatter", "n" + storeNum.incrementAndGet());
			return new FailsafeScatterGatherBackingStore(storeNum.incrementAndGet(), Paths.get(GetPropertyAction.privilegedGetProperty("java.io.tmpdir")));
		}
	}
}
