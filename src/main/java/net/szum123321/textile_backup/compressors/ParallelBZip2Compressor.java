package net.szum123321.textile_backup.compressors;

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.at4j.comp.bzip2.BZip2OutputStream;
import org.at4j.comp.bzip2.BZip2OutputStreamSettings;

import java.io.*;
import java.nio.file.Files;

public class ParallelBZip2Compressor {
	public static void createArchive(File in, File out, ServerCommandSource ctx, int coreLimit) {
		Utilities.info("Starting compression...", ctx);

		BZip2OutputStreamSettings settings = new BZip2OutputStreamSettings().setNumberOfEncoderThreads(coreLimit);

		long start = System.nanoTime();

		try (FileOutputStream fileOutputStream = new FileOutputStream(out);
			 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
			 BZip2OutputStream bZip2OutputStream = new BZip2OutputStream(bufferedOutputStream, settings);
			 TarArchiveOutputStream arc = new TarArchiveOutputStream(bZip2OutputStream)) {

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

		long end = System.nanoTime();

		Utilities.info("Compression took: " + ((end - start) / 1000000000.0) + "s", ctx);
	}
}