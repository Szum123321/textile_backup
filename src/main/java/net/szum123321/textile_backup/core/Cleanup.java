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

package net.szum123321.textile_backup.core;

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class Cleanup {
	private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
	private final static ConfigHelper config = ConfigHelper.INSTANCE;

	public static int executeFileLimit(ServerCommandSource ctx, String worldName) {
		Path root = Utilities.getBackupRootPath(worldName);
		int deletedFiles = 0;

		if (Files.isDirectory(root) && Files.exists(root) && !isEmpty(root)) {
			if (config.get().maxAge > 0) { // delete files older that configured
				final long now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);

				try(Stream<Path> stream = Files.list(root)) {
					deletedFiles += stream
							.filter(Utilities::isValidBackup)// We check if we can get file's creation date so that the next line won't throw an exception
							.filter(f -> now - Utilities.getFileCreationTime(f).get().toEpochSecond(ZoneOffset.UTC) > config.get().maxAge)
							.mapToInt(f -> deleteFile(f, ctx))
							.sum();
				} catch (IOException e) {
					log.error("An exception occurred while trying to delete old files!", e);
				}
			}

			final int noToKeep = config.get().backupsToKeep > 0 ? config.get().backupsToKeep : Integer.MAX_VALUE;
			final long maxSize = config.get().maxSize > 0 ? config.get().maxSize * 1024: Long.MAX_VALUE;

			AtomicInteger currentNo = new AtomicInteger(countBackups(root));
			AtomicLong currentSize = new AtomicLong(countSize(root));

			try(Stream<Path> stream = Files.list(root)) {
				deletedFiles += stream
						.filter(Utilities::isValidBackup)
						.sorted(Comparator.comparing(f -> Utilities.getFileCreationTime(f).get()))
						.takeWhile(f -> (currentNo.get() > noToKeep) || (currentSize.get() > maxSize))
						.peek(f -> {
							currentNo.decrementAndGet();
							try {
								currentSize.addAndGet(-Files.size(f));
							} catch (IOException e) {
								currentSize.set(0);
							}
						})
						.mapToInt(f -> deleteFile(f, ctx))
						.sum();
			} catch (IOException e) {
				log.error("An exception occurred while trying to delete old files!", e);
			}
		}

		return deletedFiles;
	}

	private static int countBackups(Path path) {
		try(Stream<Path> stream = Files.list(path)) {
			return (int) stream
					.filter(Utilities::isValidBackup)
					.count();
		} catch (IOException e) {
			log.error("Error while counting files!", e);
		}
		return 0;
	}

	private static long countSize(Path path) {
		try(Stream<Path> stream = Files.list(path)) {
			return stream
					.filter(Utilities::isValidBackup)
					.mapToLong(f -> {
						try {
							return Files.size(f);
						} catch (IOException e) {
							log.error("Couldn't delete a file!", e);
							return 0;
						}
					})
					.sum();
		} catch (IOException e) {
			log.error("Error while counting files!", e);
		}
		return 0;
	}

	private static boolean isEmpty(Path path) {
		if (Files.isDirectory(path)) {
			try (Stream<Path> entries = Files.list(path)) {
				return entries.findFirst().isEmpty();
			} catch (IOException e) {
				return false;
			}
		}

		return false;
	}

	//1 -> ok, 0 -> err
	private static int deleteFile(Path f, ServerCommandSource ctx) {
		if(Globals.INSTANCE.getLockedFile().filter(p -> p == f).isEmpty()) {
			try {
				Files.delete(f);
				log.sendInfoAL(ctx, "Deleting: {}", f);
			} catch (IOException e) {
				if(Utilities.wasSentByPlayer(ctx)) log.sendError(ctx, "Something went wrong while deleting: {}.", f);
				log.error("Something went wrong while deleting: {}.", f, e);
				return 0;
			}
			return 1;
		}

		return 0;
	}
}