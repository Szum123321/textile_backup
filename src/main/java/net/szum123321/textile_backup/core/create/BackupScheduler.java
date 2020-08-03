package net.szum123321.textile_backup.core.create;

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
        long now = Instant.now().getEpochSecond();

        if(TextileBackup.CONFIG.doBackupsOnEmptyServer || server.getPlayerManager().getCurrentPlayerCount() > 0) {
            if(scheduled) {
                if(nextBackup <= now) {
                    TextileBackup.executorService.submit(
                            BackupHelper.create(
                                    new BackupContext.Builder()
                                            .setServer(server)
                                            .setInitiator(BackupContext.BackupInitiator.Timer)
                                            .setSave()
                                            .build()
                            )
                    );

                    nextBackup = now + TextileBackup.CONFIG.backupInterval;
                }
            } else {
                nextBackup = now + TextileBackup.CONFIG.backupInterval;
                scheduled = true;
            }
        } else if(!TextileBackup.CONFIG.doBackupsOnEmptyServer && server.getPlayerManager().getCurrentPlayerCount() == 0) {
            if(scheduled && nextBackup <= now) {
                TextileBackup.executorService.submit(
                        BackupHelper.create(
                                new BackupContext.Builder()
                                        .setServer(server)
                                        .setInitiator(BackupContext.BackupInitiator.Timer)
                                        .setSave()
                                        .build()
                        )
                );

                scheduled = false;
            }
        }
    }
}