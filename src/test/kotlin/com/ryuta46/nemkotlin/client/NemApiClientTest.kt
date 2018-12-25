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
package com.ryuta46.nemkotlin.client

import com.google.gson.Gson
import com.ryuta46.nemkotlin.Settings
import com.ryuta46.nemkotlin.TestUtils.Companion.checkResult
import com.ryuta46.nemkotlin.TestUtils.Companion.checkResultIsInsufficientBalance
import com.ryuta46.nemkotlin.TestUtils.Companion.checkResultIsMultisigNotACosigner
import com.ryuta46.nemkotlin.TestUtils.Companion.waitUntil
import com.ryuta46.nemkotlin.TestUtils.Companion.waitUntilNotNull
import com.ryuta46.nemkotlin.account.AccountGenerator
import com.ryuta46.nemkotlin.account.MessageEncryption
import com.ryuta46.nemkotlin.enums.MessageType
import com.ryuta46.nemkotlin.enums.ModificationType
import com.ryuta46.nemkotlin.enums.Version
import com.ryuta46.nemkotlin.exceptions.NetworkException
import com.ryuta46.nemkotlin.model.AccountMetaDataPair
import com.ryuta46.nemkotlin.model.MultisigCosignatoryModification
import com.ryuta46.nemkotlin.model.Transaction
import com.ryuta46.nemkotlin.model.TransactionMetaDataPair
import com.ryuta46.nemkotlin.transaction.MosaicAttachment
import com.ryuta46.nemkotlin.transaction.TransactionHelper
import com.ryuta46.nemkotlin.util.ConvertUtils
import com.ryuta46.nemkotlin.util.ConvertUtils.Companion.toByteArray
import com.ryuta46.nemkotlin.util.StandardLogger
import junit.framework.TestCase.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith


@RunWith(Theories::class)
class NemApiClientTest {
    companion object {
        @DataPoints
        @JvmStatic
        fun getTransferTransactionAnnounceFixture() = arrayOf(
                TransferTransactionAnnounceFixture(1, "", MessageType.Plain, emptyList()),
                TransferTransactionAnnounceFixture(0, "test", MessageType.Plain, emptyList()),
                TransferTransactionAnnounceFixture(0, "TEST ENCRYPT MESSAGE", MessageType.Encrypted, emptyList()),
                TransferTransactionAnnounceFixture(0, "", MessageType.Plain, listOf(MosaicAttachment("nem", "xem", 1, 8_999_999_999L, 6))),
                TransferTransactionAnnounceFixture(0, "", MessageType.Plain, listOf(MosaicAttachment("ttech", "maxdivisibility", 30_000_000_000L, 9_000_000_000L, 6))),

                TransferTransactionAnnounceFixture(0, "", MessageType.Plain,
                        listOf(
                                MosaicAttachment("ename", "ecoin1", 1, 9_000_000_000L, 0),
                                MosaicAttachment("ename", "ecoin0", 1, 9_000_000_000L, 0))),

                TransferTransactionAnnounceFixture(0, "", MessageType.Plain,
                        listOf(
                                MosaicAttachment("ename", "ecoin0", 1, 9_000_000_000L, 0),
                                MosaicAttachment("ename", "ecoin1", 2, 9_000_000_000L, 0))),

                TransferTransactionAnnounceFixture(0, "", MessageType.Plain,
                        listOf(
                                MosaicAttachment("nem", "xem", 1, 8_999_999_999L, 6),
                                MosaicAttachment("ename", "ecoin0", 1, 9_000_000_000L, 0))),

                TransferTransactionAnnounceFixture(0, "", MessageType.Plain,
                        listOf(
                                MosaicAttachment("ename", "ecoin0", 2, 9_000_000_000L, 0),
                                MosaicAttachment("nem", "xem", 1, 8_999_999_999L, 6)))
        )

        @DataPoints @JvmStatic fun getReadMessageFixture() = arrayOf(
                ReadMessageFixture("9eda9271565628765caf51e9c89fadb41ed7413ed94c62e4d75870f1197d3872", "TEST PLAIN TEXT MESSAGE"),
                ReadMessageFixture("e19c81ec1ab9d2c96edd5418933054e2edfedd483d530324acab533153e09db3", "TEST ENCRYPTED MESSAGE")
        )

    }

