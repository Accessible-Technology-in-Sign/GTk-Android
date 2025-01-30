package edu.gatech.ccg.slrgtk.engine

import android.app.Activity
import android.graphics.Bitmap
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import edu.gatech.ccg.slrgtk.camera.StreamCameraX
import edu.gatech.ccg.slrgtk.common.requestCameraPermission

@ExperimentalCamera2Interop
class CameraTestEngine (
    activity: Activity
) {
    var camera = StreamCameraX()
    init {
        requestCameraPermission(activity)
        camera.poll(activity)
    }
}