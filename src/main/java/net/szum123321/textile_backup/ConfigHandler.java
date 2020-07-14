/*
    A simple backup mod for Fabric
    Copyright (C) 2020  Szum123321

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package net.szum123321.textile_backup;

import blue.endless.jankson.Comment;
import io.github.cottonmc.cotton.config.annotations.ConfigFile;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@ConfigFile(name = TextileBackup.MOD_ID)
public class ConfigHandler {
    @Comment("\nTime between automatic backups in seconds\n" +
            "When set to 0 backups will not be performed automatically\n")
    public long backupInterval = 3600;

    @Comment("\nShould backups be done even if there are no players?\n")
    public boolean doBackupsOnEmptyServer = false;

    @Comment("\nShould backup be made on server shutdown?\n")
    public boolean shutdownBackup = true;

    @Comment("\nA path to backup folder\n")
    public String path = "backup/";

    @Comment("\nThis setting allows you to exclude files form being backuped.\n"+
                "Be very careful when setting it, as it is easy corrupt your world!\n")
    public Set<String> fileBlacklist = new HashSet<>();

    @Comment("\nShould every world has its won backup folder?\n")
    public boolean perWorldBackup = false;

    @Comment("\nMaximum number of backups to keep. If set to 0 then no backup will be deleted based their amount\n")
    public int backupsToKeep = 10;

    @Comment("\nMaximum age of backups to keep in seconds.\n If set to 0 then backups will not be deleted based their age \n")
    public long maxAge = 0;

    @Comment("\nMaximum size of backup folder in kilo bytes (1024).\n" +
            "If set to 0 then backups will not be deleted\n")
    public int maxSize = 0;

    @Comment("\nCompression level \n0 - 9\n Only affects zip compression.\n")
    public int compression = 6;

    @Comment(value = "\nAvailable formats are:\n" +
                    "ZIP - normal zip archive using standard deflate compression\n" +
                    "GZIP - tar.gz using gzip compression\n" +
                    "BZIP2 - tar.bz2 archive using bzip2 compression\n" +
                    "LZMA - tar.xz using lzma compression\n")
    public ArchiveFormat format = ArchiveFormat.ZIP;

    @Comment("\nLimit how many cores can be used for compression.\n")
    public int compressionCoreCountLimit = 0;

    @Comment("\nPrint info to game out\n")
    public boolean log = true;

    @Comment("\nMinimal permission level required to run commands\n")
    public int permissionLevel = 4;

    @Comment("\nPlayer on singleplayer is always allowed to run command. Warning! On lan party everyone will be allowed to run it.\n")
    public boolean alwaysSingleplayerAllowed = true;

    @Comment("\nPlayers allowed to run backup commands without sufficient permission level\n")
    public Set<String> playerWhitelist = new HashSet<>();

    @Comment("\nPlayers banned from running backup commands besides their sufficient permission level\n")
    public Set<String> playerBlacklist = new HashSet<>();

    @Comment("\nFormat of date&time used to name backup files.\n" +
            "Remember not to use '#' symbol or any other character that is not allowed by your operating system such as:\n" +
            "':', '\\', etc...\n" +
            "For more info: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html\n")
    public String dateTimeFormat = "dd.MM.yyyy_HH-mm-ss";

    public Optional<String> sanitize() {
        if(compressionCoreCountLimit > Runtime.getRuntime().availableProcessors())
            return Optional.of("compressionCoreCountLimit is too big! Your system only has: " + Runtime.getRuntime().availableProcessors() + " cores!");

        try {
            DateTimeFormatter.ofPattern(dateTimeFormat);
        } catch (IllegalArgumentException e) {
            return Optional.of("dateTimeFormat is wrong!\n" + e.getMessage() + "\n See: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html");
        }

        return Optional.empty();
    }

    public enum ArchiveFormat {
        ZIP(".zip"),
        GZIP(".tar.gz"),
        BZIP2(".tar.bz2"),
        LZMA(".tar.xz");

        private final String extension;

        private ArchiveFormat(String extension){
            this.extension = extension;
        }

        public String getExtension() {
            return extension;
        }
    }
}
