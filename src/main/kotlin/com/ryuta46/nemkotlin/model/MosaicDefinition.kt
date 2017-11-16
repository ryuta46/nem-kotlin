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
 * A mosaic definition describes an asset class.
 *
 * @property creator The public key of the mosaic definition creator.
 * @property id The mosaic id.
 * @property description The mosaic description.
 * @property properties The mosaic properties.
 */
data class MosaicDefinition(
        val creator: String,
        val id: MosaicId,
        val description: String,
        val properties: List<Property>
) {
    val divisibility: Int?
        get() = properties.find { it.name == "divisibility" } ?.value?.toInt()

    val initialSupply: Long?
        get() = properties.find { it.name == "initialSupply" } ?.value?.toLong()

    val supplyMutable: Boolean?
        get() = properties.find { it.name == "supplyMutable" } ?.value?.toBoolean()

    val transferable: Boolean?
        get() = properties.find { it.name == "transferable" } ?.value?.toBoolean()
}
