
Read this in other languages: [English](README.md), [日本語](README.ja.md)

# nem-kotlin

nem-kotlin is a client library for easy use of NEM(New Economy Movement) API.

This library wraps HTTP requests to NIS(NEM Infrastructure Server) and HTTP responses from NIS.

This library also provides crypt related utilities like key pair generation signing and verifying.

## Sample

Sample projects are in [samples](samples) directory.

## How to setup

### Download jar

Download the latest jar

for gradle users: (If you use gradle versioned 2.x, specify 'compile' instead of 'implmentaion')

```gradle
implementation 'com.ryuta46:nem-kotlin:0.2.0'
```




for maven users:

```xml
<dependency>
  <groupId>com.ryuta46</groupId>
  <artifactId>nem-kotlin</artifactId>
  <version>0.2.0</version>
</dependency>
```

### Setup depended libraries

nem-kotlin depends gson, spongy castle and eddsa library.

If you want to use reactive client, download RxJava too.

If you want to use WebSocket client, download RxJava and Java-WebSocket too.

To use nem-kotlin, download them

for gradle users:

```gradle
implementation 'com.madgag.spongycastle:prov:1.51.0.0'
implementation 'com.madgag.spongycastle:core:1.51.0.0'
implementation 'net.i2p.crypto:eddsa:0.2.0'
implementation 'com.google.code.gson:gson:2.8.2'

// for reactive client or WebSocket client users
implementation 'io.reactivex.rxjava2:rxandroid:2.0.1'
implementation 'io.reactivex.rxjava2:rxkotlin:2.1.0'

// for WebSocket client users
implementation 'org.java-websocket:Java-WebSocket:1.3.6'
```

for maven users:

```xml
<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
  <version>2.8.2</version>
</dependency>
<dependency>
  <groupId>com.madgag.spongycastle</groupId>
  <artifactId>prov</artifactId>
  <version>1.51.0.0</version>
</dependency>
<dependency>
  <groupId>com.madgag.spongycastle</groupId>
  <artifactId>core</artifactId>
  <version>1.51.0.0</version>
</dependency>
<dependency>
  <groupId>net.i2p.crypto</groupId>
  <artifactId>eddsa</artifactId>
  <version>0.2.0</version>
</dependency>
<dependency>
  <groupId>io.reactivex.rxjava2</groupId>
  <artifactId>rxjava</artifactId>
  <version>2.1.0</version>
</dependency>
<dependency>
  <groupId>io.reactivex.rxjava2</groupId>
  <artifactId>rxkotlin</artifactId>
  <version>2.1.0</version>
</dependency>
<dependency>
  <groupId>org.java-websocket</groupId>
  <artifactId>Java-WebSocket</artifactId>
  <version>1.3.6</version>
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

### Sending XEM and Mosaics

TransactionHelper is an utility to create transactions which required account signing.

To send XEM,
```kotlin
val transaction = TransactionHelper.createXemTransferTransaction(account, receiverAddress, amount)
val result = client.transactionAnnounce(transaction)
```
Note that the amount specified above is micro nem unit. ( 1 XEM = 1,000,000 micro nem)

To send mosaic,
```kotlin
val transaction = TransactionHelper.createMosaicTransferTransaction(account, receiverAddress,
    listOf(MosaicAttachment(mosaicNamespaceId, mosaicName, quantity, mosaicSupply, mosaicDivisibility))
)
val result = client.transactionAnnounce(transaction)
```

Mosaic's supply and divisibility are used to calculate minimum transaction fee.

You can get these parameters of mosaic with 'namespaceMosaicDefinitionFromName' if you don't know them.

```kotlin
val response = client.namespaceMosaicDefinitionFromName(namespaceId, name)
if (mosaicDefinition != null) {
    supply = response.mosaic.initialSupply!!
    divisibility = response.mosaic.divisibility!!
}
```

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

