/*
 * A simple backup mod for Fabric
 * Copyright (C) 2020  Szum123321
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.szum123321.textile_backup.commands.restore;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import net.minecraft.text.LiteralText;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.restore.RestoreHelper;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RestoreBackupCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return CommandManager.literal("restore")
                .then(CommandManager.argument("file", StringArgumentType.word())
                            .suggests(new FileSuggestionProvider())
                        .executes(ctx -> execute(
                                StringArgumentType.getString(ctx, "file"),
                                null,
                                ctx.getSource()
                        ))
                ).then(CommandManager.argument("file", StringArgumentType.word())
                        .suggests(new FileSuggestionProvider())
                        .then(CommandManager.argument("comment", StringArgumentType.word())
                                .executes(ctx -> execute(
                                        StringArgumentType.getString(ctx, "file"),
                                        StringArgumentType.getString(ctx, "comment"),
                                        ctx.getSource()
                                        ))
                        )
                ).executes(context -> {
                    ServerCommandSource source = context.getSource();

                    source.sendFeedback(new LiteralText("To restore given backup you have to provide exact creation time in format:"), false);
                    source.sendFeedback(new LiteralText("[YEAR]-[MONTH]-[DAY]_[HOUR].[MINUTE].[SECOND]"), false);
                    source.sendFeedback(new LiteralText("Example: 2020-08-05_10.58.33"), false);

                    return 1;
                });
    }

    private static int execute(String file, String comment, ServerCommandSource source) throws CommandSyntaxException {
        LocalDateTime dateTime;

        try {
            dateTime = LocalDateTime.from(Statics.defaultDateTimeFormatter.parse(file));
        } catch (DateTimeParseException e) {
            LiteralText message = new LiteralText("An exception occurred while trying to parse:\n");
            message.append(e.getParsedString())
                    .append("\n");

            for(int i = 0; i < e.getErrorIndex(); i++)
                message.append(" ");

            message.append("^");

            throw new CommandSyntaxException(new SimpleCommandExceptionType(message), message);
        }

        Optional<File> backupFile = RestoreHelper.findFileAndLockIfPresent(dateTime, source.getMinecraftServer());

        if(backupFile.isPresent()) {
            Statics.LOGGER.info("Found file to restore {}", backupFile.get().getName());
        } else {
            Statics.LOGGER.sendInfo(source, "No file created on {} was found!", dateTime.format(Statics.defaultDateTimeFormatter));

            return 0;
        }

        if(Statics.restoreAwaitThread == null || !Statics.restoreAwaitThread.isAlive()) {
            if(source.getEntity() != null)
                Statics.LOGGER.info("Backup restoration was initiated by: {}", source.getName());
            else
                Statics.LOGGER.info("Backup restoration was initiated form Server Console");

            Statics.restoreAwaitThread = RestoreHelper.create(backupFile.get(), source.getMinecraftServer(), comment);

            Statics.restoreAwaitThread.start();
        } else if(Statics.restoreAwaitThread != null && Statics.restoreAwaitThread.isAlive()) {
            source.sendFeedback(new LiteralText("Someone has already started another restoration."), false);
            return 0;
        }

        return 1;
    }

    private static final class FileSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
            String remaining = builder.getRemaining();

            for(RestoreHelper.RestoreableFile file : RestoreHelper.getAvailableBackups(ctx.getSource().getMinecraftServer())) {
                String formattedCreationTime = file.getCreationTime().format(Statics.defaultDateTimeFormatter);

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
