package edu.gatech.ccg.slrgtk.engine

import android.app.Activity
import android.util.Log
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import edu.gatech.ccg.slrgtk.camera.StreamCameraX
import edu.gatech.ccg.slrgtk.common.requestCameraPermission
import edu.gatech.ccg.slrgtk.model.MPHands
import edu.gatech.ccg.slrgtk.model.MPVisionInput
import edu.gatech.ccg.slrgtk.system.Buffer

@ExperimentalCamera2Interop
class BufferTestEngine(activity: Activity) {
    var camera: StreamCameraX = StreamCameraX()
    var mp: MPHands = MPHands(activity)
    var buffer: Buffer<HandLandmarkerResult> = Buffer()
    init {
        requestCameraPermission(activity)
        camera.addCallback("ImageReceiver") {
                image ->
            mp.run(MPVisionInput(image, System.currentTimeMillis()))
        }
        mp.addCallback("MPHandsPrinter") {
                result ->
            Log.d("MP", "${result.result}")
        }
        mp.addCallback("BufferManager") {
                result ->
            if (result.result.landmarks().size > 0)
                // TODO: maybe some logic to clear if the results are blank for extended periods of time?
            buffer.addElement(result.result)
        }
        buffer.addCallback("BufferPrinter") {
            bufferedVal ->
            Log.d("Buffer", "Got ${bufferedVal.size} elements.")
        }
        camera.poll(activity)
    }
}