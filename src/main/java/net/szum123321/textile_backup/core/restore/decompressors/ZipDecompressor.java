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
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.core.Utilities;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

public class ZipDecompressor {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

    public static void decompress(File inputFile, File target) {
        Instant start = Instant.now();

        try(ZipFile zipFile = new ZipFile(inputFile)) {
            zipFile.getEntries().asIterator().forEachRemaining(entry -> {
                File file = target.toPath().resolve(entry.getName()).toFile();

                if(entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();

                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        log.error("Failed to create {}", parent);
                    } else {
                        try (OutputStream outputStream = Files.newOutputStream(file.toPath());
                             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
                            IOUtils.copy(zipFile.getInputStream(entry), bufferedOutputStream);
                        } catch (IOException e) {
                            log.error("An exception occurred while trying to decompress file: {}", entry.getName(), e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            log.error("An exception occurred! ", e);
        }

        log.info("Decompression took: {} seconds.", Utilities.formatDuration(Duration.between(start, Instant.now())));
    }
}
