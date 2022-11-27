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

package net.szum123321.textile_backup.core.restore;

import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;

import java.util.concurrent.atomic.AtomicInteger;

/*
    This thread waits some amount of time and then starts a new, independent thread
*/
public class AwaitThread extends Thread {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    private final static AtomicInteger threadCounter = new AtomicInteger(0);

    private final int delay;
    private final int thisThreadId = threadCounter.getAndIncrement();
    private final Runnable taskRunnable;

    public AwaitThread(int delay, Runnable taskRunnable) {
        this.setName("Textile Backup await thread nr. " + thisThreadId);
        this.delay = delay;
        this.taskRunnable = taskRunnable;
    }

    @Override
    public void run() {
        log.info("Countdown begins... Waiting {} second.", delay);

        // 𝄞 This is final count down! Tu ruru Tu, Tu Ru Tu Tu ♪
        try {
            Thread.sleep(delay * 1000L);
        } catch (InterruptedException e) {
            log.info("Backup restoration cancelled.");
            return;
        }

        /*
            We're leaving together,
            But still it's farewell
            And maybe we'll come back
         */
        new Thread(taskRunnable, "Textile Backup restore thread nr. " + thisThreadId).start();
    }
}
