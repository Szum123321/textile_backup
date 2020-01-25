package net.szum123321.textile_backup;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.cottonmc.cotton.config.ConfigManager;
import io.github.cottonmc.cotton.logging.ModLogger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.commands.CleanupCommand;
import net.szum123321.textile_backup.commands.StartBackupCommand;

public class TextileBackup implements ModInitializer {
    public static final String MOD_ID = "textile_backup";
    public static ModLogger logger;

    public static ConfigHandler config;

    @Override
    public void onInitialize() {
        logger = new ModLogger(this.getClass());

        logger.info("Loading TextileBackup by Szum123321");

        config = ConfigManager.loadConfig(ConfigHandler.class);
        config.backupInterval *= 1000;

        registerCommands();
    }

    private void registerCommands(){
        CommandRegistry.INSTANCE.register(false, dispatcher -> dispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("backup")
                    .then(StartBackupCommand.register())
                    .then(CleanupCommand.register())
        ));
    }
}
