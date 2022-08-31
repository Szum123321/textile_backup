/*
 *  A simple backup mod for Fabric
 *  Copyright (C) 2022  Szum123321
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package net.szum123321.textile_backup;

import net.minecraft.server.MinecraftServer;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.MakeBackupRunnable;
import net.szum123321.textile_backup.core.restore.AwaitThread;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Globals {
    public static final Globals INSTANCE = new Globals();
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    public final static DateTimeFormatter defaultDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    public final AtomicBoolean globalShutdownBackupFlag = new AtomicBoolean(true);
    public boolean disableWatchdog = false;
    private boolean disableTMPFiles = false;
    private AwaitThread restoreAwaitThread = null;
    private Path lockedPath = null;

    private Globals() {}

    public ExecutorService getQueueExecutor() { return executorService; }

    public void resetQueueExecutor()  {
        if(!executorService.isShutdown()) return;
        executorService = Executors.newSingleThreadExecutor();
    }

    public void shutdownQueueExecutor(long timeout)  {
        if(executorService.isShutdown()) return;
        executorService.shutdown();

        try {
            if(!executorService.awaitTermination(timeout, TimeUnit.MICROSECONDS)) {
                log.error("Timeout occurred while waiting for currently running backups to finish!");
                executorService.shutdownNow().stream()
                        .filter(r -> r instanceof MakeBackupRunnable)
                        .map(r -> (MakeBackupRunnable)r)
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

    public boolean disableTMPFS() { return disableTMPFiles; }
    public void updateTMPFSFlag(MinecraftServer server) {
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
}
