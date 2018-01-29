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

import com.ryuta46.nemkotlin.exceptions.EncryptionException
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.math.GroupElement
import org.spongycastle.crypto.BufferedBlockCipher
import org.spongycastle.crypto.DataLengthException
import org.spongycastle.crypto.InvalidCipherTextException
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.modes.CBCBlockCipher
import org.spongycastle.crypto.paddings.PKCS7Padding
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.crypto.params.ParametersWithIV
import java.security.SecureRandom
import kotlin.experimental.xor

/**
 * Encryption and Description functions for transfer transaction message.
 */
class MessageEncryption {
    companion object {
        private val KEY_LENGTH = 256 / 8
        private val BLOCK_SIZE = AESEngine().blockSize
        private val RANDOM = SecureRandom()

        @JvmStatic private val hashInitializer by lazy { Hash.initialize() }

        /**
         * Encrypts message bytes.
         * @param account The sender account.
         * @param receiverPublicKey The public key of the receiver.
         * @return Encrypted message.
         * @throws EncryptionException If description of the given message has failed.
         */
        @JvmStatic fun encrypt(account: Account, receiverPublicKey: ByteArray, message: ByteArray): ByteArray {
            hashInitializer
            val salt = ByteArray(receiverPublicKey.size)
            RANDOM.nextBytes(salt)

            try {
                val sharedKey = calculateSharedKey(account, receiverPublicKey, salt)
                return salt + encrypt(message, sharedKey)
            } catch (e: Throwable) {
                throw EncryptionException("Failed to decrypt message: ${e.message}")
            }
        }

        @JvmStatic private fun encrypt(message: ByteArray, key: ByteArray): ByteArray {
            if (key.size != KEY_LENGTH) {
                throw EncryptionException("Invalid key length.")
            }

            // Setup IV.
            val iv = ByteArray(BLOCK_SIZE)
            RANDOM.nextBytes(iv)

            // Setup block cipher.
            val cipher = setupBlockCipher(key, iv, true)
            return iv + transform(cipher, message)
        }

        /**
         * Decrypts message bytes.
         * @param account The receiver account.
         * @param senderPublicKey The public key of the sender.a
         * @return Decrypted message bytes.
         * @throws EncryptionException If description of the given message has failed.
         */
        @JvmStatic fun decrypt(account: Account, senderPublicKey: ByteArray, mergedEncryptedMessage: ByteArray): ByteArray {
            hashInitializer
            val salt = mergedEncryptedMessage.copyOfRange(0, senderPublicKey.size)
            val encryptedBody = mergedEncryptedMessage.copyOfRange(salt.size, mergedEncryptedMessage.size)

            try {
                val sharedKey = calculateSharedKey(account, senderPublicKey, salt)
                return decrypt(encryptedBody, sharedKey)
            } catch (e: Throwable) {
                throw EncryptionException("Failed to decrypt message: ${e.message}")
            }
        }

        @JvmStatic private fun decrypt(encryptedBody: ByteArray, key: ByteArray): ByteArray {
            if (key.size != KEY_LENGTH) {
                throw EncryptionException("Invalid key length.")
            }

            // Setup IV.
            val iv = encryptedBody.copyOfRange(0, BLOCK_SIZE)
            val encryptedMessage = encryptedBody.copyOfRange(iv.size, encryptedBody.size)

            // Setup block cipher.
            val cipher = setupBlockCipher(key, iv, false)
            return transform(cipher, encryptedMessage)
        }


        @JvmStatic private fun calculateSharedKey(account: Account, receiverPublicKey: ByteArray, salt: ByteArray): ByteArray {
            val sharedKey = ByteArray(receiverPublicKey.size)

            // Get private key and curve parameter
            val a = (account.keyPair.private as EdDSAPrivateKey).geta()
            val curve = (account.keyPair.private as EdDSAPrivateKey).params.curve

            // Calculate a(Alice) * A(Bob)
            val groupElement = GroupElement(curve, receiverPublicKey)
            groupElement.precompute(true)
            val g = groupElement.scalarMultiply(a).toByteArray()

            (0 until sharedKey.size).forEach {
                sharedKey[it] = salt[it] xor g[it]
            }
            return Hash.sha3_256(sharedKey)
        }

        @JvmStatic private fun setupBlockCipher(sharedKey: ByteArray, ivData: ByteArray, forEncryption: Boolean): BufferedBlockCipher {

            // Setup cipher parameters with key and IV.
            val keyParam = KeyParameter(sharedKey)
            val params = ParametersWithIV(keyParam, ivData)

            // Setup AES cipher in CBC mode with PKCS7 padding.
            val padding = PKCS7Padding()
            val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(AESEngine()), padding)
            cipher.reset()
            cipher.init(forEncryption, params)
            return cipher
        }


        @JvmStatic private fun transform(cipher: BufferedBlockCipher, data: ByteArray): ByteArray {
            val buf = ByteArray(cipher.getOutputSize(data.size))
            var length = cipher.processBytes(data, 0, data.size, buf, 0)
            try {
                length += cipher.doFinal(buf, length)
            } catch (e: Exception) {
                val message = when(e) {
                            is IllegalArgumentException -> "Illegal argument"
                            is DataLengthException -> "Buffer was too small"
                            is InvalidCipherTextException -> "Invalid cipher text"
                            else -> throw e
                        }
                throw EncryptionException(message)
            }

            return buf.copyOf(length)
        }

    }
}
