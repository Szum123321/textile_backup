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
 * A move-to-front (MTF) encoder and decoder for bytes. For more information on
 * MTF encoding, see<a href="http://en.wikipedia.org/wiki/Move_to_front">the
 * Wikipedia article on move-to-front transforms</a>.
 * <p>
 * This object is not thread safe. Clients must provide external synchronization
 * if they are to use it from several concurrent threads.
 * @author Karl Gustafsson
 * @since 1.1
 * @see IntMoveToFront
 */
public class ByteMoveToFront
{
	private final byte[] m_alphabet;

	private static byte[] createByteAlphabetFromRange(int minVal, int maxVal) throws IndexOutOfBoundsException
	{
		if ((minVal < 0) || (maxVal > 255) || (minVal >= maxVal))
		{
			throw new IndexOutOfBoundsException("Invalid min and/or max value: min " + minVal + ", max " + maxVal);
		}
		int alphLen = maxVal - minVal + 1;
		byte[] alphabet = new byte[alphLen];
		for (int i = 0; i < alphLen; i++)
		{
			alphabet[i] = (byte) ((i + minVal) & 0xFF);
		}
		return alphabet;
	}

	/**
	 * Create a byte MTF encoder/decoder that transforms bytes in the range
	 * between {@code minValue} and {@code maxValue}.
	 * <p>
	 * The initial alphabet of the transformer will be {@code minValue &hellip;
	 * maxValue}.
	 * @param minValue The start value of the range. This should be an unsigned
	 * byte in the range 0 to 254.
	 * @param maxValue The end value of the range. This should be an unsigned
	 * byte in the range 1 to 255.
	 * @throws IndexOutOfBoundsException If the min and/or the max values are
	 * not unsigned bytes or if the min value is equal to or greater than the
	 * max value.
	 */
	public ByteMoveToFront(int minValue, int maxValue) throws IndexOutOfBoundsException
	{
		this(createByteAlphabetFromRange(minValue, maxValue));
	}

	/**
	 * Create a byte MTF encoder/decoder that transforms bytes using the
	 * supplied initial alphabet.
	 * @param alphabet The initial alphabet. This byte array is <i>not</i>
	 * copied by this method and it will be modified by encoding or decoding
	 * operations.
	 */
	public ByteMoveToFront(byte[] alphabet)
	{
		// Null check
		alphabet.getClass();

		m_alphabet = alphabet;
	}

	/**
	 * Encode the bytes in {@code in} and store them in the array {@code out}.
	 * The MTF alphabet is also updated by this method.
	 * @param in The bytes to encode.
	 * @param out The array to store the encoded bytes in. This array must be at
	 * least as long as {@code in}.
	 * @return {@code out}
	 * @throws ArrayIndexOutOfBoundsException If any of the bytes in {@code in}
	 * are not in the MTF alphabet.
	 * @throws IllegalArgumentException If the {@code out} array is too short.
	 */
	public byte[] encode(byte[] in, byte[] out) throws ArrayIndexOutOfBoundsException, IllegalArgumentException
	{
		if (out.length < in.length)
		{
			throw new IllegalArgumentException("The output array must be at least of the same length as the input array. Was in: " + in.length + ", out: " + out.length);
		}

		for (int i = 0; i < in.length; i++)
		{
			byte val = in[i];
			if (m_alphabet[0] == val)
			{
				out[i] = 0;
			}
			else
			{
				byte prev = m_alphabet[0];
				int j = 1;
				while (true)
				{
					byte nextPrev = m_alphabet[j];
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
	 * Decode a single byte and update the MTF alphabet.
	 * @param index The index in the MTF alphabet for the byte.
	 * @return The byte.
	 */
	public byte decode(int index)
	{
		byte val = m_alphabet[index];
		for (int j = index; j > 0; j--)
		{
			m_alphabet[j] = m_alphabet[j - 1];
		}
		m_alphabet[0] = val;
		return val;
	}

	/**
	 * Decode an array of bytes and update the MTF alphabet. The decoded bytes
	 * are stored in {@code out}.
	 * @param in The bytes to decode.
	 * @param out The array to store the decoded bytes in. This array must be at
	 * least as long as {@code in}.
	 * @return {@code out}
	 * @throws ArrayIndexOutOfBoundsException If any of the bytes in {@code in}
	 * are not in the MTF alphabet.
	 * @throws IllegalArgumentException If {@code out} is too short.
	 */
	public byte[] decode(byte[] in, byte[] out) throws ArrayIndexOutOfBoundsException, IllegalArgumentException
	{
		if (out.length < in.length)
		{
			throw new IllegalArgumentException("The output array must be at least of the same length as the input array. Was in: " + in.length + ", out: " + out.length);
		}

		for (int i = 0; i < in.length; i++)
		{
			out[i] = decode(in[i]);
		}
		return out;
	}
}
