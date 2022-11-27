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

package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;

import java.time.format.DateTimeParseException;

public class CommandExceptions {
    public static final DynamicCommandExceptionType DATE_TIME_PARSE_COMMAND_EXCEPTION_TYPE = new DynamicCommandExceptionType(o -> {
        DateTimeParseException e = (DateTimeParseException)o;

        MutableText message = Text.literal("An exception occurred while trying to parse:\n")
                .append(e.getParsedString())
                .append("\n");

        for (int i = 0; i < e.getErrorIndex(); i++) message.append(" ");

        return message.append("^");
    });
}
