/*
    A simple backup mod for Fabric
    Copyright (C) 2022  Szum123321

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

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
