/*
 * Simple backup mod made for Fabric and ported to Forge
 *     Copyright (C) 2020  Szum123321
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package szum123321.textile_backup.core;

import net.minecraft.command.CommandSource;
import net.minecraft.util.text.StringTextComponent;
import szum123321.textile_backup.TextileBackup;

import java.time.format.DateTimeFormatter;

public class Utilities {
	public static boolean isWindows(){
		String os = System.getProperty("os.name");
		return os.toLowerCase().startsWith("win");
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

	public static void log(String s, CommandSource ctx){
		if(ctx != null)
			ctx.sendFeedback(new StringTextComponent(s), false);

		if(TextileBackup.config.log)
			TextileBackup.logger.info(s);
	}

	public static void error(String s, CommandSource ctx){
		if(ctx != null)
			ctx.sendFeedback(new StringTextComponent(s), true);

		if(TextileBackup.config.log)
			TextileBackup.logger.error(s);
	}
}
