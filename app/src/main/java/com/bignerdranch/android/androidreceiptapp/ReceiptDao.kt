package com.bignerdranch.android.androidreceiptapp

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import java.util.*

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipt")
    fun getReceipts(): LiveData<List<Receipt>>

    @Query("SELECT * FROM receipt WHERE ReceiptID=(:id)")
    fun getReceipt(id: UUID): LiveData<Receipt?>

    @Query("SELECT * FROM receiptentry WHERE ReceiptID=(:id)")
    fun getEntries(id: UUID): LiveData<List<ReceiptEntry>>
}