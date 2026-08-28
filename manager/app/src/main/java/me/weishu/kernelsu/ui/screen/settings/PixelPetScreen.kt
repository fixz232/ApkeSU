package me.weishu.kernelsu.ui.screen.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.ApkeMetricGrid
import me.weishu.kernelsu.ui.component.ApkeMetricItem
import me.weishu.kernelsu.ui.component.ApkeSecondaryScaffold
import me.weishu.kernelsu.ui.component.ApkeUiTokens
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.pixel.PixelPetAccessory
import me.weishu.kernelsu.ui.component.pixel.PixelPetAccessorySlot
import me.weishu.kernelsu.ui.component.pixel.PixelPetAppearance
import me.weishu.kernelsu.ui.component.pixel.PixelPetAchievement
import me.weishu.kernelsu.ui.component.pixel.PixelPetBackupPreview
import me.weishu.kernelsu.ui.component.pixel.PixelPetBackupIncompatibleException
import me.weishu.kernelsu.ui.component.pixel.PixelPetAvatar
import me.weishu.kernelsu.ui.component.pixel.PixelPetHabitat
import me.weishu.kernelsu.ui.component.pixel.PixelPetItemSurface
import me.weishu.kernelsu.ui.component.pixel.PixelPetDailyTask
import me.weishu.kernelsu.ui.component.pixel.PixelPetFoodKind
import me.weishu.kernelsu.ui.component.pixel.PixelPetFurnitureKind
import me.weishu.kernelsu.ui.component.pixel.MAX_PIXEL_PET_FURNITURE
import me.weishu.kernelsu.ui.component.pixel.PIXEL_PET_FURNITURE_REPAIR_COST
import me.weishu.kernelsu.ui.component.pixel.PixelPetPersonality
import me.weishu.kernelsu.ui.component.pixel.PixelPetPage
import me.weishu.kernelsu.ui.component.pixel.PixelPetPageBar
import me.weishu.kernelsu.ui.component.pixel.PixelPetPanel
import me.weishu.kernelsu.ui.component.pixel.PixelPetProgressBar
import me.weishu.kernelsu.ui.component.pixel.PixelPetPreviewFrame
import me.weishu.kernelsu.ui.component.pixel.PixelPetRestoreMode
import me.weishu.kernelsu.ui.component.pixel.PixelPetScreenBackdrop
import me.weishu.kernelsu.ui.component.pixel.PixelPetSpecies
import me.weishu.kernelsu.ui.component.pixel.PixelPetState
import me.weishu.kernelsu.ui.component.pixel.PixelPetStore
import me.weishu.kernelsu.ui.component.pixel.PixelPetWeather
import me.weishu.kernelsu.ui.component.pixel.rememberPixelPetState
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor

