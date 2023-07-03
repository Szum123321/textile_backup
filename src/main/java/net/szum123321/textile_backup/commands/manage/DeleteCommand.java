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

package net.szum123321.textile_backup.commands.manage;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.commands.CommandExceptions;
import net.szum123321.textile_backup.commands.FileSuggestionProvider;
import net.szum123321.textile_backup.core.RestoreableFile;
import net.szum123321.textile_backup.core.Utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class DeleteCommand {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return CommandManager.literal("delete")
                .then(CommandManager.argument("file", StringArgumentType.word())
                        .suggests(FileSuggestionProvider.Instance())
                        .executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx, "file")))
                );
    }

    private static int execute(ServerCommandSource source, String fileName) throws CommandSyntaxException {
        LocalDateTime dateTime;

        try {
            dateTime = LocalDateTime.from(Globals.defaultDateTimeFormatter.parse(fileName));
        } catch (DateTimeParseException e) {
            throw CommandExceptions.DATE_TIME_PARSE_COMMAND_EXCEPTION_TYPE.create(e);
        }

        Path root = Utilities.getBackupRootPath(Utilities.getLevelName(source.getServer()));

        RestoreableFile.applyOnFiles(root, Optional.empty(),
                e -> log.sendErrorAL(source, "在尝试删除备份文件时发生了异常！", e),
                stream -> stream.filter(f -> f.getCreationTime().equals(dateTime)).map(RestoreableFile::getFile).findFirst()
                ).ifPresentOrElse(file -> {
                    if(Globals.INSTANCE.getLockedFile().filter(p -> p == file).isEmpty()) {
                        try {
                            Files.delete((Path) file);
                            log.sendInfo(source, "备份: {} 被成功删除!", file);

                            if(Utilities.wasSentByPlayer(source))
                                log.info("玩家 {} 删除了备份: {}.", source.getPlayer().getName(), file);
                        } catch (IOException e) {
                            log.sendError(source, "在尝试删除备份文件时发生了异常！");
                        }
                    } else {
                        log.sendError(source, "由于备份正在恢复中，无法删除该文件.");
                        log.sendHint(source, "如果您想中止恢复过程，请使用以下命令：/backup killR");
                    }
                }, () -> {
                    log.sendInfo(source, "根据您提供的文件名找不到相应的文件.");
                    log.sendInfo(source, "也许您可以试试: /backup list");
                }
        );
        return 0;
    }
}
