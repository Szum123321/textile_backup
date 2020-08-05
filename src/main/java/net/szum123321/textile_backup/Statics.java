package net.szum123321.textile_backup;

import net.szum123321.textile_backup.core.CustomLogger;
import net.szum123321.textile_backup.core.create.BackupScheduler;
import net.szum123321.textile_backup.core.restore.AwaitThread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Statics {
    public static final String MOD_ID = "textile_backup";
    public static final String MOD_NAME = "Textile Backup";
    public static final CustomLogger LOGGER = new CustomLogger(MOD_ID, MOD_NAME);
    public static ConfigHandler CONFIG;

    public static final BackupScheduler scheduler = new BackupScheduler();
    public static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static final AtomicBoolean globalShutdownBackupFlag = new AtomicBoolean(true);
    public static AwaitThread restoreAwaitThread;
}
