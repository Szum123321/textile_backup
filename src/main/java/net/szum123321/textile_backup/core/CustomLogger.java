/*
 * A simple backup mod for Fabric
 * Copyright (C) 2020  Szum123321
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

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.szum123321.textile_backup.core.create.BackupContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.spi.StandardLevel;

/*
    This is practically just a copy-pate of Cotton's ModLogger with few changes
*/
public class CustomLogger {
    private final boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();

    private final MessageFactory messageFactory;
    private final Logger logger;

    private final String prefix;
    private final Text prefixText;

    public CustomLogger(String name, String prefix) {
        this.messageFactory = ParameterizedMessageFactory.INSTANCE;
        this.logger = LogManager.getLogger(name, messageFactory);
        this.prefix = "[" + prefix + "]" + " ";
        this.prefixText = new LiteralText(this.prefix).formatted(Formatting.AQUA);
    }

    public void log(Level level, String msg, Object... data) {
        logger.log(level, prefix + msg, data);
    }

    public void trace(String msg, Object... data) {
        log(Level.TRACE, msg, data);
    }

    public void debug(String msg, Object... data) {
        log(Level.DEBUG, msg, data);
    }

    public void info(String msg, Object... data) {
        log(Level.INFO, msg, data);
    }

    public void warn(String msg, Object... data) {
        log(Level.WARN, msg, data);
    }

    public void error(String msg, Object... data) {
        log(Level.ERROR, msg, data);
    }

    public void fatal(String msg, Object... data) {
        log(Level.FATAL, msg, data);
    }

    public void devError(String msg, Object... data) {
        if (isDev) error(msg, data);
    }

    public void devWarn(String msg, Object... data) {
        if (isDev) warn(msg, data);
    }

    public void devInfo(String msg, Object... data) {
        if (isDev) info(msg, data);
    }

    public void devDebug(String msg, Object... data) {
        if (isDev) debug(msg, data);
    }

    public void devTrace(String msg, Object... data) {
        if(isDev) trace(msg, data);
    }

    private void sendToPlayer(Level level, ServerCommandSource source, String msg, Object... args) {
        if(source != null && source.getEntity() != null) {
            LiteralText text = new LiteralText(messageFactory.newMessage(msg, args).getFormattedMessage());

            if(level.intLevel() <= StandardLevel.WARN.intLevel())
                text.formatted(Formatting.RED);
            else
                text.formatted(Formatting.WHITE);

            source.sendFeedback(prefixText.shallowCopy().append(text), false);
        }
    }

    public void sendInfo(ServerCommandSource source, String msg, Object... args) {
        sendToPlayer(Level.INFO, source, msg, args);
    }

    public void sendError(ServerCommandSource source, String msg, Object... args) {
        sendToPlayer(Level.ERROR, source, msg, args);
    }

    public void sendInfo(BackupContext context, String msg, Object... args) {
        sendInfo(context.getCommandSource(), msg, args);
    }

    public void sendError(BackupContext context, String msg, Object... args) {
        sendError(context.getCommandSource(), msg, args);
    }
}
