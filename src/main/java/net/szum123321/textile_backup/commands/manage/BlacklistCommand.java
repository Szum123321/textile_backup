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
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;

public class BlacklistCommand {
	private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
	private final static ConfigHelper config = ConfigHelper.INSTANCE;

	public static LiteralArgumentBuilder<ServerCommandSource> register() {
		return CommandManager.literal("blacklist")
				.then(CommandManager.literal("add")
						.then(CommandManager.argument("player", EntityArgumentType.player())
								.executes(BlacklistCommand::executeAdd)
						)
				).then(CommandManager.literal("remove")
						.then(CommandManager.argument("player", EntityArgumentType.player())
								.executes(BlacklistCommand::executeRemove)
						)
				).then(CommandManager.literal("list")
						.executes(ctx -> executeList(ctx.getSource()))
				).executes(ctx -> help(ctx.getSource()));
	}

	private static int help(ServerCommandSource source) {
		log.sendInfo(source, "可用的命令有：add [player]，remove [player]，list. ");

		return 1;
	}

	private static int executeList(ServerCommandSource source) {
		StringBuilder builder = new StringBuilder();

		builder.append("目前在黑名单上的有：");

		for(String name : config.get().playerBlacklist){
			builder.append(name);
			builder.append(", ");
		}

		log.sendInfo(source, builder.toString());

		return 1;
	}

	private static int executeAdd(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

		if(config.get().playerBlacklist.contains(player.getEntityName())) {
			log.sendInfo(ctx.getSource(), "玩家: {} 已经在黑名单中!", player.getEntityName());
		} else {
			config.get().playerBlacklist.add(player.getEntityName());
			config.save();

			StringBuilder builder = new StringBuilder();

			builder.append("玩家: ");
			builder.append(player.getEntityName());
			builder.append(" 被添加到黑名单");

			if(config.get().playerWhitelist.contains(player.getEntityName())){
				config.get().playerWhitelist.remove(player.getEntityName());
				config.save();
				builder.append(" 并且被移除白名单");
			}

			builder.append(" 成功.");

			ctx.getSource().getServer().getCommandManager().sendCommandTree(player);

			log.sendInfo(ctx.getSource(), builder.toString());
		}

		return 1;
	}

	private static int executeRemove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

		if(!config.get().playerBlacklist.contains(player.getEntityName())) {
			log.sendInfo(ctx.getSource(), "玩家: {} 还从未被列入黑名单.", player.getEntityName());
		} else {
			config.get().playerBlacklist.remove(player.getEntityName());
			config.save();

			ctx.getSource().getServer().getCommandManager().sendCommandTree(player);

			log.sendInfo(ctx.getSource(), "玩家: {} 被移除黑名单成功! ", player.getEntityName());
		}

		return 1;
	}
}
