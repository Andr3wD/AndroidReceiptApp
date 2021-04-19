package com.bignerdranch.android.androidreceiptapp

import androidx.lifecycle.ViewModel
import java.util.*

class ReceiptEntryListViewModel() : ViewModel() {

    private lateinit var id: UUID

    private val receiptRepository = ReceiptRepository.get()
    val entryListLiveData = receiptRepository.getEntries(id)

    fun bind(id: UUID) {
        this.id = id
    }
}