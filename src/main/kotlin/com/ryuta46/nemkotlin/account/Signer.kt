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

package com.ryuta46.nemkotlin.account

import net.i2p.crypto.eddsa.EdDSAEngine
import java.security.SecureRandom

/**
 * Signing and Verifying functions
 */
class Signer {
    companion object {
        /**
         * Calculates signature of message byte array with given account.
         * @param account The account.
         * @param message Input message byte array.
         * @return Signature.
         */
        @JvmStatic fun sign(account: Account, message: ByteArray): ByteArray {
            val engine = EdDSAEngine()
            engine.initSign(account.keyPair.private)

            return engine.signOneShot(message)
        }

        /**
         * Verify signature.
         * @param account The account.
         * @param message Input message byte array.
         * @param signature Signature.
         * @return Whether the signature is correct.
         */
        @JvmStatic fun verify(account: Account, message: ByteArray, signature: ByteArray): Boolean {
            val engine = EdDSAEngine()
            engine.initVerify(account.keyPair.public)

            return engine.verifyOneShot(message, signature)
        }

        /**
         * Validate key pair of given account.
         * @param account The account.
         * @return Whether the key pair is correct.
         */
        @JvmStatic fun validateKeyPair(account: Account): Boolean {
            // create random byte array to validate signature.
            val message = ByteArray(256)
            SecureRandom().nextBytes(message)

            val signature = sign(account, message)
            return verify(account, message, signature)
        }
    }

}
