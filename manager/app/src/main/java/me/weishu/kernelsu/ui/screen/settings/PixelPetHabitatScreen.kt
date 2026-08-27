package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.pixel.PixelPetFurnitureEditor
import me.weishu.kernelsu.ui.component.pixel.PixelPetFurnitureInteractionLayer
import me.weishu.kernelsu.ui.component.pixel.PixelPetFurniture
import me.weishu.kernelsu.ui.component.pixel.PixelPetLkmHabitatBackdrop
import me.weishu.kernelsu.ui.component.pixel.PixelPetLkmInteractivePet
import me.weishu.kernelsu.ui.component.pixel.PixelPetPreviewFrame
import me.weishu.kernelsu.ui.component.pixel.PixelPetProgressBar
import me.weishu.kernelsu.ui.component.pixel.PixelPetStageMode
import me.weishu.kernelsu.ui.component.pixel.PixelPetStore
import me.weishu.kernelsu.ui.component.pixel.rememberPixelPetIdlePhase
import me.weishu.kernelsu.ui.component.pixel.rememberPixelPetState
import me.weishu.kernelsu.ui.navigation3.LocalNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelPetHabitatScreen() {
    val navigator = LocalNavigator.current
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val petState = rememberPixelPetState()
    val idlePhase = rememberPixelPetIdlePhase()
    var showPlacementHint by rememberSaveable { mutableStateOf(true) }
    var selectedFurnitureId by rememberSaveable { mutableStateOf<String?>(null) }
    var undoStack by remember { mutableStateOf<List<List<PixelPetFurniture>>>(emptyList()) }
    var immersive by rememberSaveable { mutableStateOf(false) }
    val onBack = dropUnlessResumed { navigator.pop() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pixel_pet_habitat_fullscreen)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(android.R.string.cancel))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            petState.value = PixelPetStore.setLkmPositionLocked(
                                context,
                                !petState.value.lkmPositionLocked,
                            )
                        },
                    ) {
                        Icon(
                            if (petState.value.lkmPositionLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            stringResource(
                                if (petState.value.lkmPositionLocked) {
                                    R.string.pixel_pet_unlock_position
                                } else {
                                    R.string.pixel_pet_lock_position
                                },
                            ),
                        )
                    }
                    IconButton(onClick = { navigator.push(me.weishu.kernelsu.ui.navigation3.Route.PixelPetChat) }) {
                        Icon(Icons.Rounded.ChatBubbleOutline, stringResource(R.string.pixel_pet_open_chat))
                    }
                    IconButton(onClick = { showPlacementHint = !showPlacementHint }) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.pixel_pet_habitat_place))
                    }
                    IconButton(onClick = { immersive = true }) {
                        Icon(Icons.Rounded.Fullscreen, stringResource(R.string.pixel_pet_habitat_fullscreen))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            PixelPetLkmHabitatBackdrop(
                state = petState.value,
                modifier = Modifier.fillMaxSize(),
                idlePhase = idlePhase,
                wallpaperVisible = false,
                showFurniture = false,
            )
            PixelPetFurnitureEditor(
                state = petState.value,
                onMove = { id, x, y ->
                    undoStack = (undoStack + listOf(petState.value.furniture)).takeLast(12)
                    petState.value = PixelPetStore.moveFurniture(context, id, x, y)
                },
                onInteract = { id ->
                    petState.value = PixelPetStore.interactWithFurniture(context, id)
                },
                onSelected = { selectedFurnitureId = it },
                modifier = Modifier.fillMaxSize(),
            )
            PixelPetLkmInteractivePet(
                state = petState.value,
                compact = false,
                onInteract = { petState.value = PixelPetStore.interact(context) },
                onOpenChat = { navigator.push(me.weishu.kernelsu.ui.navigation3.Route.PixelPetChat) },
                onPet = { petState.value = PixelPetStore.pet(context) },
                onFeed = { petState.value = PixelPetStore.feed(context) },
                onExplore = { petState.value = PixelPetStore.exploreHabitat(context) },
                onPositionChanged = { x, y, landscape ->
                    petState.value = PixelPetStore.setLkmPosition(
                        context,
                        x,
                        y,
                        snap = true,
                        landscape = landscape,
                    )
                },
                idlePhase = idlePhase,
                showFurniture = false,
                modifier = Modifier.fillMaxSize(),
            )
            if (showPlacementHint) {
                PixelPetPreviewFrame(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.pixel_pet_habitat_drag_hint),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            PixelPetPreviewFrame(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(petState.value.habitat.labelRes),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(
                                R.string.pixel_pet_furniture_count,
                                petState.value.furniture.size,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = stringResource(R.string.pixel_pet_energy_value, petState.value.energy),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = stringResource(R.string.pixel_pet_cleanliness_value, petState.value.cleanliness),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = stringResource(R.string.pixel_pet_mood_value, petState.value.moodValue),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    if (selectedFurnitureId != null || undoStack.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                enabled = undoStack.isNotEmpty(),
                                onClick = {
                                    val previous = undoStack.lastOrNull() ?: return@IconButton
                                    undoStack = undoStack.dropLast(1)
                                    petState.value = PixelPetStore.replaceFurnitureLayout(context, previous)
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.pixel_pet_undo_layout))
                            }
                            IconButton(
                                enabled = selectedFurnitureId != null,
                                onClick = {
                                    selectedFurnitureId?.let { id ->
                                        undoStack = (undoStack + listOf(petState.value.furniture)).takeLast(12)
                                        petState.value = PixelPetStore.moveFurnitureLayer(context, id, -1)
                                    }
                                },
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.pixel_pet_lower_furniture))
                            }
                            IconButton(
                                enabled = selectedFurnitureId != null,
                                onClick = {
                                    selectedFurnitureId?.let { id ->
                                        undoStack = (undoStack + listOf(petState.value.furniture)).takeLast(12)
                                        petState.value = PixelPetStore.rotateFurniture(context, id)
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.RotateRight,
                                    stringResource(R.string.pixel_pet_rotate_furniture),
                                )
                            }
                            IconButton(
                                enabled = selectedFurnitureId != null,
                                onClick = {
                                    selectedFurnitureId?.let { id ->
                                        undoStack = (undoStack + listOf(petState.value.furniture)).takeLast(12)
                                        petState.value = PixelPetStore.moveFurnitureLayer(context, id, 1)
                                    }
                                },
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.pixel_pet_raise_furniture))
                            }
                            IconButton(
                                enabled = selectedFurnitureId != null,
                                onClick = {
                                    selectedFurnitureId?.let { id ->
                                        undoStack = (undoStack + listOf(petState.value.furniture)).takeLast(12)
                                        petState.value = PixelPetStore.removeFurniture(context, id)
                                        selectedFurnitureId = null
                                    }
                                },
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.pixel_pet_furniture_remove))
                            }
                        }
                    }
                }
            }
        }
    }
    if (immersive) {
        PixelPetImmersiveHabitat(
            state = petState.value,
            idlePhase = idlePhase,
            onDismiss = { immersive = false },
            onStateChanged = { petState.value = it },
        )
    }
}

