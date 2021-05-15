package com.bignerdranch.android.androidreceiptapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bignerdranch.android.androidreceiptapp.Receipt
import com.bignerdranch.android.androidreceiptapp.ReceiptEntry

@Database(entities = [ Receipt::class, ReceiptEntry::class ], version=1, exportSchema = false)
@TypeConverters(ReceiptTypeConverters::class)
abstract class ReceiptDatabase : RoomDatabase() {

    abstract fun receiptDao(): ReceiptDao
}