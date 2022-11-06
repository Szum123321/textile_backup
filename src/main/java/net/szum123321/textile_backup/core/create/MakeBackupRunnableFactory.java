/*
 *  A simple backup mod for Fabric
 *  Copyright (C) 2022  Szum123321
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package net.szum123321.textile_backup.core.create;

import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.core.Utilities;

import java.time.LocalDateTime;

public class MakeBackupRunnableFactory {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    private final static ConfigHelper config = ConfigHelper.INSTANCE;
    
    public static Runnable create(BackupContext ctx) {
        if(config.get().broadcastBackupStart) {
            Utilities.notifyPlayers(ctx.server(),
                    "Warning! Server backup will begin shortly. You may experience some lag."
            );
        } else {
            log.sendInfoAL(ctx, "Warning! Server backup will begin shortly. You may experience some lag.");
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Backup started ");

        builder.append(ctx.initiator().getPrefix());

        if(ctx.startedByPlayer())
            builder.append(ctx.commandSource().getDisplayName().getString());
        else
            builder.append(ctx.initiator().getName());

        builder.append(" on: ");
        builder.append(Utilities.getDateTimeFormatter().format(LocalDateTime.now()));

        log.info(builder.toString());

        if (ctx.shouldSave()) {
            log.sendInfoAL(ctx, "Saving server...");

            ctx.server().getPlayerManager().saveAllPlayerData();

            try {
                ctx.server().save(false, true, true);
            } catch (Exception e) {
                log.sendErrorAL(ctx,"An exception occurred when trying to save the world!");
            }
        }

        return new MakeBackupRunnable(ctx);
    }
}
