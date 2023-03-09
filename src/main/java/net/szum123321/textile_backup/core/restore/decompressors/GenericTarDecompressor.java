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
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public class GenericTarDecompressor {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

    public static long decompress(Path input, Path target) throws IOException {
        Instant start = Instant.now();
        FileTreeHashBuilder treeBuilder = new FileTreeHashBuilder(0);

        try (InputStream fileInputStream = Files.newInputStream(input);
             InputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             InputStream compressorInputStream = getCompressorInputStream(bufferedInputStream);
             TarArchiveInputStream archiveInputStream = new TarArchiveInputStream(compressorInputStream)) {
            TarArchiveEntry entry;

            while ((entry = archiveInputStream.getNextTarEntry()) != null) {
                if(!archiveInputStream.canReadEntryData(entry))
                    throw new IOException("Couldn't read archive entry! " + entry.getName());

                Path file = target.resolve(entry.getName());

                if(entry.isDirectory()) {
                    Files.createDirectories(file);
                } else {
                    Files.createDirectories(file.getParent());
                    try (OutputStream outputStream = Files.newOutputStream(file);
                         HashingOutputStream out = new HashingOutputStream(outputStream, file, treeBuilder)) {
                        IOUtils.copy(archiveInputStream, out);
                    }
                }
            }
        } catch (CompressorException e) {
            throw new IOException(e);
        }

        log.info("Decompression took {} seconds.", Utilities.formatDuration(Duration.between(start, Instant.now())));

        try {
            return treeBuilder.getValue(false);
        } catch (InterruptedException ignored) {
            return 0;
        }
    }

    private static InputStream getCompressorInputStream(InputStream inputStream) throws CompressorException {
        try {
            return new CompressorStreamFactory().createCompressorInputStream(inputStream);
        } catch (CompressorException e) {
            final byte[] tarHeader = new byte[512];
            int signatureLength;

            inputStream.mark(tarHeader.length);

            try {
                signatureLength = IOUtils.readFully(inputStream, tarHeader);
                inputStream.reset();
            } catch (IOException e1) {
                throw new CompressorException("IOException while reading tar signature", e1);
            }

            if(TarArchiveInputStream.matches(tarHeader, signatureLength)) return inputStream;

            throw e;
        }
    }
}