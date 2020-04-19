/* AT4J -- Archive file tools for Java -- http://www.at4j.org
 * Copyright (C) 2009 Karl Gustafsson
 *
 * This file is a part of AT4J
 *
 * AT4J is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * AT4J is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.at4j.support.io;

import java.io.Closeable;
import java.io.IOException;

/**
 * This interface identifies a sink for bits.
 * <p>
 * The sink is assumed to have a position which may or may not be at a byte
 * boundary (every eight bits).
 * <p>
 * If an implementing class also extends {@link java.io.OutputStream} it can be
 * used as an output stream. This interface redefines
 * {@link java.io.OutputStream}'s write methods with the extra condition that
 * they may only be used if the current position of the sink is at a byte
 * boundary. The {@link #writeBytes(byte[], int, int)} method does not have that
 * limitation.
 * @author Karl Gustafsson
 * @since 1.1
 * @see java.io.OutputStream
 * @see BitInput
 */
public interface BitOutput extends Closeable
{
	/**
	 * Pad the output with zeroes to the next byte boundary. If the current
	 * position is already at a byte boundary, this method does nothing.
	 * @throws IOException On I/O errors.
	 */
	void padToByteBoundary() throws IOException;

	/**
	 * Get the value of the unfinished byte. The value is shifted so that the
	 * least significant bit positions are used.
	 * {@link #getNumberOfBitsInUnfinishedByte()} returns how many bit positions
	 * that are used.
	 * <p>
	 * If the current position is at a byte boundary, 0 is returned.
	 * @return The value of the unfinished byte.
	 */
	int getUnfinishedByte();

	/**
	 * Get the number of bits that have been written to the last byte.
	 * <p>
	 * If the current position is at a byte boundary, 0 is returned.
	 * @return The number of bits that have been written to the last byte. This
	 * is a number between 0 and 7 (inclusive).
	 */
	int getNumberOfBitsInUnfinishedByte();

	/**
	 * Write a single bit.
	 * @param val The bit ({@code true == 1}, {@code false == 0}).
	 * @throws IOException On I/O errors.
	 */
	void writeBit(boolean val) throws IOException;

	/**
	 * Write up to eight bits.
	 * @param val The value to write. The bits written are the {@code no}
	 * rightmost bits of {@code val}. It is not verified that {@code val} fits
	 * within its {@code no} rightmost bits. If it does not, the written value
	 * is simply truncated.
	 * @param no The number of bits to write. This must be between 0 and 8
	 * (inclusive).
	 * @throws IndexOutOfBoundsException If {@code no} is less than 0 or greater
	 * than 8.
	 * @throws IOException On I/O errors
	 * @see #writeBitsLittleEndian(int, int)
	 */
	void writeBits(int val, int no) throws IndexOutOfBoundsException, IOException;

	/**
	 * Write up to 32 bits. The bits are written little endian with the most
	 * significant bit first.
	 * @param val The value to write. The bits written are the {@code no}
	 * rightmost bits of {@code val}. It is not verified that {@code val} fits
	 * within its {@code no} rightmost bits. If it does not, the written value
	 * is simply truncated.
	 * @param no The number of bits to write. This must be between 0 and 32
	 * (inclusive)
	 * @throws IndexOutOfBoundsException If {@code no} is less than 0 or more
	 * than 32.
	 * @throws IOException On I/O errors.
	 * @see #writeBits(int, int)
	 */
	void writeBitsLittleEndian(int val, int no) throws IndexOutOfBoundsException, IOException;

	/**
	 * Write an array of bytes to the output. Unlike
	 * {@link #write(byte[], int, int)}, this method does not require that the
	 * current position is at a byte boundary.
	 * @param barr The bytes to write.
	 * @param off The offset in the byte array.
	 * @param len The number of bytes to write.
	 * @throws IndexOutOfBoundsException If the offset or the length is negative
	 * or if the offset + length is larger than the byte array.
	 * @throws IOException On I/O errors
	 * @see #write(byte[], int, int)
	 */
	void writeBytes(byte[] barr, int off, int len) throws IndexOutOfBoundsException, IOException;

	/**
	 * See {@link java.io.OutputStream#write(int)}.
	 * <p>
	 * This method requires that the current position of the output is at a byte
	 * boundary.
	 * @param b The byte to write (0 - 255).
	 * @throws IOException On I/O errors or if the current position is not at a
	 * byte boundary.
	 * @see java.io.OutputStream#write(int)
	 */
	void write(int b) throws IOException;

	/**
	 * See {@link java.io.OutputStream#write(byte[])}.
	 * <p>
	 * This method requires that the current position of the output is at a byte
	 * boundary.
	 * @param barr The bytes to write.
	 * @throws IOException On I/O errors or if the current position is not at a
	 * byte boundary.
	 * @see java.io.OutputStream#write(byte[])
	 */
	void write(byte[] barr) throws IOException;

	/**
	 * See {@link java.io.OutputStream#write(byte[], int, int)}.
	 * <p>
	 * This method requires that the current position of the output is at a byte
	 * boundary.
	 * @param barr The bytes to write.
	 * @param off The offset in the byte array.
	 * @param len The number of bytes to write.
	 * @throws IndexOutOfBoundsException If the offset or the length is negative
	 * or if the offset + length is larger than the byte array.
	 * @throws IOException On I/O errors or if the current position is not at a
	 * byte boundary.
	 * @see java.io.OutputStream#write(byte[], int, int)
	 * @see #writeBytes(byte[], int, int)
	 */
	void write(byte[] barr, int off, int len) throws IndexOutOfBoundsException, IOException;
}
