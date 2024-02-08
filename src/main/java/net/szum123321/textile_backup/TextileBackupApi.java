package net.szum123321.textile_backup;

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.core.ActionInitiator;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Future;

public interface TextileBackupApi {
    String TEXTILE_API_ID = "textile_api";

    Future<Lock> tryLock(BackupMetadata meta) throws RuntimeException;

    interface Lock {
        void release();
    }
    interface BackupMetadata {
        boolean shouldSave();
        Optional<ServerCommandSource> getCommandSource();

        ActionInitiator getActonInitiator();

        String getComment();

        LocalDateTime getStartTime();
    }
}
