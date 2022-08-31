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

package net.szum123321.textile_backup.core.create;

import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.core.ActionInitiator;
import net.szum123321.textile_backup.core.Cleanup;
import net.szum123321.textile_backup.core.create.compressors.*;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.compressors.tar.AbstractTarArchiver;
import net.szum123321.textile_backup.core.create.compressors.tar.ParallelBZip2Compressor;
import net.szum123321.textile_backup.core.create.compressors.tar.ParallelGzipCompressor;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class MakeBackupRunnable implements Runnable {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    private final static ConfigHelper config = ConfigHelper.INSTANCE;

    private final BackupContext context;

    public MakeBackupRunnable(BackupContext context) {
        this.context = context;
    }

    @Override
    public void run() {
        try {
            Utilities.disableWorldSaving(context.server());
            Globals.INSTANCE.disableWatchdog = true;

            Globals.INSTANCE.updateTMPFSFlag(context.server());

            log.sendInfoAL(context, "Starting backup");

            Path world = Utilities.getWorldFolder(context.server());

            log.trace("Minecraft world is: {}", world);

            Path outFile = Utilities
                    .getBackupRootPath(Utilities.getLevelName(context.server()))
                    .resolve(getFileName());

            log.trace("Outfile is: {}", outFile);

            Files.createDirectories(outFile.getParent());
            Files.createFile(outFile);

            int coreCount;

            if(config.get().compressionCoreCountLimit <= 0) {
                coreCount = Runtime.getRuntime().availableProcessors();
            } else {
                coreCount = Math.min(config.get().compressionCoreCountLimit, Runtime.getRuntime().availableProcessors());
            }

            log.trace("Running compression on {} threads. Available cores: {}", coreCount, Runtime.getRuntime().availableProcessors());

            switch (config.get().format) {
                case ZIP -> {
                    if (coreCount > 1 && !Globals.INSTANCE.disableTMPFS()) {
                        ParallelZipCompressor.getInstance().createArchive(world, outFile, context, coreCount);
                        log.trace("Using PARALLEL Zip Compressor. Threads: {}", coreCount);
                    } else {
                        ZipCompressor.getInstance().createArchive(world, outFile, context, coreCount);
                        log.trace("Using REGULAR Zip Compressor. Threads: {}");
                    }
                }
                case BZIP2 -> ParallelBZip2Compressor.getInstance().createArchive(world, outFile, context, coreCount);
                case GZIP -> ParallelGzipCompressor.getInstance().createArchive(world, outFile, context, coreCount);
                case LZMA -> new AbstractTarArchiver() {
                    protected OutputStream getCompressorOutputStream(OutputStream stream, BackupContext ctx, int coreLimit) throws IOException {
                        return new LZMACompressorOutputStream(stream);
                    }
                }.createArchive(world, outFile, context, coreCount);
                case TAR -> new AbstractTarArchiver().createArchive(world, outFile, context, coreCount);
            }

            Cleanup.executeFileLimit(context.commandSource(), Utilities.getLevelName(context.server()));

            if(config.get().broadcastBackupDone) {
                Utilities.notifyPlayers(
                        context.server(),
                        "Done!"
                );
            } else {
                log.sendInfoAL(context, "Done!");
            }
        } catch (IOException e) {
            log.error("An exception occurred when trying to create new backup file!", e);

            if(context.initiator() == ActionInitiator.Player)
                log.sendError(context, "An exception occurred when trying to create new backup file!");
        } finally {
            Utilities.enableWorldSaving(context.server());
            Globals.INSTANCE.disableWatchdog = false;
        }
    }

    private String getFileName(){
        LocalDateTime now = LocalDateTime.now();

        return Utilities.getDateTimeFormatter().format(now) +
                (context.comment() != null ? "#" + context.comment().replaceAll("[\\\\/:*?\"<>|#]", "") : "") +
                config.get().format.getCompleteString();
    }
}
