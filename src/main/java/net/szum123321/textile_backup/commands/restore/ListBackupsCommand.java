package net.szum123321.textile_backup.commands.restore;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.szum123321.textile_backup.core.Utilities;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

public class ListBackupsCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return CommandManager.literal("list")
                .executes(ctx -> {
                    StringBuilder builder = new StringBuilder();

                    File[] files = Utilities.getBackupRootPath(Utilities.getLevelName(ctx.getSource().getMinecraftServer())).listFiles();

                    if(files.length == 0) {
                        builder.append("There a no backups available for this world.");
                    } else if(files.length == 1) {
                        builder.append("There is only one backup available: ");
                        builder.append(files[0].getName());
                    } else {
                        Iterator<File> iterator = Arrays.stream(files).iterator();
                        builder.append("Available backups: ");

                        builder.append(iterator.next());

                        while(iterator.hasNext()) {
                            builder.append("\n");
                            builder.append(iterator.next());
                        }
                    }

                    ctx.getSource().sendFeedback(new LiteralText(builder.toString()), false);

                    return 1;
                });
    }
}
