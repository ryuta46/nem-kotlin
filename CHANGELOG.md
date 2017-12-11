
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