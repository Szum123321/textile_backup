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

package net.szum123321.textile_backup.core.restore;

import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.Utilities;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class RestoreHelper {
    public static Optional<File> findFileAndLockIfPresent(LocalDateTime backupTime, MinecraftServer server) {
        File root = Utilities.getBackupRootPath(Utilities.getLevelName(server));

        Optional<File> optionalFile =  Arrays.stream(root.listFiles())
                .filter(Utilities::isValidBackup)
                .filter(file -> Utilities.getFileCreationTime(file).get().equals(backupTime))
                .findFirst();

        optionalFile.ifPresent(file -> Statics.untouchableFile = file);

        return optionalFile;
    }

    public static AwaitThread create(File backupFile, MinecraftServer server, String comment) {
        MutableText msg = new LiteralText("Warning! The server is going to shut down in " + Statics.CONFIG.restoreDelay + " seconds!");
        msg.formatted(Formatting.WHITE);
        msg = Statics.LOGGER.getPrefixText().append(msg);

        server.getPlayerManager().broadcastChatMessage(msg, MessageType.SYSTEM, Util.NIL_UUID);

        Statics.globalShutdownBackupFlag.set(false);

        return new AwaitThread(
                Statics.CONFIG.restoreDelay,
                new RestoreBackupRunnable(server, backupFile, comment)
        );
    }

    public static List<RestoreableFile> getAvailableBackups(MinecraftServer server) {
        File root = Utilities.getBackupRootPath(Utilities.getLevelName(server));

        return Arrays.stream(root.listFiles())
                .filter(Utilities::isValidBackup)
                .map(RestoreableFile::new)
                .collect(Collectors.toList());
    }

    public static class RestoreableFile {
        private final LocalDateTime creationTime;
        private final String comment;

        protected RestoreableFile(File file) {
            String extension = Utilities.getFileExtension(file).orElseThrow(() -> new NoSuchElementException("Couldn't get file extention")).getString();
            this.creationTime = Utilities.getFileCreationTime(file).orElseThrow(() -> new NoSuchElementException("Couldn't get file creation time."));

            final String filename = file.getName();

            if(filename.split("#").length > 1) {
                this.comment = filename.split("#")[1].split(extension)[0];
            } else {
                this.comment = null;
            }
        }

        public LocalDateTime getCreationTime() {
            return creationTime;
        }

        public String getComment() {
            return comment;
        }

        public String toString() {
            return this.getCreationTime().format(Statics.defaultDateTimeFormatter) + (comment != null ? "#" + comment : "");
        }
    }
}
