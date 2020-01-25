package net.szum123321.textile_backup;

import blue.endless.jankson.Comment;
import io.github.cottonmc.cotton.config.annotations.ConfigFile;

@ConfigFile(name = TextileBackup.MOD_ID)
public class ConfigHandler {
    @Comment("\nTime between backups in seconds\n")
    public long backupInterval = 3600;

    @Comment("\nShould backups be done even if there is no players?\n")
    public boolean doBackupsOnEmptyServer = false;

    @Comment("\nShould backups be made on server shutdown\n")
    public boolean shutdownBackup = true;

    @Comment("\nA path to backup folder\n")
    public String path = "backup/";

    @Comment("\nMaximum number of backups to keep. if 0 then no backup will be deleted\n")
    public int backupsToKeep = 0;

    @Comment("\nMaximum age of backups to keep in seconds.\n if 0 then backups will not be deleted based on age \n")
    public int maxAge = 0;

    @Comment("\nCompression level \n0 - 9\n")
    public int compression = 1;

    @Comment("\nPrint info to game out\n")
    public boolean log = true;
}
