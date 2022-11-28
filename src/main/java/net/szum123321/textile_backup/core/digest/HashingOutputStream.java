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
import org.jetbrains.annotations.NotNull;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public class HashingOutputStream extends FilterOutputStream {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

    private final Path path;
    private final Hash hasher = Globals.CHECKSUM_SUPPLIER.get();
    private final FileTreeHashBuilder hashBuilder;

    private long cnt = 0;

    public HashingOutputStream(OutputStream out, Path path, FileTreeHashBuilder hashBuilder) {
        super(out);
        this.path = path;
        this.hashBuilder = hashBuilder;
    }

    @Override
    public void flush() throws IOException {
        //log.info("Called flush! {}", path);
        super.flush();
    }

    @Override
    public void write(int b) throws IOException {
        hasher.update(b);
        cnt++;
        super.write(b);
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) throws IOException {
        cnt += len;
        log.info("Called: {} with {}", path, len);
        hasher.update(b, off, len);
        super.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        long h = hasher.getValue();
        log.info("Read in: {}, of {}, with hash {}", path, cnt, h);
        hashBuilder.update(path, h);
        super.close();

    }
}
