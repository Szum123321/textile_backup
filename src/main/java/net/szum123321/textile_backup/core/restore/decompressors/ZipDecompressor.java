/*
 * A simple backup mod for Fabric
 * Copyright (C)  2022   Szum123321
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.szum123321.textile_backup.core.restore.decompressors;

import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.core.digest.FileTreeHashBuilder;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.digest.HashingOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;

public class ZipDecompressor {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

    public static long decompress(Path inputFile, Path target) throws IOException {
        Instant start = Instant.now();

        FileTreeHashBuilder hashBuilder = new FileTreeHashBuilder(0);

        try(ZipFile zipFile = new ZipFile(inputFile.toFile())) {
            for (Iterator<ZipArchiveEntry> it = zipFile.getEntries().asIterator(); it.hasNext(); ) {
                ZipArchiveEntry entry = it.next();
                Path file = target.resolve(entry.getName());

                if(entry.isDirectory()) {
                    Files.createDirectories(file);
                } else {
                    Files.createDirectories(file.getParent());
                    try (OutputStream outputStream = Files.newOutputStream(file);
                         HashingOutputStream out = new HashingOutputStream(outputStream, file, hashBuilder);
                         InputStream in = zipFile.getInputStream(entry)) {

                        IOUtils.copy(in, out);
                    }
                }
            }
        }

        log.info("Decompression took: {} seconds.", Utilities.formatDuration(Duration.between(start, Instant.now())));

        try {
            return hashBuilder.getValue(false);
        } catch (InterruptedException ignored) {
            return 0;
        }
    }
}
