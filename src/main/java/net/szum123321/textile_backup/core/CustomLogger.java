package net.szum123321.textile_backup.core;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

    public void debug(String msg, Object... data) { log(Level.DEBUG, msg, data); }

    public void info(String msg, Object... data) { log(Level.INFO, msg, data); }

    public void warn(String msg, Object... data) {
        log(Level.WARN, msg, data);
    }

    public void error(String msg, Object... data) {
        log(Level.ERROR, msg, data);
    }

    public void fatal(String msg, Object... data) {
        log(Level.FATAL, msg, data);
    }
/*
    public void warn(String msg, Throwable throwable) { logger.warn(prefix + msg, throwable); }

    public void error(String msg, Throwable throwable) { logger.error(prefix + msg, throwable); }

    public void fatal(String msg, Throwable throwable) { logger.fatal(prefix + msg, throwable); }
*/
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
        info(msg, args);
    }

    public void sendError(ServerCommandSource source, String msg, Object... args) {
        sendToPlayer(Level.ERROR, source, msg, args);
    }
}
