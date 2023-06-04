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

package net.szum123321.textile_backup;

import net.minecraft.server.MinecraftServer;
import net.szum123321.textile_backup.core.digest.BalticHash;
import net.szum123321.textile_backup.core.digest.Hash;
import net.szum123321.textile_backup.core.Utilities;

import net.szum123321.textile_backup.core.restore.AwaitThread;
import org.apache.commons.io.FileUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.zip.CRC32;

public class Globals {
    public static final Globals INSTANCE = new Globals();
    private static final TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    public static final DateTimeFormatter defaultDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    public static final Supplier<Hash> CHECKSUM_SUPPLIER = BalticHash::new;/*() -> new Hash() {
        private final CRC32 crc = new CRC32();

        @Override
        public void update ( int b){
            crc.update(b);
        }

        @Override
        public void update ( long b) {
            ByteBuffer v = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            v.putLong(b);
            crc.update(v.array());
        }

        @Override
        public void update ( byte[] b, int off, int len){
            crc.update(b, off, len);
        }

        @Override
        public long getValue () {
            return crc.getValue();
        }
    };*/

    private ExecutorService executorService = null;//TODO: AAAAAAAAAAAAAAA MEMORY LEAK!!!!!!!!!
    public final AtomicBoolean globalShutdownBackupFlag = new AtomicBoolean(true);
    public boolean disableWatchdog = false;
    private boolean disableTMPFiles = false;
    private AwaitThread restoreAwaitThread = null;
    private Path lockedPath = null;

    private String combinedVersionString;

    private Globals() {}

    public ExecutorService getQueueExecutor() { return executorService; }

    public void resetQueueExecutor()  {
        if(Objects.nonNull(executorService) && !executorService.isShutdown()) return;
        executorService = Executors.newSingleThreadExecutor();
    }

    public void shutdownQueueExecutor(long timeout)  {
        if(executorService.isShutdown()) return;
        executorService.shutdown();

        try {
            if(!executorService.awaitTermination(timeout, TimeUnit.MICROSECONDS)) {
                log.error("Timeout occurred while waiting for currently running backups to finish!");
                executorService.shutdownNow().stream()
                       // .filter(r -> r instanceof ExecutableBackup)
                       // .map(r -> (ExecutableBackup)r)
                        .forEach(r -> log.error("Dropping: {}", r.toString()));
                if(!executorService.awaitTermination(1000, TimeUnit.MICROSECONDS))
                    log.error("Couldn't shut down the executor!");
            }
        } catch (InterruptedException e) {
            log.error("An exception occurred!", e);
        }

    }

    public Optional<AwaitThread> getAwaitThread() { return Optional.ofNullable(restoreAwaitThread); }

    public void setAwaitThread(AwaitThread th) { restoreAwaitThread = th; }


    public Optional<Path> getLockedFile() { return Optional.ofNullable(lockedPath); }
    public void setLockedFile(Path p) { lockedPath = p; }

    public synchronized boolean disableTMPFS() { return disableTMPFiles; }
    public synchronized void updateTMPFSFlag(MinecraftServer server) {
        disableTMPFiles = false;
        Path tmp_dir = Path.of(System.getProperty("java.io.tmpdir"));
        if(
                FileUtils.sizeOfDirectory(Utilities.getWorldFolder(server).toFile()) >=
                        tmp_dir.toFile().getUsableSpace()
        ) {
            log.error("Not enough space left in TMP directory! ({})", tmp_dir);
            disableTMPFiles = true;
        }

        if(!Files.isWritable(tmp_dir)) {
            log.error("TMP filesystem ({}) is read-only!", tmp_dir);
            disableTMPFiles = true;
        }

        if(disableTMPFiles) log.error("Might cause: https://github.com/Szum123321/textile_backup/wiki/ZIP-Problems");
    }

    public String getCombinedVersionString() {
        return combinedVersionString;
    }

    public void setCombinedVersionString(String combinedVersionString) {
        this.combinedVersionString = combinedVersionString;
    }
}
