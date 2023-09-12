package net.szum123321.textile_backup.core;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

public class WorldSavingState {
	private final Set<RegistryKey<World>> data;

	private WorldSavingState(Set<RegistryKey<World>> data) {
		this.data = data;
	}

	public static WorldSavingState disable(MinecraftServer server) {
		Set<RegistryKey<World>> data = new HashSet<>();

		for (ServerWorld serverWorld : server.getWorlds()) {
			if (serverWorld == null || serverWorld.savingDisabled) continue;
			serverWorld.savingDisabled = true;
			data.add(serverWorld.getRegistryKey());
		}
		return new WorldSavingState(data);
	}

	public void enable(MinecraftServer server) {
		for (ServerWorld serverWorld : server.getWorlds()) {
			if (serverWorld != null && data.contains(serverWorld.getRegistryKey()))
				serverWorld.savingDisabled = false;
		}
	}
}
