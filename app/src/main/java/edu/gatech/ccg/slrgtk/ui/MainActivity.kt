package edu.gatech.ccg.slrgtk.ui

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import edu.gatech.ccg.slrgtk.engine.BufferTestEngine
import edu.gatech.ccg.slrgtk.engine.CameraTestEngine
import edu.gatech.ccg.slrgtk.engine.LiteRTTestEngine
import edu.gatech.ccg.slrgtk.engine.MPTestEngine
import edu.gatech.ccg.slrgtk.preview.HandLandmarkAnnotator
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.LinkedList

@ExperimentalCamera2Interop
class MainActivity : ComponentActivity() {
    private lateinit var engine: LiteRTTestEngine
    private var image = MutableStateFlow(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    private var points: MutableStateFlow<HandLandmarkerResult> = MutableStateFlow(object: HandLandmarkerResult() {
        override fun timestampMs(): Long {
            return 0
        }
        override fun landmarks(): MutableList<MutableList<NormalizedLandmark>> {
            return LinkedList()
        }
        override fun worldLandmarks(): MutableList<MutableList<Landmark>> {
            return LinkedList()
        }
        override fun handednesses(): MutableList<MutableList<Category>> {
            return LinkedList()
        }

    })
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        engine = CameraTestEngine(this)
//        engine = MPTestEngine(this)
//        engine = BufferTestEngine(this)
        engine = LiteRTTestEngine(this)
        engine.mp.addCallback("painter") {
            result ->
            image.value = result.originalImage
            points.value = result.result
        }
        enableEdgeToEdge()
        setContent {
//                Content()
            HandLandmarkScreen()
        }
    }
    @Composable
    fun Content() {
        val im by image.collectAsState()
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Canvas(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                drawImage(
                    image = im.asImageBitmap(),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f) // Starting position on the Canvas
                )
            }
        }
    }


    @Composable
    fun HandLandmarkScreen() {
        val im by image.collectAsState()
        val p by points.collectAsState()

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            HandLandmarkAnnotator(
                imageBitmap = im.asImageBitmap(),
                points = p,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

}
