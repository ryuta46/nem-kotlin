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

package com.ryuta46.nemkotlin.util

import com.ryuta46.nemkotlin.model.NodeInfo
import com.ryuta46.nemkotlin.net.HttpClient
import com.ryuta46.nemkotlin.net.HttpURLConnectionClient
import io.reactivex.Single
import java.net.URI


/**
 * NIS related utility functions.
 */
class NisUtils {
    /**
     * An array of node information
     */
    private data class NodeInfoArray(val nodes: List<NodeInfo>)

    companion object {
        /**
         * Gets super nodes from given node list URL. These nodes are for mainnet.
         * The default value of node list URL is "https://supernodes.nem.io/nodes/"
         * @param nodeListUrl Node list URL.
         * @param httpClient HTTP client to communicate with the node list server.
         * @param logger Logging function.
         */
        @JvmStatic fun getSuperNodes(
                nodeListUrl: String = "https://supernodes.nem.io/nodes/",
                httpClient: HttpClient = HttpURLConnectionClient(),
                logger: Logger = NoOutputLogger()) : Single<List<NodeInfo>> {
            return Single.create { subscriber ->
                try {
                    val uri = URI(nodeListUrl)
                    val response = NetworkUtils.get<NodeInfoArray>(uri, httpClient, logger)
                    subscriber.onSuccess(response.nodes)
                } catch (e: Throwable) {
                    subscriber.onError(e)
                }
            }
        }
        /**
         * Gets fixed test nodes. These nodes are for testnet.
         */
        @JvmStatic fun getTestNodes() = listOf(
                NodeInfo("104.128.226.60", 7890),
                NodeInfo("23.228.67.85", 7890),
                //NodeInfo("192.3.61.243", 7890), This node is invalid ?
                NodeInfo("50.3.87.123", 7890)
        )
    }
}
