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
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * Utility used for removing old backups
 */
public class Cleanup implements Callable<Integer> {
	private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
	private final static ConfigHelper config = ConfigHelper.INSTANCE;

	private final ServerCommandSource ctx;
	private final String worldName;

	public Cleanup(ServerCommandSource ctx, String worldName) {
		this.ctx = ctx;
		this.worldName = worldName;
	}

	public Integer call() {
		Path root = Utilities.getBackupRootPath(worldName);
		int deletedFiles = 0;

		if (!Files.isDirectory(root) || !Files.exists(root) || isEmpty(root)) return 0;

		if (config.get().maxAge > 0) { // delete files older that configured
			final long now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);

			deletedFiles += RestoreableFile.applyOnFiles(root, 0L,
					e -> log.error("An exception occurred while trying to delete old files!", e),
					stream -> stream.filter(f -> now - f.getCreationTime().toEpochSecond(ZoneOffset.UTC) > config.get().maxAge)
							.filter(f -> deleteFile(f.getFile(), ctx))
							.count()
			);
		}

		final int noToKeep = config.get().backupsToKeep > 0 ? config.get().backupsToKeep : Integer.MAX_VALUE;
		final long maxSize = config.get().maxSize > 0 ? config.get().maxSize * 1024: Long.MAX_VALUE; //max number of bytes to keep

		long[] counts = count(root);
		long n = counts[0], size = counts[1];

		var it = RestoreableFile.applyOnFiles(root, null,
				e -> log.error("An exception occurred while trying to delete old files!", e),
				s -> s.sorted().toList().iterator());

		if(Objects.isNull(it)) return deletedFiles;

		while(it.hasNext() && (n > noToKeep || size > maxSize)) {
			Path f = it.next().getFile();
			long x;
			try {
				x = Files.size(f);
			} catch (IOException e) { size = 0; continue; }

			if(!deleteFile(f, ctx)) continue;

			size -= x;
			n--;
			deletedFiles++;
		}

		return deletedFiles;
	}

	private long[] count(Path root) {
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

	private boolean isEmpty(Path root) {
		if (!Files.isDirectory(root)) return false;
		return RestoreableFile.applyOnFiles(root, false, e -> {}, s -> s.findFirst().isEmpty());
	}

	//1 -> ok, 0 -> err
	private boolean deleteFile(Path f, ServerCommandSource ctx) {
		if(Globals.INSTANCE.getLockedFile().filter(p -> p == f).isPresent()) return false;
		try {
			Files.delete(f);
			log.sendInfoAL(ctx, "Deleted: {}", f);
		} catch (IOException e) {
			if(Utilities.wasSentByPlayer(ctx)) log.sendError(ctx, "Something went wrong while deleting: {}.", f);
			log.error("Something went wrong while deleting: {}.", f, e);
			return false;
		}
		return true;
	}
}