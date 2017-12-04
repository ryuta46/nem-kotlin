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


/**
 * @property timeStamp The number of seconds elapsed since the creation of the nemesis block.
 * @property signature The transaction signature.
 * @property fee The fee for the transaction.
 * @property type The transaction type.
 * @property deadline The deadline of the transaction.
 * @property version The version of the structure.
 * @property signer The public key of the account that created the transaction.
 */
interface CommonTransaction {
    val timeStamp: Int
    val signature: String
    val fee: Long
    val type: Int
    val deadline: Int
    val version: Int
    val signer: String
}

open class CommonTransactionImpl: CommonTransaction {
    override val timeStamp: Int = 0
    override val signature: String = ""
    override val fee: Long = 0
    override val type: Int = 0
    override val deadline: Int = 0
    override val version: Int = 0
    override val signer: String = ""
}

class Transaction : CommonTransactionImpl(){
    // for Transfer
    val amount: Long? = null
    val recipient: String? = null
    val mosaics: List<Mosaic>? = null
    val message: Message? = null

    // for Importance Transfer
    val mode: Int? = null
    val remoteAccount: String? = null

    // for Multisig Aggregate Modification Transfer
    val modifications: List<MultisigCosignatoryModification>? = null
    val minCosignatories: MinimumCosignatoriesModification? = null

    // for Multisig Signature
    val otherHash: TransactionHash? = null
    val otherAccount: String? = null

    // for Multisig
    val otherTrans: Transaction? = null
    val multisigSignatureTransaction: List<MultisigSignatureTransaction>? = null

    // for Provision Namespace
    val rentalFee: Long? = null
    val rentalFeeSink: String? = null
    val newPart: String? = null
    val parent: String? = null

    // for Mosaic Definition Creation
    val creationFee: Long? = null
    val creationFeeSink: String? = null
    val mosaicDefinition: MosaicDefinition? = null

    // for Mosaic Supply Change
    val supplyType: Int? = null
    val delta: Long? = null
    val mosaicId: MosaicId? = null


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
                    otherTrans ?: Transaction(),
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