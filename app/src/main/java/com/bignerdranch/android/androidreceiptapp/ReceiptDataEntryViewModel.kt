package com.bignerdranch.android.androidreceiptapp

import androidx.lifecycle.ViewModel
import java.util.*

class ReceiptDataEntryViewModel : ViewModel() {

    var dateFound: Date = Date()
    var maxCostFound: Double = 0.0
    var storeNameGuess: String = ""

    fun clearMemory() {
        dateFound = Date()
        maxCostFound = 0.0
        storeNameGuess = ""
    }
}