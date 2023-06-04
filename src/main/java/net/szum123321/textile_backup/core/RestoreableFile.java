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

import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.config.ConfigPOJO;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

/**
 * This class parses backup files, extracting its creation time, format and possibly comment
 */
public class RestoreableFile implements Comparable<RestoreableFile> {
    private final Path file;
    private final ConfigPOJO.ArchiveFormat archiveFormat;
    private final LocalDateTime creationTime;
    private final String comment;

    private RestoreableFile(Path file, ConfigPOJO.ArchiveFormat archiveFormat, LocalDateTime creationTime, String comment) {
        this.file = file;
        this.archiveFormat = archiveFormat;
        this.creationTime = creationTime;
        this.comment = comment;
    }

    //removes repetition of the files stream thingy with awfully large lambdas
    public static <T> T applyOnFiles(Path root, T def, Consumer<IOException> errorConsumer, Function<Stream<RestoreableFile>, T> streamConsumer) {
        try (Stream<Path> stream = Files.list(root)) {
            return streamConsumer.apply(stream.flatMap(f -> RestoreableFile.build(f).stream()));
        } catch (IOException e) {
            errorConsumer.accept(e);
        }
        return def;
    }

    public static Optional<RestoreableFile> build(Path file) throws NoSuchElementException {
        if(!Files.exists(file) || !Files.isRegularFile(file)) return Optional.empty();

        String filename = file.getFileName().toString();

        var format = Arrays.stream(ConfigPOJO.ArchiveFormat.values())
                .filter(f -> filename.endsWith(f.getCompleteString()))
                .findAny()
                .orElse(null);

        if(Objects.isNull(format)) return Optional.empty();

        int parsed_pos = filename.length() - format.getCompleteString().length();

        String comment = null;

        if(filename.contains("#")) {
            comment = filename.substring(filename.indexOf("#") + 1, parsed_pos);
            parsed_pos -= comment.length() + 1;
        }

        var time_string = filename.substring(0, parsed_pos);

        try {
            return Optional.of(new RestoreableFile(file, format, LocalDateTime.from(Utilities.getDateTimeFormatter().parse(time_string)), comment));
        } catch (Exception ignored) {}

        try {
            return Optional.of(new RestoreableFile(file, format, LocalDateTime.from(Globals.defaultDateTimeFormatter.parse(time_string)), comment));
        } catch (Exception ignored) {}

        try {
            FileTime fileTime = Files.readAttributes(file, BasicFileAttributes.class, NOFOLLOW_LINKS).creationTime();
            return Optional.of(new RestoreableFile(file, format, LocalDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.systemDefault()), comment));
        } catch (IOException ignored) {}

        return Optional.empty();
    }

    public Path getFile() { return file; }

    public ConfigPOJO.ArchiveFormat getArchiveFormat() { return archiveFormat; }

     public LocalDateTime getCreationTime() { return creationTime; }

    public Optional<String> getComment() { return Optional.ofNullable(comment); }

    @Override
    public int compareTo(@NotNull RestoreableFile o) { return creationTime.compareTo(o.creationTime); }

    public String toString() {
        return this.getCreationTime().format(Globals.defaultDateTimeFormatter) + (comment != null ? "#" + comment : "");
    }
}
