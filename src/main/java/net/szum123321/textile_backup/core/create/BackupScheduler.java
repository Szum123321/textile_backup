package net.szum123321.textile_backup.core.create;

import net.minecraft.server.MinecraftServer;
import net.szum123321.textile_backup.Statics;

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

        if(Statics.CONFIG.doBackupsOnEmptyServer || server.getPlayerManager().getCurrentPlayerCount() > 0) {
            if(scheduled) {
                if(nextBackup <= now) {
                    Statics.executorService.submit(
                            BackupHelper.create(
                                    new BackupContext.Builder()
                                            .setServer(server)
                                            .setInitiator(BackupContext.BackupInitiator.Timer)
                                            .setSave()
                                            .build()
                            )
                    );

                    nextBackup = now + Statics.CONFIG.backupInterval;
                }
            } else {
                nextBackup = now + Statics.CONFIG.backupInterval;
                scheduled = true;
            }
        } else if(!Statics.CONFIG.doBackupsOnEmptyServer && server.getPlayerManager().getCurrentPlayerCount() == 0) {
            if(scheduled && nextBackup <= now) {
                Statics.executorService.submit(
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