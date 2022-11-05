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
import net.szum123321.textile_backup.core.LivingServer;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.BackupContext;
import net.szum123321.textile_backup.core.create.MakeBackupRunnableFactory;
import net.szum123321.textile_backup.core.restore.decompressors.GenericTarDecompressor;
import net.szum123321.textile_backup.core.restore.decompressors.ZipDecompressor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

//TODO: Verify backup's validity?
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
        awaitServerShutdown();

        if(config.get().backupOldWorlds) {
            MakeBackupRunnableFactory.create(
                    BackupContext.Builder
                            .newBackupContextBuilder()
                            .setServer(ctx.server())
                            .setInitiator(ActionInitiator.Restore)
                            .setComment("Old_World" + (ctx.comment() != null ? "_" + ctx.comment() : ""))
                            .build()
            ).run();
        }

        Path worldFile = Utilities.getWorldFolder(ctx.server());

        try {
            Path tmp = Files.createTempDirectory(
                    worldFile.getParent(),
                    ctx.restoreableFile().getFile().getFileName().toString());

            log.info("Starting decompression...");

            if (ctx.restoreableFile().getArchiveFormat() == ConfigPOJO.ArchiveFormat.ZIP)
                ZipDecompressor.decompress(ctx.restoreableFile().getFile(), tmp);
            else
                GenericTarDecompressor.decompress(ctx.restoreableFile().getFile(), tmp);

            log.info("Deleting old world...");

            Utilities.deleteDirectory(worldFile);
            Files.move(tmp, worldFile);

            if (config.get().deleteOldBackupAfterRestore) {
                log.info("Deleting old backup");

                Files.delete(ctx.restoreableFile().getFile());
            }
        } catch (IOException e) {
            log.error("An exception occurred while trying to restore a backup!", e);
        }

        //in case we're playing on client
        Globals.INSTANCE.globalShutdownBackupFlag.set(true);

        log.info("Done!");
    }

    private void awaitServerShutdown() {
        while(((LivingServer)ctx.server()).isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("Exception occurred!", e);
            }
        }
    }
}