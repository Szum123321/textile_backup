package net.szum123321.textile_backup.api;

import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.core.ActionInitiator;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TextileBackupApi {
    String TEXTILE_API_ID = "textile_api";

    TextileLock tryLock(final TextileBackupMetadata meta) throws RuntimeException;

    interface TextileLock extends AutoCloseable {
        default void close() {
            release();
        }

        void release();
    }

    interface TextileBackupMetadata {
        boolean shouldSave();
        Optional<ServerCommandSource> getCommandSource();

        ActionInitiator getActonInitiator();

        String getComment();

        LocalDateTime getStartTime();
    }
}
