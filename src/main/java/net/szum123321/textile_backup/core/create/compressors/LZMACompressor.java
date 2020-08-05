package net.szum123321.textile_backup.core.create.compressors;

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.Utilities;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;


import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

public class LZMACompressor {
	public static void createArchive(File in, File out, ServerCommandSource ctx) {
		Statics.LOGGER.sendInfo(ctx, "Starting compression...");

		Instant start = Instant.now();

		try (FileOutputStream outStream = new FileOutputStream(out);
			 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outStream);
			 XZCompressorOutputStream compressorStream = new XZCompressorOutputStream(bufferedOutputStream);// CompressorStreamClass.getConstructor().newInstance(bufferedOutputStream);
			 TarArchiveOutputStream arc = new TarArchiveOutputStream(compressorStream)) {

			arc.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			arc.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

			File input = in.getCanonicalFile();

			Files.walk(input.toPath()
			).filter(path -> !path.equals(input.toPath()) &&
					path.toFile().isFile() &&
					!Utilities.isBlacklisted(input.toPath().relativize(path))
			).forEach(path -> {
				File file = path.toAbsolutePath().toFile();

				try (FileInputStream fin = new FileInputStream(file);
					 BufferedInputStream bfin = new BufferedInputStream(fin)) {
					ArchiveEntry entry = arc.createArchiveEntry(file, input.toPath().relativize(path).toString());

					arc.putArchiveEntry(entry);
					IOUtils.copy(bfin, arc);

					arc.closeArchiveEntry();
				} catch (IOException e) {
					Statics.LOGGER.error("An exception occurred while trying to compress: {}", path.getFileName(), e);
					Statics.LOGGER.sendError(ctx, "Something went wrong while compressing files!");
				}
			});

			arc.finish();
		} catch (IOException e) {
			Statics.LOGGER.error("An exception occurred!", e);
			Statics.LOGGER.sendError(ctx, "Something went wrong while compressing files!");
		}

		Statics.LOGGER.sendInfo(ctx, "Compression took: {} seconds.", Utilities.formatDuration(Duration.between(start, Instant.now())));
	}
}