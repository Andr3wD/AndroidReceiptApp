package com.bignerdranch.android.androidreceiptapp.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bignerdranch.android.androidreceiptapp.Receipt
import com.bignerdranch.android.androidreceiptapp.ReceiptEntry
import java.util.*

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipt")
    fun getReceipts(): LiveData<List<Receipt>>

    @Query("SELECT * FROM receipt WHERE ReceiptID=(:id)")
    fun getReceipt(id: UUID): LiveData<Receipt?>

    @Query("SELECT * FROM receiptentry WHERE ReceiptID=(:id)")
    fun getEntries(id: UUID): LiveData<List<ReceiptEntry>>

    @Update
    fun updateReceipt(receipt: Receipt)

    @Update
    fun updateEntry(entry: ReceiptEntry)

    @Insert
    fun addReceipt(receipt: Receipt)

    @Insert
    fun addEntry(entry: ReceiptEntry)
}