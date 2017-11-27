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
import com.ryuta46.nemkotlin.TestUtils.Companion.waitUntilNotNull
import com.ryuta46.nemkotlin.account.AccountGenerator
import com.ryuta46.nemkotlin.enums.Version
import com.ryuta46.nemkotlin.exceptions.NetworkException
import com.ryuta46.nemkotlin.model.*
import com.ryuta46.nemkotlin.transaction.MosaicAttachment
import com.ryuta46.nemkotlin.transaction.TransactionHelper
import com.ryuta46.nemkotlin.util.ConvertUtils
import com.ryuta46.nemkotlin.util.StandardLogger
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import junit.framework.TestCase.*
import org.junit.Test

class RxNemWebSocketClientTest {
    private fun <T>printModel(model: T) {
        //val jsonString = GsonBuilder().setPrettyPrinting().create().toJson(model)
        val jsonString = Gson().toJson(model)
        println(jsonString)
    }


    //private val client = RxNemWebSocketClient("http://bob.nem.ninja:7778", logger = StandardLogger())
    private val client = RxNemWebSocketClient(Settings.TEST_WEB_SOCKET, logger = StandardLogger())
    private val syncClient = NemApiClient(Settings.TEST_HOST, logger = StandardLogger())
    //private val client = RxNemWebSocketClient("http://62.75.251.134:7778", logger = StandardLogger())

    private fun transactionAnnounceMosaic(): String {
        if (Settings.PRIVATE_KEY.isEmpty()) {
            return ""
        }
        val account = AccountGenerator.fromSeed(ConvertUtils.toByteArray(Settings.PRIVATE_KEY), Version.Test)

        val request = TransactionHelper.createMosaicTransferTransaction(
                account,
                Settings.RECEIVER,
                listOf(MosaicAttachment("ttech", "ryuta", 1, 1_000_000,0)),
                Version.Test)

        val result = syncClient.transactionAnnounce(request)

        assertEquals(1, result.type)
        assertEquals(1, result.code)
        assertEquals("SUCCESS", result.message)
        return result.transactionHash.data
    }

