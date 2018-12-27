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
package com.ryuta46.nemkotlinandroidsample

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.google.gson.GsonBuilder
import com.ryuta46.nemkotlin.account.Account
import com.ryuta46.nemkotlin.account.AccountGenerator
import com.ryuta46.nemkotlin.client.RxNemApiClient
import com.ryuta46.nemkotlin.enums.Version
import com.ryuta46.nemkotlin.model.MosaicDefinitionMetaDataPair
import com.ryuta46.nemkotlin.transaction.MosaicAttachment
import com.ryuta46.nemkotlin.transaction.TransactionHelper
import com.ryuta46.nemkotlin.util.ConvertUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    companion object {
        private const val KEY_PRIVATE_KEY = "privateKey"
    }

    @BindView(R.id.textAddress) lateinit var textAddress: TextView
    @BindView(R.id.textMessage) lateinit var textMessage: TextView
    @BindView(R.id.buttonAccountInfo) lateinit var buttonAccountInfo: Button
    @BindView(R.id.buttonSendXem) lateinit var buttonSendXem: Button
    @BindView(R.id.buttonMosaicInfo) lateinit var buttonMosaicInfo: Button
    @BindView(R.id.buttonSendMosaic) lateinit var buttonSendMosaic: Button

    private lateinit var account: Account

    private val client = RxNemApiClient("https://nistest.ttechdev.com:7891", logger = AndroidLogger())

    private val mosaicNamespaceId = "ename"
    private val mosaicName = "ecoin0"
    private var mosaicSupply: Long = 0
    private var mosaicDivisibility: Int = 0

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ButterKnife.bind(this)

        // Account generation.
        val savedPrivateKey = loadPrivateKey()
        if (savedPrivateKey.isEmpty()) {
            account = AccountGenerator.fromRandomSeed(Version.Test)
            savePrivateKey(account.privateKeyString)
        } else {
            account = AccountGenerator.fromSeed(ConvertUtils.toByteArray(savedPrivateKey), Version.Test)
        }

        textAddress.text = account.address

        setupView()
        setupListeners()
    }

    private fun setupView() {
        fetchAccountInfo()
        fetchMosaicDefinition(mosaicNamespaceId, mosaicName)
    }
    private fun setupListeners() {
        buttonAccountInfo.setOnClickListener {
            textMessage.text = ""
            fetchAccountInfo()
        }

        buttonSendXem.setOnClickListener {
            val view = layoutInflater.inflate(R.layout.dialog_send_xem, null)
            AlertDialog.Builder(this)
                    .setView(view)
                    .setPositiveButton("OK") { _, _ ->
                        textMessage.text = ""
                        val addressEdit: EditText = view.findViewById(R.id.editTextAddress)
                        val xemEdit: EditText =  view.findViewById(R.id.editTextMicroNem)

                        try {
                            val microNem = xemEdit.text.toString().toLong()
                            fetchTimeStamp {timeStamp ->
                                sendXem(addressEdit.text.toString(), microNem, timeStamp)
                            }

                        } catch (e: NumberFormatException) {
                            return@setPositiveButton
                        }
                    }
                    .setNegativeButton("CANCEL") { _, _ -> }
                    .show()

        }
        buttonMosaicInfo.setOnClickListener {
            textMessage.text = ""
            showMessageOnResponse(client.accountMosaicOwned(account.address))
        }

        buttonSendMosaic.setOnClickListener {
            val view = layoutInflater.inflate(R.layout.dialog_send_xem, null)
            val message: TextView = view.findViewById(R.id.labelXem)
            message.text = "Mosaic quantity"

            AlertDialog.Builder(this)
                    .setView(view)
                    .setPositiveButton("OK") { _, _ ->
                        textMessage.text = ""
                        val addressEdit: EditText = view.findViewById(R.id.editTextAddress)
                        val xemEdit: EditText =  view.findViewById(R.id.editTextMicroNem)

                        try {
                            val microNem = xemEdit.text.toString().toLong()
                            fetchTimeStamp {timeStamp ->
                                sendMosaic(addressEdit.text.toString(), microNem, timeStamp)
                            }

                        } catch (e: NumberFormatException) {
                            return@setPositiveButton
                        }
                    }
                    .setNegativeButton("CANCEL") { _, _ -> }
                    .show()


        }
    }

    private fun fetchAccountInfo() {
        showMessageOnResponse(client.accountGet(account.address))
    }

    private fun fetchTimeStamp(handler: (timeStamp: Int) -> Unit) {
        compositeDisposable.add(client.networkTime()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {response ->
                    handler(response.receiveTimeStampBySeconds)
                }
        )
    }

    private fun fetchMosaicDefinition(namespaceId: String, name: String) {
        compositeDisposable.add(client.namespaceMosaicDefinitionFromName(namespaceId, name)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext{ _: Throwable -> Observable.empty<MosaicDefinitionMetaDataPair>() }
                .subscribe { response: MosaicDefinitionMetaDataPair ->
                    mosaicSupply = response.mosaic.initialSupply!!
                    mosaicDivisibility = response.mosaic.divisibility!!
                })
    }

    private fun sendXem(receiverAddress: String, microNem: Long, timeStamp: Int) {
        val transaction = TransactionHelper.createXemTransferTransaction(account, receiverAddress, microNem, Version.Test, timeStamp = timeStamp)
        showMessageOnResponse(client.transactionAnnounce(transaction))
    }


    private fun sendMosaic(receiverAddress: String, quantity: Long, timeStamp: Int) {
        val transaction = TransactionHelper.createMosaicTransferTransaction(account, receiverAddress,
                listOf(MosaicAttachment(mosaicNamespaceId, mosaicName, quantity, mosaicSupply, mosaicDivisibility)),
                Version.Test, timeStamp = timeStamp)

        showMessageOnResponse(client.transactionAnnounce(transaction))
    }

    private fun <T> showMessageOnResponse(observable: Observable<T>) {
        compositeDisposable.add(observable.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext{ _: Throwable -> Observable.empty<T>() }
                .subscribe { response: T ->
                    val jsonString = GsonBuilder().setPrettyPrinting().create().toJson(response)
                    textMessage.text = jsonString
                })
    }

    private fun loadPrivateKey() : String {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        return pref.getString(KEY_PRIVATE_KEY, "")
    }

    private fun savePrivateKey(value: String) {
        // TODO: You should encrypt private key and save.
        val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
        editor.putString(KEY_PRIVATE_KEY, value)
        editor.apply()
    }
}
