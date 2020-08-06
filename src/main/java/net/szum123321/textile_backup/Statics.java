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

package net.szum123321.textile_backup;

import net.szum123321.textile_backup.core.CustomLogger;
import net.szum123321.textile_backup.core.create.BackupScheduler;
import net.szum123321.textile_backup.core.restore.AwaitThread;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Statics {
    public static final String MOD_ID = "textile_backup";
    public static final String MOD_NAME = "Textile Backup";
    public static final CustomLogger LOGGER = new CustomLogger(MOD_ID, MOD_NAME);
    public static ConfigHandler CONFIG;

    public static final BackupScheduler scheduler = new BackupScheduler();
    public static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public final static DateTimeFormatter defaultDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    public static final AtomicBoolean globalShutdownBackupFlag = new AtomicBoolean(true);
    public static AwaitThread restoreAwaitThread;
    public static File untouchableFile;
}
