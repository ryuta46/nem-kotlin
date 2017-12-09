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

open class TransactionImpl : Transaction {
    override var timeStamp: Int = 0
    override var signature: String = ""
    override var fee: Long = 0
    override var type: Int = 0
    override var deadline: Int = 0
    override var version: Int = 0
    override var signer: String = ""

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
}

class GeneralTransaction : TransactionImpl(){
    // for Transfer
    var amount: Long? = null
    var recipient: String? = null
    var mosaics: List<Mosaic>? = null
    var message: Message? = null

    // for Importance Transfer
    var mode: Int? = null
    var remoteAccount: String? = null

    // for Multisig Aggregate Modification Transfer
    var modifications: List<MultisigCosignatoryModification>? = null
    var minCosignatories: MinimumCosignatoriesModification? = null

    // for Multisig Signature
    var otherHash: TransactionHash? = null
    var otherAccount: String? = null

    // for Multisig
    var otherTrans: GeneralTransaction? = null
    var multisigSignatureTransaction: List<MultisigSignatureTransaction>? = null

    // for Provision Namespace
    var rentalFee: Long? = null
    var rentalFeeSink: String? = null
    var newPart: String? = null
    var parent: String? = null

    // for Mosaic Definition Creation
    var creationFee: Long? = null
    var creationFeeSink: String? = null
    var mosaicDefinition: MosaicDefinition? = null

    // for Mosaic Supply Change
    var supplyType: Int? = null
    var delta: Long? = null
    var mosaicId: MosaicId? = null

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

    val asImportanceTransfer: ImportanceTransferTransaction?
        get() {
            if (type != TransactionType.ImportanceTransfer.rawValue) {
                return null
            }
            return ImportanceTransferTransaction(this,
                    mode ?: 0,
                    remoteAccount ?: "")
        }

    val asMultisigAggregateModificationTransfer: MultisigAggregateModificationTransaction?
        get() {
            if (type != TransactionType.MultisigAggregateModificationTransfer.rawValue) {
                return null
            }
            return MultisigAggregateModificationTransaction(this,
                    modifications ?: emptyList(),
                    minCosignatories ?: MinimumCosignatoriesModification(0))
        }

    val asMultisigSignature: MultisigSignatureTransaction?
        get(){
            if (type != TransactionType.MultisigSignature.rawValue) {
                return null
            }
            return MultisigSignatureTransaction(this,
                    otherHash ?: TransactionHash(""),
                    otherAccount ?: "")
        }

    val asMutisig: MultisigTransaction?
        get() {
            if (type != TransactionType.Multisig.rawValue) {
                return null
            }
            return MultisigTransaction(this,
                    otherTrans ?: GeneralTransaction(),
                    multisigSignatureTransaction ?: emptyList())
        }

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