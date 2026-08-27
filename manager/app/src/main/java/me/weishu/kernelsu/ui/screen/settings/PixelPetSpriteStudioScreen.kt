package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.pixel.PixelPetAction
import me.weishu.kernelsu.ui.component.pixel.PixelPetFacing
import me.weishu.kernelsu.ui.component.pixel.PixelPetGrowthStage
import me.weishu.kernelsu.ui.component.pixel.PixelPetModelColors
import me.weishu.kernelsu.ui.component.pixel.PixelPetSpriteAtlas
import me.weishu.kernelsu.ui.component.pixel.PixelPetSpriteDraftStore
import me.weishu.kernelsu.ui.component.pixel.PixelPetSpriteEditorKey
import me.weishu.kernelsu.ui.component.pixel.PixelPetSpriteFrame
import me.weishu.kernelsu.ui.component.pixel.PixelPetSpriteInk
import me.weishu.kernelsu.ui.component.pixel.PixelPetSpecies
import me.weishu.kernelsu.ui.component.pixel.PixelPetPanel
import me.weishu.kernelsu.ui.component.pixel.PixelPetPreviewFrame
import me.weishu.kernelsu.ui.component.pixel.PixelPetScreenBackdrop
import me.weishu.kernelsu.ui.component.pixel.colorFor
import me.weishu.kernelsu.ui.component.pixel.pixelPetModelColors
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor
import kotlin.math.floor
import kotlin.math.min

@Composable
fun PixelPetSpriteStudioScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    var species by remember { mutableStateOf(PixelPetSpecies.Cat) }
    var stage by remember { mutableStateOf(PixelPetGrowthStage.Adult) }
    var action by remember { mutableStateOf(PixelPetAction.Idle) }
    var facing by remember { mutableStateOf(PixelPetFacing.Front) }
    var frameIndex by remember { mutableIntStateOf(0) }
    var selectedInk by remember { mutableStateOf(PixelPetSpriteInk.Highlight) }
    var onionSkin by remember { mutableStateOf(true) }
    var comparePrevious by remember { mutableStateOf(false) }
    var editVersion by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        PixelPetSpriteAtlas.registerMemoryCallbacks(context)
        onDispose { }
    }

    val frameCount = PixelPetSpriteAtlas.frameCount(action)
    if (frameIndex >= frameCount) frameIndex = frameCount - 1
    val key = PixelPetSpriteEditorKey(species, stage, action, frameIndex, facing)
    val colors = pixelPetModelColors(species)
    var packState by remember(species) { mutableStateOf(PixelPetSpriteAtlas.packState(species)) }
    val currentFrame = remember(species, stage, action, frameIndex, facing, editVersion, packState) {
        PixelPetSpriteAtlas.loadedFrame(species, stage, action, frameIndex, facing)
    }
    val previousFrame = remember(species, stage, action, frameIndex, facing, editVersion, packState) {
        PixelPetSpriteAtlas.loadedFrame(species, stage, action, frameIndex - 1, facing)
    }
    LaunchedEffect(context, species, currentFrame == null || previousFrame == null) {
        if (currentFrame == null || previousFrame == null) {
            packState = withContext(Dispatchers.Default) {
                PixelPetSpriteAtlas.prepare(context, species)
            }
        }
    }
    if (currentFrame == null || previousFrame == null) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.pixel_pet_sprite_studio_title)) },
                    navigationIcon = {
                        IconButton(onClick = dropUnlessResumed { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.close))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = immersiveScrolledTopBarColor(MaterialTheme.colorScheme.surface),
                    ),
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                PixelPetScreenBackdrop()
                CircularProgressIndicator()
            }
        }
        return
    }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { output ->
            PixelPetSpriteDraftStore.exportPng(currentFrame, colors, output)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pixel_pet_sprite_studio_title)) },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.close))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            exportLauncher.launch(
                                "${species.name.lowercase()}_${stage.name.lowercase()}_${action.name.lowercase()}_${frameIndex + 1}.png",
                            )
                        },
                    ) {
                        Icon(Icons.Rounded.FileDownload, stringResource(R.string.pixel_pet_sprite_export))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = immersiveScrolledTopBarColor(MaterialTheme.colorScheme.surface),
                ),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            PixelPetScreenBackdrop()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 14.dp, top = innerPadding.calculateTopPadding() + 8.dp, end = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.pixel_pet_sprite_studio_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PixelPetPanel(title = stringResource(R.string.pixel_pet_sprite_selection)) {
                    SpriteStudioSelector(
                        label = stringResource(R.string.pixel_pet_sprite_species),
                        value = species.name,
                        options = PixelPetSpecies.entries,
                        labelFor = { it.name },
                    ) { species = it }
                    SpriteStudioSelector(
                        label = stringResource(R.string.pixel_pet_sprite_stage),
                        value = stage.name,
                        options = PixelPetGrowthStage.entries,
                        labelFor = { it.name },
                    ) { stage = it }
                    SpriteStudioSelector(
                        label = stringResource(R.string.pixel_pet_sprite_action),
                        value = action.name,
                        options = PixelPetAction.entries,
                        labelFor = { it.name },
                    ) {
                        action = it
                        frameIndex = 0
                    }
                    SpriteStudioSelector(
                        label = stringResource(R.string.pixel_pet_sprite_direction),
                        value = facing.name,
                        options = PixelPetFacing.entries,
                        labelFor = { it.name },
                    ) { facing = it }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.pixel_pet_sprite_frame, frameIndex + 1, frameCount), modifier = Modifier.weight(1f))
                        IconButton(onClick = { frameIndex = (frameIndex - 1).mod(frameCount) }) {
                            Icon(Icons.AutoMirrored.Rounded.NavigateBefore, stringResource(R.string.pixel_pet_sprite_previous))
                        }
                        IconButton(onClick = { frameIndex = (frameIndex + 1).mod(frameCount) }) {
                            Icon(Icons.AutoMirrored.Rounded.NavigateNext, stringResource(R.string.pixel_pet_sprite_next))
                        }
                    }
                }
                PixelPetPanel(title = stringResource(R.string.pixel_pet_sprite_preview)) {
                    if (comparePrevious) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            SpriteFramePreview(previousFrame, colors, Modifier.weight(1f), alpha = 0.72f)
                            SpriteFramePreview(currentFrame, colors, Modifier.weight(1f))
                        }
                    } else {
                        SpriteFramePreview(currentFrame, colors, Modifier.fillMaxWidth())
                    }
                    SpriteStudioToggle(
                        label = stringResource(R.string.pixel_pet_sprite_onion_skin),
                        checked = onionSkin,
                        onCheckedChange = { onionSkin = it },
                    )
                    SpriteStudioToggle(
                        label = stringResource(R.string.pixel_pet_sprite_compare_previous),
                        checked = comparePrevious,
                        onCheckedChange = { comparePrevious = it },
                    )
                }
                PixelPetPanel(title = stringResource(R.string.pixel_pet_sprite_palette)) {
                    PixelPetSpriteInk.entries.chunked(5).forEach { inks ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            inks.forEach { ink ->
                                FilterChip(
                                    selected = selectedInk == ink,
                                    onClick = { selectedInk = ink },
                                    modifier = Modifier.weight(1f),
                                    label = { Text(ink.name.take(4)) },
                                )
                            }
                            repeat(5 - inks.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                PixelPetPanel(
                    title = stringResource(R.string.pixel_pet_sprite_canvas),
                    trailing = {
                        IconButton(
                            onClick = {
                                PixelPetSpriteDraftStore.reset(context, key)
                                editVersion++
                            },
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.pixel_pet_sprite_reset))
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            R.string.pixel_pet_sprite_canvas_summary,
                            PixelPetSpriteDraftStore.editedCellCount(context, key),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PixelSpriteCanvas(
                        current = currentFrame,
                        previous = previousFrame,
                        colors = colors,
                        onionSkin = onionSkin,
                        modifier = Modifier.fillMaxWidth(),
                    ) { x, y ->
                        PixelPetSpriteDraftStore.setCell(
                            context = context,
                            key = key,
                            x = x,
                            y = y,
                            width = currentFrame.width,
                            height = currentFrame.height,
                            ink = selectedInk,
                        )
                        editVersion++
                    }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        PixelPetSpriteDraftStore.reset(context, key)
                        editVersion++
                    },
                ) {
                    Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.pixel_pet_sprite_reset))
                }
            }
        }
    }
}

