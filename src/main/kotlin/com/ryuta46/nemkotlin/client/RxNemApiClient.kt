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

import com.ryuta46.nemkotlin.model.*
import com.ryuta46.nemkotlin.net.HttpClient
import com.ryuta46.nemkotlin.net.HttpURLConnectionClient
import com.ryuta46.nemkotlin.util.Logger
import com.ryuta46.nemkotlin.util.NoOutputLogger
import io.reactivex.Observable

/**
 * NEM API client to NIS.
 * This class returns an observable object of RxJava.
 *
 * This class calls each NemApiClient method internally.
 *
 * @param hostUrl NIS host URL.
 * @param httpClient HTTP client to communicate with NIS.
 * @param logger Logging function.
 */
class RxNemApiClient(hostUrl: String,
                     httpClient: HttpClient = HttpURLConnectionClient(),
                     logger: Logger = NoOutputLogger()) {
    val syncClient: NemApiClient = NemApiClient(hostUrl, httpClient, logger)

    /**
     * Creates an observable object
     */
    inline fun <T> observe(crossinline body: () -> T): Observable<T> {
        return Observable.create { subscriber ->
            try {
                val response = body()
                if (response != null) subscriber.onNext(response)
            } catch (e: Throwable) {
                subscriber.onError(e)
            }
            subscriber.onComplete()
        }

    }

    /**
     * @see NemApiClient.get
     */
    inline fun <reified T : Any>get(path: String, query: Map<String, String> = emptyMap()): Observable<T> =
            observe { syncClient.get<T>(path, query) }

    /**
     * @see NemApiClient.post
     */
    inline fun <R, reified S : Any>post(path: String, body: R, query: Map<String, String> = emptyMap()): Observable<S> =
            observe { syncClient.post<R, S>(path, body, query) }



    /**
     * @see NemApiClient.heartbeat
     */
    fun heartbeat(): Observable<NemRequestResult> =
            observe { syncClient.heartbeat() }

    /**
     * @see NemApiClient.status
     */
    fun status(): Observable<NemRequestResult> =
            observe { syncClient.status() }

    /**
     * @see NemApiClient.accountGet
     */
    fun accountGet(address: String): Observable<AccountMetaDataPair> =
            observe { syncClient.accountGet(address) }

    /**
     * @see NemApiClient.accountGetFromPublicKey
     */
    fun accountGetFromPublicKey(publicKey: String): Observable<AccountMetaDataPair> =
            observe { syncClient.accountGetFromPublicKey(publicKey) }

    /**
     * @see NemApiClient.accountGetForwarded
     */
    fun accountGetForwarded(address: String): Observable<AccountMetaDataPair> =
            observe { syncClient.accountGetForwarded(address) }

    /**
     * @see NemApiClient.accountGetForwardedFromPublicKey
     */
    fun accountGetForwardedFromPublicKey(publicKey: String): Observable<AccountMetaDataPair> =
            observe { syncClient.accountGetFromPublicKey(publicKey) }

    /**
     * @see NemApiClient.accountStatus
     */
    fun accountStatus(address: String): Observable<AccountMetaData> =
            observe { syncClient.accountStatus(address) }

    /**
     * @see NemApiClient.accountTransfersIncoming
     */
    fun accountTransfersIncoming(address: String, hash: String = "", id: Int = -1): Observable<List<TransactionMetaDataPair>> =
            observe { syncClient.accountTransfersIncoming(address, hash, id) }

    /**
     * @see NemApiClient.accountTransfersOutgoing
     */
    fun accountTransfersOutgoing(address: String, hash: String = "", id: Int = -1): Observable<List<TransactionMetaDataPair>> =
            observe { syncClient.accountTransfersOutgoing(address, hash, id) }

    /**
     * @see NemApiClient.accountTransfersAll
     */
    fun accountTransfersAll(address: String, hash: String = "", id: Int = -1): Observable<List<TransactionMetaDataPair>> =
            observe { syncClient.accountTransfersAll(address, hash, id) }

    /**
     * @see NemApiClient.accountUnconfirmedTransactions
     */
    fun accountUnconfirmedTransactions(address: String): Observable<List<UnconfirmedTransactionMetaDataPair>> =
            observe { syncClient.accountUnconfirmedTransactions(address) }

    /**
     * @see NemApiClient.accountHarvests
     */
    fun accountHarvests(address: String): Observable<List<HarvestInfo>> =
            observe { syncClient.accountHarvests(address) }

    /**
     * @see NemApiClient.accountImportances
     */
    fun accountImportances(): Observable<List<AccountImportanceViewModel>> =
            observe { syncClient.accountImportances() }

    /**
     * @see NemApiClient.accountNamespacePage
     */
    fun accountNamespacePage(address: String, parent: String = "", id: Int = -1, pageSize: Int = -1): Observable<List<Namespace>> =
            observe { syncClient.accountNamespacePage(address, parent, id, pageSize) }

    /**
     * @see NemApiClient.accountMosaicOwned
     */
    fun accountMosaicOwned(address: String): Observable<List<Mosaic>> =
            observe { syncClient.accountMosaicOwned(address) }

    /**
     * @see NemApiClient.accountHistoricalGet
     */
    fun accountHistoricalGet(address: String, startHeight: Int, endHeight: Int, increment: Int): Observable<List<AccountHistoricalDataViewModel>> =
            observe { syncClient.accountHistoricalGet(address, startHeight, endHeight, increment) }

    /**
     * @see NemApiClient.namespace
     */
    fun namespace(namespace: String): Observable<Namespace> =
            observe { syncClient.namespace(namespace) }

    /**
     * @see NemApiClient.namespaceMosaicDefinitionPage
     */
    fun namespaceMosaicDefinitionPage(namespace: String, id: Int = -1, pageSize: Int = -1): Observable<List<MosaicDefinitionMetaDataPair>> =
            observe { syncClient.namespaceMosaicDefinitionPage(namespace, id, pageSize) }

    /**
     * @see NemApiClient.namespaceMosaicDefinitionFromName
     */
    fun namespaceMosaicDefinitionFromName(namespace: String, name: String): Observable<MosaicDefinitionMetaDataPair?> =
            observe { syncClient.namespaceMosaicDefinitionFromName(namespace, name) }

    /**
     * @see NemApiClient.transactionAnnounce
     */
    fun transactionAnnounce(requestAnnounce: RequestAnnounce): Observable<NemAnnounceResult> =
            observe { syncClient.transactionAnnounce(requestAnnounce) }

    /**
     * @see NemApiClient.networkTime
     */
    fun networkTime(): Observable<NodeTimeStamp> =
            observe { syncClient.networkTime() }

    /**
     * @see NemApiClient.mosaicSupply
     */
    fun mosaicSupply(mosaicId: MosaicId): Observable<MosaicSupply> =
            observe { syncClient.mosaicSupply(mosaicId) }
}

