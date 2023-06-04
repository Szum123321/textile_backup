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

package net.szum123321.textile_backup.core.digest;

import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.core.CompressionStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * What this class does is it collects the hashed files and combines them into a single number,
 * thus we can verify file tree integrity
 */
public class FileTreeHashBuilder {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    private final Object lock = new Object();
    private long hash = 0, filesProcessed = 0, filesTotalSize = 0;

    private final CountDownLatch latch;

    public FileTreeHashBuilder(int filesToProcess) {
        latch = new CountDownLatch(filesToProcess);
    }

    public void update(Path path, long newHash, long bytes) throws IOException {
        if(path.getFileName().toString().equals(CompressionStatus.DATA_FILENAME)) return;

        latch.countDown();

        synchronized (lock) {
            this.hash ^= newHash;
            filesTotalSize += bytes;
            filesProcessed++;
        }
    }

    public int getRemaining() { return (int) latch.getCount(); }

    public long getValue(boolean lock) throws InterruptedException {
        long leftover = latch.getCount();
        if(lock) latch.await();
        else if(leftover != 0) log.warn("Finishing with {} files unprocessed!", leftover);

        var hasher = Globals.CHECKSUM_SUPPLIER.get();

        log.debug("Closing: files: {}, bytes {}, raw hash {}", filesProcessed, filesTotalSize, hash);
        hasher.update(hash);
        hasher.update(filesProcessed);
        hasher.update(filesTotalSize);

        return hasher.getValue();
    }
}
