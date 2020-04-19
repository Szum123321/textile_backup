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
 * This interface identifies a source for bits.
 * <p>
 * The source is assumed to have a position which may or may not be at a byte
 * boundary (every eight bits).
 * <p>
 * If an implementing class also extends {@link java.io.InputStream} it can be
 * used as an input stream. This interface redefines {@link java.io.InputStream}
 * 's read methods with the extra condition that they may only be used if the
 * current position of the source is at a byte boundary. The
 * {@link #readBytes(byte[], int, int)} method does not have that limitation.
 * @author Karl Gustafsson
 * @since 1.1
 * @see java.io.InputStream
 * @see BitOutput
 */
public interface BitInput extends Closeable
{
	/**
	 * Has the input come to its end? If so, nothing more can be read from it.
	 * @return {@code true} if no more can be read from this input.
	 */
	boolean isAtEof();

	/**
	 * Move the position to the next byte boundary. If the current position is
	 * already at a byte boundary, this method does nothing.
	 * @throws IOException On I/O errors or if this input is already at the end
	 * of the available data.
	 */
	void skipToByteBoundary() throws IOException;

	/**
	 * Read the value of the next bit in the stream.
	 * @return {@code true} if the value is 1, {@code false} if it is 0.
	 * @throws IOException On I/O errors or if this input is already at the end
	 * of the available data.
	 */
	boolean readBit() throws IOException;

	/**
	 * Read up to eight bits from the input.
	 * @param no The number of bits to read.
	 * @return The bits as the least significant bits of the returned integer.
	 * For instance, if {@code 1011} is read, the returned integer will have the
	 * value {@code 1 * 8 + 0 * 4 + 1 * 2 + 1 * 1 == 11}.
	 * @throws IndexOutOfBoundsException If {@code no} is less than 0 or greater
	 * than 8.
	 * @throws IOException On I/O errors or if this input is already at the end
	 * of the available data.
	 * @see #readBitsLittleEndian(int)
	 */
	int readBits(int no) throws IndexOutOfBoundsException, IOException;

	/**
	 * Read up to 32 bits from the input. The first eight bits that is read will
	 * be the most significant byte of the returned integer.
	 * @param no The number of bits to read.
	 * @return The bits read as the least significant bits of the returned
	 * integer. (Just like for {@link #readBits(int)}.
	 * @throws IndexOutOfBoundsException If {@code no} is less than 0 or greater
	 * than 32.
	 * @throws IOException On I/O errors or if this input is already at the end
	 * of the available data.
	 * @see #readBits(int)
	 */
	int readBitsLittleEndian(int no) throws IndexOutOfBoundsException, IOException;

	/**
	 * Read bytes from the input. Unlike {@link #read(byte[], int, int)}, this
	 * method does not require that the current position is at a byte boundary.
	 * <p>
	 * Another difference to {@link #read(byte[], int, int)} is that this method
	 * throws an {@link IOException} if it cannot read all requested bytes.
	 * @param barr The byte array to read bytes into.
	 * @param off The offset in the array to start writing read bytes at.
	 * @param len The number of bytes to read.
	 * @return {@code barr}.
	 * @throws IndexOutOfBoundsException If the length or the offset is negative
	 * or if the sum of the length and the offset is greater than the length of
	 * the supplied byte array.
	 * @throws IOException On I/O errors or if there was not enough bytes to
	 * read from the input.
	 * @see #read(byte[], int, int)
	 */
	public byte[] readBytes(byte[] barr, int off, int len) throws IndexOutOfBoundsException, IOException;

	/**
	 * Read a single byte from the input. See {@link java.io.InputStream#read()}
	 * .
	 * <p>
	 * This method requires that the current position in the input is at a byte
	 * boundary.
	 * @return The read byte or {@code -1} if the current position is at the end
	 * of the input.
	 * @throws IOException On I/O errors or if the current position is not at a
	 * byte boundary.
	 * @see java.io.InputStream#read()
	 */
	int read() throws IOException;

	/**
	 * Read bytes into the supplied array. See
	 * {@link java.io.InputStream#read(byte[])}.
	 * <p>
	 * This method requires that the current position in the input is at a byte
	 * boundary.
	 * @param barr The byte array to read bytes into.
	 * @return The number of bytes read.
	 * @throws IOException On I/O errors or if the current position is not at a
	 * byte boundary.
	 * @see java.io.InputStream#read(byte[])
	 */
	int read(byte[] barr) throws IOException;

	/**
	 * Read bytes into the supplied array. See
	 * {@link java.io.InputStream#read(byte[], int, int)}.
	 * <p>
	 * This method requires that the current position in the input is at a byte
	 * boundary.
	 * @param barr The byte array to read bytes into.
	 * @param offset The offset position in the array to start write read bytes
	 * to.
	 * @param len The number of bytes to read.
	 * @return The number of bytes actually read.
	 * @throws IndexOutOfBoundsException If the offset or the length is negative
	 * or if the sum of the offset and the length is greater than the length of
	 * the supplied byte array.
	 * @throws IOException On I/O errors or if the current position is not at a
	 * byte boundary.
	 */
	int read(byte[] barr, int offset, int len) throws IndexOutOfBoundsException, IOException;

	/**
	 * Skip bytes in the input. See {@link java.io.InputStream#skip(long)}.
	 * <p>
	 * This method requires that the current position in the input is at a byte
	 * boundary.
	 * @param n The number of bytes to skip.
	 * @return The number of bytes skipped.
	 * @throws IOException On I/O errors or if the current position is not at a
	 * byte boundary.
	 */
	long skip(long n) throws IOException;

	/**
	 * Get the number of bytes available in the input. See
	 * {@link java.io.InputStream#available()}.
	 * <p>
	 * This method requires that the current position in the input is at a byte
	 * boundary.
	 * @throws IOException On I/O errors or if the current position is not at a
	 * byte boundary.
	 */
	int available() throws IOException;
}
