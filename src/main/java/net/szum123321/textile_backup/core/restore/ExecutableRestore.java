package net.szum123321.textile_backup.core.restore;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.config.ConfigPOJO;
import net.szum123321.textile_backup.core.ActionInitiator;
import net.szum123321.textile_backup.core.CompressionStatus;
import net.szum123321.textile_backup.core.RestoreableFile;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.ExecutableBackup;
import net.szum123321.textile_backup.core.restore.decompressors.GenericTarDecompressor;
import net.szum123321.textile_backup.core.restore.decompressors.ZipDecompressor;
import net.szum123321.textile_backup.mixin.MinecraftServerSessionAccessor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.FutureTask;

public record ExecutableRestore(RestoreableFile restoreableFile,
                                MinecraftServer server,
                                @Nullable String comment,
                                ActionInitiator initiator,
                                ServerCommandSource commandSource) implements Runnable {

    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    private final static ConfigHelper config = ConfigHelper.INSTANCE;

    @Override
    public void run() {
        Globals.INSTANCE.globalShutdownBackupFlag.set(false);

        log.info("Shutting down server...");

        Path worldFile = Utilities.getWorldFolder(server), tmp;

        try {
            tmp = Files.createTempDirectory(
                    server.getRunDirectory().toPath(),
                    restoreableFile.getFile().getFileName().toString()
            );
        } catch (IOException e) {
            log.error("An exception occurred while unpacking backup", e);
            return;
        }

        FutureTask<Void> waitForShutdown = new FutureTask<>(() -> {
            server.stop(true);

            if(config.get().backupOldWorlds) {
                return ExecutableBackup.Builder
                        .newBackupContextBuilder()
                        .setServer(server)
                        .setInitiator(ActionInitiator.Restore)
                        .noCleanup()
                        .setComment("Old_World" + (comment != null ? "_" + comment : ""))
                        //.announce()
                        .build().call();
            }
            return null;
        });

        //run the thread.
        new Thread(waitForShutdown, "Server shutdown wait thread").start();

        try {
            log.info("Starting decompression...");

            long hash;

            if (restoreableFile.getArchiveFormat() == ConfigPOJO.ArchiveFormat.ZIP)
                hash = ZipDecompressor.decompress(restoreableFile.getFile(), tmp);
            else
                hash = GenericTarDecompressor.decompress(restoreableFile.getFile(), tmp);

            log.info("Waiting for server to fully terminate...");

            //locks until the backup is finished and the server is dead
            waitForShutdown.get();

            Optional<String> errorMsg;

            if(Files.notExists(CompressionStatus.resolveStatusFilename(tmp))) {
                errorMsg = Optional.of("Status file not found!");
            } else {
                CompressionStatus status = CompressionStatus.readFromFile(tmp);

                log.info("Status: {}", status);

                Files.delete(tmp.resolve(CompressionStatus.DATA_FILENAME));

                errorMsg = status.validate(hash, restoreableFile);
            }

            if(errorMsg.isEmpty() || !config.get().integrityVerificationMode.verify()) {
                if (errorMsg.isEmpty()) log.info("Backup valid. Restoring");
                else log.info("Backup is damaged, but verification is disabled [{}]. Restoring", errorMsg.get());

                //Disables write lock to override world file
                ((MinecraftServerSessionAccessor) server).getSession().close();

                Utilities.deleteDirectory(worldFile);
                Files.move(tmp, worldFile);

                if (config.get().deleteOldBackupAfterRestore) {
                    log.info("Deleting restored backup file");
                    Files.delete(restoreableFile.getFile());
                }
            } else {
                log.error(errorMsg.get());
            }

        } catch (Exception e) {
            log.error("An exception occurred while trying to restore a backup!", e);
        } finally {
            //Regardless of what happened, we should still clean up
            if(Files.exists(tmp)) {
                try {
                    Utilities.deleteDirectory(tmp);
                } catch (IOException ignored) {}
            }
        }

        //in case we're playing on client
        Globals.INSTANCE.globalShutdownBackupFlag.set(true);

        log.info("Done!");
    }

    public static final class Builder {
        private RestoreableFile file;
        private MinecraftServer server;
        private String comment;
        private ServerCommandSource serverCommandSource;

        private Builder() {
        }

        public static ExecutableRestore.Builder newRestoreContextBuilder() {
            return new ExecutableRestore.Builder();
        }

        public ExecutableRestore.Builder setFile(RestoreableFile file) {
            this.file = file;
            return this;
        }

        public ExecutableRestore.Builder setServer(MinecraftServer server) {
            this.server = server;
            return this;
        }

        public ExecutableRestore.Builder setComment(@Nullable String comment) {
            this.comment = comment;
            return this;
        }

        public ExecutableRestore.Builder setCommandSource(ServerCommandSource commandSource) {
            this.serverCommandSource = commandSource;
            return this;
        }

        public ExecutableRestore build() {
            if (server == null) server = serverCommandSource.getServer();

            ActionInitiator initiator = serverCommandSource.getEntity() instanceof PlayerEntity ? ActionInitiator.Player : ActionInitiator.ServerConsole;

            return new ExecutableRestore(file, server, comment, initiator, serverCommandSource);
        }
    }
}
