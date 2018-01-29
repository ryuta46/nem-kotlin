
Read this in other languages: [English](README.md), [日本語](README.ja.md)

# nem-kotlin

nem-kotlin は NEM(New Economy Movement) のAPIを簡単に使うためのライブラリです.

このライブラリは、NIS(NEM Infrastructure Server) に対してのHTTPリクエストと、NISからのHTTPのレスポンスのラッパーとして機能します。

また、このライブラリではキーペアの作成や署名、署名検証といった暗号化関連のユーティリティも提供します。

## サンプル

サンプルのプロジェクトが [samples](samples) ディレクトリにあります。ご参照ください。

## セットアップ方法

### jar ファイルのダウンロード

最新の jar をダウンロードしてください。

gradle を使う場合: (gradle のバージョン 2.x を使う場合は、 'implmentaion' のかわりに 'compile' を指定してください)

```gradle
implementation 'com.ryuta46:nem-kotlin:0.4.0'
```

maven を使う場合:

```xml
<dependency>
  <groupId>com.ryuta46</groupId>
  <artifactId>nem-kotlin</artifactId>
  <version>0.4.0</version>
</dependency>
```


## 使用方法

### アカウント作成

'AccountGenerator' で NEM のアカウントを作成できます。ネットワークバージョン(Main network か Test network か) を指定してください.

```kotlin
val account = AccountGenerator.fromRandomSeed(Version.Main)
```

秘密鍵がすでにあるのであれば、その情報からアカウントを生成することも出来ます。
```kotlin
val account = AccountGenerator.fromSeed(ConvertUtils.toByteArray("YOUR_PRIVATE_KEY"), Version.Main)
```

秘密鍵のバイト列をスワップして読み込む必要があるかもしれません。(e.g. NanoWalletが生成した秘密鍵を使う場合など。)
このライブラリが要求する秘密鍵のバイト列表現のエンディアンが異なっている場合があるためです.

```kotlin
val account = AccountGenerator.fromSeed(ConvertUtils.swapByteArray(ConvertUtils.toByteArray("NANO_WALLET_PRIVATE_KEY")), Version.Main)
```

### APIクライアントのセットアップ

NEM の API クライアント作成時に、NIS の URL を指定します。

```kotlin
val client = NemApiClient("http://62.75.251.134:7890")
```

'NemApiClient' は同期クライアントです。そのため、このクライアントでは各APIの呼び出し時、NISからレスポンスが得られるまで呼び出し元スレッドをブロックします。

Reactive なクライアントを使いたい場合は、'RxNemApiClient' を使います。
'RxNemApiClient' は NemApiClient と同名のメソッドを持ちますが、戻り値の型が Observable になっています。

```kotlin
val rxClient = RxNemApiClient("http://62.75.251.134:7890")
```

### ノード情報の取得

下記のように処理を行うことで、API クライアントのセットアップに必要なスーパーノードの一覧を取得することができます。
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

`getSuperNodes()` は、ノードのリストをサーバ( デフォルトは "https://supernodes.nem.io/nodes/")から取得する関数ですので、非同期の処理となっています。.

`getTestNodes()` を使うことで、テストネット用のノードの情報を取得する事もできます。
この関数は固定のノード情報を返すだけですので、同期的に処理を行います。

### アカウント情報の取得

API クライアントは NEM の API に対応したメソッドを持っています。

アカウントの情報を取得する場合は下記のようにします。

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

### XEM、モザイクの送金

送金など、署名が必要なトランザクションの生成は 'TransactionHelper' を使います。

XEM 送金をする場合
```kotlin
val transaction = TransactionHelper.createXemTransferTransaction(account, receiverAddress, amount)
val result = client.transactionAnnounce(transaction)
```

上記で指定している amount はマイクロ NEM 単位であることに注意してください。( 1 XEM = 1,000,000 マイクロ NEM)

モザイク送信をする場合
```kotlin
val transaction = TransactionHelper.createMosaicTransferTransaction(account, receiverAddress,
    listOf(MosaicAttachment(mosaicNamespaceId, mosaicName, quantity, mosaicSupply, mosaicDivisibility))
)
val result = client.transactionAnnounce(transaction)
```

モザイクの供給量や可分性は、最低手数料を計算する際に用いられます。

もしそれらの値が不明な場合は、'namespaceMosaicDefinitionFromName' を使って取得することが出来ます。

```kotlin
val response = client.namespaceMosaicDefinitionFromName(namespaceId, name)
if (response != null) {
    supply = response.mosaic.initialSupply!!
    divisibility = response.mosaic.divisibility!!
}
```