    @Test fun accountGet(){
        var result: AccountMetaDataPair? = null
        val subscription = client.accountGet(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .subscribe { result = it }

        waitUntilNotNull { result }
        subscription.dispose()
        assertNotNull(result)
        printModel(result)

    }

    @Test fun recentTransactions(){
        var result: List<TransactionMetaDataPair>? = null
        val subscription = client.recentTransactions(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .subscribe { result = it }

        waitUntilNotNull { result }
        subscription.dispose()
        assertNotNull(result)
        printModel(result)
    }


    @Test fun transactions(){
        var result: TransactionMetaDataPair? = null
        var hash = ""
        var subscribed: Boolean? = null
        val subscription = client.transactions(Settings.ADDRESS) { subscribed = true }
                .subscribeOn(Schedulers.newThread())
                .subscribe { transaction: TransactionMetaDataPair ->
                    if (hash == transaction.meta.hash.data) result = transaction
                }

        waitUntilNotNull { subscribed }

        hash = transactionAnnounceMosaic()
        if (hash.isEmpty()) {
            assertNull(result)
            return
        }

        // the transaction is not confirmed yet.
        assertNull(result)

        waitUntilNotNull(10 * 60 * 1000) { result }
        subscription.dispose()
        printModel(result)
        assertNotNull(result)
    }

    @Test fun unconfirmed() {
        var result: TransactionMetaDataPair? = null
        var hash = ""
        var subscribed: Boolean? = null
        val subscription = client.unconfirmed(Settings.ADDRESS) { subscribed = true }
                .subscribeOn(Schedulers.newThread())
                .subscribe { transaction: TransactionMetaDataPair ->
                    if (hash == transaction.meta.hash.data) result = transaction
                }

        waitUntilNotNull { subscribed }

        hash = transactionAnnounceMosaic()
        if (hash.isEmpty()) {
            assertNull(result)
            return
        }

        waitUntilNotNull { result }
        subscription.dispose()
        assertNotNull(result)
        printModel(result)
    }

    @Test fun accountMosaicOwnedDefinition() {
        var result: MosaicDefinition? = null
        val subscription = client.accountMosaicOwnedDefinition(Settings.RECEIVER)
                .subscribeOn(Schedulers.newThread())
                .subscribe { mosaicDefinition: MosaicDefinition ->
                    result = mosaicDefinition
                }

        waitUntilNotNull { result }
        subscription.dispose()
        assertNotNull(result)
        printModel(result)
    }


    @Test fun accountMosaicOwned(){
        var result: Mosaic? = null
        val subscription = client.accountMosaicOwned(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .subscribe { mosaic: Mosaic ->
                    result = mosaic
                }

        waitUntilNotNull { result }
        subscription.dispose()
        assertNotNull(result)
        printModel(result)

    }
    @Test fun accountNamespaceOwned(){
        var result: Namespace? = null
        val subscription = client.accountNamespaceOwned(Settings.RECEIVER)
                .subscribeOn(Schedulers.newThread())
                .subscribe { namespace: Namespace ->
                    result = namespace
                }

        waitUntilNotNull { result }
        subscription.dispose()
        assertNotNull(result)
        printModel(result)
    }

    @Test fun blocks(){
        var result: Block? = null
        val subscription = client.blocks()
                .subscribeOn(Schedulers.newThread())
                .subscribe { block: Block ->
                    result = block
                }

        waitUntilNotNull(10 * 60 * 1000) { result }
        subscription.dispose()
        assertNotNull(result)
        printModel(result)


    }

    @Test fun blocksNew(){
        var result: BlockHeight? = null
        val subscription = client.blocksNew()
                .subscribeOn(Schedulers.newThread())
                .subscribe { block: BlockHeight ->
                    result = block
                }

        waitUntilNotNull(10 * 60 * 1000) { result }
        subscription.dispose()
        assertNotNull(result)
        printModel(result)
    }

    @Test fun errorNetwork() {
        var result: Throwable? = null
        val errorClient = RxNemWebSocketClient("http://invalidinvalid:7778", logger = StandardLogger())

        val subscription = errorClient.accountGet(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .onErrorResumeNext { e: Throwable ->
                    result = e
                    Observable.empty()
                }
                .subscribe { _: AccountMetaDataPair -> }

        waitUntilNotNull { result }
        subscription.dispose()
        assertNotNull(result)

        assertTrue(result is NetworkException)
    }

    // unconfirmed -> confirmed transaction sequence
    @Test fun transactionSequence() {
        var unconfirmed : TransactionMetaDataPair? = null
        var confirmed : TransactionMetaDataPair? = null
        var hash = ""
        var subscribed: Boolean? = null

        val subscriptionUnconfirmed = client.unconfirmed(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .subscribe { transaction: TransactionMetaDataPair ->
                    if (hash == transaction.meta.hash.data) unconfirmed = transaction
                }

        val subscriptionConfirmed = client.transactions(Settings.ADDRESS) { subscribed = true}
                .subscribeOn(Schedulers.newThread())
                .subscribe { transaction: TransactionMetaDataPair ->
                    if (hash == transaction.meta.hash.data) confirmed = transaction
                }

        waitUntilNotNull { subscribed }

        hash = transactionAnnounceMosaic()
        if (hash.isEmpty()) {
            assertNull(unconfirmed)
            assertNull(confirmed)
            return
        }

        // check unconfirmed transaction
        waitUntilNotNull { unconfirmed }

        // the confirmed transaction is not confirmed yet.
        assertNull(confirmed)

        // check confirmed transaction
        waitUntilNotNull(10 * 60 * 1000) { confirmed }
        subscriptionUnconfirmed.dispose()
        subscriptionConfirmed.dispose()

        Thread.sleep(10 * 60)
        printModel(unconfirmed)
        printModel(confirmed)
        assertNotNull(confirmed)
    }

    @Test fun reusingSubscriptionId() {
        // ID 0 accountGet
        var account: AccountMetaDataPair? = null
        var namespace: Namespace? = null
        val subscriptionAccount = client.accountGet(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .subscribe { account = it }

        // ID 1 namespace
        val subscriptionNamespace = client.accountNamespaceOwned(Settings.RECEIVER)
                .subscribeOn(Schedulers.newThread())
                .subscribe { namespace = it }

        // Wait
        waitUntilNotNull { account }
        waitUntilNotNull { namespace }

        assertNotNull(account)
        assertNotNull(namespace)

        // Release ID 1
        subscriptionNamespace.dispose()

        // Reuse ID 1 as Mosaic
        var mosaic: Mosaic? = null
        val subscriptionMosaic = client.accountMosaicOwned(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .subscribe { mosaic = it }


        waitUntilNotNull { mosaic }
        subscriptionMosaic.dispose()
        subscriptionAccount.dispose()

        assertNotNull(mosaic)

        printModel(account)
        printModel(namespace)
        printModel(mosaic)
    }

}

