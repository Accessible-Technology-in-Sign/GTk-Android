package edu.gatech.ccg.slrgtk.model

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import edu.gatech.ccg.slrgtk.common.CallbackManager
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.util.concurrent.ConcurrentHashMap


interface ModelI<I> {
    fun run(input: I)
}

data class MPVisionInput (
    val image: Bitmap,
    val timestamp: Long
);

data class MPHandsOutput (
    val originalImage: Bitmap,
    val result: HandLandmarkerResult
)

class MPHands(
    private val context: Context,
    private val modelPath: String = "file:///android_asset/hand_landmarker.task",
    private val runningMode: RunningMode = RunningMode.LIVE_STREAM,
    private val numHands: Int = 1,
    private val minHandDetectionConfidence: Float = 0.5f,
    private val minTrackingConfidence: Float = 0.5f,
    private val minHandPresenceConfidence: Float = 0.5f
) : ModelI<MPVisionInput>, CallbackManager<MPHandsOutput>() {

    private val outputInputLookup = ConcurrentHashMap<Long, Bitmap>()

    private val handLandmarker: HandLandmarker by lazy {
        HandLandmarker.createFromOptions(
            context,
            HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(modelPath).build())
                .setRunningMode(runningMode)
                .setNumHands(numHands)
                .setMinHandDetectionConfidence(minHandDetectionConfidence)
                .setMinTrackingConfidence(minTrackingConfidence)
                .setMinHandPresenceConfidence(minHandPresenceConfidence)
                .apply {
                    if (runningMode == RunningMode.LIVE_STREAM) {
                        setResultListener { result, _ ->
                            outputInputLookup[result.timestampMs()]?.let { bitmap ->
                                triggerCallbacks(
                                    MPHandsOutput(originalImage = bitmap, result = result)
                                )
                                outputInputLookup.remove(result.timestampMs())
                            }
                            // Clean up old timestamps
                            val oldTimestamps = outputInputLookup.keys.filter { it < result.timestampMs() }
                            oldTimestamps.forEach { timestamp ->
                                outputInputLookup.remove(timestamp)?.recycle()
                            }
                        }
                    }
                }
                .build()
        )
    }

    override fun run(input: MPVisionInput) {
        when (runningMode) {
            RunningMode.LIVE_STREAM -> {
                input.image?.let { bitmap ->
                    val timestamp = System.currentTimeMillis()
                    outputInputLookup[timestamp] = bitmap
                    handLandmarker.detectAsync(BitmapImageBuilder(input.image).build(), timestamp)
                }
            }
            RunningMode.IMAGE -> {
                input.image?.let { bitmap ->
                    val results = handLandmarker.detect(BitmapImageBuilder(input.image).build())
                    triggerCallbacks(MPHandsOutput(originalImage = bitmap, result = results))
                }
            }
            RunningMode.VIDEO -> {
                input.image?.let { bitmap ->
                    val timestamp = System.currentTimeMillis()
                    val results = handLandmarker.detectForVideo(BitmapImageBuilder(input.image).build(), timestamp)
                    triggerCallbacks(MPHandsOutput(originalImage = bitmap, result = results))
                }
            }
        }
    }
}

data class ClassPredictions (
    val classes: List<String>,
    val probabilities: FloatArray
);

data class PopsignIsolatedSLRInput (
    val result: List<HandLandmarkerResult>
);

class LiteRTPopsignIsolatedSLR(
    model: File,
    val mapping: List<String>
) : ModelI<PopsignIsolatedSLRInput>, CallbackManager<ClassPredictions>() {
    val interpreter: Interpreter = Interpreter(
        model,
        Interpreter.Options().apply{ this.setNumThreads(1)}
    )

    init {
        interpreter.allocateTensors()
    }

    private val modelInputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 60, 21 * 2, 1), DataType.FLOAT32)
    private val modelOutputTensor = TensorBuffer.createFixedSize(interpreter.getOutputTensor(0).shape(), DataType.FLOAT32)

    override fun run(input: PopsignIsolatedSLRInput) {
        modelInputTensor.loadArray(getInputArray(input))
        interpreter.run(modelInputTensor.buffer,  modelOutputTensor.buffer)
        triggerCallbacks(ClassPredictions(mapping, modelOutputTensor.floatArray))
    }

    @Deprecated("Should be replaced with it's own system for handling various tensor structuring strategies (padding and dropping frames etc.)")
    private fun getInputArray(input: PopsignIsolatedSLRInput): FloatArray {
        return input.result.flatMap { handResult ->
            handResult.landmarks().flatMap { handLandmarks ->
                handLandmarks.flatMap { landmark ->
                    listOf(landmark.x(), landmark.y())
                }
            }
        }.toFloatArray()
    }
}
