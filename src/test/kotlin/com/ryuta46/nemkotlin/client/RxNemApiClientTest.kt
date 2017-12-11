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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ryuta46.nemkotlin.Settings
import com.ryuta46.nemkotlin.TestUtils
import com.ryuta46.nemkotlin.TestUtils.Companion.waitUntilNotNull
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
        //val jsonString = GsonBuilder().setPrettyPrinting().create().toJson(model)
        val jsonString = Gson().toJson(model)
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

    @Test fun heartbeat() {
        val result = client.heartbeat().blockingFirst()
        assertEquals(1, result.code)
        assertEquals(2, result.type)
        assertEquals("ok", result.message)
        printModel(result)
    }

    @Test fun status() {
        val result = client.status().blockingFirst()
        assertEquals(6, result.code)
        assertEquals(4, result.type)
        assertEquals("status", result.message)
        printModel(result)
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


    @Test fun accountGetForwarded() {
        // API Document sample request.
        val result = mainClient.accountGetForwarded("NC2ZQKEFQIL3JZEOB2OZPWXWPOR6LKYHIROCR7PK").blockingFirst()
        printModel(result)
        assertEquals("NALICE2A73DLYTP4365GNFCURAUP3XVBFO7YNYOW", result.account.address)
    }
    @Test fun accountGetForwardedMyself() {
        val result = client.accountGetForwarded(Settings.ADDRESS).blockingFirst()
        printModel(result)
        assertEquals(result.account.address, Settings.ADDRESS)
    }

    @Test fun accountGetForwardedFromPublicKey() {
        val result = mainClient.accountGetForwardedFromPublicKey("bdd8dd702acb3d88daf188be8d6d9c54b3a29a32561a068b25d2261b2b2b7f02").blockingFirst()
        printModel(result)
        assertEquals("NALICE2A73DLYTP4365GNFCURAUP3XVBFO7YNYOW", result.account.address)
    }

    @Test fun accountGetForwardedFromPublicKeyMyself() {
        val result = client.accountGetForwardedFromPublicKey(Settings.PUBLIC_KEY).blockingFirst()
        printModel(result)
        assertEquals(result.account.address, Settings.ADDRESS)
    }

    @Test fun accountStatus() {
        val result = client.accountStatus(Settings.ADDRESS).blockingFirst()
        printModel(result)

        assertEquals("LOCKED", result.status)
        assertEquals("INACTIVE", result.remoteStatus)
        assertTrue(result.cosignatoryOf.isNotEmpty())
        assertTrue(result.cosignatories.isEmpty())
    }

    @Test fun accountTransfersIncoming() {
        val result = client.accountTransfersIncoming(Settings.ADDRESS).blockingFirst()
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            val transfer = it.transaction.asTransfer ?: it.transaction.asMultisig?.otherTrans?.asTransfer ?: return@forEach
            assertEquals(Settings.ADDRESS, transfer.recipient)
        }
    }

    @Test fun accountTransfersOutgoing() {
        val result = client.accountTransfersOutgoing(Settings.ADDRESS).blockingFirst()
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            assertEquals(Settings.PUBLIC_KEY, it.transaction.signer)
        }
    }

    @Test fun accountTransfersAll() {
        val result = client.accountTransfersAll(Settings.ADDRESS).blockingFirst()
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            val transfer = it.transaction.asTransfer ?: it.transaction.asMultisig?.otherTrans?.asTransfer ?: return@forEach
            assertTrue(Settings.PUBLIC_KEY == transfer.signer || Settings.ADDRESS == transfer.recipient)
        }

    }

    @Test fun accountUnconfirmedTransactions() {
        transactionAnnounce(getTransferAnnounceFixture()[2])
        val result = client.accountUnconfirmedTransactions(Settings.ADDRESS).blockingFirst()
        printModel(result)

        if (Settings.PRIVATE_KEY.isEmpty()) {
            assertTrue(result.isEmpty())
        } else {
            assertTrue(result.isNotEmpty())
            result.forEach {
                assertEquals(Settings.PUBLIC_KEY, it.transaction.signer)

            }
        }
    }

    @Test fun accountHarvests() {
        val result = client.accountHarvests(Settings.ADDRESS).blockingFirst()
        printModel(result)
        assertTrue(result.isEmpty())
    }


    @Test fun accountImportances() {
        val result = client.accountImportances().blockingFirst()
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            assertTrue(it.address.isNotEmpty())
        }
    }


    @Test fun accountNamespacePage() {
        val result = client.accountNamespacePage(Settings.RECEIVER).blockingFirst()
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            assertEquals(Settings.RECEIVER, it.owner)
        }
    }

    @Test fun accountNamespacePageInvalidParent() {
        val result = client.accountNamespacePage(Settings.RECEIVER, "ttech").blockingFirst()
        printModel(result)
        assertTrue(result.isEmpty())
    }


    @Test fun accountMosaicOwned() {
        val mosaicArray = client.accountMosaicOwned(Settings.ADDRESS).blockingFirst()
        printModel(mosaicArray)
        assertTrue(mosaicArray.isNotEmpty())
    }

    // Account historiacal get does not support in the NIS.
    @Test fun accountHistoricalGet() {
        var err: Throwable? = null
        val result = client.accountHistoricalGet(Settings.ADDRESS, 0, 0,1)
                .onErrorResumeNext{e: Throwable ->
                    err = e
                    Observable.empty()
                }
                .subscribe{}

        waitUntilNotNull { err }

        assertTrue(err is NetworkException)

        printModel(result)
    }

    @Test fun namespaceMosaicDefinitionPage(){
        val mosaicDefinitionArray = mainClient.namespaceMosaicDefinitionPage("ttech").blockingFirst()
        printModel(mosaicDefinitionArray)
        assertTrue(mosaicDefinitionArray.isNotEmpty())
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