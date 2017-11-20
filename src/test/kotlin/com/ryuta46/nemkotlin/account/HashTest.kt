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

import com.ryuta46.nemkotlin.util.ConvertUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

@RunWith(Theories::class)
class HashTest {
    companion object {
        @DataPoints @JvmStatic fun getSha3_256Fixtures() = arrayOf(
                Sha3_256Fixture("3f9f8c791f4b55c84278c10c7596f959a43a71dc35888d16e3d26a33456b6df2", "400b2df583636fcf57001b96812c799969e51358c0e192015f48a1251fc10704"),
                Sha3_256Fixture("689a556a8b28f1640e52e5546569f03d1367f6bf66", "a2b5586dda65af73d93a2edb06783073b502c0039fa017173e6974b642567493")

        )
        @DataPoints @JvmStatic fun getSha3_512Fixtures() = arrayOf(
                Sha3_512Fixture("0f3928e8aa57f53b0e77c412be66547b2b1ef28eff58e8403c12025d50c66209", "ce36c5c2f558904e89755a989e92f9f49841ca705c8a6b14ee818e82f468945b97a548ca1706f74996e8f9e3f5a08df5fd86d346169b802b3ea01e47cb1a4a00")

        )
        @DataPoints @JvmStatic fun getRipemd_160Fixtures() = arrayOf(
                Ripemd_160Fixture("400b2df583636fcf57001b96812c799969e51358c0e192015f48a1251fc10704", "9a556a8b28f1640e52e5546569f03d1367f6bf66")
        )
    }

    data class Sha3_256Fixture(val message: String, val expected: String)
    data class Sha3_512Fixture(val message: String, val expected: String)
    data class Ripemd_160Fixture(val message: String, val expected: String)

    @Before fun setUp() {
        Hash.initialize()
    }
    @After fun tearDown() {
        //Hash.finalize()
    }

    @Theory fun sha3_256(fixture: Sha3_256Fixture) {
        val actual = ConvertUtils.toHexString(Hash.sha3_256(ConvertUtils.toByteArray(fixture.message)))
        assertEquals(fixture.expected, actual)
    }

    @Theory fun sha3_512(fixture: Sha3_512Fixture) {
        val actual = ConvertUtils.toHexString(Hash.sha3_512(ConvertUtils.toByteArray(fixture.message)))
        assertEquals(fixture.expected, actual)
    }

    @Theory fun ripemd160(fixture: Ripemd_160Fixture) {
        val actual = ConvertUtils.toHexString(Hash.ripemd160(ConvertUtils.toByteArray(fixture.message)))
        assertEquals(fixture.expected, actual)
    }


}
