package com.bignerdranch.android.androidreceiptapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import kotlin.math.abs


class ReceiptDataEntryCameraFragment : Fragment() {

    private lateinit var cameraImageDisplay: ImageView
    private lateinit var cameraTakeButton: Button
    private lateinit var camManager: CameraManager
    private lateinit var imgReader: ImageReader
    private lateinit var finalImage: Bitmap
    private var takenLastImage = false
    private var takeImage: Boolean = false
    private var takeLastImage: Boolean = false
    private lateinit var cameraSession: CameraCaptureSession
    private lateinit var cameraDevice: CameraDevice
    private lateinit var oldDetectedRectangle: MutableList<MatOfPoint>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_picture_taking, container, false)

        var cameraTakeButton = view.findViewById(R.id.take_picture_button) as Button

        cameraImageDisplay = view.findViewById(R.id.camera_display_view) as ImageView
        cameraImageDisplay.rotation = 90F

        camManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val pixelDens = camManager.getCameraCharacteristics(camManager.cameraIdList[0]).get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)


        // Using JPEG instead of YUV_420_888 because https://stackoverflow.com/questions/28430024/convert-android-media-image-yuv-420-888-to-bitmap?answertab=votes#tab-top. Faster and easier.
        imgReader = ImageReader.newInstance(pixelDens!!.width/10, pixelDens!!.height/10, ImageFormat.JPEG, 10)
        imgReader.setOnImageAvailableListener({

            // Thanks to https://stackoverflow.com/questions/26673127/android-imagereader-acquirelatestimage-returns-invalid-jpg

            // Grab the latest image
            val latestImage = it.acquireLatestImage()

            // If we're not taking the last image, then just keep displaying the low resolution image.
            if (!takeLastImage) {
                val bit = jpegToBitmap(latestImage)
                val contourBit = handleImage(bit, false) // Get a bitmap with the contours applied to it.
                cameraImageDisplay.setImageBitmap(contourBit)
            }

            if (latestImage != null)
                latestImage.close() // Close the image so the ImageReader buffer doesn't get full.

//            if (takeImage) {
//                if (!takenLastImage) {
//                    takenLastImage = true
//                    val bit = jpegToBitmap(latestImage)
//                    val finalImage = handleImage(bit, true) // Get a bitmap with the contours applied to it.
//                    cameraImageDisplay.setImageBitmap(finalImage)
//
//
//                    // See https://codelabs.developers.google.com/codelabs/mlkit-android#4
//                    val inImage = InputImage.fromBitmap(finalImage, 90)
//                    val recognizer = TextRecognition.getClient()
//                    val result = recognizer.process(inImage).addOnSuccessListener { visionText ->
//                        Log.d("visiontest", visionText.text)
//
//                    }.addOnFailureListener { exc ->
//                        Log.e("visiontest", "ERR: FAILED TO PARSE IMAGE")
//                    }
//                }
//            } else {
//
//            }
//
//            if (latestImage != null)
//                latestImage.close() // Close the image so the ImageReader buffer doesn't get full.

        }, null)


        cameraTakeButton.setOnClickListener {

            takeLastImage = !takeLastImage
            if (takeLastImage) {
                cameraSession.close()
            } else {
                makeCameraStuff(imgReader.surface, CameraDevice.TEMPLATE_PREVIEW, 50)
            }
            // Abort to take last image.

        }

        // Make a camera2 camera attached to the ImageReader surface

        makeCameraStuff(imgReader.surface, CameraDevice.TEMPLATE_PREVIEW, 100)

        return view
    }


    private fun handleImage(imageBitmap: Bitmap, crop: Boolean): Bitmap {
        // TODO start new fragment with 4 point transform.
        // https://www.pyimagesearch.com/2014/09/01/build-kick-ass-mobile-document-scanner-just-5-minutes/
        // Don't trust immutable yet...
        // TODO switch to vals eventually.
        // TODO compress into 1 var that constantly replaces itself
        var testMat: Mat = Mat()
        var testMat2: Mat = Mat()
        var testGreyMat: Mat = Mat()
        var testGreyGaussianMat: Mat = Mat()
        var testGreyGaussianEdgeMat: Mat = Mat()

        // Convert image bitmap to OpenCV Mat
        Utils.bitmapToMat(imageBitmap, testMat)

        if (takeLastImage) {
            Utils.bitmapToMat(imageBitmap, testMat2)
            Imgproc.resize(testMat2, testMat, Size(testMat.width()/10.0,testMat.height()/10.0))
        }

        //Imgproc.resize(testMat2, testMat2, Size(testMat.width().toDouble(), testMat.height().toDouble()))



        // Make image greyscale
        Imgproc.cvtColor(testMat, testGreyMat, Imgproc.COLOR_BGR2GRAY)
        // Blur the image
        //Imgproc.pyrDown(testMat,testMat)
        Imgproc.GaussianBlur(testGreyMat, testGreyGaussianMat, Size(3.0, 3.0), 0.0)
        //Imgproc.medianBlur(testGreyMat, testGreyGaussianMat, 27)
        //Imgproc.Laplacian(testGreyGaussianMat,testGreyGaussianMat, CvType.CV_8U,3,1.0,0.0, Core.BORDER_DEFAULT)
        //1 Apply edge detection
        Imgproc.Canny(testGreyGaussianMat, testGreyGaussianEdgeMat, 75.0, 75.0*3)


        var matPointList: MutableList<MatOfPoint> =
            ArrayList() // The points that will be found by the findContours() method
        var contourHierarchyMat = Mat() // No clue what this does.
        // See https://docs.opencv.org/3.4/d4/d73/tutorial_py_contours_begin.html
        Imgproc.findContours(
            testGreyGaussianEdgeMat,
            matPointList,
            contourHierarchyMat,
            Imgproc.RETR_LIST,
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
                break
            }
        }


