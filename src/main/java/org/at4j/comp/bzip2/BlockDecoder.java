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
import java.util.Arrays;

import org.at4j.support.comp.ByteMoveToFront;
import org.at4j.support.comp.IntMoveToFront;
import org.at4j.support.io.LittleEndianBitInputStream;
import org.at4j.support.lang.At4JException;
import org.at4j.support.lang.UnsignedInteger;

/**
 * This is used by the {@link BZip2InputStream} to decode data blocks.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class BlockDecoder
{
	// The magic number identifying a block of compressed data
	private static final byte[] COMPRESSED_BLOCK_MAGIC = new byte[] { (byte) 0x31, (byte) 0x41, (byte) 0x59, (byte) 0x26, (byte) 0x53, (byte) 0x59 };
	// The magic number identifying the end of stream block
	private static final byte[] EOS_BLOCK_MAGIC = new byte[] { (byte) 0x17, (byte) 0x72, (byte) 0x45, (byte) 0x38, (byte) 0x50, (byte) 0x90 };

	// The number of symbols to read from each Huffman tree before switching
	private static final int SYMBOLS_TO_READ_FROM_EACH_TREE = 50;

	// The symbol value of the special RUNA symbol.
	private static final int RUNA_SYMBOL = 0;
	// The symbol value of the special RUNB symbol.
	private static final int RUNB_SYMBOL = 1;

	private static final int MAX_NO_OF_MTF_SYMBOLS = 258;

	private static final byte[] INITIAL_MOVE_TO_FRONT_ALPHABET = new byte[MAX_NO_OF_MTF_SYMBOLS];
	static
	{
		for (int i = 0; i < MAX_NO_OF_MTF_SYMBOLS; i++)
		{
			INITIAL_MOVE_TO_FRONT_ALPHABET[i] = (byte) i;
		}
	}

	private final LittleEndianBitInputStream m_in;
	private final int m_blockSize;

	// Data read from the block header

	// Block checksum (CRC)
	private int m_readBlockChecksum;
	// The pointer to the original data used in the BW transform
	private int m_originalDataPointer;
	// The Huffman trees used for decompression
	private HighValueBranchHuffmanTree[] m_huffmanTrees;
	// The EOB (End Of Block) symbol index.
	private int m_endOfBlockSymbol;
	// The number of times that the Huffman trees are switched in the input.
	// The trees are switched every 50 bytes.
	private int m_numberOfTimesHuffmanTreesAreSwitched;
	private int[] m_treeUse;
	// Mapping between symbol values and byte values.
	private byte[] m_symbolSequenceNos;
	// Frequency of each byte in the pre-BW data
	private int[] m_byteFrequencies;

	// State variables

	// The number of the currently selected Huffman tree
	private HighValueBranchHuffmanTree m_curTree;
	// The number of symbols left to read from the current Huffman tree 
	private int m_symbolsLeftToReadFromCurTree;
	// The current number of Huffman tree switches
	private int m_switchNo;
	// A counter for the number of bytes decoded in this block.
	private int m_noBytesDecoded;
	private ByteMoveToFront m_mtfTransformer;
	// This will hold the decoded data (before the Burrows Wheeler decoding)
	private final byte[] m_decoded;

	BlockDecoder(LittleEndianBitInputStream in, int blockSize)
	{
		m_in = in;
		m_blockSize = blockSize;
		m_decoded = new byte[blockSize];
	}

	private void throwIOException(String msg) throws IOException
	{
		throw new IOException(msg + ". Position in input stream: " + m_in.getNumberOfBytesRead());
	}

	private void checkInterrupted() throws InterruptedException
	{
		if (Thread.interrupted())
		{
			throw new InterruptedException();
		}
	}

	private void trace(String s)
	{
		System.out.println(s);
	}

	static HighValueBranchHuffmanTree decodeHuffmanTree(final int totalNumberOfSymbols, final LittleEndianBitInputStream in) throws IOException
	{
		int[] symbolLengths = new int[totalNumberOfSymbols];

		// Starting bit length for Huffman deltas in this tree
		int currentBitLength = in.readBits(5);
		if (currentBitLength > 20)
		{
			throw new IOException("Invalid starting bit length for Huffman deltas: " + currentBitLength + ". Must be <= 20");
		}

		// Initialize min and max lengths per tree with values that
		// will certainly be overwritten.
		int minBitLengthPerTree = 20;
		int maxBitLengthPerTree = 0;

		for (int j = 0; j < totalNumberOfSymbols; j++)
		{
			while (in.readBit())
			{
				currentBitLength += in.readBit() ? -1 : 1;
				if ((currentBitLength < 1) || (currentBitLength > 20))
				{
					throw new IOException("Invalid bit length " + currentBitLength);
				}
			}
			symbolLengths[j] = currentBitLength;

			if (currentBitLength < minBitLengthPerTree)
			{
				minBitLengthPerTree = currentBitLength;
			}
			if (currentBitLength > maxBitLengthPerTree)
			{
				maxBitLengthPerTree = currentBitLength;
			}
		}
		return new HighValueBranchHuffmanTree(symbolLengths, minBitLengthPerTree, maxBitLengthPerTree, false);
	}

	private void readCompressedBlockHeader() throws IOException
	{
		byte[] barr = new byte[4];

		// Block checksum
		m_readBlockChecksum = (int) UnsignedInteger.fromLittleEndianByteArrayToLong(m_in.readBytes(barr, 0, 4), 0);

		// Randomized block?
		if (m_in.readBit())
		{
			throwIOException("Randomized block mode is not supported");
		}

		// Starting pointer into BWT
		m_in.readBytes(barr, 1, 3);
		barr[0] = 0;
		m_originalDataPointer = (int) UnsignedInteger.fromLittleEndianByteArrayToLong(barr, 0);
		if (m_originalDataPointer > m_blockSize)
		{
			throw new IOException("Invalid starting pointer " + m_originalDataPointer + ". It must be less than the block size " + m_blockSize);
		}

		// Huffman used codes
		boolean[] usedSymbols = new boolean[256];
		int numberOfUsedSymbols = 0;

		boolean[] inUseBlocks = new boolean[16];
		for (int i = 0; i < 16; i++)
		{
			inUseBlocks[i] = m_in.readBit();
		}
		for (int i = 0; i < 16; i++)
		{
			if (inUseBlocks[i])
			{
				for (int j = 0; j < 16; j++)
				{
					if (m_in.readBit())
					{
						usedSymbols[i * 16 + j] = true;
						numberOfUsedSymbols++;
					}
				}
			}
		}
		if (numberOfUsedSymbols == 0)
		{
			throwIOException("No symbols used in table");
		}

		// Create a mapping for the sequence numbers of all used bytes
		m_symbolSequenceNos = new byte[numberOfUsedSymbols];
		int useIndex = 0;
		for (int i = 0; i < 256; i++)
		{
			if (usedSymbols[i])
			{
				m_symbolSequenceNos[useIndex++] = (byte) (i & 0xFF);
			}
		}
		assert useIndex == numberOfUsedSymbols;

		m_byteFrequencies = new int[256];

		// The number of Huffman trees to use
		int numberOfHuffmanTrees = m_in.readBits(3);
		if (numberOfHuffmanTrees < 2 || numberOfHuffmanTrees > 6)
		{
			throwIOException("Invalid number of Huffman trees " + numberOfHuffmanTrees + ". Must be between 2 and 6 (inclusive)");
		}

		// The number of times the trees to use are swapped in the input.
		// The trees are swapped each 50 bytes.
		m_numberOfTimesHuffmanTreesAreSwitched = m_in.readBitsLittleEndian(15);
		if (m_numberOfTimesHuffmanTreesAreSwitched < 1)
		{
			throwIOException("Invalid number of times the Huffman trees are switched in the input: " + m_numberOfTimesHuffmanTreesAreSwitched);
		}

		// Zero-terminated bit runs for each tree switch
		int[] treeUseMtf = new int[m_numberOfTimesHuffmanTreesAreSwitched];
		for (int i = 0; i < m_numberOfTimesHuffmanTreesAreSwitched; i++)
		{
			treeUseMtf[i] = 0;
			while (m_in.readBit())
			{
				treeUseMtf[i]++;
			}
			if (treeUseMtf[i] > numberOfHuffmanTrees)
			{
				throwIOException("Invalid Huffman tree use MTF " + treeUseMtf[i] + ". Must be less than the number of Huffman trees, " + numberOfHuffmanTrees);
			}
		}

		// Decode the tree use MTF values
		m_treeUse = new int[m_numberOfTimesHuffmanTreesAreSwitched];
		// The "alphabet" for the MTF encoding -- the indices of the different
		// tree uses.
		int[] treeUseIndices = new int[numberOfHuffmanTrees];
		for (int i = 0; i < numberOfHuffmanTrees; i++)
		{
			treeUseIndices[i] = i;
		}
		new IntMoveToFront(treeUseIndices).decode(treeUseMtf, m_treeUse);

		// Settings for the Huffman trees

		// The total number of used symbols is the value we calculated above - 1
		// + RUNA, RUNB and an end of stream marker. 
		int totalNumberOfSymbols = numberOfUsedSymbols + 2;
		m_huffmanTrees = new HighValueBranchHuffmanTree[numberOfHuffmanTrees];
		for (int i = 0; i < numberOfHuffmanTrees; i++)
		{
			m_huffmanTrees[i] = decodeHuffmanTree(totalNumberOfSymbols, m_in);
		}

		// The symbol value for the end of the data block.
		m_endOfBlockSymbol = totalNumberOfSymbols - 1;
	}

	private void selectNewHuffmanTree() throws IOException
	{
		if (m_switchNo >= m_numberOfTimesHuffmanTreesAreSwitched)
		{
			throwIOException("One Huffman tree switch too many: " + m_switchNo);
		}
		m_symbolsLeftToReadFromCurTree = SYMBOLS_TO_READ_FROM_EACH_TREE;
		m_curTree = m_huffmanTrees[m_treeUse[m_switchNo]];
		m_switchNo++;
	}

	private int readSymbol() throws IOException
	{
		if (m_symbolsLeftToReadFromCurTree == 0)
		{
			selectNewHuffmanTree();
		}
		final int symbol = m_curTree.readNext(m_in);
		m_symbolsLeftToReadFromCurTree--;
		return symbol;
	}

	private void decodeSingleByte(final int symbolMtf) throws IOException
	{
		// Move To Front decode the symbol
		final int byteIndex = m_mtfTransformer.decode(symbolMtf - 1) & 0xFF;

		final byte value = m_symbolSequenceNos[byteIndex];
		m_decoded[m_noBytesDecoded++] = value;
		m_byteFrequencies[value & 0xFF]++;
	}

	// returns the next symbol
	private int handleRunaAndRunb(int symbol) throws IOException
	{
		int n = 1;
		int multiplier = 0;
		while (symbol == RUNA_SYMBOL || symbol == RUNB_SYMBOL)
		{
			if (symbol == RUNA_SYMBOL)
			{
				multiplier += n;
			}
			else
			{
				multiplier += 2 * n;
			}
			// Multiply n with 2
			n <<= 1;
			symbol = readSymbol();
		}

		// The repeated value is at the front of the MTF list
		final int byteIndex = m_mtfTransformer.decode(0) & 0xFF;
		final byte value = m_symbolSequenceNos[byteIndex];
		if (multiplier == 1)
		{
			m_decoded[m_noBytesDecoded++] = value;
			m_byteFrequencies[value & 0xFF]++;
		}
		else
		{
			Arrays.fill(m_decoded, m_noBytesDecoded, m_noBytesDecoded + multiplier, value);
			m_noBytesDecoded += multiplier;
			m_byteFrequencies[value & 0xFF] += multiplier;
		}
		return symbol;
	}

	CompressedDataBlock readCompressedDataBlock() throws IOException, InterruptedException
	{
		readCompressedBlockHeader();

		int symbol = readSymbol();

		while (true)
		{
			checkInterrupted();

			if (symbol == RUNA_SYMBOL || symbol == RUNB_SYMBOL)
			{
				symbol = handleRunaAndRunb(symbol);
			}
			else if (symbol == m_endOfBlockSymbol)
			{
				BurrowsWheelerDecoder bwd = new BurrowsWheelerDecoder(m_decoded, m_noBytesDecoded, m_byteFrequencies, m_originalDataPointer);
				return new CompressedDataBlock(new RLEDecodingInputStream(bwd.decode(), m_readBlockChecksum), m_readBlockChecksum);
			}
			else
			{
				decodeSingleByte(symbol);
				symbol = readSymbol();
			}
		}
	}

	private void initDecoderState()
	{
		// Initialize the MTF alphabet
		final byte[] moveToFrontAlphabet = new byte[MAX_NO_OF_MTF_SYMBOLS];
		System.arraycopy(INITIAL_MOVE_TO_FRONT_ALPHABET, 0, moveToFrontAlphabet, 0, MAX_NO_OF_MTF_SYMBOLS);
		m_mtfTransformer = new ByteMoveToFront(moveToFrontAlphabet);
		m_curTree = null;
		m_symbolsLeftToReadFromCurTree = 0;
		m_switchNo = 0;
		m_noBytesDecoded = 0;
	}

	Block getNextBlock() throws IOException
	{
		initDecoderState();

		byte[] barr = new byte[6];
		m_in.readBytes(barr, 0, 6);
		if (Arrays.equals(COMPRESSED_BLOCK_MAGIC, barr))
		{
			trace("Found block of compressed data");
			try
			{
				return readCompressedDataBlock();
			}
			catch (InterruptedException e)
			{
				throw new At4JException(e);
			}
		}
		else if (Arrays.equals(EOS_BLOCK_MAGIC, barr))
		{
			trace("Found end of stream block");
			m_in.readBytes(barr, 0, 4);
			int readCrc32 = (int) UnsignedInteger.fromLittleEndianByteArrayToLong(barr, 0);
			return new EosBlock(readCrc32);
		}
		else
		{
			throwIOException("Invalid block header " + Arrays.toString(barr) + ". Expected compressed data block or end of stream block");
			// Never reached
			return null;
		}
	}
}
