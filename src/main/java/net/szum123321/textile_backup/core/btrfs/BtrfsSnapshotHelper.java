package net.szum123321.textile_backup.core.btrfs;

import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.core.ActionInitiator;
import net.szum123321.textile_backup.core.create.BackupContext;

import java.io.File;

public class BtrfsSnapshotHelper {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

    public static void createSnapshot(File inputFile, File outputFile, BackupContext ctx) {
        if (BtrfsUtil.isSubVol(inputFile)) {
            if (!BtrfsUtil.createSnapshot(inputFile, outputFile)) {
                if (ctx != null && ctx.getInitiator() == ActionInitiator.Player)
                    log.sendError(ctx, "Failed to create snapshot.");
                else log.error("Failed to create snapshot.");
            }
        } else if (ctx != null && ctx.getInitiator() == ActionInitiator.Player)
            log.sendError(ctx, "The world folder is not in a btrfs subvolume. Backup canceled.");
        else log.error("The world folder is not in a btrfs subvolume. Backup canceled.");
    }
}
