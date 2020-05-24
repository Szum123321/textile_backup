package net.szum123321.textile_backup.core;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class SchedulerData {
	private final String fileName;
	private final long creationTime;
	private final long delayTime;
	private boolean confirmed;

	public long getCreationTime() { return creationTime; }

	public long getDelay() { return delayTime; }

	public boolean isConfirmed() {
		return confirmed;
	}

	public void confirm() {
		this.confirmed = true;
	}

	public String getFileName() {
		return fileName;
	}

	public SchedulerData(String fileName, long delayTime) {
		this.fileName = fileName;
		this.creationTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
		this.delayTime = delayTime;

		confirmed = false;
	}
}
