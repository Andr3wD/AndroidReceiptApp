package com.bignerdranch.android.androidreceiptapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

private const val TAG = "EntryListFragment"

class ReceiptEntryListFragment : Fragment() {
    private lateinit var id: UUID
    private lateinit var entryRecyclerView: RecyclerView
    private var adapter: EntryAdapter? = EntryAdapter(emptyList())

    private val entryListViewModel: ReceiptEntryListViewModel by lazy {
        ViewModelProviders.of(this).get(ReceiptEntryListViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_entry_list, container, false)

        entryRecyclerView =
                view.findViewById(R.id.entry_recycler_view) as RecyclerView
        entryRecyclerView.layoutManager = LinearLayoutManager(context)
        entryRecyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        entryListViewModel.bind(this.id)
        entryListViewModel.entryListLiveData.observe(
                viewLifecycleOwner,
                Observer { entries ->
                    entries?.let {
                        Log.i(TAG, "Got receipts ${entries.size}")
                        updateUI(entries)
                    }
                })
    }

    private fun updateUI(entries: List<ReceiptEntry>) {
        adapter = EntryAdapter(entries)
        entryRecyclerView.adapter = adapter
    }

    private inner class EntryHolder(view: View)
        : RecyclerView.ViewHolder(view), View.OnClickListener {

        private lateinit var entry: ReceiptEntry

        private val nameTextView: TextView = itemView.findViewById(R.id.entry_name)
        private val priceTextView: TextView = itemView.findViewById(R.id.entry_price)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(entry: ReceiptEntry) {
            this.entry = entry
            nameTextView.text = this.entry.Name
            priceTextView.text = "${this.entry.Price}"
        }

        override fun onClick(v: View) {
            Toast.makeText(context, "${entry.Name} pressed!", Toast.LENGTH_SHORT)
        }
    }

    private inner class EntryAdapter(var entries: List<ReceiptEntry>) :
            RecyclerView.Adapter<EntryHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryHolder {
            val view = layoutInflater.inflate(R.layout.list_item_entry, parent, false)
            return EntryHolder(view)
        }

        override fun getItemCount() = entries.size

        override fun onBindViewHolder(holder:EntryHolder, position: Int) {
            val entry = entries[position]
            holder.bind(entry)
        }
    }

    companion object {
        fun newInstance(id: UUID): ReceiptEntryListFragment {
            var result = ReceiptEntryListFragment()
            result.id = id
            return result
        }
    }
}