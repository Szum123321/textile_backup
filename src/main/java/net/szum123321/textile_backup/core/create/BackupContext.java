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

package net.szum123321.textile_backup.core.create;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.core.ActionInitiator;
import org.jetbrains.annotations.NotNull;

public record BackupContext(@NotNull MinecraftServer server,
                            ServerCommandSource commandSource,
                            ActionInitiator initiator,
                            boolean save,
                            String comment) {

    public MinecraftServer getServer() {
        return server;
    }

    public ServerCommandSource getCommandSource() {
        return commandSource;
    }

    public ActionInitiator getInitiator() {
        return initiator;
    }

    public boolean startedByPlayer() {
        return initiator == ActionInitiator.Player;
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
        private ActionInitiator initiator;
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

        public static Builder newBackupContextBuilder() {
            return new Builder();
        }

        public Builder setCommandSource(ServerCommandSource commandSource) {
            this.commandSource = commandSource;
            return this;
        }

        public Builder setServer(MinecraftServer server) {
            this.server = server;
            return this;
        }

        public Builder setInitiator(ActionInitiator initiator) {
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

        public Builder saveServer() {
            this.save = true;
            return this;
        }

        public BackupContext build() {
            if (guessInitiator) {
                initiator = commandSource.getEntity() instanceof PlayerEntity ? ActionInitiator.Player : ActionInitiator.ServerConsole;
            } else if (initiator == null) {
                initiator = ActionInitiator.Null;
            }

            if (server == null) {
                if (commandSource != null) setServer(commandSource.getServer());
                else
                    throw new RuntimeException("Neither MinecraftServer or ServerCommandSource were provided!");
            }

            return new BackupContext(server, commandSource, initiator, save, comment);
        }
    }
}
