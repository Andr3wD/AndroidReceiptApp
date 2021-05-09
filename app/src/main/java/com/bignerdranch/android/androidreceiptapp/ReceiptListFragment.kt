package com.bignerdranch.android.androidreceiptapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.MutableLiveData
import java.util.*


private const val TAG = "ReceiptListFragment"

class ReceiptListFragment : Fragment() {

    /**
     * Required interface for hosting activities
     */
    interface Callbacks {
        fun onReceiptSelected(receiptId: UUID)
    }

    private var callbacks: Callbacks? = null

    private lateinit var receiptRecyclerView: RecyclerView
    private var adapter: ReceiptAdapter? = ReceiptAdapter(mutableListOf<Receipt>())

    private val receiptListViewModel: ReceiptListViewModel by lazy {
        ViewModelProviders.of(this).get(ReceiptListViewModel::class.java)
    }
    private val receiptDetailViewModel: ReceiptDetailViewModel by lazy {
        ViewModelProviders.of(this).get(ReceiptDetailViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

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
        val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val receiptHolder = viewHolder as ReceiptHolder
                receiptListViewModel.deleteReceipt(receiptHolder.getReceipt())
                receiptListViewModel.receiptListLiveData.value!!.toMutableList().removeAt(viewHolder.adapterPosition)
                receiptRecyclerView.adapter!!.notifyItemRemoved(viewHolder.adapterPosition)
            }
        }
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(receiptRecyclerView)
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

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_receipt_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.new_receipt -> {
//                val receipt = Receipt()
//                receiptListViewModel.addReceipt(receipt)
//                callbacks?.onReceiptSelected((receipt.ReceiptID))
//                true
//            }
//            else -> return super.onOptionsItemSelected(item)
        val fragment = ReceiptDataEntryFragment.newInstance()
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.fragment_container,fragment)?.addToBackStack("receipt-list-fragment")?.commit()
        return true
    }
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

        fun getReceipt() = receipt

        fun bind(receipt: Receipt) {
            this.receipt = receipt
            titleTextView.text = this.receipt.Title
            dateTextView.text = formatCrimeDate(this.receipt.Date)
            totalTextView.text = "$%.2f".format(this.receipt.TotalSpent)
            // Set icon based on store
            when (this.receipt.Store.toLowerCase()) {
                // TODO: Add more store icons (recommended size is 60x60px)
                "target" -> imageView.setImageResource(R.drawable.target_logo)
                "albertsons" -> imageView.setImageResource(R.drawable.albertsons_logo)
                "costco" -> imageView.setImageResource(R.drawable.costco_logo)
                "walmart" -> imageView.setImageResource(R.drawable.walmart_logo)
                else -> imageView.setImageResource(R.drawable.ic_baseline_receipt)
            }
        }

        override fun onClick(v: View) {
            // Toast.makeText(context, "${receipt.Title} pressed!", Toast.LENGTH_SHORT).show()
            receiptDetailViewModel.loadReceipt(receipt.ReceiptID)
            callbacks?.onReceiptSelected(receipt.ReceiptID)
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