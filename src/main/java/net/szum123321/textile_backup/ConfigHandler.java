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
