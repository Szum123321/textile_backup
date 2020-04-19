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
import java.math.BigInteger;

/**
 * This object represents an unsigned long (eight bytes or 64 bits) with a value
 * between {code 0} and {@code 18446744073709551615}. It is immutable.
 * <p>
 * Unsigned longs are created by calling any of the static creation methods of
 * this class.
 * @author Karl Gustafsson
 * @since 1.0
 * @see SignedLong
 * @see UnsignedByte
 * @see UnsignedShort
 * @see UnsignedInteger
 */
public final class UnsignedLong implements Serializable, Comparable<UnsignedLong>
{
	private static final long serialVersionUID = 1L;

	/**
	 * The minimum allowed value (0).
	 */
	public static final BigInteger MIN_VALUE = BigInteger.valueOf(0L);

	/**
	 * The maximum allowed value (18446744073709551615).
	 */
	public static final BigInteger MAX_VALUE;

	/**
	 * The value zero.
	 */
	public static final UnsignedLong ZERO = new UnsignedLong(0L);

	/**
	 * The value one.
	 */
	public static final UnsignedLong ONE = new UnsignedLong(1L);

	private static final BigInteger HIGHEST_BIT_VALUE;
	static
	{
		BigInteger mv = BigInteger.valueOf(2L);
		MAX_VALUE = mv.pow(64).subtract(BigInteger.ONE);
		HIGHEST_BIT_VALUE = mv.pow(63);
	}

	private final long m_value;

	private UnsignedLong(long value)
	{
		m_value = value;
	}

	/**
	 * Create an unsigned long. The supplied value is treated as an unsigned
	 * long, which means that negative argument values will result in unsigned
	 * long values between {@code 9223372036854775808} and {@code
	 * 18446744073709551615} (inclusive).
	 * @param value The value.
	 * @return An unsigned long value.
	 */
	public static UnsignedLong valueOf(long value)
	{
		if (value == 0L)
		{
			return ZERO;
		}
		else if (value == 1L)
		{
			return ONE;
		}
		else
		{
			return new UnsignedLong(value);
		}
	}

	/**
	 * Create an unsigned long value from the supplied {@link BigInteger} value
	 * which must be in the range {@code 0} to {@code 18446744073709551615}
	 * (inclusive)
	 * @param value The value.
	 * @return An unsigned long value.
	 * @throws IllegalArgumentException If the supplied value is negative or if
	 * it is greater than {@link #MAX_VALUE}.
	 */
	public static UnsignedLong valueOf(BigInteger value) throws IllegalArgumentException
	{
		if ((value.compareTo(MIN_VALUE) < 0) || (value.compareTo(MAX_VALUE) > 0))
		{
			throw new IllegalArgumentException("Illegal unsigned long value " + value + ". It must be between 0 and " + MAX_VALUE + " (inclusive)");
		}
		return valueOf(value.longValue());
	}

	/**
	 * Get the unsigned long value as a {@link BigInteger}.
	 * @return The unsigned long value as a {@link BigInteger}.
	 */
	public BigInteger bigIntValue()
	{
		BigInteger res = BigInteger.valueOf(m_value & 0x7FFFFFFFFFFFFFFFL);
		return m_value < 0 ? res.add(HIGHEST_BIT_VALUE) : res;
	}

	/**
	 * Return the value as a signed long. If the value is less than
	 * {@link Long#MAX_VALUE}, it is returned as a positive long. If not, it is
	 * returned as a negative long.
	 * @return The value as a signed long value.
	 */
	public long longValue()
	{
		return m_value;
	}

	/**
	 * Get the unsigned long value as a big-endian, eight bytes long byte array.
	 * @return The value represented as a big-endian byte array.
	 */
	public byte[] getBigEndianByteArray()
	{
		byte[] res = new byte[8];
		res[0] = (byte) (m_value & 0xFF);
		res[1] = (byte) ((m_value >>> 8) & 0xFF);
		res[2] = (byte) ((m_value >>> 16) & 0xFF);
		res[3] = (byte) ((m_value >>> 24) & 0xFF);
		res[4] = (byte) ((m_value >>> 32) & 0xFF);
		res[5] = (byte) ((m_value >>> 40) & 0xFF);
		res[6] = (byte) ((m_value >>> 48) & 0xFF);
		res[7] = (byte) ((m_value >>> 56) & 0xFF);
		return res;
	}

	/**
	 * Create an unsigned long value from a eight bytes long, big-endian byte
	 * array.
	 * @param barr The byte array. It must be eight bytes long.
	 * @return The unsigned long.
	 * @throws IllegalArgumentException If the supplied byte array is not eight
	 * bytes long.
	 * @see #fromBigEndianByteArray(byte[], int)
	 */
	public static UnsignedLong fromBigEndianByteArray(byte[] barr) throws IllegalArgumentException
	{
		if (barr.length != 8)
		{
			throw new IllegalArgumentException("The supplied byte array must be eight bytes long");
		}
		return fromBigEndianByteArray(barr, 0);
	}

	/**
	 * Create an unsigned long value from eight bytes read from the given offset
	 * position in the supplied byte array. The most significant byte is the
	 * last byte read.
	 * @param barr The byte array to read from.
	 * @param offset The offset in the byte array where the least significant
	 * (first) byte is.
	 * @return An unsigned long.
	 * @throws ArrayIndexOutOfBoundsException If the supplied array is too short
	 * or if the offset is negative.
	 * @see #fromBigEndianByteArray(byte[])
	 */
	public static UnsignedLong fromBigEndianByteArray(byte[] barr, int offset) throws ArrayIndexOutOfBoundsException
	{
		return valueOf((barr[offset] & 0xFFL) + ((barr[offset + 1] & 0xFFL) << 8) + ((barr[offset + 2] & 0xFFL) << 16) + ((barr[offset + 3] & 0xFFL) << 24) + ((barr[offset + 4] & 0xFFL) << 32) + ((barr[offset + 5] & 0xFFL) << 40)
				+ ((barr[offset + 6] & 0xFFL) << 48) + ((barr[offset + 7] & 0xFFL) << 56));
	}

	@Override
	public boolean equals(Object o)
	{
		if (o != null && o instanceof UnsignedLong)
		{
			return m_value == ((UnsignedLong) o).m_value;
		}
		else
		{
			return false;
		}
	}

	@Override
	public int hashCode()
	{
		return (int) (m_value ^ (m_value >>> 32));
	}

	public int compareTo(UnsignedLong l2)
	{
		return bigIntValue().compareTo(l2.bigIntValue());
	}

	@Override
	public String toString()
	{
		return bigIntValue().toString();
	}
}
