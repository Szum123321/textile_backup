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
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.core.digest.BalticHash;
import net.szum123321.textile_backup.core.digest.Hash;
import net.szum123321.textile_backup.core.Utilities;

import net.szum123321.textile_backup.core.restore.ExecutableRestore;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class Globals {
    public static final Globals INSTANCE = new Globals();
    private static final TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    public static final DateTimeFormatter defaultDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    public static final Supplier<Hash> CHECKSUM_SUPPLIER = BalticHash::new;

    private ExecutorService executorService = null;//TODO: AAAAAAAAAAAAAAA MEMORY LEAK!!!!!!!!!
    public AtomicInteger ACTIVE_BACKUP_COUNTER = new AtomicInteger(0);
    public final AtomicBoolean globalShutdownBackupFlag = new AtomicBoolean(true);
    public boolean disableWatchdog = false;
    private boolean disableTMPFiles = false;

    public Waiter restoreAwaiter = new Waiter();

    private String combinedVersionString;

    public ExecutorService getQueueExecutor() {
        return executorService;
    }

    public void resetQueueExecutor() {
        if (Objects.nonNull(executorService) && !executorService.isShutdown()) return;
        executorService = Executors.newSingleThreadExecutor();
    }

    public void shutdownQueueExecutor(long timeout) {
        if (executorService.isShutdown()) return;
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(timeout, TimeUnit.MICROSECONDS)) {
                log.error("Timeout occurred while waiting for currently running backups to finish!");
                executorService.shutdownNow()
                        .forEach(r -> log.error("Dropping: {}", r.toString()));
                if (!executorService.awaitTermination(1000, TimeUnit.MICROSECONDS))
                    log.error("Couldn't shut down the executor!");
            }
        } catch (InterruptedException e) {
            log.error("An exception occurred!", e);
        }

    }

    public synchronized boolean disableTMPFS() {
        return disableTMPFiles;
    }

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

        if (!Files.isWritable(tmp_dir)) {
            log.error("TMP filesystem ({}) is read-only!", tmp_dir);
            disableTMPFiles = true;
        }

        if (disableTMPFiles) log.error("Might cause: https://github.com/Szum123321/textile_backup/wiki/ZIP-Problems");
    }

    public String getCombinedVersionString() {
        return combinedVersionString;
    }

    public void setCombinedVersionString(String combinedVersionString) {
        this.combinedVersionString = combinedVersionString;
    }

    public static class Waiter {
        private ExecutableRestore state = null;
        private Thread delayThread = null;

        public synchronized void schedule(ExecutableRestore executable) throws CollisionException {
            if (Objects.nonNull(delayThread)) throw new CollisionException();
            state = executable;

            delayThread = new AwaitThread(ConfigHelper.INSTANCE.get().restoreDelay, executable, () -> {
                synchronized (this) {
                    this.state = null;
                    this.delayThread = null;
                }
            });

            delayThread.start();
        }

        public synchronized void cancel() {
            if (Objects.nonNull(delayThread)) delayThread.interrupt();
        }

        public boolean isRunning() {
            return Objects.nonNull(delayThread) && delayThread.isAlive();
        }

        public Optional<Path> getFile() {
            return Optional.ofNullable(state).map(v -> v.restoreableFile().getFile());
        }

        public static class CollisionException extends Exception {
            public CollisionException() {

            }
        }
        public static class AwaitThread extends Thread {
            private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
            private final static AtomicInteger threadCounter = new AtomicInteger(0);

            private final int delay;
            private final int thisThreadId = threadCounter.getAndIncrement();
            private final Runnable taskRunnable;
            private final Runnable callback;

            public AwaitThread(int delay, Runnable taskRunnable, Runnable callback) {
                this.setName("Textile Backup await thread nr. " + thisThreadId);
                this.delay = delay;
                this.taskRunnable = taskRunnable;
                this.callback = callback;
            }

            @Override
            public void run() {
                log.info("Countdown begins... Waiting {} second.", delay);

                // 𝄞 This is final count down! Tu ruru Tu, Tu Ru Tu Tu ♪
                try {
                    Thread.sleep(delay * 1000L);
                     /*
                        We're leaving together,
                        But still it's farewell
                        And maybe we'll come back
                     */
                    //Detach restore process
                    new Thread(taskRunnable, "Textile Backup restore thread nr. " + thisThreadId).start();
                } catch (InterruptedException e) {
                    log.info("Backup restoration cancelled.");
                } finally {
                    callback.run();
                }
            }
        }
    }
}
