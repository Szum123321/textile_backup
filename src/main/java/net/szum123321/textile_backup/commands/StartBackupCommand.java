package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.core.BackupHelper;

public class StartBackupCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register(){
        return CommandManager.literal("start")
                .requires(ctx -> ctx.hasPermissionLevel(4))
                .executes(ctx -> execute(ctx.getSource()));
    }

    private static int execute(ServerCommandSource source){
        BackupHelper.create(source.getMinecraftServer(), source, true);

        return 1;
    }
}
