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
import com.ryuta46.nemkotlin.exceptions.ParseException
import com.ryuta46.nemkotlin.model.*
import com.ryuta46.nemkotlin.net.OkHttpWebSocketClient
import com.ryuta46.nemkotlin.net.StompFrame
import com.ryuta46.nemkotlin.net.WebSocketClient
import com.ryuta46.nemkotlin.net.WebSocketListener
import com.ryuta46.nemkotlin.util.LogWrapper
import com.ryuta46.nemkotlin.util.Logger
import com.ryuta46.nemkotlin.util.NetworkUtils
import com.ryuta46.nemkotlin.util.NoOutputLogger
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.net.URL

class RxNemWebSocketClient(hostUrl: String,
                           webSocketClient: WebSocketClient = OkHttpWebSocketClient(),
                           logger: Logger = NoOutputLogger()) {

    private val logWrapper = LogWrapper(logger, this::class.java.simpleName)

    private class WebSocketController(private val hostUrl: String, private val webSocketClient: WebSocketClient, logger: Logger) : WebSocketListener {
        private val logWrapper = LogWrapper(logger, RxNemWebSocketClient::class.java.simpleName)
        private val frameQueue = mutableListOf<StompFrame>()
        private var isOpened = false

        val subject: Subject<StompFrame> = BehaviorSubject.create<StompFrame>()

        fun open() {
            logWrapper.i("Opening connection to $hostUrl")
            val url = NetworkUtils.createUrlString(hostUrl, "/w/messages/websocket", emptyMap())
            webSocketClient.open(URL(url), this)
        }

        fun sendFrame(frame: StompFrame) {
            synchronized(this) {
                if (isOpened) {
                    logWrapper.i("Sending frame: ${frame.lineDescription}")
                    webSocketClient.send(frame.toString().toByteArray())
                } else {
                    logWrapper.i("Queue frame: ${frame.lineDescription}")
                    frameQueue.add(frame)
                }
            }
        }

        fun close() {
            webSocketClient.close()
        }

        override fun onMessage(bytes: ByteArray) {
            val frame = StompFrame.parse(String(bytes))
            logWrapper.i("Received frame: ${frame.lineDescription}")
            if (frame.command == StompFrame.Command.Connected) {
                synchronized(this) {
                    // Send queued frames.
                    frameQueue.forEach { frame ->
                        logWrapper.i("Sending queued frame: ${frame.lineDescription}")
                        webSocketClient.send(frame.toString().toByteArray())
                    }
                    frameQueue.clear()
                    isOpened = true
                }
            }
            subject.onNext(frame)
        }

        override fun onOpen() {
            val url = URL(hostUrl)
            // Send CONNECT frame.
            val connect = StompFrame(StompFrame.Command.Connect, mapOf("accept-version" to "1.0,1.2,2.0", "host" to url.host))
            logWrapper.i("Sending frame: ${connect.lineDescription}")
            webSocketClient.send(connect.toString().toByteArray())
        }

        override fun onClose(reason: String?) {
            logWrapper.i("Closed socket to $hostUrl. reason: $reason")
            synchronized(this) {
                frameQueue.clear()
                isOpened = false
            }
        }

        override fun onFailure(message: String) {
            logWrapper.e("Error occurred during communication. message: $message")
        }
    }
    private val socketController = WebSocketController(hostUrl, webSocketClient, logger)
    private val usedIds = mutableSetOf<Int>()

    private inline fun <reified T : Any>subscribe(path: String, afterSubscriptionFrame: StompFrame? = null): Observable<T> {
        // Create new observable object and restore.

        return Observable.create<T> { subscriber ->
            var id = 0
            synchronized(this@RxNemWebSocketClient) {
                if (usedIds.isEmpty()) {
                    socketController.open()
                }
                // Search subscription ID
                id = (0..Int.MAX_VALUE).find { !usedIds.contains(it) } ?: throw IllegalArgumentException("Too many observables.")
                // Sending subscribe frame
                val subscribe = StompFrame(StompFrame.Command.Subscribe, mapOf("id" to id.toString(), "destination" to path))
                socketController.sendFrame(subscribe)
                // If there is a frame after subscription, send it.
                afterSubscriptionFrame?.let { socketController.sendFrame(it) }
                usedIds.add(id)
            }

            // Subscript received frame subject
            val subscription = socketController.subject.subscribe { frame: StompFrame ->
                if (frame.headers["subscription"] == id.toString()) {
                    try {
                        val objT = Gson().fromJson(frame.body, T::class.java)
                        subscriber.onNext(objT)
                    } catch (e: JsonParseException) {
                        val message = "Failed to parse response: ${frame.body}"
                        logWrapper.e(message)
                        subscriber.onError(ParseException(message))
                    }
                }
            }
            subscriber.setDisposable(object : Disposable {
                override fun dispose() {
                    logWrapper.i("Disposing subscription")
                    subscription.dispose()

                    // Sending unsubscribe frame
                    val unsubscribe = StompFrame(StompFrame.Command.Unsubscribe, mapOf("id" to id.toString()))
                    socketController.sendFrame(unsubscribe)
                    synchronized(this@RxNemWebSocketClient) {
                        usedIds.remove(id)
                        if (usedIds.isEmpty()) {
                            socketController.close()
                        }
                    }
                }
                override fun isDisposed(): Boolean = subscription.isDisposed
            })
        }
    }

    private fun createSendFrame(path: String, body: String) : StompFrame {
        return StompFrame(StompFrame.Command.Send,
                mapOf("destination" to path,
                        "content-length" to body.length.toString()), body)
    }

    fun accountGet(address: String): Observable<AccountMetaDataPair> {
        return subscribe("/account/$address",
                createSendFrame("/w/api/account/get", "{'account':'$address'}"))
    }

    fun recentTransactions(address: String): Observable<TransactionMetaDataPairArray> {
        return subscribe("/recenttransactions/$address",
                createSendFrame("/w/api/account/transfers/all", "{'account':'$address'}"))
    }

    fun unconfirmed(address: String): Observable<TransactionMetaDataPair> = subscribe("/unconfirmed/$address")

    fun transactions(address: String): Observable<TransactionMetaDataPair> = subscribe("/transactions/$address")


    // Cast to MosaicDefinition type
    private data class MosaicDefinitionMapper(val mosaicDefinition: MosaicDefinition)
    fun accountMosaicOwnedDefinition(address: String): Observable<MosaicDefinition> {
        val bridge = subscribe<MosaicDefinitionMapper>("/account/mosaic/owned/definition/$address",
                createSendFrame("/w/api/account/mosaic/owned/definition", "{'account':'$address'}"))
        return bridge.map { it.mosaicDefinition }
    }

    fun accountMosaicOwned(address: String): Observable<Mosaic> {
        return subscribe("/account/mosaic/owned/$address",
                createSendFrame("/w/api/account/mosaic/owned", "{'account':'$address'}"))
    }

    fun accountNamespaceOwned(address: String): Observable<Namespace> {
        return subscribe("/account/namespace/owned/$address",
                createSendFrame("/w/api/account/namespace/owned", "{'account':'$address'}"))
    }

    fun blocks(): Observable<Block> = subscribe("/blocks")

    fun blocksNew(): Observable<BlockHeight> = subscribe("/blocks/new")



}
