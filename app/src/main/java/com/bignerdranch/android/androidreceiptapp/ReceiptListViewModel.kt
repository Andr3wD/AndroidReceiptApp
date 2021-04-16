package com.bignerdranch.android.androidreceiptapp

import androidx.lifecycle.ViewModel

class ReceiptListViewModel : ViewModel() {

    val receipts = mutableListOf<Receipt>()

    init {
        for (i in 0 until 100) {
            val receipt = Receipt()
            receipts += receipt
        }
    }
}