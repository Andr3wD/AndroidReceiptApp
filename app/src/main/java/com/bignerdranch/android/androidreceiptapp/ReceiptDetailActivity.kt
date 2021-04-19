package com.bignerdranch.android.androidreceiptapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.util.*

private const val TAG = "ReceiptDetailActivity"
private const val EXTRA_RECEIPT_ID = "com.bignerdranch.android.androidreceiptapp.receipt_id"

class ReceiptDetailActivity : AppCompatActivity() {

    private var receipt_id: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_detail)

        receipt_id = intent.getStringExtra(EXTRA_RECEIPT_ID)
        if (receipt_id == null) {
            Log.w(TAG, "Null receipt ID found in intent")
        }
        val currentFragment =
                supportFragmentManager.findFragmentById(R.id.detail_entry_list_fragment)

        if (currentFragment == null) {
            val fragment = ReceiptEntryListFragment.newInstance(UUID.fromString(receipt_id))
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.detail_entry_list_fragment, fragment)
                    .commit()
        }
    }

    companion object {
        fun newIntent(packageContext: Context, receiptId: String): Intent {
            return Intent(packageContext, ReceiptDetailActivity::class.java).apply {
                putExtra(EXTRA_RECEIPT_ID, receiptId)
            }
        }
    }
}