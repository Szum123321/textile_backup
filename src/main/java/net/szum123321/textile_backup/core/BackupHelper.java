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

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;
import net.szum123321.textile_backup.TextileBackup;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class BackupHelper {
    public static void create(MinecraftServer server, ServerCommandSource ctx, boolean save, String comment) {
        LocalDateTime now = LocalDateTime.now();

        StringBuilder builder = new StringBuilder();
        builder.append("Backup started by: ");

        if( ctx != null )
            builder.append(ctx.getName());
        else
            builder.append("SERVER");

        builder.append(" on: ");
        builder.append(Utilities.getDateTimeFormatter().format(now));

        Utilities.log(builder.toString(), null);

        Utilities.log("Saving server...", ctx);

        if(save)
            server.save(true, false, false);

        MakeBackupThread thread = new MakeBackupThread(server, ctx, comment);

        thread.start();
    }

    public static void executeFileLimit(ServerCommandSource ctx, String worldName){
        File root = getBackupRootPath(worldName);

        FileFilter filter = f -> f.getName().endsWith("zip");

        if(root.isDirectory() && root.exists()){
            if(TextileBackup.config.maxAge > 0){
                LocalDateTime now = LocalDateTime.now();

                Arrays.stream(root.listFiles()).forEach(f ->{
                    if(f.exists() && f.isFile()){
                        LocalDateTime creationTime;

                        try {
                            creationTime = LocalDateTime.from(
                                    Utilities.getDateTimeFormatter().parse(
                                            f.getName().split(".zip")[0].split("#")[0]
                                    )
                            );
                        }catch(Exception e){
                            System.out.println(e.getClass());
                            System.out.println(e.toString());

                            creationTime = LocalDateTime.from(
                                    Utilities.getBackupDateTimeFormatter().parse(
                                            f.getName().split(".zip")[0].split("#")[0]
                                    )
                            );

                        }

                        if(now.toEpochSecond(ZoneOffset.UTC) - creationTime.toEpochSecond(ZoneOffset.UTC) > TextileBackup.config.maxAge) {
                            Utilities.log("Deleting: " + f.getName(), ctx);
                            f.delete();
                        }
                    }
                });
            }

            if(TextileBackup.config.backupsToKeep > 0 && root.listFiles().length > TextileBackup.config.backupsToKeep){
                int var1 = root.listFiles().length - TextileBackup.config.backupsToKeep;

                File[] files = root.listFiles(filter);
                assert files != null;

                Arrays.sort(files);

                for(int i = 0; i < var1; i++) {
                    Utilities.log("Deleting: " + files[i].getName(), ctx);
                    files[i].delete();
                }
            }

            if(TextileBackup.config.maxSize > 0 && FileUtils.sizeOfDirectory(root) / 1024 > TextileBackup.config.maxSize){
                 Arrays.stream(root.listFiles()).sorted().forEach(e -> {
                    if(FileUtils.sizeOfDirectory(root) / 1024 > TextileBackup.config.maxSize){
                        Utilities.log("Deleting: " + e.getName(), ctx);
                        e.delete();
                    }
                });
            }
        }
    }

    public static File getBackupRootPath(String worldName){
        File path = new File(TextileBackup.config.path);

        if(TextileBackup.config.perWorldBackup)
            path = path.toPath().resolve(worldName).toFile();

        if(!path.exists()){
            try{
                path.mkdirs();
            }catch(Exception e){
                TextileBackup.logger.error(e.getMessage());

                return FabricLoader
                        .getInstance()
                        .getGameDirectory()
                        .toPath()
                        .resolve(TextileBackup.config.path)
                        .toFile();
            }
        }

        return path;
    }
}
