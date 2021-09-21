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

import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.core.ActionInitiator;
import net.szum123321.textile_backup.core.create.compressors.*;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.compressors.tar.AbstractTarArchiver;
import net.szum123321.textile_backup.core.create.compressors.tar.ParallelBZip2Compressor;
import net.szum123321.textile_backup.core.create.compressors.tar.ParallelGzipCompressor;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;

public class MakeBackupRunnable implements Runnable {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    private final static ConfigHelper config = ConfigHelper.INSTANCE;

    private final BackupContext context;

    public MakeBackupRunnable(BackupContext context){
        this.context = context;
    }

    @Override
    public void run() {
        try {
            Utilities.disableWorldSaving(context.getServer());
            Statics.disableWatchdog = true;

            log.sendInfoAL(context, "Starting backup");

            File world = Utilities.getWorldFolder(context.getServer());

            log.trace("Minecraft world is: {}", world);

            File outFile = Utilities
                    .getBackupRootPath(Utilities.getLevelName(context.getServer()))
                    .toPath()
                    .resolve(getFileName())
                    .toFile();

            log.trace("Outfile is: {}", outFile);

            outFile.getParentFile().mkdirs();

            try {
                outFile.createNewFile();
            } catch (IOException e) {
                log.error("An exception occurred when trying to create new backup file!", e);

                if(context.getInitiator() == ActionInitiator.Player)
                    log.sendError(context, "An exception occurred when trying to create new backup file!");

                return;
            }

            int coreCount;

            if(config.get().compressionCoreCountLimit <= 0) {
                coreCount = Runtime.getRuntime().availableProcessors();
            } else {
                coreCount = Math.min(config.get().compressionCoreCountLimit, Runtime.getRuntime().availableProcessors());
            }

            log.trace("Running compression on {} threads. Available cores: {}", coreCount, Runtime.getRuntime().availableProcessors());

            switch (config.get().format) {
                case ZIP -> {
                    if (coreCount > 1)
                        ParallelZipCompressor.getInstance().createArchive(world, outFile, context, coreCount);
                    else
                        ZipCompressor.getInstance().createArchive(world, outFile, context, coreCount);
                }
                case BZIP2 -> ParallelBZip2Compressor.getInstance().createArchive(world, outFile, context, coreCount);
                case GZIP -> ParallelGzipCompressor.getInstance().createArchive(world, outFile, context, coreCount);
                case LZMA -> new AbstractTarArchiver() {
                    protected OutputStream getCompressorOutputStream(OutputStream stream, BackupContext ctx, int coreLimit) throws IOException {
                        return new LZMACompressorOutputStream(stream);
                    }
                }.createArchive(world, outFile, context, coreCount);
                case TAR -> new AbstractTarArchiver().createArchive(world, outFile, context, coreCount);
                default -> {
                    log.warn("Specified compressor ({}) is not supported! Zip will be used instead!", config.get().format);
                    if (context.getInitiator() == ActionInitiator.Player)
                        log.sendError(context.getCommandSource(), "Error! No correct compression format specified! Using default compressor!");
                    ZipCompressor.getInstance().createArchive(world, outFile, context, coreCount);
                }
            }

            BackupHelper.executeFileLimit(context.getCommandSource(), Utilities.getLevelName(context.getServer()));

            BackupHelper.notifyPlayers(context, "Server backup completed!");
        } finally {
            Utilities.enableWorldSaving(context.getServer());
            Statics.disableWatchdog = false;
        }
    }

    private String getFileName(){
        LocalDateTime now = LocalDateTime.now();

        return Utilities.getDateTimeFormatter().format(now) +
                (context.getComment() != null ? "#" + context.getComment().replace("#", "") : "") +
                config.get().format.getCompleteString();
    }
}
