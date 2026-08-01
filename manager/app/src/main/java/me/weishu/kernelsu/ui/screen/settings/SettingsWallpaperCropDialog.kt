package me.weishu.kernelsu.ui.screen.settings

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.rememberCustomImageBitmap
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.FULL_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.MediaTransform
import me.weishu.kernelsu.ui.util.ResponsiveCropSet
import me.weishu.kernelsu.ui.util.centeredCropForAspect
import me.weishu.kernelsu.ui.util.generateResponsiveCrops
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperCrop
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

private const val WALLPAPER_CROP_EDITOR_PREFERENCES = "wallpaper_crop_editor"
private const val SMART_SUBJECT_ENABLED_KEY = "smart_subject_enabled"

@Composable
fun SettingsWallpaperCropDialog(
    show: Boolean,
    uriString: String?,
    crop: CustomWallpaperCrop,
    onCropChange: (CustomWallpaperCrop) -> Unit,
    onDismissRequest: () -> Unit,
    title: String? = null,
    emptyText: String? = null,
    editorAspectRatio: Float = 9f / 16f,
    cropAspectRatio: Float? = null,
    defaultCrop: CustomWallpaperCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
    previewBitmap: ImageBitmap? = null,
    transform: MediaTransform = MediaTransform(),
    onTransformChange: (MediaTransform) -> Unit = {},
    onGenerateResponsiveCrops: ((ResponsiveCropSet) -> Unit)? = null,
) {
    if (!show) return

    val context = LocalContext.current
    val cropEditorPreferences = remember(context) {
        context.applicationContext.getSharedPreferences(
            WALLPAPER_CROP_EDITOR_PREFERENCES,
            Context.MODE_PRIVATE,
        )
    }
    val imageBitmap = previewBitmap ?: rememberCustomImageBitmap(
        uriString = uriString,
        crop = FULL_CUSTOM_WALLPAPER_CROP,
    )
    var editCrop by remember(uriString) { mutableStateOf(initialEditableCrop(crop, defaultCrop)) }
    var editTransform by remember(uriString) { mutableStateOf(transform.normalized()) }
    var history by remember(uriString) { mutableStateOf(emptyList<CropEditSnapshot>()) }
    var pendingResponsiveCrops by remember(uriString) { mutableStateOf<ResponsiveCropSet?>(null) }
    var smartSubjectEnabled by remember(cropEditorPreferences) {
        mutableStateOf(cropEditorPreferences.getBoolean(SMART_SUBJECT_ENABLED_KEY, true))
    }
    val dialogTitle = title ?: stringResource(R.string.settings_wallpaper_crop)
    val dialogEmptyText = emptyText ?: stringResource(R.string.settings_wallpaper_not_selected)
    val maxDialogContentHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.72f)
        .coerceAtLeast(360.dp)

    OverlayDialog(
        show = true,
        title = dialogTitle,
        onDismissRequest = onDismissRequest,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogContentHeight),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    WallpaperCropEditor(
                        imageBitmap = imageBitmap,
                        uriString = uriString,
                        crop = editCrop,
                        emptyText = dialogEmptyText,
                        editorAspectRatio = editorAspectRatio,
                        cropAspectRatio = cropAspectRatio,
                        transform = editTransform,
                        onCropChange = { editCrop = it },
                        onEditStart = {
                            history = (
                                history + CropEditSnapshot(editCrop, editTransform, pendingResponsiveCrops)
                                ).takeLast(20)
                        },
                    )
                    if (imageBitmap != null) {
                        WallpaperCropTools(
                            imageBitmap = imageBitmap,
                            crop = editCrop,
                            transform = editTransform,
                            cropAspectRatio = cropAspectRatio,
                            smartSubjectEnabled = smartSubjectEnabled,
                            canUndo = history.isNotEmpty(),
                            onSmartSubjectEnabledChange = { enabled ->
                                smartSubjectEnabled = enabled
                                cropEditorPreferences.edit()
                                    .putBoolean(SMART_SUBJECT_ENABLED_KEY, enabled)
                                    .apply()
                                if (!enabled) pendingResponsiveCrops = null
                            },
                            onCommitBeforeChange = {
                                history = (
                                    history + CropEditSnapshot(editCrop, editTransform, pendingResponsiveCrops)
                                    ).takeLast(20)
                            },
                            onCropChange = { editCrop = it },
                            onTransformChange = { editTransform = it.normalized() },
                            onUndo = {
                                val previous = history.lastOrNull() ?: return@WallpaperCropTools
                                history = history.dropLast(1)
                                editCrop = previous.crop
                                editTransform = previous.transform
                                pendingResponsiveCrops = previous.responsiveCrops
                            },
                            onReset = {
                                history = (
                                    history + CropEditSnapshot(editCrop, editTransform, pendingResponsiveCrops)
                                    ).takeLast(20)
                                editCrop = initialEditableCrop(defaultCrop, defaultCrop)
                                editTransform = MediaTransform()
                                pendingResponsiveCrops = null
                            },
                            onGenerateResponsiveCrops = { pendingResponsiveCrops = it },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MiuixTextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(android.R.string.cancel),
                        onClick = onDismissRequest,
                    )
                    MiuixTextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(android.R.string.ok),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            onCropChange(editCrop)
                            pendingResponsiveCrops
                                ?.withCropForAspectRatio(cropAspectRatio ?: editorAspectRatio, editCrop)
                                ?.let { onGenerateResponsiveCrops?.invoke(it) }
                            onTransformChange(editTransform)
                            onDismissRequest()
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun WallpaperCropEditor(
    imageBitmap: ImageBitmap?,
    uriString: String?,
    crop: CustomWallpaperCrop,
    emptyText: String,
    editorAspectRatio: Float,
    cropAspectRatio: Float?,
    transform: MediaTransform,
    onCropChange: (CustomWallpaperCrop) -> Unit,
    onEditStart: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .aspectRatio(editorAspectRatio)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            imageBitmap != null -> CropBox(
                imageBitmap = imageBitmap,
                crop = crop,
                cropAspectRatio = cropAspectRatio,
                transform = transform,
                onCropChange = onCropChange,
                onEditStart = onEditStart,
            )

            uriString.isNullOrBlank() -> Text(
                modifier = Modifier.padding(24.dp),
                text = emptyText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> CircularProgressIndicator()
        }
    }
}

