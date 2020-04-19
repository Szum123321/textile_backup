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

/**
 * This class represents a signed long value (i.e. a plain {@code long} value).
 * If the {@link java.lang.Long} class had not been declared {@code final}, this
 * class would probably have extended it.
 * <p>
 * Signed long objects are created by calling any of the static creation methods
 * on this class.
 * <p>
 * Instances of this class are immutable.
 * @author Karl Gustafsson
 * @since 1.0
 * @see UnsignedLong
 * @see SignedInteger
 */
public class SignedLong implements Comparable<SignedLong>
{
	/**
	 * This constant represents the value {@code 0}.
	 */
	public static final SignedLong ZERO = new SignedLong(0);

	/**
	 * This constant represents the value {@code 1}.
	 */
	public static final SignedLong ONE = new SignedLong(1);

	private final long m_value;

	/**
	 * Create a new signed long value.
	 * @param value The value.
	 */
	private SignedLong(long value)
	{
		m_value = value;
	}

	/**
	 * Create a new signed long value.
	 * @param value The long value.
	 * @return The signed long value.
	 */
	public static SignedLong valueOf(long value)
	{
		if (value == 0)
		{
			return ZERO;
		}
		else if (value == 1)
		{
			return ONE;
		}
		else
		{
			return new SignedLong(value);
		}
	}

	/**
	 * Get the signed long value.
	 * @return The signed long value.
	 */
	public long longValue()
	{
		return m_value;
	}

	/**
	 * Get the signed long value represented as a big-endian byte array (eight
	 * bytes long).
	 * @return The long value represented as a big-endian byte array.
	 * @see #fromBigEndianByteArray(byte[])
	 * @see #getLittleEndianByteArray()
	 */
	public byte[] getBigEndianByteArray()
	{
		byte[] res = new byte[8];
		res[0] = (byte) m_value;
		res[1] = (byte) (m_value >> 8);
		res[2] = (byte) (m_value >> 16);
		res[3] = (byte) (m_value >> 24);
		res[4] = (byte) (m_value >> 32);
		res[5] = (byte) (m_value >> 40);
		res[6] = (byte) (m_value >> 48);
		res[7] = (byte) (m_value >> 56);
		return res;
	}

	/**
	 * Create a signed long value from an eight bytes long big-endian byte
	 * array.
	 * @param barr The byte array. It must be eight bytes long.
	 * @return The signed long value.
	 * @throws IllegalArgumentException If the byte array is not eight bytes
	 * long.
	 * @see #getBigEndianByteArray()
	 * @see #fromLittleEndianByteArray(byte[])
	 */
	public static SignedLong fromBigEndianByteArray(byte[] barr) throws IllegalArgumentException
	{
		if (barr.length != 8)
		{
			throw new IllegalArgumentException("Illegal size of supplied byte array: " + barr.length + ". It must be eight bytes long");
		}
		long value = barr[0] & 0xFF;
		value += ((barr[1] & 0xFFL) << 8);
		value += ((barr[2] & 0xFFL) << 16);
		value += ((barr[3] & 0xFFL) << 24);
		value += ((barr[4] & 0xFFL) << 32);
		value += ((barr[5] & 0xFFL) << 40);
		value += ((barr[6] & 0xFFL) << 48);
		value += ((barr[7] & 0xFFL) << 56);
		return valueOf(value);
	}

	/**
	 * Get the signed long value represented as a little-endian byte array
	 * (eight bytes long).
	 * @return The long value represented as a little-endian byte array.
	 * @see #getBigEndianByteArray()
	 * @see #fromBigEndianByteArray(byte[])
	 */
	public byte[] getLittleEndianByteArray()
	{
		byte[] res = new byte[8];
		res[0] = (byte) (m_value >> 56);
		res[1] = (byte) (m_value >> 48);
		res[2] = (byte) (m_value >> 40);
		res[3] = (byte) (m_value >> 32);
		res[4] = (byte) (m_value >> 24);
		res[5] = (byte) (m_value >> 16);
		res[6] = (byte) (m_value >> 8);
		res[7] = (byte) m_value;
		return res;
	}

	/**
	 * Create a signed long value from an eight bytes long little-endian byte
	 * array.
	 * @param barr The byte array. It must be eight bytes long.
	 * @return The signed long value.
	 * @throws IllegalArgumentException If the byte array is not eight bytes
	 * long.
	 * @see #getLittleEndianByteArray()
	 * @see #fromBigEndianByteArray(byte[])
	 */
	public static SignedLong fromLittleEndianByteArray(byte[] barr) throws IllegalArgumentException
	{
		if (barr.length != 8)
		{
			throw new IllegalArgumentException("Illegal size of supplied byte array: " + barr.length + ". It must be eight bytes long");
		}
		long value = barr[7] & 0xFF;
		value += ((barr[6] & 0xFFL) << 8);
		value += ((barr[5] & 0xFFL) << 16);
		value += ((barr[4] & 0xFFL) << 24);
		value += ((barr[3] & 0xFFL) << 32);
		value += ((barr[2] & 0xFFL) << 40);
		value += ((barr[1] & 0xFFL) << 48);
		value += ((barr[0] & 0xFFL) << 56);
		return valueOf(value);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o != null && o instanceof SignedLong)
		{
			return m_value == ((SignedLong) o).m_value;
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

	public int compareTo(SignedLong l2)
	{
		return Long.valueOf(m_value).compareTo(Long.valueOf(l2.m_value));
	}

	@Override
	public String toString()
	{
		return "" + m_value;
	}
}
