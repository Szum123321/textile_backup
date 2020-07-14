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

package net.szum123321.textile_backup.core;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.TextileBackup;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

public class BackupHelper {
	public static Runnable create(MinecraftServer server, ServerCommandSource ctx, boolean save, String comment) {
		LocalDateTime now = LocalDateTime.now();

		StringBuilder builder = new StringBuilder();
		builder.append("Backup started by: ");

		if (ctx != null)
			builder.append(ctx.getName());
		else
			builder.append("SERVER");

		builder.append(" on: ");
		builder.append(Utilities.getDateTimeFormatter().format(now));

		Utilities.info(builder.toString(), null);

		Utilities.info("Saving server...", ctx);

		if (save)
			server.save(true, true, false);

		return new MakeBackupRunnable(server, ctx, comment);
	}

	public static void executeFileLimit(ServerCommandSource ctx, String worldName) {
		File root = getBackupRootPath(worldName);

		if (root.isDirectory() && root.exists() && root.listFiles() != null) {
			if (TextileBackup.config.maxAge > 0) { // delete files older that configured
				final LocalDateTime now = LocalDateTime.now();

				Arrays.stream(root.listFiles())
						.filter(BackupHelper::isFileOk)
						.filter(f -> Utilities.getFileCreationTime(f).isPresent())  // We check if we can get file's creation date so that the next line won't throw an exception
						.filter(f -> now.toEpochSecond(ZoneOffset.UTC) - Utilities.getFileCreationTime(f).get().toEpochSecond(ZoneOffset.UTC) > TextileBackup.config.maxAge)
						.forEach(f -> {
							if(f.delete())
								Utilities.info("Deleting: " + f.getName(), ctx);
							else
								Utilities.sendError("Something went wrong while deleting: " + f.getName(), ctx);
						});
			}

			if (TextileBackup.config.backupsToKeep > 0 && root.listFiles().length > TextileBackup.config.backupsToKeep) {
				AtomicInteger i = new AtomicInteger(root.listFiles().length);

				Arrays.stream(root.listFiles())
						.filter(BackupHelper::isFileOk)
						.filter(f -> Utilities.getFileCreationTime(f).isPresent())
						.sorted(Comparator.comparing(f -> Utilities.getFileCreationTime(f).get()))
						.takeWhile(f -> i.get() > TextileBackup.config.backupsToKeep)
						.forEach(f -> {
							if(f.delete())
								Utilities.info("Deleting: " + f.getName(), ctx);
							else
								Utilities.sendError("Something went wrong while deleting: " + f.getName(), ctx);

							i.getAndDecrement();
						});
			}

			if (TextileBackup.config.maxSize > 0 && FileUtils.sizeOfDirectory(root) / 1024 > TextileBackup.config.maxSize) {
				Arrays.stream(root.listFiles())
						.filter(BackupHelper::isFileOk)
						.filter(f -> Utilities.getFileCreationTime(f).isPresent())
						.sorted(Comparator.comparing(f -> Utilities.getFileCreationTime(f).get()))
						.takeWhile(f -> FileUtils.sizeOfDirectory(root) / 1024 > TextileBackup.config.maxSize)
						.forEach(f -> {
							if(f.delete())
								Utilities.info("Deleting: " + f.getName(), ctx);
							else
								Utilities.sendError("Something went wrong while deleting: " + f.getName(), ctx);
						});
			}
		}
	}

	private static boolean isFileOk(File f) {return f.exists() && f.isFile(); }

	public static File getBackupRootPath(String worldName) {
		File path = new File(TextileBackup.config.path).getAbsoluteFile();

		if (TextileBackup.config.perWorldBackup)
			path = path.toPath().resolve(worldName).toFile();

		if (!path.exists()) {
			try {
				path.mkdirs();
			} catch (Exception e) {
				TextileBackup.LOGGER.error("An exception occurred!", e);

				return FabricLoader
						.getInstance()
						.getGameDirectory()
						.toPath()
						.resolve(TextileBackup.config.path)
						.toFile();
			}
		}

		return path;
	}
}