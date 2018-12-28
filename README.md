
Read this in other languages: [English](README.md), [日本語](README.ja.md)

# nem-kotlin

nem-kotlin is a client library for easy use of NEM API.

This library wraps HTTP requests to NIS(NEM Infrastructure Server) and HTTP responses from NIS.

This library also provides crypt related utilities like key pair generation signing and verifying.

## Sample

Sample projects are in [samples](samples) directory.

## How to setup

### Download jar

Download the latest jar

for gradle users: (If you use gradle versioned 2.x, specify 'compile' instead of 'implmentaion')

```gradle
implementation 'com.ryuta46:nem-kotlin:0.5.0'
```

for maven users:

```xml
<dependency>
  <groupId>com.ryuta46</groupId>
  <artifactId>nem-kotlin</artifactId>
  <version>0.5.0</version>
</dependency>
```

## How to use

### Account generation

'AccountGenerator' generates a NEM account. Network version is required( for main network or test network).

```kotlin
val account = AccountGenerator.fromRandomSeed(Version.Main)
```

If you have private key already, retrieve the account from the seed.
```kotlin
val account = AccountGenerator.fromSeed(ConvertUtils.toByteArray("YOUR_PRIVATE_KEY"), Version.Main)
```

Swapping the private key bytes may be needed because the endianness differs from it required in this library. (e.g. In case of using a Nano Wallet's private key.)

```kotlin
val account = AccountGenerator.fromSeed(ConvertUtils.swapByteArray(ConvertUtils.toByteArray("NANO_WALLET_PRIVATE_KEY")), Version.Main)
```

### API client setup

NEM API client is constructed with a NIS URL.

```kotlin
val client = NemApiClient("http://62.75.251.134:7890")
```

'NemApiClient' is synchronous client, so it blocks the caller thread until the response is received.

You can also use reactive client 'RxNemApiClient'.
'RxNemApiClient' has the same methods of NemApiClient, but the return value is an Observable object.
```kotlin
val rxClient = RxNemApiClient("http://62.75.251.134:7890")
```

### Getting super nodes information

You can get super node list as follows
```kotlin
var nodes: List<NodeInfo> = emptyList()
NisUtils.getSuperNodes().subscribe {
    nodes = it
}
....

// Select a node and initialize a client with it.
val node = nodes.first()
val client = NemApiClient("http://${node.ip}:${node.nisPort}")

// Create clients for all nodes
val clients = nodes.map { NemApiClient("http://${it.ip}:${it.nisPort}") }
```

`getSuperNodes()` is asynchronous function because it fetches node list from a server ( The default server URL is "https://supernodes.nem.io/nodes/").

You can also get nodes for test net with `getTestNodes()`.
This function returns nodes list synchronously because the list is fixed.


### Getting an account information

API client has methods corresponding to NEM APIs.

To get an account information,

```kotlin
val accountInfo = client.accountGet(account.address).account
```

```kotlin
rxClient.accountGet(account.address)
    .subscribeOn(Schedulers.newThread())
    .subscribe { response: AccountMetaDataPair ->
          val accountInfo = response.account
    }
```

### Creating a transaction

**When creating a transaction, you should get network time from the NIS and use it as timeStamp.**

First, get network time from the NIS
```kotlin
val networkTime = client.networkTime()
val timeStamp = networkTime.receiveTimeStampBySeconds
```

Next, create a transaction using the acquired network time as timeStamp.
```kotlin
val transaction = TransactionHelper.createXemTransferTransaction(account, receiverAddress, amount, timeStamp = timeStamp)
```

The local time is used when the `timeStamp` parameter is omitted, but it may cause `FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE` error when the transaction is announced.

### Sending XEM and Mosaics

TransactionHelper is an utility to create transactions which required account signing.

To send XEM,
```kotlin
val transaction = TransactionHelper.createXemTransferTransaction(account, receiverAddress, amount, timeStamp = timeStamp)
val result = client.transactionAnnounce(transaction)
```
Note that the amount specified above is micro nem unit. ( 1 XEM = 1,000,000 micro nem)

To send mosaic,
```kotlin
val transaction = TransactionHelper.createMosaicTransferTransaction(account, receiverAddress,
    listOf(MosaicAttachment(mosaicNamespaceId, mosaicName, quantity, mosaicSupply, mosaicDivisibility), timeStamp = timeStamp)
)
val result = client.transactionAnnounce(transaction)
```

Mosaic's supply and divisibility are used to calculate minimum transaction fee.

You can get these parameters of mosaic with 'namespaceMosaicDefinitionFromName' if you don't know them.

```kotlin
val response = client.namespaceMosaicDefinitionFromName(namespaceId, name)
if (response != null) {
    supply = response.mosaic.initialSupply!!
    divisibility = response.mosaic.divisibility!!
}
```

### Sending and Receiving message.

To send XEM with a plain text message,
```kotlin
val message = "message".toByteArray(Charsets.UTF_8)
val transaction = TransactionHelper.createXemTransferTransaction(account, receiverAddress, amount,
        message = message,
        messageType = MessageType.Plain,
        timeStamp = timeStamp)
val result = client.transactionAnnounce(transaction)
```

With a encrypted message,
```kotlin
val message = MessageEncryption.encrypt(account, receiverPublicKey, "message".toByteArray(Charsets.UTF_8))
val transaction = TransactionHelper.createXemTransferTransaction(account, receiverAddress, amount,
        message = message,
        messageType = MessageType.Encrypted,
        timeStamp = timeStamp)
val result = client.transactionAnnounce(transaction)
```

You can read message from a transaction as follows
```kotlin
val message = transaction.asTransfer?.message ?: return

if (message.type == MessageType.Plain.rawValue) {
    // for plain text message
    val plainTextMessage = String(ConvertUtils.toByteArray(message.payload), Charsets.UTF_8)
} else {
    // for encrypted text message
    val decryptedBytes = MessageEncryption.decrypt(account, senderPublicKey, ConvertUtils.toByteArray(message.payload))
    val encryptedTextMessage = String(decryptedBytes, Charsets.UTF_8)
}
```


### Multisig related transactions

Multisig related transactions(MultisigTransaction, MultisigSignatureTransaction, MultisigAggreageModificationTransaction) are also created by TransactionHelper.

To change an account to multisig account,
```kotlin
val multisigRequest = TransactionHelper.createMultisigAggregateModificationTransaction(account,
    modifications = listOf(MultisigCosignatoryModification(ModificationType.Add.rawValue, signerAccount.publicKeyString)),
    minimumCosignatoriesModification = 1,
    timeStamp = timeStamp)

val multisigResult = client.transactionAnnounce(multisigRequest)
```
**Note that there is no way to change multisig account to normal account.**

To send XEM from multisig account,
```kotlin
// Create inner transaction of which transfers XEM
val transferTransaction = TransactionHelper.createXemTransferTransactionObject(multisigAccountPublicKey, receiverAddress, amount, timeStamp = timeStamp)

// Create multisig transaction
val multisigRequest = TransactionHelper.createMultisigTransaction(signerAccount, transferTransaction, timeStamp = timeStamp)
val multisigResult = client.transactionAnnounce(multisigRequest)
```

And to sign the transaction,
```kotlin
val signatureRequest = TransactionHelper.createMultisigSignatureTransaction(anotherSignerAccount, innerTransactionHash, multisigAccountAddress, timeStamp = timeStamp)
val signatureResult = client.transactionAnnounce(signatureRequest)
```

You can get innerTransactionHash with `client.accountUnconfirmedTransactions(anotherSignerAddress)`

### More APIs

If there is no method corresponding to the api you want to use, you can use 'get' or 'post' method of the client.

```kotlin
data class HarvestInfoArray(val data: List<HarvestInfo>)
...
val harvests: HarvestInfoArray = client.get("/account/harvests/", mapOf("address" to account.address")
```

### Customizing API client behavior

You can use custom HTTP client to communicate with NIS by implementing 'HttpClient' interface.

And you can also use system depended log functionality(e.g. android.util.Log) by implementing 'Logger' interface.

To use them, specify them in constructing client object
```kotlin
val client = NemApiClient("http://62.75.251.134:7890", yourHttpClient, yourLogger)
```

The default HTTP client is 'HttpURLConnectionClient' which uses 'HttpURLConnection'.

The default logger is 'NoOutputLogger' which outputs nothing.

You can also use 'StandardOutputLogger' which output log to standard output.


### How to use WebSocket client

You can use WebSocket client.

```kotlin
val wsClient = RxNemWebSocketClient("http://62.75.251.134:7778")
```

WebSocket client returns Observable object with each APIs.

The Information corresponding to each API is notified through the observable each time the information has changed.

e.g. Printing owned mosaic information each time the amount of owned mosaic has changed.
```kotlin
val subscription = wsClient.accountMosaicOwned(address)
                .subscribeOn(Schedulers.newThread())
                .subscribe { mosaic: Mosaic ->
                    print(Gson().toJson(mosaic))
                }
```

**Note that you MUST dispose the subscription after you don't need to observe it.**

The subscription is NOT completed automatically.

```kotlin
// Do not forget to dispose the subscription after you don't need to observe it.
subscription.dispose()
```

## Author

[Taizo Kusuda](https://ryuta46.com)

Twitter [@ryuta461](https://twitter.com/ryuta461)

Donation Address:
NAEZYI6YPR4YIRN4EAWSP3GEYU6ATIXKTXSVBEU5
