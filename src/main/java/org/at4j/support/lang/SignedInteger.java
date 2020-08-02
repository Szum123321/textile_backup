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
 * This class represents a signed integer value (i.e. a plain {@code int}
 * value). If the {@link java.lang.Integer} class had not been declared {@code
 * final}, this class would probably have extended it.
 * <p>
 * Signed integer objects are created by calling any of the static creation
 * methods on this class.
 * <p>
 * Instances of this class are immutable.
 * @author Karl Gustafsson
 * @since 1.1.1
 * @see UnsignedInteger
 * @see SignedLong
 */
public class SignedInteger implements Comparable<SignedInteger>
{
	/**
	 * This constant represents the value {@code 0}.
	 */
	public static final SignedInteger ZERO = new SignedInteger(0);

	/**
	 * This constant represents the value {@code 1}.
	 */
	public static final SignedInteger ONE = new SignedInteger(1);

	private final int m_value;

	/**
	 * Create a new signed integer value.
	 * @param value The value.
	 */
	private SignedInteger(int value)
	{
		m_value = value;
	}

	/**
	 * Create a new signed integer value.
	 * @param value The integer value.
	 * @return The signed integer value.
	 */
	public static SignedInteger valueOf(int value)
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
			return new SignedInteger(value);
		}
	}

	/**
	 * Get the signed integer value.
	 * @return The signed integer value.
	 */
	public long intValue()
	{
		return m_value;
	}

	/**
	 * Get the signed integer value represented as a big-endian byte array (four
	 * bytes long).
	 * @return The integer value represented as a big-endian byte array.
	 * @see #fromBigEndianByteArray(byte[])
	 * @see #getLittleEndianByteArray()
	 */
	public byte[] getBigEndianByteArray()
	{
		byte[] res = new byte[4];
		res[0] = (byte) m_value;
		res[1] = (byte) (m_value >> 8);
		res[2] = (byte) (m_value >> 16);
		res[3] = (byte) (m_value >> 24);
		return res;
	}

	/**
	 * Create a signed integer value from an four bytes long big-endian byte
	 * array.
	 * @param barr The byte array. It must be four bytes long.
	 * @return The signed four value.
	 * @throws IllegalArgumentException If the byte array is not four bytes
	 * long.
	 * @see #getBigEndianByteArray()
	 * @see #fromLittleEndianByteArray(byte[])
	 */
	public static SignedInteger fromBigEndianByteArray(byte[] barr) throws IllegalArgumentException
	{
		if (barr.length != 4)
		{
			throw new IllegalArgumentException("Illegal size of supplied byte array: " + barr.length + ". It must be four bytes long");
		}
		int value = barr[0] & 0xFF;
		value += ((barr[1] & 0xFFL) << 8);
		value += ((barr[2] & 0xFFL) << 16);
		value += ((barr[3] & 0xFFL) << 24);
		return valueOf(value);
	}

	/**
	 * Get the signed integer value represented as a little-endian byte array
	 * (four bytes long).
	 * @return The integer value represented as a little-endian byte array.
	 * @see #getBigEndianByteArray()
	 * @see #fromBigEndianByteArray(byte[])
	 */
	public byte[] getLittleEndianByteArray()
	{
		byte[] res = new byte[4];
		res[0] = (byte) (m_value >> 24);
		res[1] = (byte) (m_value >> 16);
		res[2] = (byte) (m_value >> 8);
		res[3] = (byte) m_value;
		return res;
	}

	/**
	 * Create a signed integer value from an four bytes long little-endian byte
	 * array.
	 * @param barr The byte array. It must be four bytes long.
	 * @return The signed integer value.
	 * @throws IllegalArgumentException If the byte array is not four bytes
	 * long.
	 * @see #getLittleEndianByteArray()
	 * @see #fromBigEndianByteArray(byte[])
	 */
	public static SignedInteger fromLittleEndianByteArray(byte[] barr) throws IllegalArgumentException
	{
		if (barr.length != 4)
		{
			throw new IllegalArgumentException("Illegal size of supplied byte array: " + barr.length + ". It must be four bytes long");
		}
		int value = barr[3] & 0xFF;
		value += ((barr[2] & 0xFFL) << 8);
		value += ((barr[1] & 0xFFL) << 16);
		value += ((barr[0] & 0xFFL) << 24);
		return valueOf(value);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o != null && o instanceof SignedInteger)
		{
			return m_value == ((SignedInteger) o).m_value;
		}
		else
		{
			return false;
		}
	}

	@Override
	public int hashCode()
	{
		return m_value;
	}

	public int compareTo(SignedInteger l2)
	{
		return Integer.valueOf(m_value).compareTo(l2.m_value);
	}

	@Override
	public String toString()
	{
		return "" + m_value;
	}
}
