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

/**
 * A block is the structure that contains the transaction information.
 *
 * @property timeStamp The number of seconds elapsed since the creation of the nemesis block.
 * @property signature The signature of the block.
 * @property prevBlockHash The sha3-256 hash of the last block as hex-string.
 * @property type The block type. There are currently two block types used:
 * -1: Only the nemesis block has this type.
 * 1: Regular block type.
 * @property transactions The array of transaction structures.
 * @property version The block version.
 * @property signer The public key of the harvester of the block as hexadecimal number.
 * @property height The height of the block. Each block has a unique height. Subsequent blocks differ in height by 1.
 */
data class Block(
        val timeStamp: Int,
        val signature: String,
        val prevBlockHash: TransactionHash,
        val type: Int,
        val transactions: List<Transaction>,
        val version: Int,
        val signer: String,
        val height: Int
)