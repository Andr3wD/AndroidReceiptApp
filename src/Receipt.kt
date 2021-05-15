package com.bignerdranch.android.androidreceiptapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Receipt (@PrimaryKey var ReceiptID: UUID = UUID.randomUUID(),
                    var Title: String,
                    var Date : Date,
                    var TotalSpent: Double,
                    var Store: String
)