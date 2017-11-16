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
 * Nodes can support a feature for retrieving historical data of accounts.
 * If a node supports this feature, it will return an array of AccountHistoricalDataViewModel objects.
 *
 * @property height The height for which the data is valid.
 * @property address The address of the account.
 * @property balance The balance of the account.
 * @property vestedBalance The vested part of the balance.
 * @property unvestedBalance The unvested part of the balance.
 * @property importance The importance of the account.
 * @property pageRank The page rank part of the importance.
 */
data class AccountHistoricalDataViewModel(
        val height: Int,
        val address: String,
        val balance: Long,
        val vestedBalance: Long,
        val unvestedBalance: Long,
        val importance: Double,
        val pageRank: Double
)