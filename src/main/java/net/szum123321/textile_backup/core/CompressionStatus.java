/*
 * A simple backup mod for Fabric
 *     Copyright (C) 2022  Szum123321
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.szum123321.textile_backup.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

public record CompressionStatus(long treeHash, LocalDateTime date, long startTimestamp, long finishTimestamp, Map<Path, Exception> brokenFiles) implements Serializable {
    public static final String DATA_FILENAME = "textile_status.data";
    public boolean isValid(long decompressedHash) { return true; }

    public static CompressionStatus readFromFile(Path folder) throws IOException, ClassNotFoundException {
        try(InputStream i = Files.newInputStream(folder.resolve(DATA_FILENAME));
            ObjectInputStream obj = new ObjectInputStream(i)) {
            return (CompressionStatus) obj.readObject();
        }
    }

    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream bo = new ByteArrayOutputStream();
             ObjectOutputStream o = new ObjectOutputStream(bo)) {
            o.writeObject(this);
            return bo.toByteArray();
        }
    }

}