@Composable
private fun CropBox(
    imageBitmap: ImageBitmap,
    crop: CustomWallpaperCrop,
    cropAspectRatio: Float?,
    transform: MediaTransform,
    onCropChange: (CustomWallpaperCrop) -> Unit,
    onEditStart: () -> Unit,
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var dragMode by remember { mutableStateOf(CropDragMode.Move) }
    var dragStartCrop by remember { mutableStateOf(crop) }
    var dragStartPosition by remember { mutableStateOf(Offset.Zero) }
    val currentCrop by rememberUpdatedState(crop)
    val minSize = 0.12f
    val normalizedAspectRatio = remember(imageBitmap.width, imageBitmap.height, cropAspectRatio) {
        normalizedCropAspectRatio(imageBitmap, cropAspectRatio)
    }
    val maxHandleTouchRadius = with(LocalDensity.current) { 36.dp.toPx() }
    LaunchedEffect(imageBitmap.width, imageBitmap.height, normalizedAspectRatio) {
        val fittedCrop = fitCropToAspect(crop, normalizedAspectRatio, minSize)
        if (fittedCrop != crop) {
            onCropChange(fittedCrop)
        }
    }
    val cropGestureModifier = Modifier.pointerInput(
        boxSize,
        imageBitmap.width,
        imageBitmap.height,
        normalizedAspectRatio,
        maxHandleTouchRadius,
    ) {
        detectDragGestures(
            onDragStart = { start ->
                onEditStart()
                val imageRect = fittedImageRect(boxSize, imageBitmap)
                val startCrop = currentCrop
                dragMode = detectCropDragMode(
                    position = start,
                    rect = startCrop.toRect(imageRect),
                    maxHandleTouchRadius = maxHandleTouchRadius,
                )
                dragStartCrop = startCrop
                dragStartPosition = start
            },
            onDrag = { change, _ ->
                change.consume()
                val imageRect = fittedImageRect(boxSize, imageBitmap)
                val start = dragStartPosition.toCropOffset(imageRect)
                val current = change.position.toCropOffset(imageRect)
                val dx = current.x - start.x
                val dy = current.y - start.y
                val baseCrop = dragStartCrop
                onCropChange(
                    when (dragMode) {
                        CropDragMode.Move -> moveCrop(baseCrop, dx, dy)
                        CropDragMode.TopLeft -> resizeCrop(
                            crop = baseCrop,
                            position = current,
                            dragMode = CropDragMode.TopLeft,
                            minSize = minSize,
                            aspectRatio = normalizedAspectRatio,
                        )
                        CropDragMode.TopRight -> resizeCrop(
                            crop = baseCrop,
                            position = current,
                            dragMode = CropDragMode.TopRight,
                            minSize = minSize,
                            aspectRatio = normalizedAspectRatio,
                        )
                        CropDragMode.BottomLeft -> resizeCrop(
                            crop = baseCrop,
                            position = current,
                            dragMode = CropDragMode.BottomLeft,
                            minSize = minSize,
                            aspectRatio = normalizedAspectRatio,
                        )
                        CropDragMode.BottomRight -> resizeCrop(
                            crop = baseCrop,
                            position = current,
                            dragMode = CropDragMode.BottomRight,
                            minSize = minSize,
                            aspectRatio = normalizedAspectRatio,
                        )
                    }
                )
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it },
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = transform.quarterTurns * 90f
                    scaleX = if (transform.flipHorizontal) -1f else 1f
                },
            bitmap = imageBitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val imageRect = fittedImageRect(size, imageBitmap)
            val cropRect = crop.toRect(imageRect)
            val maskColor = Color.Black.copy(alpha = 0.50f)
            drawRect(maskColor, topLeft = Offset.Zero, size = Size(size.width, imageRect.top))
            drawRect(maskColor, topLeft = Offset.Zero, size = Size(imageRect.left, size.height))
            drawRect(maskColor, topLeft = Offset(imageRect.right, 0f), size = Size(size.width - imageRect.right, size.height))
            drawRect(maskColor, topLeft = Offset(0f, imageRect.bottom), size = Size(size.width, size.height - imageRect.bottom))
            drawRect(maskColor, topLeft = imageRect.topLeft, size = Size(imageRect.width, cropRect.top - imageRect.top))
            drawRect(maskColor, topLeft = Offset(imageRect.left, cropRect.bottom), size = Size(imageRect.width, imageRect.bottom - cropRect.bottom))
            drawRect(maskColor, topLeft = Offset(imageRect.left, cropRect.top), size = Size(cropRect.left - imageRect.left, cropRect.height))
            drawRect(maskColor, topLeft = Offset(cropRect.right, cropRect.top), size = Size(imageRect.right - cropRect.right, cropRect.height))
            drawRect(
                color = Color.White,
                topLeft = cropRect.topLeft,
                size = cropRect.size,
                style = Stroke(width = 3.dp.toPx()),
            )
            val guideColor = Color.White.copy(alpha = 0.62f)
            val thirdWidth = cropRect.width / 3f
            val thirdHeight = cropRect.height / 3f
            for (i in 1..2) {
                val x = cropRect.left + thirdWidth * i
                drawLine(guideColor, Offset(x, cropRect.top), Offset(x, cropRect.bottom), strokeWidth = 1.dp.toPx())
                val y = cropRect.top + thirdHeight * i
                drawLine(guideColor, Offset(cropRect.left, y), Offset(cropRect.right, y), strokeWidth = 1.dp.toPx())
            }
            val handleRadius = 6.dp.toPx()
            listOf(
                cropRect.topLeft,
                Offset(cropRect.right, cropRect.top),
                Offset(cropRect.left, cropRect.bottom),
                Offset(cropRect.right, cropRect.bottom),
            ).forEach { point ->
                drawCircle(Color.White, radius = handleRadius, center = point)
                drawCircle(Color.Black.copy(alpha = 0.32f), radius = handleRadius, center = point, style = Stroke(width = 1.dp.toPx()))
            }
            val hintY = cropRect.bottom - 18.dp.toPx()
            drawLine(
                color = Color.White.copy(alpha = 0.70f),
                start = Offset(cropRect.left + 18.dp.toPx(), hintY),
                end = Offset(cropRect.right - 18.dp.toPx(), hintY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(cropGestureModifier)
        )
    }
}

@Composable
private fun WallpaperCropTools(
    imageBitmap: ImageBitmap,
    crop: CustomWallpaperCrop,
    transform: MediaTransform,
    cropAspectRatio: Float?,
    smartSubjectEnabled: Boolean,
    canUndo: Boolean,
    onSmartSubjectEnabledChange: (Boolean) -> Unit,
    onCommitBeforeChange: () -> Unit,
    onCropChange: (CustomWallpaperCrop) -> Unit,
    onTransformChange: (MediaTransform) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onGenerateResponsiveCrops: ((ResponsiveCropSet) -> Unit)?,
) {
    val zoom = (1f / maxOf(crop.width, crop.height).coerceAtLeast(0.05f)).coerceIn(1f, 8f)
    var zoomEditing by remember(imageBitmap, cropAspectRatio) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CenterFocusStrong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.settings_wallpaper_smart_subject_detection),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = smartSubjectEnabled,
                onCheckedChange = onSmartSubjectEnabledChange,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CropToolButton(
                icon = Icons.Rounded.CenterFocusStrong,
                label = stringResource(R.string.settings_wallpaper_smart_subject),
                enabled = smartSubjectEnabled,
                onClick = {
                    onCommitBeforeChange()
                    val focus = salientFocus(imageBitmap)
                    val targetAspect = cropAspectRatio ?: 1f
                    onCropChange(
                        centeredCropForAspect(
                            imageWidth = imageBitmap.width,
                            imageHeight = imageBitmap.height,
                            targetAspectRatio = targetAspect,
                            focusX = focus.x,
                            focusY = focus.y,
                        )
                    )
                    onGenerateResponsiveCrops?.invoke(
                        generateResponsiveCrops(
                            imageWidth = imageBitmap.width,
                            imageHeight = imageBitmap.height,
                            focusX = focus.x,
                            focusY = focus.y,
                        )
                    )
                },
            )
            CropToolButton(
                icon = Icons.AutoMirrored.Rounded.RotateRight,
                label = stringResource(R.string.settings_wallpaper_rotate),
                onClick = {
                    onCommitBeforeChange()
                    onTransformChange(transform.copy(quarterTurns = transform.quarterTurns + 1))
                },
            )
            CropToolButton(
                icon = Icons.Rounded.Flip,
                label = stringResource(R.string.settings_wallpaper_flip),
                onClick = {
                    onCommitBeforeChange()
                    onTransformChange(transform.copy(flipHorizontal = !transform.flipHorizontal))
                },
            )
            CropToolButton(
                icon = Icons.AutoMirrored.Rounded.Undo,
                label = stringResource(R.string.settings_wallpaper_undo),
                enabled = canUndo,
                onClick = onUndo,
            )
            CropToolButton(
                icon = Icons.Rounded.RestartAlt,
                label = stringResource(R.string.settings_wallpaper_reset_edit),
                onClick = onReset,
            )
        }
        Text(
            text = stringResource(R.string.settings_wallpaper_zoom, zoom),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Slider(
            value = zoom,
            onValueChange = { next ->
                if (!zoomEditing) {
                    onCommitBeforeChange()
                    zoomEditing = true
                }
                onCropChange(scaleCropForZoom(crop, next, cropAspectRatio, imageBitmap))
            },
            onValueChangeFinished = { zoomEditing = false },
            valueRange = 1f..8f,
        )
    }
}

