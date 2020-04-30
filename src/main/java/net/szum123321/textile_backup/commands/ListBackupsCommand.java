package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;
import net.szum123321.textile_backup.core.RestoreHelper;

import java.util.Iterator;

public class ListBackupsCommand {
	public static LiteralArgumentBuilder<ServerCommandSource> register(){
		return CommandManager.literal("list").executes(ListBackupsCommand::execute);
	}

	public static int execute(CommandContext<ServerCommandSource> ctx) {
		StringBuilder builder = new StringBuilder();

		Iterator<String> i = RestoreHelper.getAvailableBackups(ctx.getSource().getWorld().getLevelProperties().getLevelName()).iterator();

		if(i.hasNext()) {
			builder.append(i.next());

			while (i.hasNext()) {
				builder.append(",\n");
				builder.append(i.next());
			}

			ctx.getSource().sendFeedback(new TranslatableText("command.list.success", builder.toString()), false);
		} else {
			ctx.getSource().sendFeedback(new TranslatableText("command.list.none"), false);
		}

		return 1;
	}
}
