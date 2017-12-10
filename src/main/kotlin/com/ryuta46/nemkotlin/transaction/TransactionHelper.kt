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
import com.ryuta46.nemkotlin.model.*
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
        private const val multisigAggregateModificationFee = 500_000L
        private const val multisigFee = 150_000L
        private const val multisigSignatureFee = 150_000L

        /**
         * Creates XEM transfer transaction object.
         * @param publicKey Public key string of sender account. (Required)
         * @param receiverAddress Receiver address. (Required)
         * @param microNem Micro nem unit XEM. (Required)
         * @param version Network version. (Optional. The default is Main network.)
         * @param message Message payload. (Optional. The default is empty.)
         * @param messageType Message type.(Optional. The default is plain text.)
         * @param fee Micro nem unit transaction fee. if negative value is specified, calculated minimum fee is used. (Optional. The default is -1)
         * @param timestamp Timestamp as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, calculated with the current time is used. (Optional. The default is -1)
         * @param deadline Deadline as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, 1 hour after the timestamp is used. (Optional. The default is -1)
         */
        @JvmStatic fun createXemTransferTransactionObject(publicKey: String, receiverAddress: String, microNem: Long,
                                                          version: Version = Version.Main,
                                                          message: String = "", messageType: MessageType = MessageType.Plain,
                                                          fee: Long = -1, timestamp: Int = -1, deadline: Int = -1): TransferTransaction {
            // calculate minimum transaction fee.
            val calculatedFee = when {
                fee >= 0 -> fee
                else -> Math.max(minimumTransferFee, calculateXemTransferFee(microNem) + calculateMessageTransferFee(message))
            }
            val type = TransactionType.Transfer
            val common = GeneralTransaction(
                    type = type.rawValue,
                    version = version.rawValue.shl(24) + type.versionOffset,
                    timeStamp = timestamp,
                    signer = publicKey,
                    fee = calculatedFee,
                    deadline = deadline)

            return TransferTransaction(common,
                    microNem,
                    receiverAddress,
                    emptyList(), // No mosaic attachment
                    Message(message, messageType.rawValue))
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
            return createRequestAnnounce(sender,
                    createXemTransferTransactionObject(sender.publicKeyString,
                            receiverAddress,
                            microNem,
                            version,
                            message, messageType,
                            fee,
                            timestamp, deadline))
        }

        /**
         * Creates Mosaic transfer transaction object.
         * @param publicKey Public key string of sender account. (Required)
         * @param receiverAddress Receiver address. (Required)
         * @param mosaics Mosaic list to transfer. (Required)
         * @param version Network version. (Optional. The default is Main network.)
         * @param message Message payload. (Optional. The default is empty.)
         * @param messageType Message type.(Optional. The default is plain text.)
         * @param fee Micro nem unit transaction fee. if negative value is specified, calculated minimum fee is used. (Optional. The default is -1)
         * @param timestamp Timestamp as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, calculated with the current time is used. (Optional. The default is -1)
         * @param deadline Deadline as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, 1 hour after the timestamp is used. (Optional. The default is -1)
         */
        @JvmStatic fun createMosaicTransferTransactionObject(publicKey: String, receiverAddress: String, mosaics: List<MosaicAttachment>,
                                                             version: Version = Version.Main,
                                                             message: String = "", messageType: MessageType = MessageType.Plain,
                                                             fee: Long = -1, timestamp: Int = -1, deadline: Int = -1): TransferTransaction {
            // calculate minimum transaction fee.
            val calculatedFee = when {
                fee >= 0 -> fee
                mosaics.isEmpty() -> Math.max(minimumTransferFee, calculateXemTransferFee(0) + calculateMessageTransferFee(message))
                else -> {
                    var mosaicTransferFeeTotal = 0L
                    mosaics.forEach {
                        mosaicTransferFeeTotal += calculateMosaicTransferFee(it)
                    }
                    Math.max(minimumTransferFee, mosaicTransferFeeTotal + calculateMessageTransferFee(message))
                }
            }
            val type = TransactionType.Transfer
            val common = GeneralTransaction(
                    type = type.rawValue,
                    version = version.rawValue.shl(24) + type.versionOffset,
                    timeStamp = timestamp,
                    signer = publicKey,
                    fee = calculatedFee,
                    deadline = deadline)

            return TransferTransaction(common,
                    1_000_000L, // amount is always 1,000,000
                    receiverAddress,
                    mosaics.map { Mosaic(MosaicId(it.namespaceId, it.name), it.quantity) },
                    Message(message, messageType.rawValue))
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
            return createRequestAnnounce(sender,
                    createMosaicTransferTransactionObject(sender.publicKeyString,
                            receiverAddress,
                            mosaics,
                            version,
                            message, messageType,
                            fee,
                            timestamp, deadline))
        }


        /**
         * Creates multisig aggregate modification transaction object.
         * @param publicKey Public key string of sender account. (Required)
         * @param version Network version. (Optional. The default is Main network.)
         * @param modifications The list of multisig cosignatory modification. (Optional. The default is empty list.)
         * @param minimumCosignatoriesModification The relative value of minimum cosignatories modification.(Optional. The default is 0)
         * @param fee Micro nem unit transaction fee. if negative value is specified, calculated minimum fee is used. (Optional. The default is -1)
         * @param timestamp Timestamp as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, calculated with the current time is used. (Optional. The default is -1)
         * @param deadline Deadline as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, 1 hour after the timestamp is used. (Optional. The default is -1)
         */
        @JvmStatic fun createMultisigAggregateModificationTransactionObject(publicKey: String, version: Version = Version.Main,
                                                                         modifications: List<MultisigCosignatoryModification> = emptyList(),
                                                                         minimumCosignatoriesModification: Int = 0,
                                                                         fee: Long = -1 , timestamp: Int = -1, deadline: Int = -1): MultisigAggregateModificationTransaction {
            // calculate minimum transaction fee.
            val calculatedFee = when {
                fee >= 0 -> fee
                else -> calculateMultisigAggregateModificationFee()
            }
            val type = TransactionType.MultisigAggregateModificationTransfer
            val common = GeneralTransaction(
                    type = type.rawValue,
                    version = version.rawValue.shl(24) + type.versionOffset,
                    timeStamp = timestamp,
                    signer = publicKey,
                    fee = calculatedFee,
                    deadline = deadline)

            return MultisigAggregateModificationTransaction(common, modifications, MinimumCosignatoriesModification(minimumCosignatoriesModification))
        }

        /**
         * Creates multisig aggregate modification transaction.
         * @param sender Sender account. (Required)
         * @param version Network version. (Optional. The default is Main network.)
         * @param modifications The list of multisig cosignatory modification. (Optional. The default is empty list.)
         * @param minimumCosignatoriesModification The relative value of minimum cosignatories modification.(Optional. The default is 0)
         * @param fee Micro nem unit transaction fee. if negative value is specified, calculated minimum fee is used. (Optional. The default is -1)
         * @param timestamp Timestamp as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, calculated with the current time is used. (Optional. The default is -1)
         * @param deadline Deadline as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, 1 hour after the timestamp is used. (Optional. The default is -1)
         */
        @JvmStatic fun createMultisigAggregateModificationTransaction(sender: Account, version: Version = Version.Main,
                                                                      modifications: List<MultisigCosignatoryModification> = emptyList(),
                                                                      minimumCosignatoriesModification: Int = 0,
                                                                      fee: Long = -1 , timestamp: Int = -1, deadline: Int = -1): RequestAnnounce {
            return createRequestAnnounce(sender,
                    createMultisigAggregateModificationTransactionObject(
                            sender.publicKeyString,
                            version,
                            modifications,
                            minimumCosignatoriesModification,
                            fee,
                            timestamp,
                            deadline))
        }


        /**
         * Creates multisig transaction object.
         * @param publicKey Public key string of sender account. (Required)
         * @param innerTransaction Transfer, importance transfer or aggregate modification transaction.
         * @param version Network version. (Optional. The default is Main network.)
         * @param fee Micro nem unit transaction fee. if negative value is specified, calculated minimum fee is used. (Optional. The default is -1)
         * @param timestamp Timestamp as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, calculated with the current time is used. (Optional. The default is -1)
         * @param deadline Deadline as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, 1 hour after the timestamp is used. (Optional. The default is -1)
         */
        @JvmStatic fun createMultisigTransactionObject(publicKey: String,
                                                    innerTransaction: Transaction,
                                                    version: Version = Version.Main,
                                                    fee: Long = -1 , timestamp: Int = -1, deadline: Int = -1): MultisigTransaction {
            // calculate minimum transaction fee.
            val calculatedFee = when {
                fee >= 0 -> fee
                else -> calculateMultisigFee()
            }
            val inner = when(innerTransaction) {
                is TransferTransaction -> GeneralTransaction(innerTransaction)
                is MultisigAggregateModificationTransaction -> GeneralTransaction(innerTransaction)
                else -> throw IllegalArgumentException("Illegal inner transaction type")
            }


            val type = TransactionType.Multisig
            val common = GeneralTransaction(
                    type = type.rawValue,
                    version = version.rawValue.shl(24) + type.versionOffset,
                    timeStamp = timestamp,
                    signer = publicKey,
                    fee = calculatedFee,
                    deadline = deadline)

            return MultisigTransaction(common, inner, emptyList())
        }

        /**
         * Creates multisig transaction object.
         * @param sender Sender account. (Required)
         * @param innerTransaction Transfer, importance transfer or aggregate modification transaction.
         * @param version Network version. (Optional. The default is Main network.)
         * @param fee Micro nem unit transaction fee. if negative value is specified, calculated minimum fee is used. (Optional. The default is -1)
         * @param timestamp Timestamp as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, calculated with the current time is used. (Optional. The default is -1)
         * @param deadline Deadline as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, 1 hour after the timestamp is used. (Optional. The default is -1)
         */
        @JvmStatic fun createMultisigTransaction(sender: Account,
                                                 innerTransaction: Transaction,
                                                 version: Version = Version.Main,
                                                 fee: Long = -1 , timestamp: Int = -1, deadline: Int = -1): RequestAnnounce {
            return createRequestAnnounce(sender,
                    createMultisigTransactionObject(
                            sender.publicKeyString,
                            innerTransaction,
                            version,
                            fee,
                            timestamp,
                            deadline))
        }


        /**
         * Creates multisig transaction object.
         * @param publicKey Public key string of sender account. (Required)
         * @param otherHash Target transaction hash. (Required)
         * @param multisigAccount Traget multisig account address. (Required)
         * @param version Network version. (Optional. The default is Main network.)
         * @param fee Micro nem unit transaction fee. if negative value is specified, calculated minimum fee is used. (Optional. The default is -1)
         * @param timestamp Timestamp as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, calculated with the current time is used. (Optional. The default is -1)
         * @param deadline Deadline as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, 1 hour after the timestamp is used. (Optional. The default is -1)
         */
        @JvmStatic fun createMultisigSignatureTransactionObject(publicKey: String,
                                                             otherHash: String, multisigAccount: String,
                                                             version: Version = Version.Main,
                                                             fee: Long = -1 , timestamp: Int = -1, deadline: Int = -1): MultisigSignatureTransaction {
            // calculate minimum transaction fee.
            val calculatedFee = when {
                fee >= 0 -> fee
                else -> calculateMultisigSignatureFee()
            }
            val type = TransactionType.MultisigSignature
            val common = GeneralTransaction(
                    type = type.rawValue,
                    version = version.rawValue.shl(24) + type.versionOffset,
                    timeStamp = timestamp,
                    signer = publicKey,
                    fee = calculatedFee,
                    deadline = deadline)
            return MultisigSignatureTransaction(common, TransactionHash(otherHash), multisigAccount)
        }

        /**
         * Creates multisig transaction object.
         * @param sender Sender account. (Required)
         * @param otherHash Target transaction hash. (Required)
         * @param multisigAccount Traget multisig account address. (Required)
         * @param version Network version. (Optional. The default is Main network.)
         * @param fee Micro nem unit transaction fee. if negative value is specified, calculated minimum fee is used. (Optional. The default is -1)
         * @param timestamp Timestamp as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, calculated with the current time is used. (Optional. The default is -1)
         * @param deadline Deadline as the number of seconds elapsed since the creation of the nemesis block. if negative value is specified, 1 hour after the timestamp is used. (Optional. The default is -1)
         */
        @JvmStatic fun createMultisigSignatureTransaction(sender: Account,
                                                          otherHash: String, multisigAccount: String,
                                                          version: Version = Version.Main,
                                                          fee: Long = -1 , timestamp: Int = -1, deadline: Int = -1): RequestAnnounce {
            return createRequestAnnounce(sender,
                    createMultisigSignatureTransactionObject(
                            sender.publicKeyString,
                            otherHash, multisigAccount,
                            version,
                            fee,
                            timestamp,
                            deadline))
        }

        /**
         * Signs the given transaction and creates RequestAnnounse object for transaction.
         * @param sender Sender account.
         * @param transaction GeneralTransaction object.
         */
        @JvmStatic fun createRequestAnnounce(sender: Account, transaction: Transaction): RequestAnnounce {
            // create signature
            val transactionBytes = transaction.byteArray
            val signature = Signer.sign(sender, transactionBytes)
            return RequestAnnounce(toHexString(transactionBytes), toHexString(signature))
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
         * Calculates aggregate modification fee. This has been flat in v0.6.93.
         */
        @JvmStatic fun calculateMultisigAggregateModificationFee(): Long = multisigAggregateModificationFee

        /**
         * Calculates multisig transaction fee.
         */
        @JvmStatic fun calculateMultisigFee(): Long = multisigFee

        /**
         * Calculates multisig transaction fee.
         */
        @JvmStatic fun calculateMultisigSignatureFee(): Long = multisigSignatureFee

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

