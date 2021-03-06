package net.szum123321.textile_backup.commands.manage;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.cottonmc.cotton.config.ConfigManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.szum123321.textile_backup.Statics;

public class BlacklistCommand {
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
		Statics.LOGGER.sendInfo(source, "Available command are: add [player], remove [player], list.");

		return 1;
	}

	private static int executeList(ServerCommandSource source) {
		StringBuilder builder = new StringBuilder();

		builder.append("Currently on the blacklist are: ");

		for(String name : Statics.CONFIG.playerBlacklist){
			builder.append(name);
			builder.append(", ");
		}

		Statics.LOGGER.sendInfo(source, builder.toString());

		return 1;
	}

	private static int executeAdd(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

		if(Statics.CONFIG.playerBlacklist.contains(player.getEntityName())) {
			Statics.LOGGER.sendInfo(ctx.getSource(), "Player: {} is already blacklisted.", player.getEntityName());
		} else {
			Statics.CONFIG.playerBlacklist.add(player.getEntityName());
			ConfigManager.saveConfig(Statics.CONFIG);

			StringBuilder builder = new StringBuilder();

			builder.append("Player: ");
			builder.append(player.getEntityName());
			builder.append(" added to the blacklist");

			if(Statics.CONFIG.playerWhitelist.contains(player.getEntityName())){
				Statics.CONFIG.playerWhitelist.remove(player.getEntityName());
				builder.append(" and removed form the whitelist");
			}

			builder.append(" successfully.");

			ctx.getSource().getMinecraftServer().getCommandManager().sendCommandTree(player);

			Statics.LOGGER.sendInfo(ctx.getSource(), builder.toString());
		}

		return 1;
	}

	private static int executeRemove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

		if(!Statics.CONFIG.playerBlacklist.contains(player.getEntityName())) {
			Statics.LOGGER.sendInfo(ctx.getSource(), "Player: {} newer was blacklisted.", player.getEntityName());
		} else {
			Statics.CONFIG.playerBlacklist.remove(player.getEntityName());
			ConfigManager.saveConfig(Statics.CONFIG);

			ctx.getSource().getMinecraftServer().getCommandManager().sendCommandTree(player);

			Statics.LOGGER.sendInfo(ctx.getSource(), "Player: {} removed from the blacklist successfully.", player.getEntityName());
		}

		return 1;
	}
}
