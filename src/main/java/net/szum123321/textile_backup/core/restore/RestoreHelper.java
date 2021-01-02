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
import net.minecraft.text.MutableText;
import net.minecraft.util.Util;
import net.szum123321.textile_backup.ConfigHandler;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.ActionInitiator;
import net.szum123321.textile_backup.core.Utilities;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RestoreHelper {
    public static Optional<RestoreableFile> findFileAndLockIfPresent(LocalDateTime backupTime, MinecraftServer server) {
        File root = Utilities.getBackupRootPath(Utilities.getLevelName(server));

        Optional<RestoreableFile> optionalFile =  Arrays.stream(root.listFiles())
                .map(RestoreableFile::newInstance)
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .filter(rf -> rf.getCreationTime().equals(backupTime))
                .findFirst();

        optionalFile.ifPresent(file -> Statics.untouchableFile = file.getFile());

        return optionalFile;
    }

    public static AwaitThread create(RestoreContext ctx) {
        if(ctx.getInitiator() == ActionInitiator.Player)
            Statics.LOGGER.info("Backup restoration was initiated by: {}", ctx.getCommandSource().getName());
        else
            Statics.LOGGER.info("Backup restoration was initiated form Server Console");

        notifyPlayer(ctx);

        return new AwaitThread(
                Statics.CONFIG.restoreDelay,
                new RestoreBackupRunnable(ctx)
        );
    }

    private static void notifyPlayer(RestoreContext ctx) {
        MutableText message = Statics.LOGGER.getPrefixText().shallowCopy();
        message.append("Warning! The server is going to shut down in " + Statics.CONFIG.restoreDelay + " seconds!");

        ctx.getServer().getPlayerManager().broadcastChatMessage(
                message,
                MessageType.GAME_INFO,
                ctx.getInitiator() == ActionInitiator.Player ? ctx.getCommandSource().getEntity().getUuid() : Util.NIL_UUID
        );
    }

    public static List<RestoreableFile> getAvailableBackups(MinecraftServer server) {
        File root = Utilities.getBackupRootPath(Utilities.getLevelName(server));

        return Arrays.stream(root.listFiles())
                .filter(Utilities::isValidBackup)
                .map(RestoreableFile::newInstance)
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .collect(Collectors.toList());
    }

    public static class RestoreableFile implements Comparable<RestoreableFile> {
        private final File file;
        private final ConfigHandler.ArchiveFormat archiveFormat;
        private final LocalDateTime creationTime;
        private final String comment;

        private RestoreableFile(File file) throws NoSuchElementException {
            this.file = file;
            archiveFormat = Utilities.getArchiveExtension(file).orElseThrow(() -> new NoSuchElementException("Couldn't get file extension!"));
            String extension = archiveFormat.getCompleteString();
            creationTime = Utilities.getFileCreationTime(file).orElseThrow(() -> new NoSuchElementException("Couldn't get file creation time!"));

            final String filename = file.getName();

            if(filename.split("#").length > 1) {
                this.comment = filename.split("#")[1].split(extension)[0];
            } else {
                this.comment = null;
            }
        }

        public static Optional<RestoreableFile> newInstance(File file) {
            try {
                return Optional.of(new RestoreableFile(file));
            } catch (NoSuchElementException ignored) {}

            return Optional.empty();
        }

        public File getFile() {
            return file;
        }

        public ConfigHandler.ArchiveFormat getArchiveFormat() {
            return archiveFormat;
        }

        public LocalDateTime getCreationTime() {
            return creationTime;
        }

        public String getComment() {
            return comment;
        }

        @Override
        public int compareTo(@NotNull RestoreHelper.RestoreableFile o) {
            return creationTime.compareTo(o.creationTime);
        }

        public String toString() {
            return this.getCreationTime().format(Statics.defaultDateTimeFormatter) + (comment != null ? "#" + comment : "");
        }
    }
}