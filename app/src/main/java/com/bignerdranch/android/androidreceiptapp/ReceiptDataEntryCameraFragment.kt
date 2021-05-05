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
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
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
    private var takeLastImage: Boolean = false
    private lateinit var cameraSession: CameraCaptureSession
    private lateinit var cameraDevice: CameraDevice
    private lateinit var pixelDens: android.util.Size

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the view.
        val view = inflater.inflate(R.layout.fragment_picture_taking, container, false)

        // Get the views
        cameraTakeButton = view.findViewById(R.id.take_picture_button) as Button
        cameraImageDisplay = view.findViewById(R.id.camera_display_view) as ImageView
        cameraImageDisplay.rotation = 90F

        // Get the cameraManager for camera info
        camManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // Get the camera[0] resolution
        pixelDens = camManager.getCameraCharacteristics(camManager.cameraIdList[0])
            .get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)!!

        // Using JPEG instead of YUV_420_888 because https://stackoverflow.com/questions/28430024/convert-android-media-image-yuv-420-888-to-bitmap?answertab=votes#tab-top. Faster and easier.
        // Make an ImageReader for showing a simple preview of the receipt.
        imgReader = ImageReader.newInstance(
            pixelDens!!.width / 5,
            pixelDens!!.height / 5,
            ImageFormat.JPEG,
            10
        )

        // Set the listener for when a new image is taken
        imgReader.setOnImageAvailableListener({
            // Thanks to https://stackoverflow.com/questions/26673127/android-imagereader-acquirelatestimage-returns-invalid-jpg

            // Grab the latest image
            val latestImage = it.acquireLatestImage()

            // If we're not taking the last image, then just keep displaying the low resolution image.
            if (!takeLastImage) {
                // Convert Image JPEG to Bitmap
                val bit = jpegToBitmap(latestImage)
                // Send the Bitmap through the image handler
                val contourBit = handleImage(bit, false)
                // Display the image to the user.
                cameraImageDisplay.setImageBitmap(contourBit)
            }

            latestImage?.close() // Close the image (if it exists) so the ImageReader buffer doesn't get full.
        }, null)


        // Add the listener to the takeimage button
        cameraTakeButton.setOnClickListener {
            takeLastImage = !takeLastImage
            if (takeLastImage) {
                // Close the current preview and subsequently take a high res image, handle it, and display it.
                cameraSession.close()
            } else {
                // Go back to preview
                makeCameraStuff(imgReader.surface, CameraDevice.TEMPLATE_PREVIEW, 50)
            }
        }

        // Make a camera2 camera attached to the ImageReader surface
        makeCameraStuff(imgReader.surface, CameraDevice.TEMPLATE_PREVIEW, 50)
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

        // If we're taking the final high res image:
        if (takeLastImage) {
            // Then put the actual image in testMat2 for later referring to
            Utils.bitmapToMat(imageBitmap, testMat2)
            // Resize the testMat original image to be 1/10th the resolution.
            Imgproc.resize(testMat2, testMat, Size(testMat.width() / 10.0, testMat.height() / 10.0))
        }


        // Make image greyscale
        Imgproc.cvtColor(testMat, testGreyMat, Imgproc.COLOR_BGR2GRAY)
        // Blur the image
        Imgproc.GaussianBlur(testGreyMat, testGreyGaussianMat, Size(3.0, 3.0), 0.0)
        // Apply edge detection
        Imgproc.Canny(testGreyGaussianMat, testGreyGaussianEdgeMat, 75.0, 75.0 * 3)


        // The points that will be found by the findContours() method
        var matPointList: MutableList<MatOfPoint> = ArrayList()
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

        // Need a MutableList for the contour drawing.
        var singlePointList: MutableList<MatOfPoint> = ArrayList()

        // Parse through points found and find the points that make a 4 cornered object.
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


        // If we're taking the high res image, then:
        if (takeLastImage) {
            // Get the original high res image back
            testMat = testMat2

            // Make temporary variable to hold scaled shape points
            val temp = MatOfPoint()
            temp.fromList(
                singlePointList[0].toArray().map {
                    Point(
                        it.x * 10,
                        it.y * 10
                    ) // Scale x and y by 10, to what the original image is.
                }
            )
            singlePointList[0] = temp
        }

        // If we're cropping, then do it now.
        if (crop) {
            return cropWithContour(testMat, singlePointList[0], imageBitmap)
        }
        // If we couldn't find a 4 point object
        if (singlePointList.count() == 0) {
            // Then just give the original image back.
            return imageBitmap
        } else { // If a 4 cornered object was found:

            // Draw the found rectangle/square on the original image Mat
            Imgproc.drawContours(testMat, singlePointList, -1, Scalar(0.0, 0.0, 255.0), 3)

            // Convert the original image Mat to a BitMap
            // WARN new bitmap width and height **MUST MATCH** the Mat's width and height that's being converted from
            var testBit: Bitmap =
                Bitmap.createBitmap(testMat.width(), testMat.height(), imageBitmap.config)
            Utils.matToBitmap(testMat, testBit)

            return testBit
        }
    }

    private fun cropWithContour(mat: Mat, matPoint: MatOfPoint, origBit: Bitmap): Bitmap {
        // WARNING imageview is rotated 90 degrees to 0 the image out, but the bitmap and calculations done here are for the original 90 degree rotated image.
        // See https://docs.opencv.org/3.4/da/d6e/tutorial_py_geometric_transformations.html,
        // https://docs.opencv.org/3.4/da/d54/group__imgproc__transform.html#gaf73673a7e8e18ec6963e3774e6a94b87
        // https://github.com/KMKnation/Four-Point-Invoice-Transform-with-OpenCV/blob/f012c16b7508e6141a1c3a77c24680198bf1c174/four_point_object_extractor.py#L8
        // https://www.pyimagesearch.com/2014/08/25/4-point-opencv-getperspective-transform-example/
        // https://stackoverflow.com/questions/9808601/is-getperspectivetransform-broken-in-opencv-python2-wrapper
        // https://stackoverflow.com/questions/17637730/android-opencv-getperspectivetransform-and-warpperspective
        // https://laptrinhx.com/4-point-opencv-getperspective-transform-example-4048720482/
        // https://medium.com/analytics-vidhya/opencv-perspective-transformation-9edffefb2143

        var topL: Point
        var topR: Point
        var botL: Point
        var botR: Point

        // Sort by x value
        val sPX = matPoint.toArray().sortedWith(compareBy { it.x })
        Log.d(
            "testPointsSPX",
            "0=[${sPX[0].x}, ${sPX[0].y}],1=[${sPX[1].x}, ${sPX[1].y}],2=[${sPX[2].x}, ${sPX[2].y}],3=[${sPX[3].x}, ${sPX[3].y}]"
        )
        // Left points will be the first 2 values
        val leftPoint1 = sPX[0]
        val leftPoint2 = sPX[1]
        // Find which point is top or bottom of that side.
        if (leftPoint1.y > leftPoint2.y) {
            botL = leftPoint1
            topL = leftPoint2
        } else {
            botL = leftPoint2
            topL = leftPoint1
        }
        // Right points will be the last 2 values.
        val rightPoint1 = sPX[2]
        val rightPoint2 = sPX[3]
        // Find which point is top or bottom of that side.
        if (rightPoint1.y > rightPoint2.y) {
            botR = rightPoint1
            topR = rightPoint2
        } else {
            botR = rightPoint2
            topR = rightPoint1
        }

        // Make the points that we're going to transform from.
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

        // Get the maximum height and width that appear in the image.
        // THIS USES THE RAW IMAGE WIDTH/HEIGHT
        val maxHeight = kotlin.math.max(abs(botL.y - topL.y), abs(botR.y - topR.y))
        val maxWidth = kotlin.math.max(abs(botL.x - botR.x), abs(topL.x - topR.x))

        // Make the transformTo points.
        val transformTo = Mat(4, 1, CvType.CV_32FC2)
        // USING ORIGINAL RAW IMAGE AS HEIGHT/WIDTH
        // (0,0) = topL; (0,height) = botL; (width, 0) = topR; (width, height) = botR
        transformTo.put(0, 0, 0.0, 0.0, 0.0, maxHeight, maxWidth, 0.0, maxWidth, maxHeight)

        // Get the transform from transformFrom to transformTo
        val transform = Imgproc.getPerspectiveTransform(transformFrom, transformTo)
        // Make the newMat that will hold the transformed image.
        val newMat = Mat(4, 1, CvType.CV_32S)
        // Warp the given mat using the transform and put it in newMat
        Imgproc.warpPerspective(
            mat,
            newMat,
            transform,
            Size(maxWidth, maxHeight)
        )

        // Apply threshold to image so it comes out crisp. TODO change values to taste.
        // See https://docs.opencv.org/3.4/d7/d4d/tutorial_py_thresholding.html
        Imgproc.cvtColor(newMat, newMat, Imgproc.COLOR_BGR2GRAY)
        Imgproc.adaptiveThreshold(
            newMat,
            newMat,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            21,
            21.0
        )

        // Finally, convert the opencv MAT to Bitmap so we can actually work with it.
        var newBit: Bitmap = Bitmap.createBitmap(newMat.width(), newMat.height(), origBit.config)
        Utils.matToBitmap(newMat, newBit)
        return newBit
    }


    private fun makeCameraStuff(surf: Surface, template: Int, jpegQuality: Byte) {
        // https://medium.com/androiddevelopers/understanding-android-camera-capture-sessions-and-requests-4e54d9150295

        // Make sure we have permissions
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


        // Open the camera and give it a callback
        camManager.openCamera(camManager.cameraIdList[0], object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.d("test", "CAMERA OPEN")
                // NOTE: for some reason, I can't add multiple surfaces and hotswap them later when rebuilding the capture request for the high res image. It just errors.
                var targets: MutableList<Surface> = arrayOf(surf).toMutableList()


                // Save the camera for later.
                cameraDevice = camera

                // Deprecated, but next 'available' is version 28, so...
                // Create a capture session
                camera.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {

                        // Save this session for later closing, especially since we're repeating the request here.
                        cameraSession = session

                        // Create a request for capture
                        val capture = session.device.createCaptureRequest(template)
                        // Add the surface to the target of the camera output
                        capture.addTarget(surf)
                        // Set the quality of the JPEG image.
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
                        // This is called when this capture session is closed.
                        // If it's closed and we're wanting to take the last image, then take it.
                        // TODO lookat. This might cause other problems when switching back to the old fragment.
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
        // Prepare the new surface with actual image resolution
        var fullSizeImgReader =
            ImageReader.newInstance(pixelDens!!.width, pixelDens!!.height, ImageFormat.JPEG, 4)

        // Set the listener for when a new image is available
        fullSizeImgReader.setOnImageAvailableListener({
            // Get the image
            val latestImage = it.acquireLatestImage()
            // Convert the Image JPEG object to Bitmap
            val bit = jpegToBitmap(latestImage)
            // Apply opencv magic to the Bitmap and crop it.
            val finalImage = handleImage(bit, true)
            // Display the new magic image
            cameraImageDisplay.setImageBitmap(finalImage)

            // See https://codelabs.developers.google.com/codelabs/mlkit-android#4
            // Prepare the image to be inputted
            val inImage = InputImage.fromBitmap(finalImage, 90)
            // Init the text recognizer.
            val recognizer = TextRecognition.getClient()

            val extractor = EntityExtraction.getClient(
                EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
            )

            var canExtract = false
            extractor.downloadModelIfNeeded()
                .addOnSuccessListener {
                    Log.d("extractortest", "Model Downloaded")
                    canExtract = true
                }
                .addOnFailureListener {
                    Log.e("extractortest", "Model could not be downloaded!")
                }



            // Get the result
            val result = recognizer.process(inImage).addOnSuccessListener { visionText ->
                Log.d("visiontest", visionText.text)

//                for (b in visionText.textBlocks) {
//                    Log.d("visiontest", "newblock")
//                    for (l in b.lines) {
//                        Log.d("visiontest", l.text)
//                    }
//                }

                if (canExtract) {
                    val params = EntityExtractionParams.Builder(visionText.text).setEntityTypesFilter(
                        setOf(
                            Entity.TYPE_MONEY
                        )).build()

                    extractor.annotate(params).addOnSuccessListener {
                        for (eA in it) {
                            for (e in eA.entities) {
                                Log.d("extractortest", e.asMoneyEntity().integerPart.toString() + " " + e.asMoneyEntity().fractionalPart.toString())
                            }
                        }
                    }
                }


            }.addOnFailureListener { exc ->
                Log.e("visiontest", "ERR: FAILED TO PARSE IMAGE")
            }

            // Close the latestImage so more can be received if needed.
            latestImage?.close()
        }, null)

        // Completely remake the capture session. This shouldn't require this, but I get errors trying to hotswap.

        // Add the new high res ImageReader to the list of targets to be selected from.
        var targets: MutableList<Surface> = arrayOf(fullSizeImgReader.surface).toMutableList()
        // Create a camera2 capture session
        cameraDevice.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {

                // Create new capture for good resolution image
                val capture =
                    session.device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                // Set the JPEG to not lose quality
                capture.set(CaptureRequest.JPEG_QUALITY, 100)
                // Add the new full camera resolution image reader to take on the image.
                capture.addTarget(fullSizeImgReader.surface)

                // Start the capture
                session.capture(capture.build(), null, null)
                Log.d("test", "FULL CAMERA CONFIG SUCCESSFUL")
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("test", "FULL CAMERA CONFIG FAILED")
            }

        }, null)
    }

    /**
     * Convert from JPEG Image objects to Bitmap.
     */
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