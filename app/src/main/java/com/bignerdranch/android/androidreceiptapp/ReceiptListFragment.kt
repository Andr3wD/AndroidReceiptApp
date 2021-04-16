package com.bignerdranch.android.androidreceiptapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

private const val TAG = "ReceiptListFragment"

class ReceiptListFragment : Fragment() {

    private lateinit var receiptRecyclerView: RecyclerView
    private var adapter: ReceiptAdapter? = null

    private val receiptListViewModel: ReceiptListViewModel by lazy {
        ViewModelProviders.of(this).get(ReceiptListViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Total receipts: ${receiptListViewModel.receipts.size}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_receipt_list, container, false)

        receiptRecyclerView =
            view.findViewById(R.id.receipt_recycler_view) as RecyclerView
        receiptRecyclerView.layoutManager = LinearLayoutManager(context)

        updateUI()

        return view
    }

    private fun updateUI() {
        val receipts = receiptListViewModel.receipts
        adapter = ReceiptAdapter(receipts)
        receiptRecyclerView.adapter = adapter
    }

    private fun formatCrimeDate(d: Date) : CharSequence? {
        return android.text.format.DateFormat.format("EEEE, MMM dd, yyyy", d)
    }

    private inner class ReceiptHolder(view: View)
        : RecyclerView.ViewHolder(view), View.OnClickListener {

        private lateinit var receipt: Receipt

        private val titleTextView: TextView = itemView.findViewById(R.id.receipt_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.receipt_date)
        private val totalTextView: TextView = itemView.findViewById(R.id.receipt_total)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(receipt: Receipt) {
            this.receipt = receipt
            titleTextView.text = this.receipt.Title
            dateTextView.text = formatCrimeDate(this.receipt.Date)
            totalTextView.text = this.receipt.TotalSpent.toString()
        }

        override fun onClick(v: View) {
            Toast.makeText(context, "${receipt.Title} pressed!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private inner class ReceiptAdapter(var receipts: List<Receipt>) :
        RecyclerView.Adapter<ReceiptHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : ReceiptHolder {
            val view = layoutInflater.inflate(R.layout.list_item_receipt, parent, false)
            return ReceiptHolder(view)
        }

        override fun getItemCount() = receipts.size

        override fun onBindViewHolder(holder: ReceiptHolder, position: Int) {
            val receipt = receipts[position]
            holder.bind(receipt)
        }
    }

    companion object {
        fun newInstance(): ReceiptListFragment {
            return ReceiptListFragment()
        }
    }
}