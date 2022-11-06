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
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.at4j.support.io.LittleEndianBitOutputStream;

/**
 * This is an {@link OutputStream} for bzip2 compressing data.
 * <p>
 * For more information on the inner workings of bzip2, see <a
 * href="http://en.wikipedia.org/wiki/Bzip2">the Wikipedia article on bzip2</a>.
 * <p>
 * This stream is <i>not</i> safe for concurrent access by several writing
 * threads. A client must provide external synchronization to use this from
 * several threads.
 * @author Karl Gustafsson
 * @since 1.1
 * @see BZip2OutputStreamSettings
 */
public class BZip2OutputStream extends OutputStream implements AutoCloseable
{
	private static final byte[] EOS_MAGIC = new byte[] { 0x17, 0x72, 0x45, 0x38, 0x50, (byte) 0x90 };

	// This is used to generate unique hash codes for each created stream
	// object.
	private static final AtomicInteger HASH_CODE_GENERATOR = new AtomicInteger(0);

	private final LittleEndianBitOutputStream m_wrapped;
	// The block size in bytes
	private final int m_blockSize;
	// This may be null

	// Data stream that writes to the block currently being filled with data.
	private final BlockOutputStream m_blockOutputStream;
	// If several threads are used to encode the data, this is used to write the
	// encoded blocks in the right order.
	private final EncodedBlockWriter m_encodedBlockWriter;
	private final BZip2EncoderExecutorServiceImpl m_executorService;
	private final boolean m_iCreatedExecutor;
	private final int m_hashCode = HASH_CODE_GENERATOR.getAndIncrement();

	private boolean m_closed;
	private long m_pos = 0;

	private static void writeFileHeader(OutputStream os, int blockSize) throws IOException
	{
		// File header
		os.write('B');
		os.write('Z');
		// File version
		os.write('h');
		// Block size as a character. The ASCII code for 0 is 48.
		os.write(blockSize + 48);
	}

	/**
	 * Create a new bzip2 compressing output stream with default settings.
	 * @param wrapped Compressed data is written to this stream.
	 * @throws IOException On errors writing the file header.
	 * @see #BZip2OutputStream(OutputStream, BZip2OutputStreamSettings)
	 */
	public BZip2OutputStream(OutputStream wrapped) throws IOException
	{
		this(wrapped, new BZip2OutputStreamSettings());
	}

	/**
	 * Create a new bzip2 compressing output stream.
	 * @param wrapped Compressed data is written to this stream.
	 * @param settings Compression settings.
	 * @throws IOException On errors writing the file header.
	 * @see #BZip2OutputStream(OutputStream)
	 */
	public BZip2OutputStream(OutputStream wrapped, BZip2OutputStreamSettings settings) throws IOException
	{
		// Null checks
		wrapped.getClass();
		settings.getClass();

		m_wrapped = new LittleEndianBitOutputStream(wrapped);
		// bzip2 uses 1kb == 1000b
		m_blockSize = settings.getBlockSize() * 100 * 1000;

		writeFileHeader(wrapped, settings.getBlockSize());

		EncodingScratchpad sp;
		if (settings.getExecutorService() != null)
		{
			// Use the supplied executor service
			// There is only one allowed implementation for now.
			m_executorService = (BZip2EncoderExecutorServiceImpl) settings.getExecutorService();
			m_iCreatedExecutor = false;
			m_encodedBlockWriter = new EncodedBlockWriter(m_wrapped);
			// Each encoder thread has its own scratchpad
			sp = null;
		}
		else if (settings.getNumberOfEncoderThreads() > 0)
		{
			// Use separate encoder threads.
			m_executorService = new BZip2EncoderExecutorServiceImpl(settings.getNumberOfEncoderThreads(), new SingleObserverErrorState());
			m_iCreatedExecutor = true;
			m_encodedBlockWriter = new EncodedBlockWriter(m_wrapped);
			// Each encoder thread has its own scratchpad
			sp = null;
		}
		else
		{
			// Encode in the thread writing to the stream.
			m_executorService = null;
			m_iCreatedExecutor = false;
			sp = new EncodingScratchpad();
			m_encodedBlockWriter = null;
		}

		m_blockOutputStream = new BlockOutputStream(m_wrapped, m_blockSize, settings.getNumberOfHuffmanTreeRefinementIterations() , m_executorService, this, m_encodedBlockWriter, sp);
	}

