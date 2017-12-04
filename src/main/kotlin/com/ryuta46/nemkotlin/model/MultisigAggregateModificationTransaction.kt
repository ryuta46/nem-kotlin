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
 * Multisig aggregate modification transactions are part of the NEM's multisig account system.
 * A multisig aggregate modification transaction holds an array of multisig cosignatory modifications and a single multisig minimum cosignatories modification inside the transaction.
 * A multisig aggregate modification transaction can be wrapped by a multisig transaction. Aggregate modification transactions that use the minCosignatories field need to have version 0x68000002 (decimal 1744830466) for mainnet and 0x98000002 (decimal -1744830462) for testnet.
 *
 * @property modifications An array of multisig modifications.
 * @property minCosignatories The minimum cosignatories modification.
 */
class MultisigAggregateModificationTransaction(common: CommonTransaction,
                                               val modifications: List<MultisigCosignatoryModification>,
                                               val minCosignatories: MinimumCosignatoriesModification
) : CommonTransaction by common