### メッセージの送受信

平文のメッセージを含めて XEM 送信を行う場合
```kotlin
val message = "message".toByteArray(Charsets.UTF_8)
val transaction = TransactionHelper.createXemTransferTransaction(account, receiverAddress, amount,
        message = message,
        messageType = MessageType.Plain)
val result = client.transactionAnnounce(transaction)
```

暗号化メッセージを含めて XEM 送信を行う場合
```kotlin
val message = MessageEncryption.encrypt(account, receiverPublicKey, "message".toByteArray(Charsets.UTF_8))
val transaction = TransactionHelper.createXemTransferTransaction(account, receiverAddress, amount,
        message = message,
        messageType = MessageType.Encrypted)
val result = client.transactionAnnounce(transaction)
```

受信したトランザクションからメッセージを読み取るには、下記のように実装します。
```
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


### マルチシグ関連

マルチシグ関連のトランザクション(MultisigTransaction, MultisigSignatureTransaction, MultisigAggreageModificationTransaction) も TransactionHelper で作成します。

アカウントをマルチシグアカウントに変更する場合
```kotlin
val multisigRequest = TransactionHelper.createMultisigAggregateModificationTransaction(account,
    modifications = listOf(MultisigCosignatoryModification(ModificationType.Add.rawValue, signerAccount.publicKeyString)),
    minimumCosignatoriesModification = 1)

val multisigResult = client.transactionAnnounce(multisigRequest)
```
**マルチシグアカウントを通常アカウントに戻す方法はありませんのでご注意ください。**

マルチシグアカウントからXEMを送金する場合
```kotlin
// Create inner transaction of which transfers XEM
val transferTransaction = TransactionHelper.createXemTransferTransactionObject(multisigAccountPublicKey, receiverAddress, amount)

// Create multisig transaction
val multisigRequest = TransactionHelper.createMultisigTransaction(signerAccount, transferTransaction)
val multisigResult = client.transactionAnnounce(multisigRequest)
```

さらに、そのトランザクションに署名を行う場合
```kotlin
val signatureRequest = TransactionHelper.createMultisigSignatureTransaction(anotherSignerAccount, innerTransactionHash, multisigAccountAddress)
val signatureResult = client.transactionAnnounce(signatureRequest)
```

innerTransactionHash は `client.accountUnconfirmedTransactions(anotherSignerAddress)` にて取得できます。

### その他の API

もし使いたい API に対応するメソッドが定義されていない場合は、'get' と 'post' メソッドを使うことができます。

```kotlin
data class HarvestInfoArray(val data: List<HarvestInfo>)
...
val harvests: HarvestInfoArray = client.get("/account/harvests/", mapOf("address" to account.address")
```

### API クライアントの動作のカスタマイズ

'HttpClient' インターフェイスを実装することで、独自の HTTP クライアントを使うことが出来ます。

また、'Logger' インターフェイスを実装すことで、システム依存のログ機能(android.util.Log など) を使うこともできます。

これらは、クライアントのコンストラクタで指定します。
```kotlin
val client = NemApiClient("http://62.75.251.134:7890", yourHttpClient, yourLogger)
```

デフォルトの HTTP クライアントは 'HttpURLConnectionClient' で、'HttpURLConnection' を使っています。

デフォルトの Logger は 'NoOutputLogger' で、何も出力をしません。

標準出力にログを出力する 'StandardOutputLogger' を使うことも出来ます。


### WebSocket クライアントの使い方

WebSocket クライアントを使うことができます。

```kotlin
val wsClient = RxNemWebSocketClient("http://62.75.251.134:7778")
```

WebSocket クライアントでは、各 API に対応した Observable を返します。

各 API に対応した情報が更新される度に、Observable を通して通知が行われます。

例) モザイクの保有量が変わる度に、モザイク保有量を表示
```kotlin
val subscription = wsClient.accountMosaicOwned(address)
                .subscribeOn(Schedulers.newThread())
                .subscribe { mosaic: Mosaic ->
                    print(Gson().toJson(mosaic))
                }
```

**通知が必要なくなったら必ず dispose を呼び出してください。**

自動的に通知が完了するということはありません。

```kotlin
// Do not forget to dispose the subscription after you don't need to observe it.
subscription.dispose()
```

## 開発者

[Taizo Kusuda](https://ryuta46.com)

Twitter [@ryuta461](https://twitter.com/ryuta461)

寄付アドレス:
NAEZYI6YPR4YIRN4EAWSP3GEYU6ATIXKTXSVBEU5
