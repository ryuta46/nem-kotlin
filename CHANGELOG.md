## Version 0.4.2

2018-11-12

* Fix: Sort mosaic list before serialization.

## Version 0.4.1

2018-08-11

* Fix: Overflow during calculating mosaic transfer fee.
* Fix: Set BouncyCastle's priority to high for Java9 or later.

## Version 0.4.0

2018-01-06

* Add: Encryption and decryption feature for a message in a transfer transaction.
* Add: Getting super nodes and test nodes function.

## Version 0.3.1

2018-01-06

* Fix: HttpURLConnectionClient doesn't work on Android when the API level is lower than 24.

## Version 0.3.0

2017-12-12

* Change: Transaction -> GeneralTransaction.
* Add: Multisig related transactions.
* Add: "multisigInfo" parameter in AccountInfo.
* Fix: "pageSize" parameter of accountNamespacePage API in RxNemApiClient doesn't work.

## Version 0.2.2

2017-12-05

* Add: Data models of each transaction type.

## Version 0.2.1

2017-12-03

* Add: Mosaic fullname property.
* Fix: WebSocket client subscribed callback call timing.
* Fix: Type of WebSocket send frame(binary -> text)
* Fix: WebSocket client concurrency.


## Version 0.2.0

2017-11-28

* Add: WebSocket client.
* Add: Account related APIs.
* Fix: Format logs.
* Fix: Report response body even if the response status is not OK(200).
* Fix: Change API return type from XXXArray to List<XXX>.

## Version 0.1.0

2017-11-21

* Initial release