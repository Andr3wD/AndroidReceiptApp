package com.bignerdranch.android.androidreceiptapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

private const val TAG = "ReceiptDetailFragment"
private const val ARG_RECEIPT_ID = "receipt_id"

class ReceiptDetailFragment : Fragment() {

    private lateinit var titleTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var totalTextView: TextView
    private lateinit var entryRecyclerView: RecyclerView
    private lateinit var addEntryButton: FloatingActionButton
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
        addEntryButton = view.findViewById(R.id.detail_add_button)
        addEntryButton.setOnClickListener {
            receiptDetailViewModel.addEntry("New Entry", "", 0.0)
            Toast.makeText(this@ReceiptDetailFragment.requireContext(), "Created new entry", Toast.LENGTH_SHORT).show()
        }

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
                    totalTextView.text = "$%.2f".format(receipt.TotalSpent)
                }
            }
        )
    }

    override fun onStop() {
        super.onStop()
        adapter?.onStop()
    }

    private inner class EntryHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private lateinit var entry: ReceiptEntry
        private var initialName : Boolean = true
        private var initialPrice : Boolean = true

        private var nameEditText: EditText = itemView.findViewById(R.id.entry_name)
        private var priceEditText: EditText = itemView.findViewById(R.id.entry_price)

        init {
            itemView.setOnClickListener(this)
            with(nameEditText) {
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        //
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (initialName) {
                            initialName = false
                            return
                        }
                        entry.Name = s.toString()
                    }

                    override fun afterTextChanged(s: Editable?) {
                        //
                    }
                })
            }

            with(priceEditText) {
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        //
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (initialPrice) {
                            initialPrice = false
                            return
                        }
                        val regex = Regex("^[0-9]*\\.?[0-9]+\$")
                        val parsedPrice = regex.replace(s.toString(),"")
                        var parsedPriceDouble : Double
                        try {
                            entry.Price = parsedPrice.toDouble()
                        }
                        catch (e: NumberFormatException) {
                            entry.Price = entry.Price
                            //Toast the user here to warn them about an invalid entry
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        //
                    }
                })
            }
        }

        fun bind(entry: ReceiptEntry) {
            this.entry = entry
            nameEditText.setText(this.entry.Name)
            priceEditText.setText("$%.2f".format(entry.Price))
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

        fun onStop()
        {
            for (entry in entries)
            {
                receiptDetailViewModel.updateEntry(entry)
            }
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