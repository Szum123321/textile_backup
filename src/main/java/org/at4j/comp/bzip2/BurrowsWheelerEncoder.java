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

/**
 * Burrows Wheeler encoder.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class BurrowsWheelerEncoder
{
	static class BurrowsWheelerEncodingResult
	{
		// The values of the last column of the matrix
		final byte[] m_lastColumn;
		// The row number of the first row (the row which contains the incoming
		// data) in the sorted matrix
		final int m_firstPointer;

		private BurrowsWheelerEncodingResult(byte[] lastColumn, int firstPointer)
		{
			m_lastColumn = lastColumn;
			m_firstPointer = firstPointer;
		}
	}

	// The shortest length that will be quicksorted rather than shell sorted
	private static int MIN_QUICKSORT_LENGTH = 18;

	// The data array containing the unencoded data.
	private final byte[] m_data;
	// The length of the data in the array. Data occupies the positions 0 to
	// m_length - 1 in the array.
	private final int m_length;
	// Contains preallocated data structures. Used to reduce the number of
	// temporary objects that are created and thus avoid time spent gc:ing.
	private final EncodingScratchpad m_scratchpad;

	/**
	 * @param data This array should contain a 100 byte overshoot. See
	 * {@link ThreeWayRadixQuicksort#ThreeWayRadixQuicksort(byte[], int, int, EncodingScratchpad)}
	 * .
	 */
	BurrowsWheelerEncoder(byte[] data, int length, EncodingScratchpad sp)
	{
		if (length > data.length)
		{
			throw new IllegalArgumentException("Invalid data length " + length + ". It must be <= the length of the data array (" + data.length + ")");
		}
		m_data = data;
		m_length = length;
		m_scratchpad = sp;
	}

	/**
	 * Run a Burrows Wheeler encoding.
	 */
	BurrowsWheelerEncodingResult encode()
	{
		// Create all rotations of m_data, put them in a matrix and sort the
		// first column. For each row in the matrix, ptr contains a pointer to
		// the first byte of the row's m_data rotation.
		int[] ptr = new ThreeWayRadixQuicksort(m_data, m_length, MIN_QUICKSORT_LENGTH, m_scratchpad).sort();

		// Get the contents of the last column in the matrix. This, and the
		// pointer to the ĺocation of where the first byte in m_data is in the
		// last column, is the result from the Burrows Wheeler encoding.
		byte[] lastColumn = m_scratchpad.m_lastColumn;
		int firstRow = -1;

		for (int i = 0; i < m_length; i++)
		{
			int fePtr = ptr[i] - 1;
			if (fePtr < 0)
			{
				fePtr += m_length;
				firstRow = i;
			}
			lastColumn[i] = m_data[fePtr];
		}
		return new BurrowsWheelerEncodingResult(lastColumn, firstRow);
	}
}
