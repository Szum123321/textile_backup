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

package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.BackupHelper;

public class CleanupCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register(){
        return CommandManager.literal("cleanup")
                .requires(ctx -> TextileBackup.config.whitelist.contains(ctx.getName()) ||
                        ctx.hasPermissionLevel(TextileBackup.config.permissionLevel) &&
                                !TextileBackup.config.blacklist.contains(ctx.getName()))
                .executes(ctx -> execute(ctx.getSource()));
    }

    private static int execute(ServerCommandSource source){
        BackupHelper.executeFileLimit(source, source.getMinecraftServer().getLevelName());
        source.sendFeedback(new TranslatableText("Done"), false);

        return 1;
    }
}
