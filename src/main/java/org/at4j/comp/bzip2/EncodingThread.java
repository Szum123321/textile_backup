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

/**
 * This is the kind of thread used for encoding bzip2 blocks.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class EncodingThread extends Thread
{
	private final EncodingScratchpad m_scratchpad = new EncodingScratchpad();
	private final ErrorState m_errorState;

	EncodingThread(Runnable r, ErrorState es)
	{
		super(r);
		m_errorState = es;
	}

	/**
	 * Get this thread's scratchpad.
	 */
	EncodingScratchpad getScratchpad()
	{
		return m_scratchpad;
	}

	ErrorState getErrorState()
	{
		return m_errorState;
	}
}
