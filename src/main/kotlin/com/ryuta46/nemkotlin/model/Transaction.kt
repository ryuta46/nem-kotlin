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

import com.ryuta46.nemkotlin.enums.MessageType
import com.ryuta46.nemkotlin.enums.TransactionType
import com.ryuta46.nemkotlin.transaction.TransactionHelper
import com.ryuta46.nemkotlin.util.ConvertUtils
import com.ryuta46.nemkotlin.util.ConvertUtils.Companion.toByteArrayWithLittleEndian


/**
 * @property timeStamp The number of seconds elapsed since the creation of the nemesis block.
 * @property signature The transaction signature.
 * @property fee The fee for the transaction.
 * @property type The transaction type.
 * @property deadline The deadline of the transaction.
 * @property version The version of the structure.
 * @property signer The public key of the account that created the transaction.
 */
interface Transaction {
    val timeStamp: Int
    val signature: String
    val fee: Long
    val type: Int
    val deadline: Int
    val version: Int
    val signer: String
    val byteArray: ByteArray
}

private class EmptyTransaction : Transaction {
    override val timeStamp: Int = 0
    override val signature: String = ""
    override val fee: Long = 0
    override val type: Int = 0
    override val deadline: Int = 0
    override val version: Int = 0
    override val signer: String = ""
    override val byteArray: ByteArray = ByteArray(0)
}


