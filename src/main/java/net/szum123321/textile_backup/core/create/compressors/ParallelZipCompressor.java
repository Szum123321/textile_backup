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
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.BackupContext;
import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.compress.parallel.InputStreamSupplier;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;

/*
	This part of code is based on:
	https://stackoverflow.com/questions/54624695/how-to-implement-parallel-zip-creation-with-scatterzipoutputstream-with-zip64-su
	answer by:
	https://stackoverflow.com/users/2987755/dkb
*/
public class ParallelZipCompressor {
	public static void createArchive(File inputFile, File outputFile, BackupContext ctx, int coreLimit) {
		Statics.LOGGER.sendInfo(ctx, "Starting compression...");

		Instant start = Instant.now();

		Path rootPath = inputFile.toPath();

		try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
			 ZipArchiveOutputStream arc = new ZipArchiveOutputStream(bufferedOutputStream)) {

			ParallelScatterZipCreator scatterZipCreator = new ParallelScatterZipCreator(Executors.newFixedThreadPool(coreLimit));

			arc.setMethod(ZipArchiveOutputStream.DEFLATED);
			arc.setUseZip64(Zip64Mode.AsNeeded);
			arc.setLevel(Statics.CONFIG.compression);
			arc.setComment("Created on: " + Utilities.getDateTimeFormatter().format(LocalDateTime.now()));

			Files.walk(inputFile.toPath())
					.filter(path -> !Utilities.isBlacklisted(inputFile.toPath().relativize(path)))
					.map(Path::toFile)
					.filter(File::isFile)
					.forEach(file -> {
						try {  //IOException gets thrown only when arc is closed
							ZipArchiveEntry entry = (ZipArchiveEntry)arc.createArchiveEntry(file, rootPath.relativize(file.toPath()).toString());

							entry.setMethod(ZipEntry.DEFLATED);
							scatterZipCreator.addArchiveEntry(entry, new FileInputStreamSupplier(file));
						} catch (IOException e) {
							Statics.LOGGER.error("An exception occurred while trying to compress: {}", file.getName(), e);
							Statics.LOGGER.sendError(ctx, "Something went wrong while compressing files!");
						}
					});

			scatterZipCreator.writeTo(arc);
		} catch (IOException | InterruptedException | ExecutionException e) {
			Statics.LOGGER.error("An exception occurred!", e);
			Statics.LOGGER.sendError(ctx, "Something went wrong while compressing files!");
		}

		Statics.LOGGER.sendInfo(ctx, "Compression took: {} seconds.", Utilities.formatDuration(Duration.between(start, Instant.now())));
	}

	static class FileInputStreamSupplier implements InputStreamSupplier {
		private final File sourceFile;
		private InputStream stream;

		FileInputStreamSupplier(File sourceFile) {
			this.sourceFile = sourceFile;
		}

		public InputStream get() {
			try {
				stream = new BufferedInputStream(new FileInputStream(sourceFile));
			} catch (IOException e) {
				Statics.LOGGER.error("An exception occurred while trying to create input stream!", e);
			}

			return stream;
		}
	}
}
