package net.szum123321.textile_backup.commands.permission;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.cottonmc.cotton.config.ConfigManager;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.szum123321.textile_backup.Statics;

public class WhitelistCommand {
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
		source.sendFeedback(new LiteralText("Available command are: add [player], remove [player], list."), false);

		return 1;
	}

	private static int executeList(ServerCommandSource source){
		StringBuilder builder = new StringBuilder();

		builder.append("Currently on the whitelist are: ");

		for(String name : Statics.CONFIG.playerWhitelist){
			builder.append(name);
			builder.append(", ");
		}

		source.sendFeedback(new LiteralText(builder.toString()), false);

		return 1;
	}

	private static int executeAdd(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

		if(Statics.CONFIG.playerWhitelist.contains(player.getEntityName())) {
			ctx.getSource().sendFeedback(new TranslatableText("Player: %s is already whitelisted.", player.getEntityName()), false);
		}else{
			Statics.CONFIG.playerWhitelist.add(player.getEntityName());
			ConfigManager.saveConfig(Statics.CONFIG);

			StringBuilder builder = new StringBuilder();

			builder.append("Player: ");
			builder.append(player.getEntityName());
			builder.append(" added to the whitelist");

			if(Statics.CONFIG.playerBlacklist.contains(player.getEntityName())){
				Statics.CONFIG.playerBlacklist.remove(player.getEntityName());
				builder.append(" and removed form the blacklist");
			}

			builder.append(" successfully.");

			ctx.getSource().getMinecraftServer().getCommandManager().sendCommandTree(player);

			Statics.LOGGER.sendInfo(ctx.getSource(), builder.toString());
		}

		return 1;
	}

	private static int executeRemove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

		if(!Statics.CONFIG.playerWhitelist.contains(player.getEntityName())) {
			ctx.getSource().sendFeedback(new TranslatableText("Player: %s newer was on the whitelist.", player.getEntityName()), false);
		}else{
			Statics.CONFIG.playerWhitelist.remove(player.getEntityName());
			ConfigManager.saveConfig(Statics.CONFIG);
			StringBuilder builder = new StringBuilder();

			builder.append("Player: ");
			builder.append(player.getEntityName());
			builder.append(" removed from the whitelist successfully.");

			ctx.getSource().getMinecraftServer().getCommandManager().sendCommandTree(player);

			Statics.LOGGER.sendInfo(ctx.getSource(), builder.toString());
		}

		return 1;
	}
}
