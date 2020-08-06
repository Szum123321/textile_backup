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
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionType;
import net.szum123321.textile_backup.ConfigHandler;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.mixin.MinecraftServerSessionAccessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class Utilities {
	public static String getLevelName(MinecraftServer server) {
		return 	((MinecraftServerSessionAccessor)server).getSession().getDirectoryName();
	}

	public static File getWorldFolder(MinecraftServer server) {
		return ((MinecraftServerSessionAccessor)server)
				.getSession()
				.getWorldDirectory(RegistryKey.of(Registry.DIMENSION, DimensionType.OVERWORLD_REGISTRY_KEY.getValue()));
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	public static boolean isBlacklisted(Path path) {
		if(isWindows()) { //hotfix!
			if (path.getFileName().toString().equals("session.lock")) {
				Statics.LOGGER.trace("Skipping session.lock");
				return true;
			}
		}

		for(String i : Statics.CONFIG.fileBlacklist) {
			if(path.startsWith(i))
				return true;
		}

		return false;
	}

	public static Optional<ConfigHandler.ArchiveFormat> getFileExtension(String fileName) {
		String[] parts = fileName.split("\\.");

		switch (parts[parts.length - 1]) {
			case "zip":
				return Optional.of(ConfigHandler.ArchiveFormat.ZIP);
			case "bz2":
				return Optional.of(ConfigHandler.ArchiveFormat.BZIP2);
			case "gz":
				return Optional.of(ConfigHandler.ArchiveFormat.GZIP);
			case "xz":
				return Optional.of(ConfigHandler.ArchiveFormat.LZMA);

			default:
				return Optional.empty();
		}
	}

	public static Optional<ConfigHandler.ArchiveFormat> getFileExtension(File f) {
		return getFileExtension(f.getName());
	}

	public static Optional<LocalDateTime> getFileCreationTime(File file) {
		LocalDateTime creationTime = null;

		if(getFileExtension(file).isPresent()) {
			String fileExtension = getFileExtension(file).get().getString();

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

	public static File getBackupRootPath(String worldName) {
		File path = new File(Statics.CONFIG.path).getAbsoluteFile();

		if (Statics.CONFIG.perWorldBackup)
			path = path.toPath().resolve(worldName).toFile();

		if (!path.exists()) {
			try {
				path.mkdirs();
			} catch (Exception e) {
				Statics.LOGGER.error("An exception occurred!", e);

				return FabricLoader
						.getInstance()
						.getGameDirectory()
						.toPath()
						.resolve(Statics.CONFIG.path)
						.toFile();
			}
		}

		return path;
	}

	public static boolean isValid(File f) {
		return getFileExtension(f).isPresent() && getFileCreationTime(f).isPresent();
	}

	public static DateTimeFormatter getDateTimeFormatter() {
		return DateTimeFormatter.ofPattern(Statics.CONFIG.dateTimeFormat);
	}

	public static DateTimeFormatter getBackupDateTimeFormatter() {
		return Statics.defaultDateTimeFormatter;
	}

	public static String formatDuration(Duration duration) {
		DateTimeFormatter formatter;

		if(duration.toHours() > 0)
			formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
		else if(duration.toMinutes() > 0)
			formatter = DateTimeFormatter.ofPattern("mm:ss.SSS");
		else
			formatter = DateTimeFormatter.ofPattern("ss.SSS");

		return LocalTime.ofNanoOfDay(duration.toNanos()).format(formatter);
	}
}
