package net.szum123321.textile_backup.core.create.compressors.parallel_zip_fix;

import org.apache.commons.compress.parallel.ScatterGatherBackingStore;
import sun.security.action.GetPropertyAction;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

public class CompositeScatterGatherBackingStore implements ScatterGatherBackingStore {
    private final static String NO_SPACE_LEFT_ON_DEVICE_EXCEPTION_MESSAGE = "No space left on device";
    private final static Path mainTmpPath = Paths.get(GetPropertyAction.privilegedGetProperty("java.io.tmpdir"));

    private final static AtomicInteger TMP_FILE_COUNTER = new AtomicInteger(0);
    private static Path localTmpPath;

    private final File mainTarget;
    private long mainBytesWritten = 0;
    private File localTarget = null;
    private OutputStream os;

    public CompositeScatterGatherBackingStore(Path localTmpPath) throws IOException {
        this.localTmpPath = localTmpPath;

        mainTarget = mainTmpPath.resolve("scatter_storage_" + TMP_FILE_COUNTER.getAndIncrement()).toFile();
        mainTarget.createNewFile();
        //mainTmpFile.deleteOnExit();

        os = Files.newOutputStream(mainTarget.toPath());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if(localTarget == null)
            return new SequenceInputStream(
                    new SizeLimitedInputStream((int) mainBytesWritten, Files.newInputStream(mainTarget.toPath())),
                    Files.newInputStream(localTarget.toPath())
            );

        return Files.newInputStream(mainTarget.toPath());
    }

    @Override
    public void writeOut(byte[] data, int offset, int length) throws IOException {
        try {
            os.write(data, offset, length);
            if(localTarget == null) mainBytesWritten += length;
        } catch (IOException e) {
            if(e.getMessage().equals(NO_SPACE_LEFT_ON_DEVICE_EXCEPTION_MESSAGE)) {
                if(localTarget == null) {
                    os.close();

                    localTarget = localTmpPath.resolve(mainTarget.getName()).toFile();
                    localTarget.createNewFile();
                    //localTmpFile.deleteOnExit();
                    os = Files.newOutputStream(localTarget.toPath());
                    os.write(data, offset, length);
                }
            } else {
                throw e;
            }
        }
    }

    @Override
    public void closeForWriting() throws IOException {
        os.close();
    }

    @Override
    public void close() throws IOException {
        if(mainTarget.exists() && !mainTarget.delete()) mainTarget.deleteOnExit();
        if(localTarget != null && localTarget.exists() && !localTarget.delete()) localTarget.deleteOnExit();
    }
    /*
    public static void setMemoryStorageSize(long size) {
        MAX_MEMORY_STORAGE_SIZE = size;
    }

    public static void resetMemoryStorage() {
        MEMORY_SPACE_USED.set(MAX_MEMORY_STORAGE_SIZE);
    }
     */
}
