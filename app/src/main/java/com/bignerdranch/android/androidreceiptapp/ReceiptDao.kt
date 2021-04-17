package com.bignerdranch.android.androidreceiptapp

import androidx.room.Dao
import androidx.room.Query
import java.util.*

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipt")
    fun getReceipts(): List<Receipt>

    @Query("SELECT * FROM receiptentry WHERE ReceiptID=(:id)")
    fun getEntries(id: UUID): List<ReceiptEntry>
}