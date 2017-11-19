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
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.*
import org.junit.Test
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theory

class RxNemApiClientTest {
    companion object {

        @DataPoints @JvmStatic fun getTransferAnnounceFixture() = arrayOf(
                TransferAnnounceFixture(1, "", emptyList()),
                TransferAnnounceFixture(0, "test", emptyList()),
                TransferAnnounceFixture(0, "", listOf(MosaicAttachment("nem", "xem", 1, 8_999_999_999L, 6)))
        )
    }

    private val client: RxNemApiClient
        get() = RxNemApiClient(Settings.TEST_HOST, logger = StandardLogger())

    private val mainClient: RxNemApiClient
        get() = RxNemApiClient(Settings.MAIN_HOST, logger = StandardLogger())


    private fun <T>printModel(model: T) {
        val jsonString = GsonBuilder().setPrettyPrinting().create().toJson(model)
        println(jsonString)
    }

    @Test
    fun get() {
        var result: AccountMetaDataPair? = null

        val observable = client.get<AccountMetaDataPair>("/account/get",mapOf("address" to Settings.ADDRESS))

        observable.subscribeOn(Schedulers.newThread())
                .onErrorResumeNext{ _: Throwable ->
                    Observable.empty<AccountMetaDataPair>()
                }
                .subscribe { response: AccountMetaDataPair ->
                    result = response
                }

        assertNull(result)

        result = observable.blockingFirst()

        assertNotNull(result)
        assertEquals(result!!.account.publicKey, Settings.PUBLIC_KEY)
        assertEquals(result!!.account.address, Settings.ADDRESS)
        printModel(result!!)
    }

    @Test
    fun errorOnNetwork() {
        val client = RxNemApiClient("http://illegalhostillegalhostillegalhost", logger = StandardLogger())
        var result: AccountMetaDataPair? = null

        val observable = client.get<AccountMetaDataPair>("/account/get",mapOf("address" to Settings.ADDRESS))

        val errors = mutableListOf<Throwable>()

        observable.subscribeOn(Schedulers.newThread())
                .onErrorResumeNext{ e: Throwable ->
                    errors.add(e)
                    Observable.empty<AccountMetaDataPair>()
                }
                .subscribe { response: AccountMetaDataPair ->
                    result = response
                }

        assertNull(result)

        Thread.sleep(5000)

        assertNull(result)
        assertEquals(1, errors.size)

        assertEquals(NetworkException::class, errors[0]::class)
    }


    @Test fun accountGet() {
        val accountMetaDataPair = client.accountGet(Settings.ADDRESS).blockingFirst()
        printModel(accountMetaDataPair)

        assertEquals(accountMetaDataPair.account.publicKey, Settings.PUBLIC_KEY)
        assertEquals(accountMetaDataPair.account.address, Settings.ADDRESS)
    }

    @Test
    fun accountGetFromPublicKey() {
        val result = client.accountGetFromPublicKey(Settings.PUBLIC_KEY).blockingFirst()
        printModel(result)

        assertEquals(result.account.publicKey, Settings.PUBLIC_KEY)
        assertEquals(result.account.address, Settings.ADDRESS)
    }


    @Test fun accountMosaicOwned() {
        val mosaicArray = client.accountMosaicOwned(Settings.ADDRESS).blockingFirst()
        printModel(mosaicArray)
        assertTrue(mosaicArray.data.isNotEmpty())
    }


    @Test fun namespaceMosaicDefinitionPage(){
        val mosaicDefinitionArray = mainClient.namespaceMosaicDefinitionPage("ttech").blockingFirst()
        printModel(mosaicDefinitionArray)
        assertTrue(mosaicDefinitionArray.data.isNotEmpty())
    }

    @Test fun namespaceMosaicDefinitionFromName(){
        val mosaicDefinition = mainClient.namespaceMosaicDefinitionFromName("ttech", "ryuta").blockingFirst()
        assertNotNull(mosaicDefinition)
        printModel(mosaicDefinition)
    }

    @Test(expected = NoSuchElementException::class)
    fun namespaceMosaicDefinitionFromNameNull(){
        mainClient.namespaceMosaicDefinitionFromName("ttech", "ryutainvalidinvalid").blockingFirst()
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
        val result = client.transactionAnnounce(request).blockingFirst()
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