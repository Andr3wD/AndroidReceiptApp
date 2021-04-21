package com.bignerdranch.android.androidreceiptapp

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

class ReceiptDataEntryActivity : Fragment() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_data_entry)
//    }

    private lateinit var cameraButton: ImageButton
    private lateinit var cameraImageDisplay: ImageView
    private lateinit var surfView: SurfaceView
    private val REQUEST_IMAGE_CAPTURE = 1
    lateinit var camManager: CameraManager
    lateinit var imgReader: ImageReader


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_data_entry, container, false)


        cameraButton = view.findViewById(R.id.receipt_camera)
        cameraImageDisplay = view.findViewById(R.id.receipt_camera_image)
        cameraImageDisplay.rotation = 90.0F
        imgReader = ImageReader.newInstance(500,500,ImageFormat.JPEG, 10)
        imgReader.setOnImageAvailableListener(ImageReader.OnImageAvailableListener {

            val latestImage = it.acquireLatestImage()
            val bb: ByteBuffer = latestImage.planes[0].buffer
            bb.rewind()
            val ba = ByteArray(bb.remaining())
            bb.get(ba)
            val bit: Bitmap = BitmapFactory.decodeByteArray(ba, 0, ba.size)
            latestImage.close()
            val contourBit = getContours(bit)

            cameraImageDisplay.setImageBitmap(contourBit)


        }, null)
        makeCameraStuff(imgReader.surface)

//        surfView = view.findViewById(R.id.receipt_camera_image)
        cameraButton.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            Log.d("Receipt_Entry", "Trying camera intent")
            try {
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
                Log.d("Receipt_Entry", "Intent completed")
            } catch (e: ActivityNotFoundException) {
                Log.e("Receipt_Entry", "Failed to find camera activity!")
            }
        }

