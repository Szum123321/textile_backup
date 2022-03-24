package net.szum123321.textile_backup.core.btrfs;

import com.sun.jna.Library;

public interface BtrfsUtilJnaInterface extends Library {
    int btrfs_util_is_subvolume(String path, Object... args);
    void btrfs_util_create_subvolume(String path, Object... args);
    void btrfs_util_create_snapshot(String source, String destination, Object... args);
    void btrfs_util_delete_subvolume(String path, Object... args);
}