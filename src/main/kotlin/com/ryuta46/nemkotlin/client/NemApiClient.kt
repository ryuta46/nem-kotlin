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
import com.ryuta46.nemkotlin.model.AccountMetaDataPair
import com.ryuta46.nemkotlin.net.HttpClient
import com.ryuta46.nemkotlin.net.HttpRequest
import com.ryuta46.nemkotlin.net.HttpResponse
import com.ryuta46.nemkotlin.net.HttpURLConnectionClient
import com.ryuta46.nemkotlin.util.Logger
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
                   val logger: Logger = NoOutputLogger()) {

    /**
     * Requests with GET method.
     * This is general way to communicate with NIS if there is no method corresponding to the api you want to use.
     *
     * @param path API path started with "/".
     * @param queries Request queries consisted of key and value.
     * @return data model corresponding to the API response.
     */
    inline fun <reified T : Any>get(path: String, queries: Map<String, String> = emptyMap()): T {
        val uri = URI(createUrlString(path, queries))

        logger.log(Logger.Level.Info, "get request url = $uri")
        val request = HttpRequest(
                uri = uri,
                method = "GET",
                body = "",
                properties = emptyMap())

        val response: HttpResponse
        try {
            response = httpClient.load(request)
        } catch (e: Throwable) {
            logger.log(Logger.Level.Error, e.message ?: "Some error occurred during communcation with NIS.")
            throw e
        }

        if (response.status != HttpURLConnection.HTTP_OK) {
            val message = "Illegal response status: ${response.status}"
            logger.log(Logger.Level.Error, message)
            throw NetworkException(message)
        }

        val responseString = response.body
        try {
            logger.log(Logger.Level.Info, "response = $responseString")
            return Gson().fromJson(responseString, T::class.java)
        } catch (e: JsonParseException) {
            val message = "Failed to parse response: $responseString"
            logger.log(Logger.Level.Error, message)
            throw ParseException(message)
        }
    }

    /**
     * Creates URL String by concatenating hostURL, path and queries.
     * @param path Path.
     * @param queries Queries.
     * @return Full URL String.
     */
    fun createUrlString(path: String, queries: Map<String, String>): String {
        val url = hostUrl + path
        if (queries.isEmpty()){
            return url
        }
        return url +  "?" + queries.toList().joinToString(separator = "&") {
            it.first + "=" + it.second
        }
    }

    /**
     * Retrieves the account data by providing the public key for the account.
     *
     * @param publicKey The public key of the account as hex string.
     * @return AccountMetaDataPair of the account.
     */
    fun accountGetFromPublicKey(publicKey: String): AccountMetaDataPair =
            get("/account/get/from-public-key", mapOf("publicKey" to publicKey))

}

