package com.bignerdranch.android.androidreceiptapp

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.OpenCVLoader
import java.util.*

private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity(), ReceiptDataEntryFragment.Callbacks, ReceiptListFragment.Callbacks {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // https://stackoverflow.com/questions/38552144/how-get-permission-for-camera-in-android-specifically-marshmallow
        // https://stackoverflow.com/questions/34153965/unfortunately-camera-has-stopped
        // Get camera permissions
        val requestCode = 0
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), requestCode)
            // TODO handle when no permissions granted.
        }

        // https://github.com/quickbirdstudios/opencv-android
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCv", "Unable to load OpenCV")
        } else {
            Log.d("OpenCv", "OpenCV loaded")
        }

        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val fragment = ReceiptListFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }
    }

    override fun onCameraSelected() {
        val fragment = ReceiptDataEntryCameraFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("camera-fragment")
            .commit()
    }

    override fun onReceiptSelected(receiptID: UUID) {
        val fragment = ReceiptDetailFragment.newInstance(receiptID)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("receipt-list-fragment")
            .commit()
    }
}