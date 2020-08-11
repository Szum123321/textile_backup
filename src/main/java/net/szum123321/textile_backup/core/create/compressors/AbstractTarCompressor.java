/*
 * A simple backup mod for Fabric
 * Copyright (C) 2020  Szum123321
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

package net.szum123321.textile_backup.core.create.compressors;

import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.BackupContext;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public abstract class AbstractTarCompressor {
    protected abstract OutputStream openCompressorStream(OutputStream outputStream, int coreCountLimit) throws IOException;

    public void createArchive(File inputFile, File outputFile, BackupContext ctx, int coreLimit) {
        Statics.LOGGER.sendInfo(ctx, "Starting compression...");

        Instant start = Instant.now();

        try (FileOutputStream outStream = new FileOutputStream(outputFile);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outStream);
             OutputStream compressorOutputStream = openCompressorStream(bufferedOutputStream, coreLimit);
             TarArchiveOutputStream arc = new TarArchiveOutputStream(compressorOutputStream)) {
            arc.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            arc.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

            Files.walk(inputFile.toPath())
                    .filter(path -> !Utilities.isBlacklisted(inputFile.toPath().relativize(path)))
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .forEach(file -> {
                        try (FileInputStream fileInputStream = new FileInputStream(file)){
                            ArchiveEntry entry = arc.createArchiveEntry(file, inputFile.toPath().relativize(file.toPath()).toString());
                            arc.putArchiveEntry(entry);

                            IOUtils.copy(fileInputStream, arc);

                            arc.closeArchiveEntry();
                        } catch (IOException e) {
                            Statics.LOGGER.error("An exception occurred while trying to compress: {}", file.getName(), e);
                            Statics.LOGGER.sendError(ctx, "Something went wrong while compressing files!");
                        }
                    });
        } catch (IOException e) {
            Statics.LOGGER.error("An exception occurred!", e);
            Statics.LOGGER.sendError(ctx, "Something went wrong while compressing files!");
        }

        Statics.LOGGER.sendInfo(ctx, "Compression took: {} seconds.", Utilities.formatDuration(Duration.between(start, Instant.now())));
    }
}
