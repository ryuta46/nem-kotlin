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

import com.ryuta46.nemkotlin.TestUtils.Companion.checkResult
import com.ryuta46.nemkotlin.TestUtils.Companion.printModel
import com.ryuta46.nemkotlin.client.NemApiClient
import org.junit.Test

class NisUtilsTest {
    @Test fun getSuperNodes() {
        val actual = NisUtils.getSuperNodes(logger = StandardLogger()).blockingGet()
        printModel(actual)

        val clients = actual.map { NemApiClient("http://${it.ip}:${it.nisPort}", logger = StandardLogger()) }

        // check top 10
        (0..10).forEach {
            val result = clients[it].heartbeat()
            checkResult(result)
        }
    }

    @Test fun getTestNodes() {
        val actual = NisUtils.getTestNodes()
        printModel(actual)

        val clients = actual.map { NemApiClient("http://${it.ip}:${it.nisPort}", logger = StandardLogger()) }

        // check all host
        clients.map { it.heartbeat() }.forEach { checkResult(it) }
    }
}