package net.szum123321.textile_backup.core.create.compressors.parallel_zip_fix;

import org.jetbrains.annotations.NotNull;

import java.io.*;

public class SizeLimitedInputStream extends FilterInputStream {
    //private final int maxSize;
    private int dataLeft;

    public SizeLimitedInputStream(int maxSize, InputStream inputStream) {
        super(inputStream);
        //this.maxSize = maxSize;
        this.dataLeft = maxSize;
    }

    @Override
    public int read() throws IOException {
        if(dataLeft == 0) return -1;
        int read = super.read();

        if(read != -1) dataLeft--;
        return read;
    }

    @Override
    public int available() throws IOException {
        return Math.min(dataLeft, super.available());
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        if(dataLeft == 0) return -1;

        int read = super.read(b, off, Math.min(dataLeft, len));

        if(read != -1) dataLeft -= read;

        return read;
    }
}
