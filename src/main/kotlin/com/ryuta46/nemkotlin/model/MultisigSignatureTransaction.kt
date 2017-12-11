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

import com.ryuta46.nemkotlin.util.ConvertUtils
import com.ryuta46.nemkotlin.util.ConvertUtils.Companion.toByteArrayWithLittleEndian

/**
 * Multisig signature transactions are part of the NEM's multisig account system.
 * Multisig signature transactions are included in the corresponding multisig transaction and are the way a cosignatory of a multisig account can sign a multisig transaction for that account.
 *
 * @property otherHash The hash of the inner transaction of the corresponding multisig transaction.
 * @property otherAccount The address of the corresponding multisig account.
 */
class MultisigSignatureTransaction(private val common: Transaction,
                                   val otherHash: TransactionHash,
                                   val otherAccount: String
) : Transaction by common {
    override val byteArray: ByteArray
        get() {
            val hash = ConvertUtils.toByteArray(otherHash.data)
            return common.byteArray +
                    (toByteArrayWithLittleEndian(hash.size) + hash).let {
                        toByteArrayWithLittleEndian(it.size) + it
                    } +
                    toByteArrayWithLittleEndian(otherAccount.length) +
                    otherAccount.toByteArray()
        }
}