@Composable
fun PixelPetScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current.applicationContext
    val petState = rememberPixelPetState()
    var showNameDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var nameDraft by rememberSaveable { mutableStateOf("") }
    var pendingBackup by remember { mutableStateOf<PixelPetBackupPreview?>(null) }
    var pendingBackupRaw by remember { mutableStateOf<String?>(null) }
    var shopMessage by rememberSaveable { mutableStateOf("") }
    var selectedPage by rememberSaveable { mutableStateOf(PixelPetPage.Companion) }
    val companionScrollState = rememberScrollState()
    val habitatScrollState = rememberScrollState()
    val storeScrollState = rememberScrollState()
    val growthScrollState = rememberScrollState()
    val activeScrollState = when (selectedPage) {
        PixelPetPage.Companion -> companionScrollState
        PixelPetPage.Habitat -> habitatScrollState
        PixelPetPage.Store -> storeScrollState
        PixelPetPage.Growth -> growthScrollState
    }
    val insufficientCoinsMessage = stringResource(R.string.pixel_pet_insufficient_coins)
    val interactionCooldownMessage = stringResource(R.string.pixel_pet_interaction_cooldown)
    val rewardCappedMessage = stringResource(R.string.pixel_pet_reward_capped)
    val notificationDeniedMessage = stringResource(R.string.pixel_pet_reminder_denied)
    val backupSuccessMessage = stringResource(R.string.pixel_pet_backup_success)
    val backupExportedMessage = stringResource(R.string.pixel_pet_backup_exported)
    val backupFailedMessage = stringResource(R.string.pixel_pet_backup_failed)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            petState.value = PixelPetStore.setReminder(context, true)
        } else {
            shopMessage = notificationDeniedMessage
        }
    }
    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(PixelPetStore.exportBackup(context))
            } ?: error("Unable to open pet backup")
        }.onSuccess {
            shopMessage = backupExportedMessage
        }.onFailure { error ->
            shopMessage = if (error is PixelPetBackupIncompatibleException) {
                context.getString(R.string.pixel_pet_backup_incompatible, error.version)
            } else {
                backupFailedMessage
            }
        }
    }
    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Unable to read pet backup")
            PixelPetStore.previewBackup(raw) to raw
        }.onSuccess { (preview, raw) ->
            pendingBackup = preview.copy(state = preview.state.copy(chatMessages = preview.state.chatMessages))
            pendingBackupRaw = raw
        }.onFailure {
            shopMessage = backupFailedMessage
        }
    }
    val onBack = dropUnlessResumed { navigator.pop() }

    ApkeSecondaryScaffold(
        title = stringResource(R.string.pixel_pet_title),
        onBack = onBack,
        containerColor = Color.Transparent,
    ) { innerPadding, _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            PixelPetScreenBackdrop()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(activeScrollState)
                    .navigationBarsPadding()
                    .padding(
                        start = ApkeUiTokens.PageHorizontalPadding,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        end = ApkeUiTokens.PageHorizontalPadding,
                        bottom = innerPadding.calculateBottomPadding() + 18.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            PetHeroCard(
                state = petState.value,
                onEnabledChange = { petState.value = PixelPetStore.setEnabled(context, it) },
                onRename = {
                    nameDraft = petState.value.name
                    showNameDialog = true
                },
            )
            PixelPetPageBar(
                selected = selectedPage,
                onSelected = { selectedPage = it },
            )
            if (selectedPage == PixelPetPage.Growth) {
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ApkeUiTokens.MinTouchTarget),
                    onClick = { navigator.push(Route.PixelPetSpriteStudio) },
                ) {
                    Text(stringResource(R.string.pixel_pet_sprite_studio_entry))
                }
            }
            if (shopMessage.isNotBlank()) {
                Text(
                    text = shopMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (selectedPage == PixelPetPage.Companion) {
                PetSection(title = stringResource(R.string.pixel_pet_progress_title)) {
                    ApkeMetricGrid(
                        items = listOf(
                            ApkeMetricItem(
                                label = stringResource(R.string.pixel_pet_hunger),
                                value = "${petState.value.hunger}%",
                            ),
                            ApkeMetricItem(
                                label = stringResource(R.string.pixel_pet_energy),
                                value = "${petState.value.energy}%",
                            ),
                            ApkeMetricItem(
                                label = stringResource(R.string.pixel_pet_mood),
                                value = "${petState.value.moodValue}%",
                            ),
                        ),
                    )
                }
                PetPrimaryActions(
                    state = petState.value,
                    onInteract = {
                        val now = System.currentTimeMillis()
                        if (now - petState.value.lastInteractionAt < 750L) {
                            shopMessage = interactionCooldownMessage
                        } else {
                            val previousInteractions = petState.value.dailyInteractions
                            petState.value = PixelPetStore.interact(context, now)
                            shopMessage = if (previousInteractions >= 30) rewardCappedMessage else ""
                        }
                    },
                    onFeed = { petState.value = PixelPetStore.feed(context) },
                    onCheckIn = { petState.value = PixelPetStore.checkIn(context) },
                )
            }
            if (selectedPage == PixelPetPage.Growth) {
                PetSection(title = stringResource(R.string.pixel_pet_species_title)) {
                when {
                    petState.value.hatched -> {
                        Text(
                            text = stringResource(
                                R.string.pixel_pet_species_selected,
                                stringResource(petState.value.species?.labelRes ?: PixelPetSpecies.Cat.labelRes),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    petState.value.isIncubating -> {
                        Text(
                            text = stringResource(
                                R.string.pixel_pet_incubating,
                                formatPixelPetIncubation(petState.value.incubationRemainingMillis()),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    else -> {
                        Text(
                            text = stringResource(R.string.pixel_pet_species_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                PixelPetSpecies.entries.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.forEach { species ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = petState.value.species == species,
                                onClick = {
                                    petState.value = PixelPetStore.chooseSpecies(context, species)
                                },
                                label = { Text(stringResource(species.labelRes)) },
                                enabled = petState.value.enabled && !petState.value.hatched && !petState.value.isIncubating,
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
                PetSection(title = stringResource(R.string.pixel_pet_interaction_title)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        enabled = petState.value.enabled && petState.value.hatched,
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - petState.value.lastInteractionAt < 750L) {
                                shopMessage = interactionCooldownMessage
                            } else {
                                val previousInteractions = petState.value.dailyInteractions
                                petState.value = PixelPetStore.interact(context, now)
                                shopMessage = if (previousInteractions >= 30) {
                                    rewardCappedMessage
                                } else {
                                    ""
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Rounded.Favorite, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.pixel_pet_interact))
                    }
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        enabled = petState.value.enabled && petState.value.hatched && petState.value.coins >= 5,
                        onClick = { petState.value = PixelPetStore.feed(context) },
                    ) {
                        Icon(Icons.Rounded.Restaurant, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.pixel_pet_feed))
                    }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = petState.value.enabled && petState.value.hatched && petState.value.canCheckIn,
                    onClick = { petState.value = PixelPetStore.checkIn(context) },
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.pixel_pet_check_in))
                }
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = petState.value.enabled && petState.value.hatched,
                    onClick = {
                        val previous = petState.value.totalHabitatInteractions
                        val updated = PixelPetStore.exploreHabitat(context)
                        petState.value = updated
                        shopMessage = if (updated.totalHabitatInteractions == previous) {
                            interactionCooldownMessage
                        } else {
                            ""
                        }
                    },
                ) {
                    Icon(Icons.Rounded.Stars, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.pixel_pet_explore_habitat))
                }
                if (petState.value.enabled && petState.value.hatched) {
                    Text(
                        text = if (petState.value.canCheckIn) {
                            stringResource(R.string.pixel_pet_check_in_ready)
                        } else {
                            stringResource(R.string.pixel_pet_check_in_next, petState.value.checkInStreak)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
                if (petState.value.enabled && petState.value.hatched) {
                    Text(
                        text = stringResource(
                            R.string.pixel_pet_action_status,
                            stringResource(petState.value.currentAction.labelRes),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    PetSection(title = stringResource(R.string.pixel_pet_chat_title)) {
                    Text(
                        text = stringResource(R.string.pixel_pet_chat_summary, petState.value.teachingEnergy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { navigator.push(Route.PixelPetChat) },
                    ) {
                        Text(stringResource(R.string.pixel_pet_open_chat))
                    }
                }
                    PetSection(title = stringResource(R.string.pixel_pet_personality_title)) {
                    Text(
                        text = stringResource(R.string.pixel_pet_personality_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PixelPetPersonality.entries.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            row.forEach { personality ->
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = petState.value.personality == personality,
                                    onClick = {
                                        petState.value = PixelPetStore.setPersonality(context, personality)
                                    },
                                    label = { Text(stringResource(personality.labelRes)) },
                                )
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    }
                    PetSection(title = stringResource(R.string.pixel_pet_appearance_title)) {
                        Text(
                            text = stringResource(R.string.pixel_pet_appearance_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PixelPetAppearance.entries.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                row.forEach { appearance ->
                                    FilterChip(
                                        modifier = Modifier.weight(1f),
                                        selected = petState.value.appearance == appearance,
                                        onClick = {
                                            petState.value = PixelPetStore.setAppearance(context, appearance)
                                        },
                                        label = { Text(stringResource(appearance.labelRes)) },
                                    )
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            if (selectedPage == PixelPetPage.Habitat) {
                PetSection(title = stringResource(R.string.pixel_pet_habitat_title)) {
                PixelPetHabitat.entries.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.forEach { habitat ->
                            val unlocked = habitat in petState.value.unlockedHabitats
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = petState.value.habitat == habitat,
                                onClick = { petState.value = PixelPetStore.setHabitat(context, habitat) },
                                label = {
                                    Text(
                                        if (unlocked) {
                                            stringResource(habitat.labelRes)
                                        } else {
                                            stringResource(R.string.pixel_pet_locked_level, habitat.unlockLevel)
                                        },
                                    )
                                },
                                enabled = petState.value.enabled && unlocked,
                            )
                        }
                    }
                }
                if (petState.value.enabled && petState.value.hatched) {
                    Text(
                        text = stringResource(
                            R.string.pixel_pet_weather_summary,
                            stringResource(petState.value.currentWeather().labelRes),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = petState.value.weatherOverride == null,
                            onClick = { petState.value = PixelPetStore.setWeatherOverride(context, null) },
                            label = { Text(stringResource(R.string.pixel_pet_weather_auto)) },
                        )
                        PixelPetWeather.entries.take(2).forEach { weather ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = petState.value.weatherOverride == weather,
                                onClick = { petState.value = PixelPetStore.setWeatherOverride(context, weather) },
                                label = { Text(stringResource(weather.labelRes)) },
                            )
                        }
                    }
                    PixelPetWeather.entries.drop(2).chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            row.forEach { weather ->
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = petState.value.weatherOverride == weather,
                                    onClick = { petState.value = PixelPetStore.setWeatherOverride(context, weather) },
                                    label = { Text(stringResource(weather.labelRes)) },
                                )
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = petState.value.enabled && petState.value.hatched,
                    onClick = { navigator.push(Route.PixelPetHabitat) },
                ) {
                    Text(stringResource(R.string.pixel_pet_habitat_fullscreen))
                }
            }
            if (selectedPage == PixelPetPage.Store) {
                PetSection(title = stringResource(R.string.pixel_pet_shop_title)) {
                PixelPetAccessorySlot.entries.forEach { slot ->
                    Text(
                        text = stringResource(slot.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    PixelPetAccessory.entries.filter { it.slot == slot }.forEach { accessory ->
                        PetShopRow(
                            accessory = accessory,
                            state = petState.value,
                            enabled = petState.value.enabled && petState.value.hatched,
                            onClick = {
                                if (
                                    accessory !in petState.value.ownedAccessories &&
                                    petState.value.coins < accessory.cost
                                ) {
                                    shopMessage = insufficientCoinsMessage
                                } else {
                                    shopMessage = ""
                                    petState.value = PixelPetStore.buyOrEquip(context, accessory)
                                }
                            },
                        )
                    }
                }
                if (petState.value.equippedAccessoriesOrLegacy.isNotEmpty()) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = petState.value.enabled && petState.value.hatched,
                        onClick = {
                            shopMessage = ""
                            petState.value = PixelPetStore.unequip(context)
                        },
                    ) {
                        Text(stringResource(R.string.pixel_pet_unequip))
                    }
                }
                Text(
                    text = stringResource(R.string.pixel_pet_food_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                PixelPetFoodKind.entries.forEach { food ->
                    PetFoodRow(
                        food = food,
                        state = petState.value,
                        enabled = petState.value.enabled && petState.value.hatched,
                        onBuy = {
                            if (petState.value.coins < food.cost) {
                                shopMessage = insufficientCoinsMessage
                            } else {
                                shopMessage = ""
                                petState.value = PixelPetStore.buyFood(context, food)
                            }
                        },
                        onUse = { petState.value = PixelPetStore.useFood(context, food) },
                    )
                }
                Text(
                    text = stringResource(R.string.pixel_pet_furniture_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (petState.value.furniture.size >= MAX_PIXEL_PET_FURNITURE) {
                    Text(
                        text = stringResource(
                            R.string.pixel_pet_furniture_limit,
                            petState.value.furniture.size,
                            MAX_PIXEL_PET_FURNITURE,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PixelPetFurnitureKind.entries.forEach { furniture ->
                    PetFurnitureRow(
                        furniture = furniture,
                        state = petState.value,
                        enabled = petState.value.enabled && petState.value.hatched,
                        onBuy = {
                            if (petState.value.coins < furniture.cost) {
                                shopMessage = insufficientCoinsMessage
                            } else {
                                shopMessage = ""
                                petState.value = PixelPetStore.buyFurniture(context, furniture)
                            }
                        },
                        onPlace = {
                            if (petState.value.furniture.size >= MAX_PIXEL_PET_FURNITURE) {
                                shopMessage = context.getString(
                                    R.string.pixel_pet_furniture_limit,
                                    petState.value.furniture.size,
                                    MAX_PIXEL_PET_FURNITURE,
                                )
                            } else {
                                petState.value = PixelPetStore.placeFurniture(
                                    context,
                                    furniture,
                                    x = 0.18f + (petState.value.furniture.size % 5) * 0.16f,
                                    y = 0.68f,
                                )
                            }
                        },
                        onRepair = { id ->
                            val before = petState.value
                            val repaired = PixelPetStore.repairFurniture(context, id)
                            petState.value = repaired
                            if (repaired == before && before.coins < PIXEL_PET_FURNITURE_REPAIR_COST) {
                                shopMessage = insufficientCoinsMessage
                            }
                        },
                    )
                }
                }
            }
            if (selectedPage == PixelPetPage.Growth) {
                PetSection(title = stringResource(R.string.pixel_pet_task_title)) {
                PixelPetDailyTask.entries.forEach { task ->
                    PetRewardRow(
                        label = stringResource(task.labelRes),
                        reward = stringResource(R.string.pixel_pet_reward, task.coinsReward, task.growthReward),
                        complete = petState.value.isTaskComplete(task),
                        claimed = task in petState.value.claimedDailyTasks,
                        enabled = petState.value.enabled && petState.value.hatched,
                        onClaim = { petState.value = PixelPetStore.claimDailyTask(context, task) },
                    )
                }
            }
                PetSection(title = stringResource(R.string.pixel_pet_achievement_title)) {
                PixelPetAchievement.entries.forEach { achievement ->
                    PetRewardRow(
                        label = stringResource(achievement.labelRes),
                        reward = stringResource(
                            R.string.pixel_pet_reward,
                            achievement.coinsReward,
                            achievement.growthReward,
                        ),
                        complete = petState.value.isAchievementComplete(achievement),
                        claimed = achievement in petState.value.claimedAchievements,
                        enabled = petState.value.enabled && petState.value.hatched,
                        onClaim = { petState.value = PixelPetStore.claimAchievement(context, achievement) },
                    )
                }
            }
                PetSection(title = stringResource(R.string.pixel_pet_archive_title)) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = petState.value.enabled && petState.value.hatched,
                    onClick = { petState.value = PixelPetStore.saveLook(context) },
                ) {
                    Text(stringResource(R.string.pixel_pet_save_look))
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = petState.value.enabled && petState.value.hatched,
                    onClick = { petState.value = PixelPetStore.resetLkmPosition(context) },
                ) {
                    Text(stringResource(R.string.pixel_pet_reset_position))
                }
                if (petState.value.savedLooks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.pixel_pet_no_saved_looks),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    petState.value.savedLooks.forEachIndexed { index, look ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = petState.value.enabled && petState.value.hatched,
                            onClick = { petState.value = PixelPetStore.applyLook(context, look) },
                        ) {
                            Text(stringResource(R.string.pixel_pet_apply_look, index + 1))
                        }
                    }
                }
            }
                PetSection(title = stringResource(R.string.pixel_pet_reminder_title)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.pixel_pet_reminder_title))
                        Text(
                            text = stringResource(R.string.pixel_pet_reminder_summary),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StyledSwitch(
                        checked = petState.value.reminderEnabled,
                        onCheckedChange = { enabled ->
                            if (
                                enabled &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                petState.value = PixelPetStore.setReminder(context, enabled)
                            }
                        },
                    )
                }
            }
                PetSection(title = stringResource(R.string.pixel_pet_backup_title)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { backupExportLauncher.launch("pixel-pet-save.json") },
                    ) {
                        Icon(Icons.Rounded.FileDownload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.pixel_pet_export))
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { backupImportLauncher.launch(arrayOf("application/json", "text/plain")) },
                    ) {
                        Icon(Icons.Rounded.FileUpload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.pixel_pet_import))
                    }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showResetDialog = true },
                ) {
                    Text(stringResource(R.string.pixel_pet_reset))
                }
            }
                PetSection(title = stringResource(R.string.pixel_pet_progress_title)) {
                PetStat(
                    icon = Icons.Rounded.Restaurant,
                    label = stringResource(R.string.pixel_pet_hunger),
                    value = petState.value.hunger,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                PetStat(
                    icon = Icons.Rounded.Favorite,
                    label = stringResource(R.string.pixel_pet_affection),
                    value = petState.value.affectionInLevel,
                    color = MaterialTheme.colorScheme.error,
                    valueLabel = stringResource(
                        R.string.pixel_pet_affection_level_summary,
                        petState.value.affectionLevel,
                        petState.value.affection,
                    ),
                )
                PetStat(
                    icon = Icons.Rounded.Stars,
                    label = stringResource(R.string.pixel_pet_growth),
                    value = (petState.value.growthInLevel * 4),
                    color = MaterialTheme.colorScheme.primary,
                )
                PetStat(
                    icon = Icons.Rounded.Pets,
                    label = stringResource(R.string.pixel_pet_energy),
                    value = petState.value.energy,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                PetStat(
                    icon = Icons.Rounded.Favorite,
                    label = stringResource(R.string.pixel_pet_cleanliness),
                    value = petState.value.cleanliness,
                    color = MaterialTheme.colorScheme.secondary,
                )
                PetStat(
                    icon = Icons.Rounded.Stars,
                    label = stringResource(R.string.pixel_pet_mood),
                    value = petState.value.moodValue,
                    color = MaterialTheme.colorScheme.primary,
                )
                PetStat(
                    icon = Icons.Rounded.Favorite,
                    label = stringResource(R.string.pixel_pet_sleep_quality),
                    value = petState.value.sleepQuality,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                PetStat(
                    icon = Icons.Rounded.Stars,
                    label = stringResource(R.string.pixel_pet_exploration),
                    value = petState.value.exploration,
                    color = MaterialTheme.colorScheme.secondary,
                )
                PetTextStat(
                    label = stringResource(R.string.pixel_pet_total_interactions),
                    value = petState.value.totalInteractions.toString(),
                )
                PetTextStat(
                    label = stringResource(R.string.pixel_pet_total_feeds),
                    value = petState.value.totalFeeds.toString(),
                )
                PetTextStat(
                    label = stringResource(R.string.pixel_pet_highest_streak),
                    value = petState.value.highestCheckInStreak.toString(),
                )
                }
            }
            }
        }
    }

    pendingBackup?.let { preview ->
        AlertDialog(
            onDismissRequest = {
                pendingBackup = null
                pendingBackupRaw = null
            },
            title = { Text(stringResource(R.string.pixel_pet_backup_preview_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(
                            R.string.pixel_pet_backup_preview_summary,
                            preview.state.name,
                            preview.state.furniture.size,
                            preview.state.foodInventory.values.sum(),
                        ),
                    )
                    Text(
                        stringResource(
                            R.string.pixel_pet_backup_preview_schema,
                            preview.schemaVersion,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        enabled = pendingBackupRaw != null,
                        onClick = {
                            val raw = pendingBackupRaw ?: return@TextButton
                            petState.value = PixelPetStore.restoreBackup(context, raw, PixelPetRestoreMode.Merge)
                            pendingBackup = null
                            pendingBackupRaw = null
                            shopMessage = backupSuccessMessage
                        },
                    ) { Text(stringResource(R.string.pixel_pet_backup_merge)) }
                    Button(
                        enabled = pendingBackupRaw != null,
                        onClick = {
                            val raw = pendingBackupRaw ?: return@Button
                            petState.value = PixelPetStore.restoreBackup(context, raw, PixelPetRestoreMode.Replace)
                            pendingBackup = null
                            pendingBackupRaw = null
                            shopMessage = backupSuccessMessage
                        },
                    ) { Text(stringResource(R.string.pixel_pet_backup_replace)) }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingBackup = null
                    pendingBackupRaw = null
                }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(R.string.pixel_pet_rename_title)) },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it.take(20) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.pixel_pet_name_label)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    petState.value = PixelPetStore.rename(context, nameDraft)
                    showNameDialog = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.pixel_pet_reset_title)) },
            text = { Text(stringResource(R.string.pixel_pet_reset_summary)) },
            confirmButton = {
                TextButton(onClick = {
                    petState.value = PixelPetStore.resetProgress(context)
                    shopMessage = ""
                    showResetDialog = false
                }) {
                    Text(stringResource(R.string.pixel_pet_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

private fun formatPixelPetIncubation(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun PetPrimaryActions(
    state: PixelPetState,
    onInteract: () -> Unit,
    onFeed: () -> Unit,
    onCheckIn: () -> Unit,
) {
    val ready = state.enabled && state.hatched
    PetSection(title = stringResource(R.string.pixel_pet_interaction_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = ApkeUiTokens.MinTouchTarget),
                enabled = ready,
                onClick = onInteract,
            ) {
                Icon(Icons.Rounded.Favorite, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.pixel_pet_interact), maxLines = 1)
            }
            FilledTonalButton(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = ApkeUiTokens.MinTouchTarget),
                enabled = ready && state.coins >= 5,
                onClick = onFeed,
            ) {
                Icon(Icons.Rounded.Restaurant, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.pixel_pet_feed), maxLines = 1)
            }
        }
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ApkeUiTokens.MinTouchTarget),
            enabled = ready && state.canCheckIn,
            onClick = onCheckIn,
        ) {
            Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.pixel_pet_check_in))
        }
    }
}

@Composable
private fun PetHeroCard(
    state: PixelPetState,
    onEnabledChange: (Boolean) -> Unit,
    onRename: () -> Unit,
) {
    PixelPetPreviewFrame(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PixelPetAvatar(state = state, size = 84.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (state.hatched) state.name else stringResource(R.string.pixel_pet_egg_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (state.hatched) {
                        stringResource(
                            R.string.pixel_pet_stage_level_summary,
                            stringResource(state.growthStage.labelRes),
                            state.level,
                            state.growth,
                        )
                    } else if (state.isIncubating) {
                        stringResource(
                            R.string.pixel_pet_incubating,
                            formatPixelPetIncubation(state.incubationRemainingMillis()),
                        )
                    } else {
                        stringResource(R.string.pixel_pet_egg_summary)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Stars, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Text(stringResource(R.string.pixel_pet_coins, state.coins), style = MaterialTheme.typography.labelMedium)
                }
                if (state.hatched) {
                    Text(
                        text = stringResource(R.string.pixel_pet_check_in_streak, state.checkInStreak),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = onRename, modifier = Modifier.height(44.dp)) {
                        Text(stringResource(R.string.pixel_pet_rename))
                    }
                }
            }
            StyledSwitch(checked = state.enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
private fun PetSection(title: String, content: @Composable () -> Unit) {
    PixelPetPanel(title = title, content = content)
}

@Composable
private fun PetShopRow(
    accessory: PixelPetAccessory,
    state: PixelPetState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val owned = accessory in state.ownedAccessories
    val equipped = accessory in state.equippedAccessoriesOrLegacy
    val affordable = owned || state.coins >= accessory.cost
    PixelPetItemSurface(
        selected = equipped,
        onClick = if (enabled) onClick else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Pets, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(accessory.labelRes), modifier = Modifier.weight(1f))
            Text(
                text = when {
                    equipped -> stringResource(R.string.pixel_pet_equipped)
                    owned -> stringResource(R.string.pixel_pet_owned)
                    else -> stringResource(R.string.pixel_pet_item_cost, accessory.cost)
                },
                color = when {
                    equipped -> MaterialTheme.colorScheme.primary
                    owned -> MaterialTheme.colorScheme.secondary
                    affordable -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun PetFoodRow(
    food: PixelPetFoodKind,
    state: PixelPetState,
    enabled: Boolean,
    onBuy: () -> Unit,
    onUse: () -> Unit,
) {
    val amount = state.foodInventory[food] ?: 0
    val canUse = enabled && state.hunger < 100
    PixelPetItemSurface {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Restaurant, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(food.labelRes))
                    Text(
                        text = stringResource(R.string.pixel_pet_food_inventory, amount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (amount > 0) {
                    TextButton(enabled = canUse, onClick = onUse) {
                        Text(stringResource(R.string.pixel_pet_use_food))
                    }
                }
                TextButton(enabled = enabled, onClick = onBuy) {
                    Text(stringResource(R.string.pixel_pet_item_cost, food.cost))
                }
            }
            if (amount > 0 && state.hunger >= 100) {
                Text(
                    text = stringResource(R.string.pixel_pet_food_full),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 38.dp, end = 10.dp, bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PetFurnitureRow(
    furniture: PixelPetFurnitureKind,
    state: PixelPetState,
    enabled: Boolean,
    onBuy: () -> Unit,
    onPlace: () -> Unit,
    onRepair: (String) -> Unit,
) {
    val owned = furniture in state.ownedFurniture
    val placed = state.furniture.count { it.kind == furniture }
    val damagedFurniture = state.furniture.firstOrNull { it.kind == furniture && it.durability < 100 }
    val reachedLimit = state.furniture.size >= MAX_PIXEL_PET_FURNITURE
    PixelPetItemSurface(selected = owned) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Pets, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(furniture.labelRes))
                    Text(
                        text = stringResource(R.string.pixel_pet_furniture_placed, placed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (owned) {
                    if (damagedFurniture != null) {
                        TextButton(
                            enabled = enabled && state.coins >= PIXEL_PET_FURNITURE_REPAIR_COST,
                            onClick = { onRepair(damagedFurniture.id) },
                        ) {
                            Text(stringResource(R.string.pixel_pet_furniture_repair))
                        }
                    }
                    TextButton(enabled = enabled && !reachedLimit, onClick = onPlace) {
                        Text(stringResource(R.string.pixel_pet_furniture_place))
                    }
                } else {
                    TextButton(enabled = enabled, onClick = onBuy) {
                        Text(stringResource(R.string.pixel_pet_item_cost, furniture.cost))
                    }
                }
            }
            damagedFurniture?.let { item ->
                Text(
                    text = stringResource(R.string.pixel_pet_furniture_damaged, item.durability),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 38.dp, end = 10.dp, bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PetRewardRow(
    label: String,
    reward: String,
    complete: Boolean,
    claimed: Boolean,
    enabled: Boolean,
    onClaim: () -> Unit,
) {
    PixelPetItemSurface(selected = claimed) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = reward,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                claimed -> Text(
                    stringResource(R.string.pixel_pet_claimed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                complete -> Button(
                    enabled = enabled,
                    onClick = onClaim,
                ) { Text(stringResource(R.string.pixel_pet_claim)) }
                else -> Text(
                    text = "-",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PetTextStat(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PetStat(
    icon: ImageVector,
    label: String,
    value: Int,
    color: Color,
    valueLabel: String = "$value%",
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = color)
        Text(label, modifier = Modifier.width(64.dp), style = MaterialTheme.typography.labelMedium)
        LinearProgressIndicator(
            progress = { value.coerceIn(0, 100) / 100f },
            modifier = Modifier.weight(1f),
            color = color,
        )
        Text(valueLabel, style = MaterialTheme.typography.labelSmall)
    }
}
