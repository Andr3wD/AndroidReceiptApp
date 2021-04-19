package com.bignerdranch.android.androidreceiptapp

import androidx.lifecycle.ViewModel

class ReceiptListViewModel : ViewModel() {

    private val receiptRepository = ReceiptRepository.get()
    val receiptListLiveData = receiptRepository.getReceipts()

    /*
    fun addReceipt(receipt: Receipt) {
        receiptRepository.addReceipt(receipt)
    }
    */
}