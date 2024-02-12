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
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.core.RestoreableFile;
import net.szum123321.textile_backup.core.Utilities;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class RestoreHelper {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

    public static LinkedList<RestoreableFile> getAvailableBackups(MinecraftServer server) {
        Path root = Utilities.getBackupRootPath(Utilities.getLevelName(server));

        return RestoreableFile.applyOnFiles(root, new LinkedList<>(),
                e -> log.error("Error while listing available backups", e),
                s -> s.sorted().collect(Collectors.toCollection(LinkedList::new)));
    }
}