@Composable
private fun CropToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(enabled = enabled, onClick = onClick) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
        }
        Text(
            text = label,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

private data class CropEditSnapshot(
    val crop: CustomWallpaperCrop,
    val transform: MediaTransform,
    val responsiveCrops: ResponsiveCropSet?,
)

private fun scaleCropForZoom(
    crop: CustomWallpaperCrop,
    zoom: Float,
    cropAspectRatio: Float?,
    imageBitmap: ImageBitmap,
): CustomWallpaperCrop {
    val normalizedAspect = normalizedCropAspectRatio(imageBitmap, cropAspectRatio)
    val base = fitCropToAspect(DEFAULT_CUSTOM_WALLPAPER_CROP, normalizedAspect, 0.12f)
    val width = (base.width / zoom.coerceIn(1f, 8f)).coerceAtLeast(0.12f)
    val height = if (normalizedAspect != null) width / normalizedAspect else base.height / zoom.coerceIn(1f, 8f)
    val centerX = (crop.left + crop.right) / 2f
    val centerY = (crop.top + crop.bottom) / 2f
    val safeWidth = width.coerceAtMost(1f)
    val safeHeight = height.coerceIn(0.12f, 1f)
    val left = (centerX - safeWidth / 2f).coerceIn(0f, 1f - safeWidth)
    val top = (centerY - safeHeight / 2f).coerceIn(0f, 1f - safeHeight)
    return sanitizeCustomWallpaperCrop(CustomWallpaperCrop(left, top, left + safeWidth, top + safeHeight))
}

private fun salientFocus(imageBitmap: ImageBitmap): Offset {
    val pixels = imageBitmap.toPixelMap()
    val stepX = (imageBitmap.width / 40).coerceAtLeast(1)
    val stepY = (imageBitmap.height / 40).coerceAtLeast(1)
    var total = 0.0
    var weightedX = 0.0
    var weightedY = 0.0
    var y = stepY
    while (y < imageBitmap.height - stepY) {
        var x = stepX
        while (x < imageBitmap.width - stepX) {
            val center = pixels[x, y]
            val right = pixels[x + stepX, y]
            val down = pixels[x, y + stepY]
            val luminance = center.red * 0.2126f + center.green * 0.7152f + center.blue * 0.0722f
            val rightLuminance = right.red * 0.2126f + right.green * 0.7152f + right.blue * 0.0722f
            val downLuminance = down.red * 0.2126f + down.green * 0.7152f + down.blue * 0.0722f
            val chroma = maxOf(center.red, center.green, center.blue) - minOf(center.red, center.green, center.blue)
            val edge = abs(luminance - rightLuminance) + abs(luminance - downLuminance)
            val weight = (0.03f + edge * 2.2f + chroma * 0.55f).toDouble()
            total += weight
            weightedX += weight * x
            weightedY += weight * y
            x += stepX
        }
        y += stepY
    }
    if (total <= 0.0) return Offset(0.5f, 0.5f)
    return Offset(
        (weightedX / total / imageBitmap.width).toFloat().coerceIn(0.12f, 0.88f),
        (weightedY / total / imageBitmap.height).toFloat().coerceIn(0.12f, 0.88f),
    )
}

private enum class CropDragMode {
    Move,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
}

private fun initialEditableCrop(crop: CustomWallpaperCrop, defaultCrop: CustomWallpaperCrop): CustomWallpaperCrop {
    val safeCrop = sanitizeCustomWallpaperCrop(crop)
    return if (
        safeCrop.left <= 0.001f &&
        safeCrop.top <= 0.001f &&
        safeCrop.right >= 0.999f &&
        safeCrop.bottom >= 0.999f
    ) {
        sanitizeCustomWallpaperCrop(defaultCrop)
    } else {
        safeCrop
    }
}

private fun detectCropDragMode(
    position: Offset,
    rect: Rect,
    maxHandleTouchRadius: Float,
): CropDragMode {
    val handleTouchRadius = minOf(maxHandleTouchRadius, rect.width / 3f, rect.height / 3f)
    val points = listOf(
        CropDragMode.TopLeft to rect.topLeft,
        CropDragMode.TopRight to Offset(rect.right, rect.top),
        CropDragMode.BottomLeft to Offset(rect.left, rect.bottom),
        CropDragMode.BottomRight to Offset(rect.right, rect.bottom),
    )
    return points.minByOrNull { (_, point) -> (point - position).getDistance() }
        ?.takeIf { (_, point) -> (point - position).getDistance() <= handleTouchRadius }
        ?.first
        ?: CropDragMode.Move
}

private fun Offset.toCropOffset(rect: Rect): Offset {
    return Offset(
        x = ((x - rect.left) / rect.width.coerceAtLeast(1f)).coerceIn(0f, 1f),
        y = ((y - rect.top) / rect.height.coerceAtLeast(1f)).coerceIn(0f, 1f),
    )
}

private fun CustomWallpaperCrop.toRect(rect: Rect): Rect {
    return Rect(
        left = rect.left + left * rect.width,
        top = rect.top + top * rect.height,
        right = rect.left + right * rect.width,
        bottom = rect.top + bottom * rect.height,
    )
}

private fun fittedImageRect(size: IntSize, imageBitmap: ImageBitmap): Rect {
    return fittedImageRect(Size(size.width.toFloat(), size.height.toFloat()), imageBitmap)
}

private fun fittedImageRect(size: Size, imageBitmap: ImageBitmap): Rect {
    val boxWidth = size.width.coerceAtLeast(1f)
    val boxHeight = size.height.coerceAtLeast(1f)
    val imageRatio = imageBitmap.width.toFloat() / imageBitmap.height.toFloat().coerceAtLeast(1f)
    val boxRatio = boxWidth / boxHeight
    val drawWidth: Float
    val drawHeight: Float
    if (imageRatio > boxRatio) {
        drawWidth = boxWidth
        drawHeight = boxWidth / imageRatio
    } else {
        drawHeight = boxHeight
        drawWidth = boxHeight * imageRatio
    }
    val left = (boxWidth - drawWidth) / 2f
    val top = (boxHeight - drawHeight) / 2f
    return Rect(left = left, top = top, right = left + drawWidth, bottom = top + drawHeight)
}

private fun moveCrop(crop: CustomWallpaperCrop, dx: Float, dy: Float): CustomWallpaperCrop {
    val width = crop.width
    val height = crop.height
    val left = (crop.left + dx).coerceIn(0f, 1f - width)
    val top = (crop.top + dy).coerceIn(0f, 1f - height)
    return CustomWallpaperCrop(
        left = left,
        top = top,
        right = left + width,
        bottom = top + height,
    )
}

private fun normalizedCropAspectRatio(
    imageBitmap: ImageBitmap,
    cropAspectRatio: Float?,
): Float? {
    val aspectRatio = cropAspectRatio?.takeIf { it > 0f } ?: return null
    val imageAspectRatio = imageBitmap.width.toFloat() / imageBitmap.height.toFloat().coerceAtLeast(1f)
    return (aspectRatio / imageAspectRatio).coerceIn(0.05f, 20f)
}

private fun fitCropToAspect(
    crop: CustomWallpaperCrop,
    aspectRatio: Float?,
    minSize: Float,
): CustomWallpaperCrop {
    val safeCrop = sanitizeCustomWallpaperCrop(crop)
    if (aspectRatio == null || aspectRatio <= 0f) return safeCrop

    val centerX = ((safeCrop.left + safeCrop.right) / 2f).coerceIn(0f, 1f)
    val centerY = ((safeCrop.top + safeCrop.bottom) / 2f).coerceIn(0f, 1f)
    val maxHeight = minOf(1f, 1f / aspectRatio)
    val minHeight = minSize.coerceAtMost(maxHeight)
    val currentRatio = safeCrop.width / safeCrop.height.coerceAtLeast(0.001f)
    var height = if (currentRatio > aspectRatio) {
        safeCrop.height
    } else {
        safeCrop.width / aspectRatio
    }.coerceIn(minHeight, maxHeight)
    var width = (height * aspectRatio).coerceAtMost(1f)
    height = (width / aspectRatio).coerceAtMost(1f)

    val left = (centerX - width / 2f).coerceIn(0f, 1f - width)
    val top = (centerY - height / 2f).coerceIn(0f, 1f - height)
    return sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height,
        )
    )
}

