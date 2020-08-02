package net.szum123321.textile_backup.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.szum123321.textile_backup.ConfigHandler;
import net.szum123321.textile_backup.TextileBackup;
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

	public static boolean isBlacklisted(Path path) {
		if(isWindows()) { //hotfix!
			if (path.getFileName().toString().equals("session.lock")) {
				TextileBackup.LOGGER.trace("Skipping session.lock");
				return true;
			}
		}

		for(String i : TextileBackup.config.fileBlacklist) {
			if(path.startsWith(i))
				return true;
		}

		return false;
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	public static Optional<String> getFileExtension(File f) {
		String[] parts = f.getName().split("\\.");

		switch (parts[parts.length - 1]) {
			case "zip":
				return Optional.of(ConfigHandler.ArchiveFormat.ZIP.getExtension());
			case "bz2":
				return Optional.of(ConfigHandler.ArchiveFormat.BZIP2.getExtension());
			case "gz":
				return Optional.of(ConfigHandler.ArchiveFormat.GZIP.getExtension());
			case "xz":
				return Optional.of(ConfigHandler.ArchiveFormat.LZMA.getExtension());

			default:
				return Optional.empty();
		}
	}

	public static Optional<LocalDateTime> getFileCreationTime(File file) {
		LocalDateTime creationTime = null;

		if(getFileExtension(file).isPresent()) {
			String fileExtension = getFileExtension(file).get();

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
		}

		if(creationTime == null) {
			try {
				FileTime fileTime = (FileTime) Files.getAttribute(file.toPath(), "creationTime");
				creationTime = LocalDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.systemDefault());
			} catch (IOException ignored3) {}
		}

		return Optional.ofNullable(creationTime);
	}

	public static DateTimeFormatter getDateTimeFormatter(){
		if(!TextileBackup.config.dateTimeFormat.equals(""))
			return DateTimeFormatter.ofPattern(TextileBackup.config.dateTimeFormat);
		else
			return getBackupDateTimeFormatter();
	}

	public static DateTimeFormatter getBackupDateTimeFormatter() {
		return DateTimeFormatter.ofPattern("dd.MM.yyyy_HH-mm-ss");
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

	public static void info(String s, ServerCommandSource ctx){
		if(ctx != null && ctx.getEntity() != null)
			ctx.sendFeedback(new LiteralText(s), false);

		if(TextileBackup.config.log)
			TextileBackup.LOGGER.info(s);
	}

	public static void sendError(String message, ServerCommandSource source) {
		if(source != null) {
			source.sendFeedback(new LiteralText(message).styled(style -> style.withColor(Formatting.RED)), false);
		}
	}
}
