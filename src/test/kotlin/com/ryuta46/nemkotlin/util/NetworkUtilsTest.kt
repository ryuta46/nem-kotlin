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

import com.ryuta46.nemkotlin.Settings
import junit.framework.TestCase.assertEquals
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theory


class NetworkUtilsTest {
    companion object {
        @DataPoints
        @JvmStatic fun getCreateUrlStringFixtures() = arrayOf(
                CreateUrlStringFixture("", emptyMap(), Settings.TEST_HOST),
                CreateUrlStringFixture("/path", emptyMap(), Settings.TEST_HOST + "/path"),
                CreateUrlStringFixture("/path", mapOf("qu" to "val"), Settings.TEST_HOST + "/path?qu=val"),

                CreateUrlStringFixture("/path", mapOf("k1" to "v1", "k2" to "v2"), Settings.TEST_HOST + "/path?k1=v1&k2=v2")
        )
    }

    data class CreateUrlStringFixture(val path: String, val queries: Map<String, String>, val expected: String)

    @Theory fun createUrlString(fixture: CreateUrlStringFixture) {
        val actual = NetworkUtils.createUrlString(Settings.TEST_HOST, fixture.path, fixture.queries)
        println(actual)
        assertEquals(fixture.expected, actual)
    }
}