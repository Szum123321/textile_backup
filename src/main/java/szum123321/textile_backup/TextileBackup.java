/*
 * Simple backup mod made for Fabric and ported to Forge
 *     Copyright (C) 2020  Szum123321
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package szum123321.textile_backup;

import net.minecraft.command.Commands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.ModLifecycleEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import szum123321.textile_backup.commands.BlacklistCommand;
import szum123321.textile_backup.commands.CleanupCommand;
import szum123321.textile_backup.commands.StartBackupCommand;
import szum123321.textile_backup.commands.WhitelistCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import szum123321.textile_backup.core.ServerBackupScheduler;

@Mod(TextileBackup.MOD_ID)
public class TextileBackup {
	public static final String MOD_ID = "textile_backup";
	public static final Logger logger = LogManager.getLogger(MOD_ID);

	public static ConfigData config;

	public TextileBackup() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
	}

	public void setup(FMLCommonSetupEvent event){

		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(ServerBackupScheduler.class);

		config = ConfigHandler.loadConfig();
		logger.info("Now");
	}

	@SubscribeEvent
	public void registerCommands(FMLServerStartingEvent event){
		event.getCommandDispatcher().register(
				Commands.literal("backup")
						.requires(ctx -> {
							try {
								return ((config.playerWhitelist.contains(ctx.getEntity().getName().getString()) ||
										ctx.hasPermissionLevel(config.permissionLevel)) &&
										!config.playerBlacklist.contains(ctx.getEntity().getName().getString())) ||
										(ctx.getServer().isSinglePlayer() && config.alwaysSingleplayerAllowed);
							}catch (Exception e){ //Command was called from server console.
								return true;
							}
						}).then(BlacklistCommand.register())
						.then(CleanupCommand.register())
						.then(StartBackupCommand.register())
						.then(WhitelistCommand.register())
		);
	}
}
