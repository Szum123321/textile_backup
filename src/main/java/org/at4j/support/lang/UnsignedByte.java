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
 * This object represents an unsigned byte (eight bits) with a value between
 * {@code 0} and {@code 255} (inclusive). It is immutable.
 * <p>
 * Unsigned byte instances are created by calling any of the static {@code
 * valueOf} methods on this class.
 * @author Karl Gustafsson
 * @since 1.0
 * @see UnsignedShort
 * @see UnsignedInteger
 * @see UnsignedLong
 */
public final class UnsignedByte implements Serializable, Comparable<UnsignedByte>
{
	private static final long serialVersionUID = 1L;

	/**
	 * The maximum value of an unsigned byte (255).
	 */
	public static final short MAX_VALUE = (1 << 8) - 1;

	/**
	 * The minimum value of an unsigned byte (0).
	 */
	public static final short MIN_VALUE = 0;

	/**
	 * The value 0.
	 */
	public static final UnsignedByte ZERO = new UnsignedByte((byte) 0);

	/**
	 * The value 1.
	 */
	public static final UnsignedByte ONE = new UnsignedByte((byte) 1);

	private final byte m_value;

	private UnsignedByte(byte value)
	{
		m_value = value;
	}

	/**
	 * Create an unsigned byte value from the supplied byte value. The supplied
	 * value is treated as if it was unsigned, which means that negative
	 * argument values will result in unsigned byte values between 128 and 255.
	 * @param value The value.
	 * @return The unsigned byte value.
	 * @see #valueOf(short)
	 * @see #valueOf(int)
	 */
	public static UnsignedByte valueOf(byte value)
	{
		switch (value)
		{
			case 0:
				return ZERO;
			case 1:
				return ONE;
			default:
				return new UnsignedByte(value);
		}
	}

	private static UnsignedByte valueOfSafe(int value)
	{
		return valueOf((byte) (value & 0xFF));
	}

	/**
	 * Create a new unsigned byte value from the supplied {@code short} value
	 * which must be in the range {@code 0} to {@code 255} (inclusive).
	 * @param value The value.
	 * @return An unsigned byte value.
	 * @throws IllegalArgumentException If the supplied value is not in the
	 * permitted range.
	 */
	public static UnsignedByte valueOf(short value) throws IllegalArgumentException
	{
		if ((value < MIN_VALUE) || (value > MAX_VALUE))
		{
			throw new IllegalArgumentException("Illegal unsigned byte value " + value + ". It must be between " + MIN_VALUE + " and " + MAX_VALUE + " (inclusive)");
		}
		return valueOf((byte) (value & 0xFF));
	}

	/**
	 * Create a new unsigned byte value from the supplied {@code int} value
	 * which must be in the range {@code 0} to {@code 255} (inclusive).
	 * @param value The value.
	 * @return An unsigned byte value.
	 * @throws IllegalArgumentException If the supplied value is not in the
	 * permitted range.
	 */
	public static UnsignedByte valueOf(int value) throws IllegalArgumentException
	{
		if ((value < MIN_VALUE) || (value > MAX_VALUE))
		{
			throw new IllegalArgumentException("Illegal unsigned byte value " + value + ". It must be between " + MIN_VALUE + " and " + MAX_VALUE + " (inclusive)");
		}
		return valueOf((byte) (value & 0xFF));
	}

	/**
	 * Get the unsigned byte value as an {@code int}.
	 * @return The value.
	 */
	public int intValue()
	{
		return m_value & 0xFF;
	}

	/**
	 * Get the unsigned byte value as a {@code short}.
	 * @return The value.
	 */
	public short shortValue()
	{
		return (short) (m_value & 0xFF);
	}

	/**
	 * Get the unsigned byte value as a signed byte value between {@code -128}
	 * and {@code 127} (inclusive).
	 * @return The value.
	 */
	public byte byteValue()
	{
		return m_value;
	}

	/**
	 * Is the specified bit set in the byte value?
	 * @param no The index number of the bit. Bit 0 is the bit representing the
	 * value 1, bit 7 is the bit representing the value 128.
	 * @return {@code true} if the specified bit is set.
	 * @throws IllegalArgumentException If {@code no} is not in the range
	 * {@code 0 <= no <= 7} (inclusive).
	 */
	public boolean isBitSet(int no) throws IllegalArgumentException
	{
		if (no < 0 || no > 7)
		{
			throw new IllegalArgumentException("Invalid bit number " + no + ". It must be between 0 and 7 (inclusive)");
		}
		return (m_value & (1 << no)) > 0;
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof UnsignedByte) && (((UnsignedByte) o).m_value == m_value);
	}

	@Override
	public int hashCode()
	{
		return m_value;
	}

	public int compareTo(UnsignedByte b2)
	{
		return intValue() - b2.intValue();
	}

	@Override
	public String toString()
	{
		return Short.toString((short) (m_value & 0xFF));
	}
}
