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
    private final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return CommandManager.literal("restore")
                .then(CommandManager.argument("file", StringArgumentType.word())
                            .suggests(new FileSuggestionProvider())
                            .executes(RestoreBackupCommand::execute)
                ).then(CommandManager.argument("file", StringArgumentType.word())
                        .suggests(new FileSuggestionProvider())
                        .then(CommandManager.argument("comment", StringArgumentType.word())
                                .executes(RestoreBackupCommand::executeWithCommand)
                        )
                );
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        String file = StringArgumentType.getString(ctx, "file");
        LocalDateTime dateTime = LocalDateTime.from(dateTimeFormatter.parse(file));

        if(ctx.getSource().getEntity() != null)
            TextileBackup.LOGGER.info("Backup restoration was initiated by: {}", ctx.getSource().getName());
        else
            TextileBackup.LOGGER.info("Backup restoration was initiated form Server Console");

        RestoreHelper.create(dateTime, ctx.getSource().getMinecraftServer(), null).start();

        return 1;
    }

    private static int executeWithCommand(CommandContext<ServerCommandSource> ctx) {
        String file = StringArgumentType.getString(ctx, "file");
        String comment = StringArgumentType.getString(ctx, "comment");

        LocalDateTime dateTime = LocalDateTime.from(dateTimeFormatter.parse(file));

        if(ctx.getSource().getEntity() != null)
            TextileBackup.LOGGER.info("Backup restoration was initiated by: {}", ctx.getSource().getName());
        else
            TextileBackup.LOGGER.info("Backup restoration was initiated form Server Console");

        RestoreHelper.create(dateTime, ctx.getSource().getMinecraftServer(), comment).start();

        return 1;
    }

    private static final class FileSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
            String remaining = builder.getRemaining();

            for(RestoreHelper.RestoreableFile file : RestoreHelper.getAvailableBackups(ctx.getSource().getMinecraftServer())) {
                String formattedCreationTime = file.getCreationTime().format(dateTimeFormatter);

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
