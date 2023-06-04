/*
 * A simple backup mod for Fabric
 * Copyright (C)  2022   Szum123321
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.szum123321.textile_backup.commands.manage;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.core.RestoreableFile;
import net.szum123321.textile_backup.core.restore.RestoreHelper;

import java.util.*;

public class ListBackupsCommand {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return CommandManager.literal("list")
                .executes(ctx -> { StringBuilder builder = new StringBuilder();
                    var backups = RestoreHelper.getAvailableBackups(ctx.getSource().getServer());

                    if(backups.size() == 0) {
                        builder.append("There a no backups available for this world.");
                    } else if(backups.size() == 1) {
                        builder.append("There is only one backup available: ");
                        builder.append(backups.get(0).toString());
                    } else {
                        backups.sort(null);
                        Iterator<RestoreableFile> iterator = backups.iterator();
                        builder.append("Available backups:\n");

                        builder.append(iterator.next());

                        while(iterator.hasNext()) {
                            builder.append(",\n");
                            builder.append(iterator.next().toString());
                        }
                    }

                    log.sendInfo(ctx.getSource(), builder.toString());

                    return 1;
                });
    }
}
