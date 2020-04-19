package net.szum123321.textile_backup.core.compressors;

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;


import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;

public class GenericTarCompressor {
	public static void createArchive(File in, File out, Class<? extends OutputStream> CompressorStreamClass, ServerCommandSource ctx) {
		Utilities.log("Starting compression...", ctx);

		long start = System.nanoTime();

		try (FileOutputStream outStream = new FileOutputStream(out);
			 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outStream);
			 OutputStream compressorStream = CompressorStreamClass.getDeclaredConstructor(OutputStream.class).newInstance(bufferedOutputStream);// CompressorStreamClass.getConstructor().newInstance(bufferedOutputStream);
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
					TextileBackup.logger.error(e.getMessage());
				}
			});

			arc.finish();
		} catch (IOException | IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
			TextileBackup.logger.error(e.toString());
		}

		long end = System.nanoTime();

		Utilities.log("Compression took: " + ((end - start) / 1000000000.0) + "s", ctx);
	}
}