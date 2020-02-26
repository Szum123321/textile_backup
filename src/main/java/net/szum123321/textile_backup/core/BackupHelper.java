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

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class BackupHelper {
    public static File getBackupRootPath(){
        File path = new File(TextileBackup.config.path);

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

    public static void log(String s, ServerCommandSource ctx){
        if(ctx != null)
            ctx.sendFeedback(new TranslatableText(s), true);

        if(TextileBackup.config.log)
            TextileBackup.logger.info(s);
    }

    public static void error(String s, ServerCommandSource ctx){
        if(ctx != null)
            ctx.sendFeedback(new TranslatableText(s), true);

        if(TextileBackup.config.log)
            TextileBackup.logger.error(s);
    }

    public static void create(MinecraftServer server, ServerCommandSource ctx, boolean save) {
        LocalDateTime now = LocalDateTime.now();

        StringBuilder builder = new StringBuilder();
        builder.append("Backup started by: ");

        if( ctx != null )
            builder.append(ctx.getName());
        else
            builder.append("SERVER");

        builder.append(" on: ");
        builder.append(getDateTimeFormatter().format(now));

        log(builder.toString(), null);

        log("Saving server...", ctx);

        if(save)
            server.save(true, false, false);

        MakeBackupThread thread = new MakeBackupThread(server, ctx);

        thread.start();

        executeFileLimit(ctx);
    }

    public static void executeFileLimit(ServerCommandSource ctx){
        File root = getBackupRootPath();
        
        if(root.isDirectory()){
            if(TextileBackup.config.maxAge > 0){
                LocalDateTime now = LocalDateTime.now();

                for(File f: root.listFiles()){
                    if(f.exists() && f.isFile()){
                        LocalDateTime time = LocalDateTime.from(
                                getDateTimeFormatter().parse(
                                        f.getName().split(".zip")[0]
                                )
                        );

                        if(now.toEpochSecond(ZoneOffset.UTC) - time.toEpochSecond(ZoneOffset.UTC) > TextileBackup.config.maxAge)
                            f.delete();
                    }
                }
            }

            if(TextileBackup.config.backupsToKeep > 0 && root.listFiles().length > TextileBackup.config.backupsToKeep){
                int var1 = root.listFiles().length - TextileBackup.config.backupsToKeep;

                File[] file = root.listFiles();
                Arrays.sort(file);

                for(int i = 0; i < var1; i++){
                    file[i].delete();

                }
            }
        }
    }

    public static DateTimeFormatter getDateTimeFormatter(){
           String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith("win")) {
            return DateTimeFormatter.ofPattern("dd.MM.yyyy_HH-mm-ss");
        } else {
            return DateTimeFormatter.ofPattern("dd.MM.yyyy_HH:mm:ss");
        }
    }
}
