/*
 * A simple backup mod for Fabric
 * Copyright (C) 2020  Szum123321
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

package net.szum123321.textile_backup.commands.restore;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.szum123321.textile_backup.core.Utilities;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

public class ListBackupsCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return CommandManager.literal("list")
                .executes(ctx -> {
                    StringBuilder builder = new StringBuilder();

                    File[] files = Utilities.getBackupRootPath(Utilities.getLevelName(ctx.getSource().getMinecraftServer())).listFiles();

                    if(files.length == 0) {
                        builder.append("There a no backups available for this world.");
                    } else if(files.length == 1) {
                        builder.append("There is only one backup available: ");
                        builder.append(files[0].getName());
                    } else {
                        Iterator<File> iterator = Arrays.stream(files).iterator();
                        builder.append("Available backups: ");

                        builder.append(iterator.next());

                        while(iterator.hasNext()) {
                            builder.append(",\n");
                            builder.append(iterator.next());
                        }
                    }

                    ctx.getSource().sendFeedback(new LiteralText(builder.toString()), false);

                    return 1;
                });
    }
}
