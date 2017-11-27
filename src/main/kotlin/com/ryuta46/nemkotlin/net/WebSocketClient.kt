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

import java.net.URI
import com.ryuta46.nemkotlin.exceptions.NetworkException

/**
 * WebSocket interface to access to NIS.
 */
interface WebSocketClient {
    /**
     * Opens connection to given URI.
     * @param uri Destination URI.
     * @param listener Event listener.
     * @throws NetworkException if connecting to the uri is failed.
     */
    fun open(uri: URI, listener: WebSocketListener)

    /**
     * Sends bytes to opened socket.
     * @param bytes Bytes to be sent.
     * @throws NetworkException if sending bytes is failed.
     */
    fun send(bytes: ByteArray)

    /**
     * Closes the socket.
     */
    fun close()
}

