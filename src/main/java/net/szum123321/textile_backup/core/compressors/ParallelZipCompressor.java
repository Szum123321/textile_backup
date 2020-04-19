package net.szum123321.textile_backup.core.compressors;

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;
import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.compress.parallel.InputStreamSupplier;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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
	public static void createArchive(File in, File out, ServerCommandSource ctx) {
		Utilities.log("Starting compression...", ctx);

		long start = System.nanoTime();

		try (FileOutputStream fileOutputStream = new FileOutputStream(out);
			 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
			 ZipArchiveOutputStream arc = new ZipArchiveOutputStream(bufferedOutputStream)) {

			ParallelScatterZipCreator scatterZipCreator = new ParallelScatterZipCreator();

			arc.setMethod(ZipArchiveOutputStream.DEFLATED);
			arc.setUseZip64(Zip64Mode.AsNeeded);
			arc.setLevel(TextileBackup.config.compression);
			arc.setComment("Created on: " + Utilities.getDateTimeFormatter().format(LocalDateTime.now()));

			File input = in.getCanonicalFile();

			Files.walk(input.toPath()
			).filter(path -> !path.equals(input.toPath()) &&
					path.toFile().isFile() &&
					!Utilities.isBlacklisted(input.toPath().relativize(path))
			).forEach(p -> {
				ZipArchiveEntry entry = new ZipArchiveEntry(input.toPath().relativize(p).toString());
				entry.setMethod(ZipEntry.DEFLATED);
				FileInputStreamSupplier supplier = new FileInputStreamSupplier(p);
				scatterZipCreator.addArchiveEntry(entry, supplier);
			});

			scatterZipCreator.writeTo(arc);

			arc.finish();
		} catch (IOException | InterruptedException | ExecutionException e) {
			TextileBackup.logger.error(e.getMessage());
		}

		long end = System.nanoTime();

		Utilities.log("Compression took: " + ((end - start) / 1000000000.0) + "s", ctx);
	}

	static class FileInputStreamSupplier implements InputStreamSupplier {
		private final Path sourceFile;
		private InputStream stream;

		FileInputStreamSupplier(Path sourceFile) {
			this.sourceFile = sourceFile;
		}

		public InputStream get() {
			try {
				stream = Files.newInputStream(sourceFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return stream;
		}
	}
}
