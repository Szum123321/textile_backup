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

package net.szum123321.textile_backup.core;

/**
 * Enum representing possible sources of action
 */
public enum ActionInitiator {
    Player("Player", "by"),
    ServerConsole("Server Console", "from"), //some/ting typed a command and it was not a player (command blocks and server console count)
    Timer("Timer", "by"), //a.k.a scheduler
    Shutdown("Server Shutdown", "by"),
    Restore("Backup Restoration", "because of"),
    Null("Null (That shouldn't have happened)", "form");

    private final String name;
    private final String prefix;

    ActionInitiator(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix + ": ";
    }
}
