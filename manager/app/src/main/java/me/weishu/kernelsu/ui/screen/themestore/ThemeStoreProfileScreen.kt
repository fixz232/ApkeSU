package me.weishu.kernelsu.ui.screen.themestore

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.rememberCustomImageBitmap
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproColors
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproScreen
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.util.FULL_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.ThemeAuthorGender
import me.weishu.kernelsu.ui.util.ThemeAuthorProfile
import me.weishu.kernelsu.ui.util.readThemeAuthorProfile
import me.weishu.kernelsu.ui.util.readThemeLibrary
import me.weishu.kernelsu.ui.util.saveThemeAuthorProfile
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun ThemeStoreMyScreen(
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf(readThemeAuthorProfile(context)) }
    var savedThemeCount by remember { mutableIntStateOf(readThemeLibrary(context).size) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var displayName by rememberSaveable { mutableStateOf(profile.displayName) }
    var realName by rememberSaveable { mutableStateOf(profile.realName) }
    var gender by rememberSaveable { mutableStateOf(profile.gender) }
    var bio by rememberSaveable { mutableStateOf(profile.bio) }
    var pendingAvatarUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var removeAvatar by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    fun resetDraft() {
        displayName = profile.displayName
        realName = profile.realName
        gender = profile.gender
        bio = profile.bio
        pendingAvatarUri = null
        removeAvatar = false
    }

    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        pendingAvatarUri = uri
        removeAvatar = false
    }

    LifecycleResumeEffect(editing) {
        if (!editing) {
            profile = readThemeAuthorProfile(context)
            savedThemeCount = readThemeLibrary(context).size
        }
        onPauseOrDispose { }
    }

    val onBack = dropUnlessResumed { navigator.pop() }
    val content: @Composable (PaddingValues) -> Unit = { paddingValues ->
        ThemeStoreProfileContent(
            profile = profile,
            savedThemeCount = savedThemeCount,
            editing = editing,
            busy = busy,
            displayName = displayName,
            realName = realName,
            gender = gender,
            bio = bio,
            avatarUriString = when {
                pendingAvatarUri != null -> pendingAvatarUri.toString()
                removeAvatar -> null
                else -> profile.avatarUriString
            },
            modifier = modifier.padding(paddingValues),
            onEdit = {
                resetDraft()
                editing = true
            },
            onCancel = {
                resetDraft()
                editing = false
            },
            onDisplayNameChange = { displayName = it.take(32) },
            onRealNameChange = { realName = it.take(32) },
            onGenderChange = { gender = it },
            onBioChange = { bio = it.take(160) },
            onPickAvatar = { avatarLauncher.launch(arrayOf("image/*")) },
            onRemoveAvatar = {
                pendingAvatarUri = null
                removeAvatar = true
            },
            onSave = {
                if (busy) return@ThemeStoreProfileContent
                busy = true
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            saveThemeAuthorProfile(
                                context = context,
                                profile = ThemeAuthorProfile(
                                    displayName = displayName,
                                    realName = realName,
                                    gender = gender,
                                    bio = bio,
                                    avatarUriString = profile.avatarUriString,
                                ),
                                avatarSource = pendingAvatarUri,
                                removeAvatar = removeAvatar,
                            )
                        }
                        if (result.success && result.profile != null) {
                            profile = result.profile
                            resetDraft()
                            editing = false
                            Toast.makeText(
                                context,
                                R.string.theme_store_profile_saved,
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            errorMessage = result.error?.localizedMessage
                                ?.lineSequence()
                                ?.firstOrNull()
                                ?.take(240)
                                ?: resources.getString(R.string.theme_store_profile_save_failed)
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Throwable) {
                        errorMessage = error.localizedMessage
                            ?.lineSequence()
                            ?.firstOrNull()
                            ?.take(240)
                            ?: resources.getString(R.string.theme_store_profile_save_failed)
                    } finally {
                        busy = false
                    }
                }
            },
            onOpenLibrary = dropUnlessResumed { navigator.push(Route.ThemeStoreLibrary) },
            onOpenCreatorCenter = dropUnlessResumed { navigator.push(Route.CloudThemeCreator) },
        )
    }

    if (embedded) {
        content(PaddingValues())
    } else if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproScreen(
            title = stringResource(R.string.theme_store_my_title),
            bottomInnerPadding = 0.dp,
        ) { paddingValues ->
            Box {
                content(paddingValues)
                ThemeStoreProfileBackButton(onClick = onBack)
            }
        }
    } else {
        MiuixScaffold(
            containerColor = Color.Transparent,
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars
                .add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
            topBar = {
                MiuixTopAppBar(
                    title = stringResource(R.string.theme_store_my_title),
                    color = Color.Transparent,
                    titleColor = colorScheme.onSurface,
                    navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            MiuixIcon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = colorScheme.onBackground,
                            )
                        }
                    },
                )
            },
            content = content,
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.theme_store_profile_save_failed)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}

