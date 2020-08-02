package net.szum123321.textile_backup.compressors;

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;
import org.anarres.parallelgzip.ParallelGZIPOutputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

public class ParallelGzipCompressor {
	public static void createArchive(File in, File out, ServerCommandSource ctx, int coreLimit) {
		Utilities.info("Starting compression...", ctx);

		Instant start = Instant.now();

		try (FileOutputStream outStream = new FileOutputStream(out);
			 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outStream);
			 ParallelGZIPOutputStream gzipOutputStream = new ParallelGZIPOutputStream(bufferedOutputStream, coreLimit);
			 TarArchiveOutputStream arc = new TarArchiveOutputStream(gzipOutputStream)) {

			arc.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			arc.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

			File input = in.getCanonicalFile();

			Files.walk(input.toPath())
					.filter(path -> !path.equals(input.toPath()))
					.filter(path -> path.toFile().isFile())
					.filter(path -> !Utilities.isBlacklisted(input.toPath().relativize(path)))
					.forEach(path -> {
						File file = path.toAbsolutePath().toFile();

						try (FileInputStream fin = new FileInputStream(file);
							 BufferedInputStream bfin = new BufferedInputStream(fin)) {
							ArchiveEntry entry = arc.createArchiveEntry(file, input.toPath().relativize(path).toString());

							arc.putArchiveEntry(entry);
							IOUtils.copy(bfin, arc);

							arc.closeArchiveEntry();
						} catch (IOException e) {
							TextileBackup.LOGGER.error("An exception occurred while trying to compress file: " + path, e);
							Utilities.sendError("Something went wrong while compressing files!", ctx);
						}
					});

			arc.finish();
		} catch (IOException e) {
			TextileBackup.LOGGER.error("An exception happened!", e);
			Utilities.sendError("Something went wrong while compressing files!", ctx);
		}

		Utilities.info("Compression took: " + Utilities.formatDuration(Duration.between(start, Instant.now())) + " seconds.", ctx);
	}
}
