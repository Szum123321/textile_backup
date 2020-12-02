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

package net.szum123321.textile_backup.core.create;

import net.minecraft.server.MinecraftServer;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.ActionInitiator;

import java.time.Instant;

public class BackupScheduler {
    private boolean scheduled;
    private long nextBackup;

    public BackupScheduler() {
        scheduled = false;
        nextBackup = -1;
    }

    public void tick(MinecraftServer server) {
        long now = Instant.now().getEpochSecond();

        if(Statics.CONFIG.doBackupsOnEmptyServer || server.getPlayerManager().getCurrentPlayerCount() > 0) {
            if(scheduled) {
                if(nextBackup <= now) {
                    Statics.executorService.submit(
                            BackupHelper.create(
                                    new BackupContext.Builder()
                                            .setServer(server)
                                            .setInitiator(ActionInitiator.Timer)
                                            .saveServer()
                                            .build()
                            )
                    );

                    nextBackup = now + Statics.CONFIG.backupInterval;
                }
            } else {
                nextBackup = now + Statics.CONFIG.backupInterval;
                scheduled = true;
            }
        } else if(!Statics.CONFIG.doBackupsOnEmptyServer && server.getPlayerManager().getCurrentPlayerCount() == 0) {
            if(scheduled && nextBackup <= now) {
                Statics.executorService.submit(
                        BackupHelper.create(
                                new BackupContext.Builder()
                                        .setServer(server)
                                        .setInitiator(ActionInitiator.Timer)
                                        .saveServer()
                                        .build()
                        )
                );

                scheduled = false;
            }
        }
    }
}