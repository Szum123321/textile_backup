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
package org.at4j.support.comp;

/**
 * A move-to-front (MTF) encoder and decoder for integers. For more information
 * on MTF encoding, see<a href="http://en.wikipedia.org/wiki/Move_to_front">the
 * Wikipedia article on move-to-front transforms</a>.
 * <p>
 * This object is not thread safe. Clients must provide external synchronization
 * if they are to use it from several concurrent threads.
 * @author Karl Gustafsson
 * @since 1.1
 * @see ByteMoveToFront
 */
public class IntMoveToFront
{
	private final int[] m_alphabet;

	private static int[] createIntAlphabetFromRange(int minVal, int maxVal) throws IndexOutOfBoundsException
	{
		if (minVal >= maxVal)
		{
			throw new IndexOutOfBoundsException("Invalid min and max values. Min=" + minVal + ", max=" + maxVal);
		}
		int alphLen = maxVal - minVal + 1;
		int[] alphabet = new int[alphLen];
		for (int i = 0; i < alphLen; i++)
		{
			alphabet[i] = i + minVal;
		}
		return alphabet;
	}

	/**
	 * Create a byte MTF encoder/decoder that transforms integers in the range
	 * between {@code minValue} and {@code maxValue}.
	 * <p>
	 * The initial alphabet of the transformer will be {@code minValue &hellip;
	 * maxValue}.
	 * @param minValue The start value of the range.
	 * @param maxValue The end value of the range.
	 * @throws IndexOutOfBoundsException If the min value is equal to or greater
	 * than the max value.
	 */
	public IntMoveToFront(int minValue, int maxValue) throws IndexOutOfBoundsException
	{
		this(createIntAlphabetFromRange(minValue, maxValue));
	}

	/**
	 * Create a byte MTF encoder/decoder that transforms integers using the
	 * supplied initial alphabet.
	 * @param alphabet The initial alphabet. This integer array is <i>not</i>
	 * copied by this method and it will be modified by encoding or decoding
	 * operations.
	 */
	public IntMoveToFront(int[] alphabet)
	{
		// Null check
		alphabet.getClass();

		m_alphabet = alphabet;
	}

	/**
	 * Encode the integers in {@code in} and store them in the array {@code out}
	 * . The MTF alphabet is also updated by this method.
	 * @param in The integers to encode.
	 * @param out The array to store the encoded integers in. This array must be
	 * at least as long as {@code in}.
	 * @return {@code out}
	 * @throws ArrayIndexOutOfBoundsException If any of the integers in {@code
	 * in} are not in the MTF alphabet.
	 * @throws IllegalArgumentException If the {@code out} array is too short.
	 */
	public int[] encode(int[] in, int[] out) throws ArrayIndexOutOfBoundsException, IllegalArgumentException
	{
		if (out.length < in.length)
		{
			throw new IllegalArgumentException("The output array must be at least of the same length as the input array. Was in: " + in.length + ", out: " + out.length);
		}

		for (int i = 0; i < in.length; i++)
		{
			int val = in[i];
			if (m_alphabet[0] == val)
			{
				out[i] = 0;
			}
			else
			{
				int prev = m_alphabet[0];
				int j = 1;
				while (true)
				{
					int nextPrev = m_alphabet[j];
					if (m_alphabet[j] == val)
					{
						out[i] = (byte) (j & 0xFF);
						m_alphabet[0] = m_alphabet[j];
						m_alphabet[j] = prev;
						break;
					}
					m_alphabet[j] = prev;
					prev = nextPrev;
					j++;
				}
			}
		}
		return out;
	}

	/**
	 * Decode a single integer and update the MTF alphabet.
	 * @param index The index in the MTF alphabet for the integer.
	 * @return The integer.
	 */
	public int decode(int index)
	{
		int val = m_alphabet[index];
		for (int j = index; j > 0; j--)
		{
			m_alphabet[j] = m_alphabet[j - 1];
		}
		m_alphabet[0] = val;
		return val;
	}

	/**
	 * Decode an array of integers and update the MTF alphabet. The decoded
	 * integers are stored in {@code out}.
	 * @param in The integers to decode.
	 * @param out The array to store the decoded integers in. This array must be
	 * at least as long as {@code in}.
	 * @return {@code out}
	 * @throws ArrayIndexOutOfBoundsException If any of the integers in {@code
	 * in} are not in the MTF alphabet.
	 * @throws IllegalArgumentException If {@code out} is too short.
	 */
	public int[] decode(int[] in, int[] out) throws ArrayIndexOutOfBoundsException, IllegalArgumentException
	{
		if (out.length < in.length)
		{
			throw new IllegalArgumentException("The output array must be at least of the same length as the input array. Was in: " + in.length + ", out: " + out.length);
		}

		for (int i = 0; i < in.length; i++)
		{
			int index = in[i];
			int val = m_alphabet[index];
			for (int j = index; j > 0; j--)
			{
				m_alphabet[j] = m_alphabet[j - 1];
			}
			m_alphabet[0] = val;
			out[i] = val;
		}
		return out;
	}
}
