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
 * Accounts can rent a namespace for one year and after a year renew the contract. This is done via a ProvisionNamespaceTransaction.
 *
 * @property rentalFee The fee for renting the namespace.
 * @property rentalFeeSink The public key of the account to which the rental fee is transferred.
 * @property newPart The new part which is concatenated to the parent with a '.' as separator.
 * @property parent The parent namespace. This can be null if the transaction rents a root namespace.
 */
class ProvisionNamespaceTransaction(common: Transaction,
                                    val rentalFee: Long,
                                    val rentalFeeSink: String,
                                    val newPart: String,
                                    val parent: String? = null
) : Transaction by common


