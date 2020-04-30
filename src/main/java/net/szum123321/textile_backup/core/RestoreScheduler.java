package net.szum123321.textile_backup.core;

import net.minecraft.server.MinecraftServer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RestoreScheduler {
	Map<Integer, SchedulerData> dataMap = new HashMap<>();

	public void confirm(int token) {
		dataMap.get(token).confirm();
	}

	public void cancel(int token) {
		dataMap.remove(token);
	}

	public int add(String fileName, long delay) {
		Random r = new Random();
		int token = r.nextInt();

		while(dataMap.containsKey(token)) {
			token = r.nextInt();
		}

		dataMap.put(token, new SchedulerData(fileName, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) + delay));

		return token;
	}

	public SchedulerData getData(int token) {
		return dataMap.get(token);
	}

	public void tick(MinecraftServer server) {
		LocalDateTime time = LocalDateTime.now();

		dataMap.forEach((key, data) -> {
			if(data.isConfirmed()) {
				if(data.getCreationTime() + data.getDelay() <= time.toEpochSecond(ZoneOffset.UTC)) {
					RestoreHelper.runRestore(server, data.getFileName());
				}
			} else {
				if(data.getCreationTime() + 60 <= time.toEpochSecond(ZoneOffset.UTC)) {
					dataMap.remove(key);
				}
			}
		});
	}
}
