package net.szum123321.textile_backup.core.create.compressors.parallel_zip_fix;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Somewhat similar to ByteArrayOutputStream, except the data is stored in variable-size blocks.
 * Blocks are created to be at least {@link MemoryBlockOutputStream#MIN_BLOCK_SIZE} in size.
 * It is to limit object overhead
 */
public class MemoryBlockOutputStream extends OutputStream {
    private static final int MIN_BLOCK_SIZE = 65536; //64K
    private final Deque<DataBlock> blockQueue;

    public MemoryBlockOutputStream() {
        this.blockQueue = new ArrayDeque<>();
    }

    @Override
    public void write(int b) throws IOException {
        this.write(new byte[] {(byte)(b & 0xFF)}, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        while(len > 0) {
            if(blockQueue.isEmpty() || blockQueue.peekLast().full()) blockQueue.add(new DataBlock(len));

            //assert blockQueue.peekLast() != null;
            int written = blockQueue.peekLast().write(b, off, len);
            off += written;
            len -= written;
        }
    }

    /**
     * Warning! Returned InputStream will DESTROY data stored in the queue!
     * @return {@link InputStream} to read data stored in queue buffer
     */
    public InputStream getInputStream() {
        return new InMemoryInputStream(blockQueue);
    }

    private static class DataBlock {
        private final byte[] block;
        private final int size;
        private int written = 0;
        private int read = 0;

        public DataBlock(int size) {
            this.size = Math.max(size, MIN_BLOCK_SIZE);
            this.block = new byte[this.size];
        }

        public boolean full() {
            return written == size;
        }

        public boolean dataLeft() {
            return read < size;
        }

        public int write(byte[] b, int off, int len) {
            int tbw = Math.min(len, size - written);

            System.arraycopy(b, off, block, written, tbw);
            written += tbw;
            return tbw;
        }

        public int read(byte[] b, int off, int len) {
            //if(!dataLeft()) return -1;
            int tbr = Math.min(len, written - read);

            System.arraycopy(block, read, b, off, tbr);
            read += tbr;
            return tbr;
        }

        public byte[] getBlock() {
            return block;
        }
    }

    private static class InMemoryInputStream extends InputStream {
        private final Deque<DataBlock> blockQueue;

        public InMemoryInputStream(Deque<DataBlock> blockQueue) {
            this.blockQueue = blockQueue;
        }

        @Override
        public int read() {
            byte[] buff = new byte[1];
            return (this.read(buff, 0, 1) == -1) ? -1 : buff[0];
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if(blockQueue.isEmpty()) return -1;

            int totalRead = 0;

            while(len > 0 && !blockQueue.isEmpty()) {
                if(!blockQueue.peek().dataLeft()) {
                    blockQueue.poll();
                    continue;
                }

                int read = blockQueue.peek().read(b, off, len);

                off += read;
                len -= read;
                totalRead += read;
            }

            return totalRead;
        }
    }
}
