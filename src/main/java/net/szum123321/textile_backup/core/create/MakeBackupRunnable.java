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

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.create.compressors.*;
import net.szum123321.textile_backup.core.Utilities;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

public class MakeBackupRunnable implements Runnable {
    private final MinecraftServer server;
    private final ServerCommandSource commandSource;
    private final String comment;

    public MakeBackupRunnable(BackupContext context){
        this.server = context.getServer();
        this.commandSource = context.getCommandSource();
        this.comment = context.getComment();
    }

    @Override
    public void run() {
        Statics.LOGGER.sendInfo(commandSource, "Starting backup");

        File world = Utilities.getWorldFolder(server);

        Statics.LOGGER.trace("Minecraft world is: {}", world);

        File outFile = Utilities
                .getBackupRootPath(Utilities.getLevelName(server))
                .toPath()
                .resolve(getFileName())
                .toFile();

        Statics.LOGGER.trace("Outfile is: {}", outFile);

        outFile.getParentFile().mkdirs();

        try {
            outFile.createNewFile();
        } catch (IOException e) {
            Statics.LOGGER.error("An exception occurred when trying to create new backup file!", e);
            Statics.LOGGER.sendError(commandSource, "An exception occurred when trying to create new backup file!");

            return;
        }

        int coreCount;

        if(Statics.CONFIG.compressionCoreCountLimit <= 0) {
            coreCount = Runtime.getRuntime().availableProcessors();
        } else {
            coreCount = Math.min(Statics.CONFIG.compressionCoreCountLimit, Runtime.getRuntime().availableProcessors());
        }

        Statics.LOGGER.trace("Running compression on {} threads. Available cores = {}", coreCount, Runtime.getRuntime().availableProcessors());

        switch (Statics.CONFIG.format) {
            case ZIP:
                ParallelZipCompressor.createArchive(world, outFile, commandSource, coreCount);
                break;

            case BZIP2:
                ParallelBZip2Compressor.getInstance().createArchive(world, outFile, commandSource, coreCount);
                break;

            case GZIP:
                ParallelGzipCompressor.getInstance().createArchive(world, outFile, commandSource, coreCount);
                break;

            case LZMA:
                LZMACompressor.getInstance().createArchive(world, outFile, commandSource, coreCount);
                break;

            default:
                Statics.LOGGER.warn("Specified compressor ({}) is not supported! Zip will be used instead!", Statics.CONFIG.format);
                Statics.LOGGER.sendError(commandSource, "Error! No correct compression format specified! Using default compressor!");

                ParallelZipCompressor.createArchive(world, outFile, commandSource, coreCount);
                break;
        }

        BackupHelper.executeFileLimit(commandSource, Utilities.getLevelName(server));

        Statics.LOGGER.sendInfo(commandSource, "Done!");
    }

    private String getFileName(){
        LocalDateTime now = LocalDateTime.now();

        return Utilities.getDateTimeFormatter().format(now) + (comment != null ? "#" + comment.replace("#", "") : "") + Statics.CONFIG.format.getString();
    }
}
