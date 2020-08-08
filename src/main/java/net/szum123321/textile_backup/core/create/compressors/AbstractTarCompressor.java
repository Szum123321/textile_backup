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

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.Utilities;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

public abstract class AbstractTarCompressor {
    protected abstract OutputStream openCompressorStream(OutputStream outputStream, int coreCountLimit) throws IOException;

    public void createArchive(File inputFile, File out, ServerCommandSource ctx, int coreLimit) {
        Statics.LOGGER.sendInfo(ctx, "Starting compression...");

        Instant start = Instant.now();

        try (FileOutputStream outStream = new FileOutputStream(out);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outStream);
             OutputStream compressorOutputStream = openCompressorStream(bufferedOutputStream, coreLimit);
             TarArchiveOutputStream arc = new TarArchiveOutputStream(compressorOutputStream)) {
            arc.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            arc.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

            Files.walk(inputFile.toPath())
                    .filter(path -> !path.equals(inputFile.toPath()))
                    .filter(path -> path.toFile().isFile())
                    .filter(path -> !Utilities.isBlacklisted(inputFile.toPath().relativize(path)))
                    .forEach(path -> {
                        File file = path.toAbsolutePath().toFile();

                        try (FileInputStream fileInputStream = new FileInputStream(file);
                             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
                            ArchiveEntry entry = arc.createArchiveEntry(file, inputFile.toPath().relativize(path).toString());

                            arc.putArchiveEntry(entry);
                            IOUtils.copy(bufferedInputStream, arc);

                            arc.closeArchiveEntry();
                        } catch (IOException e) {
                            Statics.LOGGER.error("An exception occurred while trying to compress: {}", path.getFileName(), e);
                            Statics.LOGGER.sendError(ctx, "Something went wrong while compressing files!");
                        }
                    });

            arc.finish();
        } catch (IOException e) {
            Statics.LOGGER.error("An exception occurred!", e);
            Statics.LOGGER.sendError(ctx, "Something went wrong while compressing files!");
        } finally {
            Statics.LOGGER.sendInfo(ctx, "Compression took: {} seconds.", Utilities.formatDuration(Duration.between(start, Instant.now())));
        }
    }
}
