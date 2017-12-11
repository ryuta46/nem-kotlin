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
 * The account structure describes basic information for an account.
 *
 * @property address The address of the account.
 * @property balance The balance of the account in micro NEM.
 * @property vestedBalance The vested part of the balance of the account in micro NEM.
 * @property importance The importance of the account.
 * @property publicKey The public key of the account.
 * @property label The label of the account (not used, always null).
 * @property harvestedBlocks The number blocks that the account already harvested.
 */
data class AccountInfo(
        val address: String,
        val balance: Long,
        val vestedBalance: Long,
        val importance: Double,
        val publicKey: String,
        val label: String?,
        val harvestedBlocks: Int,
        val multisigInfo: MultisigInfo = MultisigInfo()
)
