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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okio.ByteString
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit


class OkHttpWebSocketClient : WebSocketClient, WebSocketClientFactory {
    private val client = //OkHttpClient()
            OkHttpClient.Builder()
                    .readTimeout(3, TimeUnit.SECONDS)
                    .writeTimeout(3, TimeUnit.SECONDS)
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .pingInterval(1, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(false)
                    .build()
    private var socket: WebSocket? = null

    override fun create(): WebSocketClient = OkHttpWebSocketClient()

    override fun open(uri: URI, listener: WebSocketListener) {
        synchronized(this) {
            val okRequest = Request.Builder().get().url(uri.toURL()).build()
            socket = client.newWebSocket(okRequest, object : okhttp3.WebSocketListener() {
                override fun onOpen(webSocket: WebSocket?, response: Response?) {
                    listener.onOpen()
                }

                override fun onMessage(webSocket: WebSocket?, text: String?) {
                    if (text != null) {
                        listener.onMessage(text.toByteArray())
                    }
                }

                override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
                    if (bytes != null) {
                        listener.onMessage(bytes.toByteArray())
                    }
                }

                override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
                }

                override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
                    listener.onClose(reason)
                }

                override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
                    val message = t.toString()// ?: response?.message() ?: "Unknown error."
                    listener.onFailure(message)
                }
            })
        }
    }

    override fun close() {
        synchronized(this) {
            val socket = socket ?: throw NetworkException("Connection is not opened.")
            // normal close
            socket.close(1000, null)

            this.socket = null
        }
    }

    override fun send(bytes: ByteArray) {
        synchronized(this) {
            val socket = socket ?: throw NetworkException("Connection is not opened.")
            if (!socket.send(ByteString.of(ByteBuffer.wrap(bytes)))) throw NetworkException("Failed to send message. ${String(bytes)}")
        }
    }



}