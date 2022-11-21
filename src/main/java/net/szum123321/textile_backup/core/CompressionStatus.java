package net.szum123321.textile_backup.core;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;

public record CompressionStatus(long[] treeHash, LocalDateTime date, long startTimestamp, long finishTimestamp, boolean ok, Path[] brokenFiles) implements Serializable {

    public static class Builder {
        public synchronized void update(Path path, long hash, Exception error) { throw new RuntimeException("UNIMPLEMENTED!"); }
        public synchronized void update(Path path, Exception error) { throw new RuntimeException("UNIMPLEMENTED!"); }
        public synchronized void update(Path path, long hash) { throw new RuntimeException("UNIMPLEMENTED!"); }

        public CompressionStatus build() { throw new RuntimeException("UNIMPLEMENTED!"); }
    }
}
