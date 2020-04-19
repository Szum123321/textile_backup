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
 * Decode Burrows Wheeler encoded data.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class BurrowsWheelerDecoder
{
	static class BWInputStream extends InputStream
	{
		private final byte[] m_decoded;
		private final int[] m_ptr;

		private int m_curPointer;
		private boolean m_eof;
		private int m_noLeftToRead;

		BWInputStream(byte[] decoded, int[] ptr, int originalDataPointer)
		{
			m_decoded = decoded;
			m_ptr = ptr;
			m_curPointer = ptr[originalDataPointer];
			m_noLeftToRead = ptr.length;
		}

		@Override
		public int read() throws IOException
		{
			if (m_eof)
			{
				return -1;
			}
			final int res = m_decoded[m_curPointer] & 0xFF;
			m_eof = --m_noLeftToRead == 0;
			m_curPointer = m_ptr[m_curPointer];
			return res;
		}
	}

	private final byte[] m_decoded;
	private final int m_noBytesDecoded;
	private final int[] m_byteFrequencies;
	private final int m_originalDataPointer;

	/**
	 * @param encoded The encoded data. This array may be longer than the actual
	 * amount of encoded data. The {@code noBytesDecoded} parameter determines
	 * how much of the array that will be used.
	 * @param noBytesEncoded The length of the encoded data.
	 * @param byteFrequencies The number of times each byte occur in the data.
	 * @param originalDataPointer The row number of the original data in the
	 * Burrows Wheeler matrix.
	 * @throws IOException On I/O errors.
	 */
	BurrowsWheelerDecoder(byte[] encoded, int noBytesEncoded, int[] byteFrequencies, int originalDataPointer) throws IOException
	{
		if (originalDataPointer > noBytesEncoded)
		{
			throw new IOException("Invalid pointer to original data in block header " + originalDataPointer + ". It is larger than the size of data in the block " + noBytesEncoded);
		}

		m_decoded = encoded;
		m_noBytesDecoded = noBytesEncoded;
		m_byteFrequencies = byteFrequencies;
		m_originalDataPointer = originalDataPointer;
	}

	InputStream decode()
	{
		// Calculate the transformation vector used to move from the encoded
		// data to the decoded.

		// The byte frequency array contains the frequency of each byte in the
		// data. Create a new array tarr that, for each byte, specifies how many
		// bytes of lower value that occurs in the data.
		int[] tarr = new int[256];
		tarr[0] = 0;
		for (int i = 1; i < 256; i++)
		{
			tarr[i] = tarr[i - 1] + m_byteFrequencies[i - 1];
		}

		// The ptr array will contain a chain of positions of the decoded bytes
		// in the decoded array.
		final int[] ptr = new int[m_noBytesDecoded];
		for (int i = 0; i < m_noBytesDecoded; i++)
		{
			int val = m_decoded[i] & 0xFF;
			// Get the position of the decoded byte position in tt. Increment
			// the tt position for the given value so that next occurrence of the
			// value will end up in the next position in tt.
			int ttPos = tarr[val]++;
			ptr[ttPos] = i;
		}

		return new BWInputStream(m_decoded, ptr, m_originalDataPointer);
	}
}
