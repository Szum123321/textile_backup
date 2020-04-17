package net.szum123321.textile_backup.core.compressors;

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;

public class GenericTarCompressor {
	public static void createArchive(File in, File out, Class<? extends CompressorOutputStream> CompressorStreamClass, ServerCommandSource ctx) {
		Utilities.log("Starting compression...", ctx);

		try (FileOutputStream outStream = new FileOutputStream(out);
			 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outStream);
			 CompressorOutputStream compressorStream = CompressorStreamClass.getDeclaredConstructor(OutputStream.class).newInstance(bufferedOutputStream);// CompressorStreamClass.getConstructor().newInstance(bufferedOutputStream);
			 TarArchiveOutputStream arc = new TarArchiveOutputStream(compressorStream)) {

			arc.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			arc.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

			File input = in.getCanonicalFile();
			int rootPathLength = input.toString().length() + 1;

			Files.walk(input.toPath()).filter(
					path -> !path.equals(input.toPath()) &&
							path.toFile().isFile() &&
							!Utilities.isBlacklisted(input.toPath().relativize(path))
			).forEach(path -> {
				File file = path.toAbsolutePath().toFile();

				try (FileInputStream fin = new FileInputStream(file);
					 BufferedInputStream bfin = new BufferedInputStream(fin)){
					ArchiveEntry entry = arc.createArchiveEntry(file, file.getAbsolutePath().substring(rootPathLength));

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

		Utilities.log("Compression finished", ctx);
	}
}