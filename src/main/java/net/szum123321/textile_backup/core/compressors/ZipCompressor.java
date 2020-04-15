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

package net.szum123321.textile_backup.core.compressors;

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;

public class ZipCompressor {
    public static void createArchive(File in, File out, ServerCommandSource ctx){
        Utilities.log("Starting compression...", ctx);

        try (FileOutputStream fileOutputStream = new FileOutputStream(out);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
             ZipArchiveOutputStream arc = new ZipArchiveOutputStream(bufferedOutputStream)){

            arc.setMethod(ZipArchiveOutputStream.DEFLATED);
            arc.setLevel(TextileBackup.config.compression);
            arc.setComment("Created on: " + Utilities.getDateTimeFormatter().format(LocalDateTime.now()));

            File input = in.getCanonicalFile();
            int rootPathLength = input.toString().length() + 1;

            Files.walk(input.toPath()).filter(path -> !path.equals(input.toPath()) && path.toFile().isFile() && !TextileBackup.config.fileBlacklist.contains(path.toString().substring(rootPathLength))).forEach(path -> {
                File file = path.toAbsolutePath().toFile();
                try (FileInputStream fstream = new FileInputStream(file)) {
                    ZipArchiveEntry entry = new ZipArchiveEntry(file, file.getAbsolutePath().substring(rootPathLength));
                    arc.putArchiveEntry(entry);

                    IOUtils.copy(fstream, arc);

                    arc.closeArchiveEntry();
                }catch (IOException e){
                    TextileBackup.logger.error(e.getMessage());
                }
            });

            arc.finish();
        } catch (IOException e) {
            TextileBackup.logger.error(e.getMessage());
        }

        Utilities.log("Compression finished", ctx);
    }
}
