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
package com.ryuta46.nemkotlin.client

import com.google.gson.GsonBuilder
import com.ryuta46.nemkotlin.exceptions.NetworkException
import com.ryuta46.nemkotlin.model.AccountMetaDataPair
import com.ryuta46.nemkotlin.util.StandardLogger
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith


@RunWith(Theories::class)
class NemApiClientTest {
    companion object {
        @JvmField
        val TEST_HOST = "http://62.75.251.134:7890"
        val TEST_ADDRESS = "NBERYPGLBLMJJYMTMNFSY3UDMXTLZFUNP4CQG7SO"
        val TEST_PUBLIC_KEY = "1e68cbd764e54655dd5ebadcdc28ef68409a87ab3deaf56fa9cb8e46145b5872"

        @DataPoints
        @JvmStatic
        fun getCreateUrlStringFixtures() = arrayOf(
                CreateUrlStringFixture("", emptyMap(), TEST_HOST),
                CreateUrlStringFixture("/path", emptyMap(), TEST_HOST + "/path"),
                CreateUrlStringFixture("/path", mapOf("qu" to "val"), TEST_HOST + "/path?qu=val"),
                CreateUrlStringFixture("/path", mapOf("k1" to "v1", "k2" to "v2"), TEST_HOST + "/path?k1=v1&k2=v2")
        )
    }

    private val client: NemApiClient
        get() = NemApiClient(TEST_HOST, logger = StandardLogger())


    private fun <T>printModel(model: T) {
        val jsonString = GsonBuilder().setPrettyPrinting().create().toJson(model)
        print(jsonString)
    }

    data class CreateUrlStringFixture(val path: String, val queries: Map<String, String>, val expected: String)

    @Theory
    fun createUrlString(fixture: CreateUrlStringFixture) {
        val real = client.createUrlString(fixture.path, fixture.queries)
        println(real)
        assertEquals(fixture.expected, real)
    }


    @Test
    fun get() {
        val accountMetaDataPair: AccountMetaDataPair = client.get("/account/get",mapOf("address" to TEST_ADDRESS))

        assertEquals(accountMetaDataPair.account.publicKey, TEST_PUBLIC_KEY)
        assertEquals(accountMetaDataPair.account.address, TEST_ADDRESS)

        printModel(accountMetaDataPair)
    }

    @Test(expected = NetworkException::class)
    fun errorOnNetwork(){
        val client = NemApiClient("http://illegalhostillegalhostillegalhost", logger = StandardLogger())
        client.get<AccountMetaDataPair>("/account/get",mapOf("address" to TEST_ADDRESS))
    }

    /*
    @Test(expected = ParseException::class)
    fun errorOnParseResponse(){
        val client = NemApiClient(TEST_HOST)
        client.get<AccountInfo>("/account/get",mapOf("address" to TEST_ADDRESS))
    }
    */

    @Test
    fun accountGetFromPublicKey() {
        val accountMetaDataPair = client.accountGetFromPublicKey(TEST_PUBLIC_KEY)

        assertEquals(accountMetaDataPair.account.publicKey, TEST_PUBLIC_KEY)
        assertEquals(accountMetaDataPair.account.address, TEST_ADDRESS)

        printModel(accountMetaDataPair)
    }
}