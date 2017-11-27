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
package com.ryuta46.nemkotlin.model

import com.ryuta46.nemkotlin.transaction.MosaicAttachment

/**
 * @property timeStamp The number of seconds elapsed since the creation of the nemesis block.
 * @property signature The transaction signature.
 * @property fee The fee for the transaction.
 * @property type The transaction type.
 * @property deadline The deadline of the transaction.
 * @property version The version of the structure.
 * @property signer The public key of the account that created the transaction.
 */
data class Transaction(
        val timeStamp: Int,
        val signature: String,
        val fee: Long,
        val type: Int,
        val deadline: Int,
        val version: Int,
        val signer: String,

        // for Transfer transaction
        val amount: Long,
        val recipient: String,
        val mosaics: List<Mosaic>,
        val message: Message
)