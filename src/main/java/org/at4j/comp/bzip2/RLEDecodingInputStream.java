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
package org.at4j.comp.bzip2;

import java.io.IOException;
import java.io.InputStream;

/**
 * This stream run length decodes read data. It is used by the
 * {@link BZip2InputStream}.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class RLEDecodingInputStream extends InputStream
{
	private static enum RLEState
	{
		READING, REPEATING, ABOUT_TO_READ_HOW_MANY_TO_REPEAT, EOF;
	}

	// Block checksum calculated while reading the block contents.
	private final CRC m_blockChecksum = new CRC();
	private final InputStream m_wrapped;
	private final long m_readChecksum;

	private RLEState m_state;

	private int m_noLeftToRepeat;
	private int m_last;
	private int m_numberOfSimilar;

	RLEDecodingInputStream(InputStream wrapped, long readChecksum)
	{
		m_wrapped = wrapped;
		m_readChecksum = readChecksum;
		m_state = RLEState.READING;
		m_numberOfSimilar = 0;
		m_last = -1;
	}

	private void handleEof() throws IOException
	{
		if (m_blockChecksum.getValue() != m_readChecksum)
		{
			throw new IOException("Invalid block checksum. Was " + m_blockChecksum.getValue() + ", expected " + m_readChecksum);
		}
	}

	@Override
	public int read() throws IOException
	{
		switch (m_state)
		{
			case EOF:
				return -1;

			case READING:
				int val = m_wrapped.read();
				if (val == -1)
				{
					m_state = RLEState.EOF;
					handleEof();
					return -1;
				}
				if (val == m_last)
				{
					m_numberOfSimilar++;
					if (m_numberOfSimilar == 4)
					{
						// Four in a row. The next value is a repeat number.
						m_state = RLEState.ABOUT_TO_READ_HOW_MANY_TO_REPEAT;
						m_numberOfSimilar = 0;
					}
				}
				else
				{
					m_numberOfSimilar = 1;
					m_last = val;
				}
				m_blockChecksum.update(val);
				return val;

			case ABOUT_TO_READ_HOW_MANY_TO_REPEAT:
				m_noLeftToRepeat = m_wrapped.read();
				if (m_noLeftToRepeat == -1)
				{
					// A rather unexpected EOF
					m_state = RLEState.EOF;
					handleEof();
					return -1;
				}
				else if (m_noLeftToRepeat == 0)
				{
					// Nothing to repeat. Go on to read the next value.
					m_state = RLEState.READING;
					return read();
				}
				else
				{
					m_state = RLEState.REPEATING;
					m_noLeftToRepeat--;
					if (m_noLeftToRepeat == 0)
					{
						// Just one to repeat, which we will do in this call.
						m_state = RLEState.READING;
					}
					m_blockChecksum.update(m_last);
					return m_last;
				}

			case REPEATING:
				m_noLeftToRepeat--;
				if (m_noLeftToRepeat == 0)
				{
					m_state = RLEState.READING;
				}
				m_blockChecksum.update(m_last);
				return m_last;

			default:
				throw new RuntimeException("Unknown state " + m_state + ". This is a bug");
		}
	}

	@Override
	public int read(byte[] barr, int off, int len) throws IOException
	{
		// The ranges are validated by BZip2InputStream
		for (int i = 0; i < len; i++)
		{
			int b = read();
			if (b < 0)
			{
				// EOF
				return i > 0 ? i : -1;
			}
			barr[off + i] = (byte) (b & 0xFF);
		}
		return len;
	}

	@Override
	public void close() throws IOException
	{
		m_wrapped.close();
		super.close();
	}
}
