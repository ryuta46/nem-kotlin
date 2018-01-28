package com.ryuta46.nemkotlin.util

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.ryuta46.nemkotlin.exceptions.NetworkException
import com.ryuta46.nemkotlin.exceptions.ParseException
import com.ryuta46.nemkotlin.net.HttpClient
import com.ryuta46.nemkotlin.net.HttpRequest
import com.ryuta46.nemkotlin.net.HttpResponse
import com.ryuta46.nemkotlin.net.HttpURLConnectionClient
import java.net.HttpURLConnection
import java.net.URI

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

class NetworkUtils {
    companion object {
        /**
         * Creates URL String by concatenating hostURL, path and queries.
         * @param path Path.
         * @param queries Queries.
         * @return Full URL String.
         */
        @JvmStatic fun createUrlString(hostUrl: String, path: String, queries: Map<String, String>): String {
            val url = hostUrl + path
            if (queries.isEmpty()){
                return url
            }
            return url +  "?" + queries.toList().joinToString(separator = "&") {
                it.first + "=" + it.second
            }
        }


        /**
         * Requests with GET method.
         * @param uri Request URI.
         * @param httpClient HTTP client to communicate with a server.
         * @param logger Logging function.
         * @return data model corresponding to the API response.
         */
        inline fun <reified T : Any>get(uri: URI,
                                        httpClient: HttpClient = HttpURLConnectionClient(),
                                        logger: Logger = NoOutputLogger() ): T {
            val logWrapper = LogWrapper(logger, NetworkUtils::class.java.simpleName)

            logWrapper.i("get request url = $uri")
            val request = HttpRequest(
                    uri = uri,
                    method = "GET",
                    body = "",
                    properties = mapOf("User-Agent" to "nem-kotlin" ))

            val response: HttpResponse
            try {
                response = httpClient.load(request)
            } catch (e: Throwable) {
                logWrapper.e(e.message ?: "Some error occurred during communication.")
                throw e
            }

            if (response.status != HttpURLConnection.HTTP_OK) {
                val message = "Illegal response status: ${response.status} ${response.body}"
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
         *
         * @param uri Request URI.
         * @param body Request Body.
         * @param httpClient HTTP client to communicate with a server.
         * @param logger Logging function.
         * @return data model corresponding to the API response.
         */
        inline fun <R, reified S : Any>post(uri: URI,
                                            body: R,
                                            httpClient: HttpClient = HttpURLConnectionClient(),
                                            logger: Logger = NoOutputLogger() ): S {
            val logWrapper = LogWrapper(logger, NetworkUtils::class.java.simpleName)

            val requestBodyString = Gson().toJson(body)

            logWrapper.i("post request url = $uri, body = $requestBodyString")
            val request = HttpRequest(
                    uri = uri,
                    method = "POST",
                    body = requestBodyString,
                    properties = mapOf(
                            "User-Agent" to "nem-kotlin",
                            "Content-Type" to "application/json; charset=utf-8")
            )

            val response: HttpResponse
            try {
                response = httpClient.load(request)
            } catch (e: Throwable) {
                logWrapper.e(e.message ?: "Some error occurred during communication.")
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



    }


}