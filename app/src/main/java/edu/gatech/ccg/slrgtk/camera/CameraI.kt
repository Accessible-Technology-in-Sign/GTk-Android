package edu.gatech.ccg.slrgtk.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CaptureRequest
import android.view.Display
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import edu.gatech.ccg.slrgtk.common.CallbackManager
import java.util.concurrent.Executors

interface CameraI<T> {
    fun poll(context: Context)
    fun pause(context: Context)
}

@ExperimentalCamera2Interop
class StreamCameraX(
    private val enableISO: Boolean = false,
    private val cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
) : CallbackManager<Bitmap>(), CameraI<Bitmap> {
    private val executor = Executors.newSingleThreadExecutor()
    private var orientationEventListener: OrientationEventListener? = null
    private var currentRotation = Surface.ROTATION_0

    private val useCase: UseCase = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .setTargetRotation(currentRotation)
        .also {
            Camera2Interop.Extender(it)
                .setCaptureRequestOption(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    if (enableISO) CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                    else CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
                )
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    if (enableISO) CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                    else CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                )
        }
        .build()
        .also { imageAnalysis ->
            imageAnalysis.setAnalyzer(executor) { image ->
                val bitmap = image.toBitmap()
                val rotatedBitmap = when (image.imageInfo.rotationDegrees) {
                    0 -> bitmap
                    90 -> rotateBitmap(bitmap, 90f)
                    180 -> rotateBitmap(bitmap, 180f)
                    270 -> rotateBitmap(bitmap, 270f)
                    else -> bitmap
                }

                val finalBitmap = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                    mirrorBitmap(rotatedBitmap)
                } else {
                    rotatedBitmap
                }

                triggerCallbacks(finalBitmap)
                image.close()
            }
        }

    private fun setupOrientationListener(context: Context) {
        orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return

                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                if (currentRotation != rotation) {
                    currentRotation = rotation
                    (useCase as ImageAnalysis).targetRotation = rotation
                }
            }
        }
    }

    override fun poll(context: Context) {
        setupOrientationListener(context)
        orientationEventListener?.enable()

        val cameraProviderPromise = ProcessCameraProvider.getInstance(context)
        cameraProviderPromise.addListener({
            cameraProviderPromise.get().also { cameraProvider ->
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    ProcessLifecycleOwner.get(),
                    cameraSelector,
                    useCase
                )
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun pause(context: Context) {
        orientationEventListener?.disable()
        val cameraProviderPromise = ProcessCameraProvider.getInstance(context)
        cameraProviderPromise.addListener({
            cameraProviderPromise.get().unbindAll()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun mirrorBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
