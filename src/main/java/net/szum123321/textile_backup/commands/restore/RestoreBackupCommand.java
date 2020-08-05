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
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import net.minecraft.text.LiteralText;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.restore.AwaitThread;
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
            Statics.LOGGER.info("Backup restoration was initiated by: {}", ctx.getSource().getName());
        else
            Statics.LOGGER.info("Backup restoration was initiated form Server Console");

        if(Statics.restoreAwaitThread == null || !Statics.restoreAwaitThread.isAlive()) {
            Statics.restoreAwaitThread = new AwaitThread(
                    Statics.CONFIG.restoreDelay,
                    RestoreHelper.create(dateTime, ctx.getSource().getMinecraftServer(), null)
            );

            Statics.restoreAwaitThread.start();
        } else if(Statics.restoreAwaitThread != null && Statics.restoreAwaitThread.isAlive()) {
            ctx.getSource().sendFeedback(new LiteralText("Someone has already started another restoration."), false);
        }

        return 1;
    }

    private static int executeWithCommand(CommandContext<ServerCommandSource> ctx) {
        String file = StringArgumentType.getString(ctx, "file");
        String comment = StringArgumentType.getString(ctx, "comment");

        LocalDateTime dateTime = LocalDateTime.from(dateTimeFormatter.parse(file));

        if(ctx.getSource().getEntity() != null)
            Statics.LOGGER.info("Backup restoration was initiated by: {}", ctx.getSource().getName());
        else
            Statics.LOGGER.info("Backup restoration was initiated form Server Console");

        if(Statics.restoreAwaitThread == null || !Statics.restoreAwaitThread.isAlive()) {
            Statics.restoreAwaitThread = new AwaitThread(
                    Statics.CONFIG.restoreDelay,
                    RestoreHelper.create(dateTime, ctx.getSource().getMinecraftServer(), comment)
            );

            Statics.restoreAwaitThread.start();
        } else if(Statics.restoreAwaitThread != null && Statics.restoreAwaitThread.isAlive()) {
            ctx.getSource().sendFeedback(new LiteralText("Someone has already started another restoration."), false);
        }

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
