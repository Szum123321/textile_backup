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
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import java.io.*;

public class LZMACompressor extends AbstractTarArchiver {
	public static LZMACompressor getInstance() {
		return new LZMACompressor();
	}

	@Override
	protected OutputStream getCompressorOutputStream(OutputStream stream, BackupContext ctx, int coreLimit) throws IOException {
		return new XZCompressorOutputStream(stream);
	}
}