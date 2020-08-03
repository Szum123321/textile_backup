package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.szum123321.textile_backup.core.Utilities;

import java.util.Arrays;

public class ListBackupsCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return CommandManager.literal("list")
                .executes(ctx -> {
                    StringBuilder builder = new StringBuilder();

                    builder.append("Available backups: ");

                    Arrays.stream(Utilities.getBackupRootPath(Utilities.getLevelName(ctx.getSource().getMinecraftServer()))
                            .listFiles())
                            .forEach(file -> builder.append(file.getName()).append(", "));

                    ctx.getSource().sendFeedback(new LiteralText(builder.toString()), false);

                    return 1;
                });
    }
}
