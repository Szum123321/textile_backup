package net.szum123321.textile_backup.compressors;

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.TextileBackup;
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
		Utilities.info("Starting compression...", ctx);

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
					TextileBackup.LOGGER.error("An exception occurred while trying to compress: " + path.getFileName(), e);
					Utilities.sendError("Something went wrong while compressing files!", ctx);
				}
			});

			arc.finish();
		} catch (IOException e) {
			TextileBackup.LOGGER.error("An exception occurred!", e);
			Utilities.sendError("Something went wrong while compressing files!", ctx);
		}

		Utilities.info("Compression took: " + Utilities.formatDuration(Duration.between(start, Instant.now())) + " seconds.", ctx);
	}
}