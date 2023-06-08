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

package net.szum123321.textile_backup.core.create;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class BrokenFileHandler {
    private final Map<String, Exception> store = new HashMap<>();
    public void handle(Path file, Exception e) { store.put(file.toString(), e); }

    public boolean valid() { return store.isEmpty(); }

    public Map<String, Exception> get() {
        return store;
    }
}
