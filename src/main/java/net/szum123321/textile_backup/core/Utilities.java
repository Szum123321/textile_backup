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

import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.config.ConfigPOJO;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.mixin.MinecraftServerSessionAccessor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public class Utilities {
	private final static ConfigHelper config = ConfigHelper.INSTANCE;
	private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

	public static void notifyPlayers(MinecraftServer server, UUID sender, String msg) {
		MutableText message = log.getPrefixText();
		message.append(new LiteralText(msg).formatted(Formatting.WHITE));

		server.getPlayerManager().broadcast(
				message,
				MessageType.SYSTEM,
				sender
		);
	}

	public static String getLevelName(MinecraftServer server) {
		return 	((MinecraftServerSessionAccessor)server).getSession().getDirectoryName();
	}

	public static File getWorldFolder(MinecraftServer server) {
		return ((MinecraftServerSessionAccessor)server)
				.getSession()
				.getWorldDirectory(World.OVERWORLD)
				.toFile();
	}
	
	public static File getBackupRootPath(String worldName) {
		File path = new File(config.get().path).getAbsoluteFile();

		if (config.get().perWorldBackup) path = path.toPath().resolve(worldName).toFile();

		if (!path.exists()) path.mkdirs();

		return path;
	}

	public static boolean updateTMPFSFlag(MinecraftServer server) {
		Statics.disableTMPFiles = (FileUtils.sizeOfDirectory(Utilities.getWorldFolder(server)) >=
				(new File(System.getProperty("java.io.tmpdir"))).getFreeSpace());

		if(Statics.disableTMPFiles) log.warn("Not enough space left in tmp directory!\n Might cause: https://github.com/Szum123321/textile_backup/wiki/ZIP-Problems");

		return Statics.disableTMPFiles;
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

	public static boolean isBlacklisted(Path path) {
		if(isWindows()) { //hotfix!
			if (path.getFileName().toString().equals("session.lock")) return true;
		}

		return config.get().fileBlacklist.stream().anyMatch(path::startsWith);
	}

	public static Optional<ConfigPOJO.ArchiveFormat> getArchiveExtension(String fileName) {
		String[] parts = fileName.split("\\.");

		return Arrays.stream(ConfigPOJO.ArchiveFormat.values())
				.filter(format -> format.getLastPiece().equals(parts[parts.length - 1]))
				.findAny();
	}

	public static Optional<ConfigPOJO.ArchiveFormat> getArchiveExtension(File f) {
		return getArchiveExtension(f.getName());
	}

	public static Optional<LocalDateTime> getFileCreationTime(File file) {
		LocalDateTime creationTime = null;

		if(getArchiveExtension(file).isPresent()) {
			String fileExtension = getArchiveExtension(file).get().getCompleteString();

			try {
				creationTime = LocalDateTime.from(
						Utilities.getDateTimeFormatter().parse(
								file.getName().split(fileExtension)[0].split("#")[0]
						)
				);
			} catch (Exception ignored) {}

			if(creationTime == null) {
				try {
					creationTime = LocalDateTime.from(
							Utilities.getBackupDateTimeFormatter().parse(
									file.getName().split(fileExtension)[0].split("#")[0]
							)
					);
				} catch (Exception ignored2){}
			}

			if(creationTime == null) {
				try {
					FileTime fileTime = (FileTime) Files.getAttribute(file.toPath(), "creationTime");
					creationTime = LocalDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.systemDefault());
				} catch (IOException ignored3) {}
			}
		}

		return Optional.ofNullable(creationTime);
	}

	public static boolean isValidBackup(File f) {
		return getArchiveExtension(f).isPresent() && getFileCreationTime(f).isPresent() && isFileOk(f);
	}

	public static boolean isFileOk(File f) {
		return f.exists() && f.isFile();
	}

	public static DateTimeFormatter getDateTimeFormatter() {
		return DateTimeFormatter.ofPattern(config.get().dateTimeFormat);
	}

	public static DateTimeFormatter getBackupDateTimeFormatter() {
		return Statics.defaultDateTimeFormatter;
	}

	public static String formatDuration(Duration duration) {
		DateTimeFormatter formatter;

		if(duration.toHours() > 0) formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
		else if(duration.toMinutes() > 0) formatter = DateTimeFormatter.ofPattern("mm:ss.SSS");
		else formatter = DateTimeFormatter.ofPattern("ss.SSS");

		return LocalTime.ofNanoOfDay(duration.toNanos()).format(formatter);
	}
}
