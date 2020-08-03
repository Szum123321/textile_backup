/*
    A simple backup mod for Fabric
    Copyright (C) 2020  Szum123321

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package net.szum123321.textile_backup.core.create;

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.Utilities;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class BackupHelper {
	public static Runnable create(BackupContext ctx) {
		StringBuilder builder = new StringBuilder();

		builder.append("Backup started ");

		builder.append(ctx.getInitiator().getPrefix());

		if(ctx.startedByPlayer()) {
			builder.append(ctx.getCommandSource().getDisplayName().getString());
		} else {
			builder.append(ctx.getInitiator().getName());
		}

		builder.append(" on: ");
		builder.append(Utilities.getDateTimeFormatter().format(LocalDateTime.now()));

		Utilities.info(builder.toString(), null);

		if (ctx.shouldSave()) {
			Utilities.info("Saving server...", ctx.getCommandSource());
			ctx.getServer().save(true, true, false);
		}

		return new MakeBackupRunnable(ctx);
	}

	public static int executeFileLimit(ServerCommandSource ctx, String worldName) {
		File root = Utilities.getBackupRootPath(worldName);
		AtomicInteger deletedFiles = new AtomicInteger();

		if (root.isDirectory() && root.exists() && root.listFiles() != null) {
			if (TextileBackup.CONFIG.maxAge > 0) { // delete files older that configured
				final LocalDateTime now = LocalDateTime.now();

				Arrays.stream(root.listFiles())
						.filter(BackupHelper::isFileOk)
						.filter(f -> Utilities.getFileCreationTime(f).isPresent())  // We check if we can get file's creation date so that the next line won't throw an exception
						.filter(f -> now.toEpochSecond(ZoneOffset.UTC) - Utilities.getFileCreationTime(f).get().toEpochSecond(ZoneOffset.UTC) > TextileBackup.CONFIG.maxAge)
						.forEach(f -> {
							if(f.delete()) {
								Utilities.info("Deleting: " + f.getName(), ctx);
								deletedFiles.getAndIncrement();
							} else {
								Utilities.sendError("Something went wrong while deleting: " + f.getName(), ctx);
							}
						});
			}

			if (TextileBackup.CONFIG.backupsToKeep > 0 && root.listFiles().length > TextileBackup.CONFIG.backupsToKeep) {
				int i = root.listFiles().length;

				Iterator<File> it = Arrays.stream(root.listFiles())
						.filter(BackupHelper::isFileOk)
						.filter(f -> Utilities.getFileCreationTime(f).isPresent())
						.sorted(Comparator.comparing(f -> Utilities.getFileCreationTime(f).get()))
						.iterator();

				while(i > TextileBackup.CONFIG.backupsToKeep && it.hasNext()) {
					File f = it.next();

					if(f.delete()) {
						Utilities.info("Deleting: " + f.getName(), ctx);
						deletedFiles.getAndIncrement();
					} else {
						Utilities.sendError("Something went wrong while deleting: " + f.getName(), ctx);
					}

					i--;
				}
			}

			if (TextileBackup.CONFIG.maxSize > 0 && FileUtils.sizeOfDirectory(root) / 1024 > TextileBackup.CONFIG.maxSize) {
				Iterator<File> it = Arrays.stream(root.listFiles())
						.filter(BackupHelper::isFileOk)
						.filter(f -> Utilities.getFileCreationTime(f).isPresent())
						.sorted(Comparator.comparing(f -> Utilities.getFileCreationTime(f).get()))
						.iterator();

				while(FileUtils.sizeOfDirectory(root) / 1024 > TextileBackup.CONFIG.maxSize && it.hasNext()) {
					File f = it.next();

					if(f.delete()) {
						Utilities.info("Deleting: " + f.getName(), ctx);
						deletedFiles.getAndIncrement();
					} else {
						Utilities.sendError("Something went wrong while deleting: " + f.getName(), ctx);
					}
				}
			}
		}

		return deletedFiles.get();
	}

	private static boolean isFileOk(File f) {return f.exists() && f.isFile(); }
}