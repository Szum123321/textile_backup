/*
 * Simple backup mod made for Fabric and ported to Forge
 *     Copyright (C) 2020  Szum123321
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package szum123321.textile_backup.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import szum123321.textile_backup.core.BackupHelper;

public class StartBackupCommand {
    public static LiteralArgumentBuilder<CommandSource> register(){
        return Commands.literal("start")
                .then(Commands.argument("comment", StringArgumentType.string())
                        .executes(StartBackupCommand::executeWithComment)
                ).executes(ctx -> execute(ctx.getSource()));
    }

    private static int executeWithComment(CommandContext<CommandSource> source) {
        BackupHelper.create(source.getSource().getServer(), source.getSource(), true, StringArgumentType.getString(source, "comment").replace("#", ""));

        return 1;
    }

    private static int execute(CommandSource source){
        BackupHelper.create(source.getServer(), source,true, null);

        return 1;
    }
}
