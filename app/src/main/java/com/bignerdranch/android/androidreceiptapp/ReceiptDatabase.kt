package com.bignerdranch.android.androidreceiptapp

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ Receipt::class, ReceiptEntry::class ], version=1, exportSchema = false)
@TypeConverters(ReceiptTypeConverters::class)
abstract class ReceiptDatabase : RoomDatabase() {

    abstract fun receiptDao(): ReceiptDao
}