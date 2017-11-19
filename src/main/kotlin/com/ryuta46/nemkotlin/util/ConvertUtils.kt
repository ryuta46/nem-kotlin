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
package com.ryuta46.nemkotlin.util

/**
 * Convert utility functions
 */
class ConvertUtils {
    companion object {
        /**
         * Converts from byte array to hex string.
         * @param bytes input byte array
         * @return hex string
         */
        @JvmStatic fun toHexString(bytes : ByteArray): String {
            val result = StringBuffer()
            bytes.forEach {
                result.append(String.format("%02x",it))
            }
            return result.toString()
        }

        /**
         * Converts from hex string to byte array
         * @param s input hex string
         * @return byte array
         */
        fun toByteArray(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                val upper = Character.digit(s[i], 16) shl 4
                val lower = Character.digit(s[i + 1], 16)
                data[i / 2] = ( lower + upper).toByte()
                i += 2
            }
            return data
        }

        /**
         * Swaps byte array.
         * @param bytes input byte array
         * @return swapped byte array
         */
        fun swapByteArray(bytes: ByteArray): ByteArray {
            val ret =  ByteArray(bytes.size)
            for(i in 0 until bytes.size) {
                ret[ret.size - i - 1] =  bytes[i]
            }
            return ret
        }

        /**
         * Converts from 32 bit integer value to byte array with little endian.
         * @param value input 32 bit integer.
         * @return little endian byte array of 4 elements.
         */
        fun toByteArrayWithLittleEndian(value: Int): ByteArray {
            val ret = ByteArray(4)
            for (i in 0..3) {
                ret[i] = (value shr (i * 8) and 0xFF).toByte()
            }
            return ret
        }

        /**
         * Converts from 64 bit integer value to byte array with little endian.
         * @param value input 64 bit integer.
         * @return little endian byte array of 8 elements.
         */
        fun toByteArrayWithLittleEndian(value: Long): ByteArray {
            val ret = ByteArray(8)
            for (i in 0..7) {
                ret[i] = (value shr (i * 8) and 0xFF).toByte()
            }
            return ret
        }

        /**
         * Encodes byte array to string with Base32.
         * This is simple encoding and not fill with padding characters.
         * @param bytes input byte array
         * @return Base32 encoded string.
         */
        fun encodeWithBase32(bytes: ByteArray): String {
            var ret = ""
            var bitCount = 0
            var current = 0
            bytes.forEach { byte ->
                val intVal = byte.toInt()
                for (i in 7 downTo 0) {
                    current = current.shl(1) + (intVal.shr(i) and 1)
                    bitCount++

                    if (bitCount == 5) {
                        ret += if (current < 26) {
                            'A' + current
                        } else {
                            '2' + current - 26
                        }
                        bitCount = 0
                        current = 0
                    }
                }
            }
            return ret
        }
    }
}