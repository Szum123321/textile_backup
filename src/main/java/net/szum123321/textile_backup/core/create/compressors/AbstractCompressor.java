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

package net.szum123321.textile_backup.core.create.compressors;

import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.core.*;
import net.szum123321.textile_backup.core.create.BrokenFileHandler;
import net.szum123321.textile_backup.core.create.ExecutableBackup;
import net.szum123321.textile_backup.core.create.FileInputStreamSupplier;
import net.szum123321.textile_backup.core.create.InputSupplier;
import net.szum123321.textile_backup.core.digest.FileTreeHashBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * Basic abstract class representing directory compressor with all the bells and whistles
 */
public abstract class AbstractCompressor {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);

    public void createArchive(Path inputFile, Path outputFile, ExecutableBackup ctx, int coreLimit) throws IOException, ExecutionException, InterruptedException {
        Instant start = Instant.now();

        BrokenFileHandler brokenFileHandler = new BrokenFileHandler(); //Basically a hashmap storing files and their respective exceptions

        try (OutputStream outStream = Files.newOutputStream(outputFile);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outStream);
             OutputStream arc = createArchiveOutputStream(bufferedOutputStream, ctx, coreLimit);
             Stream<Path> fileStream = Files.walk(inputFile)) {

            var fileList = fileStream
                    .filter(path -> !Utilities.isBlacklisted(inputFile.relativize(path)))
                    .filter(Files::isRegularFile)
                    .toList();

            FileTreeHashBuilder fileHashBuilder = new FileTreeHashBuilder(fileList.size());

            for (Path file : fileList) {
                try {
                    addEntry(
                            new FileInputStreamSupplier(
                                    file,
                                    inputFile.relativize(file).toString(),
                                    fileHashBuilder,
                                    brokenFileHandler),
                            arc
                    );
                } catch (IOException e) {
                    brokenFileHandler.handle(file, e);
                    fileHashBuilder.update(file, 0, 0);
                    //In Permissive mode we allow partial backups
                    if (ConfigHelper.INSTANCE.get().integrityVerificationMode.isStrict()) throw e;
                    else log.sendErrorAL(ctx, "An exception occurred while trying to compress: {}",
                            inputFile.relativize(file).toString(), e
                    );
                }
            }

            arc.flush();

            //wait for all the InputStreams to close/fail with InputSupplier

            Instant now = Instant.now();

            long treeHash = fileHashBuilder.getValue(true);
            CompressionStatus status = new CompressionStatus (
                    treeHash,
                    brokenFileHandler.get(),
                    ctx.startDate(), start.toEpochMilli(), now.toEpochMilli(),
                    Globals.INSTANCE.getCombinedVersionString()
            );

            addEntry(new StatusFileInputSupplier(status.serialize()), arc);

            finish(arc);
        } finally {
            close();
        }

        log.sendInfoAL(ctx, "Compression took: {} seconds.", Utilities.formatDuration(Duration.between(start, Instant.now())));
    }

    protected abstract OutputStream createArchiveOutputStream(OutputStream stream, ExecutableBackup ctx, int coreLimit) throws IOException;
    protected abstract void addEntry(InputSupplier inputSupplier, OutputStream arc) throws IOException;

    protected void finish(OutputStream arc) throws InterruptedException, ExecutionException, IOException {
        //This function is only needed for the ParallelZipCompressor to write out ParallelScatterZipCreator
    }

    protected void close() {
        //Same as above, just for ParallelGzipCompressor to shut down ExecutorService
    }

    private record StatusFileInputSupplier(byte[] data) implements InputSupplier {
        public InputStream getInputStream() { return new ByteArrayInputStream(data); }

        public Optional<Path> getPath() { return Optional.empty(); }

        public String getName() { return CompressionStatus.DATA_FILENAME; }

        public long size() { return data.length; }

        public InputStream get() { return getInputStream(); }
    }
 }
