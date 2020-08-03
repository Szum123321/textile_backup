package net.szum123321.textile_backup.core.restore;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.BackupContext;
import net.szum123321.textile_backup.core.create.BackupHelper;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RestoreHelper {
    public static Runnable create(LocalDateTime backupTime, MinecraftServer server) {
        server.getPlayerManager().getPlayerList()
                .forEach(serverPlayerEntity -> serverPlayerEntity.sendMessage(new LiteralText("Warning! The server is going to shut down in few moments!"), false));

        File backupFile = Arrays.stream(Utilities.getBackupRootPath(Utilities.getLevelName(server))
                .listFiles())
                .filter(file -> Utilities.getFileCreationTime(file).isPresent())
                .filter(file -> Utilities.getFileCreationTime(file).get().equals(backupTime))
                .findFirst()
                .orElseThrow();

        TextileBackup.LOGGER.info("Restoring: {}", backupFile.getName());

        TextileBackup.globalShutdownBackupFlag.set(false);

        BackupHelper.create(
                new BackupContext.Builder()
                        .setServer(server)
                        .setInitiator(BackupContext.BackupInitiator.Restore)
                        .setComment("Old_World")
                        .setSave()
                        .build()
        ).run();

        TextileBackup.LOGGER.info("Shutting down server...");

        server.stop(false);

        return new RestoreBackupRunnable(server, backupFile);
    }

    public static List<RestoreableFile> getAvailableBackups(MinecraftServer server) {
        File root = Utilities.getBackupRootPath(Utilities.getLevelName(server));

        return Arrays.stream(root.listFiles())
                .filter(File::isFile)
                .filter(file -> Utilities.getFileExtension(file.getName()).isPresent())
                .filter(file -> Utilities.getFileCreationTime(file).isPresent())
                .map(RestoreableFile::new)
                .collect(Collectors.toList());
    }

    public static class RestoreableFile {
        private final LocalDateTime creationTime;
        private final String comment;

        protected RestoreableFile(File file) {
            String extension = Utilities.getFileExtension(file).get().getString();
            this.creationTime = Utilities.getFileCreationTime(file).get();

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
    }
}
