package net.szum123321.textile_backup.core;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.szum123321.textile_backup.TextileBackup;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

public class Utilities {
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
