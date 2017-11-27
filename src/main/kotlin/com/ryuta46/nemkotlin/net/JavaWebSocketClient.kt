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
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.channels.NotYetConnectedException


/**
 * WebSocket client implementation using Java-WebSocket.
 *
 * @property connectionLostTimeout Interval checking for lost connections (in second).
 */
class JavaWebSocketClient(private val connectionLostTimeout: Int = 30) : WebSocketClient, WebSocketClientFactory {
    private var socket: org.java_websocket.client.WebSocketClient? = null

    override fun create(): WebSocketClient = JavaWebSocketClient(connectionLostTimeout)

    override fun open(uri: URI, listener: WebSocketListener) {
        synchronized(this) {
            val socket = object : org.java_websocket.client.WebSocketClient(uri) {
                override fun onOpen(handshake: ServerHandshake) {
                    listener.onOpen()
                }
                override fun onClose(code: Int, reason: String, remote: Boolean) {
                    //print("onClose code=$code, reason=$reason, remote=$remote")
                    listener.onClose(reason)
                }
                override fun onMessage(message: String) {
                    listener.onMessage(message.toByteArray())
                }
                override fun onError(ex: Exception) {
                    listener.onFailure(ex.localizedMessage)
                }
            }.apply {
                connectionLostTimeout = this@JavaWebSocketClient.connectionLostTimeout
                connect()
            }
            this.socket = socket
        }
    }

    override fun close() {
        val socket = socket ?: return
        this.socket = null
        // normal close
        socket.close()
    }

    override fun send(bytes: ByteArray) {
        synchronized(this) {
            val socket = socket ?: throw NetworkException("Connection is not opened.")
            try {
                socket.send(bytes)
            } catch(e: Exception) {
                when(e) {
                    is NotYetConnectedException, is WebsocketNotConnectedException -> {
                        throw NetworkException(e.message)
                    }
                    else -> throw e
                }
            }
        }
    }

}
