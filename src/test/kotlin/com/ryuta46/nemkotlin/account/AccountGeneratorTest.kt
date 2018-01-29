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
import com.ryuta46.nemkotlin.TestUtils.Companion.printModel
import com.ryuta46.nemkotlin.client.NemApiClient
import com.ryuta46.nemkotlin.enums.Version
import com.ryuta46.nemkotlin.util.ConvertUtils
import com.ryuta46.nemkotlin.util.StandardLogger
import junit.framework.TestCase.assertEquals
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

@RunWith(Theories::class)
class AccountGeneratorTest {
    companion object {
        @DataPoints @JvmStatic fun getFromSeedFixtures() = arrayOf(
                FromSeedFixture("c0786ab5bf1052330122fb862bd6ce4cc2b1aa486030d634fa7efe3ac3b70c2d", Version.Main, "d033867885270eb9013376d6614939188faa0a8ba1fa538c460fa44f82efc478", "NCCRHLLID4JQNVQHXCANFIGAYWFNS65FRSIPS2O6"),
                FromSeedFixture("0f3928e8aa57f53b0e77c412be66547b2b1ef28eff58e8403c12025d50c66209", Version.Main, "c2e19751291d01140e62ece9ee3923120766c6302e1099b04014fe1009bc89d3", "NCKMNCU3STBWBR7E3XD2LR7WSIXF5IVJIACOVP6B")
        )
        @DataPoints @JvmStatic fun getCalculateAddressFromFixtures() = arrayOf(
                CalculateAddressFixture("3f9f8c791f4b55c84278c10c7596f959a43a71dc35888d16e3d26a33456b6df2", Version.Main, "NCNFK2ULFDYWIDSS4VKGK2PQHUJWP5V7M2RLKWDN"),
                CalculateAddressFixture("13394e3a7bba1b41be79e51476c2be76fd42c28ad6bfcb8efb85325f4ad77de6", Version.Main, "NCUK4VQHA4OSEXD5K2TKBEE2722PCXAEQ3SPTDBJ")
        )
    }

    data class FromSeedFixture(val seed: String, val version: Version, val expectedPublicKey: String, val expectedAddress: String)

    private val mainClient: NemApiClient
        get() = NemApiClient(Settings.MAIN_HOST, logger = StandardLogger())


    @Theory fun fromRandom() {
        val actual = AccountGenerator.fromRandomSeed(Version.Main)
        printModel(mainClient.accountGet(actual.address))
    }

    @Theory fun fromSeed(fixture: FromSeedFixture) {
        val actual = AccountGenerator.fromSeed(ConvertUtils.toByteArray(fixture.seed), fixture.version)
        assertEquals(fixture.expectedPublicKey, actual.publicKeyString)
        assertEquals(fixture.expectedAddress, actual.address)
    }

    data class CalculateAddressFixture(val publicKey: String, val version: Version, val expected: String)

    @Theory fun calculateAddress(fixture: CalculateAddressFixture) {
        val actual = AccountGenerator.calculateAddress(ConvertUtils.toByteArray(fixture.publicKey), fixture.version)
        assertEquals(fixture.expected, actual)
    }
}

