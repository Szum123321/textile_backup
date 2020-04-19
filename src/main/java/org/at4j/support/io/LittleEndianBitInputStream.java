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
import java.io.InputStream;

/**
 * This is an input stream that a client can use to read single or several bits
 * from an underlying {@link InputStream}. The bits are read in little-endian
 * bit order.
 * @author Karl Gustafsson
 * @since 1.1
 */
public class LittleEndianBitInputStream extends InputStream implements BitInput
{
	// 2^0
	private static final int POINTER_START_OF_BYTE = 0;
	// 2^7
	private static final int POINTER_END_OF_BYTE = 7;

	private final InputStream m_in;

	// The current byte
	private int m_curByte;
	// The pointer to the current bit location in the current byte.
	private int m_pointerInByte = POINTER_START_OF_BYTE;

	private long m_numberOfBytesRead = 0;

	public LittleEndianBitInputStream(InputStream in) throws IOException
	{
		// Null check
		in.getClass();

		m_in = in;
		m_curByte = in.read();
		// Don't increment the number of read bytes counter. It is always one
		// byte behind.
	}

	private int readByte() throws IOException
	{
		int res = m_in.read();
		m_numberOfBytesRead += res != -1 ? 1 : 0;
		return res;
	}

	private void incrementPointerPosition() throws IOException
	{
		if (m_pointerInByte == POINTER_END_OF_BYTE)
		{
			// Read a new byte
			m_curByte = readByte();
			m_pointerInByte = POINTER_START_OF_BYTE;
		}
		else
		{
			// Increment the pointer only if we're not at EOF
			if (!isAtEof())
			{
				m_pointerInByte++;
			}
		}
	}

	public boolean isAtEof()
	{
		return m_curByte == -1;
	}

	/**
	 * Get the number of whole bytes read this far.
	 * @return The number of bytes read this far.
	 */
	public long getNumberOfBytesRead()
	{
		return m_numberOfBytesRead;
	}

	private void assertNotAtEOF() throws IOException
	{
		if (isAtEof())
		{
			throwIOException("At EOF");
		}
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

	private void throwIOException(String msg, long pos) throws IOException
	{
		throw new IOException(msg + ". Position in stream: " + pos);
	}

	private void throwIOException(String msg) throws IOException
	{
		throw new IOException(msg + ". Position in stream: " + m_numberOfBytesRead);
	}

	public void skipToByteBoundary() throws IOException
	{
		assertNotAtEOF();
		if (m_pointerInByte != POINTER_START_OF_BYTE)
		{
			m_pointerInByte = POINTER_START_OF_BYTE;
			m_curByte = readByte();
		}
	}

	public boolean readBit() throws IOException
	{
		assertNotAtEOF();
		boolean res = (m_curByte & (1 << (7 - m_pointerInByte))) > 0;
		incrementPointerPosition();
		return res;
	}

	public int readBits(int no) throws IOException, IndexOutOfBoundsException
	{
		if (no < 0 || no > 8)
		{
			throw new IndexOutOfBoundsException("Invalid number of bits: " + no + ". Must be between 0 and 8 (inclusive)");
		}
		assertNotAtEOF();

		if (no == 0)
		{
			return 0;
		}

		// Bytes are stored little bit endian
		if (no + m_pointerInByte <= 8)
		{
			// All bits to read fit in the current byte
			int res = (m_curByte >> (8 - no - m_pointerInByte)) & ((1 << no) - 1);
			m_pointerInByte += no;
			if (m_pointerInByte > POINTER_END_OF_BYTE)
			{
				m_curByte = readByte();
				m_pointerInByte = POINTER_START_OF_BYTE;
			}
			return res;
		}
		else
		{
			// Read remaining bits + first bits of next byte
			int noToReadInByte2 = no - (8 - m_pointerInByte);
			int res = (m_curByte & ((1 << (8 - m_pointerInByte)) - 1)) << noToReadInByte2;
			m_curByte = readByte();
			assertNotAtEOF();
			m_pointerInByte = noToReadInByte2;
			res += m_curByte >> (8 - noToReadInByte2);
			return res;
		}
	}

	public int readBitsLittleEndian(int no) throws IOException, IndexOutOfBoundsException
	{
		if (no < 0 || no > 32)
		{
			throw new IndexOutOfBoundsException("Invalid number of bits: " + no + ". Must be between 0 and 32 (inclusive)");
		}

		if (no == 0)
		{
			return 0;
		}

		int noReads = no / 8;
		int mod = no % 8;
		int res = 0;
		if (mod != 0)
		{
			res = readBits(mod) << (noReads * 8);
		}
		for (int i = 0; i < noReads; i++)
		{
			res += readBits(8) << ((noReads - i - 1) * 8);
		}
		return res;
	}

	public byte[] readBytes(byte[] barr, int off, int len) throws IOException, IndexOutOfBoundsException
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

		assertNotAtEOF();

		if (len == 0)
		{
			return barr;
		}

		if (isAtByteBoundary())
		{
			// Special case: we are at the byte boundary. We just have to read
			// the len next bytes and return them.
			// The read method takes care of updating all internal state.
			int noRead = read(barr, off, len);
			if (noRead != len)
			{
				throwIOException("Unexpected EOF. Wanted to read " + len + " bytes. Got " + noRead, m_numberOfBytesRead - noRead);
			}
		}
		else
		{
			int noRead = m_in.read(barr, off, len);
			m_numberOfBytesRead += noRead;
			if (noRead != len)
			{
				m_curByte = -1;
				m_pointerInByte = POINTER_START_OF_BYTE;
				throwIOException("Unexpected EOF. Wanted to read " + len + " bytes. Got " + noRead, m_numberOfBytesRead - noRead);
			}

			// Shift bytes in the result array. Bytes are stored little (bit-)
			// endian.
			int lastByte = m_curByte;
			m_curByte = barr[off + len - 1] & 0xFF;
			// The distance to shift the second byte to the right.
			int rightShiftDistance = 8 - m_pointerInByte;
			for (int i = off; i < off + len; i++)
			{
				int newLastByte = barr[i];
				barr[i] = (byte) (((lastByte << m_pointerInByte) | ((barr[i] & 0xFF) >>> rightShiftDistance)) & 0xFF);
				lastByte = newLastByte;
			}
		}
		return barr;
	}

