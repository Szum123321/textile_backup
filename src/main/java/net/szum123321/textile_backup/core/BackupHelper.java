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
import net.szum123321.textile_backup.ConfigHandler;
import net.szum123321.textile_backup.TextileBackup;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Objects;

public class BackupHelper {
	public static Thread create(MinecraftServer server, ServerCommandSource ctx, boolean save, String comment) {
		LocalDateTime now = LocalDateTime.now();

		if (ctx != null)
			Utilities.log(null, "message.creator.start", ctx.getName(), Utilities.getDateTimeFormatter().format(now));
		else
			Utilities.log(null, "message.creator.start", "SERVER", Utilities.getDateTimeFormatter().format(now));

		//Utilities.log("Saving server...", ctx);

		if (save)
			server.save(true, true, false);

		Thread thread = new Thread(new MakeBackupThread(server, ctx, comment));

		thread.start();

		return thread;
	}

	public static void executeFileLimit(ServerCommandSource ctx, String worldName) {
		File root = Utilities.getBackupRootPath(worldName);

		if (root.isDirectory() && root.exists()) {
			if (TextileBackup.config.maxAge > 0) {
				LocalDateTime now = LocalDateTime.now();

				Arrays.stream(root.listFiles()).filter(f -> f.exists() && f.isFile()).forEach(f -> {
					LocalDateTime creationTime;

					try {
						try {
							FileTime fileTime = (FileTime) Files.getAttribute(f.toPath(), "creationTime");

							creationTime = LocalDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.UTC);
						} catch (IOException ignored) {
							try {
								creationTime = LocalDateTime.from(
										Utilities.getDateTimeFormatter().parse(
												f.getName().split(Objects.requireNonNull(Utilities.getFileExtension(f)))[0].replace('#', '+').split("\\+")[0]
										)
								);
							} catch (Exception ignored2) {
								creationTime = LocalDateTime.from(
										Utilities.getBackupDateTimeFormatter().parse(
												f.getName().split(Objects.requireNonNull(Utilities.getFileExtension(f)))[0].replace('#', '+').split("\\+")[0]
										)
								);
							}
						}

						if (now.toEpochSecond(ZoneOffset.UTC) - creationTime.toEpochSecond(ZoneOffset.UTC) > TextileBackup.config.maxAge) {
							Utilities.log(ctx, "message.creator.delete_file", f.getName());
							f.delete();
						}
					} catch (NullPointerException ignored3) {}
				});
			}

			if (TextileBackup.config.backupsToKeep > 0 && root.listFiles().length > TextileBackup.config.backupsToKeep) {
				int var1 = root.listFiles().length - TextileBackup.config.backupsToKeep;

				File[] files = root.listFiles();
				assert files != null;

				Arrays.sort(files);

				for (int i = 0; i < var1; i++) {
					Utilities.log(ctx, "message.creator.delete_file", files[i].getName());
					files[i].delete();
				}
			}

			if (TextileBackup.config.maxSize > 0 && FileUtils.sizeOfDirectory(root) / 1024 > TextileBackup.config.maxSize) {
				Arrays.stream(root.listFiles()).filter(File::isFile).sorted().forEach(f -> {
					if (FileUtils.sizeOfDirectory(root) / 1024 > TextileBackup.config.maxSize) {
						Utilities.log(ctx, "message.creator.delete_file", f.getName());
						f.delete();
					}
				});
			}
		}
	}
}