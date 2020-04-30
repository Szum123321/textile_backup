package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.arguments.TimeArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.commands.arguments.FileArgumentType;

public class RestoreBackupCommand {
	public static LiteralArgumentBuilder<ServerCommandSource> register() {
		return CommandManager.literal("restore")
				.then(CommandManager.argument("file", FileArgumentType.file())
						.then(CommandManager.argument("delay", TimeArgumentType.time())
								.executes(ctx -> execute(ctx, true))
						).executes(ctx -> execute(ctx, false))
				)
				.executes(RestoreBackupCommand::help);
	}

	public static int execute(CommandContext<ServerCommandSource> ctx, boolean delay) {
		String file = FileArgumentType.getFile(ctx, "file");
		int time;

		if(delay) {
			 time = IntegerArgumentType.getInteger(ctx, "delay");
		} else {
			time = TextileBackup.config.restoreDelay;
		}

		Text text = new LiteralText("Are you sure?\n");

		int token = TextileBackup.restoreScheduler.add(file, time);

		StringBuilder builder = new StringBuilder();

		builder.append("/backup restore_internal ");
		builder.append(token);
		builder.append(" ");

		text.append(Texts.bracketed(new TranslatableText("message.general.yes")).styled(style -> {
			style.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, builder.toString() + "1"));
			style.setColor(Formatting.GREEN);
		})).append(" ").append(Texts.bracketed(new TranslatableText("message.general.no")).styled(style -> {
			style.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, builder.toString() + "-1"));
			style.setColor(Formatting.RED);
		}));

		ctx.getSource().sendFeedback(text, false);

		return 1;
	}

	public static int help(CommandContext<ServerCommandSource> ctx) {
		ctx.getSource().sendFeedback(new TranslatableText("command.restore.help"), false);
		return 1;
	}
}
