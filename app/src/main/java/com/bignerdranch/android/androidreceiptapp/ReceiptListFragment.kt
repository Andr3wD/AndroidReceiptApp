package com.bignerdranch.android.androidreceiptapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Observer
import java.util.*

private const val TAG = "ReceiptListFragment"

class ReceiptListFragment : Fragment() {
    /*

    /**
     * Required interface for hosting activities
     */
    interface Callbacks {
        fun onReceiptSelected(receiptId: UUID)
    }

    private var callbacks: Callbacks? = null
     */

    private lateinit var receiptRecyclerView: RecyclerView
    private var adapter: ReceiptAdapter? = ReceiptAdapter(emptyList())

    private val receiptListViewModel: ReceiptListViewModel by lazy {
        ViewModelProviders.of(this).get(ReceiptListViewModel::class.java)
    }

    /*
    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
        receiptRecyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        receiptListViewModel.receiptListLiveData.observe(
            viewLifecycleOwner,
            Observer { receipts ->
                receipts?.let {
                    Log.i(TAG, "Got receipts ${receipts.size}")
                    updateUI(receipts)
                }
            })
    }

    /*
    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }
    */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_receipt_list, menu)
    }

    /*
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_receipt -> {
                val receipt = Receipt()
                receiptListViewModel.addReceipt(receipt)
                callbacks?.onReceiptSelected((receipt.ReceiptID))
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
    */
    private fun updateUI(receipts: List<Receipt>) {
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
        private val imageView: ImageView = itemView.findViewById(R.id.store_image)

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