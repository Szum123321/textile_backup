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

import  net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class Cleanup {
	private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
	private final static ConfigHelper config = ConfigHelper.INSTANCE;

	public static int executeFileLimit(ServerCommandSource ctx, String worldName) {
		Path root = Utilities.getBackupRootPath(worldName);
		int deletedFiles = 0;

		if (!Files.isDirectory(root) || !Files.exists(root) || isEmpty(root)) return 0;

		if (config.get().maxAge > 0) { // delete files older that configured
			final long now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);

			deletedFiles += RestoreableFile.applyOnFiles(root, 0,
					e -> log.error("An exception occurred while trying to delete old files!", e),
					stream -> stream.filter(f -> now - f.getCreationTime().toEpochSecond(ZoneOffset.UTC) > config.get().maxAge)
							.mapToInt(f -> deleteFile(f.getFile(), ctx))
							.sum()
			);
		}

		final int noToKeep = config.get().backupsToKeep > 0 ? config.get().backupsToKeep : Integer.MAX_VALUE;
		final long maxSize = config.get().maxSize > 0 ? config.get().maxSize * 1024: Long.MAX_VALUE;

		long[] counts = count(root);

		AtomicInteger currentNo = new AtomicInteger((int) counts[0]);
		AtomicLong currentSize = new AtomicLong(counts[1]);

		deletedFiles += RestoreableFile.applyOnFiles(root, 0,
				e -> log.error("An exception occurred while trying to delete old files!", e),
				stream -> stream.sequential()
						.takeWhile(f -> (currentNo.get() > noToKeep) || (currentSize.get() > maxSize))
						.map(RestoreableFile::getFile)
						.peek(f -> {
							try {
								currentSize.addAndGet(-Files.size(f));
							} catch (IOException e) {
								currentSize.set(0);
								return;
							}
							currentNo.decrementAndGet();
						})
						.mapToInt(f -> deleteFile(f, ctx))
						.sum());

		return deletedFiles;
	}

	private static long[] count(Path root) {
		long n = 0, size = 0;

		try(Stream<Path> stream = Files.list(root)) {
			var it = stream.flatMap(f -> RestoreableFile.build(f).stream()).iterator();
			while(it.hasNext()) {
				var f = it.next();
				try {
					size += Files.size(f.getFile());
				} catch (IOException e) {
					log.error("Couldn't get size of " + f.getFile(), e);
					continue;
				}
				n++;
			}
		} catch (IOException e) {
			log.error("Error while counting files!", e);
		}

		return new long[]{n, size};
	}

	private static boolean isEmpty(Path root) {
		if (!Files.isDirectory(root)) return false;
		return RestoreableFile.applyOnFiles(root, false, e -> {}, s -> s.findFirst().isEmpty());
	}

	//1 -> ok, 0 -> err
	private static int deleteFile(Path f, ServerCommandSource ctx) {
		if(Globals.INSTANCE.getLockedFile().filter(p -> p == f).isPresent()) return 0;
		try {
			Files.delete(f);
			log.sendInfoAL(ctx, "Deleted: {}", f);
		} catch (IOException e) {
			if(Utilities.wasSentByPlayer(ctx)) log.sendError(ctx, "Something went wrong while deleting: {}.", f);
			log.error("Something went wrong while deleting: {}.", f, e);
			return 0;
		}
		return 1;
	}
}