@Composable
private fun PixelPetImmersiveHabitat(
    state: me.weishu.kernelsu.ui.component.pixel.PixelPetState,
    idlePhase: Float,
    onDismiss: () -> Unit,
    onStateChanged: (me.weishu.kernelsu.ui.component.pixel.PixelPetState) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val navigator = LocalNavigator.current
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            PixelPetLkmHabitatBackdrop(
                state = state,
                modifier = Modifier.fillMaxSize(),
                idlePhase = idlePhase,
                wallpaperVisible = false,
                showFurniture = true,
                stageMode = PixelPetStageMode.Immersive,
            )
            PixelPetFurnitureInteractionLayer(
                state = state,
                onInteract = { id -> onStateChanged(PixelPetStore.interactWithFurniture(context, id)) },
                modifier = Modifier.fillMaxSize(),
            )
            PixelPetLkmInteractivePet(
                state = state,
                compact = false,
                onInteract = { onStateChanged(PixelPetStore.interact(context)) },
                onOpenChat = {
                    onDismiss()
                    navigator.push(me.weishu.kernelsu.ui.navigation3.Route.PixelPetChat)
                },
                onPet = { onStateChanged(PixelPetStore.pet(context)) },
                onFeed = { onStateChanged(PixelPetStore.feed(context)) },
                onExplore = { onStateChanged(PixelPetStore.exploreHabitat(context)) },
                onPositionChanged = { x, y, landscape ->
                    onStateChanged(
                        PixelPetStore.setLkmPosition(
                            context = context,
                            x = x,
                            y = y,
                            snap = true,
                            landscape = landscape,
                        ),
                    )
                },
                idlePhase = idlePhase,
                showFurniture = false,
                stageMode = PixelPetStageMode.Immersive,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelPetPreviewFrame {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                        Text(
                            text = if (state.hatched) state.name else stringResource(R.string.pixel_pet_egg_name),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(state.habitat.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            onDismiss()
                            navigator.push(me.weishu.kernelsu.ui.navigation3.Route.PixelPetChat)
                        },
                    ) {
                        Icon(Icons.Rounded.ChatBubbleOutline, stringResource(R.string.pixel_pet_open_chat))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.FullscreenExit, stringResource(R.string.close))
                    }
                }
            }
            PixelPetPreviewFrame(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = stringResource(state.currentAction.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PixelPetImmersiveStat(
                            value = state.energy,
                            modifier = Modifier.weight(1f),
                        )
                        PixelPetImmersiveStat(
                            value = state.moodValue,
                            modifier = Modifier.weight(1f),
                        )
                        PixelPetImmersiveStat(
                            value = state.cleanliness,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelPetImmersiveStat(value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = value.coerceIn(0, 100).toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PixelPetProgressBar(
            value = value,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
