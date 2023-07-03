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

import java.io.IOException;

/**
 * Wrapper for specific IOException. Temporary way to get more info about issue #51
 */
public class NoSpaceLeftOnDeviceException extends IOException {
    public NoSpaceLeftOnDeviceException(Throwable cause) {
        super("底层文件系统的可用空间已耗尽. \nSee: https://github.com/Szum123321/textile_backup/wiki/ZIP-Problems", cause);
    }
}
