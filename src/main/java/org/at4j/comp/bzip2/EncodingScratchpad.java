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
 * This object contains different objects used by a bzip2 encoder thread. It is
 * used to reduce the number of object and array allocations.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class EncodingScratchpad
{
	private static final int MAX_BLOCK_LENGTH = BZip2OutputStreamSettings.MAX_BLOCK_SIZE * 100 * 1000;
	private static final int MAX_NO_OF_SEGMENTS = MAX_BLOCK_LENGTH / BlockEncoder.NO_OF_SYMBOLS_PER_SEGMENT;

	// An array that may contain the frequencies of each symbol in the data.
	final int[] m_frequencies = new int[BlockEncoder.MAX_NO_OF_MTF_SYMBOLS];

	// A move to front alphabet.
	final byte[] m_mtfAlphabet = new byte[BlockEncoder.MAX_NO_OF_MTF_SYMBOLS];

	// This two dimensional array can contain the frequencies for the different
	// symbols encoded by the different trees (up to six trees)
	final int[][] m_frequencies2d = new int[BlockEncoder.MAX_NO_OF_HUFFMAN_TREES][BlockEncoder.MAX_NO_OF_MTF_SYMBOLS];

	// Contains MTF and RL encoded data before the Huffman encoding. The maximum
	// size is the maximum size of a block + the EOB symbol. The actual size
	// will probably be significantly shorter than this
	final int[] m_encodedData = new int[MAX_BLOCK_LENGTH + 1];

	// Frequencies of each two-byte combination used for the radix sort.
	// Use an overshoot of one position.
	final int[] m_twoByteFrequencies = new int[65536 + 1];

	// Pointers created by the 3-way radix quicksort
	final int[] m_ptrs = new int[MAX_BLOCK_LENGTH];

	// A cache for sort results
	final int[] m_sortCache = new int[MAX_BLOCK_LENGTH + ThreeWayRadixQuicksort.DATA_OVERSHOOT];

	// Array for temporary data. This will be grown incrementally as the need
	// arises.
	int[] m_tempArea = new int[1024];

	// Stack for block sorting
	final ThreeWayRadixQuicksort.QuickSortRangeInfo[] m_sortStack = new ThreeWayRadixQuicksort.QuickSortRangeInfo[ThreeWayRadixQuicksort.SORT_STACK_SIZE];

	// The results when all segments of a block is encoded with all available
	// Huffman trees
	final int[][] m_encodingResults = new int[MAX_NO_OF_SEGMENTS][BlockEncoder.MAX_NO_OF_HUFFMAN_TREES];

	final int[] m_categoriesPerSegment = new int[MAX_NO_OF_SEGMENTS];

	// The last column after Burrows Wheeler encoding
	final byte[] m_lastColumn = new byte[MAX_BLOCK_LENGTH];

	// The bucket sorting order
	final int[] m_sortOrder = new int[256];
	// Used when scanning pointers
	final int[] m_copyStart = new int[256];
	final int[] m_copyEnd = new int[256];

	// Mapping between a symbol and its index number in the array of symbols
	// used by the run length encoder.
	final byte[] m_sequenceMap = new byte[256];

	// Heap used when calculating Huffman tree code lengths
	final int[] m_htHeap = new int[BlockEncoder.MAX_NO_OF_MTF_SYMBOLS + 2];
	final int[] m_htWeight = new int[BlockEncoder.MAX_NO_OF_MTF_SYMBOLS * 2];
	final int[] m_htParent = new int[BlockEncoder.MAX_NO_OF_MTF_SYMBOLS * 2];

	// Flags for all sorted large buckets
	final boolean[] m_sortedLargeBuckets = new boolean[256];
	// Flags for all sorted small buckets
	final boolean[] m_sortedSmallBuckets = new boolean[256 * 256];

	/**
	 * Get a temporary integer array of with a length of at least {@code len}
	 * integers.
	 */
	int[] getTemp(final int len)
	{
		// Is the current temp area large enough?
		if (m_tempArea.length < len)
		{
			// No. Reallocate it
			m_tempArea = new int[len + 100];
		}
		return m_tempArea;
	}
}