//        surfView.holder.addCallback(object: SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {
//                Log.d("test", "Surface Created!")
//                holder.setFixedSize(500,500)
//                makeCameraStuff(holder)
//            }
//
//            override fun surfaceChanged(
//                holder: SurfaceHolder,
//                format: Int,
//                width: Int,
//                height: Int
//            ) {
//                Log.d("test", "SURFACE CHANGED")
//            }
//
//            override fun surfaceDestroyed(holder: SurfaceHolder) {
//                Log.d("test", "SURFACE DESTROYED!")
//            }
//
//        })



        return view
    }

    private fun makeCameraStuff(surf: Surface) {
        camManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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


        camManager.openCamera(camManager.cameraIdList[0], object: CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.d("test","CAMERA OPEN")
                var targets: MutableList<Surface> = arrayOf(surf).toMutableList()
                // Deprecated, but next 'available' is version 28, so...

                camera.createCaptureSession(targets, object: CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        val capture = session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        capture.addTarget(surf)

                        session.setRepeatingRequest(capture.build(), null, null)
                        Log.d("test","CAMERA CONFIG SUCCESSFUL")
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("test","CAMERA CONFIG FAILED")
                    }

                }, null)
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.d("test","CAMERA DISCONNECT")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.e("test","CAMERA ERR")
            }

        }, null)
    }

    private fun getContours(imageBitmap: Bitmap): Bitmap {
        // TODO start new fragment with 4 point transform.
        // https://www.pyimagesearch.com/2014/09/01/build-kick-ass-mobile-document-scanner-just-5-minutes/
        // Don't trust immutable yet...
        // TODO compress into 1 file that constantly replaces itself
        var testMat: Mat = Mat()
        var testGreyMat: Mat = Mat()
        var testGreyGaussianMat: Mat = Mat()
        var testGreyGaussianEdgeMat: Mat = Mat()

        Utils.bitmapToMat(imageBitmap, testMat)
        // Greyscale
        Imgproc.cvtColor(testMat, testGreyMat, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(testGreyMat, testGreyGaussianMat, Size(5.0, 5.0), 0.0)
        Imgproc.Canny(testGreyGaussianMat, testGreyGaussianEdgeMat, 75.0, 75.0*3)


        var matPointList: MutableList<MatOfPoint> = ArrayList()
        var contourHierarchyMat = Mat()
        // See https://docs.opencv.org/3.4/d4/d73/tutorial_py_contours_begin.html
        Imgproc.findContours(testGreyGaussianEdgeMat, matPointList, contourHierarchyMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        // Convert to MatOfPoint2f https://stackoverflow.com/questions/11273588/how-to-convert-matofpoint-to-matofpoint2f-in-opencv-java-api
        var newList: MutableList<MatOfPoint2f> = ArrayList()
        for (v in matPointList) {
            val d = MatOfPoint2f()
            v.convertTo(d, CvType.CV_32F)
            newList.add(d)
        }

        newList.sortByDescending { v -> Imgproc.contourArea(v) }

        var newoneList: MutableList<MatOfPoint> = ArrayList()
//            val d = MatOfPoint()
//            newList.get(0).convertTo(d, CvType.CV_32S)
//            newoneList.add(d)
        for (v in newList) {
            val p = Imgproc.arcLength(v, true)
            val ap = MatOfPoint2f()
            Imgproc.approxPolyDP(v, ap, 0.02 * p, true)
            Log.d("test", ap.size().toString())
            if (ap.height() == 4) {
                val d = MatOfPoint()
                ap.convertTo(d, CvType.CV_32S)
                newoneList.add(d)
                break
            }
        }


//        Log.d("test", newoneList[0].toString())

        Imgproc.drawContours(testMat, newoneList, -1, Scalar(0.0, 0.0, 255.0), 3)


        var testBit: Bitmap = Bitmap.createBitmap(imageBitmap.width, imageBitmap.height, imageBitmap.config)
        Utils.matToBitmap(testMat, testBit)
//            cameraImageDisplay.setImageBitmap(testBit)

        return testBit

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

//        if (resultCode == Activity.RESULT_OK) {
//            val imageBitmap = data?.extras?.get("data") as Bitmap
//
//            // TODO start new fragment with 4 point transform.
//
//
//
//
//            // https://www.pyimagesearch.com/2014/09/01/build-kick-ass-mobile-document-scanner-just-5-minutes/
//            // Don't trust immutable yet...
//            // TODO compress into 1 file that constantly replaces itself
//            var testMat: Mat = Mat()
//            var testGreyMat: Mat = Mat()
//            var testGreyGaussianMat: Mat = Mat()
//            var testGreyGaussianEdgeMat: Mat = Mat()
//
//            Utils.bitmapToMat(imageBitmap, testMat)
//            // Greyscale
//            Imgproc.cvtColor(testMat, testGreyMat, Imgproc.COLOR_BGR2GRAY)
//            Imgproc.GaussianBlur(testGreyMat, testGreyGaussianMat, Size(5.0, 5.0), 0.0)
//            Imgproc.Canny(testGreyGaussianMat, testGreyGaussianEdgeMat, 75.0, 75.0*3)
//
//
//            var matPointList: MutableList<MatOfPoint> = ArrayList()
//            var contourHierarchyMat = Mat()
//            // See https://docs.opencv.org/3.4/d4/d73/tutorial_py_contours_begin.html
//            Imgproc.findContours(testGreyGaussianEdgeMat, matPointList, contourHierarchyMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
//
//            // Convert to MatOfPoint2f https://stackoverflow.com/questions/11273588/how-to-convert-matofpoint-to-matofpoint2f-in-opencv-java-api
//            var newList: MutableList<MatOfPoint2f> = ArrayList()
//            for (v in matPointList) {
//                val d = MatOfPoint2f()
//                v.convertTo(d, CvType.CV_32F)
//                newList.add(d)
//            }
//
//            newList.sortByDescending { v -> Imgproc.contourArea(v) }
//
//            var newoneList: MutableList<MatOfPoint> = ArrayList()
////            val d = MatOfPoint()
////            newList.get(0).convertTo(d, CvType.CV_32S)
////            newoneList.add(d)
//            for (v in newList) {
//                val p = Imgproc.arcLength(v, true)
//                val ap = MatOfPoint2f()
//                Imgproc.approxPolyDP(v, ap, 0.02 * p, true)
//                Log.d("test", ap.size().toString())
//                if (ap.height() == 4) {
//                    val d = MatOfPoint()
//                    ap.convertTo(d, CvType.CV_32S)
//                    newoneList.add(d)
//                    break
//                }
//            }
//
//           Log.d("test", newoneList[0].toString())
//
//            Imgproc.drawContours(testMat, newoneList, -1, Scalar(0.0, 0.0, 255.0))
//
//
////            var testBit: Bitmap = Bitmap.createBitmap(imageBitmap.width, imageBitmap.height, imageBitmap.config)
////            Utils.matToBitmap(testMat, testBit)
////            cameraImageDisplay.setImageBitmap(testBit)
//
//
//        }
    }

    companion object {
        fun newInstance(): ReceiptDataEntryActivity {
            return ReceiptDataEntryActivity()
        }
    }

}