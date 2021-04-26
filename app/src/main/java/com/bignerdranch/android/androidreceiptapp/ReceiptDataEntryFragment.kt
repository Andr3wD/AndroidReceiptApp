package com.bignerdranch.android.androidreceiptapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.lang.Math.abs
import java.nio.ByteBuffer
import java.time.Instant.now
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0

class ReceiptDataEntryFragment : Fragment(), DatePickerFragment.Callbacks {

    private lateinit var cameraButton: ImageButton
    private lateinit var cameraImageDisplay: ImageView
    private lateinit var saveEntryButton: Button
    private lateinit var nickNameET: EditText
    private lateinit var storeNameET: EditText
    private lateinit var dateButton: Button
    private lateinit var purchaseDate: Date

    lateinit var camManager: CameraManager
    lateinit var imgReader: ImageReader
    var takeImage: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_data_entry, container, false)

        nickNameET = view.findViewById(R.id.receipt_nickname) as EditText
        storeNameET = view.findViewById(R.id.receipt_store_name) as EditText
        dateButton = view.findViewById(R.id.receipt_date) as Button
        dateButton.setText(Date().toString())
        purchaseDate = Date()

        saveEntryButton = view.findViewById(R.id.receipt_save) as Button
        cameraButton = view.findViewById(R.id.receipt_camera) as ImageButton
        cameraImageDisplay = view.findViewById(R.id.receipt_camera_image)
        cameraImageDisplay.setDrawingCacheEnabled(true)
        cameraImageDisplay.rotation = 90F
        // Using JPEG instead of YUV_420_888 because https://stackoverflow.com/questions/28430024/convert-android-media-image-yuv-420-888-to-bitmap?answertab=votes#tab-top. Faster and easier.
        imgReader = ImageReader.newInstance(500, 500, ImageFormat.JPEG, 10)
        imgReader.setOnImageAvailableListener({

            // Thanks to https://stackoverflow.com/questions/26673127/android-imagereader-acquirelatestimage-returns-invalid-jpg
            val latestImage = it.acquireLatestImage()
            if (takeImage) {
                takeImage = false
                val byteBuffer: ByteBuffer = latestImage.planes[0].buffer
                byteBuffer.rewind()
                val byteArray = ByteArray(byteBuffer.remaining())
                byteBuffer.get(byteArray)
                val bit: Bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                val contourBit = getContours(bit) // Get a bitmap with the contours applied to it.
                cameraImageDisplay.setImageBitmap(contourBit)
            }
            if (latestImage != null)
                latestImage.close() // Close the image so the ImageReader buffer doesn't get full.
        }, null)

        cameraImageDisplay.setOnClickListener {
            takeImage = true
        }

        // Make a camera2 camera attached to the ImageReader surface
        makeCameraStuff(imgReader.surface)
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

    private fun makeCameraStuff(surf: Surface) {
        // https://medium.com/androiddevelopers/understanding-android-camera-capture-sessions-and-requests-4e54d9150295
        camManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        if (ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d("test", "NO PERMISSIONS!")
            return
        }


        camManager.openCamera(camManager.cameraIdList[0], object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.d("test", "CAMERA OPEN")
                var targets: MutableList<Surface> = arrayOf(surf).toMutableList()

                // Deprecated, but next 'available' is version 28, so...
                // Create a capture session
                camera.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        // Create a request for capture
                        val capture =
                            session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        capture.addTarget(surf)

                        // Make the request repeat
                        session.setRepeatingRequest(capture.build(), null, null)
                        Log.d("test", "CAMERA CONFIG SUCCESSFUL")
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("test", "CAMERA CONFIG FAILED")
                    }

                }, null)
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.d("test", "CAMERA DISCONNECT")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.e("test", "CAMERA ERR")
            }

        }, null)
    }

    private fun getContours(imageBitmap: Bitmap): Bitmap {
        // TODO start new fragment with 4 point transform.
        // https://www.pyimagesearch.com/2014/09/01/build-kick-ass-mobile-document-scanner-just-5-minutes/
        // Don't trust immutable yet...
        // TODO switch to vals eventually.
        // TODO compress into 1 file that constantly replaces itself
        var testMat: Mat = Mat()
        var testGreyMat: Mat = Mat()
        var testGreyGaussianMat: Mat = Mat()
        var testGreyGaussianEdgeMat: Mat = Mat()

        // Convert image bitmap to OpenCV Mat
        Utils.bitmapToMat(imageBitmap, testMat)

        // Make image greyscale
        Imgproc.cvtColor(testMat, testGreyMat, Imgproc.COLOR_BGR2GRAY)
        // Blur the image
        Imgproc.GaussianBlur(testGreyMat, testGreyGaussianMat, Size(5.0, 5.0), 0.0)
        // Apply edge detection
        Imgproc.Canny(testGreyGaussianMat, testGreyGaussianEdgeMat, 75.0, 75.0 * 3)

        var matPointList: MutableList<MatOfPoint> =
            ArrayList() // The points that will be found by the findContours() method
        var contourHierarchyMat = Mat() // No clue what this does.
        // See https://docs.opencv.org/3.4/d4/d73/tutorial_py_contours_begin.html
        Imgproc.findContours(
            testGreyGaussianEdgeMat,
            matPointList,
            contourHierarchyMat,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        // Convert to MatOfPoint2f https://stackoverflow.com/questions/11273588/how-to-convert-matofpoint-to-matofpoint2f-in-opencv-java-api
        // Convert to MatOfPoint2f for some reason
        var newPointList: MutableList<MatOfPoint2f> = ArrayList()
        for (v in matPointList) {
            val d = MatOfPoint2f()
            v.convertTo(d, CvType.CV_32F)
            newPointList.add(d)
        }
        // Sort the point list by the area the contour surrounds.
        newPointList.sortByDescending { v -> Imgproc.contourArea(v) }

        var singlePointList: MutableList<MatOfPoint> = ArrayList()
        // Parse through points found
        for (v in newPointList) {
            val p = Imgproc.arcLength(v, true)
            val ap = MatOfPoint2f() // Approximate
            Imgproc.approxPolyDP(v, ap, 0.02 * p, true)

            if (ap.height() == 4) { // If 4 points exist, then it's a square/rectangle, and is what we're looking for.
                // Sidenote: I'm about 99% sure that trying to transform anything that isn't a 4 points is really hard.

                val d = MatOfPoint()
                ap.convertTo(d, CvType.CV_32S) // Convert back to CvType.CV_32S from CvType.CV_32F
                singlePointList.add(d)
                testMat = cropWithContour(
                    testMat,
                    d
                ) //This is what I'm having problems with, the actual cropping.
                break
            }
        }

        if (singlePointList.count() == 0) {
            return imageBitmap
        } else {
            // Draw the found rectangle/square on the original image Mat
            Imgproc.drawContours(testMat, singlePointList, -1, Scalar(0.0, 0.0, 255.0), 3)

            // Convert the original image Mat to a BitMap
            // WARN new bitmap width and height MUST MATCH the Mat's width and height that's being converted from
            var testBit: Bitmap =
                Bitmap.createBitmap(testMat.width(), testMat.height(), imageBitmap.config)
            Utils.matToBitmap(testMat, testBit)

            return testBit
        }


    }

    private fun cropWithContour(mat: Mat, matPoint: MatOfPoint): Mat {
        // WARNING imageview is rotated 90, so bitmap might not be how it's actually displayed.
        // TESTING
        // See https://docs.opencv.org/3.4/da/d6e/tutorial_py_geometric_transformations.html,
        // https://docs.opencv.org/3.4/da/d54/group__imgproc__transform.html#gaf73673a7e8e18ec6963e3774e6a94b87
        // https://github.com/KMKnation/Four-Point-Invoice-Transform-with-OpenCV/blob/f012c16b7508e6141a1c3a77c24680198bf1c174/four_point_object_extractor.py#L8
        // https://www.pyimagesearch.com/2014/08/25/4-point-opencv-getperspective-transform-example/
        // https://stackoverflow.com/questions/9808601/is-getperspectivetransform-broken-in-opencv-python2-wrapper
        // https://stackoverflow.com/questions/17637730/android-opencv-getperspectivetransform-and-warpperspective
        // https://laptrinhx.com/4-point-opencv-getperspective-transform-example-4048720482/
        // https://medium.com/analytics-vidhya/opencv-perspective-transformation-9edffefb2143

        val sP = matPoint.toArray().sortedWith(compareBy({ it.x }, { it.y }))

        // Before the 90* imageView rotation:
        // TopR = sP[0].x, sP[0].y
        // TopL = sP[2].x, sP[2].y
        // BottomR = sP[1].x, sP[1].y
        // BottomL = sP[3].x, sP[3].y

        val transformFrom: Mat = Mat(4, 1, CvType.CV_32FC2)
        transformFrom.put(
            0,
            0,
            sP[0].x,
            sP[0].y,
            sP[1].x,
            sP[1].y,
            sP[2].x,
            sP[2].y,
            sP[3].x,
            sP[3].y
        )
        val maxWidth = kotlin.math.max(abs(sP[0].y - sP[2].x), abs(sP[1].x - sP[3].x))
        val maxHeight = kotlin.math.max(abs(sP[0].y - sP[1].y), abs(sP[2].y - sP[3].y))

        val transformTo: Mat = Mat(4, 1, CvType.CV_32FC2)
        //transformTo.put(0, 0, 0.0, 0.0, 0.0, 400.0, 400.0, 0.0, 400.0, 400.0)
        transformTo.put(0, 0, 0.0, 0.0, 0.0, maxHeight, maxWidth, 0.0, maxWidth, maxHeight)

//        for (x in matPoint.toArray()) {// Testing.
//            Imgproc.circle(mat, x, 3, Scalar(0.0, 255.0, 0.0), 3)
//            Imgproc.putText(mat, "${x.x}, ${x.y}, ", x, Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, Scalar(0.0, 255.0, 255.0))
//        }
        val transform = Imgproc.getPerspectiveTransform(transformFrom, transformTo)
        val newMat = Mat(4, 1, CvType.CV_32S)
        Imgproc.warpPerspective(
            mat,
            newMat,
            transform,
            Size(maxWidth, maxHeight)
        ) // TODO change size to actual image display size (imageView size).
        return newMat
    }

    companion object {
        fun newInstance(): ReceiptDataEntryFragment {
            return ReceiptDataEntryFragment()
        }
    }

}