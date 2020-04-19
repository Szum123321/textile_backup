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
 * This is a base class for exceptions in this project. It inherits
 * {@link RuntimeException}, so it is unchecked.
 * @author Karl Gustafsson
 * @since 1.0
 */
public class At4JException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	/**
	 * Create an exception with a message.
	 * @param msg The message.
	 */
	public At4JException(String msg)
	{
		super(msg);
	}

	/**
	 * Create an exception that wraps another exception.
	 * @param t The other exception.
	 */
	public At4JException(Throwable t)
	{
		super(t);
	}

	/**
	 * Create an exception that wraps another exception and has a message.
	 * @param msg The message.
	 * @param t The other exception.
	 */
	public At4JException(String msg, Throwable t)
	{
		super(msg, t);
	}
}
