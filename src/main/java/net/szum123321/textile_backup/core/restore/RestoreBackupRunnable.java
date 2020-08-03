package net.szum123321.textile_backup.core.restore;

import net.minecraft.server.MinecraftServer;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.restore.decompressors.GenericTarDecompressor;
import net.szum123321.textile_backup.core.restore.decompressors.ZipDecompressor;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.File;

public class RestoreBackupRunnable implements Runnable {
    private final MinecraftServer server;
    private final File backupFile;

    public RestoreBackupRunnable(MinecraftServer server, File backupFile) {
        this.server = server;
        this.backupFile = backupFile;
    }

    @Override
    public void run() {
        while(server.isRunning()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                TextileBackup.LOGGER.error("Exception occurred!", e);
            }
        }

        File worldFile = Utilities.getWorldFolder(server);

        deleteDirectory(worldFile);

        worldFile.mkdirs();

        switch(Utilities.getFileExtension(backupFile).get()) {
            case ZIP:
                ZipDecompressor.decompress(backupFile, worldFile);
                break;

            case GZIP:
                GenericTarDecompressor.decompress(backupFile, worldFile, GzipCompressorInputStream.class);
                break;

            case BZIP2:
                GenericTarDecompressor.decompress(backupFile, worldFile, BZip2CompressorInputStream.class);
                break;

            case LZMA:
                GenericTarDecompressor.decompress(backupFile, worldFile, XZCompressorInputStream.class);
                break;
        }

        TextileBackup.LOGGER.info("Done.");
    }

    private static void deleteDirectory(File f) {
        if(f.isDirectory()) {
            for(File f2 : f.listFiles())
                deleteDirectory(f2);
        }

        f.delete();
    }
}
