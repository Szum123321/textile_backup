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

import org.at4j.comp.bzip2.BurrowsWheelerEncoder.BurrowsWheelerEncodingResult;
import org.at4j.support.comp.IntMoveToFront;
import org.at4j.support.io.BitOutput;

/**
 * This is used by the thread encoding a bzip2 block.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class BlockEncoder
{
	private static final byte[] BLOCK_MAGIC = new byte[] { 0x31, 0x41, 0x59, 0x26, 0x53, 0x59 };

	// The maximum Huffman tree depth
	private static final int MAX_HUFFMAN_BIT_LENGTH = 17;

	// The values of the RUNA and RUNB symbols
	private static final int RUNA_SYMBOL = 0;
	private static final int RUNB_SYMBOL = 1;

	private static final int MIN_NO_OF_HUFFMAN_TREES = 2;
	static final int MAX_NO_OF_HUFFMAN_TREES = 6;

	// The maximum number of different MTF symbols: 256 bytes + RUNA + RUNB +
	// EOB - one byte (the first symbol does not have to be encoded thanks to
	// MTF and RLE)
	static final int MAX_NO_OF_MTF_SYMBOLS = 258;

	// Write 50 symbols, then swap Huffman trees.
	static final int NO_OF_SYMBOLS_PER_SEGMENT = 50;

	// Categories used when optimizing Huffman trees
	// For each tree length, in which category does a segment belong depending
	// on its encoded length percentage? 
	static final int[][] CATEGORY_PER_NO_OF_TREES_AND_PERCENTAGE = new int[][] {
	// Two trees: cutoff at 30%
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
			// Three trees: cutoff at 18% and 45%
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
					2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 },
			// Four trees: cutoff at 15%, 30% and 55%
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
					3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3 },
			// Five trees: cutoff at 12%, 25%, 40% and 60%
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
					4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 },
			// Six trees: cutoff at 8%, 25%, 36%, 51% and 63%
			{ 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
					5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 } };

	private static final byte[] INITIAL_MTF_ALPHABET = new byte[MAX_NO_OF_MTF_SYMBOLS];
	static
	{
		for (int i = 0; i < INITIAL_MTF_ALPHABET.length; i++)
		{
			INITIAL_MTF_ALPHABET[i] = (byte) (i & 0xFF);
		}
	}

	private final byte[] m_block;
	private final int m_blockNo;
	private final int m_blockSize;
	private final int m_blockChecksum;
	// Bit flags indicating which bytes that occur at least once in this block
	private final boolean[] m_seenDifferentBytes;
	// The number of different bytes seen in this block
	private final int m_numberOfSeenDifferentBytes;
	private final int m_numberOfHuffmanTreeRefinementIterations;
	// Sink to write encoded data to.
	private final BitOutput m_out;
	// This callback is called when the block encoder is done. It may be null.
	private final BlockEncodedCallback m_blockEncoderCallback;

	// This is set by the encoding thread before calling encode
	private EncodingScratchpad m_scratchpad;

	BlockEncoder(final byte[] block, final int blockNo, final int blockSize, final int blockChecksum, final boolean[] seenDifferentBytes, final int numberOfSeenDifferentBytes, final int numberOfHuffmanTreeRefinementIterations,
			final BitOutput out, final BlockEncodedCallback bec)
	{
		m_block = block;
		m_blockNo = blockNo;
		m_blockSize = blockSize;
		m_blockChecksum = blockChecksum;
		m_seenDifferentBytes = seenDifferentBytes;
		m_numberOfSeenDifferentBytes = numberOfSeenDifferentBytes;
		m_numberOfHuffmanTreeRefinementIterations = numberOfHuffmanTreeRefinementIterations;
		m_out = out;
		m_blockEncoderCallback = bec;
	}

	void setScratchpad(EncodingScratchpad sp)
	{
		m_scratchpad = sp;
	}

	/**
	 * Get the seen byte values in the current block.
	 */
	private byte[] getSeenByteValues()
	{
		byte[] res = new byte[m_numberOfSeenDifferentBytes];
		int j = 0;
		for (int i = 0; i < 256; i++)
		{
			if (m_seenDifferentBytes[i])
			{
				res[j++] = (byte) (i & 0xFF);
			}
		}
		assert j == m_numberOfSeenDifferentBytes;
		return res;
	}

	/**
	 * Add RUNA and RUNB symbols to {@code res} at {@code outIndex} to represent
	 * {@code no} repetitions of the previous symbol.
	 * <p>
	 * This method is declared package-protected for the unit tests.
	 * @return The number of symbols added. outIndex should be incremented by
	 * this value by the caller.
	 */
	static int addRunaAndRunb(int[] res, int outIndex, int no)
	{
		int noWritten = 0;
		while (no > 0)
		{
			switch (no % 2)
			{
				case 1:
					res[outIndex + noWritten++] = RUNA_SYMBOL;
					no -= 1;
					break;
				case 0:
					res[outIndex + noWritten++] = RUNB_SYMBOL;
					no -= 2;
					break;
				default:
					// Should not occur unless we use relativistic arithmetic or
					// something...
					throw new RuntimeException();
			}
			no >>>= 1;
		}
		return noWritten;
	}

	/**
	 * Create a mapping between symbols and their index numbers in the array of
	 * symbols.
	 * @param symbols The symbols.
	 * @return An array containing the index number for each symbol that occurs
	 * in {@code symbols}.
	 */
	private byte[] createSequenceMap(byte[] symbols)
	{
		byte[] res = m_scratchpad.m_sequenceMap;
		byte index = 0;
		for (int i : symbols)
		{
			res[symbols[i] & 0xFF] = index++;
		}
		return res;
	}

	private static class MTFAndRLEResult
	{
		// The encoded data as MTF symbols.
		private final int[] m_encodedData;
		private final int m_dataLen;
		private final int m_noSeenDifferentSymbols;

		private MTFAndRLEResult(int[] symbols, int dataLen, int noSeenDifferentSymbols)
		{
			m_encodedData = symbols;
			m_dataLen = dataLen;
			m_noSeenDifferentSymbols = noSeenDifferentSymbols;
		}
	}

	/**
	 * Run MTF and RLE encoding of the data in {@code data}.
	 * @param data The data to encode.
	 * @param dataLen The data length.
	 * @param symbols An array containing all different symbols that occur in
	 * {@code data}.
	 * @return MTF and RLE encoded data.
	 */
	private MTFAndRLEResult moveToFrontAndRunLengthEncode(final byte[] data, final int dataLen, final byte[] symbols)
	{
		// This array will contain the run length encoded result. The result
		// will probably be shorter than data.length thanks to the run length
		// encoding, but data.length (+ 1 for the EOB symbol) is the worst case
		// length.
		boolean[] seenSymbols = new boolean[259];
		// RUNA and RUNB are always seen (even when they are not...)
		seenSymbols[0] = true;
		seenSymbols[1] = true;
		int noSeenSymbols = 2;

		// Initialize the move to front alphabet
		final byte[] mtfAlphabet = m_scratchpad.m_mtfAlphabet;
		System.arraycopy(INITIAL_MTF_ALPHABET, 0, mtfAlphabet, 0, mtfAlphabet.length);

		// The array to store the encoded data in.
		final int[] encodedData = m_scratchpad.m_encodedData;

		// Create a mapping between a symbol and its index number in the array
		// of symbols
		final byte[] sequenceMap = createSequenceMap(symbols);

		int lastSymbolIndex = 0;
		int curOutArrayIndex = 0;
		// A counter to keep track of the number of equal symbols in a row for
		// the run length encoding
		int noSame = 0;
		for (int curInArrayIndex = 0; curInArrayIndex < dataLen; curInArrayIndex++)
		{
			final byte curSymbolIndex = sequenceMap[data[curInArrayIndex] & 0xFF];
			if (curSymbolIndex == lastSymbolIndex)
			{
				noSame++;
			}
			else
			{
				if (noSame > 0)
				{
					// Run length encode
					curOutArrayIndex += addRunaAndRunb(m_scratchpad.m_encodedData, curOutArrayIndex, noSame);
					noSame = 0;
				}

				// Search for the current symbol in the MTF alphabet and count
				// the distance
				int j = 0;
				byte lastMtf = mtfAlphabet[0];

				while (mtfAlphabet[++j] != curSymbolIndex)
				{
					final byte nextLastMtf = mtfAlphabet[j];
					mtfAlphabet[j] = lastMtf;
					lastMtf = nextLastMtf;
				}
				// Swap the symbols in the MTF alphabet.
				mtfAlphabet[j] = lastMtf;
				mtfAlphabet[0] = curSymbolIndex;

				// Output the distance. Distance 1 gets the value 2 since
				// RUNA and RUNB have the values 0 and 1.
				int symbolVal = j + 1;
				encodedData[curOutArrayIndex++] = symbolVal;
				if (!seenSymbols[symbolVal])
				{
					seenSymbols[symbolVal] = true;
					noSeenSymbols++;
				}
				lastSymbolIndex = curSymbolIndex;
			}
		}
		if (noSame > 0)
		{
			// One last run length encoding
			curOutArrayIndex += addRunaAndRunb(encodedData, curOutArrayIndex, noSame);
		}
		return new MTFAndRLEResult(encodedData, curOutArrayIndex, noSeenSymbols);
	}

	private static class EncodeAllSegmentsResult
	{
		// The shortest encoded segment length for all segments.
		private int m_shortestLength;
		// The longest encoded segment length for all segments.
		private int m_longestLength;
		// A list with encoding results (the bit length) for each segment and
		// tree.
		private int[][] m_encodingResults;
		// For each segment, the index of the tree that gave the shortest
		// encoded block.
		private int[] m_treesUsed;
	}

	/**
	 * Encode all 50-byte segments with all trees and count the encoded lengths.
	 * By doing this we can select the best Huffman tree for each segment by
	 * seeing which tree that gave the shortest encoded data.
	 * @param data The data to encode.
	 * @param dataLen The length of the data. (This may be shorter than the
	 * {@code data} array.)
	 * @param codeLengths An array of code lengths for each symbol for each
	 * investigated Huffman tree.
	 * @param numberOfHuffmanSegments The number of 50-byte segments in the
	 * current block.
	 * @param numberOfDifferentSymbols The number of different symbols in the
	 * data. This is the value of the EOB symbol + 1.
	 * @param res The result of the operation is stored in this object.
	 */
	private void encodeAllSegmentsWithAllTrees(final int[] data, final int dataLen, final int[][] codeLengths, final int numberOfHuffmanSegments, final int numberOfDifferentSymbols, final EncodeAllSegmentsResult res) throws IOException
	{
		final int noTrees = codeLengths.length;
		final int[][] encodingResults = m_scratchpad.m_encodingResults;
		// The best tree for each segment
		final int[] treesUsed = new int[numberOfHuffmanSegments];
		// The shortest seen shortest length for all segments
		int shortestLength = Integer.MAX_VALUE;
		// The longest seen -shortest- length for all segments 
		int longestLength = 0;
		for (int segmentNo = 0; segmentNo < numberOfHuffmanSegments; segmentNo++)
		{
			// Encode this segment with all Huffman trees
			int shortestLengthForSegment = Integer.MAX_VALUE;
			int bestTreeIndex = 0;
			final int[] segmentEncodingResultPerTree = new int[noTrees];
			final int segmentStart = segmentNo * NO_OF_SYMBOLS_PER_SEGMENT;
			final int segmentEnd = Math.min(segmentStart + NO_OF_SYMBOLS_PER_SEGMENT, dataLen);
			for (int treeNo = 0; treeNo < noTrees; treeNo++)
			{
				final int[] curTreeCodeLengths = codeLengths[treeNo];
				int bitLen = 0;
				for (int j = segmentStart; j < segmentEnd; j++)
				{
					bitLen += curTreeCodeLengths[data[j]];
				}

				if (treeNo == 0)
				{
					shortestLengthForSegment = bitLen;
				}
				else if (bitLen < shortestLengthForSegment)
				{
					shortestLengthForSegment = bitLen;
					bestTreeIndex = treeNo;
				}
				segmentEncodingResultPerTree[treeNo] = bitLen;
			}

			if (segmentNo == 0)
			{
				shortestLength = longestLength = shortestLengthForSegment;
			}
			// Don't count the length of the last segment since that is likely
			// to contain less than 50 symbols.
			else if ((segmentNo < (numberOfHuffmanSegments - 1)) && (shortestLengthForSegment < shortestLength))
			{
				shortestLength = shortestLengthForSegment;
			}
			else if (shortestLengthForSegment > longestLength)
			{
				longestLength = shortestLengthForSegment;
			}
			encodingResults[segmentNo] = segmentEncodingResultPerTree;
			treesUsed[segmentNo] = bestTreeIndex;
		}

		res.m_encodingResults = encodingResults;
		res.m_longestLength = longestLength;
		res.m_shortestLength = shortestLength;
		res.m_treesUsed = treesUsed;
	}

	/**
	 * Divide all segments into x categories based on how well they were encoded
	 * by the globally optimal Huffman tree. An optimal Huffman tree is created
	 * for each category.
	 * @param data The data to encode.
	 * @param dataLen The length of the data.
	 * @param eobSymbol The value of the special EOB symbol. This is the highest
	 * used symbol value.
	 * @param numberOfHuffmanTrees The number of Huffman trees to create.
	 * @param numberOfSegments The number of 50-byte segments in the block.
	 * @param easr The encoding results from encoding the data with the globally
	 * optimal Huffman tree.
	 * @param globallyOptimalTree The symbol code lengths for the globally
	 * optimal Huffman tree.
	 * @return The symbols code lengths for each created tree.
	 */
	private int[][] createNewTrees(final int[] data, final int dataLen, final int eobSymbol, final int numberOfHuffmanTrees, final int numberOfSegments, final EncodeAllSegmentsResult easr, final int[] globallyOptimalTree)
	{
		// Clear the frequencies array
		final int[][] frequencies = m_scratchpad.m_frequencies2d;
		for (int i = 0; i < numberOfHuffmanTrees; i++)
		{
			Arrays.fill(frequencies[i], 0);
		}

		// How big difference in number of bits is there between the shortest
		// and the longest encoded segment?
		final int maxDistance = easr.m_longestLength - easr.m_shortestLength;
		if (maxDistance == 0)
		{
			// Nothing to do. We're as optimal as can be.
			return new int[][] { globallyOptimalTree };
		}

		final int numberOfCategories = numberOfHuffmanTrees;
		// Which category does each 50-byte segment fall into?
		final int[] categoryPerSegment = m_scratchpad.m_categoriesPerSegment;
		// How many 50-byte segments fall into each category?
		final int[] noSegmentsPerCategory = new int[numberOfCategories];

		// This array is used to determine which category a segment falls into
		// based on its encoded length.
		final int[] catArray = CATEGORY_PER_NO_OF_TREES_AND_PERCENTAGE[numberOfHuffmanTrees - 2];

		// Don't include the last segment in the statistics since that is likely
		// to be shorter
		for (int i = 0; i < numberOfSegments - 1; i++)
		{
			// The shortest length for this segment.
			final int segmentLen = easr.m_encodingResults[i][easr.m_treesUsed[i]];
			final int percentage = (100 * (segmentLen - easr.m_shortestLength)) / maxDistance;
			assert percentage >= 0;
			assert percentage <= 100;
			final int catNo = catArray[percentage];
			noSegmentsPerCategory[catNo]++;
			categoryPerSegment[i] = catNo;
		}

		for (int i = 0; i < numberOfSegments; i++)
		{
			final int segmentStart = i * NO_OF_SYMBOLS_PER_SEGMENT;
			final int segmentEnd = Math.min(segmentStart + NO_OF_SYMBOLS_PER_SEGMENT, dataLen);
			final int[] curCatFreqs = frequencies[categoryPerSegment[i]];
			for (int j = segmentStart; j < segmentEnd; j++)
			{
				curCatFreqs[data[j]]++;
			}
		}

		int noNewTrees = 0;
		for (int i = 0; i < numberOfCategories; i++)
		{
			if (noSegmentsPerCategory[i] > 0)
			{
				// Create a new Huffman tree for this category.
				noNewTrees++;
			}
		}
		assert noNewTrees > 0;

		int[][] res = new int[noNewTrees][];
		int treeNo = 0;
		for (int i = 0; i < numberOfCategories; i++)
		{
			if (noSegmentsPerCategory[i] > 0)
			{
				res[treeNo++] = HighValueBranchHuffmanTree.createCodeLengths(frequencies[i], eobSymbol + 1, MAX_HUFFMAN_BIT_LENGTH, m_scratchpad);
			}
		}
		return res;
	}

	/**
	 * Refine the Huffman trees based on the encoding results. For each tree,
	 * make it optimal based on the data in the segments that it was the best
	 * tree for.
	 * @param data The data to encode.
	 * @param dataLen The length of the data to encode.
	 * @param codeLengths The code length for each symbol for each tree.
	 * @param easr The results when encoding the data with this set of trees.
	 * @param eobSymbol The value of the EOB symbol. This is the highest symbol
	 * value.
	 * @return Symbol code lengths for the refined trees.
	 */
	private int[][] refineTreesBasedOnEncodingResults(final int[] data, final int dataLen, final int[][] codeLengths, final EncodeAllSegmentsResult easr, final int eobSymbol)
	{
		// Clear the frequencies array
		final int[][] frequencies = m_scratchpad.m_frequencies2d;
		for (int i = 0; i < codeLengths.length; i++)
		{
			Arrays.fill(frequencies[i], 0);
		}

		int segmentNo = 0;
		int noInSegment = 0;
		int curTree = easr.m_treesUsed[segmentNo];
		for (int i = 0; i < dataLen; i++)
		{
			int symbolVal = data[i];
			frequencies[curTree][symbolVal]++;
			if (++noInSegment == NO_OF_SYMBOLS_PER_SEGMENT)
			{
				segmentNo++;
				// If the data length is a multiple of 50, we do a switch after
				// encoding the last symbol which will make segmentNo greater
				// than the index of the last element in easr.m_treesUsed.
				// Thus the check below.
				if (segmentNo < easr.m_treesUsed.length)
				{
					curTree = easr.m_treesUsed[segmentNo];
				}
				noInSegment = 0;
			}
		}

		// Recreate the trees based on the gathered frequencies
		int[][] res = new int[codeLengths.length][];
		for (int i = 0; i < codeLengths.length; i++)
		{
			res[i] = HighValueBranchHuffmanTree.createCodeLengths(frequencies[i], eobSymbol + 1, MAX_HUFFMAN_BIT_LENGTH, m_scratchpad);
		}
		return res;
	}

	/**
	 * Get the number of Huffman trees to use based on the number of 50-byte
	 * segments in the data.
	 */
	private byte getNumberOfHuffmanTrees(int noSegments)
	{
		// Values from bzip2
		if (noSegments < 200)
		{
			return 2;
		}
		else if (noSegments < 600)
		{
			return 3;
		}
		else if (noSegments < 1200)
		{
			return 4;
		}
		else if (noSegments < 2400)
		{
			return 5;
		}
		else
		{
			return 6;
		}
	}

	/**
	 * Get the minimum and maximum code length from the array.
	 * @return An int array containing the minimum and the maximum code lengths,
	 * in that order.
	 */
	private int[] getMinAndMaxCodeLengths(final int[] codeLengths)
	{
		int minLength = codeLengths[0];
		int maxLength = codeLengths[0];
		for (int i = 1; i < codeLengths.length; i++)
		{
			if (codeLengths[i] < minLength)
			{
				minLength = codeLengths[i];
			}
			else if (codeLengths[i] > maxLength)
			{
				maxLength = codeLengths[i];
			}
		}
		return new int[] { minLength, maxLength };
	}

	/**
	 * Create the Huffman trees that should be used for encoding the current
	 * block. First, an globally optimal tree is created. Then new trees are
	 * created from information on how well the globally optimal tree encoded
	 * different segments. Lastly, the created trees are optimized based on the
	 * data in the segments that they are used to encode. This last step is
	 * repeated a configurable number of times ({@code
	 * m_numberOfHuffmanTreeRefinementIterations}).
	 * @param data The data that should be encoded using the created Huffman
	 * trees.
	 * @param dataLen The length of the data, excluding the trailing EOB symbol.
	 * @param noSymbolsUsed The number of different symbols used in the data.
	 */
	private HuffmanTreesAndUsage createHuffmanTrees(final int[] data, final int dataLen, final int noSymbolsUsed) throws IOException
	{
		HuffmanTreesAndUsage res = new HuffmanTreesAndUsage();

		// The maximum possible number of trees.
		// +1 == EOB symbol
		res.m_noHuffmanSegments = ((dataLen - 1 + 1) / NO_OF_SYMBOLS_PER_SEGMENT) + 1;

		// Create a Huffman tree for the entire input.
		// Count the frequencies of the different bytes in the input.
		int[] frequencies = m_scratchpad.m_frequencies;
		Arrays.fill(frequencies, 0);

		// The maximum symbol value used (before the EOB symbol) is at least 1
		// (RUNB).
		int maxSymbolValue = 1;
		for (int j = 0; j < dataLen; j++)
		{
			int symbolVal = data[j];
			frequencies[symbolVal]++;
			if (symbolVal > maxSymbolValue)
			{
				maxSymbolValue = symbolVal;
			}
		}

		// Now we can infer the value of the EOB (End Of Block) symbol. Add it
		// to the end of the data. The data array is created so there should be
		// room for it.
		res.m_eobSymbol = maxSymbolValue + 1;
		frequencies[res.m_eobSymbol] = 1;
		data[dataLen] = res.m_eobSymbol;
		final int dataLenIncEob = dataLen + 1;

		// Maybe we're already done?
		if (res.m_noHuffmanSegments < MIN_NO_OF_HUFFMAN_TREES)
		{
			// We have to encode at least two trees anyway.
			res.m_trees = new HighValueBranchHuffmanTree[MIN_NO_OF_HUFFMAN_TREES];
			int[] codeLengths = HighValueBranchHuffmanTree.createCodeLengths(frequencies, res.m_eobSymbol + 1, MAX_HUFFMAN_BIT_LENGTH, m_scratchpad);
			int[] minAndMaxLength = getMinAndMaxCodeLengths(codeLengths);
			HighValueBranchHuffmanTree tree = new HighValueBranchHuffmanTree(codeLengths, minAndMaxLength[0], minAndMaxLength[1], true);
			for (int i = 0; i < MIN_NO_OF_HUFFMAN_TREES; i++)
			{
				res.m_trees[i] = tree;
			}
			// Use tree #0 for all segments
			res.m_treeUsage = new int[res.m_noHuffmanSegments];
		}
		else
		{
			final int[][][] huffmanCodeLengths = new int[m_numberOfHuffmanTreeRefinementIterations + 1][][];
			final int[] codeLengthsForGloballyOptimalTree = HighValueBranchHuffmanTree.createCodeLengths(frequencies, res.m_eobSymbol + 1, MAX_HUFFMAN_BIT_LENGTH, m_scratchpad);
			final EncodeAllSegmentsResult easr = new EncodeAllSegmentsResult();
			encodeAllSegmentsWithAllTrees(data, dataLen, new int[][] { codeLengthsForGloballyOptimalTree }, res.m_noHuffmanSegments, res.m_eobSymbol + 1, easr);
			huffmanCodeLengths[0] = createNewTrees(data, dataLen, res.m_eobSymbol, getNumberOfHuffmanTrees(res.m_noHuffmanSegments), res.m_noHuffmanSegments, easr, codeLengthsForGloballyOptimalTree);

			// Select the set of trees that gives the shortest total data length
			int bestIndex = -1;
			int bestLength = Integer.MAX_VALUE;
			int[] bestTreeUsage = null;
			for (int i = 0; i < huffmanCodeLengths.length; i++)
			{
				if (i > 0)
				{
					// Refine the trees
					huffmanCodeLengths[i] = refineTreesBasedOnEncodingResults(data, dataLenIncEob, huffmanCodeLengths[i - 1], easr, res.m_eobSymbol);
				}
				encodeAllSegmentsWithAllTrees(data, dataLenIncEob, huffmanCodeLengths[i], res.m_noHuffmanSegments, res.m_eobSymbol + 1, easr);

				int totLen = 0;
				for (int j = 0; j < easr.m_treesUsed.length; j++)
				{
					totLen += easr.m_encodingResults[j][easr.m_treesUsed[j]];
				}

				// Previously the length of each encoded tree was added to the
				// total length. That had negligible effect on the total encoded
				// length and a small impact on the performance.
				if (totLen < bestLength)
				{
					bestIndex = i;
					bestLength = totLen;
					bestTreeUsage = easr.m_treesUsed;
				}
			}

			int noTrees = huffmanCodeLengths[bestIndex].length;
			if (noTrees < MIN_NO_OF_HUFFMAN_TREES)
			{
				res.m_trees = new HighValueBranchHuffmanTree[MIN_NO_OF_HUFFMAN_TREES];
				int[] minAndMaxLength = getMinAndMaxCodeLengths(huffmanCodeLengths[bestIndex][0]);
				for (int i = 0; i < MIN_NO_OF_HUFFMAN_TREES; i++)
				{
					res.m_trees[i] = new HighValueBranchHuffmanTree(huffmanCodeLengths[bestIndex][0], minAndMaxLength[0], minAndMaxLength[1], true);
				}
			}
			else
			{
				res.m_trees = new HighValueBranchHuffmanTree[huffmanCodeLengths[bestIndex].length];
				for (int i = 0; i < huffmanCodeLengths[bestIndex].length; i++)
				{
					int[] minAndMaxLengths = getMinAndMaxCodeLengths(huffmanCodeLengths[bestIndex][i]);
					res.m_trees[i] = new HighValueBranchHuffmanTree(huffmanCodeLengths[bestIndex][i], minAndMaxLengths[0], minAndMaxLengths[1], true);
				}
			}
			res.m_treeUsage = bestTreeUsage;
		}
		return res;
	}

	/**
	 * Encode the Huffman tree and write it to the output.
	 * @param tree The tree to encode.
	 * @param numberOfDifferentSymbols The number of different symbols in the
	 * tree.
	 * @param out The output to write the tree to.
	 */
	static void encodeHuffmanTree(final HighValueBranchHuffmanTree tree, final int numberOfDifferentSymbols, final BitOutput out) throws IOException
	{
		// Huffman bit length for the first symbol (0..17)
		int len = tree.getBitLength(0);
		out.writeBitsLittleEndian(len, 5);
		// Encode a delta length compared to the previous length for each
		// symbol.
		for (int j = 0; j < numberOfDifferentSymbols; j++)
		{
			int prevLen = len;
			len = tree.getBitLength(j);
			while (len != prevLen)
			{
				// Alter length
				out.writeBit(true);
				if (prevLen < len)
				{
					// Make longer
					out.writeBit(false);
					prevLen++;
				}
				else
				{
					// Make shorter
					out.writeBit(true);
					prevLen--;
				}
			}
			// We are at the right length
			out.writeBit(false);
		}
	}

	/**
	 * Write the block header for an encoded data block.
	 * @param blockChecksum The block checksum.
	 * @param bwFirstPointer The pointer to the first element in the Burrows
	 * Wheeler encoded data.
	 * @param seenDifferentBytes Bit flags that are switched on for all bytes
	 * that are seen in the written data.
	 * @param mtfrle Results from the MTF and RLE encodings.
	 * @param htau The different Huffman trees and information on when they are
	 * used.
	 */
	private void writeBlockHeader(final int blockChecksum, int bwFirstPointer, boolean[] seenDifferentBytes, MTFAndRLEResult mtfrle, HuffmanTreesAndUsage htau) throws IOException
	{
		// Block magic
		for (int b : BLOCK_MAGIC) {
			m_out.writeBitsLittleEndian(b & 0xFF, 8);
		}
		// Checksum
		m_out.writeBitsLittleEndian(blockChecksum, 32);
		// Randomized? (no)
		m_out.writeBit(false);
		// Starting pointer into Burrows Wheeler matrix (24 bits)
		m_out.writeBitsLittleEndian(bwFirstPointer, 24);

		boolean[] segmentsWithData = new boolean[16];
		boolean[][] seenData = new boolean[16][16];
		for (int i = 0; i < 256; i++)
		{
			if (seenDifferentBytes[i])
			{
				segmentsWithData[i / 16] = true;
				seenData[i / 16][i % 16] = true;
			}
		}

		// Write a flag for each block of 16 bytes that have at least one byte
		// occurring in the encoded data.
		for (int i = 0; i < 16; i++)
		{
			m_out.writeBit(segmentsWithData[i]);
		}
		// For each block used, write a flag for each of the used bytes in that
		// block.
		for (int i = 0; i < 16; i++)
		{
			if (segmentsWithData[i])
			{
				for (int j = 0; j < 16; j++)
				{
					m_out.writeBit(seenData[i][j]);
				}
			}
		}

		// The number of Huffman trees used (2..6)
		m_out.writeBits(htau.m_trees.length, 3);

		// The number of times the Huffman trees are switched (each 50 bytes)
		m_out.writeBitsLittleEndian(htau.m_noHuffmanSegments, 15);

		// Which Huffman tree is selected at each switch? Use a zero-terminated
		// bit run of MTF:ed index values

		// Init the MTF alphabet
		int[] mtfAlpha = new int[htau.m_trees.length];
		for (int i = 0; i < htau.m_trees.length; i++)
		{
			mtfAlpha[i] = i;
		}
		int[] treeUsageMtf = new int[htau.m_noHuffmanSegments];
		new IntMoveToFront(mtfAlpha).encode(htau.m_treeUsage, treeUsageMtf);

		for (int i = 0; i < htau.m_noHuffmanSegments; i++)
		{
			// A zero-terminated bit run for the values 0..5
			int val = 0;
			while (val < treeUsageMtf[i])
			{
				m_out.writeBit(true);
				val++;
			}
			m_out.writeBit(false);
		}

		// Encode each Huffman tree
		for (int i = 0; i < htau.m_trees.length; i++)
		{
			encodeHuffmanTree(htau.m_trees[i], htau.m_eobSymbol + 1, m_out);
		}
	}

	private static class HuffmanTreesAndUsage
	{
		private HighValueBranchHuffmanTree[] m_trees;
		private int m_noHuffmanSegments;
		private int[] m_treeUsage;
		private int m_eobSymbol;
	}

	void encode() throws IOException
	{
		// Fix the block overshoot. Copy DATA_OVERSHOOT bytes to the end of the
		// array. Repeat the data if the block is shorter than DATA_OVERSHOOT
		// bytes.
		int noCopied = 0;
		while (noCopied < ThreeWayRadixQuicksort.DATA_OVERSHOOT)
		{
			int noToCopy = Math.min(ThreeWayRadixQuicksort.DATA_OVERSHOOT - noCopied, m_blockSize);
			System.arraycopy(m_block, 0, m_block, m_blockSize + noCopied, noToCopy);
			noCopied += noToCopy;
		}

		// Sort the data in the block.
		// data contains the written data after the initial move to front
		// transformation
		BurrowsWheelerEncodingResult burrWhee = new BurrowsWheelerEncoder(m_block, m_blockSize, m_scratchpad).encode();

		// Run Move to front and run length encoding transformations on the
		// Burrows Wheeler encoded data
		MTFAndRLEResult rleMtfSymbols = moveToFrontAndRunLengthEncode(burrWhee.m_lastColumn, m_blockSize, getSeenByteValues());
		int[] encodedData = rleMtfSymbols.m_encodedData;

		// Create the Huffman trees. This method also infers the value of the
		// EOB symbol and adds it to the end of the encodedData array.
		HuffmanTreesAndUsage htau = createHuffmanTrees(rleMtfSymbols.m_encodedData, rleMtfSymbols.m_dataLen, rleMtfSymbols.m_noSeenDifferentSymbols);

		writeBlockHeader(m_blockChecksum, burrWhee.m_firstPointer, m_seenDifferentBytes, rleMtfSymbols, htau);

		// Write the Huffman encoded data. The EOB symbol is last in the data.
		int swapNo = 0;
		int noLeftUntilSwap = 1;
		HighValueBranchHuffmanTree curTree = null;
		// +1 == EOB symbol
		for (int i = 0; i < rleMtfSymbols.m_dataLen + 1; i++)
		{
			if (--noLeftUntilSwap == 0)
			{
				curTree = htau.m_trees[htau.m_treeUsage[swapNo++]];
				noLeftUntilSwap = NO_OF_SYMBOLS_PER_SEGMENT;
			}
			curTree.write(m_out, encodedData[i]);
		}
		assert swapNo == htau.m_noHuffmanSegments;

		if (m_blockEncoderCallback != null)
		{
			m_blockEncoderCallback.reportBlockDone();
		}
	}
}