@Composable
private fun ThemeStoreProfileContent(
    profile: ThemeAuthorProfile,
    savedThemeCount: Int,
    editing: Boolean,
    busy: Boolean,
    displayName: String,
    realName: String,
    gender: ThemeAuthorGender,
    bio: String,
    avatarUriString: String?,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onRealNameChange: (String) -> Unit,
    onGenderChange: (ThemeAuthorGender) -> Unit,
    onBioChange: (String) -> Unit,
    onPickAvatar: () -> Unit,
    onRemoveAvatar: () -> Unit,
    onSave: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCreatorCenter: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            if (editing) {
                ThemeStoreProfileEditor(
                    busy = busy,
                    displayName = displayName,
                    realName = realName,
                    gender = gender,
                    bio = bio,
                    avatarUriString = avatarUriString,
                    onDisplayNameChange = onDisplayNameChange,
                    onRealNameChange = onRealNameChange,
                    onGenderChange = onGenderChange,
                    onBioChange = onBioChange,
                    onPickAvatar = onPickAvatar,
                    onRemoveAvatar = onRemoveAvatar,
                    onSave = onSave,
                    onCancel = onCancel,
                )
            } else {
                ThemeStoreProfileSummary(profile = profile, onEdit = onEdit)
            }
        }
        item {
            ThemeStoreLibraryDestination(
                savedThemeCount = savedThemeCount,
                enabled = !busy,
                onClick = onOpenLibrary,
            )
        }
        item {
            ThemeStoreCreatorDestination(
                enabled = !busy,
                onClick = onOpenCreatorCenter,
            )
        }
        item {
            Text(
                text = stringResource(R.string.theme_store_profile_export_notice),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = themeStoreProfileMutedColor(),
            )
        }
        item { Spacer(modifier = Modifier.height(18.dp)) }
    }
}

@Composable
private fun ThemeStoreCreatorDestination(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ThemeStoreProfileSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cloud_theme_creator_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = themeStoreProfileTextColor(),
                )
                Text(
                    text = stringResource(R.string.cloud_theme_creator_entry_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeStoreProfileMutedColor(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = themeStoreProfileMutedColor(),
            )
        }
    }
}

@Composable
private fun ThemeStoreProfileSummary(
    profile: ThemeAuthorProfile,
    onEdit: () -> Unit,
) {
    ThemeStoreProfileSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ThemeStoreAuthorAvatar(
                    uriString = profile.avatarUriString,
                    size = 76,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = profile.displayName
                            .takeIf(String::isNotBlank)
                            ?: profile.realName.takeIf(String::isNotBlank)
                            ?: stringResource(R.string.theme_store_profile_unnamed),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = themeStoreProfileTextColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    profile.realName
                        .takeIf { it.isNotBlank() && it != profile.displayName }
                        ?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = themeStoreProfileMutedColor(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    if (profile.gender != ThemeAuthorGender.Unspecified) {
                        Text(
                            text = themeAuthorGenderLabel(profile.gender),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.theme_store_profile_edit))
                }
            }
            Text(
                text = profile.bio.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.theme_store_profile_bio_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = if (profile.bio.isBlank()) {
                    themeStoreProfileMutedColor()
                } else {
                    themeStoreProfileTextColor()
                },
            )
        }
    }
}

