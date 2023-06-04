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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.restore.AwaitThread;

public class KillRestoreCommand {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return CommandManager.literal("killR")
                .executes(ctx -> {
                    if(Globals.INSTANCE.getAwaitThread().filter(Thread::isAlive).isEmpty()) {
                        log.sendInfo(ctx.getSource(), "Failed to stop backup restoration");
                        return -1;
                    }

                    AwaitThread thread = Globals.INSTANCE.getAwaitThread().get();

                    thread.interrupt();
                    Globals.INSTANCE.globalShutdownBackupFlag.set(true);
                    Globals.INSTANCE.setLockedFile(null);

                    log.info("{} cancelled backup restoration.", Utilities.wasSentByPlayer(ctx.getSource()) ?
                            "Player: " + ctx.getSource().getName() :
                            "SERVER"
                    );

                    if(Utilities.wasSentByPlayer(ctx.getSource()))
                        log.sendInfo(ctx.getSource(), "Backup restoration successfully stopped.");

                    return 1;
                });
    }
}
