package net.szum123321.textile_backup.core;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.szum123321.textile_backup.ConfigHandler;
import net.szum123321.textile_backup.TextileBackup;

import java.io.File;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class Utilities {
	public static void sanitizeBackupFolder(File root) {
		Arrays.stream(root.listFiles()).forEach(f -> {
			if(f.getName().contains("#")) {
				File f2 = new File(f.getName().replace('#', '+'));
				if(!f.renameTo(f2))
					Utilities.error("Error while renaming file: " + f.getName(), null);
			}
		});
	}

	public static String getFileExtension(File f) {
		String[] parts = f.getName().split("\\.");

		switch (parts[parts.length - 1]) {
			case "zip":
				return ConfigHandler.ArchiveFormat.ZIP.getExtension();
			case "bz2":
				return ConfigHandler.ArchiveFormat.BZIP2.getExtension();
			case "gz":
				return ConfigHandler.ArchiveFormat.GZIP.getExtension();
			case "xz":
				return ConfigHandler.ArchiveFormat.LZMA.getExtension();

			default:
				return null;
		}
	}

	public static File getBackupRootPath(String worldName) {
		File path = new File(TextileBackup.config.path).getAbsoluteFile();

		if (TextileBackup.config.perWorldBackup)
			path = path.toPath().resolve(worldName).toFile();

		if (!path.exists()) {
			try {
				path.mkdirs();
			} catch (Exception e) {
				TextileBackup.logger.error(e.getMessage());

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

	public static boolean isWindows(){
		String os = System.getProperty("os.name");
		return os.toLowerCase().startsWith("win");
	}

	public static boolean isBlacklisted(Path path) {
		for(String i : TextileBackup.config.fileBlacklist) {
			if(path.startsWith(i))
				return true;
		}

		return false;
	}

	public static DateTimeFormatter getDateTimeFormatter(){
		if(!TextileBackup.config.dateTimeFormat.equals(""))
			return DateTimeFormatter.ofPattern(TextileBackup.config.dateTimeFormat);
		else
			return getBackupDateTimeFormatter();
	}

	public static DateTimeFormatter getBackupDateTimeFormatter(){
		if(isWindows()){
			return DateTimeFormatter.ofPattern("dd.MM.yyyy_HH-mm-ss");
		} else {
			return DateTimeFormatter.ofPattern("dd.MM.yyyy_HH:mm:ss");
		}
	}

	public static void log(ServerCommandSource ctx, String key, Object... args){
		if(ctx != null)
			ctx.sendFeedback(new TranslatableText(key, args), false);

		if(TextileBackup.config.log)
			TextileBackup.logger.info(new TranslatableText(key, args).asString());
	}

	public static void error(String s, ServerCommandSource ctx){
		if(ctx != null)
			ctx.sendFeedback(new LiteralText(s), true);

		if(TextileBackup.config.log)
			TextileBackup.logger.error(s);
	}
}
