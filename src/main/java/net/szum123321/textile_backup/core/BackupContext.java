package net.szum123321.textile_backup.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

public class BackupContext {
    private final MinecraftServer server;
    private final ServerCommandSource commandSource;
    private final BackupInitiator initiator;
    private final boolean save;
    private final String comment;

    protected BackupContext(@NotNull  MinecraftServer server, ServerCommandSource commandSource, @NotNull BackupInitiator initiator, boolean save, String comment) {
        this.server = server;
        this.commandSource = commandSource;
        this.initiator = initiator;
        this.save = save;
        this.comment = comment;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public ServerCommandSource getCommandSource() {
        return commandSource;
    }

    public BackupInitiator getInitiator() {
        return initiator;
    }

    public boolean startedByPlayer() {
        return initiator == BackupInitiator.Player;
    }

    public boolean shouldSave() {
        return save;
    }

    public String getComment() {
        return comment;
    }

    public static class Builder {
        private MinecraftServer server;
        private ServerCommandSource commandSource;
        private BackupInitiator initiator;
        private boolean save;
        private String comment;

        private boolean guessInitiator;

        public Builder() {
            this.server = null;
            this.commandSource = null;
            this.initiator = null;
            this.save = false;
            this.comment = null;

            guessInitiator = false;
        }

        public Builder setCommandSource(ServerCommandSource commandSource) {
            this.commandSource = commandSource;
            return this;
        }

        public Builder setServer(MinecraftServer server) {
            this.server = server;
            return this;
        }

        public Builder setInitiator(BackupInitiator initiator) {
            this.initiator = initiator;
            return this;
        }

        public Builder setComment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder guessInitiator() {
            this.guessInitiator = true;
            return this;
        }

        public Builder setSave() {
            this.save = true;
            return this;
        }

        public BackupContext build() {
            if(guessInitiator) {
                initiator = commandSource.getEntity() == null ? BackupInitiator.ServerConsole : BackupInitiator.Player;
            } else if(initiator == null) {
                initiator = BackupInitiator.Null;
            }

            if(server == null)
                setServer(commandSource.getMinecraftServer());

            return new BackupContext(server, commandSource, initiator, save, comment);
        }
    }

    public enum BackupInitiator {
        Player ("Player", "by: "),
        ServerConsole ("Server Console", "from: "),
        Timer ("Timer", "by: "),
        Shutdown ("Server Shutdown", "by: "),
        Null ("Null (That shouldn't have happened)", "form: ");

        private final String name;
        private final String prefix;

        BackupInitiator(String name, String prefix) {
            this.name = name;
            this.prefix = prefix;
        }

        public String getName() {
            return name;
        }

        public String getPrefix() {
            return prefix;
        }
    }
}
