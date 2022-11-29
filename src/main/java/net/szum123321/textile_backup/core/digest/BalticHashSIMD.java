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

//import jdk.incubator.vector.*;

import net.szum123321.textile_backup.core.digest.BalticHash;

/*Mostly working XorSeaHash impl using SIMD. Should speed up calculation on most systems currently in use

It's actually slower. I tested it by comparing runtimes while hashing a directly opened FileInputStream.
My cpu is AMD Ryzen 5 3500U

There are two reasons I can think of: either vector construction simply takes so much time or jvm auto-vectorizes better than me

It's still probably far from being the slowest part of code, so I don't expect any major slowdowns

I will keep this code here for future work perhaps
 */
public class BalticHashSIMD extends BalticHash {/*
    public BalticHashSIMD() { throw new UnsupportedOperationException(); } //For safety

    private LongVector state = LongVector.fromArray(LongVector.SPECIES_256, IV, 0);

    @Override
    public long getValue() {
        if (buffer.position() != 0) {
            while (buffer.position() < buffer_limit) buffer.put((byte) 0);
            round();
        }

        long result = state.reduceLanesToLong(VectorOperators.XOR);
        result ^= hashed_data_length;

        return xorshift64star(result);
    }

    @Override
    public void update(byte[] data, int off, int len) {
        int pos = off;
        while (pos < len) {
            int n = Math.min(len - pos, buffer_limit - buffer.position());
            if (n == 32) {
                var v = ByteVector.fromArray(ByteVector.SPECIES_256, data, pos).reinterpretAsLongs();
                state = state.lanewise(VectorOperators.XOR, v);
                state = xorshift64star(state);
            } else {
                System.arraycopy(data, pos, _byte_buffer, buffer.position(), n);
                buffer.position(buffer.position() + n);
                if (buffer.position() == buffer_limit) round();
            }
            pos += n;
        }

        hashed_data_length += len;
    }

    @Override
    protected void round() {
        var s = ByteVector.fromArray(ByteVector.SPECIES_256, _byte_buffer, 0).reinterpretAsLongs();
        state = state.lanewise(VectorOperators.XOR, s);
        state = xorshift64star(state);

        int p = buffer.position();

        if (p > buffer_limit) {
            System.arraycopy(_byte_buffer, buffer_limit, _byte_buffer, 0, buffer.limit() - p);
            buffer.position(buffer.limit() - p);
        } else buffer.rewind();
    }

    LongVector xorshift64star(LongVector v) {
        v = v.lanewise(VectorOperators.XOR, v.lanewise(VectorOperators.ASHR, 12));
        v = v.lanewise(VectorOperators.XOR, v.lanewise(VectorOperators.LSHL, 25));
        v = v.lanewise(VectorOperators.XOR, v.lanewise(VectorOperators.ASHR, 27));
        v = v.lanewise(VectorOperators.MUL, 0x2545F4914F6CDD1DL);
        return v;
    }*/
}