@Composable
private fun ThemeStoreProfileEditor(
    busy: Boolean,
    displayName: String,
    realName: String,
    gender: ThemeAuthorGender,
    bio: String,
    avatarUriString: String?,
    onDisplayNameChange: (String) -> Unit,
    onRealNameChange: (String) -> Unit,
    onGenderChange: (ThemeAuthorGender) -> Unit,
    onBioChange: (String) -> Unit,
    onPickAvatar: () -> Unit,
    onRemoveAvatar: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    ThemeStoreProfileSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ThemeStoreAuthorAvatar(uriString = avatarUriString, size = 72)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Button(enabled = !busy, onClick = onPickAvatar) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(R.string.theme_store_profile_choose_avatar))
                    }
                    if (!avatarUriString.isNullOrBlank()) {
                        TextButton(enabled = !busy, onClick = onRemoveAvatar) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.theme_store_profile_remove_avatar))
                        }
                    }
                }
            }
            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                singleLine = true,
                label = { Text(stringResource(R.string.theme_store_profile_display_name_label)) },
                supportingText = {
                    Text(stringResource(R.string.theme_store_profile_display_name_hint))
                },
            )
            OutlinedTextField(
                value = realName,
                onValueChange = onRealNameChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                singleLine = true,
                label = { Text(stringResource(R.string.theme_store_profile_real_name_label)) },
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.theme_store_profile_gender_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = themeStoreProfileTextColor(),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ThemeAuthorGender.entries.forEach { option ->
                        FilterChip(
                            selected = gender == option,
                            enabled = !busy,
                            onClick = { onGenderChange(option) },
                            label = { Text(themeAuthorGenderLabel(option)) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = bio,
                onValueChange = onBioChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                minLines = 3,
                maxLines = 4,
                label = { Text(stringResource(R.string.theme_store_profile_bio_label)) },
                supportingText = {
                    Text(stringResource(R.string.theme_store_profile_bio_count, bio.length, 160))
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    onClick = onCancel,
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    onClick = onSave,
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.theme_store_profile_save))
                }
            }
        }
    }
}

@Composable
private fun ThemeStoreLibraryDestination(
    savedThemeCount: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ThemeStoreProfileSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.theme_store_my_library_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = themeStoreProfileTextColor(),
                )
                Text(
                    text = stringResource(R.string.theme_store_my_count, savedThemeCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeStoreProfileMutedColor(),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = themeStoreProfileMutedColor(),
            )
        }
    }
}

@Composable
private fun ThemeStoreAuthorAvatar(uriString: String?, size: Int) {
    val bitmap = rememberCustomImageBitmap(
        uriString = uriString,
        maxSide = 512,
        crop = FULL_CUSTOM_WALLPAPER_CROP,
    )
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.theme_store_profile_avatar_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                modifier = Modifier.size((size * 0.46f).dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ThemeStoreProfileBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 16.dp, top = 14.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = Color.White,
        )
    }
}

@Composable
private fun ThemeStoreProfileSurface(content: @Composable () -> Unit) {
    if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SkrootproColors.BarSurface),
        ) {
            content()
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth(), content = { content() })
    }
}

@Composable
private fun themeStoreProfileTextColor(): Color {
    return if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproColors.Text
    } else {
        MaterialTheme.colorScheme.onSurface
    }
}

@Composable
private fun themeStoreProfileMutedColor(): Color {
    return if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproColors.Muted
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
}
