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
 * This is used by the {@link BlockOutputStream} to encode a block in a separate
 * encoding thread. It uses a {@link BlockEncoder} to do the actual encoding.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class BlockEncoderRunnable implements Runnable
{
	private final BlockEncoder m_encoder;
	private final Object m_errorOwner;

	BlockEncoderRunnable(final BlockEncoder be, final Object errorOwner)
	{
		m_encoder = be;
		m_errorOwner = errorOwner;
	}

	public void run()
	{
		try
		{
			m_encoder.setScratchpad(((EncodingThread) Thread.currentThread()).getScratchpad());
			m_encoder.encode();
		}
		catch (IOException e)
		{

			((EncodingThread) Thread.currentThread()).getErrorState().registerError(e, m_errorOwner);
		}
		catch (RuntimeException e)
		{
			((EncodingThread) Thread.currentThread()).getErrorState().registerError(e, m_errorOwner);
		}
		catch (Error e)
		{

			((EncodingThread) Thread.currentThread()).getErrorState().registerError(e, m_errorOwner);
		}
	}
}
