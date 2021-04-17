package com.bignerdranch.android.androidreceiptapp

import android.app.Application

class ReceiptApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ReceiptRepository.initialize(this)
    }
}