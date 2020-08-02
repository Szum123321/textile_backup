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
package org.at4j.support.lang;

import java.io.Serializable;


/**
 * This object represents an unsigned short value (two bytes or 16 bits) with a
 * value between {code 0} and {@code 65535}. It is immutable.
 * <p>
 * Unsigned shorts are created by calling any of the static creation methods of
 * this class.
 * @author Karl Gustafsson
 * @since 1.0
 * @see UnsignedByte
 * @see UnsignedInteger
 * @see UnsignedLong
 */
public final class UnsignedShort implements Serializable, Comparable<UnsignedShort>
{
	private static final long serialVersionUID = 1L;

	/**
	 * Each unsigned short is two bytes long.
	 */
	public static final int SIZE = 2;

	/**
	 * The maximum value of an unsigned short (65535).
	 */
	public static final int MAX_VALUE = (1 << 16) - 1;

	/**
	 * The minimum value of an unsigned short (0).
	 */
	public static final int MIN_VALUE = 0;

	/**
	 * The value 0.
	 */
	public static final UnsignedShort ZERO = new UnsignedShort((short) 0);

	/**
	 * The value 1.
	 */
	public static final UnsignedShort ONE = new UnsignedShort((short) 1);

	/**
	 * The value 1000.
	 */
	public static final UnsignedShort ONE_THOUSAND = new UnsignedShort((short) 1000);

	private final short m_value;

	private UnsignedShort(short value)
	{
		m_value = value;
	}

	/**
	 * Create a new unsigned short. The supplied short is treated as an unsigned
	 * value, which means that negative argument values will result in unsigned
	 * short values between {@code 32768} and {@code 65535} (inclusive).
	 * @param value The signed short value.
	 * @return An unsigned short value.
	 */
	public static UnsignedShort valueOf(short value)
	{
		switch (value)
		{
			case 0:
				return ZERO;
			case 1:
				return ONE;
			case 1000:
				return ONE_THOUSAND;
			default:
				return new UnsignedShort(value);
		}
	}

	/**
	 * Create an unsigned short from the supplied integer value which must be
	 * between {@code 0} and {@code 65535} (inclusive).
	 * @param value The value.
	 * @return The unsigned short value.
	 * @throws IllegalArgumentException If the supplied value is not in the
	 * permitted range.
	 */
	public static UnsignedShort valueOf(int value) throws IllegalArgumentException
	{
		if ((value < MIN_VALUE) || (value > MAX_VALUE))
		{
			throw new IllegalArgumentException("Illegal unsigned short value " + value + ". It must be between " + MIN_VALUE + " and " + MAX_VALUE + " (inclusive)");
		}
		return valueOf((short) (value & 0xFFFF));
	}

	/**
	 * Get the unsigned short value.
	 * @return The value.
	 */
	public int intValue()
	{
		return m_value & 0xFFFF;
	}

	/**
	 * Get the unsigned short value as a big-endian, two bytes long byte array.
	 * @return The value represented as a big-endian byte array.
	 */
	public byte[] getBigEndianByteArray()
	{
		byte[] res = new byte[2];
		res[0] = (byte) (m_value & 0xFF);
		res[1] = (byte) ((m_value >>> 8) & 0xFF);
		return res;
	}

	/**
	 * Create an unsigned short value from a two bytes long, big-endian byte
	 * array.
	 * @param barr The byte array. It must be two bytes long.
	 * @return The unsigned short.
	 * @throws IllegalArgumentException If the supplied byte array is not two
	 * bytes long.
	 * @see #fromBigEndianByteArray(byte[], int)
	 */
	public static UnsignedShort fromBigEndianByteArray(byte[] barr) throws IllegalArgumentException
	{
		if (barr.length != 2)
		{
			throw new IllegalArgumentException("The supplied byte array must be two bytes long");
		}
		return fromBigEndianByteArray(barr, 0);
	}

	/**
	 * Create an unsigned short value from two bytes read from the given offset
	 * position in the supplied byte array. The most significant byte is the
	 * last byte read.
	 * @param barr The byte array to read from.
	 * @param offset The offset in the byte array where the least significant
	 * (first) byte is.
	 * @return An unsigned short.
	 * @throws ArrayIndexOutOfBoundsException If the supplied array is too short
	 * or if the offset is negative.
	 * @see #fromBigEndianByteArray(byte[])
	 */
	public static UnsignedShort fromBigEndianByteArray(byte[] barr, int offset) throws ArrayIndexOutOfBoundsException
	{
		return valueOf((short) ((barr[offset] & 0xFF) + ((barr[offset + 1] & 0xFF) << 8) & 0xFFFF));
	}


	@Override
	public boolean equals(Object o)
	{
		return (o instanceof UnsignedShort) && (((UnsignedShort) o).m_value == m_value);
	}

	@Override
	public int hashCode()
	{
		return m_value;
	}

	public int compareTo(UnsignedShort s2)
	{
		return intValue() - s2.intValue();
	}

	@Override
	public String toString()
	{
		return Integer.toString(m_value & 0xFFFF);
	}
}
