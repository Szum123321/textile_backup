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

import org.at4j.support.lang.At4JException;

/**
 * This object contains settings for the {@link BZip2OutputStream}.
 * <p>
 * When created, this object contains the default settings. Modify the settings
 * by calling setter methods on this object.
 * @author Karl Gustafsson
 * @since 1.1
 * @see BZip2OutputStream
 */
public class BZip2OutputStreamSettings implements Cloneable
{
	/**
	 * The minimum size of an encoded data block in hundreds of kilobytes. Using
	 * a small block size gives faster but worse compression.
	 */
	public static final int MIN_BLOCK_SIZE = 1;

	/**
	 * The maximum size of an encoded data block in hundreds of kilobytes. Using
	 * a large block size gives slower but better compression.
	 */
	public static final int MAX_BLOCK_SIZE = 9;

	/**
	 * The default block size.
	 */
	public static final int DEFAULT_BLOCK_SIZE = MAX_BLOCK_SIZE;

	/**
	 * The default number of Huffman tree refinement iterations. By having more
	 * tree refinement iterations the compression gets better, but as the number
	 * is increased the returns are diminishing.
	 */
	public static final int DEFAULT_NO_OF_HUFFMAN_TREE_REFINEMENT_ITERATIONS = 5;

	/**
	 * The default number of encoder threads.
	 */
	public static final int DEFAULT_NO_OF_ENCODER_THREADS = 0;

	private int m_blockSize = DEFAULT_BLOCK_SIZE;
	private int m_numberOfHuffmanTreeRefinementIterations = DEFAULT_NO_OF_HUFFMAN_TREE_REFINEMENT_ITERATIONS;
	private int m_numberOfEncoderThreads = DEFAULT_NO_OF_ENCODER_THREADS;
	private BZip2EncoderExecutorService m_executorService;

	/**
	 * Set the size of compressed data blocks. A high block size gives good but
	 * slow compression. A low block size gives worse but faster compression.
	 * <p>
	 * The default block size is 9 (the highest permitted value).
	 * @param bs The block size in hundreds of kilobytes. This should be between
	 * 1 and 9 (inclusive).
	 * @return {@code this}
	 * @throws IllegalArgumentException If the block size is not in the
	 * permitted range.
	 */
	public BZip2OutputStreamSettings setBlockSize(int bs) throws IllegalArgumentException
	{
		if (bs < MIN_BLOCK_SIZE || bs > MAX_BLOCK_SIZE)
		{
			throw new IllegalArgumentException("Invalid block size " + bs + ". It must be between " + MIN_BLOCK_SIZE + " and " + MAX_BLOCK_SIZE + " (inclusive)");
		}
		m_blockSize = bs;
		return this;
	}

	/**
	 * Get the block size for a compressed data block.
	 * @return The block size for a compressed data block.
	 */
	public int getBlockSize()
	{
		return m_blockSize;
	}

	/**
	 * Set the number of tree refinement iterations that are run when creating
	 * Huffman trees for each compressed data block.
	 * <p>
	 * A higher value for this parameter should give better but slower
	 * compression. As the value increases the returns are diminishing.
	 * <p>
	 * The default value is five refinement iterations.
	 * @param no The number of Huffman tree refinement iterations. This should
	 * be a positive integer larger than zero.
	 * @return {@code this}
	 * @throws IllegalArgumentException If the number is not a positive integer
	 * larger than zero.
	 */
	public BZip2OutputStreamSettings setNumberOfHuffmanTreeRefinementIterations(int no) throws IllegalArgumentException
	{
		if (no < 1)
		{
			throw new IllegalArgumentException("Invalid value " + no + ". It must be greater than zero");
		}
		m_numberOfHuffmanTreeRefinementIterations = no;
		return this;
	}

	/**
	 * Get the number of Huffman tree refinement iterations.
	 * @return The number of Huffman tree refinement iterations.
	 */
	public int getNumberOfHuffmanTreeRefinementIterations()
	{
		return m_numberOfHuffmanTreeRefinementIterations;
	}

	/**
	 * Set a  for logging diagnostic output to. Output is
	 * logged to the debug and trace levels.
	 * <p>
	 * By default no log adapter is used and hence no diagnostic output is
	 * logged.
	 * @param la A log adapter.
	 * @return {@code this}
	 */
	public BZip2OutputStreamSettings setLogAdapter(Object la)
	{
		return this;
	}


	/**
	 * Set the number of encoder threads used for bzip2 compressing data. bzip2
	 * encoding is CPU intensive and giving the encoder more threads to work
	 * with can drastically shorten the encoding time. The drawback is that the
	 * memory consumption grows since each encoder thread must keep its data in
	 * memory.
	 * <p>
	 * The default number of encoder threads is zero, which means that the
	 * thread that is writing the data to the {@link BZip2OutputStream} will be
	 * used for the encoding.
	 * <p>
	 * For the shortest encoding time, use as many threads as there are
	 * available CPU:s in the system.
	 * @param no The number of encoder threads to use. If this is set to {@code
	 * 0}, the encoding will be done in the thread writing to the stream.
	 * @return {@code this}
	 * @throws IllegalArgumentException If {@code no} is negative.
	 * @see #setExecutorService(BZip2EncoderExecutorService)
	 */
	public BZip2OutputStreamSettings setNumberOfEncoderThreads(int no) throws IllegalArgumentException
	{
		if (no < 0)
		{
			throw new IllegalArgumentException("Invalid number of encoder threads " + no + ". The number must be zero or greater");
		}

		m_numberOfEncoderThreads = no;
		return this;
	}

	public int getNumberOfEncoderThreads()
	{
		return m_numberOfEncoderThreads;
	}

	/**
	 * Set an executor service that the {@link BZip2OutputStream} will use to
	 * spread the encoding over several threads. This executor can be shared
	 * among several {@link BZip2OutputStream} objects.
	 * <p>
	 * If an executor service is set using this method, all threads that are
	 * available to the executor is used for the encoding and any value set
	 * using {@link #setNumberOfEncoderThreads(int)} is ignored.
	 * <p>
	 * An executor service is created using the
	 * {@link BZip2OutputStream#createExecutorService()} or the
	 * {@link BZip2OutputStream#createExecutorService(int)} method.
	 * @param executorService The executor service.
	 * @return {@code this}
	 * @see #setNumberOfEncoderThreads(int)
	 */
	public BZip2OutputStreamSettings setExecutorService(BZip2EncoderExecutorService executorService)
	{
		m_executorService = executorService;
		return this;
	}

	public BZip2EncoderExecutorService getExecutorService()
	{
		return m_executorService;
	}

	/**
	 * Make a copy of this object.
	 */
	@Override
	public BZip2OutputStreamSettings clone()
	{
		try
		{
			return (BZip2OutputStreamSettings) super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new At4JException("Bug", e);
		}
	}
}
