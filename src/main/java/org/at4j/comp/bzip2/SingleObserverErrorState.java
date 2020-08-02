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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is used to propagate errors from encoding threads to the thread using
 * the {@link BZip2OutputStream} when there is only one object using the
 * encoder.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class SingleObserverErrorState implements ErrorState
{
	private final AtomicReference<Throwable> m_exception = new AtomicReference<>();

	public void checkAndClearErrors(Object ownerToken) throws Error, RuntimeException, IOException
	{
		Throwable t = m_exception.getAndSet(null);
		if (t != null)
		{
			if (t instanceof IOException)
			{
				throw (IOException) t;
			}
			else if (t instanceof RuntimeException)
			{
				throw (RuntimeException) t;
			}
			else if (t instanceof Error)
			{
				throw (Error) t;
			}
			else
			{
				throw new RuntimeException(t);
			}
		}
	}

	public void registerError(Throwable t, Object ownerToken)
	{
		m_exception.set(t);
	}
}
