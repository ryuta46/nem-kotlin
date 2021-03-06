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
                   val logger: Logger = NoOutputLogger()) {

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
        return NetworkUtils.get(uri, httpClient, logger)
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
        return NetworkUtils.post(uri, body, httpClient, logger)
    }


    /**
     * Determines if NIS is up and responsive.
     * @return if NIS is up and responsive.
     */
    fun heartbeat(): NemRequestResult = get("/heartbeat")

    /**
     * Determines the status of NIS.
     * @return NIS status
     */
    fun status(): NemRequestResult = get("/status")

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
     * Given a delegate (formerly known as remote) account's address, gets the AccountMetaDataPair for the account for which the given account is the delegate account.
     * @param address The address of the account
     * @return The account for which the given account is the delegate account.
     */
    fun accountGetForwarded(address: String): AccountMetaDataPair =
            get("/account/get/forwarded", mapOf("address" to address))

    /**
     * Given a delegate (formerly known as remote) account's public key, gets the AccountMetaDataPair for the account for which the given account is the delegate account.
     * @param publicKey The public key of the account as hex string.
     * @return The account for which the given account is the delegate account.
     */
    fun accountGetForwardedFromPublicKey(publicKey: String): AccountMetaDataPair =
            get("/account/get/forwarded/from-public-key", mapOf("publicKey" to publicKey))

    /**
     * Gets the AccountMetaData from an account.
     *
     * @param address The address of the account.
     * @return Account status.
     */
    fun accountStatus(address: String): AccountMetaData =
            get("/account//status", mapOf("address" to address))

    /**
     * Gets an array of TransactionMetaDataPair objects where the recipient has the address given as parameter to the request.
     * @param address The address of the account
     * @param hash The 256 bit sha3 hash of the transaction up to which transactions are returned.(Optional)
     * @param id The transaction id up to which transactions are returned.(Optional)
     * @return Transactions for which given account is the receiver.
     */
    fun accountTransfersIncoming(address: String, hash: String = "", id: Int = -1): List<TransactionMetaDataPair> {
        val query = mutableMapOf("address" to address)
        if (hash.isNotEmpty()) {
            query.put("hash", hash)
        }
        if (id >= 0) {
            query.put("id", id.toString())
        }
        return get<TransactionMetaDataPairArray>("/account/transfers/incoming", query).data
    }

    /**
     * Gets an array of transaction meta data pairs where the sender has the address given as parameter to the request.
     * @param address The address of the account
     * @param hash The 256 bit sha3 hash of the transaction up to which transactions are returned.(Optional)
     * @param id The transaction id up to which transactions are returned.(Optional)
     * @return Transactions for which given account is the sender.
     */
    fun accountTransfersOutgoing(address: String, hash: String = "", id: Int = -1): List<TransactionMetaDataPair> {
        val query = mutableMapOf("address" to address)
        if (hash.isNotEmpty()) {
            query.put("hash", hash)
        }
        if (id >= 0) {
            query.put("id", id.toString())
        }
        return get<TransactionMetaDataPairArray>("/account/transfers/outgoing", query).data
    }

    /**
     * Gets an array of transaction meta data pairs for which an account is the sender or receiver.
     * @param address The address of the account
     * @param hash The 256 bit sha3 hash of the transaction up to which transactions are returned.(Optional)
     * @param id The transaction id up to which transactions are returned.(Optional)
     * @return Transactions for which given account is the sender or receiver.
     */
    fun accountTransfersAll(address: String, hash: String = "", id: Int = -1): List<TransactionMetaDataPair> {
        val query = mutableMapOf("address" to address)
        if (hash.isNotEmpty()) {
            query.put("hash", hash)
        }
        if (id >= 0) {
            query.put("id", id.toString())
        }
        return get<TransactionMetaDataPairArray>("/account/transfers/all", query).data
    }

    /**
     * Gets the array of transactions for which an account is the sender or receiver and which have not yet been included in a block.
     * @param address The address of the account
     * @return Unconfirmed transactions
     */
    fun accountUnconfirmedTransactions(address: String): List<UnconfirmedTransactionMetaDataPair> =
            get<UnconfirmedTransactionMetaDataPairArray>("/account/unconfirmedTransactions", mapOf("address" to address)).data


    /**
     * Gets an array of harvest info objects for an account.
     * @param address The address of the account
     * @return Harvest info.
     */
    fun accountHarvests(address: String): List<HarvestInfo> =
            get<HarvestInfoArray>("/account/harvests", mapOf("address" to address)).data

    /**
     * Gets an array of account importance view model objects.
     */
    fun accountImportances(): List<AccountImportanceViewModel> = get<AccountImportanceViewModelArray>("/account/importances").data

    /**
     * Gets an array of namespace objects for a given account address.
     * @param address The address of the account.
     * @param parent The optional parent namespace id.
     * @param id The optional namespace database id up to which namespaces are returned.
     * @param pageSize The (optional) number of namespaces to be returned.
     * @return An array of namespace definitions.
     */
    fun accountNamespacePage(address: String, parent: String = "", id: Int = -1, pageSize: Int = -1): List<Namespace> {
        val query = mutableMapOf("address" to address)
        if (parent.isNotEmpty()) {
            query.put("parent", parent)
        }
        if (id >= 0) {
            query.put("id", id.toString())
        }
        if (pageSize >= 0) {
            query.put("pagesize", pageSize.toString())
        }
        return get<NamespaceArray>("/account/namespace/page", query).data
    }

    /**
     * Gets an array of mosaic objects for a given account address.
     *
     * @param address The address of the account.
     * @return An array of mosaics the address owned.
     */
    fun accountMosaicOwned(address: String): List<Mosaic> =
            get<MosaicArray>("/account/mosaic/owned", mapOf("address" to address)).data


    /**
     * Gets historical information for an account.
     * @param address The address of the account.
     * @param startHeight The block height from which on the data should be supplied.
     * @param endHeight The block height up to which the data should be supplied.
     * @param increment The value by which the height is incremented between each data point.
     */
    fun accountHistoricalGet(address: String, startHeight: Int, endHeight: Int, increment: Int): List<AccountHistoricalDataViewModel> {
        return get<AccountHistoricalDataViewModelArray>("/account/historical/get",
                mapOf("address" to address,
                        "startHeight" to startHeight.toString(),
                        "endHeight" to endHeight.toString(),
                        "increment" to increment.toString())).data
    }


    /**
     * Gets the namespace definition for a given namespace.
     *
     * @param namespace The namespace id.
     * @return Namespace definition
     */
    fun namespace(namespace: String): Namespace =
            get("/namespace", mapOf("namespace" to namespace))

    /**
     * Gets the mosaic definitions for a given namespace. The request supports paging.
     *
     * @param namespace The namespace id.
     * @param id The topmost mosaic definition database id up to which root mosaic definitions are returned. The parameter is optional. If not supplied the most recent mosaic definition are returned.
     * @param pageSize The number of mosaic definition objects to be returned for each request. The parameter is optional. The default value is 25, the minimum value is 5 and hte maximum value is 100.
     * @return An array of mosaic definitions.
     */
    fun namespaceMosaicDefinitionPage(namespace: String, id: Int = -1, pageSize: Int = -1): List<MosaicDefinitionMetaDataPair> {
        val query = mutableMapOf("namespace" to namespace)
        if (id >= 0) {
            query.put("id", id.toString())
        }
        if (pageSize >= 0) {
            query.put("pagesize", pageSize.toString())
        }
        return get<MosaicDefinitionMetaDataPairArray>("/namespace/mosaic/definition/page", query).data
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
            definitions.forEach {
                if (it.mosaic.id.name == name) {
                    return it
                }
            }
            if (definitions.isNotEmpty()) {
                id = definitions.last().meta.id
            }
        } while (definitions.isNotEmpty())

        return null
    }


    /**
     * Creates and broadcasts a transaction. The private key is not involved.
     * @param requestAnnounce A RequestAnnounce JSON object.
     * @return NemAnnouseResult object as the request result.
     */
    fun transactionAnnounce(requestAnnounce: RequestAnnounce): NemAnnounceResult =
            post("/transaction/announce", requestAnnounce)

    /**
     * Gets network time of the node.
     * @return NodeTimeStamp object.
     */
    fun networkTime(): NodeTimeStamp =
            get("/time-sync/network-time")

    /**
     * Gets mosaic current supply.
     * @return MosaicSupply object
     */
    fun mosaicSupply(mosaicId: MosaicId): MosaicSupply =
            get("/mosaic/supply", mapOf("mosaicId" to mosaicId.fullName))
}

