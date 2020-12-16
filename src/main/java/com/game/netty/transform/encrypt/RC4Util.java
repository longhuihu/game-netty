/*
 * Copyright 2020 Long Huihu
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.game.netty.transform.encrypt;

public class RC4Util {

    private final byte[] key;

    public RC4Util(String keyStr) {
        if (keyStr == null || keyStr.length() < 16) {
            throw new RuntimeException("RC4ex key invalid: " + keyStr);
        }
        this.key = initKey(keyStr);
    }

    public void rc4(byte[] input) {
        rc4(input,0,input.length);
    }

    public void rc4(byte[] input, int offset, int length) {
        byte[] key = this.key.clone();
        int x = 0;
        int y = 0;
        for (int i = offset; i < offset+length; i++) {
            x = (x + 1) & 0xff;
            y = ((key[x] & 0xff) + y) & 0xff;
            byte tmp = key[x];
            key[x] = key[y];
            key[y] = tmp;
            int xorIndex = ((key[x] & 0xff) + (key[y] & 0xff)) & 0xff;
            input[i] = (byte) (input[i] ^ key[xorIndex]);
        }
    }

    private static byte[] initKey(String aKey) {
        byte[] b_key = aKey.getBytes();
        byte[] state = new byte[256];
        for (int i = 0; i < 256; i++) {
            state[i] = (byte) i;
        }
        int index1 = 0;
        int index2 = 0;
        for (int i = 0; i < 256; i++) {
            index2 = ((b_key[index1] & 0xff) + (state[i] & 0xff) + index2) & 0xff;
            byte tmp = state[i];
            state[i] = state[index2];
            state[index2] = tmp;
            index1 = (index1 + 1) % b_key.length;
        }
        return state;
    }
}
