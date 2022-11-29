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

package net.szum123321.textile_backup.core;

import net.szum123321.textile_backup.core.restore.RestoreContext;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

public record CompressionStatus(long treeHash, Map<Path, Exception> brokenFiles, LocalDateTime date, long startTimestamp, long finishTimestamp, String version) implements Serializable {
    public static final String DATA_FILENAME = "textile_status.data";
    public Optional<String> isValid(long hash, RestoreContext ctx) throws RuntimeException {
        if(hash != treeHash)
            return Optional.of("Tree Hash mismatch!\n  Expected: " + treeHash + ", got: " + hash);

        if(!brokenFiles.isEmpty())
            return Optional.of("Damaged files present! ^");

        if(ctx.restoreableFile().getCreationTime() != date)
            return Optional.of(
                    "Creation date mismatch!\n   Expected: " +
                            date.format(DateTimeFormatter.ISO_DATE_TIME) + ", got: " +
                            ctx.restoreableFile().getCreationTime().format(DateTimeFormatter.ISO_DATE_TIME)
            );

        return Optional.empty();
    }

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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Hash: ")
                .append(treeHash)
                .append(", Date: ")
                .append(date.format(DateTimeFormatter.ISO_DATE_TIME))
                .append(", start time stamp: ").append(startTimestamp)
                .append(", finish time stamp: ").append(finishTimestamp)
                .append(", Mod Version: ").append(version);

        builder.append(", broken files: ");
        if(brokenFiles.isEmpty()) builder.append("[]");
        else {
            builder.append("[\n");
            for(Path i: brokenFiles.keySet()) {
                builder.append(i.toString())
                        .append(":");

                ByteArrayOutputStream o = new ByteArrayOutputStream();
                brokenFiles.get(i).printStackTrace(new PrintStream(o));
                builder.append(o).append("\n");
            }
            builder.append("]");
        }

        return builder.toString();
    }
}