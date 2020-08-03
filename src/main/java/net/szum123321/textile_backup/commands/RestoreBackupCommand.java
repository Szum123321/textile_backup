package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.restore.RestoreHelper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class RestoreBackupCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return CommandManager.literal("restore")
                .then(CommandManager.argument("file", StringArgumentType.greedyString())
                            .suggests(new FileSuggestionProvider())
                        .executes(RestoreBackupCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        String arg = StringArgumentType.getString(ctx, "file");
        LocalDateTime dateTime = LocalDateTime.from(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").parse(arg));

        if(ctx.getSource().getEntity() != null)
            TextileBackup.LOGGER.info("Backup restoration was initiated by: {}", ctx.getSource().getName());
        else
            TextileBackup.LOGGER.info("Backup restoration was initiated form Server Console");

        new Thread(RestoreHelper.create(dateTime, ctx.getSource().getMinecraftServer())).start();

        return 1;
    }

    private static final class FileSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
            String remaining = builder.getRemaining();

            for(RestoreHelper.RestoreableFile file : RestoreHelper.getAvailableBackups(ctx.getSource().getMinecraftServer())) {
                String formattedCreationTime = file.getCreationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                if(formattedCreationTime.startsWith(remaining)) {
                    if(ctx.getSource().getEntity() != null) {  //was typed by player
                        if(file.getComment() != null) {
                            builder.suggest(formattedCreationTime, new LiteralMessage("Comment: " + file.getComment()));
                        } else {
                            builder.suggest(formattedCreationTime);
                        }
                    } else {  //was typed from server console
                        if(file.getComment() != null) {
                            builder.suggest(file.getCreationTime() + "#" + file.getComment());
                        } else {
                            builder.suggest(formattedCreationTime);
                        }
                    }
                }
            }
            return builder.buildFuture();
        }
    }
}
