package net.szum123321.textile_backup.core.create.compressors.parallel_zip_fix;

import org.apache.commons.compress.parallel.ScatterGatherBackingStore;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * So the main issue with the {@link org.apache.commons.compress.parallel.FileBasedScatterGatherBackingStore} is that it
 * stores its results as files in tmpfs. In most cases it a good thing, as it allows for low system memory usage.
 * Sadly some Minecraft Server Providers limit the size of the folder which causes software to fail.
 *
 * This {@link ScatterGatherBackingStore } implementation should overcome this issue by storing data in tmp files, and if that fails,
 * it will switch to {@link MemoryBlockOutputStream}. This creates another issue as the system might run out of memory if too much data would be stored.
 */

public class FailsafeScatterGatherBackingStore implements ScatterGatherBackingStore {
    private final static String NO_SPACE_LEFT_ON_DEVICE_EXCEPTION_MESSAGE = "No space left on device";

    private final Path tmpdir;
    private final int id;
    private final Deque<DataChunk<?>> queue;
    private int fileCounter;
    private OutputStream os;
    private boolean closed;

    public FailsafeScatterGatherBackingStore(int id, Path tmpdir) throws IOException {
        this.tmpdir = tmpdir;
        this.id = id;
        queue = new ArrayDeque<>();
        //this.target = File.createTempFile("parallelscaterstore", String.valueOf(id), tmpdir.toFile());

        if(!tryAddingNewFileToQueue()) {
            queue.add(new MemoryBasedDataChunk());
            os = (OutputStream) queue.peek().getSource();
        } else {
            os = Files.newOutputStream(((File)queue.peek().getSource()).toPath());
        }
        
        /*try {
            os = Files.newOutputStream(target.toPath());
        } catch (IOException ex) {
            if(ex.getMessage().equals(NO_SPACE_LEFT_ON_DEVICE_EXCEPTION_MESSAGE )) {
                //Caught it!
                state = State.Memory;
                os = new MemoryBlockOutputStream();
                target.delete();
            } else {
                //No need to stay backwards-compatible with Compress 1.13
                throw ex;
            }
        }*/
    }

    private boolean tryAddingNewFileToQueue() throws IOException {
        try {
            queue.add(new FileBasedDataChunk(File.createTempFile("parallescatterstore-" + id, String.valueOf(fileCounter++))));
        } catch (IOException e) {
            if(e.getMessage().equals(NO_SPACE_LEFT_ON_DEVICE_EXCEPTION_MESSAGE)) {
                return false;
            } else {
                throw e;
            }
        }

        return true;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        ArrayList<InputStream> list = new ArrayList<>(queue.size());
        for(DataChunk<?> dataChunk: queue) list.add(dataChunk.getInputStream());
        return new SequenceInputStream(Collections.enumeration(list));
        /*if(state == State.MemoryBackup) {
            return new SequenceInputStream(
                    new SizeLimitedInputStream(safelyWritten, Files.newInputStream(target.toPath())),
                    ((MemoryBlockOutputStream)os).getInputStream()
            );
        } else if(state == State.Memory) {
            return ((MemoryBlockOutputStream)os).getInputStream();
        } else {
            return Files.newInputStream(target.toPath());
        }*/
    }

    @Override
    public void writeOut(byte[] data, int offset, int length) throws IOException {
        try {
            os.write(data, offset, length);
            queue.peekLast().size += length;
        }  catch (IOException e) {
            if(e.getMessage().equals(NO_SPACE_LEFT_ON_DEVICE_EXCEPTION_MESSAGE)) {
                //Caught it!
                queue.add(new MemoryBasedDataChunk());
                os = (OutputStream) queue.peek().getSource();
            } else {
                throw e;
            }
        }
        /*try {
            os.write(data, offset, length);
            safelyWritten += length;
        } catch (IOException e) {
            if(e.getMessage().equals(NO_SPACE_LEFT_ON_DEVICE_EXCEPTION_MESSAGE )) {
                //Caught it!
                state = State.MemoryBackup;
                os.close();
                os = new MemoryBlockOutputStream();
                os.write(data, offset, length);
            } else {
                throw e;
            }
        }*/
    }

    @Override
    public void closeForWriting() throws IOException {
        if (!closed) {
            os.close();
            closed = true;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            closeForWriting();
        } finally {
            queue.stream()
                    .filter(dataChunk -> dataChunk instanceof FileBasedDataChunk)
                    .map(dataChunk -> (File)dataChunk.getSource())
                    .filter(file -> file.exists() && !file.delete())
                    .forEach(File::deleteOnExit);
        }
    }

    private static abstract class DataChunk <T> {
        private long size;

        public DataChunk() {
            this.size = 0;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public abstract T getSource();

        public abstract InputStream getInputStream() throws IOException;
        public abstract OutputStream getOutputStream() throws IOException;
    }

    private static class FileBasedDataChunk extends DataChunk<File> {
        private final File file;

        public FileBasedDataChunk(File file) {
            this.file = file;
        }

        @Override
        public File getSource() {
            return file;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(file.toPath());
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return Files.newOutputStream(file.toPath());
        }
    }

    private static class MemoryBasedDataChunk extends DataChunk<MemoryBlockOutputStream> {
        private final MemoryBlockOutputStream memoryBlockOutputStream;

        public MemoryBasedDataChunk() {
            memoryBlockOutputStream = new MemoryBlockOutputStream();
        }

        @Override
        public MemoryBlockOutputStream getSource() {
            return memoryBlockOutputStream;
        }

        @Override
        public InputStream getInputStream() {
            return memoryBlockOutputStream.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() {
            return memoryBlockOutputStream;
        }
    }
}
