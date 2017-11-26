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
import com.google.gson.JsonParseException
import com.ryuta46.nemkotlin.exceptions.NetworkException
import com.ryuta46.nemkotlin.exceptions.ParseException
import com.ryuta46.nemkotlin.model.*
import com.ryuta46.nemkotlin.net.HttpClient
import com.ryuta46.nemkotlin.net.HttpRequest
import com.ryuta46.nemkotlin.net.HttpResponse
import com.ryuta46.nemkotlin.net.HttpURLConnectionClient
import com.ryuta46.nemkotlin.util.LogWrapper
import com.ryuta46.nemkotlin.util.Logger
import com.ryuta46.nemkotlin.util.NetworkUtils
import com.ryuta46.nemkotlin.util.NoOutputLogger
import java.net.HttpURLConnection
import java.net.URI

/**
 * NEM API client to NIS.
 * This class execute each method synchronously.
 * You should NOT call these api methods in main thread.
 * @property hostUrl NIS host URL.
 * @property httpClient HTTP client to communicate with NIS.
 * @property logger Logging function.
 */
class NemApiClient(val hostUrl: String,
                   val httpClient: HttpClient = HttpURLConnectionClient(),
                   private val logger: Logger = NoOutputLogger()) {

    val logWrapper = LogWrapper(logger, this::class.java.simpleName)

    /**
     * Requests with GET method.
     * This is general way to communicate with NIS if there is no method corresponding to the api you want to use.
     *
     * @param path API path started with "/".
     * @param queries Request queries consisted of key and value.
     * @return data model corresponding to the API response.
     */
    inline fun <reified T : Any>get(path: String, queries: Map<String, String> = emptyMap()): T {
        val uri = URI(NetworkUtils.createUrlString(hostUrl, path, queries))

        logWrapper.i("get request url = $uri")
        val request = HttpRequest(
                uri = uri,
                method = "GET",
                body = "",
                properties = emptyMap())

        val response: HttpResponse
        try {
            response = httpClient.load(request)
        } catch (e: Throwable) {
            logWrapper.e(e.message ?: "Some error occurred during communcation with NIS.")
            throw e
        }

        if (response.status != HttpURLConnection.HTTP_OK) {
            val message = "Illegal response status: ${response.status}"
            logWrapper.e(message)
            throw NetworkException(message)
        }

        val responseString = response.body
        try {
            logWrapper.i("response = $responseString")
            return Gson().fromJson(responseString, T::class.java)
        } catch (e: JsonParseException) {
            val message = "Failed to parse response: $responseString"
            logWrapper.e(message)
            throw ParseException(message)
        }
    }

    /**
     * Requests with POST method.
     * This is general way to communicate with NIS if there is no method corresponding to the api you want to use.
     *
     * @param path API path started with "/".
     * @param body Request content model.
     * @param queries Request queries consisted of key and value.
     * @return data model corresponding to the API response.
     */
    inline fun <R, reified S : Any>post(path: String, body: R, queries: Map<String, String> = emptyMap()): S {
        val uri = URI(NetworkUtils.createUrlString(hostUrl, path, queries))
        val requestBodyString = Gson().toJson(body)

        logWrapper.i("post request url = $uri, body = $requestBodyString")
        val request = HttpRequest(
                uri = uri,
                method = "POST",
                body = requestBodyString,
                properties = mapOf("Content-Type" to "application/json; charset=utf-8")
        )

        val response: HttpResponse
        try {
            response = httpClient.load(request)
        } catch (e: Throwable) {
            logWrapper.e(e.message ?: "Some error occurred during communcation with NIS.")
            throw e
        }

        if (response.status != HttpURLConnection.HTTP_OK) {
            val message = "Illegal response status: ${response.status}"
            logWrapper.e(message)
            throw NetworkException(message)
        }

        val responseString = response.body
        try {
            logWrapper.i("response = $responseString")
            return Gson().fromJson(responseString, S::class.java)
        } catch (e: JsonParseException) {
            val message = "Failed to parse response: $responseString"
            logWrapper.e(message)
            throw ParseException(message)
        }
    }

    /**
     * Retrieves the account data by providing the address for the account.
     *
     * @param address The address of the account.
     * @return AccountMetaDataPair of the account.
     */
    fun accountGet(address: String): AccountMetaDataPair =
            get("/account/get", mapOf("address" to address))

    /**
     * Retrieves the account data by providing the public key for the account.
     *
     * @param publicKey The public key of the account as hex string.
     * @return AccountMetaDataPair of the account.
     */
    fun accountGetFromPublicKey(publicKey: String): AccountMetaDataPair =
            get("/account/get/from-public-key", mapOf("publicKey" to publicKey))

    /**
     * Gets an array of mosaic objects for a given account address.
     *
     * @param address The address of the account.
     * @return An array of mosaics the address owned.
     */
    fun accountMosaicOwned(address: String): MosaicArray =
            get("/account/mosaic/owned", mapOf("address" to address))

    /**
     * Gets the mosaic definitions for a given namespace. The request supports paging.
     *
     * @param namespace The namespace id.
     * @param id The topmost mosaic definition database id up to which root mosaic definitions are returned. The parameter is optional. If not supplied the most recent mosaic definition are returned.
     * @param pageSize The number of mosaic definition objects to be returned for each request. The parameter is optional. The default value is 25, the minimum value is 5 and hte maximum value is 100.
     * @return An array of mosaic definitions.
     */
    fun namespaceMosaicDefinitionPage(namespace: String, id: Int = -1, pageSize: Int = -1): MosaicDefinitionMetaDataPairArray {
        val query = mutableMapOf("namespace" to namespace)
        if (id >= 0) {
            query.put("id", id.toString())
        }
        if (pageSize >= 0) {
            query.put("pagesize", pageSize.toString())
        }
        return get("/namespace/mosaic/definition/page", query)
    }

    /**
     * Gets the mosaic definitions for a given namespace and name.
     * There is no NEM API corresponding to this method, but this method behaves like an API of NEM.
     * This method calls namespaceMosaicDefinitionPage repeatedly to search the mosaic of the given name and returns the result.
     * Null is returned if there is no mosaic of the name in the namespace.
     *
     * @param namespace The namespace id.
     * @param name The mosaic name.
     * @return Mosaic definition or null(if no result)
     */
    fun namespaceMosaicDefinitionFromName(namespace: String, name: String): MosaicDefinitionMetaDataPair? {
        var id = -1
        do {
            val definitions =  namespaceMosaicDefinitionPage(namespace, id, 100)
            definitions.data.forEach {
                if (it.mosaic.id.name == name) {
                    return it
                }
            }
            if (definitions.data.isNotEmpty()) {
                id = definitions.data.last().meta.id
            }
        } while (definitions.data.isNotEmpty())

        return null
    }


    /**
     * Creates and broadcasts a transaction. The private key is not involved.
     * @param requestAnnounce A RequestAnnounce JSON object.
     * @return NemAnnouseResult object as the request result.
     */
    fun transactionAnnounce(requestAnnounce: RequestAnnounce): NemAnnounceResult =
            post("/transaction/announce", requestAnnounce)
}

