package com.bignerdranch.android.androidreceiptapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bignerdranch.android.androidreceiptapp.database.ReceiptDao
import org.opencv.core.*
import java.io.File
import java.util.*

private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0
private const val REQUEST_PHOTO = 2

class ReceiptDataEntryFragment : Fragment(), DatePickerFragment.Callbacks {

    interface Callbacks {
        fun onCameraSelected()
    }

    private var callbacks: Callbacks? = null
    private lateinit var cameraButton: ImageButton
    private lateinit var photoButton: ImageButton
    private lateinit var cameraImageDisplay: ImageView
    private lateinit var saveEntryButton: Button
    private lateinit var nickNameET: EditText
    private lateinit var storeNameET: EditText
    private lateinit var totalSpentET: EditText
    private lateinit var dateButton: Button
    private lateinit var purchaseDate: Date
    private var filesDir = context?.applicationContext?.filesDir
    private lateinit var photoUri: Uri


    private lateinit var photoFile: File

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_entry, container, false)

        photoFile = File("temp")
        photoUri = FileProvider.getUriForFile(requireActivity(),
            "com.bignerdranch.android.androidreceiptapp.fileprovider",
            photoFile)

        nickNameET = view.findViewById(R.id.receipt_nickname) as EditText
        storeNameET = view.findViewById(R.id.receipt_store_name) as EditText
        totalSpentET = view.findViewById(R.id.receipt_total_spent) as EditText
        dateButton = view.findViewById(R.id.receipt_date) as Button
        dateButton.setText(Date().toString())
        purchaseDate = Date()

        var receiptRepo = ReceiptRepository.get()

        saveEntryButton = view.findViewById(R.id.receipt_save) as Button
        cameraButton = view.findViewById(R.id.receipt_camera) as ImageButton
        photoButton = view.findViewById(R.id.receipt_camera_2) as ImageButton

        cameraButton.setOnClickListener {
            callbacks?.onCameraSelected()
        }

        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager

            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(captureImage,
                    PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false
            }

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities: List<ResolveInfo> =
                    packageManager.queryIntentActivities(captureImage,
                        PackageManager.MATCH_DEFAULT_ONLY)

                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }

                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }


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

    override fun onStart() {
        super.onStart()

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(date = purchaseDate).apply {
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