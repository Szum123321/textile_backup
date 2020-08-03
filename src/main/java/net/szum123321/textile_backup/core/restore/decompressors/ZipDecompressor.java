/*
    A simple backup mod for Fabric
    Copyright (C) 2020  Szum123321

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

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
    public static void decompress(FileInputStream fileInputStream, File target) {
        Instant start = Instant.now();

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream((bufferedInputStream))) {
            ZipArchiveEntry entry;

            while ((entry = zipInputStream.getNextZipEntry()) != null) {
                if(!zipInputStream.canReadEntryData(entry)){
                    TextileBackup.LOGGER.warn("Something when wrong while trying to decompress {}", entry.getName());
                    continue;
                }

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
                        TextileBackup.LOGGER.error("An exception occurred while trying to decompress file: " + file.getName(), e);
                    }
                }
            }
        } catch (IOException e) {
            TextileBackup.LOGGER.error("An exception occurred! ", e);
        }

        TextileBackup.LOGGER.info("Compression took: {} seconds.", Utilities.formatDuration(Duration.between(start, Instant.now())));
    }
}
