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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.at4j.support.io.BitOutput;

/**
 * This callback is called by the {@link BlockEncoder} when it has encoded its
 * block.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class BlockEncodedCallback
{
	private final int m_blockNo;
	private final EncodedBlockWriter m_writer;
	private final ByteArrayOutputStream m_byteOut;
	private final BitOutput m_bitOut;

	BlockEncodedCallback(final int blockNo, final ByteArrayOutputStream byteOut, final BitOutput bitOut, final EncodedBlockWriter writer)
	{
		m_blockNo = blockNo;
		m_writer = writer;
		m_byteOut = byteOut;
		m_bitOut = bitOut;
	}

	/**
	 * This is called by the {@link BlockEncoder} when it is done.
	 */
	void reportBlockDone() throws IOException
	{
		m_writer.writeBlock(m_blockNo, new EncodedBlockData(m_byteOut.toByteArray(), m_bitOut.getNumberOfBitsInUnfinishedByte(), m_bitOut.getUnfinishedByte()));
	}
}
