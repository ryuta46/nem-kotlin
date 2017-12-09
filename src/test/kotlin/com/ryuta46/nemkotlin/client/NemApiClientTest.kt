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
        //val jsonString = GsonBuilder().setPrettyPrinting().create().toJson(model)
        val jsonString = Gson().toJson(model)
        println(jsonString)
    }

    @Test fun get() {
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

    @Test fun heartbeat() {
        val result = client.heartbeat()
        assertEquals(1, result.code)
        assertEquals(2, result.type)
        assertEquals("ok", result.message)
        printModel(result)
    }

    @Test fun status() {
        val result = client.status()
        assertEquals(6, result.code)
        assertEquals(4, result.type)
        assertEquals("status", result.message)
        printModel(result)
    }

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


    @Test fun accountGetForwarded() {
        // API Document sample request.
        val result = mainClient.accountGetForwarded("NC2ZQKEFQIL3JZEOB2OZPWXWPOR6LKYHIROCR7PK")
        printModel(result)
        assertEquals("NALICE2A73DLYTP4365GNFCURAUP3XVBFO7YNYOW", result.account.address)
    }
    @Test fun accountGetForwardedMyself() {
        val result = client.accountGetForwarded(Settings.ADDRESS)
        printModel(result)
        assertEquals(result.account.address, Settings.ADDRESS)
    }

    @Test fun accountGetForwardedFromPublicKey() {
        val result = mainClient.accountGetForwardedFromPublicKey("bdd8dd702acb3d88daf188be8d6d9c54b3a29a32561a068b25d2261b2b2b7f02")
        printModel(result)
        assertEquals("NALICE2A73DLYTP4365GNFCURAUP3XVBFO7YNYOW", result.account.address)
    }

    @Test fun accountGetForwardedFromPublicKeyMyself() {
        val result = client.accountGetForwardedFromPublicKey(Settings.PUBLIC_KEY)
        printModel(result)
        assertEquals(result.account.address, Settings.ADDRESS)
    }

    @Test fun accountStatus() {
        val result = client.accountStatus(Settings.ADDRESS)
        printModel(result)

        assertEquals("LOCKED", result.status)
        assertEquals("INACTIVE", result.remoteStatus)
        assertTrue(result.cosignatoryOf.isEmpty())
        assertTrue(result.cosignatories.isEmpty())
    }

    @Test fun accountTransfersIncoming() {
        val result = client.accountTransfersIncoming(Settings.ADDRESS)
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            assertEquals(Settings.ADDRESS, it.transaction.recipient)
        }
    }

    @Test fun accountTransfersOutgoing() {
        val result = client.accountTransfersOutgoing(Settings.ADDRESS)
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            assertEquals(Settings.PUBLIC_KEY, it.transaction.signer)
        }
    }

    @Test fun accountTransfersAll() {
        val result = client.accountTransfersAll(Settings.ADDRESS)
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            val transfer = it.transaction.asTransfer ?: it.transaction.asMutisig?.otherTrans?.asTransfer ?: return@forEach
            assertTrue(Settings.PUBLIC_KEY == transfer.signer || Settings.ADDRESS == transfer.recipient)
        }
    }

    @Test fun accountUnconfirmedTransactions() {
        transactionAnnounce(getTransferAnnounceFixture()[2])
        val result = client.accountUnconfirmedTransactions(Settings.ADDRESS)
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
        val result = client.accountHarvests(Settings.ADDRESS)
        printModel(result)
        assertTrue(result.isEmpty())
    }


    @Test fun accountImportances() {
        val result = client.accountImportances()
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            assertTrue(it.address.isNotEmpty())
        }
    }


    @Test fun accountNamespacePage() {
        val result = client.accountNamespacePage(Settings.RECEIVER)
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            assertEquals(Settings.RECEIVER, it.owner)
        }
    }

    @Test fun accountNamespacePageInvalidParent() {
        val result = client.accountNamespacePage(Settings.RECEIVER, "ttech")
        printModel(result)
        assertTrue(result.isEmpty())
    }

    @Test fun accountMosaicOwned() {
        val mosaicArray = client.accountMosaicOwned(Settings.ADDRESS)
        printModel(mosaicArray)
        assertTrue(mosaicArray.isNotEmpty())
    }

    // Account historiacal get does not support in the NIS.
    @Test(expected = NetworkException::class)
    fun accountHistoricalGet() {
        val result = client.accountHistoricalGet(Settings.ADDRESS, 0, 0,1)
        printModel(result)
    }

    @Test fun namespaceMosaicDefinitionPage(){
        val mosaicDefinitionArray = mainClient.namespaceMosaicDefinitionPage("ttech")
        printModel(mosaicDefinitionArray)
        assertTrue(mosaicDefinitionArray.isNotEmpty())
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