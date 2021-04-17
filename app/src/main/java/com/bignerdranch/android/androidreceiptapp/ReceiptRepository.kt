package com.bignerdranch.android.androidreceiptapp

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import java.lang.IllegalStateException
import java.util.*

private const val DATABASE_NAME = "receipt-database"

class ReceiptRepository private constructor(context: Context) {

    private val database : ReceiptDatabase = Room.databaseBuilder(
        context.applicationContext,
        ReceiptDatabase::class.java,
        DATABASE_NAME
    ).build()

    private val receiptDao = database.receiptDao()

    fun getReceipts(): LiveData<List<Receipt>> = receiptDao.getReceipts()

    fun getReceipt(id: UUID): LiveData<Receipt?> = receiptDao.getReceipt(id)

    fun getEntries(id: UUID): LiveData<List<ReceiptEntry>> = receiptDao.getEntries(id)

    companion object {
        private var INSTANCE: ReceiptRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = ReceiptRepository(context)
            }
        }

        fun get(): ReceiptRepository {
            return INSTANCE ?:
            throw IllegalStateException("CrimeRepository must be initialized")
        }
    }
}