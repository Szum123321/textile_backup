/*
 * A simple backup mod for Fabric
 * Copyright (C)  2022   Szum123321
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.szum123321.textile_backup.config;

import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.SerializedName;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.szum123321.textile_backup.TextileBackup;

import java.time.format.DateTimeFormatter;
import java.util.*;

//TODO: Remove BZIP2 and LZMA compressors. As for the popular vote
@Config(name = TextileBackup.MOD_ID)
public class ConfigPOJO implements ConfigData {
    @Comment("\nShould every world have its own backup folder?\n")
    @ConfigEntry.Gui.NoTooltip()
    @ConfigEntry.Gui.Excluded
    public boolean perWorldBackup = true;

    @Comment("""
            \nTime between automatic backups in seconds
            When set to 0 backups will not be performed automatically
            """)
    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.Category("Create")
    public long backupInterval = 3600;

    @Comment("\nDelay in seconds between typing-in /backup restore and it actually starting\n")
    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.Category("Restore")
    public int restoreDelay = 30;

    @Comment("\nShould backups be done even if there are no players?\n")
    @ConfigEntry.Gui.NoTooltip()
    @ConfigEntry.Category("Create")
    public boolean doBackupsOnEmptyServer = false;

    @Comment("\nShould backup be made on server shutdown?\n")
    @ConfigEntry.Gui.NoTooltip()
    @ConfigEntry.Category("Create")
    public boolean shutdownBackup = true;

    @Comment("\nShould world be backed up before restoring a backup?\n")
    @ConfigEntry.Gui.NoTooltip()
    @ConfigEntry.Category("Restore")
    public boolean backupOldWorlds = true;

    @Comment("\nA path to the backup folder\n")
    @SerializedName("path")
    @ConfigEntry.Gui.NoTooltip()
    public String backupDirectoryPath = "backup/";

    @Comment("""
            \nThis setting allows you to exclude files form being backed-up.
            Be very careful when setting it, as it is easy corrupt your world!
            """)
    @ConfigEntry.Gui.NoTooltip()
    @ConfigEntry.Category("Create")
    public List<String> fileBlacklist = new ArrayList<>();

    @Comment("\nShould backups be deleted after being restored?\n")
    @ConfigEntry.Gui.NoTooltip()
    @ConfigEntry.Category("Restore")
    public boolean deleteOldBackupAfterRestore = true;

    @Comment("\nMaximum number of backups to keep. If set to 0 then no backup will be deleted based their amount\n")
    @ConfigEntry.Gui.NoTooltip()
    public int backupsToKeep = 10;

    @Comment("\nMaximum age of backups to keep in seconds.\n If set to 0 then backups will not be deleted based their age \n")
    @ConfigEntry.Gui.NoTooltip()
    public long maxAge = 0;

    @Comment("""
            \nMaximum size of backup folder in kibi bytes (1024).
            If set to 0 then backups will not be deleted
            """)
    @ConfigEntry.Gui.Tooltip()
    public long maxSize = 0;

    @Comment("\nCompression level \n0 - 9\n Only affects zip compression.\n")
    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.BoundedDiscrete(max = 9)
    @ConfigEntry.Category("Create")
    public int compression = 7;

    @Comment("""
            \nLimit how many cores can be used for compression.
            0 means that all available cores will be used
            """)
    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.Category("Create")
    public int compressionCoreCountLimit = 0;

    @Comment("""
            \nAvailable formats are:
            ZIP - normal zip archive using standard deflate compression
            GZIP - tar.gz using gzip compression
            TAR - .tar with no compression
            """)
    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.Category("Create")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public ArchiveFormat format = ArchiveFormat.ZIP;

    @Comment("\nMinimal permission level required to run commands\n")
    @ConfigEntry.Category("Manage")
    @ConfigEntry.Gui.NoTooltip()
    public int permissionLevel = 4;

    @Comment("\nPlayer on singleplayer is always allowed to run command. Warning! On lan party everyone will be allowed to run it.\n")
    @ConfigEntry.Gui.NoTooltip()
    @ConfigEntry.Category("Manage")
    public boolean alwaysSingleplayerAllowed = true;

    @Comment("\nPlayers allowed to run backup commands without sufficient permission level\n")
    @ConfigEntry.Gui.NoTooltip()
    @ConfigEntry.Category("Manage")
    public List<String> playerWhitelist = new ArrayList<>();

    @Comment("\nPlayers banned from running backup commands besides their sufficient permission level\n")
    @ConfigEntry.Gui.NoTooltip()
    @ConfigEntry.Category("Manage")
    public List<String> playerBlacklist = new ArrayList<>();

    @Comment("\nAnnounce to ALL players when backup starts\n")
    @ConfigEntry.Gui.NoTooltip()
    @ConfigEntry.Category("Manage")
    public boolean broadcastBackupStart = true;

    @Comment("\nAnnounce to ALL players when backup finishes\n")
    @ConfigEntry.Gui.NoTooltip()
    @ConfigEntry.Category("Manage")
    public boolean broadcastBackupDone = true;

    @Comment("""
            \nFormat of date&time used to name backup files.
            Remember not to use '#' symbol or any other character that is not allowed by your operating system such as:
            ':', '\\', etc...
            For more info: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
            """)
    @ConfigEntry.Gui.Tooltip()
    public String dateTimeFormat = "yyyy.MM.dd_HH-mm-ss";

    @Comment("""
            \nThe Strict mode (default) aborts backup creation in case of any problem and deletes created files
            Permissible mode keeps partial/damaged backup but won't allow to restore it
            Very Permissible mode will skip the verification process. THIS MOST CERTAINLY WILL LEAD TO DATA LOSS OR CORRUPTION
            """)
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public IntegrityVerificationMode integrityVerificationMode = IntegrityVerificationMode.STRICT;

    @Override
    public void validatePostLoad() throws ValidationException {
        if(compressionCoreCountLimit > Runtime.getRuntime().availableProcessors())
            throw new ValidationException("compressionCoreCountLimit is too high! Your system only has: " + Runtime.getRuntime().availableProcessors() + " cores!");

        try {
            DateTimeFormatter.ofPattern(dateTimeFormat);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "dateTimeFormat is wrong! See: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html",
                    e
                    );
        }
    }

    public enum IntegrityVerificationMode {
        STRICT,
        PERMISSIBLE,
        VERY_PERMISSIBLE;

        public boolean isStrict() { return this == STRICT; }

        public boolean verify() { return this != VERY_PERMISSIBLE; }
    }

    public enum ArchiveFormat {
        ZIP("zip"),
        GZIP("tar", "gz"),
        BZIP2("tar", "bz2"),
        LZMA("tar", "xz"),
        TAR("tar");

        private final List<String> extensionPieces;

        ArchiveFormat(String... extensionParts) {
            extensionPieces = Arrays.asList(extensionParts);
        }

        public String getCompleteString() {
            StringBuilder builder = new StringBuilder();

            extensionPieces.forEach(s -> builder.append('.').append(s));

            return builder.toString();
        }

        boolean isMultipart() {
            return extensionPieces.size() > 1;
        }

        public String getLastPiece() {
            return extensionPieces.get(extensionPieces.size() - 1);
        }
    }
}
