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

import com.ryuta46.nemkotlin.enums.Version
import com.ryuta46.nemkotlin.util.ConvertUtils
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.KeyPairGenerator
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import java.security.KeyPair
import java.security.SecureRandom

/**
 * Account generation functions
 */
class AccountGenerator {
    companion object {
        @JvmStatic private val hashInitializer by lazy { Hash.initialize() }
        /**
         * Generates new account from random seed.
         * @param version Network version.
         */
        @JvmStatic fun fromRandomSeed(version: Version) : Account {
            hashInitializer
            val privateKeySeed = ByteArray(32)
            SecureRandom().nextBytes(privateKeySeed)
            return fromSeed(privateKeySeed, version)
        }

        /**
         * Retrieves an account from given seed.
         * @param seed Private key seed.
         * @param version Network version.
         */
        @JvmStatic fun fromSeed(seed: ByteArray, version: Version) : Account {
            hashInitializer
            // generate key pair to get curve parameters.
            val tmpPrivateKey = KeyPairGenerator().generateKeyPair().private as EdDSAPrivateKey
            val param = tmpPrivateKey.params
            // set hash algorithm to Keccak-512(official version of SHA3 512 bit)
            //val paramSpec = EdDSAParameterSpec(param.curve, "Keccak-512", param.scalarOps, param.b)
            val paramSpec = EdDSAParameterSpec(param.curve, "SHA3-512", param.scalarOps, param.b)
            // now create key pair from given seed.
            val privateKey = EdDSAPrivateKey(EdDSAPrivateKeySpec(seed, paramSpec))
            val publicKey = EdDSAPublicKey(EdDSAPublicKeySpec(privateKey.a, privateKey.params))
            val keyPair = KeyPair(publicKey, privateKey)

            val address = calculateAddress(publicKey.abyte, version)

            //CryptUtil.validateKeyPair(kv)
            return Account(keyPair, address)
        }

        /**
         * Calculates account address from public key and network version.
         * @param publicKey Public key of the account.
         * @param version Network version.
         */
        @JvmStatic fun calculateAddress(publicKey: ByteArray, version: Version): String {
            hashInitializer
            // 1. Calculate sha3-256 and ripemd-160 hash from public key.
            var bytes = Hash.ripemd160(Hash.sha3_256(publicKey))
            // 2. Attach network identifier to the head.
            bytes = ByteArray(1 ){_ -> version.rawValue.toByte()} + bytes
            // 3. Calculate sha3-256 and get check sum.
            val checkSum = Hash.sha3_256(bytes).slice(0..3)
            // 4. Attach checkSum to the tail
            bytes += checkSum
            // 5. Encode with Base32
            return ConvertUtils.encodeWithBase32(bytes)
        }
    }
}

