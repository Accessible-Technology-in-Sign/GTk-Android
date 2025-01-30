package edu.gatech.ccg.slrgtk.engine

import android.app.Activity
import android.util.Log
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import edu.gatech.ccg.slrgtk.camera.StreamCameraX
import edu.gatech.ccg.slrgtk.common.requestCameraPermission
import edu.gatech.ccg.slrgtk.model.MPHands
import edu.gatech.ccg.slrgtk.model.MPVisionInput

@ExperimentalCamera2Interop
class MPTestEngine(activity: Activity) {
    var camera: StreamCameraX = StreamCameraX()
    var mp: MPHands = MPHands(activity)
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
        camera.poll(activity)
    }
}