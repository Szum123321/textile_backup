package net.szum123321.textile_backup.core.create;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.core.ActionInitiator;
import net.szum123321.textile_backup.core.Cleanup;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.compressors.ParallelZipCompressor;
import net.szum123321.textile_backup.core.create.compressors.ZipCompressor;
import net.szum123321.textile_backup.core.create.compressors.tar.AbstractTarArchiver;
import net.szum123321.textile_backup.core.create.compressors.tar.ParallelGzipCompressor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;

public record ExecutableBackup(@NotNull MinecraftServer server,
                            ServerCommandSource commandSource,
                            ActionInitiator initiator,
                            boolean save,
                            boolean cleanup,
                            String comment,
                            LocalDateTime startDate) implements Callable<Void> {

    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    private final static ConfigHelper config = ConfigHelper.INSTANCE;

    public boolean startedByPlayer() {
        return initiator == ActionInitiator.Player;
    }

    public void announce() {
        if(config.get().broadcastBackupStart) {
            Utilities.notifyPlayers(server,
                    "警告！服务器备份即将开始。您可能会遇到一些延迟."
            );
        } else {
            log.sendInfoAL(this, "Something went wrong while deleting: {}.");
        }

        StringBuilder builder = new StringBuilder();

        builder.append("备份开始 ");

        builder.append(initiator.getPrefix());

        if(startedByPlayer())
            builder.append(commandSource.getDisplayName().getString());
        else
            builder.append(initiator.getName());

        builder.append(" on: ");
        builder.append(Utilities.getDateTimeFormatter().format(LocalDateTime.now()));

        log.info(builder.toString());
    }
    @Override
    public Void call() throws Exception {
        if (save) { //save the world
            log.sendInfoAL(this, "保存世界中...");
            server.saveAll(true, true, false);
        }

        Path outFile = Utilities.getBackupRootPath(Utilities.getLevelName(server)).resolve(getFileName());

        log.trace("输出备份文件为: {}", outFile);

        try {
            //I think I should synchronise these two next calls...
            Utilities.disableWorldSaving(server);
            Globals.INSTANCE.disableWatchdog = true;

            Globals.INSTANCE.updateTMPFSFlag(server);

            log.sendInfoAL(this, "开始备份");

            Path world = Utilities.getWorldFolder(server);

            log.trace("Minecraft 存档目录: {}", world);

            Files.createDirectories(outFile.getParent());
            Files.createFile(outFile);

            int coreCount;

            if (config.get().compressionCoreCountLimit <= 0) coreCount = Runtime.getRuntime().availableProcessors();
            else
                coreCount = Math.min(config.get().compressionCoreCountLimit, Runtime.getRuntime().availableProcessors());

            log.trace("正在使用{}个线程对{}进行压缩。可用核心数：{}", coreCount, Runtime.getRuntime().availableProcessors());

            switch (config.get().format) {
                case ZIP -> {
                    if (coreCount > 1 && !Globals.INSTANCE.disableTMPFS()) {
                        log.trace("使用并行压缩器进行压缩。线程数：{}", coreCount);
                        ParallelZipCompressor.getInstance().createArchive(world, outFile, this, coreCount);
                    } else {
                        log.trace("使用普通的Zip压缩器进行压缩 (单线程)");
                        ZipCompressor.getInstance().createArchive(world, outFile, this, coreCount);
                    }
                }
                case GZIP -> ParallelGzipCompressor.getInstance().createArchive(world, outFile, this, coreCount);
                case TAR -> new AbstractTarArchiver().createArchive(world, outFile, this, coreCount);
            }

            if(cleanup) new Cleanup(commandSource, Utilities.getLevelName(server)).call();

            if (config.get().broadcastBackupDone) Utilities.notifyPlayers(server, "完成!");
            else log.sendInfoAL(this, "完成!");

        } catch (Throwable e) {
            //ExecutorService swallows exception, so I need to catch everything
            log.error("在尝试创建新的备份文件时发生了异常！", e);

            if (ConfigHelper.INSTANCE.get().integrityVerificationMode.isStrict()) {
                try {
                    Files.delete(outFile);
                } catch (IOException ex) {
                    log.error("在尝试删除{}时发生了异常！", outFile, ex);
                }
            }

            if (initiator == ActionInitiator.Player)
                log.sendError(this, "在尝试创建新的备份文件时发生了异常！");

            throw e;
        } finally {
            Utilities.enableWorldSaving(server);
            Globals.INSTANCE.disableWatchdog = false;
        }

        return null;
    }

    private String getFileName() {
        return Utilities.getDateTimeFormatter().format(startDate) +
                (comment != null ? "#" + comment.replaceAll("[\\\\/:*?\"<>|#]", "") : "") +
                config.get().format.getCompleteString();
    }
    public static class Builder {
        private MinecraftServer server;
        private ServerCommandSource commandSource;
        private ActionInitiator initiator;
        private boolean save;
        private boolean cleanup;
        private String comment;
        private boolean announce;

        private boolean guessInitiator;

        public Builder() {
            this.server = null;
            this.commandSource = null;
            this.initiator = null;
            this.save = false;
            cleanup = true; //defaults
            this.comment = null;
            this.announce = false;

            guessInitiator = false;
        }

        public static ExecutableBackup.Builder newBackupContextBuilder() {
            return new ExecutableBackup.Builder();
        }

        public ExecutableBackup.Builder setCommandSource(ServerCommandSource commandSource) {
            this.commandSource = commandSource;
            return this;
        }

        public ExecutableBackup.Builder setServer(MinecraftServer server) {
            this.server = server;
            return this;
        }

        public ExecutableBackup.Builder setInitiator(ActionInitiator initiator) {
            this.initiator = initiator;
            return this;
        }

        public ExecutableBackup.Builder setComment(String comment) {
            this.comment = comment;
            return this;
        }

        public ExecutableBackup.Builder guessInitiator() {
            this.guessInitiator = true;
            return this;
        }

        public ExecutableBackup.Builder saveServer() {
            this.save = true;
            return this;
        }

        public ExecutableBackup.Builder noCleanup() {
            this.cleanup = false;
            return this;
        }

        public ExecutableBackup.Builder announce() {
            this.announce = true;
            return this;
        }

        public ExecutableBackup build() {
            if (guessInitiator) {
                initiator = Utilities.wasSentByPlayer(commandSource) ? ActionInitiator.Player : ActionInitiator.ServerConsole;
            } else if (initiator == null) throw new NoSuchElementException("未提供发起者！");

            if (server == null) {
                if (commandSource != null) setServer(commandSource.getServer());
                else throw new RuntimeException("未提供MinecraftServer或ServerCommandSource！");
            }

            ExecutableBackup v =  new ExecutableBackup(server, commandSource, initiator, save, cleanup, comment, LocalDateTime.now());

            if(announce) v.announce();
            return v;
        }
    }
}
