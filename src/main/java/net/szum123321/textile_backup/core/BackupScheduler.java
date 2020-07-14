package net.szum123321.textile_backup.core;

import net.minecraft.server.MinecraftServer;
import net.szum123321.textile_backup.TextileBackup;

import java.time.Instant;

public class BackupScheduler {
    private boolean scheduled;
    private long nextBackup;

    public BackupScheduler() {
        scheduled = false;
        nextBackup = -1;
    }

    public void tick(MinecraftServer server) {
        if(TextileBackup.config.backupInterval <= 0)
            return;

        long now = Instant.now().getEpochSecond();

        if(TextileBackup.config.doBackupsOnEmptyServer || server.getPlayerManager().getCurrentPlayerCount() > 0) {
            if(scheduled) {
                if(nextBackup >= now) {
                    TextileBackup.executorSerivece.submit(BackupHelper.create(server, null, true, null));

                    nextBackup = now + TextileBackup.config.backupInterval;
                }
            } else {
                nextBackup = now + TextileBackup.config.backupInterval;
                scheduled = true;
            }
        } else if(!TextileBackup.config.doBackupsOnEmptyServer && server.getPlayerManager().getCurrentPlayerCount() == 0) {
            if(scheduled && nextBackup >= now) {
                TextileBackup.executorSerivece.submit(BackupHelper.create(server, null, true, null));

                scheduled = false;
            }
        }
    }
}