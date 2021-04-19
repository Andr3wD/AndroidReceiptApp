package com.bignerdranch.android.androidreceiptapp

import androidx.room.Entity
import java.util.*

@Entity(primaryKeys = ["ReceiptID","EntryIndex"])
data class ReceiptEntry (var ReceiptID: UUID,
                         var EntryIndex: Int,
                         var Name: String,
                         var Category: String,
                         var Price: Double
        )