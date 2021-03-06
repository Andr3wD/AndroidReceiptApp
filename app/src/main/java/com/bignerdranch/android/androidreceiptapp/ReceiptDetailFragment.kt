package com.bignerdranch.android.androidreceiptapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.Instant.now
import java.util.*

private const val TAG = "ReceiptDetailFragment"
private const val ARG_RECEIPT_ID = "receipt_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0

class ReceiptDetailFragment : Fragment(), DatePickerFragment.Callbacks {

    private lateinit var titleEditText: EditText
    private lateinit var dateButton: Button
    private lateinit var totalEditText: EditText
    private lateinit var storeEditText: EditText
    private lateinit var entryRecyclerView: RecyclerView
    private lateinit var addEntryButton: FloatingActionButton
    private var myReceipt: Receipt? = null
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

        val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val entryHolder = viewHolder as ReceiptDetailFragment.EntryHolder
                receiptDetailViewModel.deleteEntry(entryHolder.getEntry())
                entryRecyclerView.adapter!!.notifyItemRemoved(viewHolder.adapterPosition)
            }
        }
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(entryRecyclerView)

        titleEditText = view.findViewById(R.id.detail_receipt_title)
        titleEditText.addTextChangedListener(object: TextWatcher {
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
                myReceipt?.Title = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {
                //
            }

        })
        dateButton = view.findViewById(R.id.detail_receipt_date)
        dateButton.setOnClickListener {
            myReceipt?.let { it1 ->
                DatePickerFragment.newInstance(date = it1.Date).apply {
                    setTargetFragment(this@ReceiptDetailFragment, REQUEST_DATE)
                    show(this@ReceiptDetailFragment.requireFragmentManager(), DIALOG_DATE)
                }
            }
        }
        totalEditText = view.findViewById(R.id.detail_receipt_total)
        totalEditText.addTextChangedListener(object: TextWatcher {
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
                try {
                    myReceipt?.TotalSpent = s.toString().toDouble()
                }
                catch (e: NumberFormatException) {
                    myReceipt?.TotalSpent = myReceipt?.TotalSpent!!
                }
                Log.d(TAG,myReceipt?.TotalSpent.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                //
            }

        })
        storeEditText = view.findViewById(R.id.detail_receipt_store)
        storeEditText.addTextChangedListener(object: TextWatcher {
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
                myReceipt?.Store = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {
                //
            }
        })

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
                    // Sort by entry index
                    val sorted = entries.toMutableList()
                    sorted.sortWith(Comparator { lhs, rhs ->
                        when {
                            (lhs.EntryIndex < rhs.EntryIndex) -> -1
                            (lhs.EntryIndex == rhs.EntryIndex) -> 0
                            else -> 1
                        }
                    })
                    updateUI(sorted as List<ReceiptEntry>)
                }
            }
        )
        receiptDetailViewModel.receiptLiveData.observe(
            viewLifecycleOwner,
            Observer { receipt ->
                receipt?.let {
                    myReceipt = receipt
                    titleEditText.setText(receipt.Title)
                    dateButton.text = formatDate(receipt.Date).toString()
                    storeEditText.setText(receipt.Store)
                    totalEditText.setText("%.2f".format(receipt.TotalSpent))
                }
            }
        )


    }

    override fun onStop() {
        super.onStop()
        adapter?.onStop()
        Log.d(TAG,myReceipt.toString())
        /* update the receipt on exit */
        myReceipt?.let { receiptDetailViewModel.updateReceipt(it) }
    }

    /* Method used to format the date */
    private fun formatDate(d: Date) : CharSequence? {
        return android.text.format.DateFormat.format("EEEE, MMM dd, yyyy", d)
    }

    /* Defines the Receipt Entries and how they respond to editTexts being changed */
    private inner class EntryHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private lateinit var entry: ReceiptEntry

        /* Variables added to make sure that the editText listeners don't fire upon starting */
        private var initialName : Boolean = true
        private var initialPrice : Boolean = true

        /* These variables are given listeners to respond to change */
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
                        /* Only update the price if the entry is a number */
                        try {
                            entry.Price = s.toString().toDouble()
                        }
                        catch (e: NumberFormatException) {
                            entry.Price = entry.Price
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        //
                    }
                })
            }
        }

        fun getEntry() = entry

        fun bind(entry: ReceiptEntry) {
            this.entry = entry
            nameEditText.setText(this.entry.Name)
            priceEditText.setText("%.2f".format(entry.Price))
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
            /* Account for empty names */
            if (entry.Name == "") {
                entry.Name = "New Entry"
            }
            holder.bind(entry)
        }

        fun onStop()
        {
            for (entry in entries)
            {
                if (entry.Name == "") {
                    entry.Name = "New Entry"
                }
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

    override fun onDateSelected(date: Date) {
        dateButton.setText(formatDate(date).toString())
        myReceipt?.Date = date

    }
}