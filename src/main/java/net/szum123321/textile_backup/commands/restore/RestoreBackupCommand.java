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
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.restore.RestoreContext;
import net.szum123321.textile_backup.core.restore.RestoreHelper;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RestoreBackupCommand {
    private final static DynamicCommandExceptionType DATE_TIME_PARSE_COMMAND_EXCEPTION_TYPE = new DynamicCommandExceptionType(o -> {
        DateTimeParseException e = (DateTimeParseException)o;

        MutableText message = new LiteralText("An exception occurred while trying to parse:\n")
                .append(e.getParsedString())
                .append("\n");

        for (int i = 0; i < e.getErrorIndex(); i++) message.append(" ");

        return message.append("^");
    });

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

                    Statics.LOGGER.sendInfo(source, "To restore given backup you have to provide exact creation time in format:");
                    Statics.LOGGER.sendInfo(source, "[YEAR]-[MONTH]-[DAY]_[HOUR].[MINUTE].[SECOND]");
                    Statics.LOGGER.sendInfo(source, "Example: /backup restore 2020-08-05_10.58.33");

                    return 1;
                });
    }

    private static int execute(String file, @Nullable String comment, ServerCommandSource source) throws CommandSyntaxException {
        if(Statics.restoreAwaitThread == null || (Statics.restoreAwaitThread != null && !Statics.restoreAwaitThread.isAlive())) {
            LocalDateTime dateTime;

            try {
                dateTime = LocalDateTime.from(Statics.defaultDateTimeFormatter.parse(file));
            } catch (DateTimeParseException e) {
                throw DATE_TIME_PARSE_COMMAND_EXCEPTION_TYPE.create(e);
            }

            Optional<RestoreHelper.RestoreableFile> backupFile = RestoreHelper.findFileAndLockIfPresent(dateTime, source.getMinecraftServer());

            if(backupFile.isPresent()) {
                Statics.LOGGER.info("Found file to restore {}", backupFile.get().getFile().getName());
            } else {
                Statics.LOGGER.sendInfo(source, "No file created on {} was found!", dateTime.format(Statics.defaultDateTimeFormatter));

                return 0;
            }

            Statics.restoreAwaitThread = RestoreHelper.create(
                    RestoreContext.Builder.newRestoreContextBuilder()
                        .setCommandSource(source)
                        .setFile(backupFile.get())
                        .setComment(comment)
                        .build()
            );

            Statics.restoreAwaitThread.start();

            return 1;
        } else {
            Statics.LOGGER.sendInfo(source, "Someone has already started another restoration.");

            return 0;
        }
    }

    private static final class FileSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
            String remaining = builder.getRemaining();

            for(RestoreHelper.RestoreableFile file : RestoreHelper.getAvailableBackups(ctx.getSource().getMinecraftServer())) {
                String formattedCreationTime = file.getCreationTime().format(Statics.defaultDateTimeFormatter);

                if(formattedCreationTime.startsWith(remaining)) {
                    if(ctx.getSource().getEntity() instanceof PlayerEntity) {  //was typed by player
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
