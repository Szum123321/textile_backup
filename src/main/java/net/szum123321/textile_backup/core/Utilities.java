package net.szum123321.textile_backup.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.mixin.MinecraftServerSessionAccessor;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

public class Utilities {
	public static String getLevelName(MinecraftServer server) {
		return 	((MinecraftServerSessionAccessor)server).getSession().getDirectoryName();
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

	public static DateTimeFormatter getBackupDateTimeFormatter() {
		return DateTimeFormatter.ofPattern("dd.MM.yyyy_HH-mm-ss");
	}

	public static void log(String s, ServerCommandSource ctx){
		if(ctx != null)
			ctx.sendFeedback(new LiteralText(s), false);

		if(TextileBackup.config.log)
			TextileBackup.logger.info(s);
	}

	public static void error(String s, ServerCommandSource ctx){
		if(ctx != null)
			ctx.sendFeedback(new LiteralText(s).styled(style -> style.withColor(Formatting.RED)), true);

		if(TextileBackup.config.log)
			TextileBackup.logger.error(s);
	}
}
