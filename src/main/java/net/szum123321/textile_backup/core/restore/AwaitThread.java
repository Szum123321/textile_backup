package net.szum123321.textile_backup.core.restore;

import net.szum123321.textile_backup.TextileBackup;

public class AwaitThread extends Thread {
    private final int delay;
    private final Runnable taskRunnable;
    private boolean running;

    public AwaitThread(int delay, Runnable taskRunnable) {
        this.delay = delay;
        this.taskRunnable = taskRunnable;
    }

    @Override
    public void run() {
        TextileBackup.LOGGER.info("Countdown begins...");

        running = true;
        int counter = delay * 10; // done to increase responsiveness


        while(counter > 0) {  // 𝄞 This is final count down! Tu ruru Tu, Tu Ru Tu Tu ♪
            try {
                Thread.sleep(100);
                counter--;
            } catch (InterruptedException e) {
                TextileBackup.LOGGER.info("An exception occurred while counting down", e);
            }

            if(!running)
                return;
        }

        /*
            We're leaving together,
            But still it's farewell
            And maybe we'll come back
         */
        new Thread(taskRunnable).start();

        running = false;
    }

    public synchronized void kill() {
        this.running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
