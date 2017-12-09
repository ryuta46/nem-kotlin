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
import java.net.URI
import java.net.URL
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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

    var connectionTimeout = 30

    // WebSocketControlTask lives while the socket is opened.
    // Socket continues to open until there is no SUBSCRIBE path.
    private class WebSocketControlTask(private val hostUrl: String, private val webSocketClient: WebSocketClient,logger: Logger) : WebSocketListener {
        private val logWrapper = LogWrapper(logger, RxNemWebSocketClient::class.java.simpleName)

        data class FrameQueueEntry(val frame: StompFrame, val onSubscribed:()->Unit)
        private val frameQueue = LinkedBlockingQueue<FrameQueueEntry>()

        val subject: Subject<StompFrame> = PublishSubject.create()

        private val usedIds = mutableSetOf<Int>()

        private val openResult = LinkedBlockingDeque<Boolean>()

        private var closeByServer = false
        private var closeByClient = false
        private var closeReason: String? = ""

        private var onDisposed:() -> Unit = {}

        fun start(connectionTimeout: Int, onDisposed:() -> Unit) {
            this.onDisposed = onDisposed
            Thread {
                try {
                    open()
                    val opened = openResult.poll(connectionTimeout.toLong(), TimeUnit.SECONDS) ?: false
                    if (opened) {
                        while (!(closeByClient || closeByServer)) {
                            val entry = frameQueue.poll(10, TimeUnit.MILLISECONDS) ?: continue

                            sendFrameImmediately(entry.frame)
                            entry.onSubscribed()
                        }

                        if (closeByClient) {
                            sendFrameImmediately(StompFrame(StompFrame.Command.Disconnect))
                        }
                    }
                } catch (e: Throwable) {
                    closeReason = e.message
                }
                logWrapper.d("Tear down")

                subject.onError(NetworkException(closeReason))
                subject.onComplete()

                webSocketClient.close()
            }.start()
        }

        fun registerNewId(): Int {
            val id = (0..Int.MAX_VALUE).find { !usedIds.contains(it) } ?: throw IllegalArgumentException("Too many observables.")
            usedIds.add(id)
            return id
        }

        fun unregisterId(id: Int) {
            if (usedIds.contains(id)) {
                usedIds.remove(id)
                if (usedIds.isEmpty()) {
                    logWrapper.d("Disposing control task")
                    close() // Graceful shutdown
                }
            }
        }


        private fun open() {
            logWrapper.i("Opening connection to $hostUrl")

            val path = "/w/messages/websocket"
            logWrapper.i("Request path: $path")
            val url = NetworkUtils.createUrlString(hostUrl, path , emptyMap())
            webSocketClient.open(URI(url), this)
            logWrapper.i("Called open: $path")
        }

        fun sendFrame(frame: StompFrame, onSubscribed: () -> Unit = {}) {
            frameQueue.add(FrameQueueEntry(frame, onSubscribed))
        }

        // Graceful shutdown
        fun close() {
            onDisposed()
            closeByClient = true
        }

        @Synchronized private fun sendFrameImmediately(frame: StompFrame) {
            logWrapper.i("Sending frame: ${frame.lineDescription} $this")
            webSocketClient.send(frame.toString())
        }

        override fun onMessage(bytes: ByteArray) {
            onMessage(String(bytes))
        }

        override fun onMessage(text: String) {
            logWrapper.i("Received bytes: ${text.length} $this")
            val frame = try {
                StompFrame.parse(text)
            } catch (e: ParseException) {
                logWrapper.e("Failed to parse stomp frame: ${e.message}")
                return
            }
            logWrapper.i("Received frame: ${frame.lineDescription}")
            if (frame.command == StompFrame.Command.Connected) {
                openResult.add(true)
            }
            subject.onNext(frame)
        }

        override fun onOpen() {
            val url = URL(hostUrl)
            // Send CONNECT frame.
            val connect = StompFrame(StompFrame.Command.Connect, mapOf("accept-version" to "1.1,1.0", "host" to url.host))
            sendFrameImmediately(connect)
        }

        override fun onClose(reason: String?) {
            openResult.add(false)
            onDisposed()
            logWrapper.i("Closed socket to $hostUrl. reason: $reason")
            closeReason = reason
            closeByServer = true
        }

        override fun onFailure(message: String) {
            openResult.add(false)
            onDisposed()
            logWrapper.e("Error occurred during communication. message: $message")
            closeReason = message
            closeByServer = true
        }

    }

    private var socketControlTask: WebSocketControlTask? = null
    private val taskCreationLock = ReentrantLock()

    private inline fun <reified T : Any>subscribe(path: String, noinline onSubscribed: () -> Unit, afterSubscriptionFrame: StompFrame? = null): Observable<T> {
        // Create new observable object and restore.

        return Observable.create<T> { subscriber ->
            val (task, id) =
                    taskCreationLock.withLock {
                        val task = socketControlTask ?:
                                WebSocketControlTask(hostUrl, webSocketClientFactory.create(), logger).apply {
                                    socketControlTask = this
                                    start(connectionTimeout) {
                                        // Invalidate this task
                                        if (this == socketControlTask) {
                                            logWrapper.d("Disposed task $socketControlTask")
                                            socketControlTask = null
                                        }
                                    }
                        }
                        Pair(task, task.registerNewId())
                    }

            logWrapper.d("Subscribe to subject id: $id")
            // Subscript received frame subject
            val subscription = task.subject.onErrorResumeNext { e: Throwable ->
                logWrapper.d("On error subject id: $id")

                taskCreationLock.withLock { task.unregisterId(id) }
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
            task.sendFrame(subscribe, onSubscribed)
            // If there is a frame after subscription, send it.
            afterSubscriptionFrame?.let { task.sendFrame(it) }

            subscriber.setDisposable(object : Disposable {
                override fun dispose() {
                    logWrapper.d("Disposing subscription id $id")
                    // Sending unsubscribe frame
                    val unsubscribe = StompFrame(StompFrame.Command.Unsubscribe, mapOf("id" to id.toString()))
                    task.sendFrame(unsubscribe)
                    taskCreationLock.withLock { task.unregisterId(id) }
                    subscription.dispose()
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
    fun recentTransactions(address: String, onSubscribed: () -> Unit = {}): Observable<List<TransactionMetaDataPair>> {
        return subscribe<TransactionMetaDataPairArray>("/recenttransactions/$address", onSubscribed,
                createSendFrame("/w/api/account/transfers/all", "{'account':'$address'}")).map { it.data }
    }


    // To cast to TransactionMetaDataPair
    private data class TransactionMetaDataMapper(
            val height: Long, // Long. Illegal height is returned 'unconfimerd' API.
            val id: Int, val hash: TransactionHash)
    private data class TransactionMetaDataPairMapper( val meta: TransactionMetaDataMapper, val transaction: GeneralTransaction)

    /**
     * Gets unconfirmed transactions related to the given address.
     * @param address Account address.
     */
    fun unconfirmed(address: String, onSubscribed: () -> Unit = {}): Observable<TransactionMetaDataPair> {
        return subscribe<TransactionMetaDataPairMapper>("/unconfirmed/$address", onSubscribed).map {
            TransactionMetaDataPair(TransactionMetaData(0, it.meta.id, it.meta.hash), it.transaction)
        }
    }

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