    private val client: NemApiClient
        get() = NemApiClient(Settings.TEST_HOST, logger = StandardLogger())

    private val mainClient: NemApiClient
        get() = NemApiClient(Settings.MAIN_HOST, logger = StandardLogger())


    private fun <T> printModel(model: T) {
        //val jsonString = GsonBuilder().setPrettyPrinting().create().toJson(model)
        val jsonString = Gson().toJson(model)
        println(jsonString)
    }

    @Test
    fun get() {
        val accountMetaDataPair: AccountMetaDataPair = client.get("/account/get", mapOf("address" to Settings.ADDRESS))

        assertEquals(accountMetaDataPair.account.publicKey, Settings.PUBLIC_KEY)
        assertEquals(accountMetaDataPair.account.address, Settings.ADDRESS)

        printModel(accountMetaDataPair)
    }

    @Test(expected = NetworkException::class)
    fun errorOnNetwork() {
        val client = NemApiClient("http://illegalhostillegalhostillegalhost", logger = StandardLogger())
        client.get<AccountMetaDataPair>("/account/get", mapOf("address" to Settings.ADDRESS))
    }

    /*
    @Test(expected = ParseException::class)
    fun errorOnParseResponse(){
        val client = NemApiClient(TEST_HOST)
        client.get<AccountInfo>("/account/get",mapOf("address" to TEST_ADDRESS))
    }
    */

    @Test
    fun heartbeat() {
        val result = client.heartbeat()
        assertEquals(1, result.code)
        assertEquals(2, result.type)
        assertEquals("ok", result.message)
        printModel(result)
    }

    @Test
    fun status() {
        val result = client.status()
        assertEquals(6, result.code)
        assertEquals(4, result.type)
        assertEquals("status", result.message)
        printModel(result)
    }

    @Test
    fun accountGet() {
        val accountMetaDataPair = client.accountGet(Settings.ADDRESS)
        printModel(accountMetaDataPair)

        assertEquals(accountMetaDataPair.account.publicKey, Settings.PUBLIC_KEY)
        assertEquals(accountMetaDataPair.account.address, Settings.ADDRESS)
    }

    @Test
    fun accountGetFromPublicKey() {
        val accountMetaDataPair = client.accountGetFromPublicKey(Settings.PUBLIC_KEY)
        printModel(accountMetaDataPair)

        assertEquals(accountMetaDataPair.account.publicKey, Settings.PUBLIC_KEY)
        assertEquals(accountMetaDataPair.account.address, Settings.ADDRESS)
    }


    @Test
    fun accountGetForwarded() {
        // API Document sample request.
        val result = mainClient.accountGetForwarded("NC2ZQKEFQIL3JZEOB2OZPWXWPOR6LKYHIROCR7PK")
        printModel(result)
        assertEquals("NALICE2A73DLYTP4365GNFCURAUP3XVBFO7YNYOW", result.account.address)
    }

    @Test
    fun accountGetForwardedMyself() {
        val result = client.accountGetForwarded(Settings.ADDRESS)
        printModel(result)
        assertEquals(result.account.address, Settings.ADDRESS)
    }

    @Test
    fun accountGetForwardedFromPublicKey() {
        val result = mainClient.accountGetForwardedFromPublicKey("bdd8dd702acb3d88daf188be8d6d9c54b3a29a32561a068b25d2261b2b2b7f02")
        printModel(result)
        assertEquals("NALICE2A73DLYTP4365GNFCURAUP3XVBFO7YNYOW", result.account.address)
    }

    @Test
    fun accountGetForwardedFromPublicKeyMyself() {
        val result = client.accountGetForwardedFromPublicKey(Settings.PUBLIC_KEY)
        printModel(result)
        assertEquals(result.account.address, Settings.ADDRESS)
    }

    @Test
    fun accountStatus() {
        val result = client.accountStatus(Settings.ADDRESS)
        printModel(result)

        assertEquals("LOCKED", result.status)
        assertEquals("INACTIVE", result.remoteStatus)
        assertTrue(result.cosignatoryOf.isNotEmpty())
        assertTrue(result.cosignatories.isEmpty())
    }

