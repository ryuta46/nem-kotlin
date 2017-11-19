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

import com.ryuta46.nemkotlin.util.ConvertUtils
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import java.security.KeyPair

/**
 * An account of NEM
 * @property address Account address.
 * @property privateKeyString Hex string expression of private key seed.
 * @property publicKey Public key raw byte array.
 * @property publicKeyString Hex string expression of public key.
 * @property walletCompatiblePrivateKey Hex string expression of private key seed to import to Nano Wallet.
 */
data class Account(val keyPair: KeyPair, val address: String) {
    val privateKeyString: String
        get() = ConvertUtils.toHexString((keyPair.private as EdDSAPrivateKey).seed)

    val publicKey: ByteArray
        get() = (keyPair.public as EdDSAPublicKey).abyte

    val publicKeyString: String
        get() = ConvertUtils.toHexString(publicKey)

    val walletCompatiblePrivateKey: String
        get() = ConvertUtils.toHexString(ConvertUtils.swapByteArray((keyPair.private as EdDSAPrivateKey).seed))
}