private fun resizeCrop(
    crop: CustomWallpaperCrop,
    position: Offset,
    dragMode: CropDragMode,
    minSize: Float,
    aspectRatio: Float?,
): CustomWallpaperCrop {
    if (aspectRatio == null || aspectRatio <= 0f) {
        return when (dragMode) {
            CropDragMode.TopLeft -> resizeCrop(crop, left = position.x, top = position.y, minSize = minSize)
            CropDragMode.TopRight -> resizeCrop(crop, right = position.x, top = position.y, minSize = minSize)
            CropDragMode.BottomLeft -> resizeCrop(crop, left = position.x, bottom = position.y, minSize = minSize)
            CropDragMode.BottomRight -> resizeCrop(crop, right = position.x, bottom = position.y, minSize = minSize)
            CropDragMode.Move -> crop
        }
    }

    val safeCrop = sanitizeCustomWallpaperCrop(crop)
    val movesLeft = dragMode == CropDragMode.TopLeft || dragMode == CropDragMode.BottomLeft
    val movesTop = dragMode == CropDragMode.TopLeft || dragMode == CropDragMode.TopRight
    val fixedX = if (movesLeft) safeCrop.right else safeCrop.left
    val fixedY = if (movesTop) safeCrop.bottom else safeCrop.top
    val maxWidth = if (movesLeft) fixedX else 1f - fixedX
    val maxHeight = if (movesTop) fixedY else 1f - fixedY
    val rawWidth = abs(position.x.coerceIn(0f, 1f) - fixedX).coerceAtLeast(minSize)
    val rawHeight = abs(position.y.coerceIn(0f, 1f) - fixedY).coerceAtLeast(minSize)
    var width: Float
    var height: Float

    if (rawWidth / rawHeight.coerceAtLeast(0.001f) > aspectRatio) {
        height = rawHeight
        width = height * aspectRatio
    } else {
        width = rawWidth
        height = width / aspectRatio
    }
    if (width > maxWidth) {
        width = maxWidth
        height = width / aspectRatio
    }
    if (height > maxHeight) {
        height = maxHeight
        width = height * aspectRatio
    }
    width = width.coerceIn(minSize.coerceAtMost(maxWidth), maxWidth)
    height = (width / aspectRatio).coerceAtMost(maxHeight)
    if (height < minSize && minSize <= maxHeight) {
        height = minSize
        width = (height * aspectRatio).coerceAtMost(maxWidth)
    }

    return sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(
            left = if (movesLeft) fixedX - width else fixedX,
            top = if (movesTop) fixedY - height else fixedY,
            right = if (movesLeft) fixedX else fixedX + width,
            bottom = if (movesTop) fixedY else fixedY + height,
        )
    )
}

private fun resizeCrop(
    crop: CustomWallpaperCrop,
    left: Float = crop.left,
    top: Float = crop.top,
    right: Float = crop.right,
    bottom: Float = crop.bottom,
    minSize: Float,
): CustomWallpaperCrop {
    return sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(
            left = left.coerceIn(0f, crop.right - minSize),
            top = top.coerceIn(0f, crop.bottom - minSize),
            right = right.coerceIn(crop.left + minSize, 1f),
            bottom = bottom.coerceIn(crop.top + minSize, 1f),
        )
    )
}