    @Test
    fun accountTransfersIncoming() {
        val result = client.accountTransfersIncoming(Settings.ADDRESS)
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            val transfer = it.transaction.asTransfer ?: return@forEach
            assertEquals(Settings.ADDRESS, transfer.recipient)
        }
    }

    @Test
    fun accountTransfersOutgoing() {
        val result = client.accountTransfersOutgoing(Settings.ADDRESS)
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            assertEquals(Settings.PUBLIC_KEY, it.transaction.signer)
        }
    }

    @Test
    fun accountTransfersAll() {
        val result = client.accountTransfersAll(Settings.ADDRESS)
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            val transfer = it.transaction.asTransfer ?: it.transaction.asMultisig?.otherTrans?.asTransfer ?: return@forEach
            assertTrue(Settings.PUBLIC_KEY == transfer.signer || Settings.ADDRESS == transfer.recipient)
        }
    }

    @Test
    fun accountUnconfirmedTransactions() {
        transferTransactionAnnounce(getTransferTransactionAnnounceFixture()[2])
        val result = client.accountUnconfirmedTransactions(Settings.ADDRESS)
        printModel(result)

        if (Settings.PRIVATE_KEY.isEmpty()) {
            assertTrue(result.isEmpty())
        } else {
            assertTrue(result.isNotEmpty())
            result.forEach {
                assertEquals(Settings.PUBLIC_KEY, it.transaction.signer)

            }
        }
    }

    @Test
    fun accountHarvests() {
        val result = client.accountHarvests(Settings.ADDRESS)
        printModel(result)
        assertTrue(result.isEmpty())
    }


    @Test
    fun accountImportances() {
        val result = client.accountImportances()
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            assertTrue(it.address.isNotEmpty())
        }
    }


    @Test
    fun accountNamespacePage() {
        val result = client.accountNamespacePage(Settings.RECEIVER)
        printModel(result)

        assertTrue(result.isNotEmpty())
        result.forEach {
            assertEquals(Settings.RECEIVER, it.owner)
        }
    }

    @Test
    fun accountNamespacePageInvalidParent() {
        val result = client.accountNamespacePage(Settings.RECEIVER, "ttech")
        printModel(result)
        assertTrue(result.isEmpty())
    }

    @Test
    fun accountMosaicOwned() {
        val mosaicArray = client.accountMosaicOwned(Settings.ADDRESS)
        printModel(mosaicArray)
        assertTrue(mosaicArray.isNotEmpty())
    }

    // Account historiacal get does not support in the NIS.
    @Test(expected = NetworkException::class)
    fun accountHistoricalGet() {
        val result = client.accountHistoricalGet(Settings.ADDRESS, 0, 0, 1)
        printModel(result)
    }

    @Test
    fun namespaceMosaicDefinitionPage() {
        val mosaicDefinitionArray = mainClient.namespaceMosaicDefinitionPage("ttech")
        printModel(mosaicDefinitionArray)
        assertTrue(mosaicDefinitionArray.isNotEmpty())
    }

    @Test
    fun namespaceMosaicDefinitionFromName() {
        val mosaicDefinition = mainClient.namespaceMosaicDefinitionFromName("ttech", "ryuta")
        assertNotNull(mosaicDefinition)
        printModel(mosaicDefinition)
    }

    @Test
    fun namespaceMosaicDefinitionFromNameNull() {
        val mosaicDefinition = mainClient.namespaceMosaicDefinitionFromName("ttech", "ryutainvalidinvalid")
        assertNull(mosaicDefinition)
    }


    data class TransferTransactionAnnounceFixture(val xem: Long, val message: String, val messageType: MessageType, val mosaics: List<MosaicAttachment>)

    @Theory
    fun transferTransactionAnnounce(fixture: TransferTransactionAnnounceFixture) {
        val account =
                if (Settings.PRIVATE_KEY.isNotEmpty()) AccountGenerator.fromSeed(ConvertUtils.toByteArray(Settings.PRIVATE_KEY), Version.Test)
                else AccountGenerator.fromRandomSeed(Version.Test)


        val message = when(fixture.messageType) {
            MessageType.Plain -> fixture.message.toByteArray(Charsets.UTF_8)
            MessageType.Encrypted -> {
                MessageEncryption.encrypt(account,
                        ConvertUtils.toByteArray(Settings.RECEIVER_PUBLIC),
                        fixture.message.toByteArray(Charsets.UTF_8))
            }
        }


        val fee = TransactionHelper.createMosaicTransferTransactionObject(account.publicKeyString, Settings.RECEIVER, fixture.mosaics, Version.Test, message, fixture.messageType).fee
        print(fee)
        val request = when {
            fixture.mosaics.isNotEmpty() -> TransactionHelper.createMosaicTransferTransaction(account, Settings.RECEIVER, fixture.mosaics, Version.Test, message, fixture.messageType, timestamp = client.networkTime().receiveTimeStampBySeconds)
            else -> TransactionHelper.createXemTransferTransaction(account, Settings.RECEIVER, fixture.xem * 1_000_000L, Version.Test, message, fixture.messageType, timestamp = client.networkTime().receiveTimeStampBySeconds)
        }


        val result = client.transactionAnnounce(request)
        printModel(result)

        if (Settings.PRIVATE_KEY.isNotEmpty()) {
            checkResult(result)
        } else {
            checkResultIsInsufficientBalance(result)
        }
    }


    @Test
    fun multisigAggregateModificationTransactionMyself() {
        val multisig = AccountGenerator.fromRandomSeed(Version.Test)

        println(multisig.privateKeyString)
        println(multisig.publicKeyString)
        println(multisig.address)

        if (Settings.PRIVATE_KEY.isEmpty()) {
            val ownerAccount = AccountGenerator.fromRandomSeed(Version.Test)
            val multisigRequest = TransactionHelper.createMultisigAggregateModificationTransaction(multisig, Version.Test,
                    modifications = listOf(MultisigCosignatoryModification(ModificationType.Add.rawValue, ownerAccount.publicKeyString)),
                    minimumCosignatoriesModification = 1,
                    timestamp = client.networkTime().receiveTimeStampBySeconds)

            val multisigResult = client.transactionAnnounce(multisigRequest)
            printModel(multisigResult)
            checkResultIsInsufficientBalance(multisigResult)
            return
        }

        val account = AccountGenerator.fromSeed(ConvertUtils.toByteArray(Settings.PRIVATE_KEY), Version.Test)

        // first, transfer xem to create transaction
        val request = TransactionHelper.createXemTransferTransaction(account, multisig.address, TransactionHelper.calculateMultisigAggregateModificationFee(), Version.Test, timestamp = client.networkTime().receiveTimeStampBySeconds)
        val result = client.transactionAnnounce(request)
        printModel(result)
        checkResult(result)

        // wait for transaction confirmed
        waitUntil(10 * 60 * 1000) {
            val transactions = client.accountTransfersIncoming(multisig.address)
            if (transactions.isEmpty()) {
                Thread.sleep(1 * 60 * 1000)
                false
            } else {
                true
            }
        }


        // second, create multisig transaction
        val modificationRequest = TransactionHelper.createMultisigAggregateModificationTransaction(multisig, Version.Test,
                modifications = listOf(MultisigCosignatoryModification(ModificationType.Add.rawValue, account.publicKeyString)),
                timestamp = client.networkTime().receiveTimeStampBySeconds)

        val modificationResult = client.transactionAnnounce(modificationRequest)
        printModel(modificationResult)
        checkResult(modificationResult)

        // wait for transaction confirmed
        val transactions = waitUntilNotNull(10 * 60 * 1000) {
            val transactions = client.accountTransfersOutgoing(multisig.address)
            if (transactions.isNotEmpty()) transactions else {
                Thread.sleep(1 * 60 * 1000)
                null
            }
        }
        assertNotNull(transactions)

        val multisigAccountInfo = client.accountGet(multisig.address)
        printModel(multisigAccountInfo)
        assertEquals(account.address, multisigAccountInfo.meta.cosignatories.first().address)
    }

    @Test fun multisigAggregateModificationTransaction() {
        val account =
                if (Settings.PRIVATE_KEY.isNotEmpty()) AccountGenerator.fromSeed(toByteArray(Settings.PRIVATE_KEY), Version.Test)
                else AccountGenerator.fromRandomSeed(Version.Test)
        val signer =
                if (Settings.SIGNER_PRIVATE_KEY.isNotEmpty()) AccountGenerator.fromSeed(toByteArray(Settings.SIGNER_PRIVATE_KEY), Version.Test)
                else AccountGenerator.fromRandomSeed(Version.Test)

        run {
            // Create inner transaction of which deletes signer and decrements minimum cosignatory.
            val modificationTransaction = TransactionHelper.createMultisigAggregateModificationTransactionObject(Settings.MULTISIG_PUBLIC_KEY, Version.Test,
                    modifications = listOf(MultisigCosignatoryModification(ModificationType.Delete.rawValue, signer.publicKeyString)),
                    minimumCosignatoriesModification = -1,
                    timestamp = client.networkTime().receiveTimeStampBySeconds)

            // Create multisig transaction
            val multisigRequest = TransactionHelper.createMultisigTransaction(account, modificationTransaction, Version.Test,
                    timestamp = client.networkTime().receiveTimeStampBySeconds)
            val multisigResult = client.transactionAnnounce(multisigRequest)

            printModel(multisigResult)
            if (Settings.PRIVATE_KEY.isEmpty() || Settings.SIGNER_PRIVATE_KEY.isEmpty()) {
                checkResultIsMultisigNotACosigner(multisigResult)
                return
            } else {
                checkResult(multisigResult)
            }


            // Sign the transaction
            val unconfirmedTransactions = client.accountUnconfirmedTransactions(signer.address)
            printModel(unconfirmedTransactions)

            val hash = unconfirmedTransactions.first().meta.data
            val signatureRequest = TransactionHelper.createMultisigSignatureTransaction(signer, hash, Settings.MULTISIG_ADDRESS, Version.Test, timestamp = client.networkTime().receiveTimeStampBySeconds)
            val signatureResult = client.transactionAnnounce(signatureRequest)
            printModel(signatureResult)
            checkResult(multisigResult)

            println("... Waiting for transaction confirmed of aggregate modification ...")

            // wait for transaction confirmed
            val transaction = waitUntilNotNull(10 * 60 * 1000) {
                val transactions = client.accountTransfersOutgoing(account.address)
                transactions.forEach { transaction ->
                    if (transaction.meta.hash == multisigResult.transactionHash) {
                        return@waitUntilNotNull transaction
                    }
                }
                Thread.sleep(30 * 1000)
                return@waitUntilNotNull null
            }
            assertNotNull(transaction)

            val multisigAccountInfo = client.accountGet(Settings.MULTISIG_ADDRESS)
            printModel(multisigAccountInfo)

            assertEquals(1, multisigAccountInfo.account.multisigInfo.minCosignatories)
            assertNotNull(multisigAccountInfo.meta.cosignatories.find { it.address == account.address })
            assertNull(multisigAccountInfo.meta.cosignatories.find { it.address == signer.address })
        }
        run {
            // Create inner transaction of which deletes signer and decrements minimum cosignatory.
            val modificationTransaction = TransactionHelper.createMultisigAggregateModificationTransactionObject(Settings.MULTISIG_PUBLIC_KEY, Version.Test,
                    modifications = listOf(MultisigCosignatoryModification(ModificationType.Add.rawValue, signer.publicKeyString)),
                    minimumCosignatoriesModification = 1,
                    timestamp = client.networkTime().receiveTimeStampBySeconds)

            // Create multisig transaction
            val multisigRequest = TransactionHelper.createMultisigTransaction(account, modificationTransaction, Version.Test, timestamp = client.networkTime().receiveTimeStampBySeconds)
            val multisigResult = client.transactionAnnounce(multisigRequest)

            printModel(multisigResult)
            checkResult(multisigResult)

            println("... Waiting for transaction confirmed of aggregate modification ...")

            // wait for transaction confirmed
            val transaction = waitUntilNotNull(10 * 60 * 1000) {
                val transactions = client.accountTransfersOutgoing(account.address)
                transactions.forEach { transaction ->
                    if (transaction.meta.hash == multisigResult.transactionHash) {
                        return@waitUntilNotNull transaction
                    }
                }
                Thread.sleep(30 * 1000)
                return@waitUntilNotNull null
            }
            assertNotNull(transaction)

            val multisigAccountInfo = client.accountGet(Settings.MULTISIG_ADDRESS)
            printModel(multisigAccountInfo)

            assertEquals(2, multisigAccountInfo.account.multisigInfo.minCosignatories)
            assertNotNull(multisigAccountInfo.meta.cosignatories.find { it.address == account.address })
            assertNotNull(multisigAccountInfo.meta.cosignatories.find { it.address == signer.address })
        }
    }

    @Test fun multisigSignatureTransaction() {
        val account =
                if (Settings.PRIVATE_KEY.isNotEmpty()) AccountGenerator.fromSeed(toByteArray(Settings.PRIVATE_KEY), Version.Test)
                else AccountGenerator.fromRandomSeed(Version.Test)
        val signer =
                if (Settings.SIGNER_PRIVATE_KEY.isNotEmpty()) AccountGenerator.fromSeed(toByteArray(Settings.SIGNER_PRIVATE_KEY), Version.Test)
                else AccountGenerator.fromRandomSeed(Version.Test)

        // Create inner transaction of which transfers XEM
        val transferTransaction = TransactionHelper.createXemTransferTransactionObject(Settings.MULTISIG_PUBLIC_KEY,
                Settings.ADDRESS, 10, Version.Test,
                timestamp = client.networkTime().receiveTimeStampBySeconds)

        // Create multisig transaction
        val multisigRequest = TransactionHelper.createMultisigTransaction(account, transferTransaction, Version.Test,
                timestamp = client.networkTime().receiveTimeStampBySeconds)
        val multisigResult = client.transactionAnnounce(multisigRequest)
        printModel(multisigResult)

        if (Settings.SIGNER_PRIVATE_KEY.isEmpty()) {
            checkResultIsMultisigNotACosigner(multisigResult)
            return
        }

        if (Settings.PRIVATE_KEY.isEmpty()) {
            checkResultIsInsufficientBalance(multisigResult)
            return
        } else {
            checkResult(multisigResult)
        }

        val unconfirmedTransactions = client.accountUnconfirmedTransactions(signer.address)
        if (Settings.SIGNER_PRIVATE_KEY.isEmpty()) {
            assertTrue(unconfirmedTransactions.isEmpty())
            return
        }
        printModel(unconfirmedTransactions)

        val hash = unconfirmedTransactions.first().meta.data
        val signatureRequest = TransactionHelper.createMultisigSignatureTransaction(signer, hash, Settings.MULTISIG_ADDRESS, Version.Test,
                timestamp = client.networkTime().receiveTimeStampBySeconds)

        val signatureResult = client.transactionAnnounce(signatureRequest)

        printModel(signatureResult)
        checkResult(signatureResult)
    }


    data class ReadMessageFixture(val transactionHash: String, val expected: String)


    // read message
    @Theory fun readMessage(fixture: ReadMessageFixture) {

        var id = -1
        var transaction: TransactionMetaDataPair? = null
        do {
            val transactions = client.accountTransfersIncoming(Settings.ADDRESS, id = id)
            transactions.forEach {
                if (it.meta.hash.data == fixture.transactionHash) {
                    transaction = it
                    return@forEach
                }
            }
            id = transactions.lastOrNull()?.meta?.id ?: break
        } while(transaction == null)

        assertNotNull(transaction)

        val message = transaction!!.transaction.asTransfer?.message
        assertNotNull(message)

        val actual =
                if (message!!.type == MessageType.Plain.rawValue) {
                    String(ConvertUtils.toByteArray(message.payload), Charsets.UTF_8)
                } else {
                    val account = if (Settings.PRIVATE_KEY.isNotEmpty()) {
                        AccountGenerator.fromSeed(toByteArray(Settings.PRIVATE_KEY), Version.Test)
                    } else {
                        return@readMessage
                    }
                    val decryptedBytes = MessageEncryption.decrypt(account, ConvertUtils.toByteArray(Settings.RECEIVER_PUBLIC), ConvertUtils.toByteArray(message.payload))
                    String(decryptedBytes, Charsets.UTF_8)
                }

        assertEquals(fixture.expected, actual)
    }

    @Test
    fun networkTime() {
        val result = client.networkTime()
        val localTimeStamp = TransactionHelper.currentTimeFromOrigin()
        val serverTimeStamp = result.receiveTimeStampBySeconds

        println("serverTimeStamp: $serverTimeStamp")
        println("localTimeStamp : $localTimeStamp")

        val timeDiff = Math.abs(serverTimeStamp - localTimeStamp)

        assertTrue(timeDiff < 3600)
    }
}