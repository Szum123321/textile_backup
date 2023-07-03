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

package net.szum123321.textile_backup.core.restore;

import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.config.ConfigPOJO;
import net.szum123321.textile_backup.core.ActionInitiator;
import net.szum123321.textile_backup.core.CompressionStatus;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.ExecutableBackup;
import net.szum123321.textile_backup.core.restore.decompressors.GenericTarDecompressor;
import net.szum123321.textile_backup.core.restore.decompressors.ZipDecompressor;
import net.szum123321.textile_backup.mixin.MinecraftServerSessionAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.FutureTask;

/**
 * This class restores a file provided by RestoreContext.
 */
public class RestoreBackupRunnable implements Runnable {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    private final static ConfigHelper config = ConfigHelper.INSTANCE;

    private final RestoreContext ctx;

    public RestoreBackupRunnable(RestoreContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run() {
        Globals.INSTANCE.globalShutdownBackupFlag.set(false);

        log.info("关闭服务器...");

        ctx.server().stop(false);

        Path worldFile = Utilities.getWorldFolder(ctx.server()),
                tmp;

        try {
            tmp = Files.createTempDirectory(
                    ctx.server().getRunDirectory().toPath(),
                    ctx.restoreableFile().getFile().getFileName().toString()
            );
        } catch (IOException e) {
            log.error("在解压备份时发生了异常.", e);
            return;
        }

        //By making a separate thread we can start unpacking an old backup instantly
        //Let the server shut down gracefully, and wait for the old world backup to complete
        FutureTask<Void> waitForShutdown = new FutureTask<>(() -> {
            ctx.server().getThread().join(); //wait for server thread to die and save all its state

            if(config.get().backupOldWorlds) {
                return ExecutableBackup.Builder
                                .newBackupContextBuilder()
                                .setServer(ctx.server())
                                .setInitiator(ActionInitiator.Restore)
                                .noCleanup()
                                .setComment("Old_World" + (ctx.comment() != null ? "_" + ctx.comment() : ""))
                                .announce()
                                .build().call();
            }
            return null;
        });

        //run the thread.
        new Thread(waitForShutdown, "Server shutdown wait thread").start();

        try {
            log.info("开始解压...");

            long hash;

            if (ctx.restoreableFile().getArchiveFormat() == ConfigPOJO.ArchiveFormat.ZIP)
                hash = ZipDecompressor.decompress(ctx.restoreableFile().getFile(), tmp);
            else
                hash = GenericTarDecompressor.decompress(ctx.restoreableFile().getFile(), tmp);

            log.info("等待服务器完全终止...");

            //locks until the backup is finished and the server is dead
            waitForShutdown.get();

            Optional<String> errorMsg;

            if(Files.notExists(CompressionStatus.resolveStatusFilename(tmp))) {
                errorMsg = Optional.of("未找到状态文件!");
            } else {
                CompressionStatus status = CompressionStatus.readFromFile(tmp);

                log.info("状态: {}", status);

                Files.delete(tmp.resolve(CompressionStatus.DATA_FILENAME));

                errorMsg = status.validate(hash, ctx);
            }

            if(errorMsg.isEmpty() || !config.get().integrityVerificationMode.verify()) {
                if (errorMsg.isEmpty()) log.info("备份验证有效, 正在恢复.");
                else log.info("备份已损坏，但验证已禁用[{}]。正在恢复. ", errorMsg.get());

                //Disables write lock to override world file
                ((MinecraftServerSessionAccessor) ctx.server()).getSession().close();

                Utilities.deleteDirectory(worldFile);
                Files.move(tmp, worldFile);

                if (config.get().deleteOldBackupAfterRestore) {
                    log.info("正在删除恢复的备份文件.");
                    Files.delete(ctx.restoreableFile().getFile());
                }
            } else {
                log.error(errorMsg.get());
            }

        } catch (Exception e) {
            log.error("在尝试恢复备份时发生了异常！", e);
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

        log.info("完成!");
    }
}