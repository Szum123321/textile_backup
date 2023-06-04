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

package net.szum123321.textile_backup.test;

import net.minecraft.util.math.random.Random;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.TextileLogger;
import net.szum123321.textile_backup.core.digest.BalticHash;

public class BalticHashTest {
    private final static TextileLogger log = new TextileLogger(TextileBackup.MOD_NAME);
    final static int TEST_LEN = 21377; //simple prime
    public static void run() throws RuntimeException {
        log.info("Running hash test");
        Random r = Random.create(2137);
        long x = 0;

        byte[] data = new byte[TEST_LEN];

        for(int i = 0; i < TEST_LEN; i++) data[i] = (byte)r.nextInt();

        //Test block mode
        for(int i = 0; i < 5*2; i++) x ^= randomHash(data, r);
        if(x != 0) throw new RuntimeException("Hash mismatch!");

        log.info("Test passed");
    }

    static long randomHash(byte[] data, Random r) {
        int n = data.length;

        BalticHash h = new BalticHash();

        int m = r.nextBetween(1, n);

        int nn = n, p = 0;

        for(int i = 0; i < m; i++) {
            int k = r.nextBetween(1, nn - (m - i - 1));
            h.update(data, p, k);
            p += k;
            nn -= k;
        }

        return h.getValue();
    }
}
