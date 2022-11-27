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

import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.config.ConfigPOJO;
import net.szum123321.textile_backup.core.ActionInitiator;
import net.szum123321.textile_backup.core.CompressionStatus;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.BackupContext;
import net.szum123321.textile_backup.core.create.MakeBackupRunnableFactory;
import net.szum123321.textile_backup.core.restore.decompressors.GenericTarDecompressor;
import net.szum123321.textile_backup.core.restore.decompressors.ZipDecompressor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

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

        log.info("Shutting down server...");

        ctx.server().stop(false);

        Path worldFile = Utilities.getWorldFolder(ctx.server()),
                tmp;

        try {
            tmp = Files.createTempDirectory(
                    ctx.server().getRunDirectory().toPath(),
                    ctx.restoreableFile().getFile().getFileName().toString());
        } catch (IOException e) {
            log.error("An exception occurred while unpacking backup", e);
            return;
        }

        FutureTask<Void> waitForShutdown = new FutureTask<>(() -> {
            ctx.server().getThread().join(); //wait for server to die and save all its state
            if(config.get().backupOldWorlds) {
                return MakeBackupRunnableFactory.create(
                        BackupContext.Builder
                                .newBackupContextBuilder()
                                .setServer(ctx.server())
                                .setInitiator(ActionInitiator.Restore)
                                .setComment("Old_World" + (ctx.comment() != null ? "_" + ctx.comment() : ""))
                                .build()
                ).call();
            }
            return null;
        });

        new Thread(waitForShutdown).start();

        try {
            log.info("Starting decompression...");

            long hash;

            if (ctx.restoreableFile().getArchiveFormat() == ConfigPOJO.ArchiveFormat.ZIP)
                hash = ZipDecompressor.decompress(ctx.restoreableFile().getFile(), tmp);
            else
                hash = GenericTarDecompressor.decompress(ctx.restoreableFile().getFile(), tmp);

            CompressionStatus status = CompressionStatus.readFromFile(tmp);
            Files.delete(tmp.resolve(CompressionStatus.DATA_FILENAME));

            //locks until the backup is finished
            waitForShutdown.get();

            log.info("Status: {}", status);

            boolean valid = status.isValid(hash);
            if(valid || !config.get().errorErrorHandlingMode.verify()) {
                if(valid) log.info("Backup valid. Restoring");
                else log.info("Backup is damaged, but verification is disabled. Restoring");

                Utilities.deleteDirectory(worldFile);
                Files.move(tmp, worldFile);

                if (config.get().deleteOldBackupAfterRestore) {
                    log.info("Deleting restored backup file");
                    Files.delete(ctx.restoreableFile().getFile());
                }
            } else {
                log.error("File tree hash mismatch! Got: {}, Expected {}. Aborting", hash, status.treeHash());
            }
        } catch (ExecutionException | InterruptedException | ClassNotFoundException | IOException e) {
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
}