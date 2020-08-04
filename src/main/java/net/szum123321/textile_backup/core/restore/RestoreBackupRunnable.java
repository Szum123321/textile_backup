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

import net.minecraft.server.MinecraftServer;
import net.szum123321.textile_backup.core.LivingServer;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.BackupContext;
import net.szum123321.textile_backup.core.create.BackupHelper;
import net.szum123321.textile_backup.core.restore.decompressors.GenericTarDecompressor;
import net.szum123321.textile_backup.core.restore.decompressors.ZipDecompressor;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

public class RestoreBackupRunnable implements Runnable {
    private final MinecraftServer server;
    private final File backupFile;
    private final String finalBackupComment;

    public RestoreBackupRunnable(MinecraftServer server, File backupFile, String finalBackupComment) {
        this.server = server;
        this.backupFile = backupFile;
        this.finalBackupComment = finalBackupComment;
    }

    @Override
    public void run() {
        TextileBackup.LOGGER.info("Starting countdown...");
        waitDelay();

        TextileBackup.LOGGER.info("Shutting down server...");
        server.stop(false);
        awaitServerShutdown();

        if(TextileBackup.CONFIG.backupOldWorlds) {
            BackupHelper.create(
                    new BackupContext.Builder()
                            .setServer(server)
                            .setInitiator(BackupContext.BackupInitiator.Restore)
                            .setComment("Old_World" + (finalBackupComment != null ? "_" + finalBackupComment : ""))
                            .build()
            ).run();
        }

        File worldFile = Utilities.getWorldFolder(server);

        TextileBackup.LOGGER.info("Deleting old world...");
        if(!deleteDirectory(worldFile))
            TextileBackup.LOGGER.error("Something went wrong while deleting old world!");

        worldFile.mkdirs();

        try(FileInputStream fileInputStream = new FileInputStream(backupFile)) {
            TextileBackup.LOGGER.info("Starting decompression...");

            switch(Utilities.getFileExtension(backupFile).orElseThrow(() -> new NoSuchElementException("Couldn't get file extention!"))) {
                case ZIP:
                    ZipDecompressor.decompress(fileInputStream, worldFile);
                    break;

                case GZIP:
                    GenericTarDecompressor.decompress(fileInputStream, worldFile, GzipCompressorInputStream.class);
                    break;

                case BZIP2:
                    GenericTarDecompressor.decompress(fileInputStream, worldFile, BZip2CompressorInputStream.class);
                    break;

                case LZMA:
                    GenericTarDecompressor.decompress(fileInputStream, worldFile, XZCompressorInputStream.class);
                    break;
            }
        } catch (IOException e) {
            TextileBackup.LOGGER.error("Exception occurred!", e);
        }

        TextileBackup.LOGGER.info("Done.");
    }

    private void waitDelay() {
        int delay = TextileBackup.CONFIG.restoreDelay;

        if(delay > 0) {
            try {
                Thread.sleep(1000 * delay);
            } catch (InterruptedException e) {
                TextileBackup.LOGGER.error("Exception occurred!", e);
            }
        }
    }

    private void awaitServerShutdown() {
        while(((LivingServer)server).isAlive()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                TextileBackup.LOGGER.error("Exception occurred!", e);
            }
        }
    }

    private static boolean deleteDirectory(File f) {
        boolean state = true;

        if(f.isDirectory()) {
            for(File f2 : f.listFiles())
                state &= deleteDirectory(f2);
        }

        return f.delete() && state;
    }
}