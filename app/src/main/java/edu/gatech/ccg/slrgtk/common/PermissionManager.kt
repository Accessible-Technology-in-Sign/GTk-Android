package edu.gatech.ccg.slrgtk.common

import android.app.Activity
import androidx.core.app.ActivityCompat

fun requestCameraPermission(activity: Activity) {
    ActivityCompat.requestPermissions(activity, arrayOf("android.permission.CAMERA"), 1);
}