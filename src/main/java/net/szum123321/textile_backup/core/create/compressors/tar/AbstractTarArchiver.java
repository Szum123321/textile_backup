/*
 * A simple backup mod for Fabric
 * Copyright (C) 2020  Szum123321
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

package net.szum123321.textile_backup.core.create.compressors.tar;

import net.szum123321.textile_backup.core.create.BackupContext;
import net.szum123321.textile_backup.core.create.compressors.AbstractCompressor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractTarArchiver extends AbstractCompressor {

    protected abstract OutputStream getCompressorOutputStream(OutputStream stream, BackupContext ctx, int coreLimit) throws IOException;

    @Override
    protected OutputStream createArchiveOutputStream(OutputStream stream, BackupContext ctx, int coreLimit) throws IOException {
        TarArchiveOutputStream tar = new TarArchiveOutputStream(getCompressorOutputStream(stream, ctx, coreLimit));
        tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

        return tar;
    }

    @Override
    protected void addEntry(File file, String entryName, OutputStream arc) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)){
            TarArchiveEntry entry = (TarArchiveEntry)((TarArchiveOutputStream) arc).createArchiveEntry(file, entryName);
            ((TarArchiveOutputStream)arc).putArchiveEntry(entry);

            IOUtils.copy(fileInputStream, arc);

            ((TarArchiveOutputStream)arc).closeArchiveEntry();
        }
    }
}