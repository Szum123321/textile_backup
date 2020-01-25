package net.szum123321.textile_backup.core;

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.TextileBackup;
import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Compressor {
    public static void createArchive(File in, File out, ServerCommandSource ctx){
        BackupHelper.log("Starting compression...", ctx);

        try(ZipOutputStream arc = new ZipOutputStream(new FileOutputStream(out))) {
            arc.setLevel(TextileBackup.config.compression);
            addToArchive(arc, in, ".");
        } catch (IOException e) {
            TextileBackup.logger.error(e.getMessage());
        }

        BackupHelper.log("Compression finished", ctx);
    }

    private static void addToArchive(ZipOutputStream out, File file, String dir) throws IOException {
        String name = dir + File.separator + file.getName();

        if(file.isFile()){
            ZipEntry entry = new ZipEntry(name);
            out.putNextEntry(entry);
            entry.setSize(file.length());
            IOUtils.copy(new FileInputStream(file), out);
            out.closeEntry();
        }else if(file.isDirectory() && file.listFiles() != null){
            for(File f: file.listFiles()){
                if(f != null){
                    addToArchive(out, f, name);
                }
            }
        }
    }
}
