package com.bignerdranch.android.androidreceiptapp

import androidx.room.Entity
import androidx.room.ForeignKey
import java.util.*

@Entity(primaryKeys = ["ReceiptID","EntryIndex"], foreignKeys=[ForeignKey(entity=Receipt::class, parentColumns=["ReceiptID"], childColumns=["ReceiptID"])])
data class ReceiptEntry (var ReceiptID: UUID,
                         var EntryIndex: Int,
                         var Name: String,
                         var Category: String,
                         var Price: Double
        )