@Composable
private fun <T> SpriteStudioSelector(
    label: String,
    value: String,
    options: List<T>,
    labelFor: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text(value) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(labelFor(option)) },
                        onClick = {
                            expanded = false
                            onSelected(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SpriteStudioToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SpriteFramePreview(
    frame: PixelPetSpriteFrame,
    colors: PixelPetModelColors,
    modifier: Modifier,
    alpha: Float = 1f,
) {
    PixelPetPreviewFrame(modifier = modifier.aspectRatio(1f)) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            drawSpriteFrame(frame, colors, alpha)
        }
    }
}

@Composable
private fun PixelSpriteCanvas(
    current: PixelPetSpriteFrame,
    previous: PixelPetSpriteFrame,
    colors: PixelPetModelColors,
    onionSkin: Boolean,
    modifier: Modifier,
    onCellTap: (Int, Int) -> Unit,
) {
    val gridBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
    val gridModifier = modifier
        .aspectRatio(1f)
        .clip(RoundedCornerShape(7.dp))
        .pointerInput(current, onionSkin) {
            detectTapGestures { offset ->
                val cell = min(size.width / current.width, size.height / current.height)
                if (cell <= 0f) return@detectTapGestures
                val originX = (size.width - current.width * cell) / 2f
                val originY = (size.height - current.height * cell) / 2f
                val x = floor((offset.x - originX) / cell).toInt()
                val y = floor((offset.y - originY) / cell).toInt()
                if (x in 0 until current.width && y in 0 until current.height) onCellTap(x, y)
            }
    }
    Canvas(modifier = gridModifier) {
        drawRect(gridBackground)
        if (onionSkin) drawSpriteFrame(previous, colors, alpha = 0.19f)
        drawSpriteFrame(current, colors)
        val cell = min(size.width / current.width, size.height / current.height)
        val originX = (size.width - current.width * cell) / 2f
        val originY = (size.height - current.height * cell) / 2f
        repeat(current.width + 1) { index ->
            val position = originX + index * cell
            drawLine(
                Color.Black.copy(alpha = 0.16f),
                Offset(position, originY),
                Offset(position, originY + current.height * cell),
                1f,
            )
        }
        repeat(current.height + 1) { index ->
            val position = originY + index * cell
            drawLine(
                Color.Black.copy(alpha = 0.16f),
                Offset(originX, position),
                Offset(originX + current.width * cell, position),
                1f,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpriteFrame(
    frame: PixelPetSpriteFrame,
    colors: PixelPetModelColors,
    alpha: Float = 1f,
) {
    val cell = min(size.width / frame.width, size.height / frame.height)
    val originX = (size.width - frame.width * cell) / 2f
    val originY = (size.height - frame.height * cell) / 2f
    frame.cells.forEach { item ->
        drawRect(
            color = colors.colorFor(item.value).copy(alpha = alpha),
            topLeft = Offset(originX + item.x * cell, originY + item.y * cell),
            size = androidx.compose.ui.geometry.Size(cell, cell),
        )
    }
}
