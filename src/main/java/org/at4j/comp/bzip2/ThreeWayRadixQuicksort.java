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

import java.util.Arrays;

/**
 * This sort algorithm is used by the Burrows Wheeler encoder to sort the data
 * to encode. It is an amalgation of three different sort algorithms. Radix sort
 * is used to divide the input into 65536 different buckets. The quicksort is
 * used to sort each bucket. When the quicksort iterations produce short enough
 * blocks, shell sort is used.
 * <p>
 * See <a href="http://www.ddj.com/architect/184410724">Dr. Dobb's Journal from
 * November 01 1998</a>.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class ThreeWayRadixQuicksort
{
	// The amount of overshoot in the data. See below.
	static final int DATA_OVERSHOOT = 20;

	// The deepest sort that we do with quicksort. Deeper sorts use shell sort.
	// This value should be less than the DATA_OVERSHOOT.
	private static final int QUICKSORT_DEPTH_THRESHOLD = 18;

	// The size of the sorting stack. This size is the same as for bzip2 1.0.5.
	static final int SORT_STACK_SIZE = 100;

	/**
	 * The increments for shell sort. Borrowed from bzip2.
	 * <p>
	 * Knuth's increments seem to work better than Incerpi-Sedgewick here.
	 * Possibly because the number of elems to sort is usually small, typically
	 * &lt;= 20.
	 */
	private static final int[] SHELL_SORT_INCREMENTS = { 1, 4, 13, 40, 121, 364, 1093, 3280, 9841, 29524, 88573, 265720, 797161, 2391484 };

	// Declared package private for the unit tests
	static class QuickSortRangeInfo
	{
		private final int m_bucketStartPos;
		// The length of the bucket measured in number of symbols.
		private final int m_bucketLen;
		private final int m_depth;

		QuickSortRangeInfo(int bucketStartPos, int bucketLen, int depth)
		{
			m_bucketStartPos = bucketStartPos;
			m_bucketLen = bucketLen;
			m_depth = depth;
		}
	}

	// The data array.
	private final byte[] m_data;
	// The length of the data in the array. Data occupies the positions 0 to
	// m_length - 1 in the array.
	private final int m_length;
	// The shortest data block length that quicksort will be used for. For
	// shorter blocks, shell sort is used.
	private final int m_minLengthForQuicksort;
	// Contains preallocated data structures. Used to reduce the number of
	// temporary objects that are created and thus avoid time spent gc:ing.
	private final EncodingScratchpad m_scratchpad;
	// Cache with sort results that are used to speed up the sorting. This works
	// because all strings to sort are rotations of a single string.
	private final int[] m_sortCache;
	// Use a stack of sort range information instead of calling the quicksort
	// methods recursively.
	private final QuickSortRangeInfo[] m_sortStack;
	// A pointer to the current position in the sort stack.
	private int m_sortStackPointer = -1;
	// Array containing a pointer for each element in m_data to its location in
	// the sorted data.
	// This is declared package private for the unit tests.
	final int[] m_ptr;

	/**
	 * Create a new sorting object.
	 * @param data The data to sort. This array should contain an overshoot of
	 * {@code DATA_OVERSHOOT} bytes. I.e: the data array should have a length of
	 * at least {@code length + DATA_OVERSHOOT} bytes, and the last {@code
	 * DATA_OVERSHOOT} bytes should be equal to the first {@code DATA_OVERSHOOT}
	 * bytes. This makes a few sorting optimizations possible.
	 * <p>
	 * If the length of the data is less than {@code DATA_OVERSHOOT} bytes, the
	 * overshoot should contain the data repeated.
	 * @param minLengthForQuicksort Segments that are shorter than this length
	 * are sorted with shell sort instead of quicksort.
	 */
	ThreeWayRadixQuicksort(final byte[] data, final int length, final int minLengthForQuicksort, final EncodingScratchpad sp) throws IllegalArgumentException
	{
		assert data.length >= length + DATA_OVERSHOOT;

		if (length > data.length)
		{
			throw new IllegalArgumentException("Invalid data length " + length + ". It must be <= the length of the data array (" + data.length + ")");
		}
		if (minLengthForQuicksort < 3)
		{
			throw new IllegalArgumentException("Invalid minimum length for Quicksort " + minLengthForQuicksort + ". It must be >= 3");
		}
		m_data = data;
		m_length = length;
		m_minLengthForQuicksort = minLengthForQuicksort;
		m_scratchpad = sp;
		m_sortStack = m_scratchpad.m_sortStack;
		// Clear the sortCache array
		m_sortCache = m_scratchpad.m_sortCache;
		Arrays.fill(m_sortCache, 0);
		m_ptr = m_scratchpad.m_ptrs;
	}

	/**
	 * Get the data at the specified position. It is assumed that the position
	 * is within the range of the data.
	 * <p>
	 * This method is so small so that it will likely be inlined by the Java
	 * compiler.
	 */
	private int getDataAt(final int pos)
	{
		return m_data[pos] & 0xFF;
	}

	/**
	 * Make the initial radix sort of the data into 65536 buckets. As a side
	 * effect, this method populates the {@code m_ptr} array with the results of
	 * the sort.
	 * <p>
	 * This method is declared package-private for the unit tests.
	 * @return The start positions for each bucket (in the {@code m_ptr} array).
	 */
	int[] radixSort()
	{
		// This array will contain the frequencies of each two byte combination
		// in the data.
		final int[] frequencies = m_scratchpad.m_twoByteFrequencies;
		Arrays.fill(frequencies, 0);

		// Iterate over the data and collect the frequencies of each occurring
		// two byte combination.
		int val = getDataAt(0) << 8;
		for (int i = m_length - 1; i >= 0; i--)
		{
			val = val >>> 8 | (getDataAt(i) << 8);
			frequencies[val]++;
		}

		// Convert the frequencies array to contain the last data element
		// position + 1 for each two byte bucket.
		for (int i = 1; i < 65536; i++)
		{
			frequencies[i] += frequencies[i - 1];
		}

		// The m_ptr array will contain the pointers between each two byte
		// combination's bucket location and its location in the data array.
		// This loop will also modify the frequencies array to contain the
		// starting position of each data bucket.
		val = getDataAt(0) << 8;
		for (int i = m_length - 1; i >= 0; i--)
		{
			val = val >>> 8 | (getDataAt(i) << 8);
			int pos = --frequencies[val];
			m_ptr[pos] = i;
		}

		// Now frequencies contain the first location of each bucket and m_ptr
		// contains pointers between the data locations in the buckets and the
		// data in the data array.

		return frequencies;
	}

	/**
	 * Get the position that contains the median of the values at the three
	 * positions.
	 */
	private int med3(final int pos1, final int pos2, final int pos3, final int depth)
	{
		int v1, v2, v3;
		if ((v1 = getDataAt(m_ptr[pos1] + depth)) == (v2 = getDataAt(m_ptr[pos2] + depth)))
		{
			return pos1;
		}
		if (((v3 = getDataAt(m_ptr[pos3] + depth)) == v1) || (v3 == v2))
		{
			return pos3;
		}
		return v1 < v2 ? (v2 < v3 ? pos2 : (v1 < v3 ? pos3 : pos1)) : (v2 > v3 ? pos2 : (v1 < v3 ? pos1 : pos3));
	}

	/**
	 * Select the pivot value for the quicksort.
	 * @return The position of the pivot value.
	 */
	private int selectPivot(final QuickSortRangeInfo qsri)
	{
		int pos1 = qsri.m_bucketStartPos;
		int pos3 = pos1 + qsri.m_bucketLen - 1;
		int pos2 = (pos1 + pos3) / 2;

		// For a large bucket, use a median of three median values
		if (qsri.m_bucketLen > 500)
		{
			int d = qsri.m_bucketLen / 8;
			pos1 = med3(pos1, pos1 + d, pos1 + 2 * d, qsri.m_depth);
			pos2 = med3(pos2 - d, pos2, pos2 + d, qsri.m_depth);
			pos3 = med3(pos3 - 2 * d, pos3 - d, pos3, qsri.m_depth);
		}
		return med3(pos1, pos2, pos3, qsri.m_depth);
	}

	/**
	 * Swap the elements in the two positions in the array.
	 */
	private void swap(final int pos1, final int pos2)
	{
		int v1 = m_ptr[pos1];
		m_ptr[pos1] = m_ptr[pos2];
		m_ptr[pos2] = v1;
	}

	/**
	 * Shell sort the data in the range. This is used for data ranges that are
	 * too short to be quicksorted.
	 * <p>
	 * This method is declared package private for the unit tests.
	 */
	void shellSortRange(final QuickSortRangeInfo qsri)
	{
		// If the implementation of this method looks strange it is because it
		// is heavily optimized.

		final int len = qsri.m_bucketLen;
		final int depth = qsri.m_depth;
		final int startPos = qsri.m_bucketStartPos;
		final int endPos = startPos + len;
		int incMax = 1;
		while (SHELL_SORT_INCREMENTS[incMax] < len)
		{
			incMax++;
		}

		for (int incrementPtr = incMax - 1; incrementPtr >= 0; incrementPtr--)
		{
			final int increment = SHELL_SORT_INCREMENTS[incrementPtr];
			final int startIter = startPos + increment;
			for (int i = startIter; i < endPos; i++)
			{
				INCLOOP: for (int j = i; j >= startIter; j -= increment)
				{
					int curDepth = depth;
					int curPos1 = m_ptr[j - increment] + depth - 1;
					int curPos2 = m_ptr[j] + depth - 1;

					// Tests with sort cache lookups.
					// Inner loop.
					while (true)
					{
						while (curPos1 >= m_length)
						{
							curPos1 -= m_length;
						}
						while (curPos2 >= m_length)
						{
							curPos2 -= m_length;
						}

						// Eight tests with sort cache lookups. The data
						// overshoot helps us to avoid range checks when
						// the pointers are incremented.
						if (getDataAt(++curPos1) == getDataAt(++curPos2))
						{
							if (m_sortCache[curPos1] == m_sortCache[curPos2])
							{
								// 2
								if (getDataAt(++curPos1) == getDataAt(++curPos2))
								{
									if (m_sortCache[curPos1] == m_sortCache[curPos2])
									{
										// 3
										if (getDataAt(++curPos1) == getDataAt(++curPos2))
										{
											if (m_sortCache[curPos1] == m_sortCache[curPos2])
											{
												// 4
												if (getDataAt(++curPos1) == getDataAt(++curPos2))
												{
													if (m_sortCache[curPos1] == m_sortCache[curPos2])
													{
														// 5
														if (getDataAt(++curPos1) == getDataAt(++curPos2))
														{
															if (m_sortCache[curPos1] == m_sortCache[curPos2])
															{
																// 6
																if (getDataAt(++curPos1) == getDataAt(++curPos2))
																{
																	if (m_sortCache[curPos1] == m_sortCache[curPos2])
																	{
																		// 7
																		if (getDataAt(++curPos1) == getDataAt(++curPos2))
																		{
																			if (m_sortCache[curPos1] == m_sortCache[curPos2])
																			{
																				// 8
																				if (getDataAt(++curPos1) == getDataAt(++curPos2))
																				{
																					if (m_sortCache[curPos1] == m_sortCache[curPos2])
																					{
																						curDepth += 8;
																						if (curDepth >= m_length)
																						{
																							// The strings are exactly equal. This can happen for bzip2 when
																							// we have input such as AAA (only) that does not get run length
																							// encoded.
																							break INCLOOP;
																						}

																						// The eight symbols were equals and no cache hits. Continue the inner loop
																					}
																					else
																					{
																						if (m_sortCache[curPos1] < m_sortCache[curPos2])
																						{
																							break INCLOOP;
																						}
																						else
																						{
																							swap(j - increment, j);
																							continue INCLOOP;
																						}
																					}
																				}
																				else
																				{
																					if (getDataAt(curPos1) < getDataAt(curPos2))
																					{
																						break INCLOOP;
																					}
																					else
																					{
																						swap(j - increment, j);
																						continue INCLOOP;
																					}
																				}
																			}
																			else
																			{
																				if (m_sortCache[curPos1] < m_sortCache[curPos2])
																				{
																					break INCLOOP;
																				}
																				else
																				{
																					swap(j - increment, j);
																					continue INCLOOP;
																				}
																			}
																		}
																		else
																		{
																			if (getDataAt(curPos1) < getDataAt(curPos2))
																			{
																				break INCLOOP;
																			}
																			else
																			{
																				swap(j - increment, j);
																				continue INCLOOP;
																			}
																		}
																	}
																	else
																	{
																		if (m_sortCache[curPos1] < m_sortCache[curPos2])
																		{
																			break INCLOOP;
																		}
																		else
																		{
																			swap(j - increment, j);
																			continue INCLOOP;
																		}
																	}
																}
																else
																{
																	if (getDataAt(curPos1) < getDataAt(curPos2))
																	{
																		break INCLOOP;
																	}
																	else
																	{
																		swap(j - increment, j);
																		continue INCLOOP;
																	}
																}
															}
															else
															{
																if (m_sortCache[curPos1] < m_sortCache[curPos2])
																{
																	break INCLOOP;
																}
																else
																{
																	swap(j - increment, j);
																	continue INCLOOP;
																}
															}
														}
														else
														{
															if (getDataAt(curPos1) < getDataAt(curPos2))
															{
																break INCLOOP;
															}
															else
															{
																swap(j - increment, j);
																continue INCLOOP;
															}
														}
													}
													else
													{
														if (m_sortCache[curPos1] < m_sortCache[curPos2])
														{
															break INCLOOP;
														}
														else
														{
															swap(j - increment, j);
															continue INCLOOP;
														}
													}
												}
												else
												{
													if (getDataAt(curPos1) < getDataAt(curPos2))
													{
														break INCLOOP;
													}
													else
													{
														swap(j - increment, j);
														continue INCLOOP;
													}
												}
											}
											else
											{
												if (m_sortCache[curPos1] < m_sortCache[curPos2])
												{
													break INCLOOP;
												}
												else
												{
													swap(j - increment, j);
													continue INCLOOP;
												}
											}
										}
										else
										{
											if (getDataAt(curPos1) < getDataAt(curPos2))
											{
												break INCLOOP;
											}
											else
											{
												swap(j - increment, j);
												continue INCLOOP;
											}
										}
									}
									else
									{
										if (m_sortCache[curPos1] < m_sortCache[curPos2])
										{
											break INCLOOP;
										}
										else
										{
											swap(j - increment, j);
											continue INCLOOP;
										}
									}
								}
								else
								{
									if (getDataAt(curPos1) < getDataAt(curPos2))
									{
										break INCLOOP;
									}
									else
									{
										swap(j - increment, j);
										continue INCLOOP;
									}
								}
							}
							else
							{
								if (m_sortCache[curPos1] < m_sortCache[curPos2])
								{
									break INCLOOP;
								}
								else
								{
									swap(j - increment, j);
									continue INCLOOP;
								}
							}
						}
						else
						{
							if (getDataAt(curPos1) < getDataAt(curPos2))
							{
								break INCLOOP;
							}
							else
							{
								swap(j - increment, j);
								continue INCLOOP;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Get the index of the string that has the first differing value at the
	 * given depth compared to the first string in the range.
	 * @param bucketStartPos The start of the range.
	 * @param bucketLen The length of the range.
	 * @param depth The depth to investigate.
	 * @return The index of the first differing value, or {@code -1} if all
	 * values are equal at the given depth.
	 */
	private int getPositionOfFirstDifferingValue(final int bucketStartPos, final int bucketLen, final int depth)
	{
		assert depth <= DATA_OVERSHOOT;

		final int c0 = getDataAt(m_ptr[bucketStartPos] + depth);
		final int upperBound = bucketStartPos + bucketLen;
		for (int i = bucketStartPos + 1; i < upperBound; i++)
		{
			if (getDataAt(m_ptr[i] + depth) != c0)
			{
				return i;
			}
		}
		// All values at this depth are equal
		return -1;
	}

	/**
	 * Swap the {@code len} values after {@code r1Start} with the {@code len}
	 * values after {@code r2start}.
	 * @param r1Start The start of the first range.
	 * @param r2Start The start of the second range.
	 * @param len The number of bytes to swap.
	 */
	private void swapRanges(final int r1Start, final int r2Start, final int len)
	{
		assert r1Start + len <= r2Start;

		// Is the scratchpad's temp area large enough?
		if (m_scratchpad.m_tempArea.length < len)
		{
			// No. Reallocate it
			m_scratchpad.m_tempArea = new int[len + 100];
		}

		System.arraycopy(m_ptr, r1Start, m_scratchpad.m_tempArea, 0, len);
		System.arraycopy(m_ptr, r2Start, m_ptr, r1Start, len);
		System.arraycopy(m_scratchpad.m_tempArea, 0, m_ptr, r2Start, len);
	}

	/**
	 * Add the range to the stack containing ranges that are left to sort.
	 */
	private void addRangeToStack(final int bucketStartPos, final int bucketLen, final int depth)
	{
		if (bucketLen < 2)
		{
			// Already sorted
			return;
		}
		else
		{
			m_sortStack[++m_sortStackPointer] = new QuickSortRangeInfo(bucketStartPos, bucketLen, depth);
		}
	}

	/**
	 * Quicksort the range.
	 * <p>
	 * This method is declared package-private for the unit tests.
	 */
	void quickSortRange(final QuickSortRangeInfo qsri)
	{
		// Select the pivot element.
		final int pivot = selectPivot(qsri);

		// Move the pivot into the first position
		swap(qsri.m_bucketStartPos, pivot);

		// First check if all characters are equal at the given depth, in which
		// case we increase the depth and try again
		int sortDepth = qsri.m_depth;

		// The sort depth threshold should be less than the overshoot. If it
		// were not, we would have to think of the boundaries of the m_data
		// array and such.
		assert sortDepth < DATA_OVERSHOOT;

		int posAtFirstDifferingValue = getPositionOfFirstDifferingValue(qsri.m_bucketStartPos, qsri.m_bucketLen, sortDepth);
		while (posAtFirstDifferingValue == -1)
		{
			// All characters at the current depth are equal. Sort using an
			// increased depth.

			if (sortDepth == m_length)
			{
				// We hit the tiles. All strings are equal.
				return;
			}
			else
			{
				if (++sortDepth < QUICKSORT_DEPTH_THRESHOLD)
				{
					posAtFirstDifferingValue = getPositionOfFirstDifferingValue(qsri.m_bucketStartPos, qsri.m_bucketLen, sortDepth);
				}
				else
				{
					// Use shell sort instead
					shellSortRange(qsri);
					return;
				}
			}
		}

		// Sort using the calculated depth.

		// Iterate through the data to sort using two pointers advancing
		// from each end of the data range to sort.
		// Create one area at the start of the range and one at the end of
		// the range where we move values that are equal to the pivot value.
		int lowPtr = posAtFirstDifferingValue;
		// Pointer pointing to the element after the lower pivot range
		int lowPivotRangePtr = posAtFirstDifferingValue;
		int hiPtr = qsri.m_bucketStartPos + qsri.m_bucketLen - 1;
		// Pointer pointing to the element before the upper pivot range.
		int hiPivotRangePtr = hiPtr;
		int pivotVal = getDataAt(m_ptr[qsri.m_bucketStartPos] + sortDepth);
		while (true)
		{
			int curData;
			// Move the lower pointer forward
			while (lowPtr <= hiPtr && (curData = getDataAt(m_ptr[lowPtr] + sortDepth)) <= pivotVal)
			{
				if (curData == pivotVal)
				{
					// Move the data into the lower pivot range and increase
					// the pivot range pointer.
					swap(lowPtr, lowPivotRangePtr++);
				}
				lowPtr++;
			}

			// Move the upper pointer backwards
			while (lowPtr <= hiPtr && (curData = getDataAt(m_ptr[hiPtr] + sortDepth)) >= pivotVal)
			{
				if (curData == pivotVal)
				{
					// Move the data into the upper pivot range and decrease
					// the pivot range pointer.
					swap(hiPtr, hiPivotRangePtr--);
				}
				hiPtr--;
			}

			if (lowPtr > hiPtr)
			{
				// We're done
				break;
			}

			// Now the value at lowPtr is larger than the pivot
			// value and the value at hiPtr is smaller. Swap the two
			// values and continue moving the pointers.
			swap(lowPtr++, hiPtr--);
		}

		// Merge and move the two pivot ranges to the center of the array
		// and sort the three resulting segments.

		// Swap the smallest possible ranges
		final int lowRangeLen = lowPtr - lowPivotRangePtr;
		int rlen = Math.min(lowPivotRangePtr - qsri.m_bucketStartPos, lowRangeLen);
		if (rlen > 0)
		{
			swapRanges(qsri.m_bucketStartPos, lowPtr - rlen, rlen);
		}

		final int hiRangeLen = hiPivotRangePtr - hiPtr;
		rlen = Math.min(qsri.m_bucketStartPos + qsri.m_bucketLen - hiPivotRangePtr - 1, hiRangeLen);
		if (rlen > 0)
		{
			swapRanges(lowPtr, qsri.m_bucketStartPos + qsri.m_bucketLen - rlen, rlen);
		}
		final int pivotRangeLen = qsri.m_bucketLen - lowRangeLen - hiRangeLen;

		// Sort the lower range
		addRangeToStack(qsri.m_bucketStartPos, lowRangeLen, sortDepth);
		// Sort the pivot range at an increased depth
		addRangeToStack(qsri.m_bucketStartPos + lowRangeLen, pivotRangeLen, sortDepth + 1);
		// Sort the higher range
		addRangeToStack(qsri.m_bucketStartPos + lowRangeLen + pivotRangeLen, hiRangeLen, sortDepth);
	}

	/**
	 * Sort all strings in the bucket.
	 * <p>
	 * This method is declared package private for the unit tests.
	 * @param bucketStartPos The start position of the bucket.
	 * @param bucketLen The length of the bucket.
	 * @param depth The depth to start comparing strings at. (The strings are
	 * all equal at lower depths.)
	 */
	void sortBucket(final int bucketStartPos, final int bucketLen, final int depth)
	{
		if (bucketLen < 2)
		{
			// Already sorted
			return;
		}

		assert m_sortStackPointer == -1;

		// Use a stack with quick sort pass settings instead of recursing since
		// the stack may become very large.
		m_sortStack[++m_sortStackPointer] = new QuickSortRangeInfo(bucketStartPos, bucketLen, depth);
		while (m_sortStackPointer >= 0)
		{
			QuickSortRangeInfo qsri = m_sortStack[m_sortStackPointer--];

			// The minimum length of the segments to sort is 2. That is ensured
			// by the addRangeToStack method.

			if ((qsri.m_bucketLen < m_minLengthForQuicksort) || (qsri.m_depth > QUICKSORT_DEPTH_THRESHOLD))
			{
				shellSortRange(qsri);
			}
			else
			{
				// This adds up to three new sort ranges to the stack
				// (values less than, equal to and higher than the pivot value)
				quickSortRange(qsri);
			}
		}
	}

	/**
	 * Calculate the sort order for all big buckets. (256 of them in all, each
	 * containing 256 small buckets.)
	 * <p>
	 * Smaller buckets are sorted before larger. This is a more efficient way of
	 * filling the sort cache.
	 * @param bucketStartPositions The start positions for all large buckets.
	 * @return An array containing the indices of the large buckets in the order
	 * that they should be sorted.
	 */
	private int[] establishSortOrder(final int[] bucketStartPositions)
	{
		final int[] sortOrder = m_scratchpad.m_sortOrder;
		for (int i = 0; i < 256; i++)
		{
			sortOrder[i] = i;
		}

		// Shell sort the sort orders
		// incPtr == 4 gives an increment of 121
		for (int incPtr = 4; incPtr >= 0; incPtr--)
		{
			final int increment = SHELL_SORT_INCREMENTS[incPtr];
			for (int i = increment; i < sortOrder.length; i++)
			{
				INCLOOP: for (int j = i; j >= increment; j -= increment)
				{
					// Which of the lengths of the big buckets is the longest
					final int so1 = sortOrder[j - increment];
					final int so2 = sortOrder[j];
					if ((bucketStartPositions[so1 * 256 + 255] - bucketStartPositions[so1 * 256]) > (bucketStartPositions[so2 * 256 + 255] - bucketStartPositions[so2 * 256]))
					{
						sortOrder[j] = so1;
						sortOrder[j - increment] = so2;
					}
					else
					{
						// This sort order element is in its right position.
						break INCLOOP;
					}
				}
			}
		}

		return sortOrder;
	}

	/**
	 * Sort the data. This method borrows optimizations from bzip2 1.0.5.
	 * @return An array with pointers from each byte's original position to its
	 * position in the sorted data.
	 */
	int[] sort()
	{
		if (m_length == 0)
		{
			return new int[0];
		}

		// Run a least significant digit radix sort on all two-byte permutations
		// of the incoming data. This gives 256^2 buckets with similar data
		// which can then be sorted individually.

		// This method call also creates and populates the m_ptr array.
		// The bucketStartPositions has an overshoot of one position, which
		// gives it the length 65537. The overshoot element should be equal to
		// the length of the data.
		final int[] bucketStartPositions = radixSort();
		// Fix the overshoot
		bucketStartPositions[65536] = m_length;

		final boolean[] sortedLargeBuckets = m_scratchpad.m_sortedLargeBuckets;
		Arrays.fill(sortedLargeBuckets, false);
		final boolean[] sortedSmallBuckets = m_scratchpad.m_sortedSmallBuckets;
		Arrays.fill(sortedSmallBuckets, false);
		final int[] copyStart = m_scratchpad.m_copyStart;
		final int[] copyEnd = m_scratchpad.m_copyEnd;

		// Establish a sort order for all big buckets (256 of them in all) with
		// the shortest buckets coming first. This will make the sort result
		// caching optimization most efficient
		final int[] sortOrder = establishSortOrder(bucketStartPositions);

		// Quick sort the elements in each non-empty bucket.
		for (int largeBucketIndex = 0; largeBucketIndex < 256; largeBucketIndex++)
		{
			final int largeBucketNo = sortOrder[largeBucketIndex];
			for (int smallBucketNo = 0; smallBucketNo < 256; smallBucketNo++)
			{
				// Don't sort when smallBucketNo == largeBucketNo. This small
				// bucket will be dealt with by the scanning step below.
				if (smallBucketNo != largeBucketNo)
				{
					final int bucketIndex = largeBucketNo * 256 + smallBucketNo;
					if (!sortedSmallBuckets[bucketIndex])
					{
						final int bucketStartPos = bucketStartPositions[bucketIndex];
						final int bucketLen = bucketStartPositions[bucketIndex + 1] - bucketStartPos;

						if (bucketLen > 1)
						{
							// More than one data element in this bucket. Sort it.
							sortBucket(bucketStartPos, bucketLen, 2);
						}
						sortedSmallBuckets[bucketIndex] = true;
					}
				}
			}

			// Now that we have sorted all small buckets in the large bucket n,
			// we can infer the sorted order for the small bucket n in all
			// large buckets m, including (magically) the small bucket n in the
			// large bucket n that we did not sort above.
			for (int m = 0; m < 256; m++)
			{
				copyStart[m] = bucketStartPositions[m * 256 + largeBucketNo];
				copyEnd[m] = bucketStartPositions[m * 256 + largeBucketNo + 1] - 1;
			}

			for (int i = bucketStartPositions[largeBucketNo * 256]; i < copyStart[largeBucketNo]; i++)
			{
				int k = m_ptr[i] - 1;
				if (k < 0)
				{
					k += m_length;
				}
				final int m = getDataAt(k);
				if (!sortedLargeBuckets[m])
				{
					int index = copyStart[m]++;
					if (index >= m_length)
					{
						index -= m_length;
					}
					m_ptr[index] = k;
				}
			}

			for (int i = bucketStartPositions[(largeBucketNo + 1) * 256] - 1; i > copyEnd[largeBucketNo]; i--)
			{
				int k = m_ptr[i] - 1;
				if (k < 0)
				{
					k += m_length;
				}
				final int m = getDataAt(k);
				if (!sortedLargeBuckets[m])
				{
					int index = copyEnd[m]--;
					if (index < 0)
					{
						index += m_length;
					}
					m_ptr[index] = k;
				}
			}

			// Mark all buckets that we got for free as sorted
			for (int m = 0; m < 256; m++)
			{
				sortedSmallBuckets[m * 256 + largeBucketNo] = true;
			}

			sortedLargeBuckets[largeBucketNo] = true;

			// Fix the sort cache for the large bucket.
			// Don't do it for the last sorted bucket.
			if (largeBucketIndex != 255)
			{
				final int largeBucketStart = bucketStartPositions[largeBucketNo * 256];
				final int largeBucketEnd;
				if (largeBucketNo < 255)
				{
					largeBucketEnd = bucketStartPositions[(largeBucketNo + 1) * 256];
				}
				else
				{
					largeBucketEnd = m_length;
				}
				final int largeBucketSize = largeBucketEnd - largeBucketStart;
				assert largeBucketSize >= 0;

				int shifts = 0;
				while (largeBucketSize >>> shifts > 65534)
				{
					shifts++;
				}

				for (int i = largeBucketSize - 1; i >= 0; i--)
				{
					final int sptr = m_ptr[largeBucketStart + i];
					final int qval = i >>> shifts;
					m_sortCache[sptr] = qval;
					if (sptr < DATA_OVERSHOOT)
					{
						// Update cache in overshoot too
						m_sortCache[m_length + sptr] = qval;
					}
				}
			}
		}
		return m_ptr;
	}
}
