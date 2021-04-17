package com.bignerdranch.android.androidreceiptapp

import java.util.*

@Entity
data class Receipt ( @PrimaryKey var ReceiptID: UUID = UUID.randomUUID(),
                     var Title: String = "Test Title",
                     var Date : Date = Date(),
                     var TotalSpent: Number = 0.0,
                     var Store: String = "Test Store",
                     var Entries: ArrayList<ReceiptEntry> = ArrayList<ReceiptEntry>()
) {
    val photoFileName
        get() = "IMG_$ReceiptID.jpg"
}