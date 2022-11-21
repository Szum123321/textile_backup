package net.szum123321.textile_backup.core.create;

import org.apache.commons.compress.parallel.InputStreamSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface InputSupplier extends InputStreamSupplier {
    InputStream getInputStream() throws IOException;
    Path getPath();

    String getName();
}
