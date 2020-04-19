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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * This object represents an unsigned integer (four bytes or 32 bits) with a
 * value between {code 0} and {@code 4294967295}. It is immutable.
 * <p>
 * Unsigned integers are created by calling any of the static creation methods
 * of this class.
 * @author Karl Gustafsson
 * @since 1.0
 * @see SignedInteger
 * @see UnsignedByte
 * @see UnsignedShort
 * @see UnsignedLong
 */
public final class UnsignedInteger implements Serializable, Comparable<UnsignedInteger>
{
	private static final long serialVersionUID = 1L;

	/**
	 * Each unsigned integer is four bytes long.
	 */
	public static final int SIZE = 4;

	/**
	 * The maximum value of an unsigned integer (4294967295).
	 */
	public static final long MAX_VALUE = (1L << 32) - 1;

	/**
	 * The minimum value of an unsigned integer (0).
	 */
	public static final int MIN_VALUE = 0;

	/**
	 * The value 0.
	 */
	public static final UnsignedInteger ZERO = new UnsignedInteger(0);

	/**
	 * The value 1.
	 */
	public static final UnsignedInteger ONE = new UnsignedInteger(1);

	private final int m_value;

	private UnsignedInteger(int value)
	{
		m_value = value;
	}

	/**
	 * Create a new unsigned integer. The supplied integer is treated as an
	 * unsigned value, which means that negative argument values will result in
	 * unsigned integer values between {@code 2147483648} and {@code 4294967295}
	 * (inclusive).
	 * @param value The signed integer value.
	 * @return An unsigned integer value.
	 */
	public static UnsignedInteger valueOf(int value)
	{
		switch (value)
		{
			case 0:
				return ZERO;
			case 1:
				return ONE;
			default:
				return new UnsignedInteger(value);
		}
	}

	/**
	 * Create an unsigned integer from the supplied long value which must be
	 * between {@code 0} and {@code 4294967295} (inclusive).
	 * @param value The value.
	 * @return The unsigned integer value.
	 * @throws IllegalArgumentException If the supplied value is not in the
	 * permitted range.
	 */
	public static UnsignedInteger valueOf(long value) throws IllegalArgumentException
	{
		if ((value < MIN_VALUE) || (value > MAX_VALUE))
		{
			throw new IllegalArgumentException("Illegal unsigned integer value " + value + ". It must be between " + MIN_VALUE + " and " + MAX_VALUE + " (inclusive)");
		}
		return valueOf((int) (value & 0xFFFFFFFF));
	}

	/**
	 * Get the unsigned integer value represented as a {@code long}.
	 * @return The value.
	 */
	public long longValue()
	{
		return m_value & 0xFFFFFFFFL;
	}

	/**
	 * Get the unsigned integer value converted to a signed integer.
	 * @return The unsigned integer value converted to a signed integer.
	 */
	public int intValue()
	{
		return m_value;
	}

	/**
	 * Get the unsigned integer value as a big-endian, four bytes long byte
	 * array.
	 * @return The value represented as a big-endian byte array.
	 */
	public byte[] getBigEndianByteArray()
	{
		byte[] res = new byte[4];
		res[0] = (byte) (m_value & 0xFF);
		res[1] = (byte) ((m_value >>> 8) & 0xFF);
		res[2] = (byte) ((m_value >>> 16) & 0xFF);
		res[3] = (byte) ((m_value >>> 24) & 0xFF);
		return res;
	}

	/**
	 * Create an unsigned integer value from a four bytes long, big-endian byte
	 * array.
	 * @param barr The byte array. It must be four bytes long.
	 * @return The unsigned integer.
	 * @throws IllegalArgumentException If the supplied byte array is not four
	 * bytes long.
	 * @see #fromBigEndianByteArray(byte[], int)
	 * @see #fromBigEndianByteArrayToLong(byte[], int)
	 */
	public static UnsignedInteger fromBigEndianByteArray(byte[] barr) throws IllegalArgumentException
	{
		if (barr.length != 4)
		{
			throw new IllegalArgumentException("The supplied byte array must be four bytes long");
		}
		return fromBigEndianByteArray(barr, 0);
	}

	/**
	 * Create an unsigned integer value from four bytes read from the given
	 * offset position in the supplied byte array. The most significant byte is
	 * the last byte read.
	 * @param barr The byte array to read from.
	 * @param offset The offset in the byte array where the least significant
	 * (first) byte is.
	 * @return An unsigned integer.
	 * @throws ArrayIndexOutOfBoundsException If the supplied array is too short
	 * or if the offset is negative.
	 * @see #fromBigEndianByteArray(byte[])
	 * @see #fromBigEndianByteArrayToLong(byte[], int)
	 */
	public static UnsignedInteger fromBigEndianByteArray(byte[] barr, int offset) throws ArrayIndexOutOfBoundsException
	{
		return valueOf((barr[offset] & 0xFF) + ((barr[offset + 1] & 0xFF) << 8) + ((barr[offset + 2] & 0xFF) << 16) + ((barr[offset + 3] & 0xFF) << 24));
	}

	/**
	 * Create a long value representing the unsigned integer value in the byte
	 * array at the specified offset. The most significant byte is the last byte
	 * read.
	 * @param barr The byte array to read from.
	 * @param offset The offset in the byte array where the least significant
	 * (first) byte is.
	 * @return A {@code long} representing the unsigned integer.
	 * @throws ArrayIndexOutOfBoundsException If the supplied array is too short
	 * or if the offset is negative.
	 * @see #fromBigEndianByteArray(byte[])
	 * @see #fromBigEndianByteArray(byte[], int)
	 * @see #fromLittleEndianByteArrayToLong(byte[], int)
	 * @since 1.1
	 */
	public static long fromBigEndianByteArrayToLong(byte[] barr, int offset) throws ArrayIndexOutOfBoundsException
	{
		return (barr[offset] & 0xFF) + ((barr[offset + 1] & 0xFF) << 8) + ((barr[offset + 2] & 0xFF) << 16) + ((barr[offset + 3] & 0xFF) << 24);
	}

	/**
	 * Create a long value representing the unsigned integer value in the byte
	 * array at the specified offset. The most significant byte is the first
	 * byte read.
	 * @param barr The byte array to read from.
	 * @param offset The offset in the byte array where the most significant
	 * (first) byte is.
	 * @return A {@code long} representing the unsigned integer.
	 * @throws ArrayIndexOutOfBoundsException If the supplied array is too short
	 * or if the offset is negative.
	 * @see #fromBigEndianByteArrayToLong(byte[], int)
	 * @since 1.1
	 */
	public static long fromLittleEndianByteArrayToLong(byte[] barr, int offset) throws ArrayIndexOutOfBoundsException
	{
		return (barr[offset + 3] & 0xFF) + ((barr[offset + 2] & 0xFF) << 8) + ((barr[offset + 1] & 0xFF) << 16) + ((barr[offset] & 0xFF) << 24);
	}


	@Override
	public boolean equals(Object o)
	{
		return (o instanceof UnsignedInteger) && (((UnsignedInteger) o).m_value == m_value);
	}

	@Override
	public int hashCode()
	{
		return m_value;
	}

	public int compareTo(UnsignedInteger i2)
	{
		return Long.valueOf(longValue()).compareTo(Long.valueOf(i2.longValue()));
	}

	@Override
	public String toString()
	{
		return Long.toString(m_value & 0xFFFFFFFFL);
	}
}
