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

package net.szum123321.textile_backup.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionType;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.compressors.LZMACompressor;
import net.szum123321.textile_backup.core.compressors.ParallelBZip2Compressor;
import net.szum123321.textile_backup.core.compressors.ParallelGzipCompressor;
import net.szum123321.textile_backup.core.compressors.ParallelZipCompressor;
import net.szum123321.textile_backup.mixin.MinecraftServerSessionAccessor;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

public class MakeBackupThread implements Runnable {
    private final MinecraftServer server;
    private final ServerCommandSource ctx;
    private final String comment;

    public MakeBackupThread(MinecraftServer server, ServerCommandSource ctx, String comment){
        this.server = server;
        this.ctx = ctx;
        this.comment = comment;
    }

    @Override
    public void run() {
        Utilities.info("Starting backup", ctx);

        File world = ((MinecraftServerSessionAccessor)server)
                .getSession()
                .method_27424(RegistryKey.of(Registry.DIMENSION, DimensionType.OVERWORLD_REGISTRY_KEY.getValue()));

        TextileBackup.LOGGER.trace("Minecraft world is: {}", world);

        File outFile = BackupHelper
                .getBackupRootPath(Utilities.getLevelName(server))
                .toPath()
                .resolve(getFileName())
                .toFile();

        TextileBackup.LOGGER.trace("Outfile is: {}", outFile);

        outFile.getParentFile().mkdirs();

        try {
            outFile.createNewFile();
        } catch (IOException e) {
            TextileBackup.LOGGER.error("An exception occurred when trying to create new backup file!", e);

            Utilities.sendError("An exception occurred when trying to create new backup file!", ctx);

            return;
        }

        int coreCount;

        if(TextileBackup.config.compressionCoreCountLimit <= 0) {
            coreCount = Runtime.getRuntime().availableProcessors();
        } else {
            coreCount = Math.min(TextileBackup.config.compressionCoreCountLimit, Runtime.getRuntime().availableProcessors());
        }

        TextileBackup.LOGGER.trace("Running compression on {} threads", coreCount);

        switch (TextileBackup.config.format) {
            case ZIP:
                ParallelZipCompressor.createArchive(world, outFile, ctx, coreCount);
                break;

            case BZIP2:
                ParallelBZip2Compressor.createArchive(world, outFile, ctx, coreCount);
                break;

            case GZIP:
                ParallelGzipCompressor.createArchive(world, outFile, ctx, coreCount);
                break;

            case LZMA:
                LZMACompressor.createArchive(world, outFile, ctx); // Always single-threaded ):
                break;

            default:
                TextileBackup.LOGGER.warn("Specified compressor ({}) is not supported! Zip will be used instead!", TextileBackup.config.format);

                Utilities.sendError("Error! No correct compression format specified! using default compressor!", ctx);
                ParallelZipCompressor.createArchive(world, outFile, ctx, coreCount);
                break;
        }

        BackupHelper.executeFileLimit(ctx, Utilities.getLevelName(server));

		Utilities.info("Done!", ctx);
    }

    private String getFileName(){
        LocalDateTime now = LocalDateTime.now();

        return Utilities.getDateTimeFormatter().format(now) + (comment != null ? "#" + comment.replace("#", "") : "") + TextileBackup.config.format.getExtension();
    }
}
