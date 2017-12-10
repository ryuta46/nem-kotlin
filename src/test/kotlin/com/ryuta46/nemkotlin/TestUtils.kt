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

package com.ryuta46.nemkotlin

import com.ryuta46.nemkotlin.model.NemAnnounceResult
import com.ryuta46.nemkotlin.model.NemRequestResult
import junit.framework.TestCase.assertEquals

class TestUtils {
    companion object {
        @JvmStatic fun <T>waitUntilNotNull(timeout: Int = 30 * 1000, body: () -> T?): T? {
            for(i in 0 until timeout / 10) {
                body()?.let {
                   return it
                }
                Thread.sleep(10)
            }
            return null
        }
        @JvmStatic fun waitUntil(timeout: Int = 30 * 1000, body: () -> Boolean) {
            for(i in 0 until timeout / 10) {
                if (body()) return
                Thread.sleep(10)
            }
        }

        @JvmStatic fun checkResult(result: NemAnnounceResult) {
            assertEquals(1, result.type)
            assertEquals(1, result.code)
            assertEquals("SUCCESS", result.message)
        }

        @JvmStatic fun checkResultIsInsufficientBalance(result: NemAnnounceResult) {
            assertEquals(1, result.type)
            assertEquals(5, result.code)
            assertEquals("FAILURE_INSUFFICIENT_BALANCE", result.message)
        }

        @JvmStatic fun checkResultIsMultisigNotACosigner(result: NemAnnounceResult) {
            assertEquals(1, result.type)
            assertEquals(71, result.code)
            assertEquals("FAILURE_MULTISIG_NOT_A_COSIGNER", result.message)
        }
    }
}