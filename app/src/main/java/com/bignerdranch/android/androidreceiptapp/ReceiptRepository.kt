package com.bignerdranch.android.androidreceiptapp

import android.content.Context
import java.lang.IllegalStateException

class ReceiptRepository private constructor(context: Context) {

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