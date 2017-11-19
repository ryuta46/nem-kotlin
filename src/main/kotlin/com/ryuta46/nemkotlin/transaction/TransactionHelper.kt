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
package com.ryuta46.nemkotlin.transaction

import com.ryuta46.nemkotlin.account.Account
import com.ryuta46.nemkotlin.account.Signer
import com.ryuta46.nemkotlin.enums.MessageType
import com.ryuta46.nemkotlin.enums.TransactionType
import com.ryuta46.nemkotlin.enums.Version
import com.ryuta46.nemkotlin.model.RequestAnnounce
import com.ryuta46.nemkotlin.util.ConvertUtils.Companion.toByteArrayWithLittleEndian
import com.ryuta46.nemkotlin.util.ConvertUtils.Companion.toHexString
import java.util.*

/**
 * Transaction helper functions.
 */
class TransactionHelper {
    companion object {

        private const val minimumTransferFee = 50_000L
        private const val maximumXemTransferFee = 1_250_000L
        private const val transferFeeFactor = 50_000L

        private fun commonTransactionBytes(
                type: TransactionType,
                version: Version,
                timestamp: Int,
                publicKey: ByteArray,
                fee: Long,
                deadline: Int) : ByteArray {
            // timestamp
            val calculatedTimestamp = if (timestamp >= 0) timestamp else currentTimeFromOrigin()
            // deadline
            val calculatedDeadline = if (deadline >= 0) deadline else calculatedTimestamp + 60 * 60

                return toByteArrayWithLittleEndian(type.rawValue) +
                        toByteArrayWithLittleEndian(version.rawValue.shl(24) + type.versionOffset) +
                        toByteArrayWithLittleEndian(calculatedTimestamp) +
                        toByteArrayWithLittleEndian(publicKey.size) +
                        publicKey +
                        toByteArrayWithLittleEndian(fee) +
                        toByteArrayWithLittleEndian(calculatedDeadline)
        }

        private fun createTransferTransaction(sender: Account, receiverAddress: String, microNem: Long,
                                                 version: Version, message: String, messageType: MessageType,
                                                 fee: Long, timestamp: Int, deadline: Int, mosaics: List<MosaicAttachment>): RequestAnnounce {

            // calculate minimum transaction fee.
            val calculatedFee = when {
                fee >= 0 -> fee
                mosaics.isEmpty() -> Math.max(minimumTransferFee, calculateXemTransferFee(microNem) + calculateMessageTransferFee(message))
                else -> {
                    var mosaicTransferFeeTotal = 0L
                    mosaics.forEach {
                        mosaicTransferFeeTotal += calculateMosaicTransferFee(it)
                    }
                    Math.max(minimumTransferFee, mosaicTransferFeeTotal + calculateMessageTransferFee(message))
                }
            }

            // create transaction bytes
            val transactionBytes = commonTransactionBytes(
                    type = TransactionType.Transfer,
                    version = version,
                    timestamp = timestamp,
                    publicKey = sender.publicKey,
                    fee = calculatedFee,
                    deadline = deadline) +
                    toByteArrayWithLittleEndian(receiverAddress.length) +
                    receiverAddress.toByteArray(Charsets.UTF_8) +
                    toByteArrayWithLittleEndian(microNem) +
                    if (message.isNotEmpty()) {
                        // message field length is messageType length(4 bytes) + messagePayload length(4 bytes) + messagePayload
                        val payloadBytes = message.toByteArray(Charsets.UTF_8)
                        val fieldLength = 4 + 4 + payloadBytes.size
                        toByteArrayWithLittleEndian(fieldLength) +
                                toByteArrayWithLittleEndian(messageType.rawValue) +
                                toByteArrayWithLittleEndian(payloadBytes.size) +
                                payloadBytes
                    } else {
                        toByteArrayWithLittleEndian(0)
                    } +
                    if (mosaics.isNotEmpty()) {
                        var mosaicBytes = ByteArray(0)
                        mosaics.forEach { mosaic ->
                            val mosaicNameSpaceIdBytes = mosaic.namespaceId.toByteArray(Charsets.UTF_8)
                            val mosaicNameBytes = mosaic.name.toByteArray(Charsets.UTF_8)
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

            // create signature
            val signature = Signer.sign(sender, transactionBytes)
            return RequestAnnounce(toHexString(transactionBytes), toHexString(signature))
        }

        /**
         * Creates XEM transfer transaction.
         * @param sender Sender account. (Required)
         * @param receiverAddress Receiver address. (Required)
         * @param microNem Micro nem unit XEM. (Required)
         * @param version Network version. (Optional. The default is Main network.)
         * @param message Message payload. (Optional. The default is empty.)
         * @param messageType Message type.(Optional. The default is plain text.)
         * @param fee Micro nem unit transaction fee. if negative value is specified, calculated minimum fee is used. (Optional. The default is -1)
         * @param timestamp Timestamp as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, calculated with the current time is used. (Optional. The default is -1)
         * @param deadline Deadline as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, 1 hour after the timestamp is used. (Optional. The default is -1)
         */
        @JvmStatic fun createXemTransferTransaction(sender: Account, receiverAddress: String, microNem: Long,
                                                    version: Version = Version.Main,
                                                    message: String = "", messageType: MessageType = MessageType.Plain,
                                                    fee: Long = -1, timestamp: Int = -1, deadline: Int = -1): RequestAnnounce {
            return createTransferTransaction(sender, receiverAddress, microNem,
                    version, message, messageType, fee, timestamp, deadline, emptyList()) // mosaic list is empty.
        }

        /**
         * Creates Mosaic transfer transaction.
         * @param sender Sender account. (Required)
         * @param receiverAddress Receiver address. (Required)
         * @param mosaics Mosaic list to transfer. (Required)
         * @param version Network version. (Optional. The default is Main network.)
         * @param message Message payload. (Optional. The default is empty.)
         * @param messageType Message type.(Optional. The default is plain text.)
         * @param fee Micro nem unit transaction fee. if negative value is specified, calculated minimum fee is used. (Optional. The default is -1)
         * @param timestamp Timestamp as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, calculated with the current time is used. (Optional. The default is -1)
         * @param deadline Deadline as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, 1 hour after the timestamp is used. (Optional. The default is -1)
         */
        @JvmStatic fun createMosaicTransferTransaction(sender: Account, receiverAddress: String, mosaics: List<MosaicAttachment>,
                                                    version: Version = Version.Main,
                                                    message: String = "", messageType: MessageType = MessageType.Plain,
                                                    fee: Long = -1, timestamp: Int = -1, deadline: Int = -1): RequestAnnounce {

            return createTransferTransaction(sender, receiverAddress, 1_000_000L, // amount is always 1,000,000
                    version, message, messageType, fee, timestamp, deadline, mosaics)
        }



        /**
         * Calculates transfer fee of xem. The input and the output are micro nem unit.
         * @param microNem Micro nem unit amount of XEM.
         * @return Micro nem unit transfer fee.
         */
        @JvmStatic fun calculateXemTransferFee(microNem: Long): Long = Math.max(minimumTransferFee, Math.min((microNem / 10_000_000_000L) * transferFeeFactor, maximumXemTransferFee))

                /**
         * Calculates message transfer fee. The output is micro nem unit.
         * @param message The message.
         * @return Micro nem unit transfer fee.
         */
        @JvmStatic fun calculateMessageTransferFee(message: String): Long {
            return if (message.isNotEmpty()) {
                transferFeeFactor * (1L + message.toByteArray(Charsets.UTF_8).size / 32L)
            } else {
                0L
            }
        }

        /**
         * Calculates mosaic transfer fee. The output is micro nem unit.
         * @param mosaic Mosaic information attached to the transaction.
         * @return Micro nem unit transfer fee.
         */
        @JvmStatic fun calculateMosaicTransferFee(mosaic: MosaicAttachment): Long {
            return if ( mosaic.divisibility == 0 && mosaic.supply < 10_000 ) {
                transferFeeFactor
            } else {
                val maxMosaicQuantity = 9_000_000_000_000_000L
                val totalMosaicQuantity = mosaic.supply * Math.pow(10.0, mosaic.divisibility.toDouble())

                val supplyRelatedAdjustment = Math.floor(0.8 * Math.log(maxMosaicQuantity / totalMosaicQuantity)).toLong()
                val xemEquivalent = (8_999_999_999L * mosaic.quantity) / ( mosaic.supply * Math.pow(10.0, mosaic.divisibility.toDouble()) )
                val microNemEquivalentFee = calculateXemTransferFee((xemEquivalent * Math.pow(10.0, 6.0)).toLong())

                Math.max(transferFeeFactor, microNemEquivalentFee - transferFeeFactor * supplyRelatedAdjustment)
            }
        }

        /**
         * Calculates current number of seconds elapsed since the creation of the nemesis block.
         * @return elapsed seconds
         */
        @JvmStatic fun currentTimeFromOrigin(): Int {
            // The nemesis block is created at 2015/03/29 0:06:25 UTC
            val origin = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            origin.set(2015, 2, 29, 0, 6, 25)

            val current = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

            // Convert milli seconds to seconds
            return Math.floor(((current.time.time - origin.time.time) / 1000.0)).toInt()
        }
    }

}

