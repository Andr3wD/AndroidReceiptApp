package com.bignerdranch.android.androidreceiptapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import java.util.*

class ReceiptEntryFragment : Fragment() {

    private lateinit var receiptEntry: ReceiptEntry
    private lateinit var nameField: EditText
    private lateinit var categoryField: EditText
    private lateinit var priceField: EditText

    // Does ReceiptEntryFragment use this ViewModel?
    private val receiptDetailViewModel: ReceiptDetailViewModel by lazy {
        ViewModelProviders.of(this).get(ReceiptDetailViewModel::class.java)
    }

    /*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiptEntry = ReceiptEntry()
        val receiptEntryId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        receiptDetailViewModel.loadEntry(receiptEntryId)
    }
    */

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_receipt_entry, container, false)

        nameField = view.findViewById(R.id.receipt_entry_name) as EditText
        categoryField = view.findViewById(R.id.receipt_entry_category) as EditText
        priceField = view.findViewById(R.id.receipt_entry_price) as EditText

        return view
    }
}