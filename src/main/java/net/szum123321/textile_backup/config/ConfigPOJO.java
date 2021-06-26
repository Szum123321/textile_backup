/*
 * A simple backup mod for Fabric
 * Copyright (C) 2021 Szum123321
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

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.szum123321.textile_backup.TextileBackup;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Config(name = TextileBackup.MOD_ID)
public class ConfigPOJO implements ConfigData {
    @Comment("""
            Format of date&time used to name backup files.
            Remember not to use '#' symbol or any other character that is not allowed by your operating system such as:
            ':', '\\', etc...
            For more info: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html""")
    public String dateTimeFormat = "yyyy.MM.dd_HH-mm-ss";

    @Comment("Should every world have its own backup folder?")
    @ConfigEntry.Gui.Excluded
    public boolean perWorldBackup = true;

    @Comment("A path to the backup folder")
    public String path = "backup/";

    @Comment("""
            This setting allows you to exclude files form being backed-up.
            Be very careful when setting it, as it is easy corrupt your world!""")
    public List<String> fileBlacklist = new ArrayList<>();

    @Comment("Should backups be deleted after being restored?")
    public boolean deleteOldBackupAfterRestore = true;

    @Comment("Maximum number of backups to keep.\nIf set to 0 then no backup will be deleted based their amount")
    public int backupsToKeep = 10;

    @Comment("""
            Maximum age of backups to keep in seconds.
             If set to 0 then backups will not be deleted based their age""")
    public long maxAge = 0;

    @Comment("""
            Maximum size of backup folder in kilo bytes (1024).
            If set to 0 then backups will not be deleted""")
    public int maxSize = 0;

    @Comment("""
            Time between automatic backups in seconds
            When set to 0 backups will not be performed automatically""")
    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.Category("Create")
    public long backupInterval = 3600;

    @Comment("Should backups be done even if there are no players?")
    @ConfigEntry.Category("Create")
    public boolean doBackupsOnEmptyServer = false;

    @Comment("Should backup be made on server shutdown?")
    @ConfigEntry.Category("Create")
    public boolean shutdownBackup = true;

    @Comment("Should world be backed up before restoring a backup?")
    @ConfigEntry.Category("Create")
    public boolean backupOldWorlds = true;

    @Comment("Compression level 0 - 9 Only affects zip compression.")
    @ConfigEntry.BoundedDiscrete(max = 9)
    @ConfigEntry.Category("Create")
    public int compression = 7;

    @Comment("""
            Limit how many cores can be used for compression.
            0 means that all available cores will be used""")
    @ConfigEntry.Category("Create")
    public int compressionCoreCountLimit = 0;

    @Comment(value = """
            Available formats are:
            ZIP - normal zip archive using standard deflate compression
            GZIP - tar.gz using gzip compression
            BZIP2 - tar.bz2 archive using bzip2 compression
            LZMA - tar.xz using lzma compression
            TAR - .tar with no compression""")
    @ConfigEntry.Category("Create")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public ArchiveFormat format = ArchiveFormat.ZIP;

    @Comment("Minimal permission level required to run commands")
    @ConfigEntry.Category("Manage")
    public int permissionLevel = 4;

    @Comment("""
        Player on singleplayer is always allowed to run command.
        Warning! On lan party everyone will be allowed to run it.""")
    @ConfigEntry.Category("Manage")
    public boolean alwaysSingleplayerAllowed = true;

    @Comment("Players allowed to run backup commands without sufficient permission level")
    @ConfigEntry.Category("Manage")
    public Set<String> playerWhitelist = new HashSet<>();

    @Comment("Players banned from running backup commands besides their sufficient permission level")
    @ConfigEntry.Category("Manage")
    public Set<String> playerBlacklist = new HashSet<>();

    @Comment("Delay in seconds between typing-in /backup restore and it actually starting")
    @ConfigEntry.Category("Restore")
    public int restoreDelay = 30;

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
