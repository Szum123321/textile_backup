package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.cottonmc.cotton.config.ConfigManager;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;

import java.util.Iterator;

public class BlacklistCommand {
	public static LiteralArgumentBuilder<ServerCommandSource> register(){
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

	private static int help(ServerCommandSource source){
		source.sendFeedback(new TranslatableText("command.blacklist.help"), false);

		return 1;
	}

	private static int executeList(ServerCommandSource source){
		StringBuilder builder = new StringBuilder();

		Iterator<String> iterator = TextileBackup.config.playerBlacklist.iterator();

		while (iterator.hasNext()) {
			builder.append(iterator.next());

			if(iterator.hasNext())
				builder.append(", ");
		}

		Utilities.log(source, "command.blacklist.list", builder.toString());

		return 1;
	}

	private static int executeAdd(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

		if(TextileBackup.config.playerBlacklist.contains(player.getEntityName())) {
			ctx.getSource().sendFeedback(new TranslatableText("command.blacklist.add.already", player.getEntityName()), false);
		}else{
			TextileBackup.config.playerBlacklist.add(player.getEntityName());
			ConfigManager.saveConfig(TextileBackup.config);

			if(TextileBackup.config.playerWhitelist.contains(player.getEntityName())){
				TextileBackup.config.playerWhitelist.remove(player.getEntityName());
				Utilities.log(ctx.getSource(), "command.blacklist.add.success_and_whitelist_removed", player.getName());
			} else {
				Utilities.log(ctx.getSource(), "command.blacklist.add.success", player.getName());
			}

			builder.append(" successfully.");

			ctx.getSource().getMinecraftServer().getCommandManager().sendCommandTree(player);

			Utilities.log(builder.toString(), ctx.getSource());
		}

		return 1;
	}

	private static int executeRemove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

		if(!TextileBackup.config.playerBlacklist.contains(player.getEntityName())) {
			ctx.getSource().sendFeedback(new TranslatableText("command.blacklist.remove.already", player.getEntityName()), false);
		}else{
			TextileBackup.config.playerBlacklist.remove(player.getEntityName());
			ConfigManager.saveConfig(TextileBackup.config);

			StringBuilder builder = new StringBuilder();

			builder.append("Player: ");
			builder.append(player.getEntityName());
			builder.append(" removed from the blacklist successfully.");

			ctx.getSource().getMinecraftServer().getCommandManager().sendCommandTree(player);

			Utilities.log(builder.toString(), ctx.getSource());
		}

		return 1;
	}
}
