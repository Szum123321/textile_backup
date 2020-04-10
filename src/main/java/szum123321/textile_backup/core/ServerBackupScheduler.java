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

package szum123321.textile_backup.core;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import szum123321.textile_backup.TextileBackup;

public class ServerBackupScheduler {
	private static long lastBackupTime;
	private static MinecraftServer server;

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if(event.phase == TickEvent.Phase.START) {
			TextileBackup.logger.info("Diiff: " + (System.currentTimeMillis() - lastBackupTime));
			if(System.currentTimeMillis() - lastBackupTime >= TextileBackup.config.backupInterval * 1000) {
				if(server.getPlayerList().getCurrentPlayerCount() > 0 || TextileBackup.config.doBackupsOnEmptyServer)
					BackupHelper.create(server, null, true, null);

				TextileBackup.logger.info("Time: " + lastBackupTime);

				lastBackupTime = System.currentTimeMillis();
			}
		}
	}

	@SubscribeEvent
	public static void onServerStarted(FMLServerStartingEvent event) {
		server = event.getServer();
		lastBackupTime = System.currentTimeMillis();

		TextileBackup.logger.info("Server Starting at: " + lastBackupTime);
		TextileBackup.logger.info("Interval is: " + TextileBackup.config.backupInterval);
	}

	@SubscribeEvent
	public static void onServerStopped(FMLServerStoppedEvent event) {
		if(TextileBackup.config.shutdownBackup)
			BackupHelper.create(event.getServer(), null, false, null);
	}
}