//        val pixelDens = camManager.getCameraCharacteristics(camManager.cameraIdList[0]).get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        if (takeLastImage) {
            testMat = testMat2
            val temp = MatOfPoint()

            temp.fromList(
                singlePointList[0].toArray().map {
                    Point(it.x * 10, it.y * 10)
                }
            )
            singlePointList[0] = temp
        }

        oldDetectedRectangle = singlePointList
        if (crop) {
            return cropWithContour(testMat, singlePointList[0], imageBitmap)
        }
        //testMat = testMat2
        if (singlePointList.count() == 0) {
            var testBit: Bitmap = Bitmap.createBitmap(testMat.width(), testMat.height(), imageBitmap.config)
            Utils.matToBitmap(testMat, testBit)
            return testBit
        } else {

            // Draw the found rectangle/square on the original image Mat
            Imgproc.drawContours(testMat, singlePointList, -1, Scalar(0.0, 0.0, 255.0), 3)

            for (x in singlePointList[0].toArray()) {// Testing.
                Imgproc.circle(testMat, x, 3, Scalar(0.0, 255.0, 0.0), 3)
                Imgproc.putText(testMat, "${x.x}, ${x.y}", x, Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, Scalar(0.0, 255.0, 255.0))
            }

            val sP = singlePointList[0].toArray().sortedWith(compareBy({ it.x }, { it.y }))
            Log.d("testPoints", "0=[${sP[0].x}, ${sP[0].y}],1=[${sP[1].x}, ${sP[1].y}],2=[${sP[2].x}, ${sP[2].y}],3=[${sP[3].x}, ${sP[3].y}]")

            // Convert the original image Mat to a BitMap
            // WARN new bitmap width and height **MUST MATCH** the Mat's width and height that's being converted from
            var testBit: Bitmap =
                Bitmap.createBitmap(testMat.width(), testMat.height(), imageBitmap.config)
            Utils.matToBitmap(testMat, testBit)

            return testBit
        }
    }

    private fun cropWithContour(mat: Mat, matPoint: MatOfPoint, origBit: Bitmap): Bitmap {
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


//        val botL = sP[1]
//        val topL = sP[0]
//        val botR = sP[2]
//        val topR = sP[3]

        var topL: Point
        var topR: Point
        var botL: Point
        var botR: Point

        val sPX = matPoint.toArray().sortedWith(compareBy({ it.x }))
        Log.d("testPointsSPX", "0=[${sPX[0].x}, ${sPX[0].y}],1=[${sPX[1].x}, ${sPX[1].y}],2=[${sPX[2].x}, ${sPX[2].y}],3=[${sPX[3].x}, ${sPX[3].y}]")
        val leftPoint1 = sPX[0]
        val leftPoint2 = sPX[1]
        if (leftPoint1.y > leftPoint2.y) {
            botL = leftPoint1
            topL = leftPoint2
        } else {
            botL = leftPoint2
            topL = leftPoint1
        }
        val rightPoint1 = sPX[2]
        val rightPoint2 = sPX[3]
        if (rightPoint1.y > rightPoint2.y) {
            botR = rightPoint1
            topR = rightPoint2
        } else {
            botR = rightPoint2
            topR = rightPoint1
        }


        val transformFrom = Mat(4, 1, CvType.CV_32FC2)
        transformFrom.put(
            0,
            0,
            topL.x,
            topL.y,
            botL.x,
            botL.y,
            topR.x,
            topR.y,
            botR.x,
            botR.y
        )


        // BotL = 0
        // TopL = 1
        // BotR = 3
        // TopR = 2
        // THIS USES THE RAW IMAGE WIDTH/HEIGHT
        val maxHeight = kotlin.math.max(abs(botL.y - topL.y), abs(botR.y - topR.y))
        val maxWidth = kotlin.math.max(abs(botL.x - botR.x), abs(topL.x - topR.x))

        Log.d("testwidth/height","${maxWidth}, ${maxHeight}")

//        val maxWidth = kotlin.math.max(abs(sP[0].y - sP[2].x), abs(sP[1].x - sP[3].x))
//        val maxHeight = kotlin.math.max(abs(sP[0].y - sP[1].y), abs(sP[2].y - sP[3].y))

        val transformTo = Mat(4, 1, CvType.CV_32FC2)

        //OLD: expecting: botL, topL, botR, topR

        // USING ORIGINAL RAW IMAGE AS HEIGHT/WIDTH
        // (0,0) = topL; (0,height) = botL; (width, 0) = topR; (width, height) = botR
        transformTo.put(0, 0, 0.0, 0.0, 0.0, maxHeight, maxWidth, 0.0, maxWidth, maxHeight)
        //transformTo.put(0, 0, 0.0, 0.0, 0.0, 500.0, 500.0, 0.0, 500.0, 500.0)

        val transform = Imgproc.getPerspectiveTransform(transformFrom, transformTo)
        val newMat = Mat(4, 1, CvType.CV_32S)
        Imgproc.warpPerspective(
            mat,
            newMat,
            transform,
            Size(maxWidth, maxHeight)
        ) // TODO change size to actual image display size (imageView size).

        // Apply threshold to image so it comes out crisp. TODO change values to taste.
        // See https://docs.opencv.org/3.4/d7/d4d/tutorial_py_thresholding.html

        Imgproc.cvtColor(newMat, newMat, Imgproc.COLOR_BGR2GRAY)
        Imgproc.adaptiveThreshold(newMat, newMat, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 21, 21.0)
        //Imgproc.threshold(newMat, newMat, 120.0, 255.0, Imgproc.THRESH_TRIANGLE)

        var newBit: Bitmap = Bitmap.createBitmap(newMat.width(), newMat.height(), origBit.config)
        Utils.matToBitmap(newMat, newBit)
        return newBit
    }


    private fun makeCameraStuff(surf: Surface, template: Int, jpegQuality: Byte) {
        // https://medium.com/androiddevelopers/understanding-android-camera-capture-sessions-and-requests-4e54d9150295

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
                cameraDevice = camera
                camera.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {

                        // Save this session for later use.
                        cameraSession = session

                        // Create a request for capture
                        val capture = session.device.createCaptureRequest(template)
                        capture.addTarget(surf)
                        capture.set(CaptureRequest.JPEG_QUALITY, jpegQuality)

                        // Make the request repeat
                        session.setRepeatingRequest(capture.build(), null, null)
                        Log.d("test", "CAMERA CONFIG SUCCESSFUL")
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("test", "CAMERA CONFIG FAILED")
                    }

                    override fun onClosed(session: CameraCaptureSession) {
                        super.onClosed(session)
                        if (takeLastImage) {
                            takeHighResImage()
                        }
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


    private fun takeHighResImage() {
        val pixelDens = camManager.getCameraCharacteristics(camManager.cameraIdList[0]).get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)

        // Prepare the new surface with actual image resolution
        var fullSizeImgReader = ImageReader.newInstance(pixelDens!!.width, pixelDens!!.height, ImageFormat.JPEG, 4)

        fullSizeImgReader.setOnImageAvailableListener({
            val latestImage = it.acquireLatestImage()
            val bit = jpegToBitmap(latestImage)
            val finalImage = handleImage(bit, true) // Get a bitmap with the contours applied to it.
            cameraImageDisplay.setImageBitmap(finalImage)

            latestImage?.close()
        }, null)

        var targets: MutableList<Surface> = arrayOf(fullSizeImgReader.surface).toMutableList()
        cameraDevice.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {

                // Create new capture for good resolution image
                val capture = session.device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                // Set the JPEG to not lose quality
                capture.set(CaptureRequest.JPEG_QUALITY, 100)
                // Add the new full camera resolution image reader to take on the image.
                capture.addTarget(fullSizeImgReader.surface)

                session.capture(capture.build(), null, null)
                Log.d("test", "FULL CAMERA CONFIG SUCCESSFUL")
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("test", "FULL CAMERA CONFIG FAILED")
            }

        }, null)


    }

    private fun jpegToBitmap(img: Image): Bitmap {
        val byteBuffer: ByteBuffer = img.planes[0].buffer
        byteBuffer.rewind()
        val byteArray = ByteArray(byteBuffer.remaining())
        byteBuffer.get(byteArray)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }


    companion object {
        fun newInstance(): ReceiptDataEntryCameraFragment {
            return ReceiptDataEntryCameraFragment()
        }
    }
}