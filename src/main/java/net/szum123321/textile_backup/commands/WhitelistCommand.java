package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.cottonmc.cotton.config.ConfigManager;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;

public class WhitelistCommand {
	public static LiteralArgumentBuilder<ServerCommandSource> register(){
		return CommandManager.literal("whitelist")
				.requires(ctx -> TextileBackup.config.whitelist.contains(ctx.getName()) ||
						ctx.hasPermissionLevel(TextileBackup.config.permissionLevel) &&
								!TextileBackup.config.blacklist.contains(ctx.getName()))
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
		source.sendFeedback(new TranslatableText("Available command are: add [player], remove [player], list."), false);

		return 1;
	}

	private static int executeList(ServerCommandSource source){
		StringBuilder builder = new StringBuilder();

		builder.append("Currently on the whitelist are: ");

		for(String name : TextileBackup.config.whitelist){
			builder.append(name);
			builder.append(", ");
		}

		Utilities.log(builder.toString(), source);

		return 1;
	}

	private static int executeAdd(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		PlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

		if(TextileBackup.config.whitelist.contains(player.getEntityName())) {
			ctx.getSource().sendFeedback(new TranslatableText("Player: {} is already whitelisted.", player.getEntityName()), false);
		}else{
			TextileBackup.config.whitelist.add(player.getEntityName());
			ConfigManager.saveConfig(TextileBackup.config);

			StringBuilder builder = new StringBuilder();

			builder.append("Player: ");
			builder.append(player.getEntityName());
			builder.append(" added to the whitelist");

			if(TextileBackup.config.blacklist.contains(player.getEntityName())){
				TextileBackup.config.blacklist.remove(player.getEntityName());
				builder.append(" and removed form the blacklist");
			}

			builder.append(" successfully.");

			Utilities.log(builder.toString(), ctx.getSource());
		}

		return 1;
	}

	private static int executeRemove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		PlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

		if(!TextileBackup.config.whitelist.contains(player.getEntityName())) {
			ctx.getSource().sendFeedback(new TranslatableText("Player: {} newer was on the whitelist.", player.getEntityName()), false);
		}else{
			TextileBackup.config.whitelist.remove(player.getEntityName());
			ConfigManager.saveConfig(TextileBackup.config);
			StringBuilder builder = new StringBuilder();

			builder.append("Player: ");
			builder.append(player.getEntityName());
			builder.append(" removed from the whitelist successfully.");

			Utilities.log(builder.toString(), ctx.getSource());
		}

		return 1;
	}
}
