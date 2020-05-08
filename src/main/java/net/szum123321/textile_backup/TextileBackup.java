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
import io.github.cottonmc.cotton.logging.ModLogger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.class_5218;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelStorage;
import net.szum123321.textile_backup.commands.BlacklistCommand;
import net.szum123321.textile_backup.commands.CleanupCommand;
import net.szum123321.textile_backup.commands.StartBackupCommand;
import net.szum123321.textile_backup.commands.WhitelistCommand;
import net.szum123321.textile_backup.mixin.MinecraftServerSessionAccessor;

public class TextileBackup implements ModInitializer {
    public static final String MOD_ID = "textile_backup";
    public static ModLogger logger;

    public static ConfigHandler config;

    @Override
    public void onInitialize() {
        logger = new ModLogger(this.getClass());

        logger.info("Loading TextileBackup by Szum123321");

        config = ConfigManager.loadConfig(ConfigHandler.class);

        registerCommands();

        ServerStartCallback.EVENT.register(server -> {
            LevelStorage.Session session = ((MinecraftServerSessionAccessor)server).getSession();

            System.out.println(session.getDirectory(class_5218.field_24188).toAbsolutePath());
            System.out.println(session.method_27424(DimensionType.OVERWORLD).toPath().toAbsolutePath());
        });
    }

    private void registerCommands(){
        CommandRegistry.INSTANCE.register(false, dispatcher -> dispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("backup")
                        .requires((ctx) -> {
                                    try {
                                        return ((config.playerWhitelist.contains(ctx.getEntityOrThrow().getEntityName()) ||
                                                ctx.hasPermissionLevel(config.permissionLevel)) &&
                                                !config.playerBlacklist.contains(ctx.getEntityOrThrow().getEntityName())) ||
                                                (ctx.getMinecraftServer().isSinglePlayer() &&
                                                config.alwaysSingleplayerAllowed);
                                    }catch (Exception e){ //Command was called from server console.
                                        return true;
                                    }
                                }
                        ).then(BlacklistCommand.register())
                        .then(CleanupCommand.register())
                        .then(StartBackupCommand.register())
                        .then(WhitelistCommand.register())
        ));
    }
}
