package net.szum123321.textile_backup.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.dimension.DimensionType;
import net.szum123321.textile_backup.TextileBackup;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RestoreHelper {
	public static void runRestore(MinecraftServer server, String fileToRestore) {
		File root = Utilities.getBackupRootPath(server.getWorld(DimensionType.OVERWORLD).getLevelProperties().getLevelName());

		if(Arrays.stream(root.listFiles()).map(f -> f.getName()).collect(Collectors.toList()).contains(fileToRestore)) {
			TextileBackup.config.shutdownBackup = false;

			System.out.println("Running restore!");

			//server.close();
		}
	}

	public static List<String> getAvailableBackups(String world) {
		File root = Utilities.getBackupRootPath(world);

		Utilities.sanitizeBackupFolder(root);

		return Arrays.stream(root.listFiles())
				.filter(f -> Utilities.getFileExtension(f) != null)
				.map(File::getName)
				.collect(Collectors.toList());
	}
}
