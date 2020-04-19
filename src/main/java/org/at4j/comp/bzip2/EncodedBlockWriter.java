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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.at4j.support.io.BitOutput;

/**
 * This is used to write encoded blocks in the right order when several encoding
 * threads are used with the {@link BZip2OutputStream}.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class EncodedBlockWriter
{
	// All variables are protected by this object's intrinsic lock
	private final BitOutput m_out;
	private final Map<Integer, EncodedBlockData> m_savedBlocks = new HashMap<Integer, EncodedBlockData>();
	// This latch is used to signal to the bzip2 output stream when this writer
	// is finished.
	private final CountDownLatch m_doneLatch = new CountDownLatch(1);
	private int m_nextBlockToWrite = 0;
	private boolean m_hasError;

	EncodedBlockWriter(BitOutput out)
	{
		m_out = out;
	}

	private void writeEncodedBlockData(final EncodedBlockData bd) throws IOException
	{
		m_out.writeBytes(bd.m_bytes, 0, bd.m_bytes.length);
		if (bd.m_noBits > 0)
		{
			m_out.writeBits(bd.m_bitValue, bd.m_noBits);
		}
	}

	private void writeBlockInternal(final int blockNo, final EncodedBlockData blockData) throws IOException
	{
		if (blockData == null)
		{
			// We're done
			m_doneLatch.countDown();
		}
		else
		{
			writeEncodedBlockData(blockData);

			while (m_savedBlocks.containsKey(++m_nextBlockToWrite))
			{
				final EncodedBlockData savedBd = m_savedBlocks.get(m_nextBlockToWrite);
				if (savedBd != null)
				{
					writeEncodedBlockData(savedBd);
				}
				else
				{
					m_doneLatch.countDown();
					break;
				}
			}
		}
	}

	/**
	 * It is not time to write this block just yet. Save it until it is time.
	 * @param blockNo The block number.
	 * @param blockData The block data.
	 */
	private void saveBlock(final int blockNo, EncodedBlockData blockData)
	{
		m_savedBlocks.put(blockNo, blockData);
	}

	/**
	 * Write the block data to the output if it is the next block to write. If
	 * not, queue it for later writing.
	 * @param blockNo The block number.
	 * @param blockData The block data or {@code null} as an end of stream
	 * marker.
	 * @throws IOException
	 */
	synchronized void writeBlock(final int blockNo, final EncodedBlockData blockData) throws IOException
	{
		if (m_hasError)
		{
			return;
		}

		try
		{
			if (blockNo == m_nextBlockToWrite)
			{
				writeBlockInternal(blockNo, blockData);
			}
			else
			{
				saveBlock(blockNo, blockData);
			}
		}
		catch (Error e)
		{
			m_hasError = true;
			m_doneLatch.countDown();
			throw e;
		}
		catch (RuntimeException e)
		{
			m_hasError = true;
			m_doneLatch.countDown();
			throw e;
		}
		catch (IOException e)
		{
			m_hasError = true;
			m_doneLatch.countDown();
			throw e;
		}
	}

	void waitFor() throws InterruptedException
	{
		m_doneLatch.await();
	}
}
