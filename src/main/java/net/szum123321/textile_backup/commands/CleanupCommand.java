package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;
import net.szum123321.textile_backup.core.BackupHelper;

public class CleanupCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register(){
        return CommandManager.literal("cleanup")
                .requires(ctx -> ctx.hasPermissionLevel(4))
                .executes(ctx -> execute(ctx.getSource()));
    }

    private static int execute(ServerCommandSource source){
        BackupHelper.executeFileLimit(source);

        return 1;
    }
}