	@Override
	public int read() throws IOException
	{
		assertAtByteBoundary();
		int res = m_curByte;
		if (m_curByte != -1)
		{
			m_curByte = readByte();
		}
		return res;
	}

	@Override
	public int read(byte[] barr) throws IOException
	{
		return read(barr, 0, barr.length);
	}

	@Override
	public int read(byte[] barr, int offset, int len) throws IndexOutOfBoundsException, IOException
	{
		if (offset < 0)
		{
			throw new IndexOutOfBoundsException("Illegal offset: " + offset);
		}
		else if (len < 0)
		{
			throw new IndexOutOfBoundsException("Illegal length: " + len);
		}
		else if ((offset + len) > barr.length)
		{
			throw new IndexOutOfBoundsException("Illegal offset + length: " + offset + " + " + len + ". Longer than the byte array: " + barr.length);
		}

		assertAtByteBoundary();
		if (isAtEof())
		{
			return -1;
		}
		else
		{
			barr[offset] = (byte) m_curByte;
			int res = 1;
			if (len > 1)
			{
				int noRead = m_in.read(barr, offset + 1, len - 1);
				if (noRead > 0)
				{
					res += noRead;
					m_numberOfBytesRead += noRead;
				}
			}
			m_curByte = readByte();
			return res;
		}
	}

	@Override
	public long skip(long n) throws IOException
	{
		assertAtByteBoundary();
		if (n <= 0L)
		{
			return 0L;
		}
		else
		{
			if (isAtEof())
			{
				return 0L;
			}

			if (n > 1L)
			{
				long noToSkip = n - 1L;
				long noSkipped = m_in.skip(noToSkip);
				m_numberOfBytesRead += noSkipped;
				if (noSkipped < noToSkip)
				{
					// At EOF
					m_curByte = -1;
					return noSkipped + 1;
				}
				else
				{
					m_curByte = readByte();
					return noSkipped + 1;
				}
			}
			else
			{
				m_curByte = readByte();
				return 1L;
			}
		}
	}

	@Override
	public int available() throws IOException
	{
		assertAtByteBoundary();
		return m_in.available() + m_curByte != -1 ? 1 : 0;
	}

	@Override
	public void close() throws IOException
	{
		m_in.close();
	}
}
