/*
 * A simple backup mod for Fabric
 * Copyright (C)  2022   Szum123321
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

package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.core.RestoreableFile;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.restore.RestoreHelper;

import java.util.concurrent.CompletableFuture;

public final class FileSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    private static final FileSuggestionProvider INSTANCE = new FileSuggestionProvider();

    public static FileSuggestionProvider Instance() { return INSTANCE; }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();

        var files = RestoreHelper.getAvailableBackups(ctx.getSource().getServer());

        for (RestoreableFile file: files) {
            String formattedCreationTime = file.getCreationTime().format(Globals.defaultDateTimeFormatter);

            if (formattedCreationTime.startsWith(remaining)) {
                if (Utilities.wasSentByPlayer(ctx.getSource())) {  //was typed by player
                    if (file.getComment().isPresent()) {
                        builder.suggest(formattedCreationTime, new LiteralMessage("Comment: " + file.getComment().get()));
                    } else {
                        builder.suggest(formattedCreationTime);
                    }
                } else {  //was typed from server console
                    if (file.getComment().isPresent()) {
                        builder.suggest(file.getCreationTime() + "#" + file.getComment().get());
                    } else {
                        builder.suggest(formattedCreationTime);
                    }
                }
            }
        }

        if("latest".startsWith(remaining) && !files.isEmpty()) //suggest latest
            builder.suggest("latest", new LiteralMessage (
                    files.getLast().getCreationTime().format(Globals.defaultDateTimeFormatter) +
                            (files.getLast().getComment().map(s -> "#" + s).orElse("")))
            );

        return builder.buildFuture();
    }
}
