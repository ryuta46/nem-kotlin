
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
implementation 'com.ryuta46:nem-kotlin:0.1.0'
```

maven を使う場合:

```xml
<dependency>
  <groupId>com.ryuta46</groupId>
  <artifactId>nem-kotlin</artifactId>
  <version>0.1.0</version>
</dependency>
```

### 依存ライブラリのセットアップ

nem-kotlin は gson と spongy castle、eddsa ライブラリに依存しています。

また、Reactive なクライアントを使う場合は、RxJava も必要です。

これら依存ライブラリをセットアップ方法は下記の通り。

gradle を使う場合:

```gradle
implementation 'com.madgag.spongycastle:prov:1.51.0.0'
implementation 'com.madgag.spongycastle:core:1.51.0.0'
implementation 'net.i2p.crypto:eddsa:0.2.0'
implementation 'com.google.code.gson:gson:2.8.2'

// for reactive client users
implementation 'io.reactivex.rxjava2:rxandroid:2.0.1'
implementation 'io.reactivex.rxjava2:rxkotlin:2.1.0'
```

maven を使う場合:

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


### Getting an account information

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

### Sending XEM and Mosaics

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
if (mosaicDefinition != null) {
    supply = response.mosaic.initialSupply!!
    divisibility = response.mosaic.divisibility!!
}
```

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


## 開発者

[Taizo Kusuda](https://ryuta46.com)

Twitter [@ryuta461](https://twitter.com/ryuta461)

