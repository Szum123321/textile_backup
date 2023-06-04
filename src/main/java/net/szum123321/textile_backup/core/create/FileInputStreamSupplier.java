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

package net.szum123321.textile_backup.core.create;

import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.core.digest.FileTreeHashBuilder;
import net.szum123321.textile_backup.core.digest.HashingInputStream;

import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public record FileInputStreamSupplier(Path path, String name, FileTreeHashBuilder hashTreeBuilder, BrokenFileHandler brokenFileHandler) implements InputSupplier {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return new HashingInputStream(Files.newInputStream(path), path, hashTreeBuilder, brokenFileHandler);
        } catch (IOException e) {
            //Probably good idea to just put it here. In the case an exception is thrown here, it could be possible
            //The latch would have never been lifted
            hashTreeBuilder.update(path, 0, 0);
            brokenFileHandler.handle(path, e);
            throw e;
        }
    }

    @Override
    public Optional<Path> getPath() { return Optional.of(path); }

    @Override
    public long size() throws IOException { return Files.size(path); }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InputStream get() {
        try {
            return getInputStream();
        } catch (IOException e) {
            log.error("An exception occurred while trying to create an input stream from file: {}!", path.toString(), e);
        }

        return null;
    }
}
