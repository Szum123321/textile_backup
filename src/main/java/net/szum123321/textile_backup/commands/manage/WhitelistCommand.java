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

public class WhitelistCommand {
	private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
	private final static ConfigHelper config = ConfigHelper.INSTANCE;

	public static LiteralArgumentBuilder<ServerCommandSource> register(){
		return CommandManager.literal("whitelist")
				.then(CommandManager.literal("add")
						.then(CommandManager.argument("player", EntityArgumentType.player())
								.executes(WhitelistCommand::executeAdd)
						)
				).then(CommandManager.literal("remove")
						.then(CommandManager.argument("player", EntityArgumentType.player())
								.executes(WhitelistCommand::executeRemove)
						)
				).then(CommandManager.literal("list")
						.executes(ctx -> executeList(ctx.getSource()))
				).executes(ctx -> help(ctx.getSource()));
	}

	private static int help(ServerCommandSource source){
		log.sendInfo(source, "Available command are: add [player], remove [player], list.");

		return 1;
	}

	private static int executeList(ServerCommandSource source){
		StringBuilder builder = new StringBuilder();

		builder.append("Currently on the whitelist are: ");

		for(String name : config.get().playerWhitelist){
			builder.append(name);
			builder.append(", ");
		}

		log.sendInfo(source, builder.toString());

		return 1;
	}

	private static int executeAdd(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

		if(config.get().playerWhitelist.contains(player.getEntityName())) {
			log.sendInfo(ctx.getSource(), "Player: {} is already whitelisted.", player.getEntityName());
		} else {
			config.get().playerWhitelist.add(player.getEntityName());
			config.save();

			StringBuilder builder = new StringBuilder();

			builder.append("Player: ");
			builder.append(player.getEntityName());
			builder.append(" added to the whitelist");

			if(config.get().playerBlacklist.contains(player.getEntityName())){
				config.get().playerBlacklist.remove(player.getEntityName());
				builder.append(" and removed form the blacklist");
			}

			builder.append(" successfully.");

			ctx.getSource().getMinecraftServer().getCommandManager().sendCommandTree(player);

			log.sendInfo(ctx.getSource(), builder.toString());
		}

		return 1;
	}

	private static int executeRemove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

		if(!config.get().playerWhitelist.contains(player.getEntityName())) {
			log.sendInfo(ctx.getSource(), "Player: {} newer was whitelisted.", player.getEntityName());
		} else {
			config.get().playerWhitelist.remove(player.getEntityName());
			config.save();

			ctx.getSource().getMinecraftServer().getCommandManager().sendCommandTree(player);

			log.sendInfo(ctx.getSource(), "Player: {} removed from the whitelist successfully.", player.getEntityName());
		}

		return 1;
	}
}
