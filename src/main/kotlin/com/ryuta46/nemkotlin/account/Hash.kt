/*
 * MIT License
 *
 * Copyright (c) 2017 Taizo Kusuda 
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.ryuta46.nemkotlin.account


import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.MessageDigest
import java.security.Security

/**
 * Hash functions
 */
class Hash {
    companion object {
        /**
         * Initializes hash functions. This method is called by nem-kotlin internally
         */
        @JvmStatic fun initialize() {
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }

        /**
         * Calculates SHA3 (Keccak) 256 bit hash
         * @param message Input message.
         * @return 256 bit hash
         */
        @JvmStatic fun sha3_256(message: ByteArray): ByteArray {
            //val hash = MessageDigest.getInstance("Keccak-256")
            val hash = MessageDigest.getInstance("SHA3-256")
            return hash.digest(message)
        }

        /**
         * Calculates SHA3 (Keccak) 512 bit hash
         * @param message Input message.
         * @return 512 bit hash
         */
        @JvmStatic fun sha3_512(message: ByteArray): ByteArray {
            //val hash = MessageDigest.getInstance("Keccak-512")
            val hash = MessageDigest.getInstance("SHA3-512")
            return hash.digest(message)
        }

        /**
         * Calculates RIPEMD 160 bit hash
         * @param message Input message.
         * @return 160 bit hash.
         */
        @JvmStatic fun ripemd160(message: ByteArray): ByteArray {
            val hash = MessageDigest.getInstance("RIPEMD160")
            return hash.digest(message)
        }
    }
}
