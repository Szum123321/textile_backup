package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.szum123321.textile_backup.TextileBackup;

public class IntermediateRestoreCommand {
	public static LiteralArgumentBuilder<ServerCommandSource> register() {
		return CommandManager.literal("restore_internal")
				.then(CommandManager.argument("token", IntegerArgumentType.integer())
						.then(CommandManager.argument("operation", IntegerArgumentType.integer())
								.executes(IntermediateRestoreCommand::execute)
						)
				).executes(IntermediateRestoreCommand::help);
	}

	public static int execute(CommandContext<ServerCommandSource> ctx) {
		int token = IntegerArgumentType.getInteger(ctx, "token");
		int operation = IntegerArgumentType.getInteger(ctx, "operation");

		if(operation == 1) {
			TextileBackup.restoreScheduler.confirm(token);
			ctx.getSource().getMinecraftServer().sendMessage(new LiteralText("Server is going to restart and load backup in: " + TextileBackup.restoreScheduler.getData(token)));

		} else if (operation == -1) {
			TextileBackup.restoreScheduler.cancel(token);
		}

		return 1;
	}

	public static int help(CommandContext<ServerCommandSource> ctx) {
		ctx.getSource().sendFeedback(new TranslatableText("command.intermediate_restore.help"), false);

		return 1;
	}
}
