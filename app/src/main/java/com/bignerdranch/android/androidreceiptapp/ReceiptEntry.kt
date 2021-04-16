package com.bignerdranch.android.androidreceiptapp

import java.util.*

data class ReceiptEntry (var ReceiptID: UUID,
                         var EntryIndex: Int,
                         var Name: String,
                         var Category: String
        )