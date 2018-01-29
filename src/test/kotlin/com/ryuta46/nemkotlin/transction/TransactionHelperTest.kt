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
package com.ryuta46.nemkotlin.transction

import com.ryuta46.nemkotlin.transaction.MosaicAttachment
import com.ryuta46.nemkotlin.transaction.TransactionHelper
import junit.framework.TestCase.assertEquals
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

@RunWith(Theories::class)
class TransactionHelperTest {
    companion object {
        @DataPoints @JvmStatic fun getCalculateXemTransferFeeFixtures() = arrayOf(
                CalculateXemTransferFeeFixture(0L,50_000L),
                CalculateXemTransferFeeFixture(10_000_000_000L,50_000L),
                CalculateXemTransferFeeFixture(19_999_999_999L,50_000L),
                CalculateXemTransferFeeFixture(20_000_000_000L,100_000L),
                CalculateXemTransferFeeFixture(249_999_999_999L,1_200_000L),
                CalculateXemTransferFeeFixture(250_000_000_000L,1_250_000L),
                CalculateXemTransferFeeFixture(250_000_000_001L,1_250_000L),
                CalculateXemTransferFeeFixture(1_000_000_000_000L,1_250_000L)
        )

        @DataPoints @JvmStatic fun getCalculateMessageTransferFeeFixtures() = arrayOf(
                CalculateMessageTransferFeeFixture("",0L),
                CalculateMessageTransferFeeFixture("1234567890123456789012345678901",50_000L),
                CalculateMessageTransferFeeFixture("12345678901234567890123456789012",100_000L),
                CalculateMessageTransferFeeFixture("123456789012345678901234567890123456789012345678901234567890123",100_000L)
        )

        @DataPoints @JvmStatic fun getCalculateMosaicTransferFeeFixtures() = arrayOf(
                CalculateMosaicTransferFeeFixture(MosaicAttachment("ttech", "ryuta", 1, 1_000, 0), 50_000),
                CalculateMosaicTransferFeeFixture(MosaicAttachment("ttech", "ryuta", 1, 1_000_000, 0), 50_000),
                CalculateMosaicTransferFeeFixture(MosaicAttachment("ttech", "ryuta", 23, 1_000_000, 0), 100_000),
                CalculateMosaicTransferFeeFixture(MosaicAttachment("ttech", "ryuta", 24, 1_000_000, 0), 150_000),
                CalculateMosaicTransferFeeFixture(MosaicAttachment("ttech", "ryuta", 25, 1_000_000, 0), 200_000),
                CalculateMosaicTransferFeeFixture(MosaicAttachment("ttech", "ryuta", 28, 1_000_000, 0), 350_000),
                CalculateMosaicTransferFeeFixture(MosaicAttachment("ttech", "ryuta", 29, 1_000_000, 0), 350_000)
        )
    }

    data class CalculateXemTransferFeeFixture(val microNem: Long, val expected: Long)

    @Theory fun calculateXemTransferFee(fixture: CalculateXemTransferFeeFixture) {
        assertEquals(fixture.expected, TransactionHelper.calculateXemTransferFee(fixture.microNem))
    }

    data class CalculateMessageTransferFeeFixture(val message: String, val expected: Long)

    @Theory fun calculateMessageTransferFee(fixture: CalculateMessageTransferFeeFixture) {
        assertEquals(fixture.expected, TransactionHelper.calculateMessageTransferFee(fixture.message.toByteArray(Charsets.UTF_8)))
    }

    data class CalculateMosaicTransferFeeFixture(val mosaicAttachment: MosaicAttachment, val expected: Long)

    @Theory fun calculateMosaicTransferFee(fixture: CalculateMosaicTransferFeeFixture) {
        assertEquals(fixture.expected, TransactionHelper.calculateMosaicTransferFee(fixture.mosaicAttachment))
    }

}