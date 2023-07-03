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

package net.szum123321.textile_backup.commands.restore;

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
import net.szum123321.textile_backup.core.restore.RestoreContext;
import net.szum123321.textile_backup.core.restore.RestoreHelper;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;

public class RestoreBackupCommand {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return CommandManager.literal("restore")
                .then(CommandManager.argument("file", StringArgumentType.word())
                            .suggests(FileSuggestionProvider.Instance())
                        .executes(ctx -> execute(
                                StringArgumentType.getString(ctx, "file"),
                                null,
                                ctx.getSource()
                        ))
                ).then(CommandManager.argument("file", StringArgumentType.word())
                        .suggests(FileSuggestionProvider.Instance())
                        .then(CommandManager.argument("comment", StringArgumentType.word())
                                .executes(ctx -> execute(
                                        StringArgumentType.getString(ctx, "file"),
                                        StringArgumentType.getString(ctx, "comment"),
                                        ctx.getSource()
                                        ))
                        )
                ).executes(context -> {
                    ServerCommandSource source = context.getSource();

                    log.sendInfo(source, "要恢复给定的备份，您必须以以下格式提供准确的创建时间：");
                    log.sendInfo(source, "[年]-[月]-[日]_[小时].[分钟].[秒]");
                    log.sendInfo(source, "示例：/backup restore 2020-08-05_10.58.33");
                    log.sendInfo(source, "您还可以输入 '/backup restore latest' 来恢复最新的备份。");

                    return 1;
                });
    }

    private static int execute(String file, @Nullable String comment, ServerCommandSource source) throws CommandSyntaxException {
        if(Globals.INSTANCE.getAwaitThread().filter(Thread::isAlive).isPresent()) {
            log.sendInfo(source, "已经有其他人开始了另一个恢复操作。");

            return -1;
        }

        LocalDateTime dateTime;
        Optional<RestoreableFile> backupFile;

        if(Objects.equals(file, "latest")) {
            backupFile = RestoreHelper.getLatestAndLockIfPresent(source.getServer());
            dateTime = backupFile.map(RestoreableFile::getCreationTime).orElse(LocalDateTime.now());
        } else {
            try {
                dateTime = LocalDateTime.from(Globals.defaultDateTimeFormatter.parse(file));
            } catch (DateTimeParseException e) {
                throw CommandExceptions.DATE_TIME_PARSE_COMMAND_EXCEPTION_TYPE.create(e);
            }

            backupFile = RestoreHelper.findFileAndLockIfPresent(dateTime, source.getServer());
        }

        if(backupFile.isEmpty()) {
            log.sendInfo(source, "在{}上没有找到创建的文件！", dateTime.format(Globals.defaultDateTimeFormatter));
            return -1;
        } else {
            log.info("找到要恢复的文件：{}", backupFile.get().getFile().getFileName().toString());

            Globals.INSTANCE.setAwaitThread(
                    RestoreHelper.create(
                            RestoreContext.Builder.newRestoreContextBuilder()
                                    .setCommandSource(source)
                                    .setFile(backupFile.get())
                                    .setComment(comment)
                                    .build()
                    )
            );

            Globals.INSTANCE.getAwaitThread().get().start();

            return 1;
        }
    }
}
