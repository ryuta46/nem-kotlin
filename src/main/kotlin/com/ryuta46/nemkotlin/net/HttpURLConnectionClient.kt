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
package com.ryuta46.nemkotlin.net

import com.ryuta46.nemkotlin.exceptions.NetworkException
import java.net.HttpURLConnection
import java.nio.charset.Charset

/**
 * HTTP client implementation using java.net.HttpURLConnection.
 */
class HttpURLConnectionClient : HttpClient {
    override fun load(request: HttpRequest): HttpResponse {
        val url = request.uri.toURL()
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = request.method
            request.properties.forEach { key, value ->
                connection.setRequestProperty(key, value)
            }

            if (request.body.isNotEmpty()) {
                connection.doOutput = true
                connection.outputStream.bufferedWriter().use { it.write(request.body) }
            } else {
                connection.connect()
            }


            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                return HttpResponse(status, "")
            }
            val inputStream = connection.inputStream
            val encoding = connection.contentEncoding ?: "UTF-8"

            var body = ""
            inputStream.bufferedReader(Charset.forName(encoding)).useLines {
                it.forEach { body += it }
            }
            return HttpResponse(status, body)
        } catch (e: Exception) {
            throw NetworkException(e.message)
        } finally {
            connection.disconnect()
        }
    }

}
