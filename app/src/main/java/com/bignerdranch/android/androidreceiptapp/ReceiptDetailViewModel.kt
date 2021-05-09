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
        Log.i(TAG, "Loaded ReceiptID")
        receiptIdLiveData.value = receiptId
    }

    fun addEntry(name: String, category: String, price: Double) : ReceiptEntry {
        var entry = ReceiptEntry(ReceiptID=receiptIdLiveData.value!!, EntryIndex=entriesLiveData.value!!.size, Name=name, Category=category, Price=price)
        receiptRepository.addEntry(entry)
        loadReceipt(receiptIdLiveData.value!!)
        return entry
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