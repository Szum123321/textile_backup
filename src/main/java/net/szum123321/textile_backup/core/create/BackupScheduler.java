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

package net.szum123321.textile_backup.core.create;

import net.minecraft.server.MinecraftServer;
import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.core.ActionInitiator;

import java.time.Instant;

/**
 * Runs backup on a preset interval
 * <br><br>
 * The important thing to note: <br>
 * The decision of whether to do a backup or not is made at the time of scheduling, that is, whenever the <code>nextBackup</code>
 * flag is set. This means that even if doBackupsOnEmptyServer=false, the backup that was scheduled with players online will
 * still go through. <br>
 * It might appear as though there has been made a backup with no players online despite the config. This is the expected behaviour
 * <br><br>
 * Furthermore, it uses system time
 */
public class BackupScheduler {
    private final static ConfigHelper config = ConfigHelper.INSTANCE;

    //Scheduled flag tells whether we have decided to run another backup
    private static boolean scheduled = false;
    private static long nextBackup = - 1;

    public static void tick(MinecraftServer server) {
        if(config.get().backupInterval < 1) return;
        long now = Instant.now().getEpochSecond();

        if(config.get().doBackupsOnEmptyServer || server.getPlayerManager().getCurrentPlayerCount() > 0) {
            //Either just run backup with no one playing or there's at least one player
            if(scheduled) {
                if(nextBackup <= now) {
                    //It's time to run
                    Globals.INSTANCE.getQueueExecutor().submit(
                            ExecutableBackup.Builder
                                    .newBackupContextBuilder()
                                    .setServer(server)
                                    .setInitiator(ActionInitiator.Timer)
                                    .saveServer()
                                    .announce()
                                    .build()
                    );

                    nextBackup = now + config.get().backupInterval;
                }
            } else {
                //Either server just started or a new player joined after the last backup has finished
                //So let's schedule one some time from now
                nextBackup = now + config.get().backupInterval;
                scheduled = true;
            }
        } else if(!config.get().doBackupsOnEmptyServer && server.getPlayerManager().getCurrentPlayerCount() == 0) {
            //Do the final backup. No one's on-line and doBackupsOnEmptyServer == false
            if(scheduled && nextBackup <= now) {
                //Verify we hadn't done the final one, and it's time to do so
                Globals.INSTANCE.getQueueExecutor().submit(
                        ExecutableBackup.Builder
                                .newBackupContextBuilder()
                                .setServer(server)
                                .setInitiator(ActionInitiator.Timer)
                                .saveServer()
                                .announce()
                                .build()
                );

                scheduled = false;
            }
        }
    }
}
