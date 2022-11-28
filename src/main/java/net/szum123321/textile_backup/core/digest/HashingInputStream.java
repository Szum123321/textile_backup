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
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.core.DataLeftException;
import net.szum123321.textile_backup.core.create.BrokenFileHandler;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;

//This class calculates a hash of the file on the input stream, submits it to FileTreeHashBuilder.
//In case the underlying stream hasn't been read completely in, puts it into BrokeFileHandler
public class HashingInputStream extends FilterInputStream {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    private final Path path;
    private final Hash hasher = Globals.CHECKSUM_SUPPLIER.get();
    private final FileTreeHashBuilder hashBuilder;
    private final BrokenFileHandler brokenFileHandler;

    private int cnt = 0;

    @Override
    public synchronized void reset() throws IOException {
        log.info("Called reset! {}", path);
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    public HashingInputStream(InputStream in, Path path, FileTreeHashBuilder hashBuilder, BrokenFileHandler brokenFileHandler) {
        super(in);
        this.path = path;
        this.hashBuilder = hashBuilder;
        this.brokenFileHandler = brokenFileHandler;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        int i = in.read(b, off, len);
        if(i > -1) {
            hasher.update(b, off, i);
            cnt += i;
        }
        return i;
    }

    @Override
    public int read() throws IOException {
        int i = in.read();
        if(i > -1) {
            hasher.update(i);
            cnt++;
        }
        return i;
    }

    @Override
    public void close() throws IOException {
        if(in.available() == 0) {
            long val = hasher.getValue();
            hashBuilder.update(path, val);
            log.info("Read in {}, of {}, with hash {}", path, cnt, val);
        }
        else {
            brokenFileHandler.handle(path, new DataLeftException(in.available()));
            //log.info("bad file {} {}",path, cnt);
        }
        super.close();
    }
}
