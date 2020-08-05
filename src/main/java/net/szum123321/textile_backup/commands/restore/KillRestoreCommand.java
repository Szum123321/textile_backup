package net.szum123321.textile_backup.commands.restore;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.szum123321.textile_backup.Statics;

public class KillRestoreCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return CommandManager.literal("kill_r")  //TODO: come up with something better
                .executes(ctx -> {
                    if(Statics.restoreAwaitThread != null && Statics.restoreAwaitThread.isAlive()) {
                        Statics.restoreAwaitThread.interrupt();
                        ctx.getSource().sendFeedback(new LiteralText("Backup restoration successfully stopped"), false);
                    } else {
                        ctx.getSource().sendFeedback(new LiteralText("Failed to stop backup restoration"), false);
                    }
                    return 1;
                });
    }
}
