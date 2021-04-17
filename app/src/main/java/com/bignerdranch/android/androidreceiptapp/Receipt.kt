package com.bignerdranch.android.androidreceiptapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Receipt (@PrimaryKey var ReceiptID: UUID = UUID.randomUUID(),
                    var Title: String = "Test Title",
                    var Date : Date = Date(),
                    var TotalSpent: Double = 0.0,
                    var Store: String = "Test Store"
)