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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.dimension.DimensionType;
import szum123321.textile_backup.core.BackupHelper;

public class CleanupCommand {
    public static LiteralArgumentBuilder<CommandSource> register(){
        return Commands.literal("cleanup")
                .executes(ctx -> execute(ctx.getSource()));
    }

    private static int execute(CommandSource source){
        BackupHelper.executeFileLimit(source, source.getServer().getWorld(DimensionType.OVERWORLD).getWorldInfo().getWorldName());
        source.sendFeedback(new StringTextComponent("Done"), false);

        return 1;
    }
}
