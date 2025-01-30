package edu.gatech.ccg.slrgtk.preview

import android.graphics.RuntimeShader
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import android.graphics.RenderEffect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import org.intellij.lang.annotations.Language
@Language("AGSL")
const val HAND_LANDMARK_SHADER = """
    uniform shader composable;
    layout(color) uniform float4 _PointColor;
    layout(color) uniform float4 _LineColor;
    uniform float _Radius;
    uniform float _StrokeWidth;
    uniform int _DrawingMode;
    uniform int _LandmarksPresent;
    uniform float4 _Points[21];
    uniform float2 _Resolution;
    uniform float2 _Dimension;

    // Constants for drawing modes
    const int IMAGE_ONLY = 0;
    const int SKELETON_ONLY = 1;
    const int IMAGE_AND_SKELETON = 2;

    void drawConnection(float2 adjustedUV, float2 startPos, float2 endPos, float2 scale, inout half4 color) {
        float2 lineDir = normalize(endPos - startPos);
        float lineLength = length(endPos - startPos);
        float projection = clamp(dot(adjustedUV - startPos, lineDir), 0.0, lineLength);
        float2 closestPoint = startPos + lineDir * projection;
        float distToLine = length(adjustedUV - closestPoint);
        if (distToLine < _StrokeWidth) {
            color = half4(_LineColor.rgb, _LineColor.a);
        }
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / _Resolution;
        float imageAspect = _Dimension.x / _Dimension.y;
        float viewAspect = _Resolution.x / _Resolution.y;
        
        float2 adjustedUV = uv;
        float2 scale = float2(1, 1);
        
        if (imageAspect > viewAspect) {
            scale.x = imageAspect / viewAspect;
            adjustedUV.x = (uv.x - 0.5) / scale.x + 0.5;
        } else {
            scale.y = screenAspect / viewAspect;
            adjustedUV.y = (uv.y - 0.5) / scale.y + 0.5;
        }
        
        half4 color = half4(0.3, 0.3, 0.3, 1.0);
        if (_DrawingMode != SKELETON_ONLY) {
            color = composable.eval(adjustedUV * _Dimension);
        }

        if (_LandmarksPresent != 0 && _DrawingMode != IMAGE_ONLY) {
            drawConnection(adjustedUV, _Points[0].xy, _Points[1].xy, scale, color);
            drawConnection(adjustedUV, _Points[1].xy, _Points[2].xy, scale, color);
            drawConnection(adjustedUV, _Points[2].xy, _Points[3].xy, scale, color);
            drawConnection(adjustedUV, _Points[3].xy, _Points[4].xy, scale, color);
            drawConnection(adjustedUV, _Points[0].xy, _Points[5].xy, scale, color);
            drawConnection(adjustedUV, _Points[5].xy, _Points[9].xy, scale, color);
            drawConnection(adjustedUV, _Points[9].xy, _Points[13].xy, scale, color);
            drawConnection(adjustedUV, _Points[13].xy, _Points[17].xy, scale, color);
            drawConnection(adjustedUV, _Points[0].xy, _Points[17].xy, scale, color);
            drawConnection(adjustedUV, _Points[5].xy, _Points[6].xy, scale, color);
            drawConnection(adjustedUV, _Points[6].xy, _Points[7].xy, scale, color);
            drawConnection(adjustedUV, _Points[7].xy, _Points[8].xy, scale, color);
            drawConnection(adjustedUV, _Points[9].xy, _Points[10].xy, scale, color);
            drawConnection(adjustedUV, _Points[10].xy, _Points[11].xy, scale, color);
            drawConnection(adjustedUV, _Points[11].xy, _Points[12].xy, scale, color);
            drawConnection(adjustedUV, _Points[13].xy, _Points[14].xy, scale, color);
            drawConnection(adjustedUV, _Points[14].xy, _Points[15].xy, scale, color);
            drawConnection(adjustedUV, _Points[15].xy, _Points[16].xy, scale, color);
            drawConnection(adjustedUV, _Points[17].xy, _Points[18].xy, scale, color);
            drawConnection(adjustedUV, _Points[18].xy, _Points[19].xy, scale, color);
            drawConnection(adjustedUV, _Points[19].xy, _Points[20].xy, scale, color);

            for (int k = 0; k < 21; k++) {
                float2 pointPos = _Points[k].xy;
                float distToPoint = length(adjustedUV - pointPos);
                if (distToPoint < _Radius) {
                    color = half4(_PointColor.rgb, _PointColor.a);
                }
            }
        }
        

        return color;
    }
"""

@Composable
fun HandLandmarkAnnotator(
    modifier: Modifier = Modifier,
    imageBitmap: ImageBitmap,
    pointColor: Color = Color.Red,
    lineColor: Color = Color.Blue,
    radius: Float = 0.0075f,
    strokeWidth: Float = 0.005f,
    drawingMode: Int = 2,
    points: HandLandmarkerResult? = null
) {
    Box(modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .handLandmarkShader(
                    pointColor = pointColor,
                    lineColor = lineColor,
                    radius = radius,
                    strokeWidth = strokeWidth,
                    drawingMode = drawingMode,
                    landmarksPresent = if (points?.landmarks()?.isNotEmpty() == true) 1 else 0,
                    points = points,
                    image = imageBitmap
                )
        ) {
            drawImage(imageBitmap)
        }
    }
}

fun Modifier.handLandmarkShader(
    pointColor: Color,
    lineColor: Color,
    radius: Float,
    strokeWidth: Float,
    drawingMode: Int,
    landmarksPresent: Int,
    points: HandLandmarkerResult?,
    image: ImageBitmap
): Modifier = composed {
    val runtimeShader = remember { RuntimeShader(HAND_LANDMARK_SHADER) }

    LaunchedEffect(points) {
        runtimeShader.setFloatUniform("_Radius", radius)
        runtimeShader.setFloatUniform("_StrokeWidth", strokeWidth)
        runtimeShader.setIntUniform("_DrawingMode", drawingMode)
        runtimeShader.setIntUniform("_LandmarksPresent", landmarksPresent)
        runtimeShader.setFloatUniform("_Dimension",  image.width.toFloat(), image.height.toFloat())


        if (points?.landmarks()?.isNotEmpty() == true) {
            val landmarkPoints = points.landmarks()?.first()?.flatMap { point ->
                listOf(point.x(), point.y(), 0f, 0f)  // Each point needs 4 components (xyzw) for float4
            }?.toFloatArray() ?: FloatArray(84)  // 21 points * 4 components = 84

            runtimeShader.setFloatUniform("_Points", landmarkPoints)
        }

        runtimeShader.setColorUniform("_PointColor", android.graphics.Color.valueOf(pointColor.red, pointColor.green, pointColor.blue))
        runtimeShader.setColorUniform("_LineColor", android.graphics.Color.valueOf(lineColor.red, lineColor.green, lineColor.blue))
    }

    this.onSizeChanged { size ->
        runtimeShader.setFloatUniform("_Resolution", size.width.toFloat(), size.height.toFloat())
    }.graphicsLayer {
        renderEffect = RenderEffect.createRuntimeShaderEffect(runtimeShader, "composable").asComposeRenderEffect()
    }
}
