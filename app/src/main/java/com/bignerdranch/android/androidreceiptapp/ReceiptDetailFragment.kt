package com.bignerdranch.android.androidreceiptapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

private const val TAG = "ReceiptDetailFragment"
private const val ARG_RECEIPT_ID = "receipt_id"

class ReceiptDetailFragment : Fragment() {

    private lateinit var titleTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var totalTextView: TextView
    private lateinit var entryRecyclerView: RecyclerView
    private var adapter: EntryAdapter? = EntryAdapter(emptyList())

    private val receiptDetailViewModel: ReceiptDetailViewModel by lazy {
        ViewModelProviders.of(this).get(ReceiptDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val receiptID: UUID = arguments?.getSerializable(ARG_RECEIPT_ID) as UUID
        Log.d(TAG, "args bundle receipt ID: $receiptID")
        receiptDetailViewModel.loadReceipt(receiptID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_receipt_detail, container, false)

        entryRecyclerView = view.findViewById(R.id.detail_entry_list) as RecyclerView
        entryRecyclerView.layoutManager = LinearLayoutManager(context)
        entryRecyclerView.adapter = adapter

        titleTextView = view.findViewById(R.id.detail_receipt_title)
        dateTextView = view.findViewById(R.id.detail_receipt_date)
        totalTextView = view.findViewById(R.id.detail_receipt_total)

        return view
    }

    private fun updateUI(entries: List<ReceiptEntry>) {
        adapter = EntryAdapter(entries)
        entryRecyclerView.adapter = adapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        receiptDetailViewModel.entriesLiveData.observe(
            viewLifecycleOwner,
            Observer { entries ->
                entries?.let {
                    Log.i(TAG, "Got entries ${entries.size}")
                    updateUI(entries)
                }
            }
        )
        receiptDetailViewModel.receiptLiveData.observe(
            viewLifecycleOwner,
            Observer { receipt ->
                receipt?.let {
                    titleTextView.text = receipt.Title
                    dateTextView.text = receipt.Date.toString()
                    totalTextView.text = receipt.TotalSpent.toString()
                }
            }
        )

    }

    private inner class EntryHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private lateinit var entry: ReceiptEntry

        private val nameTextView: TextView = itemView.findViewById(R.id.entry_name)
        private val priceTextView: TextView = itemView.findViewById(R.id.entry_price)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(entry: ReceiptEntry) {
            this.entry = entry
            nameTextView.text = this.entry.Name
            priceTextView.text = "$" + this.entry.Price.toString()
        }

        override fun onClick(v: View) {
            return
        }
    }

    private inner class EntryAdapter(var entries: List<ReceiptEntry>)
        : RecyclerView.Adapter<EntryHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
        : EntryHolder {
            val view = layoutInflater.inflate(R.layout.list_item_entry, parent, false)
            return EntryHolder(view)
        }

        override fun getItemCount() = entries.size

        override fun onBindViewHolder(holder: EntryHolder, position: Int) {
            val entry = entries[position]
            holder.bind(entry)
        }
    }

    companion object {

        fun newInstance(receiptID: UUID): ReceiptDetailFragment {
            val args = Bundle().apply {
                putSerializable(ARG_RECEIPT_ID, receiptID)
            }
            return ReceiptDetailFragment().apply {
                arguments = args
            }
        }
    }
}