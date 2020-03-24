package net.szum123321.textile_backup.core;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.szum123321.textile_backup.TextileBackup;

import java.time.format.DateTimeFormatter;

public class Utilities {
	public static DateTimeFormatter getDateTimeFormatter(){
		if(!TextileBackup.config.dateTimeFormat.equals(""))
			return DateTimeFormatter.ofPattern(TextileBackup.config.dateTimeFormat);
		else
			return getBackupDateTimeFormatter();
	}

	public static DateTimeFormatter getBackupDateTimeFormatter(){
		String os = System.getProperty("os.name");
		if(os.toLowerCase().startsWith("win")){
			return DateTimeFormatter.ofPattern("dd.MM.yyyy_HH-mm-ss");
		} else {
			return DateTimeFormatter.ofPattern("dd.MM.yyyy_HH:mm:ss");
		}
	}

	public static void log(String s, ServerCommandSource ctx){
		if(ctx != null)
			ctx.sendFeedback(new LiteralText(s), false);

		if(TextileBackup.config.log)
			TextileBackup.logger.info(s);
	}

	public static void error(String s, ServerCommandSource ctx){
		if(ctx != null)
			ctx.sendFeedback(new LiteralText(s), true);

		if(TextileBackup.config.log)
			TextileBackup.logger.error(s);
	}
}
