package com.bignerdranch.android.androidreceiptapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import org.opencv.core.*
import java.util.*

private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0

class ReceiptDataEntryFragment : Fragment(), DatePickerFragment.Callbacks {

    interface Callbacks {
        fun onCameraSelected()
    }

    private var callbacks: Callbacks? = null
    private lateinit var cameraButton: ImageButton
    private lateinit var cameraImageDisplay: ImageView
    private lateinit var saveEntryButton: Button
    private lateinit var nickNameET: EditText
    private lateinit var storeNameET: EditText
    private lateinit var dateButton: Button
    private lateinit var purchaseDate: Date

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_entry, container, false)

        nickNameET = view.findViewById(R.id.receipt_nickname) as EditText
        storeNameET = view.findViewById(R.id.receipt_store_name) as EditText
        dateButton = view.findViewById(R.id.receipt_date) as Button
        dateButton.setText(Date().toString())
        purchaseDate = Date()

        saveEntryButton = view.findViewById(R.id.receipt_save) as Button
        cameraButton = view.findViewById(R.id.receipt_camera) as ImageButton

        cameraButton.setOnClickListener {
            callbacks?.onCameraSelected()
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        dateButton.setOnClickListener {
            var date = Date()
            DatePickerFragment.newInstance(date = date).apply {
                setTargetFragment(this@ReceiptDataEntryFragment, REQUEST_DATE)
                show(this@ReceiptDataEntryFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }
    }

    override fun onDateSelected(date: Date) {
        dateButton.setText(date.toString())
        purchaseDate = date

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    companion object {
        fun newInstance(): ReceiptDataEntryFragment {
            return ReceiptDataEntryFragment()
        }
    }

}