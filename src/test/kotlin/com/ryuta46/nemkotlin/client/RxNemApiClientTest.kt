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
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.*
import org.junit.Test

class RxNemApiClientTest {
    companion object {
        val TEST_HOST = "http://62.75.251.134:7890"
        val TEST_ADDRESS = "NBERYPGLBLMJJYMTMNFSY3UDMXTLZFUNP4CQG7SO"
        val TEST_PUBLIC_KEY = "1e68cbd764e54655dd5ebadcdc28ef68409a87ab3deaf56fa9cb8e46145b5872"

    }

    private val client: RxNemApiClient
        get() = RxNemApiClient(TEST_HOST, logger = StandardLogger())


    private fun <T>printModel(model: T) {
        val jsonString = GsonBuilder().setPrettyPrinting().create().toJson(model)
        print(jsonString)
    }


    @Test
    fun get() {
        var result: AccountMetaDataPair? = null

        val observable = client.get<AccountMetaDataPair>("/account/get",mapOf("address" to TEST_ADDRESS))

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
        assertEquals(result!!.account.publicKey, TEST_PUBLIC_KEY)
        assertEquals(result!!.account.address, TEST_ADDRESS)
        printModel(result!!)
    }

    @Test
    fun errorOnNetwork() {
        val client = RxNemApiClient("http://illegalhostillegalhostillegalhost", logger = StandardLogger())
        var result: AccountMetaDataPair? = null

        val observable = client.get<AccountMetaDataPair>("/account/get",mapOf("address" to TEST_ADDRESS))

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


    @Test
    fun accountGetFromPublicKey() {
        val client = RxNemApiClient(TEST_HOST)

        val result = client.accountGetFromPublicKey(TEST_PUBLIC_KEY).subscribeOn(Schedulers.newThread()).blockingFirst()

        assertEquals(result.account.publicKey, TEST_PUBLIC_KEY)
        assertEquals(result.account.address, TEST_ADDRESS)
        printModel(result)
    }
}