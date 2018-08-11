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

import com.ryuta46.nemkotlin.Settings
import com.ryuta46.nemkotlin.enums.Version
import com.ryuta46.nemkotlin.util.ConvertUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

@RunWith(Theories::class)
class MessageEncryptionTest {
    companion object {
        @DataPoints
        @JvmStatic fun decryptFixtures() = arrayOf(
                DecryptFixture("f05b30eb18f1243f90e170b376e3fd61a903d202aad70af6b8f1eb3afce0274372022d55953b3ae0524635680af686e6a2fe6934e47b000d2433fbf84e8a42b0d0f35bf387569eac17221a5af9721cdd", "TEST ENCRYPT MESSAGE")
        )
    }

    data class DecryptFixture(val message: String, val expected: String)


    @Test fun validatePair() {
        val sender = AccountGenerator.fromRandomSeed(Version.Main)
        val receiver = AccountGenerator.fromRandomSeed(Version.Main)

        val message = "TEST MESSAGE"

        val encryptedMessage = MessageEncryption.encrypt(sender, receiver.publicKey, message.toByteArray(Charsets.UTF_8))

        val decryptedMessage = MessageEncryption.decrypt(receiver, sender.publicKey, encryptedMessage)

        val actual = String(decryptedMessage, Charsets.UTF_8)

        assertEquals(message, actual)
    }

    @Theory fun decrypt(fixture: DecryptFixture) {
        if (Settings.PRIVATE_KEY.isEmpty()) {
            return
        }
        val account = AccountGenerator.fromSeed(ConvertUtils.toByteArray(Settings.PRIVATE_KEY), Version.Test)
        val decryptedMessage = MessageEncryption.decrypt(account, ConvertUtils.toByteArray(Settings.RECEIVER_PUBLIC), ConvertUtils.toByteArray(fixture.message))

        val actual = String(decryptedMessage, Charsets.UTF_8)
        assertEquals(fixture.expected, actual)
    }



}

