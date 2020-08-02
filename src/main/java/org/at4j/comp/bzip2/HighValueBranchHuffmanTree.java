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

import org.at4j.support.io.BitInput;
import org.at4j.support.io.BitOutput;

/**
 * This object represents the type of Huffman tree that is used by bzip2. The
 * "high value branch" means that leaf nodes have the smallest possible values
 * and non-leaf nodes have the highest possible values at each tree depth.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class HighValueBranchHuffmanTree
{
	private static final int MAX_NO_OF_SYMBOLS = 258;

	// The shortest code length for symbols in this tree.
	private final int m_minLength;
	// The longest code length for symbols in this tree.
	private final int m_maxLength;
	// m_maxLength - m_minLength + 1;
	// Declared package private for the unit tests.
	final int m_numberOfLengths;

	// The value limit at each data length, i.e. the maximum value for leaf
	// nodes at that data length.
	// Declared package private for the unit tests.
	final int[] m_limitsPerLength;
	// The lowest value for a symbol at each length. The value for length
	// m_minLength is at index 0 in the array.
	// Declared package private for the unit tests.
	final int[] m_baseValuesPerLength;
	// The offset in the m_symbolSequenceNos array for the first symbol for each
	// Huffman code length. The array has the length m_maxLength - m_minLength +
	// 1. The value for m_minLength is at index 0 (and is 0).
	// Declared package private for the unit tests.
	final int[] m_symbolOffsetPerLength;
	// The index of the symbol table for Huffman code no n.
	// Declared package private for the unit tests.
	final int[] m_symbolSequenceNos;
	// This table contains the Huffman codes and the code bit lengths for each
	// symbol. It is created when using the constructor that calculates the
	// Huffman trees to speed up encoding.
	final int[][] m_huffmanCodesAndLengthsPerSymbol;

	/**
	 * Get the Huffman code and its bit length for a symbol.
	 * @param symbol The symbol.
	 * @param huffmanIndex The symbol's index in the list of sorted symbols.
	 * @param codeAndLength An int array of length 2 used to store the result
	 * in.
	 */
	private int[] getCodeAndLengthForSymbol(final int symbol, final int huffmanIndex, final int[] codeAndLength)
	{
		// Calculate the length of the synbol's Huffman code
		int deltaLen;
		for (deltaLen = 0; deltaLen < m_numberOfLengths - 1; deltaLen++)
		{
			if (huffmanIndex < m_symbolOffsetPerLength[deltaLen + 1])
			{
				break;
			}
		}

		codeAndLength[0] = m_baseValuesPerLength[deltaLen] + (huffmanIndex - m_symbolOffsetPerLength[deltaLen]);
		codeAndLength[1] = m_minLength + deltaLen;
		return codeAndLength;
	}

	/**
	 * Create a canonical Huffman tree for the supplied symbols.
	 * <p>
	 * Symbol lengths for a canonical Huffman tree can be created by the
	 * {@link #createCodeLengths(int[], int, int)} method.
	 * @param symbolLengths The length of the Huffman code for each symbol.
	 * @param minLength The shortest Huffman code length in the tree.
	 * @param maxLength The longest Huffman code length in the tree.
	 * @param forEncoding Should the tree be used for encoding? If so, a loookup
	 * table that contains the Huffman code for each symbol is created to speed
	 * up the encoding.
	 * @throws IllegalArgumentException If the lengths are invalid.
	 */
	HighValueBranchHuffmanTree(final int[] symbolLengths, final int minLength, final int maxLength, final boolean forEncoding) throws IllegalArgumentException
	{
		if ((minLength < 0) || (maxLength < minLength))
		{
			throw new IllegalArgumentException("Illegal min or max length, min: " + minLength + ", max: " + maxLength);
		}

		final int numberOfSymbols = symbolLengths.length;
		final int numberOfLengths = maxLength - minLength + 1;
		// Create a array of symbol sequence numbers sorted on their symbol
		// lengths
		m_symbolSequenceNos = new int[numberOfSymbols];
		// The number of symbols having each code length
		final int[] numl = new int[numberOfLengths];
		int index = 0;
		for (int i = minLength; i <= maxLength; i++)
		{
			numl[i - minLength] = 0;
			for (int j = 0; j < numberOfSymbols; j++)
			{
				if (symbolLengths[j] == i)
				{
					m_symbolSequenceNos[index++] = j;
					numl[i - minLength]++;
				}
			}
		}

		m_symbolOffsetPerLength = new int[numberOfLengths];
		m_symbolOffsetPerLength[0] = 0;
		for (int i = 0; i < numberOfLengths - 1; i++)
		{
			m_symbolOffsetPerLength[i + 1] = m_symbolOffsetPerLength[i] + numl[i];
		}

		// The value limit at each length
		m_limitsPerLength = new int[numberOfLengths - 1];
		m_baseValuesPerLength = new int[numberOfLengths];
		int prevLimit = 0;
		for (int i = minLength; i <= maxLength; i++)
		{
			index = i - minLength;
			// The base value for this length is the value of the smallest
			// allowed symbol for this length. The smallest allowed symbol is
			// the limit for the previous length with a zero at the end.
			m_baseValuesPerLength[index] = prevLimit << 1;

			if (i < maxLength)
			{
				// The limit for this length is the base value for this length
				// plus the number of symbols for this length.
				prevLimit = m_baseValuesPerLength[index] + numl[index];
				m_limitsPerLength[index] = prevLimit - 1;
			}
		}

		m_minLength = minLength;
		m_maxLength = maxLength;
		m_numberOfLengths = (byte) (maxLength - minLength + 1);
		if (forEncoding)
		{
			// Create an inverse mapping into the list of sorted symbols
			final int[] huffmanIndexPerSymbol = new int[symbolLengths.length];
			Arrays.fill(huffmanIndexPerSymbol, -1);
			for (int i = 0; i < m_symbolSequenceNos.length; i++)
			{
				huffmanIndexPerSymbol[m_symbolSequenceNos[i]] = i;
			}

			// Create a table containing the Huffman code and its bit length for
			// each symbol. This is used to speed up writes.
			m_huffmanCodesAndLengthsPerSymbol = new int[symbolLengths.length][2];
			int[] codeAndLength = new int[2];
			for (int i = 0; i < symbolLengths.length; i++)
			{
				codeAndLength = getCodeAndLengthForSymbol(i, huffmanIndexPerSymbol[i], codeAndLength);
				m_huffmanCodesAndLengthsPerSymbol[i][0] = codeAndLength[0];
				m_huffmanCodesAndLengthsPerSymbol[i][1] = codeAndLength[1];
			}
		}
		else
		{
			// Don't create these variables. They are only used when writing data
			// and it is assumed that this constructor will only be used to create
			// trees for reading data.
			m_huffmanCodesAndLengthsPerSymbol = null;
		}
	}

	private static void upHeap(final int[] heap, final int[] weight, int nHeap)
	{
		int tmp = heap[nHeap];
		while (weight[tmp] < weight[heap[nHeap >> 1]])
		{
			heap[nHeap] = heap[nHeap >>> 1];
			nHeap >>>= 1;
		}
		heap[nHeap] = tmp;
	}

	private static void downHeap(final int[] heap, final int[] weight, final int nHeap, int n)
	{
		int tmp = heap[n];
		while (true)
		{
			int yy = n << 1;
			if (yy > nHeap)
			{
				break;
			}
			if (yy < nHeap && weight[heap[yy + 1]] < weight[heap[yy]])
			{
				yy++;
			}
			if (weight[tmp] < weight[heap[yy]])
			{
				break;
			}
			heap[n] = heap[yy];
			n = yy;
		}
		heap[n] = tmp;
	}

	private static int addWeights(final int w1, final int w2)
	{
		final int d1 = w1 & 0xFF;
		final int d2 = w2 & 0xFF;
		final int ww1 = w1 & 0xFFFFFF00;
		final int ww2 = w2 & 0xFFFFFF00;
		return (ww1 + ww2) | (1 + (Math.max(d1, d2)));
	}

	int getMinLength()
	{
		return m_minLength;
	}

	int getMaxLength()
	{
		return m_maxLength;
	}

	/**
	 * Get a sorted array with symbol sequence numbers and their Huffman code
	 * lengths. The returned array is sorted with the most frequent occurring
	 * symbol first (i.e. the symbol with the shortest Huffman code).
	 * <p>
	 * This method is used for testing.
	 * @return Array a[n][0] = symbol, a[n][1] = Huffman code length
	 */
	int[][] getSortedSymbolSequenceNosAndCodeLengths()
	{
		int[][] res = new int[m_symbolSequenceNos.length][2];
		int length = m_minLength;
		for (int i = 0; i < m_symbolSequenceNos.length; i++)
		{
			while ((length < m_maxLength) && (i >= m_symbolOffsetPerLength[length - m_minLength + 1]))
			{
				length++;
			}
			res[i][0] = m_symbolSequenceNos[i];
			res[i][1] = length;
		}
		return res;
	}

	/**
	 * Read the next symbol.
	 * @param in The input to read the symbol from.
	 * @return The next symbol.
	 * @throws IOException On I/O errors.
	 */
	int readNext(final BitInput in) throws IOException
	{
		int code = in.readBits(m_minLength);
		// m_limitsPerLength.length == 0 means that all Huffman codes have the
		// same length.
		if (m_limitsPerLength.length == 0 || code <= m_limitsPerLength[0])
		{
			return m_symbolSequenceNos[code];
		}
		else
		{
			int codeLength = m_minLength;
			int index = 1;
			while (true)
			{
				code = (code << 1) | (in.readBit() ? 1 : 0);
				codeLength++;
				if ((codeLength == m_maxLength) || (code <= m_limitsPerLength[index]))
				{
					return m_symbolSequenceNos[m_symbolOffsetPerLength[index] + (code - m_baseValuesPerLength[index])];
				}
				index++;
			}
		}
	}

	/**
	 * Write a symbol.
	 * @param out The output to write to.
	 * @param symbol The symbol to write.
	 * @throws IOException On I/O errors.
	 */
	void write(final BitOutput out, final int symbol) throws IOException
	{
		out.writeBitsLittleEndian(m_huffmanCodesAndLengthsPerSymbol[symbol][0], m_huffmanCodesAndLengthsPerSymbol[symbol][1]);
	}

	/**
	 * Get the number of bits used for encoding the symbol.
	 */
	int getBitLength(int symbol)
	{
		return m_huffmanCodesAndLengthsPerSymbol[symbol][1];
	}

	/**
	 * Calculate the Huffman code lengths for the optimal, depth-limited Huffman
	 * tree for the supplied symbol frequencies.
	 * <p>
	 * This method uses the (slightly magic) algorithm from bzip2 1.0.5.
	 * @param frequencies The frequencies for each symbol in the data to be
	 * encoded.
	 * @param noSymbols The number of different symbols in the data to encode.
	 * This should be the maximum symbol value (the EOB symbol's value) + 1.
	 * @param maxLength The maximum code length which also will be the depth of
	 * the Huffman tree. If this is too small, this method will get stuck in an
	 * infinite loop.
	 * @return The Huffman code lengths for each symbol.
	 */
	static int[] createCodeLengths(final int[] frequencies, final int noSymbols, final int maxLength, final EncodingScratchpad scratchpad)
	{
		/*
		 * Nodes and heap entries run from 1. Entry 0 for both the heap and
		 * nodes is a sentinel.
		 */

		final int[] heap = scratchpad.m_htHeap;
		final int[] weight = scratchpad.m_htWeight;
		final int[] parent = scratchpad.m_htParent;

		final int[] res = new int[noSymbols];

		int actualMaxLength = -1;
		int actualMinLength = Integer.MAX_VALUE;

		for (int i = 0; i < noSymbols; i++)
		{
			weight[i + 1] = (frequencies[i] == 0 ? 1 : frequencies[i]) << 8;
		}

		while (true)
		{
			int noNodes = noSymbols;
			int nHeap = 0;

			heap[0] = 0;
			weight[0] = 0;
			parent[0] = -2;

			for (int i = 1; i <= noSymbols; i++)
			{
				parent[i] = -1;
				nHeap++;
				heap[nHeap] = i;
				upHeap(heap, weight, nHeap);
			}

			assert nHeap < MAX_NO_OF_SYMBOLS + 2;

			while (nHeap > 1)
			{
				int n1 = heap[1];
				heap[1] = heap[nHeap];
				nHeap--;
				downHeap(heap, weight, nHeap, 1);
				int n2 = heap[1];
				heap[1] = heap[nHeap];
				nHeap--;
				downHeap(heap, weight, nHeap, 1);
				noNodes++;
				parent[n1] = parent[n2] = noNodes;
				weight[noNodes] = addWeights(weight[n1], weight[n2]);
				parent[noNodes] = -1;
				nHeap++;
				heap[nHeap] = noNodes;
				upHeap(heap, weight, nHeap);
			}

			assert noNodes < MAX_NO_OF_SYMBOLS * 2;

			boolean tooLong = false;
			INNER: for (int i = 1; i <= noSymbols; i++)
			{
				int j = 0;
				int k = i;
				while (parent[k] >= 0)
				{
					k = parent[k];
					j++;
				}
				res[i - 1] = j;
				if (j > maxLength)
				{
					tooLong = true;
					break INNER;
				}

				if (j > actualMaxLength)
				{
					actualMaxLength = j;
				}
				if (j < actualMinLength)
				{
					actualMinLength = j;
				}
			}

			if (!tooLong)
			{
				break;
			}

			for (int i = 1; i <= noSymbols; i++)
			{
				int j = weight[i] >> 8;
				j = 1 + (j / 2);
				weight[i] = j << 8;
			}
		}
		return res;
	}
}
