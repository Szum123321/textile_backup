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
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import szum123321.textile_backup.ConfigHandler;
import szum123321.textile_backup.TextileBackup;
import szum123321.textile_backup.core.Utilities;

public class BlacklistCommand {
	public static LiteralArgumentBuilder<CommandSource> register(){
		return Commands.literal("blacklist")
				.then(Commands.literal("add")
						.then(Commands.argument("player", EntityArgument.player())
								.executes(BlacklistCommand::executeAdd)
						)
				).then(Commands.literal("remove")
						.then(Commands.argument("player", EntityArgument.player())
								.executes(BlacklistCommand::executeRemove)
						)
				).then(Commands.literal("list")
						.executes(ctx -> executeList(ctx.getSource()))
				).executes(ctx -> help(ctx.getSource()));
	}

	private static int help(CommandSource source){
		source.sendFeedback(new StringTextComponent("Available command are: add [player], remove [player], list."), false);

		return 1;
	}

	private static int executeList(CommandSource source){
		StringBuilder builder = new StringBuilder();

		builder.append("Currently on the blacklist are: ");

		for(String name : TextileBackup.config.playerBlacklist){
			builder.append(name);
			builder.append(", ");
		}

		Utilities.log(builder.toString(), source);

		return 1;
	}

	private static int executeAdd(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
		PlayerEntity player = EntityArgument.getPlayer(ctx, "player");

		if(TextileBackup.config.playerBlacklist.contains(player.getName().getString())) {
			ctx.getSource().sendFeedback(new TranslationTextComponent("Player: %s is already blacklisted.", player.getName()), false);
		}else{
			TextileBackup.config.playerBlacklist.add(player.getName().getString());
			ConfigHandler.saveConfig(TextileBackup.config);

			StringBuilder builder = new StringBuilder();

			builder.append("Player: ");
			builder.append(player.getName());
			builder.append(" added to the blacklist");

			if(TextileBackup.config.playerWhitelist.contains(player.getName().getString())){
				TextileBackup.config.playerWhitelist.remove(player.getName().getString());
				builder.append(" and removed form the whitelist");
			}

			builder.append(" successfully.");

			Utilities.log(builder.toString(), ctx.getSource());
		}

		return 1;
	}

	private static int executeRemove(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
		PlayerEntity player = EntityArgument.getPlayer(ctx, "player");

		if(!TextileBackup.config.playerBlacklist.contains(player.getName().getString())) {
			ctx.getSource().sendFeedback(new TranslationTextComponent("Player: %s newer was blacklisted.", player.getName()), false);
		}else{
			TextileBackup.config.playerBlacklist.remove(player.getName().getString());
			ConfigHandler.saveConfig(TextileBackup.config);

			StringBuilder builder = new StringBuilder();

			builder.append("Player: ");
			builder.append(player.getName());
			builder.append(" removed from the blacklist successfully.");

			Utilities.log(builder.toString(), ctx.getSource());
		}

		return 1;
	}
}
