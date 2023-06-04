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

package net.szum123321.textile_backup;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.ExecutableBackup;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.util.StackLocatorUtil;

/*
    This is practically just a copy-pate of Cotton's ModLogger with a few changes
*/
public class TextileLogger {
    //private final boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();

    private final MessageFactory messageFactory;
    private final Logger logger;

    private final String prefix;
    private final MutableText prefixText;

    public TextileLogger(String prefix) {
        this.messageFactory = ParameterizedMessageFactory.INSTANCE;
        this.logger = LogManager.getLogger(StackLocatorUtil.getCallerClass(2), messageFactory);
        this.prefix = "[" + prefix + "]" + " ";
        this.prefixText = Text.literal(this.prefix).styled(style -> style.withColor(0x5B23DA));
    }

    public MutableText getPrefixText() {
        return prefixText.copy();
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

    void error(String message, Throwable throwable) {
        logger.error(prefix + message, throwable);
    }

    public void fatal(String msg, Object... data) {
        log(Level.FATAL, msg, data);
    }

    boolean sendFeedback(Level level, ServerCommandSource source, String msg, Object... args) {
        if(source != null && Utilities.wasSentByPlayer(source)) {
            MutableText text = Text.literal(messageFactory.newMessage(msg, args).getFormattedMessage());

            if(level.intLevel() == Level.TRACE.intLevel()) text.formatted(Formatting.GREEN);
            else if(level.intLevel() <= Level.WARN.intLevel()) text.formatted(Formatting.RED);
            else text.formatted(Formatting.WHITE);

            source.sendFeedback(() -> prefixText.copy().append(text), false);

            return true;
        } else {
            log(level, msg, args);

            return false;
        }
    }

    public void sendHint(ServerCommandSource source, String msg, Object... args) {
        sendFeedback(Level.TRACE, source, msg, args);
    }

    public void sendInfo(ServerCommandSource source, String msg, Object... args) {
        sendFeedback(Level.INFO, source, msg, args);
    }

    public void sendInfo(ExecutableBackup context, String msg, Object... args) {
        sendInfo(context.commandSource(), msg, args);
    }

    public void sendError(ServerCommandSource source, String msg, Object... args) {
        sendFeedback(Level.ERROR, source, msg, args);
    }


    public void sendError(ExecutableBackup context, String msg, Object... args) {
        sendError(context.commandSource(), msg, args);
    }

    public void sendToPlayerAndLog(Level level, ServerCommandSource source, String msg, Object... args) {
        if(sendFeedback(level, source, msg, args))
            log(level, msg, args);
    }

    //send info and log
    public void sendInfoAL(ServerCommandSource source, String msg, Object... args) {
        sendToPlayerAndLog(Level.INFO, source, msg, args);
    }

    public void sendInfoAL(ExecutableBackup context, String msg, Object... args) {
        sendInfoAL(context.commandSource(), msg, args);
    }

    public void sendErrorAL(ServerCommandSource source, String msg, Object... args) {
        sendToPlayerAndLog(Level.ERROR, source, msg, args);
    }

    public void sendErrorAL(ExecutableBackup context, String msg, Object... args) {
        sendErrorAL(context.commandSource(), msg, args);
    }
}
