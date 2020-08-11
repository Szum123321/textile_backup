/*
    A simple backup mod for Fabric
    Copyright (C) 2020  Szum123321

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package net.szum123321.textile_backup;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.cottonmc.cotton.config.ConfigManager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.commands.create.CleanupCommand;
import net.szum123321.textile_backup.commands.create.StartBackupCommand;
import net.szum123321.textile_backup.commands.permission.BlacklistCommand;
import net.szum123321.textile_backup.commands.permission.WhitelistCommand;
import net.szum123321.textile_backup.commands.restore.KillRestoreCommand;
import net.szum123321.textile_backup.commands.restore.ListBackupsCommand;
import net.szum123321.textile_backup.commands.restore.RestoreBackupCommand;
import net.szum123321.textile_backup.core.create.BackupContext;
import net.szum123321.textile_backup.core.create.BackupHelper;

import java.util.Optional;
import java.util.concurrent.Executors;

public class TextileBackup implements ModInitializer {
    @Override
    public void onInitialize() {
        Statics.LOGGER.info("Starting Textile Backup by Szum123321.");

        Statics.CONFIG = ConfigManager.loadConfig(ConfigHandler.class);
        Optional<String> errorMessage = Statics.CONFIG.sanitize();

        if(errorMessage.isPresent()) {
            Statics.LOGGER.fatal("TextileBackup config file has wrong settings!\n{}", errorMessage.get());
            System.exit(1);
        }

        if(Statics.CONFIG.backupInterval > 0)
            ServerTickEvents.END_SERVER_TICK.register(Statics.scheduler::tick);

        ServerLifecycleEvents.SERVER_STARTING.register(ignored -> {
            if(Statics.executorService.isShutdown())
                Statics.executorService = Executors.newSingleThreadExecutor();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            Statics.executorService.shutdown();

            if (Statics.CONFIG.shutdownBackup && Statics.globalShutdownBackupFlag.get()) {
                BackupHelper.create(
                        new BackupContext.Builder()
                                .setServer(server)
                                .setInitiator(BackupContext.BackupInitiator.Shutdown)
                                .setComment("shutdown")
                                .build()
                ).run();
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("backup")
                        .requires((ctx) -> {
                                    try {
                                        return ((Statics.CONFIG.playerWhitelist.contains(ctx.getEntityOrThrow().getEntityName()) ||
                                                ctx.hasPermissionLevel(Statics.CONFIG.permissionLevel)) &&
                                                !Statics.CONFIG.playerBlacklist.contains(ctx.getEntityOrThrow().getEntityName())) ||
                                                (ctx.getMinecraftServer().isSinglePlayer() &&
                                                        Statics.CONFIG.alwaysSingleplayerAllowed);
                                    } catch (Exception ignored) { //Command was called from server console.
                                        return true;
                                    }
                                }
                        ).then(BlacklistCommand.register())
                        .then(CleanupCommand.register())
                        .then(StartBackupCommand.register())
                        .then(WhitelistCommand.register())
                        .then(RestoreBackupCommand.register())
                        .then(ListBackupsCommand.register())
                        .then(KillRestoreCommand.register())
        ));
    }
}
