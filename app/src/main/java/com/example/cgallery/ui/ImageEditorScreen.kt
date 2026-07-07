package com.example.cgallery.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cgallery.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    item: MediaItem,
    onSave: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    var rotation by remember { mutableStateOf(0f) }
    val paths = remember { mutableStateListOf<DrawnPath>() }
    val currentPoints = remember { mutableStateListOf<Offset>() }
    var currentColor by remember { mutableStateOf(Color.Red) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Image") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton({ rotation = (rotation + 90f) % 360f }) { Icon(Icons.Default.RotateRight, "rotate") }
                    IconButton({
                        scope.launch {
                            val bitmap = renderToBitmap(context, item.uri, rotation, paths, canvasSize)
                            onSave(bitmap)
                        }
                    }) { Icon(Icons.Default.Save, "save") }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton({ currentColor = Color.Red }) { Icon(Icons.Default.Edit, null, tint = Color.Red) }
                    IconButton({ currentColor = Color.Green }) { Icon(Icons.Default.Edit, null, tint = Color.Green) }
                    IconButton({ currentColor = Color.Blue }) { Icon(Icons.Default.Edit, null, tint = Color.Blue) }
                    IconButton({ paths.clear() }) { Icon(Icons.Default.Delete, "clear") }
                }
            }
        }
    ) { p ->
        Box(Modifier.fillMaxSize().padding(p).background(Color.Black).onGloballyPositioned { canvasSize = it.size.toSize() }) {
            // We rotate the entire content Box including Canvas to keep drawing simple
            Box(Modifier.fillMaxSize().graphicsLayer(rotationZ = rotation)) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(item.uri).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints.clear()
                            currentPoints.add(offset)
                        },
                        onDrag = { change, _ ->
                            currentPoints.add(change.position)
                        },
                        onDragEnd = {
                            paths.add(DrawnPath(currentPoints.toList(), currentColor))
                            currentPoints.clear()
                        }
                    )
                }) {
                    paths.forEach { path ->
                        drawPath(path.points, path.color)
                    }
                    if (currentPoints.isNotEmpty()) {
                        drawPath(currentPoints.toList(), currentColor)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPath(points: List<Offset>, color: Color) {
    if (points.size < 2) return
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }
    drawPath(path, color, style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

data class DrawnPath(val points: List<Offset>, val color: Color)

suspend fun renderToBitmap(
    context: android.content.Context, 
    uri: android.net.Uri, 
    rotation: Float, 
    paths: List<DrawnPath>,
    canvasSize: Size
): Bitmap = withContext(Dispatchers.IO) {
    val inputStream = context.contentResolver.openInputStream(uri)
    val source = BitmapFactory.decodeStream(inputStream)
    inputStream?.close() ?: return@withContext Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    
    val matrix = Matrix().apply { postRotate(rotation) }
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    
    val output = rotated.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)
    
    val bitmapWidth = rotated.width.toFloat()
    val bitmapHeight = rotated.height.toFloat()
    val canvasWidth = canvasSize.width
    val canvasHeight = canvasSize.height
    
    // Calculate scale and offset for ContentScale.Fit
    val scale = Math.min(canvasWidth / bitmapWidth, canvasHeight / bitmapHeight)
    val dx = (canvasWidth - bitmapWidth * scale) / 2f
    val dy = (canvasHeight - bitmapHeight * scale) / 2f
    
    val paint = Paint().apply {
        strokeWidth = 20f / scale
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    paths.forEach { drawnPath ->
        paint.color = drawnPath.color.toArgb()
        val path = android.graphics.Path()
        if (drawnPath.points.isNotEmpty()) {
            val p0 = drawnPath.points[0]
            path.moveTo((p0.x - dx) / scale, (p0.y - dy) / scale)
            for (i in 1 until drawnPath.points.size) {
                val p = drawnPath.points[i]
                path.lineTo((p.x - dx) / scale, (p.y - dy) / scale)
            }
        }
        canvas.drawPath(path, paint)
    }
    output
}
