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
import com.ryuta46.nemkotlin.Settings
import com.ryuta46.nemkotlin.account.AccountGenerator
import com.ryuta46.nemkotlin.enums.Version
import com.ryuta46.nemkotlin.exceptions.NetworkException
import com.ryuta46.nemkotlin.model.AccountMetaDataPair
import com.ryuta46.nemkotlin.transaction.MosaicAttachment
import com.ryuta46.nemkotlin.transaction.TransactionHelper
import com.ryuta46.nemkotlin.util.ConvertUtils
import com.ryuta46.nemkotlin.util.StandardLogger
import junit.framework.TestCase.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith


@RunWith(Theories::class)
class NemApiClientTest {
    companion object {
        @DataPoints @JvmStatic fun getCreateUrlStringFixtures() = arrayOf(
                CreateUrlStringFixture("", emptyMap(), Settings.TEST_HOST),
                CreateUrlStringFixture("/path", emptyMap(), Settings.TEST_HOST + "/path"),
                CreateUrlStringFixture("/path", mapOf("qu" to "val"), Settings.TEST_HOST + "/path?qu=val"),

                CreateUrlStringFixture("/path", mapOf("k1" to "v1", "k2" to "v2"), Settings.TEST_HOST + "/path?k1=v1&k2=v2")
        )

        @DataPoints @JvmStatic fun getTransferAnnounceFixture() = arrayOf(
                TransferAnnounceFixture(1, "", emptyList()),
                TransferAnnounceFixture(0, "test", emptyList()),
                TransferAnnounceFixture(0, "", listOf(MosaicAttachment("nem", "xem", 1, 8_999_999_999L, 6)))
        )
    }

    private val client: NemApiClient
        get() = NemApiClient(Settings.TEST_HOST, logger = StandardLogger())

    private val mainClient: NemApiClient
        get() = NemApiClient(Settings.MAIN_HOST, logger = StandardLogger())


    private fun <T>printModel(model: T) {
        val jsonString = GsonBuilder().setPrettyPrinting().create().toJson(model)
        println(jsonString)
    }

    data class CreateUrlStringFixture(val path: String, val queries: Map<String, String>, val expected: String)

    @Theory
    fun createUrlString(fixture: CreateUrlStringFixture) {
        val actual = client.createUrlString(fixture.path, fixture.queries)
        println(actual)
        assertEquals(fixture.expected, actual)
    }


    @Test
    fun get() {
        val accountMetaDataPair: AccountMetaDataPair = client.get("/account/get",mapOf("address" to Settings.ADDRESS))

        assertEquals(accountMetaDataPair.account.publicKey, Settings.PUBLIC_KEY)
        assertEquals(accountMetaDataPair.account.address, Settings.ADDRESS)

        printModel(accountMetaDataPair)
    }

    @Test(expected = NetworkException::class)
    fun errorOnNetwork(){
        val client = NemApiClient("http://illegalhostillegalhostillegalhost", logger = StandardLogger())
        client.get<AccountMetaDataPair>("/account/get",mapOf("address" to Settings.ADDRESS))
    }

    /*
    @Test(expected = ParseException::class)
    fun errorOnParseResponse(){
        val client = NemApiClient(TEST_HOST)
        client.get<AccountInfo>("/account/get",mapOf("address" to TEST_ADDRESS))
    }
    */

    @Test fun accountGet() {
        val accountMetaDataPair = client.accountGet(Settings.ADDRESS)
        printModel(accountMetaDataPair)

        assertEquals(accountMetaDataPair.account.publicKey, Settings.PUBLIC_KEY)
        assertEquals(accountMetaDataPair.account.address, Settings.ADDRESS)
    }

    @Test fun accountGetFromPublicKey() {
        val accountMetaDataPair = client.accountGetFromPublicKey(Settings.PUBLIC_KEY)
        printModel(accountMetaDataPair)

        assertEquals(accountMetaDataPair.account.publicKey, Settings.PUBLIC_KEY)
        assertEquals(accountMetaDataPair.account.address, Settings.ADDRESS)
    }

    @Test fun accountMosaicOwned() {
        val mosaicArray = client.accountMosaicOwned(Settings.ADDRESS)
        printModel(mosaicArray)
        assertTrue(mosaicArray.data.isNotEmpty())
    }

    @Test fun namespaceMosaicDefinitionPage(){
        val mosaicDefinitionArray = mainClient.namespaceMosaicDefinitionPage("ttech")
        printModel(mosaicDefinitionArray)
        assertTrue(mosaicDefinitionArray.data.isNotEmpty())
    }

    @Test fun namespaceMosaicDefinitionFromName(){
        val mosaicDefinition = mainClient.namespaceMosaicDefinitionFromName("ttech", "ryuta")
        assertNotNull(mosaicDefinition)
        printModel(mosaicDefinition)
    }

    @Test fun namespaceMosaicDefinitionFromNameNull(){
        val mosaicDefinition = mainClient.namespaceMosaicDefinitionFromName("ttech", "ryutainvalidinvalid")
        assertNull(mosaicDefinition)
    }


    data class TransferAnnounceFixture(val xem: Long, val message: String, val mosaics:List<MosaicAttachment>)

    @Theory fun transactionAnnounce(fixture: TransferAnnounceFixture) {
        val account =
                if (Settings.PRIVATE_KEY.isNotEmpty()) AccountGenerator.fromSeed(ConvertUtils.toByteArray(Settings.PRIVATE_KEY), Version.Test)
                else AccountGenerator.fromRandomSeed(Version.Test)


        val request = when {
            fixture.mosaics.isNotEmpty() -> TransactionHelper.createMosaicTransferTransaction(account, Settings.RECEIVER, fixture.mosaics, Version.Test, fixture.message)
            else -> TransactionHelper.createXemTransferTransaction(account, Settings.RECEIVER, 1, Version.Test, fixture.message)
        }

        val result = client.transactionAnnounce(request)
        printModel(result)

        if (Settings.PRIVATE_KEY.isNotEmpty()) {
            assertEquals(1, result.type)
            assertEquals(1, result.code)
            assertEquals("SUCCESS", result.message)
        } else {
            assertEquals(1, result.type)
            assertEquals(5, result.code)
            assertEquals("FAILURE_INSUFFICIENT_BALANCE", result.message)
        }
    }

}