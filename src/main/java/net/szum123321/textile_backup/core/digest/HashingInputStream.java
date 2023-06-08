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

package net.szum123321.textile_backup.core.digest;

import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.core.DataLeftException;
import net.szum123321.textile_backup.core.create.BrokenFileHandler;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * This class calculates a hash of the file on the input stream, submits it to FileTreeHashBuilder.
 * In case the underlying stream hasn't been read completely in, puts it into BrokeFileHandler

 * Furthermore, ParallelZip works by putting all the file requests into a queue and then compressing them
 * with multiple threads. Thus, we have to make sure that all the files have been read before requesting the final value
 * That is what CountDownLatch does
 */
public class HashingInputStream extends FilterInputStream {
    private final Path path;
    private final Hash hash = Globals.CHECKSUM_SUPPLIER.get();
    private final FileTreeHashBuilder hashBuilder;
    private final BrokenFileHandler brokenFileHandler;

    private long bytesWritten = 0;

    public HashingInputStream(InputStream in, Path path, FileTreeHashBuilder hashBuilder, BrokenFileHandler brokenFileHandler) {
        super(in);
        this.path = path;
        this.hashBuilder = hashBuilder;
        this.brokenFileHandler = brokenFileHandler;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        int i;
        try {
             i = in.read(b, off, len);
        } catch(IOException e) {
            throw new IOException("An exception occurred while trying to access: [" + path.toString() + "]", e);
        }
        if(i != -1) {
            hash.update(b, off, i);
            bytesWritten += i;
        }
        return i;
    }

    @Override
    public int read() throws IOException {
        int i;
        try {
            i = in.read();
        } catch(IOException e) {
            throw new IOException("An exception occurred while trying to access: [" + path.toString() + "]", e);
        }
        if(i != -1) {
            hash.update(i);
            bytesWritten++;
        }
        return i;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void close() throws IOException {
        hash.update(path.getFileName().toString().getBytes(StandardCharsets.UTF_8));

        hashBuilder.update(path, hash.getValue(), bytesWritten);

        if(in.available() != 0) brokenFileHandler.handle(path, new DataLeftException(in.available()));

        super.close();
    }
}
