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
import com.ryuta46.nemkotlin.net.*
import com.ryuta46.nemkotlin.util.LogWrapper
import com.ryuta46.nemkotlin.util.Logger
import com.ryuta46.nemkotlin.util.NetworkUtils
import com.ryuta46.nemkotlin.util.NoOutputLogger
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import sun.plugin.dom.exception.InvalidStateException
import java.net.URI
import java.net.URL

/**
 * NEM WebSocket client to NIS.
 * This class returns an observable object of RxJava.
 *
 * @param hostUrl NIS host URL.
 * @param webSocketClientFactory WebSocket client factory to create WebSocketClient communicating with NIS.
 * @param logger Logging function.
 */
class RxNemWebSocketClient(private val hostUrl: String,
                           private val webSocketClientFactory: WebSocketClientFactory = JavaWebSocketClient(),
                           private val logger: Logger = NoOutputLogger()) {

    private val logWrapper = LogWrapper(logger, this::class.java.simpleName)

    // WebSocketControlTask lives while the socket is opened.
    // Socket continues to open until there is no SUBSCRIBE path.
    private class WebSocketControlTask(private val hostUrl: String, private val webSocketClient: WebSocketClient, logger: Logger) : WebSocketListener {
        private val logWrapper = LogWrapper(logger, RxNemWebSocketClient::class.java.simpleName)
        private val frameQueue = mutableListOf<StompFrame>()

        enum class State {
            Ready,
            Opening,
            Opened,
            Closing,
            Closed
        }

        private var state = State.Ready


        val subject: Subject<StompFrame> = PublishSubject.create()
        private var isStreamClosed = false

        private fun closeStream(e: Throwable?) {
            synchronized(this) {
                if (isStreamClosed) return
                if (e != null) {
                    subject.onError(e)
                }
                subject.onComplete()
                isStreamClosed = true
            }
        }

        fun open() {
            synchronized(this) {
                if (state != State.Ready) throw InvalidStateException("Failed to open socket.")
                logWrapper.i("Opening connection to $hostUrl")
                val url = NetworkUtils.createUrlString(hostUrl, "/w/messages/websocket", emptyMap())
                webSocketClient.open(URI(url), this)
                state = State.Opening
            }
        }

        fun sendFrame(frame: StompFrame) {
            synchronized(this) {
                when(state) {
                    State.Opening -> {
                        logWrapper.i("Queue frame: ${frame.lineDescription}")
                        frameQueue.add(frame)
                    }
                    State.Opened -> {
                        logWrapper.i("Sending frame: ${frame.lineDescription}")
                        webSocketClient.send(frame.toString().toByteArray())
                    }
                    else -> {
                        logWrapper.i("Ignored frame: ${frame.lineDescription}")
                    }
                }
            }
        }

        // Graceful shutdown
        fun close() {
            synchronized(this) {
                state = when (state) {
                    State.Opened -> {
                        sendFrame(StompFrame(StompFrame.Command.Disconnect))
                        webSocketClient.close()
                        State.Closing
                    }
                    else -> {
                        webSocketClient.close()
                        State.Closed
                    }
                }

            }
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
                    state = State.Opened
                }
            }
            subject.onNext(frame)
        }

        override fun onOpen() {
            val url = URL(hostUrl)
            // Send CONNECT frame.
            val connect = StompFrame(StompFrame.Command.Connect, mapOf("accept-version" to "1.0,1.2", "host" to url.host))
            logWrapper.i("Sending frame: ${connect.lineDescription}")
            webSocketClient.send(connect.toString().toByteArray())
        }

        override fun onClose(reason: String?) {
            logWrapper.i("Closed socket to $hostUrl. reason: $reason")

            // if there is any listener after closed socket, notify error to them.
            closeStream(NetworkException(reason))

            synchronized(this) {
                webSocketClient.close()
                state = State.Closed
            }
        }

        override fun onFailure(message: String) {
            logWrapper.e("Error occurred during communication. message: $message")
            closeStream(NetworkException(message))
        }
    }
    private var socketControlTask: WebSocketControlTask? = null
    private val usedIds = mutableSetOf<Int>()

    private inline fun <reified T : Any>subscribe(path: String, crossinline onSubscribed: () -> Unit, afterSubscriptionFrame: StompFrame? = null): Observable<T> {
        // Create new observable object and restore.

        return Observable.create<T> { subscriber ->
            synchronized(this@RxNemWebSocketClient) {
                val task = socketControlTask ?: WebSocketControlTask(hostUrl, webSocketClientFactory.create(), logger).apply {
                    socketControlTask = this
                    open()
                }
                // Search subscription ID
                val id = (0..Int.MAX_VALUE).find { !usedIds.contains(it) } ?: throw IllegalArgumentException("Too many observables.")

                // Subscript received frame subject
                val subscription = task.subject.onErrorResumeNext { e: Throwable ->
                    subscriber.onError(e)
                    Observable.empty()
                }.subscribe { frame: StompFrame ->
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

                // Send subscribe frame
                val subscribe = StompFrame(StompFrame.Command.Subscribe, mapOf("id" to id.toString(), "destination" to path))
                task.sendFrame(subscribe)
                // If there is a frame after subscription, send it.
                afterSubscriptionFrame?.let { task.sendFrame(it) }

                // subscription call back
                onSubscribed()

                usedIds.add(id)
                subscriber.setDisposable(object : Disposable {
                    override fun dispose() {
                        synchronized(this@RxNemWebSocketClient) {
                            logWrapper.i("Disposing subscription")
                            subscription.dispose()

                            // Sending unsubscribe frame
                            val unsubscribe = StompFrame(StompFrame.Command.Unsubscribe, mapOf("id" to id.toString()))
                            task.sendFrame(unsubscribe)

                            usedIds.remove(id)
                            if (usedIds.isEmpty()) {
                                task.close() // Graceful shutdown
                                socketControlTask = null
                            }
                        }
                    }
                    override fun isDisposed(): Boolean = subscription.isDisposed
                })
            }
        }
    }

    private fun createSendFrame(path: String, body: String) : StompFrame {
        return StompFrame(StompFrame.Command.Send,
                mapOf("destination" to path,
                        "content-length" to body.length.toString()), body)
    }

    /**
     * Gets an account information.
     * @param address Account address.
     */
    fun accountGet(address: String, onSubscribed: () -> Unit = {}): Observable<AccountMetaDataPair> {
        return subscribe("/account/$address", onSubscribed,
                createSendFrame("/w/api/account/get", "{'account':'$address'}"))
    }

    /**
     * Gets recent transactions related to the given address.
     * @param address Account address.
     */
    fun recentTransactions(address: String, onSubscribed: () -> Unit = {}): Observable<TransactionMetaDataPairArray> {
        return subscribe("/recenttransactions/$address", onSubscribed,
                createSendFrame("/w/api/account/transfers/all", "{'account':'$address'}"))
    }

    /**
     * Gets unconfirmed transactions related to the given address.
     * @param address Account address.
     */
    // FIXME: Bridge to unconfirmed transaction data model.
    fun unconfirmed(address: String, onSubscribed: () -> Unit = {}): Observable<TransactionMetaDataPair>
            = subscribe("/unconfirmed/$address", onSubscribed)

    /**
     * Gets confirmed transactions related to the given address.
     */
    fun transactions(address: String, onSubscribed: () -> Unit = {}): Observable<TransactionMetaDataPair>
            = subscribe("/transactions/$address", onSubscribed)

    // To cast to MosaicDefinition type
    private data class MosaicDefinitionMapper(val mosaicDefinition: MosaicDefinition)

    /**
     * Gets mosaic definitions owned by the given address.
     * @param address Owner address of mosaic definitions.
     */
    fun accountMosaicOwnedDefinition(address: String, onSubscribed: () -> Unit = {}): Observable<MosaicDefinition> {
        val bridge = subscribe<MosaicDefinitionMapper>("/account/mosaic/owned/definition/$address", onSubscribed,
                createSendFrame("/w/api/account/mosaic/owned/definition", "{'account':'$address'}"))
        return bridge.map { it.mosaicDefinition }
    }

    /**
     * Gets mosaics owned by the given address.
     * @param address Owner address of mosaics.
     */
    fun accountMosaicOwned(address: String, onSubscribed: () -> Unit = {}): Observable<Mosaic> {
        return subscribe("/account/mosaic/owned/$address", onSubscribed,
                createSendFrame("/w/api/account/mosaic/owned", "{'account':'$address'}"))
    }

    /**
     * Gets namespaces owned by the given address.
     * @param address Owner address of namespaces.
     */
    fun accountNamespaceOwned(address: String, onSubscribed: () -> Unit = {}): Observable<Namespace> {
        return subscribe("/account/namespace/owned/$address", onSubscribed,
                createSendFrame("/w/api/account/namespace/owned", "{'account':'$address'}"))
    }

    /**
     * Gets the latest block information.
     */
    fun blocks(onSubscribed: () -> Unit = {}): Observable<Block> = subscribe("/blocks", onSubscribed)

    /**
     * Gets the new block height.
     */
    fun blocksNew(onSubscribed: () -> Unit = {}): Observable<BlockHeight> = subscribe("/blocks/new", onSubscribed)



}
