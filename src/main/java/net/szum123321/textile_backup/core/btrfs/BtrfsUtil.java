package net.szum123321.textile_backup.core.btrfs;

import java.io.File;

public class BtrfsUtil {
    public static BtrfsUtilJnaInterface buInterface;


    public static boolean isSubVol(File file) {
        int ret = buInterface.btrfs_util_is_subvolume(file.toString());
        return ret == 0;
    }

    public static boolean createSnapshot(File source, File dest) {
        buInterface.btrfs_util_create_snapshot(source.toString(), dest.toString(), 0, null, null);
        return isSubVol(dest);
    }

    public static boolean deleteSubVol(File file) {
        buInterface.btrfs_util_delete_subvolume(file.toString(), 0);
        return !file.exists();
    }
}
