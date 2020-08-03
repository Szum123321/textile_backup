package net.szum123321.textile_backup.core.restore.decompressors;

import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

public class GenericTarDecompressor {
    public static void decompress(File archiveFile, File target, Class<? extends CompressorInputStream> DecompressorStream) {
        Instant start = Instant.now();

        try (FileInputStream inputStream = new FileInputStream(archiveFile);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
             CompressorInputStream compressorInputStream = DecompressorStream.getDeclaredConstructor(InputStream.class).newInstance(bufferedInputStream);
             TarArchiveInputStream archiveInputStream = new TarArchiveInputStream(compressorInputStream)) {
            TarArchiveEntry entry;

            while ((entry = archiveInputStream.getNextTarEntry()) != null) {
                if(!archiveInputStream.canReadEntryData(entry))
                    continue;

                File file = target.toPath().resolve(entry.getName()).toFile();

                if(entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();

                    if (!parent.isDirectory() && !parent.mkdirs())
                        throw new IOException("Failed to create directory " + parent);

                    try (OutputStream outputStream = Files.newOutputStream(file.toPath());
                         BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
                        IOUtils.copy(archiveInputStream, bufferedOutputStream);
                    } catch (IOException e) {
                        TextileBackup.LOGGER.error("An exception occurred while trying to compress file: " + file.getName(), e);
                    }
                }
            }
        } catch (IOException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            TextileBackup.LOGGER.error("An exception occurred! ", e);
        }

        TextileBackup.LOGGER.info("Decompression took {} seconds.", Utilities.formatDuration(Duration.between(start, Instant.now())));
    }
}