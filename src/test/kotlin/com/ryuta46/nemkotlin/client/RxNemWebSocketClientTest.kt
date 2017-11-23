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
import com.ryuta46.nemkotlin.model.*
import com.ryuta46.nemkotlin.util.StandardLogger
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

import org.junit.Test

class RxNemWebSocketClientTest {
    private fun <T>printModel(model: T) {
        val jsonString = GsonBuilder().setPrettyPrinting().create().toJson(model)
        println(jsonString)
    }


    //private val client = RxNemWebSocketClient("http://bob.nem.ninja:7778", logger = StandardLogger())
    private val client = RxNemWebSocketClient("http://23.228.67.85:7778", logger = StandardLogger())
    //private val client = RxNemWebSocketClient("http://62.75.251.134:7778", logger = StandardLogger())

    @Test fun accountGet(){
        client.accountGet(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .subscribe { account: AccountMetaDataPair ->
                    printModel(account)
                }


    }

    @Test fun recentTransactions(){
        client.recentTransactions(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .subscribe { transactions: TransactionMetaDataPairArray ->
                    printModel(transactions)
                }


    }


    @Test fun transactions(){
        client.transactions(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .subscribe { transaction: TransactionMetaDataPair ->
                    printModel(transaction)
                }


    }

    @Test fun unconfirmed() {
        client.unconfirmed(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .subscribe { transaction: TransactionMetaDataPair ->
                    printModel(transaction)
                }


    }

    @Test fun accountMosaicOwnedDefinition() {
        client.accountMosaicOwnedDefinition(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .subscribe { mosaicDefinition: MosaicDefinition ->
                    printModel(mosaicDefinition)
                }
    }


    @Test fun accountMosaicOwned(){
        client.accountMosaicOwned(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .subscribe { mosaic: Mosaic ->
                    printModel(mosaic)
                }

    }
    @Test fun accountNamespaceOwned(){
        client.accountNamespaceOwned(Settings.ADDRESS)
                .subscribeOn(Schedulers.newThread())
                .subscribe { namespace: Namespace ->
                    printModel(namespace)
                }

    }

    @Test fun blocks(){
        val subscription = client.blocks()
                .subscribeOn(Schedulers.newThread())
                .subscribe { block: Block ->
                    printModel(block)
                }

        //Thread.sleep(30 * 60 * 1000)
        //Thread.sleep(5 * 60 * 1000)

        subscription.dispose()
    }

    @Test fun blocksNew(){
        val subscription = client.blocksNew()
                .subscribeOn(Schedulers.newThread())
                .subscribe { block: BlockHeight ->
                    printModel(block)
                }

        //Thread.sleep(30 * 60 * 1000)
        //Thread.sleep(5 * 60 * 1000)

        subscription.dispose()
    }

}

