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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This {@link ErrorState} may have several observers which forces us to have to
 * care about the owner of each registered error.
 * <p>
 * This is used when sharing the same
 * {@link java.util.concurrent.ExecutorService} between several
 * {@link BZip2OutputStream}:s.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class MultipleObserverErrorState implements ErrorState
{
	private Map<Object, Throwable> m_errors = new ConcurrentHashMap<Object, Throwable>(4);

	public void checkAndClearErrors(Object ownerToken) throws Error, RuntimeException, IOException
	{
		Throwable t = m_errors.remove(ownerToken);
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
		m_errors.put(ownerToken, t);
	}
}
