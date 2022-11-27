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

package net.szum123321.textile_backup.core.restore;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.core.ActionInitiator;
import net.szum123321.textile_backup.core.RestoreableFile;

import javax.annotation.Nullable;

public record RestoreContext(RestoreableFile restoreableFile,
                             MinecraftServer server,
                             @Nullable String comment,
                             ActionInitiator initiator,
                             ServerCommandSource commandSource) {
    public static final class Builder {
        private RestoreableFile file;
        private MinecraftServer server;
        private String comment;
        private ServerCommandSource serverCommandSource;

        private Builder() {
        }

        public static Builder newRestoreContextBuilder() {
            return new Builder();
        }

        public Builder setFile(RestoreableFile file) {
            this.file = file;
            return this;
        }

        public Builder setServer(MinecraftServer server) {
            this.server = server;
            return this;
        }

        public Builder setComment(@Nullable String comment) {
            this.comment = comment;
            return this;
        }

        public Builder setCommandSource(ServerCommandSource commandSource) {
            this.serverCommandSource = commandSource;
            return this;
        }

        public RestoreContext build() {
            if (server == null) server = serverCommandSource.getServer();

            ActionInitiator initiator = serverCommandSource.getEntity() instanceof PlayerEntity ? ActionInitiator.Player : ActionInitiator.ServerConsole;

            return new RestoreContext(file, server, comment, initiator, serverCommandSource);
        }
    }
}
