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

package net.szum123321.textile_backup.core.restore;

import net.minecraft.server.MinecraftServer;
import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.core.ActionInitiator;
import net.szum123321.textile_backup.core.RestoreableFile;
import net.szum123321.textile_backup.core.Utilities;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class RestoreHelper {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    private final static ConfigHelper config = ConfigHelper.INSTANCE;

    public static Optional<RestoreableFile> findFileAndLockIfPresent(LocalDateTime backupTime, MinecraftServer server) {
        Path root = Utilities.getBackupRootPath(Utilities.getLevelName(server));

        Optional<RestoreableFile> optionalFile =
                RestoreableFile.applyOnFiles(root, Optional.empty(),
                        e -> log.error("An exception occurred while trying to lock the file!", e),
                        s -> s.filter(rf -> rf.getCreationTime().equals(backupTime))
                                .findFirst());

        optionalFile.ifPresent(r -> Globals.INSTANCE.setLockedFile(r.getFile()));

        return optionalFile;
    }

    public static Optional<RestoreableFile> getLatestAndLockIfPresent( MinecraftServer server) {
        var available = RestoreHelper.getAvailableBackups(server);

        if(available.isEmpty()) return Optional.empty();
        else {
            var latest = available.getLast();
            Globals.INSTANCE.setLockedFile(latest.getFile());
            return Optional.of(latest);
        }
    }

    public static AwaitThread create(RestoreContext ctx) {
        if(ctx.initiator() == ActionInitiator.Player)
            log.info("Backup restoration was initiated by: {}", ctx.commandSource().getName());
        else
            log.info("Backup restoration was initiated form Server Console");

        Utilities.notifyPlayers(
                ctx.server(),
                "Warning! The server is going to shut down in " + config.get().restoreDelay + " seconds!"
        );

        return new AwaitThread(
                config.get().restoreDelay,
                new RestoreBackupRunnable(ctx)
        );
    }

    public static LinkedList<RestoreableFile> getAvailableBackups(MinecraftServer server) {
        Path root = Utilities.getBackupRootPath(Utilities.getLevelName(server));

        return RestoreableFile.applyOnFiles(root, new LinkedList<>(),
                e -> log.error("Error while listing available backups", e),
                s -> s.sorted().collect(Collectors.toCollection(LinkedList::new)));
    }
}