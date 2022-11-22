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

package net.szum123321.textile_backup.core.restore;

import org.jetbrains.annotations.NotNull;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.zip.Checksum;

public class HashingOutputStream extends FilterOutputStream {
    private final Path path;
    private final Checksum hasher;

    public HashingOutputStream(OutputStream out, Path path, Checksum hasher) {
        super(out);
        this.path = path;
        this.hasher = hasher;
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);
        hasher.update(b);
    }

    @Override
    public void write(byte[] @NotNull b, int off, int len) throws IOException {
        super.write(b, off, len);
        hasher.update(b, off, len);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
