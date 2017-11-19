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


import junit.framework.TestCase.assertEquals
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

@RunWith(Theories::class)
class ConvertUtilsTest {
    companion object {
        @DataPoints @JvmStatic fun getToHexStringFixtures() = arrayOf(
                ToHexStringFixture(byteArrayOf(0x00, 0x01, 0x7f, 0x80.toByte(), 0xff.toByte()), "00017f80ff")
        )
        @DataPoints @JvmStatic fun getToByteArrayFixtures() = arrayOf(
                ToByteArrayFixture("00017f80ff", byteArrayOf(0x00, 0x01, 0x7f, 0x80.toByte(), 0xff.toByte()))
        )
        @DataPoints @JvmStatic fun getSwapByteArrayFixtures() = arrayOf(
                SwapByteArrayFixture(byteArrayOf(0x00, 0x01, 0x7f, 0x80.toByte(), 0xff.toByte()), byteArrayOf(0xff.toByte(), 0x80.toByte(), 0x7f, 0x01, 0x00))
        )

        @DataPoints @JvmStatic fun getToByteArrayWithLittleEndianIntFixture() = arrayOf(
                ToByteArrayWithLittleEndianIntFixture(0x007f80ff, byteArrayOf(0xff.toByte(), 0x80.toByte(), 0x7f, 0x00))
        )

        @DataPoints @JvmStatic fun getToByteArrayWithLittleEndianLongFixture() = arrayOf(
                ToByteArrayWithLittleEndianLongFixture(0x0001407f80caf0ff, byteArrayOf(0xff.toByte(), 0xf0.toByte(), 0xca.toByte(), 0x80.toByte(), 0x7f, 0x40, 0x01, 0x00))
        )

        @DataPoints @JvmStatic fun getEncodeWithBase32Fixtures() = arrayOf(
                EncodeWithBase32Fixture("689a556a8b28f1640e52e5546569f03d1367f6bf66a2b5586d", "NCNFK2ULFDYWIDSS4VKGK2PQHUJWP5V7M2RLKWDN")
        )
    }

    data class ToHexStringFixture(val bytes: ByteArray, val expected: String)

    @Theory fun toHexString(fixture: ToHexStringFixture) {
        assertEquals(fixture.expected, ConvertUtils.toHexString(fixture.bytes))
    }

    data class ToByteArrayFixture(val str: String, val expected: ByteArray)

    @Theory fun toByteArray(fixture: ToByteArrayFixture) {
        val actual = ConvertUtils.toByteArray(fixture.str)
        assertEquals(fixture.expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(fixture.expected[i], actual[i])
        }
    }

    data class SwapByteArrayFixture(val bytes: ByteArray, val expected: ByteArray)

    @Theory fun swapByteArray(fixture: SwapByteArrayFixture) {
        val actual = ConvertUtils.swapByteArray(fixture.bytes)
        assertEquals(fixture.expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(fixture.expected[i], actual[i])
        }
    }

    data class ToByteArrayWithLittleEndianIntFixture(val value: Int, val expected: ByteArray)

    @Theory fun toByteArrayWithLittleEndian(fixture: ToByteArrayWithLittleEndianIntFixture) {
        val actual = ConvertUtils.toByteArrayWithLittleEndian(fixture.value)
        assertEquals(fixture.expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(fixture.expected[i], actual[i])
        }
    }

    data class ToByteArrayWithLittleEndianLongFixture(val value: Long, val expected: ByteArray)

    @Theory fun toByteArrayWithLittleEndian(fixture: ToByteArrayWithLittleEndianLongFixture) {
        val actual = ConvertUtils.toByteArrayWithLittleEndian(fixture.value)
        assertEquals(fixture.expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(fixture.expected[i], actual[i])
        }
    }



    data class EncodeWithBase32Fixture(val message: String, val expected: String)

    @Theory fun encodeWithBase32(fixture: EncodeWithBase32Fixture) {
        val actual = ConvertUtils.encodeWithBase32(ConvertUtils.toByteArray(fixture.message))
        assertEquals(fixture.expected, actual)
    }

}

