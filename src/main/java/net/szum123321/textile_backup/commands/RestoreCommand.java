package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class RestoreCommand {
	public static LiteralArgumentBuilder<ServerCommandSource> register(){
		Collection<String> list = new ArrayList<>();
		list.add("hhhhh");
		list.add("DSFFASFA");
		return CommandManager.literal("restore")
				.then(CommandManager.argument("file", new FileArgumentType(list))
						.executes(RestoreCommand::execute)
				)
				.executes(ctx -> help(ctx.getSource()));
	}

	public static int execute(CommandContext<ServerCommandSource> ctx) {
		ctx.getSource().sendFeedback(new LiteralText(FileArgumentType.getString(ctx, "file")), false);

		return 1;
	}

	public static int help(ServerCommandSource source) {
		source.sendFeedback(new LiteralText("HEJ!"), false);
		return 1;
	}
}
