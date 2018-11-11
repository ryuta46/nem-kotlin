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
 * Transfer transactions contain data about transfers of XEM or mosaics to another account.
 * @property amount The amount of micro NEM that is transferred from sender to recipient.
 * @property recipient The address of the recipient.
 * @property mosaics The array of Mosaic objects.
 * @property message Optionally a transaction can contain a message. In this case the transaction contains a message substructure. If not the field is null.
 */
class TransferTransaction(private val common: Transaction,
    // for Transfer transaction
                          val amount: Long,
                          val recipient: String,
                          val mosaics: List<Mosaic>,
                          val message: Message?
) : Transaction by common {
    override val byteArray: ByteArray
        get() {
            return common.byteArray +
                    toByteArrayWithLittleEndian(recipient.length) +
                    recipient.toByteArray(Charsets.UTF_8) +
                    toByteArrayWithLittleEndian(amount) +
                    if (message?.payload?.isNotEmpty() == true) {
                        // message field length is messageType length(4 bytes) + messagePayload length(4 bytes) + messagePayload
                        val payloadBytes = ConvertUtils.toByteArray(message.payload)
                        (toByteArrayWithLittleEndian(message.type) +
                                toByteArrayWithLittleEndian(payloadBytes.size) +
                                payloadBytes).let {
                            toByteArrayWithLittleEndian(it.size) + it
                        }
                    } else {
                        toByteArrayWithLittleEndian(0)
                    } +
                    if (mosaics.isNotEmpty()) {
                        var mosaicBytes = ByteArray(0)
                        mosaics.sortedBy { it.mosaicId.fullName }.forEach { mosaic ->
                            val mosaicNameSpaceIdBytes = mosaic.mosaicId.namespaceId.toByteArray(Charsets.UTF_8)
                            val mosaicNameBytes = mosaic.mosaicId.name.toByteArray(Charsets.UTF_8)
                            // mosaic ID structure length is
                            // mosaic namespace length(4 bytes) + mosaic namespace + mosaic name length(4 bytes) + mosaic name
                            val mosaicIdStructureLength =
                                    4 + mosaicNameSpaceIdBytes.size + 4 + mosaicNameBytes.size

                            // mosaic structure length is
                            // mosaic ID structure length(4 bytes) + mosaic id structure + quantity(8 bytes)
                            val mosaicStructureLength =
                                    4 + mosaicIdStructureLength + 8

                            mosaicBytes += toByteArrayWithLittleEndian(mosaicStructureLength) +
                                    toByteArrayWithLittleEndian(mosaicIdStructureLength) +
                                    toByteArrayWithLittleEndian(mosaicNameSpaceIdBytes.size) +
                                    mosaicNameSpaceIdBytes +
                                    toByteArrayWithLittleEndian(mosaicNameBytes.size) +
                                    mosaicNameBytes +
                                    toByteArrayWithLittleEndian(mosaic.quantity)
                        }
                        toByteArrayWithLittleEndian(mosaics.size) + mosaicBytes
                    } else {
                        toByteArrayWithLittleEndian(0)
                    }
        }
}
