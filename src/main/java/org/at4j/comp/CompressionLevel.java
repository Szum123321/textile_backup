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
package org.at4j.comp;

/**
 * This is an enumeration over different generic compression levels supported by
 * some of At4J's compression algorithm.
 * @author Karl Gustafsson
 * @since 1.0.2
 */
public enum CompressionLevel
{
	BEST("best"), DEFAULT("default"), FASTEST("fastest");

	private final String m_tag;

	private CompressionLevel(String tag)
	{
		m_tag = tag;
	}

	@Override
	public String toString()
	{
		return m_tag + " compression";
	}
}
