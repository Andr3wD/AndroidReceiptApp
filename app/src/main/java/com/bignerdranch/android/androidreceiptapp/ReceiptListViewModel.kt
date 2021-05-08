package com.bignerdranch.android.androidreceiptapp

import androidx.lifecycle.ViewModel

class ReceiptListViewModel : ViewModel() {

    private val receiptRepository = ReceiptRepository.get()
    var receiptListLiveData = receiptRepository.getReceipts()

    fun deleteReceipt(receipt: Receipt) {
        receiptRepository.deleteReceipt(receipt)
    }

    fun loadReceipts() {
        receiptListLiveData = receiptRepository.getReceipts()
    }

    /*
    fun addReceipt(receipt: Receipt) {
        receiptRepository.addReceipt(receipt)
    }
    */
}