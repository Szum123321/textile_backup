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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.zip.Checksum;

public class FileTreeHashBuilder {
    private final static ThreadLocal<byte[]> buff =
            ThreadLocal.withInitial(() -> new byte[Long.BYTES]);
    private final Object lock = new Object();
    private final Supplier<Checksum> hasherProvider;
    private long hash = 0, filesProcessed = 0, filesTotalSize = 0;

    public FileTreeHashBuilder(Supplier<Checksum> provider) { hasherProvider = provider; }

    public void update(Path path, long newHash) throws IOException {
        byte[] raw = buff.get();
        var hasher = hasherProvider.get();

        long size = Files.size(path);

        hasher.update(ByteBuffer.wrap(raw).putLong(size).array());
        hasher.update(path.toString().getBytes(StandardCharsets.UTF_8));
        hasher.update(ByteBuffer.wrap(raw).putLong(hash).array());

        synchronized (lock) {
            //This way exact order of files processed doesn't matter.
            this.hash ^= hasher.getValue();
            filesProcessed++;
            filesTotalSize += size;
        }
    }

    public long getValue() {
        var hasher = hasherProvider.get();
        byte[] raw = buff.get();

        hasher.update(ByteBuffer.wrap(raw).putLong(hash).array());
        hasher.update(ByteBuffer.wrap(raw).putLong(filesProcessed).array());
        hasher.update(ByteBuffer.wrap(raw).putLong(filesTotalSize).array());

        return hasher.getValue();
    }
}
