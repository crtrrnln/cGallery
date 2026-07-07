package com.example.cgallery.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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

enum class EditMode { DRAW, ROTATE, CROP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    item: MediaItem,
    onSave: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    var editMode by remember { mutableStateOf(EditMode.DRAW) }
    var rotation by remember { mutableStateOf(0f) }
    val paths = remember { mutableStateListOf<DrawnPath>() }
    val currentPoints = remember { mutableStateListOf<Offset>() }
    var currentColor by remember { mutableStateOf(Color.Red) }
    var cropRect by remember { mutableStateOf<Rect?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Image") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton({
                        scope.launch {
                            val bitmap = renderToBitmap(context, item.uri, rotation, paths, canvasSize, cropRect)
                            onSave(bitmap)
                        }
                    }) { Icon(Icons.Default.Save, "save") }
                }
            )
        },
        bottomBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                AnimatedVisibility(visible = editMode == EditMode.ROTATE) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Rotation: ${rotation.toInt()}°", style = MaterialTheme.typography.labelMedium)
                        Slider(value = rotation, onValueChange = { rotation = it }, valueRange = -180f..180f)
                    }
                }
                BottomAppBar {
                    IconButton({ editMode = EditMode.DRAW }, colors = if (editMode == EditMode.DRAW) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()) { Icon(Icons.Default.Edit, "draw") }
                    IconButton({ editMode = EditMode.ROTATE }, colors = if (editMode == EditMode.ROTATE) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()) { Icon(Icons.Default.RotateRight, "rotate") }
                    IconButton({ editMode = EditMode.CROP }, colors = if (editMode == EditMode.CROP) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()) { Icon(Icons.Default.Crop, "crop") }
                    
                    Spacer(Modifier.weight(1f))
                    
                    if (editMode == EditMode.DRAW) {
                        ColorPicker(currentColor) { currentColor = it }
                        IconButton({ paths.clear() }) { Icon(Icons.Default.Delete, "clear") }
                    } else if (editMode == EditMode.CROP) {
                        IconButton({ cropRect = null }) { Icon(Icons.Default.RestartAlt, "reset crop") }
                    }
                }
            }
        }
    ) { p ->
        Box(Modifier.fillMaxSize().padding(p).background(Color.Black).onGloballyPositioned { canvasSize = it.size.toSize() }) {
            Box(Modifier.fillMaxSize().graphicsLayer(rotationZ = rotation)) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(item.uri).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                Canvas(modifier = Modifier.fillMaxSize().pointerInput(editMode) {
                    if (editMode == EditMode.DRAW) {
                        detectDragGestures(
                            onDragStart = { offset -> currentPoints.clear(); currentPoints.add(offset) },
                            onDrag = { change, _ -> currentPoints.add(change.position) },
                            onDragEnd = { paths.add(DrawnPath(currentPoints.toList(), currentColor)); currentPoints.clear() }
                        )
                    } else if (editMode == EditMode.CROP) {
                        detectDragGestures(
                            onDragStart = { offset -> if (cropRect == null) cropRect = Rect(offset, Size(10f, 10f)) },
                            onDrag = { change, _ ->
                                val cur = cropRect ?: Rect(change.position, Size(0f, 0f))
                                cropRect = Rect(cur.left, cur.top, change.position.x, change.position.y)
                            }
                        )
                    }
                }) {
                    if (editMode == EditMode.DRAW || editMode == EditMode.ROTATE) {
                        paths.forEach { drawPath(it.points, it.color) }
                        if (currentPoints.isNotEmpty()) drawPath(currentPoints.toList(), currentColor)
                    }
                    
                    cropRect?.let { rect ->
                        drawRect(Color.White, rect.topLeft, rect.size, style = Stroke(2.dp.toPx()))
                        drawRect(Color.White.copy(0.2f), rect.topLeft, rect.size)
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPicker(selected: Color, onColorSelect: (Color) -> Unit) {
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.White)
    Row(verticalAlignment = Alignment.CenterVertically) {
        colors.forEach { color ->
            Box(Modifier.size(32.dp).padding(4.dp).clip(CircleShape).background(color).border(if (selected == color) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape).pointerInput(Unit) { detectTapGestures { onColorSelect(color) } })
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPath(points: List<Offset>, color: Color) {
    if (points.size < 2) return
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    drawPath(path, color, style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

data class DrawnPath(val points: List<Offset>, val color: Color)

suspend fun renderToBitmap(
    context: android.content.Context, 
    uri: android.net.Uri, 
    rotation: Float, 
    paths: List<DrawnPath>,
    canvasSize: Size,
    cropRect: Rect?
): Bitmap = withContext(Dispatchers.IO) {
    val inputStream = context.contentResolver.openInputStream(uri)
    val source = BitmapFactory.decodeStream(inputStream)
    inputStream?.close() ?: return@withContext Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    
    val matrix = Matrix().apply { postRotate(rotation) }
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    
    val bitmapWidth = rotated.width.toFloat()
    val bitmapHeight = rotated.height.toFloat()
    val scale = Math.min(canvasSize.width / bitmapWidth, canvasSize.height / bitmapHeight)
    val dx = (canvasSize.width - bitmapWidth * scale) / 2f
    val dy = (canvasSize.height - bitmapHeight * scale) / 2f
    
    val output = rotated.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)
    val paint = Paint().apply { strokeWidth = 20f / scale; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true }

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
    
    var finalResult = output
    cropRect?.let { rect ->
        val l = ((rect.left - dx) / scale).coerceIn(0f, bitmapWidth).toInt()
        val t = ((rect.top - dy) / scale).coerceIn(0f, bitmapHeight).toInt()
        val w = (rect.width / scale).coerceIn(1f, bitmapWidth - l).toInt()
        val h = (rect.height / scale).coerceIn(1f, bitmapHeight - t).toInt()
        if (w > 0 && h > 0) {
            finalResult = Bitmap.createBitmap(output, l, t, w, h)
        }
    }
    finalResult
}
