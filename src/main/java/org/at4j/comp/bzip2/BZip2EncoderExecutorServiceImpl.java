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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is the only implementation of {@link BZip2EncoderExecutorService}. All
 * objects that are using that interface assume that it is implemented by this
 * class.
 * @author Karl Gustafsson
 * @since 1.1
 */
final class BZip2EncoderExecutorServiceImpl implements BZip2EncoderExecutorService
{
	/**
	 * This rejected execution handler shoehorns in a job in an
	 * {@link ExecutorService}'s job queue if it is rejected by the service.
	 * This requires that the service's job queue has an upper bound and that it
	 * blocks when trying to insert more elements than the bound.
	 * @author Karl Gustafsson
	 * @since 1.1
	 */
	private static class ShoehornInJobRejectedExecutionHandler implements RejectedExecutionHandler
	{
		private static final ShoehornInJobRejectedExecutionHandler INSTANCE = new ShoehornInJobRejectedExecutionHandler();

		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
		{
			//			System.out.print("Shoehorning... ");
			try
			{
				executor.getQueue().put(r);
			}
			catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}
			//			System.out.println("done");
		}
	}

	private final ThreadPoolExecutor m_executor;
	private final ErrorState m_errorState;

	BZip2EncoderExecutorServiceImpl(int noThreads, ErrorState es)
	{
		m_executor = new ThreadPoolExecutor(noThreads, noThreads, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1), new EncodingThreadFactory(es), ShoehornInJobRejectedExecutionHandler.INSTANCE);
		m_errorState = es;
	}

	ErrorState getErrorState()
	{
		return m_errorState;
	}

	void execute(BlockEncoderRunnable r)
	{
		m_executor.execute(r);
	}

	public void shutdown()
	{
		m_executor.shutdown();
	}
}
