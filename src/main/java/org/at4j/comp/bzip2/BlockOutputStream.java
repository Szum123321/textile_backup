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
import java.io.OutputStream;
import java.util.Arrays;

import org.at4j.support.io.BitOutput;
import org.at4j.support.io.LittleEndianBitOutputStream;

/**
 * Used by {@link BZip2OutputStream} to RLE encode data and then write it to
 * compressed blocks.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class BlockOutputStream extends OutputStream
{
	/**
	 * The different states of the run length encoder.
	 */
	private static enum RLEState
	{
		ENCODING_SINGLE, COUNTING_MULTIPLE;
	}

	// The maximum number of encoded repeated bytes.
	private static final int MAX_NO_OF_RLE_REPEATS = 251;

	// The state of the run length encoder.
	private RLEState m_rleState;
	// The last byte value that write was called with. Used to keep track of
	// the run length encoding.
	private int m_last = -1;
	// How many equal bytes in a row has write been called with. Used to keep
	// track of the run length encoding.
	private int m_numberOfSame;
	// Encoded data is written to this.
	private final BitOutput m_wrapped;
	// The size of a Burrows Wheeler block, in bytes.
	private final int m_blockSize;
	// How many times should the Huffman trees be refined before encoding data?
	private final int m_numberOfHuffmanTreeRefinementIterations;
	// Bit flags indicating which bytes that occur at least once in the current
	// block.
	private boolean[] m_seenDifferentBytesInCurBlock;
	// The data in the current block.
	private byte[] m_block;
	// If we are using separate encoding threads, this executor is used to
	// schedule blocks for execution. Otherwise it is null.
	private final BZip2EncoderExecutorServiceImpl m_encodingExecutor;
	// A token identifying who owns the errors that may be caused by jobs that
	// we might schedule in the executor. This is null if no executor is used.
	private final Object m_errorOwner;

	// Contains preallocated data structures. Used to reduce the number of
	// temporary objects that are created and thus avoid time spent gc:ing.
	// This is null if an executor is used for encoding.
	private final EncodingScratchpad m_scratchpad;

	// If we use several encoder threads, this object is used for writing the
	// encoded blocks in the right order. Otherwise it is null.
	private final EncodedBlockWriter m_encodedBlockWriter;

	// The checksum for the current block.
	private CRC m_blockChecksum;
	// The checksum for the entire file.
	private int m_fileChecksum = 0;

	// The number of different bytes seen in the current block.
	private int m_noSeenDifferentBytesInCurBlock;
	private int m_blockPointer;

	private int m_blockNo = 0;

	BlockOutputStream(BitOutput wrapped, int blockSize, int numberOfHuffmanTreeRefinementIterations, BZip2EncoderExecutorServiceImpl ex, Object errorOwner, EncodedBlockWriter ebw, EncodingScratchpad sp)
	{
		// Can only have one, not both.
		assert ex == null ^ sp == null;

		m_wrapped = wrapped;
		m_blockSize = blockSize;
		m_numberOfHuffmanTreeRefinementIterations = numberOfHuffmanTreeRefinementIterations;
		m_blockChecksum = new CRC();
		m_scratchpad = sp;
		// May be null.
		m_encodingExecutor = ex;
		// May be null
		m_errorOwner = errorOwner;
		// May be null.
		m_encodedBlockWriter = ebw;

		startNewBlock();
	}

	private void startNewBlock()
	{
		m_blockPointer = 0;

		if (m_encodingExecutor != null)
		{
			// We use several threads for encoding. Create new instances for
			// data that may be used right now by an encoder.
			m_seenDifferentBytesInCurBlock = new boolean[256];
			m_block = new byte[m_blockSize + ThreeWayRadixQuicksort.DATA_OVERSHOOT];
		}
		else
		{
			// We encode in this thread. It is safe to reuse variables.
			if (m_seenDifferentBytesInCurBlock == null)
			{
				m_seenDifferentBytesInCurBlock = new boolean[256];
			}
			else
			{
				Arrays.fill(m_seenDifferentBytesInCurBlock, false);
			}

			if (m_block == null)
			{
				m_block = new byte[m_blockSize + ThreeWayRadixQuicksort.DATA_OVERSHOOT];
			}
		}
		m_noSeenDifferentBytesInCurBlock = 0;

		// Reset the run length encoder state
		m_last = -1;
		m_numberOfSame = 0;
		m_rleState = RLEState.ENCODING_SINGLE;
	}

	private boolean isFull()
	{
		return m_blockPointer == m_blockSize;
	}

	private boolean isEmpty()
	{
		return m_blockPointer == 0;
	}

	int getFileChecksum()
	{
		return m_fileChecksum;
	}

	/**
	 * Write a compressed data block.
	 */
	private void writeCurBlock() throws IOException
	{
		final int blockChecksum = m_blockChecksum.getValue();
		m_blockChecksum = new CRC();
		if (m_encodingExecutor == null)
		{
			// Encode the block in the current thread.
			BlockEncoder be = new BlockEncoder(m_block, m_blockNo, m_blockPointer, blockChecksum, m_seenDifferentBytesInCurBlock, m_noSeenDifferentBytesInCurBlock, m_numberOfHuffmanTreeRefinementIterations, m_wrapped, null);
			be.setScratchpad(m_scratchpad);
			be.encode();
		}
		else
		{
			// Hand off the block to another thread for encoding.

			// Allocate an output buffer that is 2/3rds of the size of the
			// written data.
			ByteArrayOutputStream baos = new ByteArrayOutputStream((2 * m_blockPointer) / 3);
			BitOutput out = new LittleEndianBitOutputStream(baos);
			BlockEncodedCallback bec = new BlockEncodedCallback(m_blockNo, baos, out, m_encodedBlockWriter);
			BlockEncoder be = new BlockEncoder(m_block, m_blockNo, m_blockPointer, blockChecksum, m_seenDifferentBytesInCurBlock, m_noSeenDifferentBytesInCurBlock, m_numberOfHuffmanTreeRefinementIterations, out, bec);
			m_encodingExecutor.execute(new BlockEncoderRunnable(be, m_errorOwner));
		}

		// Update the file checksum
		m_fileChecksum = (m_fileChecksum << 1) | (m_fileChecksum >>> 31);
		m_fileChecksum ^= blockChecksum;

		m_blockNo++;
	}

	/**
	 * Write a single byte.
	 */
	private void writeByte(final int b) throws IOException
	{
		m_block[m_blockPointer++] = (byte) (b & 0xFF);
		if (!m_seenDifferentBytesInCurBlock[b])
		{
			m_seenDifferentBytesInCurBlock[b] = true;
			m_noSeenDifferentBytesInCurBlock++;
		}

		if (isFull())
		{
			//			File f = new File("/tmp/block_" + ++m_blockNo + ".dat");
			//			OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
			//			try
			//			{
			//				os.write(m_block, 0, m_blockPointer);
			//			}
			//			finally
			//			{
			//				os.close();
			//			}

			writeCurBlock();
			startNewBlock();
		}
	}

	@Override
	public void write(final int b) throws IOException
	{
		// Run length encode
		switch (m_rleState)
		{
			case ENCODING_SINGLE:
				if (b == m_last)
				{
					m_numberOfSame++;
					if (m_numberOfSame == 4)
					{
						if (m_blockPointer == m_blockSize - 1)
						{
							// Corner case. bzip2 cannot handle blocks that end
							// with four equal bytes. End this block one byte
							// earlier.
							writeCurBlock();
							startNewBlock();
							write(b);
							return;
						}
						else
						{
							// Four equal in a row. Change state
							m_rleState = RLEState.COUNTING_MULTIPLE;
							m_numberOfSame = 0;
						}
					}
				}
				else
				{
					m_last = b;
					m_numberOfSame = 1;
				}
				m_blockChecksum.update(b);
				writeByte(b);
				break;

			case COUNTING_MULTIPLE:
				if (b == m_last)
				{
					m_numberOfSame++;
					if (m_numberOfSame == MAX_NO_OF_RLE_REPEATS)
					{
						// Cannot repeat this anymore. Update checksum, write
						// and switch state.
						for (int i = 0; i < MAX_NO_OF_RLE_REPEATS; i++)
						{
							m_blockChecksum.update(b);
						}
						writeByte(MAX_NO_OF_RLE_REPEATS);
						m_rleState = RLEState.ENCODING_SINGLE;
						m_numberOfSame = 0;
					}
				}
				else
				{
					// A byte that is not same as the last. Stop counting,
					// update the checksum and change state.
					for (int i = 0; i < m_numberOfSame; i++)
					{
						m_blockChecksum.update(m_last);
					}
					writeByte(m_numberOfSame);
					m_blockChecksum.update(b);
					writeByte(b);
					m_numberOfSame = 1;
					m_last = b;
					m_rleState = RLEState.ENCODING_SINGLE;
				}
				break;

			default:
				throw new RuntimeException("Unknown encoding state " + m_rleState + ". This is a bug");
		}
	}

	@Override
	public void write(final byte[] data) throws IOException
	{
		for (int i = 0; i < data.length; i++)
		{
			write(data[i] & 0xFF);
		}
	}

	@Override
	public void write(final byte[] data, final int offset, final int len) throws IOException
	{
		// Range validation is done by BZip2OutputStream
		for (int i = offset; i < offset + len; i++)
		{
			write(data[i] & 0xFF);
		}
	}

	@Override
	public void close() throws IOException
	{
		if (m_rleState == RLEState.COUNTING_MULTIPLE)
		{
			// Update the checksum and write the current count.
			for (int i = 0; i < m_numberOfSame; i++)
			{
				m_blockChecksum.update(m_last & 0xFF);
			}
			writeByte(m_numberOfSame);
		}

		if (!isEmpty())
		{
			writeCurBlock();
		}

		if (m_encodedBlockWriter != null)
		{
			// Tell the encoded block writer that we're done.
			m_encodedBlockWriter.writeBlock(m_blockNo, null);
		}

		// Don't close the wrapped BitOutput. It will be used later on to write
		// the EOF block.

		super.close();
	}
}
