package com.bignerdranch.android.androidreceiptapp

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.util.*

private const val TAG = "ReceiptDetailViewModel"

class ReceiptDetailViewModel : ViewModel() {

    private val receiptRepository = ReceiptRepository.get()
    private val receiptIdLiveData = MutableLiveData<UUID>()

    var receiptLiveData: LiveData<Receipt?> =
            Transformations.switchMap(receiptIdLiveData) { receiptId ->
                receiptRepository.getReceipt(receiptId)
            }
    var entriesLiveData: LiveData<List<ReceiptEntry>> =
            Transformations.switchMap(receiptIdLiveData) { receiptId ->
                receiptRepository.getEntries(receiptId)
            }

    fun loadReceipt(receiptId: UUID) {
        receiptIdLiveData.value = receiptId
        Log.i(TAG, "Loading max index")
    }

    fun updateReceipt(receipt: Receipt) {
        receiptRepository.updateReceipt(receipt)
    }

    fun addEntry(name: String, category: String, price: Double) {
        // Get the max index of the entries. We're doing this here every time because I could not get
        // a sql query that does this to work with Room
        var maxIndex = -1
        for (entry in entriesLiveData.value!!) {
            if (entry.EntryIndex > maxIndex)
                maxIndex = entry.EntryIndex
        }
        var entry = ReceiptEntry(ReceiptID=receiptIdLiveData.value!!, EntryIndex=maxIndex+1, Name=name, Category=category, Price=price)
        receiptRepository.addEntry(entry)
        loadReceipt(receiptIdLiveData.value!!)
    }

    fun deleteEntry(entry: ReceiptEntry) {
        receiptRepository.deleteEntry((entry))
    }

    fun updateEntry(entry: ReceiptEntry) {
        receiptRepository.updateEntry(entry)

    }

    /*
    fun saveReceipt(receipt: Receipt) {
        receiptRepository.updateReceipt(receipt)
    }

    fun saveEntry(entry: ReceiptEntry) {
        receiptRepository.updateEntry(entry)
    }

    fun loadEntry(entryId: UUID) {
        receiptIdLiveData.value = receiptId
    }
    */
}