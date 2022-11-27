/*
    A simple backup mod for Fabric
    Copyright (C) 2022  Szum123321

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

package net.szum123321.textile_backup.core;

import net.szum123321.textile_backup.Globals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
public class FileTreeHashBuilder {
    private final Object lock = new Object();
    private long hash = 0, filesProcessed = 0, filesTotalSize = 0;

    public void update(Path path, long newHash) throws IOException {
        var hasher = Globals.CHECKSUM_SUPPLIER.get();

        long size = Files.size(path);

        hasher.update(path.toString().getBytes(StandardCharsets.UTF_8));
        hasher.update(newHash);

        synchronized (lock) {
            //This way, the exact order of files processed doesn't matter.
            this.hash ^= hasher.getValue();
            filesProcessed++;
            filesTotalSize += size;
        }
    }

    public long getValue() {
        var hasher = Globals.CHECKSUM_SUPPLIER.get();

        hasher.update(hash);
        hasher.update(filesProcessed);
        hasher.update(filesTotalSize);

        return hasher.getValue();
    }
}
