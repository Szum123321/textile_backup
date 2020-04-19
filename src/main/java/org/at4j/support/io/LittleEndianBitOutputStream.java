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

import java.io.IOException;
import java.io.OutputStream;

/**
 * This is an {@link OutputStream} that implements {@link BitOutput} and hence
 * can be used to write individual bits to the output. The bits are stored in
 * little-endian order.
 * @author Karl Gustafsson
 * @since 1.1
 */
public class LittleEndianBitOutputStream extends OutputStream implements BitOutput
{
	// 2^0
	private static final int POINTER_START_OF_BYTE = 0;
	// 2^7
	private static final int POINTER_END_OF_BYTE = 7;

	private final OutputStream m_out;

	// The current byte
	private int m_curByte = 0;
	// The pointer to the current bit location in the current byte.
	private int m_pointerInByte = POINTER_START_OF_BYTE;

	private long m_numberOfBytesWritten = 0;

	public LittleEndianBitOutputStream(OutputStream wrapped)
	{
		// Null check
		wrapped.getClass();

		m_out = wrapped;
	}

	private boolean isAtByteBoundary()
	{
		return m_pointerInByte == POINTER_START_OF_BYTE;
	}

	private void assertAtByteBoundary() throws IOException
	{
		if (!isAtByteBoundary())
		{
			throwIOException("Not at byte boundary. Position: pos=" + m_pointerInByte);
		}
	}

	private void throwIOException(String msg) throws IOException
	{
		throw new IOException(msg + ". Position in stream: " + m_numberOfBytesWritten);
	}

	private void writeCurByte() throws IOException
	{
		m_out.write(m_curByte);
		m_numberOfBytesWritten++;
		m_pointerInByte = POINTER_START_OF_BYTE;
		m_curByte = 0;
	}

	/**
	 * Get the total number of whole <i>bytes</i> written by this stream so far.
	 * @return The number of whole bytes written.
	 */
	public long getNumberOfBytesWritten()
	{
		return m_numberOfBytesWritten;
	}

	public int getUnfinishedByte()
	{
		return m_pointerInByte > 0 ? m_curByte >>> (7 - (m_pointerInByte - 1)) : 0;
	}

	public int getNumberOfBitsInUnfinishedByte()
	{
		return m_pointerInByte;
	}

	public void padToByteBoundary() throws IOException
	{
		if (m_pointerInByte > POINTER_START_OF_BYTE)
		{
			writeCurByte();
		}
	}

	public void writeBit(boolean val) throws IOException
	{
		if (val)
		{
			m_curByte = m_curByte | 1 << (7 - m_pointerInByte);
		}
		m_pointerInByte++;

		if (m_pointerInByte > POINTER_END_OF_BYTE)
		{
			// Write the current byte and start a new one
			writeCurByte();
		}
	}

	public void writeBits(int val, int no) throws IOException, IndexOutOfBoundsException
	{
		if (no < 0 || no > 8)
		{
			throw new IndexOutOfBoundsException("Invalid number of bits " + no + ". Must be between 0 and 8 (inclusive)");
		}

		if (no == 0)
		{
			return;
		}

		if (m_pointerInByte + no <= 8)
		{
			// All bits to write fit in the current byte
			m_curByte = m_curByte | ((val & ((1 << no) - 1)) << (8 - m_pointerInByte - no));
			m_pointerInByte += no;
			if (m_pointerInByte > POINTER_END_OF_BYTE)
			{
				writeCurByte();
			}
		}
		else
		{
			// Bits will have to be written in the next byte too
			int bitsToWriteInCurByte = 8 - m_pointerInByte;
			int bitsToWriteInNextByte = no - bitsToWriteInCurByte;
			m_curByte = m_curByte | (val >>> (no - bitsToWriteInCurByte));
			writeCurByte();
			m_curByte = (val & ((1 << bitsToWriteInNextByte) - 1)) << (8 - bitsToWriteInNextByte);
			m_pointerInByte = bitsToWriteInNextByte;
		}
	}

	public void writeBitsLittleEndian(int val, int no) throws IndexOutOfBoundsException, IOException
	{
		if (no < 0 || no > 32)
		{
			throw new IndexOutOfBoundsException("Invalid number of bits to write " + no + ". It must be between 0 and 32 (inclusive)");
		}

		if (no == 0)
		{
			return;
		}

		int noWrites = no / 8;
		int mod = no % 8;
		if (mod != 0)
		{
			writeBits(val >>> (noWrites * 8), mod);
		}
		for (int i = 0; i < noWrites; i++)
		{
			writeBits(val >>> ((noWrites - i - 1) * 8), 8);
		}
	}

	public void writeBytes(byte[] barr, int off, int len) throws IndexOutOfBoundsException, IOException
	{
		if (off < 0)
		{
			throw new IndexOutOfBoundsException("Invalid offset " + off + ". It must be >= 0");
		}
		if (len < 0)
		{
			throw new IndexOutOfBoundsException("Invalid length " + len + ". It must be >= 0");
		}
		if (off + len > barr.length)
		{
			throw new IndexOutOfBoundsException("Invalid offset + length (" + off + " + " + len + "). It must be <= the length of the supplied array (" + barr.length + ")");
		}

		if (len == 0)
		{
			return;
		}

		if (isAtByteBoundary())
		{
			// Special case
			m_out.write(barr, off, len);
			m_numberOfBytesWritten += len;
		}
		else
		{
			// Copy the bytes to write to a new array. We cannot modify barr,
			// even if it is tempting.
			byte[] toWrite = new byte[len];
			System.arraycopy(barr, off, toWrite, 0, len);

			int prevByte = m_curByte;
			int leftShiftDistance = 8 - m_pointerInByte;
			for (int i = 0; i < len; i++)
			{
				// Shift in bits from the previous byte and shift out bytes
				// from this byte
				int nextPrevByte = (toWrite[i] & 0xFF) << leftShiftDistance;
				toWrite[i] = (byte) ((prevByte | ((toWrite[i] & 0xFF) >>> m_pointerInByte)) & 0xFF);
				prevByte = nextPrevByte;
			}
			m_curByte = prevByte & 0xFF;
			m_out.write(toWrite);
			m_numberOfBytesWritten += len;
		}
	}

	@Override
	public void write(int b) throws IOException
	{
		assertAtByteBoundary();
		m_out.write(b);
		m_numberOfBytesWritten++;
	}

	@Override
	public void write(byte[] barr) throws IOException
	{
		write(barr, 0, barr.length);
	}

	@Override
	public void write(byte[] barr, int off, int len) throws IOException
	{
		assertAtByteBoundary();
		m_out.write(barr, off, len);
		m_numberOfBytesWritten += len;
	}

	/**
	 * Close the output stream.
	 * <p>
	 * This method does not automatically pad the last written bits to a full
	 * byte. If there are bits written to it the stream must be padded before
	 * closing it. See {@link #padToByteBoundary()}.
	 */
	@Override
	public void close() throws IOException
	{
		m_out.close();
		super.close();
	}
}
