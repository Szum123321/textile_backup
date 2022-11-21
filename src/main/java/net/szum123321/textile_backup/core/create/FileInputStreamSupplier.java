package net.szum123321.textile_backup.core.create;

import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.core.CompressionStatus;

import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;

public record FileInputStreamSupplier(Path path, String name, CompressionStatus.Builder builder) implements InputSupplier {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            //TODO: put in hasher
            return new HashingInputStream(Files.newInputStream(path), path, null, builder);
        } catch (IOException e) {
            builder.update(path, e);
            throw e;
        }
    }

    @Override
    public Path getPath() {
        return path;
    }

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
