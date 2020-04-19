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

/**
 * This is used to keep track of encoding errors.
 * <p>
 * Every error is registered with an owner token that is a unique identifier for
 * the object that is affected by the error. The owner token object must have a
 * good {@link Object#hashCode()} method.
 * @author Karl Gustafsson
 * @since 1.1
 */
interface ErrorState
{
	/**
	 * Register an {@link Exception} or an {@link Error}.
	 * @param t The exception or error.
	 * @param ownerToken A unique identifier for the error owner, i.e. the
	 * object that the encoding thread is performing work for.
	 */
	void registerError(Throwable t, Object ownerToken);

	/**
	 * Check for errors.
	 * @param ownerToken The owner.
	 * @throws Error If there is a registered {@link Error} for this owner.
	 * @throws RuntimeException If there is a registered
	 * {@link RuntimeException} for this owner.
	 * @throws IOException If there is a registered {@link IOException} for this
	 * owner.
	 */
	void checkAndClearErrors(Object ownerToken) throws Error, RuntimeException, IOException;
}
