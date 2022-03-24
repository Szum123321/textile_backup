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

import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.config.ConfigPOJO;
import net.szum123321.textile_backup.core.ActionInitiator;
import net.szum123321.textile_backup.core.LivingServer;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.btrfs.BtrfsSnapshotHelper;
import net.szum123321.textile_backup.core.btrfs.BtrfsUtil;
import net.szum123321.textile_backup.core.create.BackupContext;
import net.szum123321.textile_backup.core.create.BackupHelper;
import net.szum123321.textile_backup.core.restore.decompressors.GenericTarDecompressor;
import net.szum123321.textile_backup.core.restore.decompressors.ZipDecompressor;

import java.io.File;

public class RestoreBackupRunnable implements Runnable {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    private final static ConfigHelper config = ConfigHelper.INSTANCE;

    private final RestoreContext ctx;

    public RestoreBackupRunnable(RestoreContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run() {
        Statics.globalShutdownBackupFlag.set(false);

        log.info("Shutting down server...");

        ctx.getServer().stop(false);
        awaitServerShutdown();

        if(config.get().backupOldWorlds) {
            BackupHelper.create(
                    BackupContext.Builder
                            .newBackupContextBuilder()
                            .setServer(ctx.getServer())
                            .setInitiator(ActionInitiator.Restore)
                            .setComment("Old_World" + (ctx.getComment() != null ? "_" + ctx.getComment() : ""))
                            .build()
            ).run();
        }

        File worldFile = Utilities.getWorldFolder(ctx.getServer());

        log.info("Deleting old world...");

        if(!deleteDirectory(worldFile))
            log.error("Something went wrong while deleting old world!");


        if(config.get().format== ConfigPOJO.ArchiveFormat.BTRFS_SNAPSHOT)
            log.info("Starting snapshotting...");
        else {
            worldFile.mkdirs();
            log.info("Starting decompression...");
        }

        if(ctx.getFile().getArchiveFormat() == ConfigPOJO.ArchiveFormat.ZIP)
            ZipDecompressor.decompress(ctx.getFile().getFile(), worldFile);
        else if(ctx.getFile().getArchiveFormat() == ConfigPOJO.ArchiveFormat.BTRFS_SNAPSHOT)
            BtrfsSnapshotHelper.createSnapshot(ctx.getFile().getFile(), worldFile,null);
        else
            GenericTarDecompressor.decompress(ctx.getFile().getFile(), worldFile);

        if(config.get().deleteOldBackupAfterRestore) {
            log.info("Deleting old backup");

            if((ctx.getFile().getArchiveFormat() == ConfigPOJO.ArchiveFormat.BTRFS_SNAPSHOT && !BtrfsUtil.deleteSubVol(ctx.getFile().getFile())) || (ctx.getFile().getArchiveFormat() != ConfigPOJO.ArchiveFormat.BTRFS_SNAPSHOT && !ctx.getFile().getFile().delete()))
                log.info("Something went wrong while deleting old backup");
        }

        //in case we're playing on client
        Statics.globalShutdownBackupFlag.set(true);

        log.info("Done!");

        //Might solve #37
        //Idk if it's a good idea...
        //Runtime.getRuntime().exit(0);
    }

    private void awaitServerShutdown() {
        while(((LivingServer)ctx.getServer()).isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("Exception occurred!", e);
            }
        }
    }

    private static boolean deleteDirectory(File f) {
        boolean state = true;

        if(f.isDirectory()) {
            for(File f2 : f.listFiles())
                state &= deleteDirectory(f2);
        }

        return (BtrfsUtil.isSubVol(f) ? BtrfsUtil.deleteSubVol(f) : f.delete()) && state;
    }
}