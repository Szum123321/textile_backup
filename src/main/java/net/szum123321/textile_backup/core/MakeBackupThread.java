package net.szum123321.textile_backup.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.dimension.DimensionType;
import net.szum123321.textile_backup.TextileBackup;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

public class MakeBackupThread extends Thread {
    MinecraftServer server;
    ServerCommandSource ctx;

    public MakeBackupThread(MinecraftServer server, ServerCommandSource ctx){
        this.server = server;
        this.ctx = ctx;
    }

    @Override
    public void run() {
        File world = server
                .getWorld(DimensionType.OVERWORLD)
                .getSaveHandler()
                .getWorldDir();
        File outFile = BackupHelper
                .getBackupRootPath()
                .toPath()
                .resolve(getFileName())
                .toFile();

        outFile.getParentFile().mkdirs();

        try {
            outFile.createNewFile();
        } catch (IOException e) {
            BackupHelper.error("Error while trying to create backup file!\n" + e.getMessage(), ctx);
            return;
        }

        Compressor.createArchive(world, outFile, ctx);

        BackupHelper.executeFileLimit(ctx);

        BackupHelper.log("Done!", ctx);
    }

    private String getFileName(){
        LocalDateTime now = LocalDateTime.now();

        return BackupHelper.getDateTimeFormatter().format(now) + ".zip";
    }
}
