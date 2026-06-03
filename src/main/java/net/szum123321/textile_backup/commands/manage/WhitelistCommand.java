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
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;

public class WhitelistCommand {
	private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
	private final static ConfigHelper config = ConfigHelper.INSTANCE;

	public static LiteralArgumentBuilder<CommandSourceStack> register(){
		return Commands.literal("whitelist")
				.then(Commands.literal("add")
						.then(Commands.argument("player", EntityArgument.player())
								.executes(WhitelistCommand::executeAdd)
						)
				).then(Commands.literal("remove")
						.then(Commands.argument("player", EntityArgument.player())
								.executes(WhitelistCommand::executeRemove)
						)
				).then(Commands.literal("list")
						.executes(ctx -> executeList(ctx.getSource()))
				).executes(ctx -> help(ctx.getSource()));
	}

	private static int help(CommandSourceStack source){
		log.sendInfo(source, "Available command are: add [player], remove [player], list.");

		return 1;
	}

	private static int executeList(CommandSourceStack source){
		StringBuilder builder = new StringBuilder();

		builder.append("Currently on the whitelist are: ");

		for(String name : config.get().playerWhitelist){
			builder.append(name);
			builder.append(", ");
		}

		log.sendInfo(source, builder.toString());

		return 1;
	}

	private static int executeAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

		if(config.get().playerWhitelist.contains(player.getScoreboardName())) {
			log.sendInfo(ctx.getSource(), "Player: {} is already whitelisted.", player.getScoreboardName());
		} else {
			config.get().playerWhitelist.add(player.getScoreboardName());
			config.save();

			StringBuilder builder = new StringBuilder();

			builder.append("Player: ");
			builder.append(player.getScoreboardName());
			builder.append(" added to the whitelist");

			if(config.get().playerBlacklist.contains(player.getScoreboardName())){
				config.get().playerBlacklist.remove(player.getScoreboardName());
				config.save();
				builder.append(" and removed form the blacklist");
			}

			builder.append(" successfully.");

			ctx.getSource().getServer().getCommands().sendCommands(player);

			log.sendInfo(ctx.getSource(), builder.toString());
		}

		return 1;
	}

	private static int executeRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

		if(!config.get().playerWhitelist.contains(player.getScoreboardName())) {
			log.sendInfo(ctx.getSource(), "Player: {} newer was whitelisted.", player.getScoreboardName());
		} else {
			config.get().playerWhitelist.remove(player.getScoreboardName());
			config.save();

			ctx.getSource().getServer().getCommands().sendCommands(player);

			log.sendInfo(ctx.getSource(), "Player: {} removed from the whitelist successfully.", player.getScoreboardName());
		}

		return 1;
	}
}
