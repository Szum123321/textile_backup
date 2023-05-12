/*
 * A simple backup mod for Fabric
 * Copyright (C)  2022   Szum123321
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.szum123321.textile_backup.core.digest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * This algorithm copies the construction of <a href="https://ticki.github.io/blog/seahash-explained/">SeaHash</a> including its IV.
 * What it differs in is that it uses Xoroshift64* instead of PCG as its pseudo-random function. Although it might lower
 * the output quality, I don't think it matters that much, honestly. One advantage of xoroshift is that it should be
 * easier to implement with AVX. Java should soon ship its vector api by default.
 */
public class BalticHash implements Hash {
    //SeaHash IV
    protected final static long[] IV = { 0x16f11fe89b0d677cL, 0xb480a793d8e6c86cL, 0x6fe2e5aaf078ebc9L, 0x14f994a4c5259381L };
    private final long[] state = Arrays.copyOf(IV, IV.length);
    protected final int buffer_limit = state.length * Long.BYTES;
    protected final byte[] _byte_buffer = new byte[(state.length + 1) * Long.BYTES];
    //Enforce endianness
    protected final ByteBuffer buffer = ByteBuffer.wrap(_byte_buffer).order(ByteOrder.LITTLE_ENDIAN);
    protected long hashed_data_length = 0;

    public void update(int b) {
        buffer.put((byte)b);
        hashed_data_length += 1;
        if (buffer.position() >= buffer_limit) round();
    }

    public void update(long b) {
        buffer.putLong(b);
        hashed_data_length += Long.BYTES;
        if(buffer.position() >= buffer_limit) round();
    }

    public void update(byte[] data, int off, int len) {
        int pos = 0;
        while(pos < len) {
            int n = Math.min(len - pos, buffer_limit - buffer.position());
            System.arraycopy(data, off + pos, _byte_buffer, buffer.position(), n);
            pos += n;
            buffer.position(buffer.position() + n);
            if(buffer.position() >= buffer_limit) round();
        }

        hashed_data_length += len;
    }

    public long getValue() {
        if(buffer.position() != 0) {
            while(buffer.position() < buffer_limit) buffer.put((byte)0);
            round();
        }

        long result = state[0];
        result ^= state[1];
        result ^= state[2];
        result ^= state[3];
        result ^= hashed_data_length;

        return xorshift64star(result);
    }

    protected void round() {
        int p = buffer.position();
        buffer.rewind();

        for(int i = 0; i < 4; i++) state[i] ^= buffer.getLong();
        for(int i = 0; i < 4; i++) state[i] = xorshift64star(state[i]);

        if(p > buffer_limit) {
            System.arraycopy(_byte_buffer, buffer_limit, _byte_buffer, 0, buffer.limit() - p);
            buffer.position(buffer.limit() - p);
        } else buffer.rewind();
    }

    long xorshift64star(long s) {
        s ^= (s >> 12);
        s ^= (s << 25);
        s ^= (s >> 27);
        return s * 0x2545F4914F6CDD1DL;
    }
}