class GeneralTransaction(
        transaction: Transaction = EmptyTransaction(),
        override val timeStamp: Int = transaction.timeStamp,
        override val signature: String = transaction.signature,
        override val fee: Long = transaction.fee,
        override val type: Int = transaction.type,
        override val deadline: Int = transaction.deadline,
        override val version: Int = transaction.version,
        override val signer: String = transaction.signer,

        // for Transfer
        val amount: Long? = null,
        val recipient: String? = null,
        val mosaics: List<Mosaic>? = null,
        val message: Message? = null,
        // for Importance Transfer
        val mode: Int? = null,
        val remoteAccount: String? = null,
        // for Multisig Aggregate Modification Transfer
        val modifications: List<MultisigCosignatoryModification>? = null,
        val minCosignatories: MinimumCosignatoriesModification? = null,
        // for Multisig Signature
        val otherHash: TransactionHash? = null,
        val otherAccount: String? = null,
        // for Multisig
        val otherTrans: GeneralTransaction? = null,
        val multisigSignatureTransaction: List<MultisigSignatureTransaction>? = null,

        // for Provision Namespace
        val rentalFee: Long? = null,
        val rentalFeeSink: String? = null,
        val newPart: String? = null,
        val parent: String? = null,

        // for Mosaic Definition Creation
        val creationFee: Long? = null,
        val creationFeeSink: String? = null,
        val mosaicDefinition: MosaicDefinition? = null,

        // for Mosaic Supply Change
        val supplyType: Int? = null,
        val delta: Long? = null,
        val mosaicId: MosaicId? = null
) : Transaction {

    override val byteArray: ByteArray
        get() {
            val calculatedTimestamp = if (timeStamp >= 0) timeStamp else TransactionHelper.currentTimeFromOrigin()
            // deadline
            val calculatedDeadline = if (deadline >= 0) deadline else calculatedTimestamp + 60 * 60
            val publicKey = ConvertUtils.toByteArray(signer)

            return toByteArrayWithLittleEndian(type) +
                    toByteArrayWithLittleEndian(version) +
                    toByteArrayWithLittleEndian(calculatedTimestamp) +
                    toByteArrayWithLittleEndian(publicKey.size) +
                    publicKey +
                    toByteArrayWithLittleEndian(fee) +
                    toByteArrayWithLittleEndian(calculatedDeadline)
        }

    // for Transfer
    constructor(transaction: TransferTransaction): this(transaction,
            amount = transaction.amount,
            recipient = transaction.recipient,
            mosaics = transaction.mosaics,
            message = transaction.message)

    val asTransfer: TransferTransaction?
        get() {
            if (type != TransactionType.Transfer.rawValue) {
                return null
            }
            return TransferTransaction(this,
                    amount ?: 0L,
                    recipient ?: "",
                    mosaics ?: emptyList(),
                    message ?: Message("", MessageType.Plain.rawValue))
        }

    // for Importance Transfer
    constructor(transaction: ImportanceTransferTransaction): this(transaction,
            mode = transaction.mode,
            remoteAccount = transaction.remoteAccount)

    val asImportanceTransfer: ImportanceTransferTransaction?
        get() {
            if (type != TransactionType.ImportanceTransfer.rawValue) {
                return null
            }
            return ImportanceTransferTransaction(this,
                    mode ?: 0,
                    remoteAccount ?: "")
        }

    // for Multisig Aggregate Modification Transfer
    constructor(transaction: MultisigAggregateModificationTransaction) : this(transaction,
            modifications = transaction.modifications,
            minCosignatories = transaction.minCosignatories)

    val asMultisigAggregateModificationTransfer: MultisigAggregateModificationTransaction?
        get() {
            if (type != TransactionType.MultisigAggregateModificationTransfer.rawValue) {
                return null
            }
            return MultisigAggregateModificationTransaction(this,
                    modifications ?: emptyList(),
                    minCosignatories ?: MinimumCosignatoriesModification(0))
        }

    // for Multisig Signature
    constructor(transaction: MultisigSignatureTransaction) : this(transaction,
            otherHash = transaction.otherHash,
            otherAccount = transaction.otherAccount)

    val asMultisigSignature: MultisigSignatureTransaction?
        get(){
            if (type != TransactionType.MultisigSignature.rawValue) {
                return null
            }
            return MultisigSignatureTransaction(this,
                    otherHash ?: TransactionHash(""),
                    otherAccount ?: "")
        }

    // for Multisig
    constructor(transaction: MultisigTransaction) : this(transaction,
            otherTrans = transaction.otherTrans,
            multisigSignatureTransaction = transaction.multisigSignatureTransaction)

    val asMultisig: MultisigTransaction?
        get() {
            if (type != TransactionType.Multisig.rawValue) {
                return null
            }
            return MultisigTransaction(this,
                    otherTrans ?: GeneralTransaction(),
                    multisigSignatureTransaction ?: emptyList())
        }

    // for Provision Namespace
    constructor(transaction: ProvisionNamespaceTransaction) : this(transaction,
            rentalFee = transaction.rentalFee,
            rentalFeeSink = transaction.rentalFeeSink,
            newPart = transaction.newPart,
            parent = transaction.parent)

    val asProvisionNamespace: ProvisionNamespaceTransaction?
        get() {
            if (type != TransactionType.ProvisionNamespace.rawValue) {
                return null
            }
            return ProvisionNamespaceTransaction(this,
                    rentalFee ?: 0L,
                    rentalFeeSink ?: "",
                    newPart ?: "",
                    parent)
        }

    // for Mosaic Definition Creation
    constructor(transaction: MosaicDefinitionCreationTransaction) : this(transaction,
            creationFee = transaction.creationFee,
            creationFeeSink = transaction.creationFeeSink,
            mosaicDefinition = transaction.mosaicDefinition)

    val asMosaicDefinitionCreation: MosaicDefinitionCreationTransaction?
        get() {
            if (type != TransactionType.MosaicDefinitionCreation.rawValue) {
                return null
            }
            return MosaicDefinitionCreationTransaction(this,
                    creationFee ?: 0L,
                    creationFeeSink ?: "",
                    mosaicDefinition ?: MosaicDefinition("", MosaicId("", ""), "", emptyList()))
        }

    // for Mosaic Supply Change
    constructor(transaction: MosaicSupplyChangeTransaction) : this(transaction,
            supplyType = transaction.supplyType,
            delta = transaction.delta,
            mosaicId = transaction.mosaicId)

    val asMosaicSupplyChange: MosaicSupplyChangeTransaction?
        get() {
            if (type != TransactionType.MosaicSupplyChange.rawValue) {
                return null
            }
            return MosaicSupplyChangeTransaction(this,
                    supplyType ?: 0,
                    delta ?: 0,
                    mosaicId ?: MosaicId("", ""))
        }
}