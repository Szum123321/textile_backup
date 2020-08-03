package net.szum123321.textile_backup.core.restore.decompressors;

import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

public class ZipDecompressor {
    public static void decompress(File archiveFile, File target) {
        Instant start = Instant.now();

        try (FileInputStream inputStream = new FileInputStream(archiveFile);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
             ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream((bufferedInputStream))) {
            ZipArchiveEntry entry;

            while ((entry = zipInputStream.getNextZipEntry()) != null) {
                if(!zipInputStream.canReadEntryData(entry))
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
                        IOUtils.copy(zipInputStream, bufferedOutputStream);
                    } catch (IOException e) {
                        TextileBackup.LOGGER.error("An exception occurred while trying to compress file: " + file.getName(), e);
                    }
                }
            }
        } catch (IOException e) {
            TextileBackup.LOGGER.error("An exception occurred! ", e);
        }

        TextileBackup.LOGGER.info("Compression took: {} seconds.", Utilities.formatDuration(Duration.between(start, Instant.now())));
    }
}
