package com.bignerdranch.android.androidreceiptapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class ReceiptEntry (@PrimaryKey var ReceiptID: UUID,
                         @PrimaryKey var EntryIndex: Int,
                         var Name: String,
                         var Category: String
        )