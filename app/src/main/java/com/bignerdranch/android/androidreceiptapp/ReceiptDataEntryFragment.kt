package com.bignerdranch.android.androidreceiptapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.bignerdranch.android.androidreceiptapp.database.ReceiptDao
import org.opencv.core.*
import java.io.File
import java.util.*

private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0
private const val REQUEST_PHOTO = 2

class ReceiptDataEntryFragment : Fragment(), DatePickerFragment.Callbacks {

    /* Aspects that will mostly be sourced from the layout */
    private lateinit var cameraButton: ImageButton
    private lateinit var photoButton: ImageButton
    private lateinit var cameraImage: ImageView
    private lateinit var saveEntryButton: Button
    private lateinit var nickNameET: EditText
    private lateinit var storeNameET: EditText
    private lateinit var totalSpentET: EditText
    private lateinit var dateButton: Button
    private lateinit var purchaseDate: Date

    private val receiptDataEntryViewModel: ReceiptDataEntryViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(ReceiptDataEntryViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_entry, container, false)
        Log.d("dataEntryFrag","onCreateView called")

        nickNameET = view.findViewById(R.id.receipt_nickname) as EditText
        storeNameET = view.findViewById(R.id.receipt_store_name) as EditText
        totalSpentET = view.findViewById(R.id.receipt_total_spent) as EditText
        dateButton = view.findViewById(R.id.receipt_date) as Button
        cameraImage = view.findViewById(R.id.receipt_camera_image) as ImageView

        purchaseDate = receiptDataEntryViewModel.dateFound
        dateButton.setText(purchaseDate.toString())
        totalSpentET.setText(receiptDataEntryViewModel.maxCostFound.toString())

        var receiptRepo = ReceiptRepository.get()

        saveEntryButton = view.findViewById(R.id.receipt_save) as Button
        cameraButton = view.findViewById(R.id.receipt_camera) as ImageButton
        photoButton = view.findViewById(R.id.receipt_camera_2) as ImageButton

        cameraButton.setOnClickListener {
            val fragment = ReceiptDataEntryCameraFragment.newInstance()
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container,fragment)?.addToBackStack("camera-fragment")?.commit()
        }

        /* Save the entry and check for errors*/
        saveEntryButton.setOnClickListener {
            if (storeNameET.text.toString().length <= 0 ||
                nickNameET.text.toString().length <= 0 ||
                 totalSpentET.text.toString().length <= 0) {
                Toast.makeText(context, "One or more errors in entry", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            var totalSpentDouble: Double?

            try
            {
                totalSpentDouble = totalSpentET.text.toString().toDouble()
            }
            catch (e: NumberFormatException)
            {
                Toast.makeText(context,"Total spent is not a number",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            var title = nickNameET.text.toString()
            var storeName = storeNameET.text.toString()
            var receipt = Receipt(Title=title,Store=storeName,TotalSpent = totalSpentDouble,Date=purchaseDate)
            receiptRepo.addReceipt(receipt)
            val fragment = ReceiptListFragment.newInstance()
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container,fragment)?.commit()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        Log.d("dataEntryFrag", "resuming")
        purchaseDate = receiptDataEntryViewModel.dateFound

        dateButton.setText(formatDate(purchaseDate).toString())
        totalSpentET.setText(receiptDataEntryViewModel.maxCostFound.toString())
        storeNameET.setText(receiptDataEntryViewModel.storeNameGuess)
        receiptDataEntryViewModel.clearMemory()
    }

    override fun onStart() {
        super.onStart()

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(date = purchaseDate).apply {
                setTargetFragment(this@ReceiptDataEntryFragment, REQUEST_DATE)
                show(this@ReceiptDataEntryFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }
    }

    /* method for datefragment */
    override fun onDateSelected(date: Date) {
        dateButton.setText(formatDate(date).toString())
        purchaseDate = date

    }

    /* Method used to format the date */
    private fun formatDate(d: Date) : CharSequence? {
        return android.text.format.DateFormat.format("EEEE, MMM dd, yyyy", d)
    }

    companion object {
        fun newInstance(): ReceiptDataEntryFragment {
            return ReceiptDataEntryFragment()
        }
    }

}