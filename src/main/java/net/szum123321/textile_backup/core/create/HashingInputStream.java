package net.szum123321.textile_backup.core.create;

import net.szum123321.textile_backup.core.CompressionStatus;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.Checksum;

public class HashingInputStream extends FilterInputStream {

    private final Path path;
    private final Checksum hasher;
    private final CompressionStatus.Builder statusBuilder;

    public HashingInputStream(InputStream in, Path path, Checksum hasher, CompressionStatus.Builder statusBuilder) {
        super(in);
        this.hasher = hasher;
        this.statusBuilder = statusBuilder;
        this.path = path;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        int i = in.read(b, off, len);
        if(i > -1) hasher.update(b, off, i);
        return i;
    }

    @Override
    public int read() throws IOException {
        int i = in.read();
        if(i > -1) hasher.update(i);
        return i;
    }

    @Override
    public void close() throws IOException {
        if(in.available() == 0) statusBuilder.update(path, hasher.getValue());
        else statusBuilder.update(path, hasher.getValue(), new RuntimeException("AAAaa"));
        super.close();
    }
}
