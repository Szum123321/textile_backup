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

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.mixin.MinecraftServerSessionAccessor;
import org.apache.commons.io.file.SimplePathVisitor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class Utilities {
	private final static ConfigHelper config = ConfigHelper.INSTANCE;
	private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

	//I'm keeping this wrapper function for easier backporting
	public static boolean wasSentByPlayer(ServerCommandSource source) { return source.isExecutedByPlayer(); }

	public static void notifyPlayers(@NotNull MinecraftServer server, String msg) {
		MutableText message = log.getPrefixText();
		message.append(Text.literal(msg).formatted(Formatting.WHITE));

		server.getPlayerManager().broadcast(message, false);
	}

	public static String getLevelName(MinecraftServer server) {
		return 	((MinecraftServerSessionAccessor)server).getSession().getDirectoryName();
	}

	public static Path getWorldFolder(MinecraftServer server) {
		return ((MinecraftServerSessionAccessor)server)
				.getSession()
				.getWorldDirectory(World.OVERWORLD);
	}

	public static void deleteDirectory(Path path) throws IOException {
		Files.walkFileTree(path, new SimplePathVisitor() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public static void disableWorldSaving(MinecraftServer server) {
		for (ServerWorld serverWorld : server.getWorlds()) {
			if (serverWorld != null && !serverWorld.savingDisabled)
				serverWorld.savingDisabled = true;
		}
	}

	public static void enableWorldSaving(MinecraftServer server) {
		for (ServerWorld serverWorld : server.getWorlds()) {
			if (serverWorld != null && serverWorld.savingDisabled)
				serverWorld.savingDisabled = false;
		}
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	public static Path getBackupRootPath(String worldName) {
		Path path = Path.of(config.get().backupDirectoryPath).toAbsolutePath();

		if (config.get().perWorldBackup) path = path.resolve(worldName);

		if(Files.notExists(path)) {
			try {
				Files.createDirectories(path);
			} catch (IOException e) {
				//I REALLY shouldn't be handling this here
			}
		}

		return path;
	}

	public static boolean isBlacklisted(Path path) {
		if (path.getFileName().equals(Path.of("session.lock"))) return true;

		if(path.getFileName().equals(Path.of(CompressionStatus.DATA_FILENAME))) return true;

		return config.get().fileBlacklist.stream().anyMatch(path::startsWith);
	}

	public static DateTimeFormatter getDateTimeFormatter() {
		return DateTimeFormatter.ofPattern(config.get().dateTimeFormat);
	}

	public static String formatDuration(Duration duration) {
		DateTimeFormatter formatter;

		if(duration.toHours() > 0) formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
		else if(duration.toMinutes() > 0) formatter = DateTimeFormatter.ofPattern("mm:ss.SSS");
		else formatter = DateTimeFormatter.ofPattern("ss.SSS");

		return LocalTime.ofNanoOfDay(duration.toNanos()).format(formatter);
	}
}
