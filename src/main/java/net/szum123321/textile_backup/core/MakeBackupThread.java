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
import net.minecraft.world.dimension.DimensionType;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

public class MakeBackupThread implements Runnable {
    private MinecraftServer server;
    private ServerCommandSource ctx;
    private String comment;

    public MakeBackupThread(MinecraftServer server, ServerCommandSource ctx, String comment){
        this.server = server;
        this.ctx = ctx;
        this.comment = comment;
    }

    @Override
    public void run() {
        File world = server
                .getWorld(DimensionType.OVERWORLD)
                .getSaveHandler()
                .getWorldDir();

        File outFile = BackupHelper
                .getBackupRootPath(server.getWorld(DimensionType.OVERWORLD).getLevelProperties().getLevelName())
                .toPath()
                .resolve(getFileName())
                .toFile();

        outFile.getParentFile().mkdirs();

        try {
            outFile.createNewFile();
        } catch (IOException e) {
            Utilities.error("Error while trying to create backup file!\n" + e.getMessage(), ctx);
            return;
        }

        Compressor.createArchive(world, outFile, ctx);

        BackupHelper.executeFileLimit(ctx, server.getWorld(DimensionType.OVERWORLD).getLevelProperties().getLevelName());

		Utilities.log("Done!", ctx);
    }

    private String getFileName(){
        LocalDateTime now = LocalDateTime.now();

        return Utilities.getDateTimeFormatter().format(now) + (comment != null ? "#" + comment.replace("#", ""): "") + ".zip";
    }
}
