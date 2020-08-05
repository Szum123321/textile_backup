package net.szum123321.textile_backup.core.restore;

import net.szum123321.textile_backup.Statics;

public class AwaitThread extends Thread {
    private final int delay;
    private final Runnable taskRunnable;

    public AwaitThread(int delay, Runnable taskRunnable) {
        this.delay = delay;
        this.taskRunnable = taskRunnable;
    }

    @Override
    public void run() {
        Statics.LOGGER.info("Countdown begins...");

        // 𝄞 This is final count down! Tu ruru Tu, Tu Ru Tu Tu ♪
        try {
            Thread.sleep(delay * 1000);
        } catch (InterruptedException e) {
            return;
        }

        /*
            We're leaving together,
            But still it's farewell
            And maybe we'll come back
         */
        new Thread(taskRunnable).start();
    }
}
