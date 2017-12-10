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

package com.ryuta46.nemkotlin

class Settings {
    companion object {
        const val MAIN_HOST = "http://62.75.251.134:7890"
        const val TEST_HOST = "http://23.228.67.85:7890"
        //const val TEST_WEB_SOCKET = "http://23.228.67.85:7778"
        const val TEST_WEB_SOCKET = "http://104.128.226.60:7778"
        //const val TEST_HOST = "http://bob.nem.ninja:7778"

        // Test main account
        const val ADDRESS = "TDDYOPCS46Z5STBF3F5OI5PA2JE52JO6XVXICZIR"
        const val PRIVATE_KEY = ""
        const val PUBLIC_KEY = "7dde4bc3e7be43fc42c9579c0425da8528552e8d5f19a2533611b589e576f15f"

        // Multisig account
        const val MULTISIG_ADDRESS = "TA6CNQUQJZ4OIY3W7T5RCSJUYQMCCNR4HKGPUUE6"
        const val MULTISIG_PRIVATE_KEY = ""
        const val MULTISIG_PUBLIC_KEY = "ef60a3aec6fa82f60661958e79f36adaf01378d455367949f827597ea6bceea8"

        // Multisig signer account
        const val SIGNER_ADDRESS = "TCESVNPDCV67YDDBKJQ6OXF5HMZHENY3DO2E662L"
        const val SIGNER_PRIVATE_KEY = ""
        const val SIGNER_PUBLIC_KEY = "c6d0577111a52889e6cb414372a298bfe17fbd6e0d2eaa7437ab3ff7751fdbfa"

        const val RECEIVER = "TCRUHA3423WEYZN64CZ62IVK53VQ5JGIRJT5UMAE"
    }
}