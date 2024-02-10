package net.szum123321.textile_backup.api;

import net.szum123321.textile_backup.TextileLogger;

public class TestTextileBackup implements TextileBackupApi {
    private final static TextileLogger log = new TextileLogger(net.szum123321.textile_backup.TextileBackup.MOD_NAME);
    @Override
    public TextileLock tryLock(final TextileBackupMetadata meta) {
        log.info("Seeping", meta.toString());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Good morning!");

        return  () -> log.info("Released!");
    }
}