	private void assertNotClosed() throws IOException
	{
		if (m_closed)
		{
			throw new IOException("This stream is closed");
		}
	}

	private void checkErrorState() throws IOException, RuntimeException
	{
		if (m_executorService != null)
		{
			m_executorService.getErrorState().checkAndClearErrors(this);
		}
	}

	private void debug(String msg)
	{

	}

	private void writeEosBlock() throws IOException
	{
		// Write the end of stream magic
		for (byte b : EOS_MAGIC) {
			m_wrapped.writeBitsLittleEndian(b & 0xFF, 8);
		}
		// Write file checksum
		m_wrapped.writeBitsLittleEndian(m_blockOutputStream.getFileChecksum(), 32);
		m_wrapped.padToByteBoundary();
	}

	@Override
	public void write(int b) throws IOException
	{
		assertNotClosed();
		checkErrorState();

		m_pos++;
		m_blockOutputStream.write(b & 0xFF);
	}

	@Override
	public void write(byte[] data) throws IOException
	{
		assertNotClosed();
		checkErrorState();

		m_pos += data.length;
		m_blockOutputStream.write(data);
	}

	@Override
	public void write(byte[] data, int offset, int len) throws IOException, IndexOutOfBoundsException
	{
		assertNotClosed();
		checkErrorState();

		if (offset < 0)
		{
			throw new IndexOutOfBoundsException("Offset: " + offset);
		}
		if (len < 0)
		{
			throw new IndexOutOfBoundsException("Length: " + len);
		}
		if (offset + len > data.length)
		{
			throw new IndexOutOfBoundsException("Offset: " + offset + " + Length: " + len + " > length of data: " + data.length);
		}

		m_pos += len;
		m_blockOutputStream.write(data, offset, len);
	}

	@Override
	public void close() throws IOException
	{
		checkErrorState();

		if (!m_closed)
		{
			// This writes out any remaining run length encoding data and closes
			// the block output stream.
			m_blockOutputStream.close();

			if ((m_pos > 0) && (m_encodedBlockWriter != null))
			{
				// Wait for all blocks to be written.
				try
				{
					m_encodedBlockWriter.waitFor();
				}
				catch (InterruptedException e)
				{
					// Repackage
					throw new IOException("Interrupted. The output file is most likely corrupted.");
				}
				checkErrorState();
			}

			writeEosBlock();

			m_wrapped.close();

			debug("Original size: " + m_pos + ", compressed size: " + m_wrapped.getNumberOfBytesWritten());

			if (m_iCreatedExecutor && (m_executorService != null))
			{
				m_executorService.shutdown();
			}
			m_closed = true;
			super.close();
		}
	}

	@Override
	public int hashCode()
	{
		return m_hashCode;
	}

	@Override
	public boolean equals(Object o)
	{
		return this == o;
	}

	/**
	 * Create a {@link BZip2EncoderExecutorService} that can be shared between
	 * several {@link BZip2OutputStream}:s to spread the bzip2 encoding work
	 * over several threads. The created executor service can be passed to the
	 * {@link BZip2OutputStream} constructor in a
	 * {@link BZip2OutputStreamSettings} object.
	 * @param noThreads The number of threads available to the executor.
	 * @return The executor service.
	 */
	public static BZip2EncoderExecutorService createExecutorService(int noThreads)
	{
		return new BZip2EncoderExecutorServiceImpl(noThreads, new MultipleObserverErrorState());
	}

	/**
	 * Create a {@link BZip2EncoderExecutorService} that can be shared between
	 * several {@link BZip2OutputStream}:s to spread the bzip2 encoding work
	 * over several threads. The created executor service can be passed to the
	 * {@link BZip2OutputStream} constructor in a
	 * {@link BZip2OutputStreamSettings} object.
	 * <p>
	 * The created executor will have as many threads available to it as there
	 * are CPU:s available to the JVM.
	 * @return The executor service.
	 */
	public static BZip2EncoderExecutorService createExecutorService()
	{
		return createExecutorService(Runtime.getRuntime().availableProcessors());
	}
}
