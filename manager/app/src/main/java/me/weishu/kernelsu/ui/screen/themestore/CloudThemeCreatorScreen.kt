package me.weishu.kernelsu.ui.screen.themestore

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Rule
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Drafts
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproColors
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproScreen
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.CLOUD_THEME_CREATOR_PICKER_MIME_TYPE
import me.weishu.kernelsu.ui.util.CLOUD_THEME_CREATOR_REVIEWER
import me.weishu.kernelsu.ui.util.CloudThemeCategory
import me.weishu.kernelsu.ui.util.CloudThemeCreatorActivity
import me.weishu.kernelsu.ui.util.CloudThemeCreatorApplicationStatus
import me.weishu.kernelsu.ui.util.CloudThemeCreatorRegistrySnapshot
import me.weishu.kernelsu.ui.util.CloudThemeCreatorRepository
import me.weishu.kernelsu.ui.util.CloudThemeRepository
import me.weishu.kernelsu.ui.util.CloudThemeSubmissionDraft
import me.weishu.kernelsu.ui.util.CloudThemeSubmissionReview
import me.weishu.kernelsu.ui.util.CloudThemeSubmissionReviewStatus
import me.weishu.kernelsu.ui.util.THEME_STORE_FILE_EXTENSION
import me.weishu.kernelsu.ui.util.THEME_STORE_FILE_MIME_TYPE
import me.weishu.kernelsu.ui.util.buildCloudThemeCreatorApplicationUrl
import me.weishu.kernelsu.ui.util.buildCloudThemeSubmissionIssueUrl
import me.weishu.kernelsu.ui.util.buildCloudThemeSubmissionManifest
import me.weishu.kernelsu.ui.util.canonicalCloudThemePackageFileName
import me.weishu.kernelsu.ui.util.exportCloudThemeStorePackage
import me.weishu.kernelsu.ui.util.isValidCloudThemeGithubLogin
import me.weishu.kernelsu.ui.util.readThemeAuthorProfile
import me.weishu.kernelsu.ui.util.safeCloudThemeMessage
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private enum class CreatorCenterPage(@StringRes val titleRes: Int, val icon: ImageVector) {
    Qualification(R.string.cloud_theme_creator_tab_qualification, Icons.Rounded.VerifiedUser),
    Submission(R.string.cloud_theme_creator_tab_submission, Icons.Rounded.CloudUpload),
    Records(R.string.cloud_theme_creator_tab_records, Icons.Rounded.History),
}

@Composable
fun CloudThemeCreatorScreen(initialPageIndex: Int = 0) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val creatorRepository = remember(context) { CloudThemeCreatorRepository(context) }
    val cloudRepository = remember(context) { CloudThemeRepository(context) }
    val localProfile = remember(context) { readThemeAuthorProfile(context) }
    var selectedPageIndex by rememberSaveable {
        mutableIntStateOf(initialPageIndex.coerceIn(0, CreatorCenterPage.entries.lastIndex))
    }
    var draft by remember {
        mutableStateOf(
            creatorRepository.readDraft().let { saved ->
                saved.copy(
                    authorName = saved.authorName.ifBlank { localProfile.displayName },
                    authorBio = saved.authorBio.ifBlank { localProfile.bio },
                    minManagerVersionCodeText = saved.minManagerVersionCodeText
                        .ifBlank { BuildConfig.VERSION_CODE.toString() },
                )
            }
        )
    }
    var registrySnapshot by remember { mutableStateOf<CloudThemeCreatorRegistrySnapshot?>(null) }
    var creatorActivity by remember { mutableStateOf<CloudThemeCreatorActivity?>(null) }
    var knownCategories by remember { mutableStateOf<List<CloudThemeCategory>>(emptyList()) }
    var registryLoading by remember { mutableStateOf(true) }
    var activityLoading by remember { mutableStateOf(false) }
    var packageLoading by remember { mutableStateOf(false) }
    var remoteVerifying by remember { mutableStateOf(false) }
    var clearDraftDialog by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val approvedCreator = registrySnapshot?.registry?.creator(draft.githubLogin)
    val approved = approvedCreator != null
    val normalizedGithubLogin = draft.githubLogin.trim().lowercase()
    val currentActivity = creatorActivity?.takeIf {
        it.githubLogin == normalizedGithubLogin
    }
    val applicationStatus = if (approved) {
        CloudThemeCreatorApplicationStatus.Approved
    } else if (currentActivity?.applicationStatus == CloudThemeCreatorApplicationStatus.Approved) {
        CloudThemeCreatorApplicationStatus.RegistryPending
    } else {
        currentActivity?.applicationStatus ?: CloudThemeCreatorApplicationStatus.NotApplied
    }

    fun showError(error: Throwable) {
        errorMessage = error.safeCloudThemeMessage()
    }

    fun openGithubIssueForm(url: String) {
        runCatching {
            val intent = Intent.makeMainSelectorActivity(
                Intent.ACTION_MAIN,
                Intent.CATEGORY_APP_BROWSER,
            ).apply {
                data = url.toUri()
            }
            context.startActivity(intent)
        }.recoverCatching {
            uriHandler.openUri(url)
        }.getOrThrow()
    }

    fun refreshActivity() {
        if (activityLoading || !isValidCloudThemeGithubLogin(draft.githubLogin)) return
        val requestedLogin = draft.githubLogin.trim()
        activityLoading = true
        scope.launch {
            try {
                val loaded = creatorRepository.loadCreatorActivity(requestedLogin)
                if (draft.githubLogin.trim().equals(requestedLogin, ignoreCase = true)) {
                    creatorActivity = loaded
                }
            } catch (error: Throwable) {
                if (draft.githubLogin.trim().equals(requestedLogin, ignoreCase = true)) {
                    showError(error)
                }
            } finally {
                activityLoading = false
            }
        }
    }

    fun refreshRegistry(force: Boolean) {
        if (registryLoading) return
        val requestedLogin = draft.githubLogin.trim()
        registryLoading = true
        scope.launch {
            try {
                registrySnapshot = creatorRepository.loadRegistry(forceRefresh = force)
                if (isValidCloudThemeGithubLogin(requestedLogin)) {
                    val loaded = runCatching {
                        creatorRepository.loadCreatorActivity(requestedLogin)
                    }.onFailure {
                        if (draft.githubLogin.trim().equals(requestedLogin, ignoreCase = true)) {
                            showError(it)
                        }
                    }.getOrNull()
                    if (loaded != null &&
                        draft.githubLogin.trim().equals(requestedLogin, ignoreCase = true)
                    ) {
                        creatorActivity = loaded
                    }
                }
            } catch (error: Throwable) {
                showError(error)
            } finally {
                registryLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val requestedLogin = draft.githubLogin.trim()
        try {
            registrySnapshot = creatorRepository.loadRegistry()
            knownCategories = runCatching { cloudRepository.loadCatalog().catalog.categories }
                .getOrDefault(emptyList())
            if (isValidCloudThemeGithubLogin(requestedLogin)) {
                val loaded = runCatching {
                    creatorRepository.loadCreatorActivity(requestedLogin)
                }.getOrNull()
                if (loaded != null &&
                    draft.githubLogin.trim().equals(requestedLogin, ignoreCase = true)
                ) {
                    creatorActivity = loaded
                }
            }
        } catch (error: Throwable) {
            showError(error)
        } finally {
            registryLoading = false
        }
    }

    LaunchedEffect(draft, packageLoading) {
        if (packageLoading) return@LaunchedEffect
        delay(400)
        runCatching {
            withContext(Dispatchers.IO) { creatorRepository.saveDraft(draft) }
        }.onFailure(::showError)
    }

    suspend fun inspectAndStorePackage(uri: Uri, generated: Boolean) {
        val inspection = creatorRepository.inspectPackage(uri)
        draft = draft.copy(
            packageUri = inspection.uriString,
            packageName = inspection.displayName,
            packageSha256 = inspection.sha256,
            packageSizeBytes = inspection.sizeBytes,
            packageVersion = inspection.packageVersion,
            packageResourceCount = inspection.configuredResourceCount,
            authorName = draft.authorName.ifBlank {
                inspection.authorDisplayName.orEmpty()
            },
        ).invalidateRemoteVerification()
        Toast.makeText(
            context,
            when {
                generated -> R.string.cloud_theme_creator_cloud_package_created
                inspection.warnings.isEmpty() -> R.string.cloud_theme_creator_package_valid
                else -> R.string.cloud_theme_creator_package_valid_with_warnings
            },
            Toast.LENGTH_LONG,
        ).show()
    }

    val canonicalPackageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(THEME_STORE_FILE_MIME_TYPE),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        if (packageLoading) return@rememberLauncherForActivityResult
        val packageSnapshot = draft
        packageLoading = true
        scope.launch {
            try {
                creatorRepository.exportInspectedPackage(
                    sourceUriString = packageSnapshot.packageUri,
                    destination = uri,
                    expectedSha256 = packageSnapshot.packageSha256,
                    expectedSizeBytes = packageSnapshot.packageSizeBytes,
                )
                Toast.makeText(
                    context,
                    R.string.cloud_theme_creator_package_exported,
                    Toast.LENGTH_LONG,
                ).show()
            } catch (error: Throwable) {
                showError(error)
            } finally {
                packageLoading = false
            }
        }
    }
    val packageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        if (packageLoading) return@rememberLauncherForActivityResult
        packageLoading = true
        scope.launch {
            try {
                inspectAndStorePackage(uri, generated = false)
            } catch (error: Throwable) {
                showError(error)
            } finally {
                packageLoading = false
            }
        }
    }
    val cloudPackageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(THEME_STORE_FILE_MIME_TYPE),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        if (packageLoading) return@rememberLauncherForActivityResult
        packageLoading = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    exportCloudThemeStorePackage(context, uri)
                }
                if (!result.success) {
                    throw result.error ?: IllegalStateException("Unable to create cloud theme package")
                }
                inspectAndStorePackage(uri, generated = true)
            } catch (error: Throwable) {
                showError(error)
            } finally {
                packageLoading = false
            }
        }
    }

    fun openCreatorApplication() {
        runCatching {
            val url = buildCloudThemeCreatorApplicationUrl(
                githubLogin = draft.githubLogin,
                displayName = draft.authorName,
            )
            creatorRepository.saveDraft(draft)
            openGithubIssueForm(url)
        }.onFailure(::showError)
    }

    fun verifyRemotePackage() {
        if (remoteVerifying) return
        remoteVerifying = true
        scope.launch {
            try {
                require(draft.hasInspectedPackage) {
                    resources.getString(R.string.cloud_theme_creator_select_package_first)
                }
                val result = creatorRepository.verifyRemotePackage(
                    packageUrl = draft.packageUrl,
                    expectedSha256 = draft.packageSha256,
                    expectedSizeBytes = draft.packageSizeBytes,
                )
                draft = draft.copy(
                    remoteVerifiedUrl = draft.packageUrl.trim(),
                    remoteVerifiedSha256 = result.sha256,
                    remoteVerifiedAt = result.verifiedAt,
                )
                Toast.makeText(
                    context,
                    R.string.cloud_theme_creator_remote_verified,
                    Toast.LENGTH_LONG,
                ).show()
            } catch (error: Throwable) {
                draft = draft.invalidateRemoteVerification()
                showError(error)
            } finally {
                remoteVerifying = false
            }
        }
    }

    fun submitForReview() {
        runCatching {
            require(approved) {
                resources.getString(R.string.cloud_theme_creator_not_approved_error)
            }
            val manifest = buildCloudThemeSubmissionManifest(draft)
            val url = buildCloudThemeSubmissionIssueUrl(draft, manifest)
            creatorRepository.saveDraft(draft)
            openGithubIssueForm(url)
        }.onFailure(::showError)
    }

    val onBack = dropUnlessResumed { navigator.pop() }
    val content: @Composable (PaddingValues) -> Unit = { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedPageIndex) {
                CreatorCenterPage.entries.forEachIndexed { index, page ->
                    Tab(
                        selected = selectedPageIndex == index,
                        onClick = { selectedPageIndex = index },
                        text = {
                            Text(
                                text = stringResource(page.titleRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                            )
                        },
                    )
                }
            }
            when (CreatorCenterPage.entries[selectedPageIndex]) {
                CreatorCenterPage.Qualification -> CreatorQualificationPage(
                    draft = draft,
                    registrySnapshot = registrySnapshot,
                    applicationStatus = applicationStatus,
                    applicationUrl = currentActivity?.applicationUrl,
                    registryLoading = registryLoading,
                    activityLoading = activityLoading,
                    onGithubLoginChange = {
                        draft = draft.copy(githubLogin = it.take(39))
                        creatorActivity = null
                    },
                    onCreatorNameChange = { draft = draft.copy(authorName = it.take(64)) },
                    onRefresh = { refreshRegistry(force = true) },
                    onApply = ::openCreatorApplication,
                    onOpenApplication = { url ->
                        runCatching { uriHandler.openUri(url) }.onFailure(::showError)
                    },
                )

                CreatorCenterPage.Submission -> CreatorSubmissionPage(
                    approved = approved,
                    approvedName = approvedCreator?.displayName,
                    draft = draft,
                    knownCategories = knownCategories,
                    packageLoading = packageLoading,
                    remoteVerifying = remoteVerifying,
                    onDraftChange = { draft = it },
                    onSelectPackage = {
                        packageLauncher.launch(arrayOf(CLOUD_THEME_CREATOR_PICKER_MIME_TYPE))
                    },
                    onCreatePackage = {
                        cloudPackageLauncher.launch(
                            "apkesu-cloud-theme.$THEME_STORE_FILE_EXTENSION"
                        )
                    },
                    onExportPackage = {
                        canonicalPackageLauncher.launch(
                            canonicalCloudThemePackageFileName(draft.packageName)
                        )
                    },
                    onVerifyRemote = ::verifyRemotePackage,
                    onSaveDraft = {
                        runCatching { creatorRepository.saveDraft(draft) }
                            .onSuccess {
                                Toast.makeText(
                                    context,
                                    R.string.cloud_theme_creator_draft_saved,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            .onFailure(::showError)
                    },
                    onClearDraft = { clearDraftDialog = true },
                    onSubmit = ::submitForReview,
                    onShowQualification = { selectedPageIndex = CreatorCenterPage.Qualification.ordinal },
                )

                CreatorCenterPage.Records -> CreatorRecordsPage(
                    githubLogin = draft.githubLogin,
                    approved = approved,
                    applicationStatus = applicationStatus,
                    activity = currentActivity,
                    loading = activityLoading,
                    onRefresh = ::refreshActivity,
                    onOpenIssue = { url ->
                        runCatching { uriHandler.openUri(url) }.onFailure(::showError)
                    },
                )
            }
        }
    }

    if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproScreen(
            title = stringResource(R.string.cloud_theme_creator_title),
            bottomInnerPadding = 0.dp,
        ) { paddingValues ->
            Box {
                content(paddingValues)
                CreatorBackButton(onClick = onBack)
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
                    title = stringResource(R.string.cloud_theme_creator_title),
                    color = Color.Transparent,
                    titleColor = colorScheme.onSurface,
                    navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            MiuixIcon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.close),
                                tint = colorScheme.onBackground,
                            )
                        }
                    },
                )
            },
            content = content,
        )
    }

    if (clearDraftDialog) {
        AlertDialog(
            onDismissRequest = { clearDraftDialog = false },
            title = { Text(stringResource(R.string.cloud_theme_creator_clear_draft_title)) },
            text = { Text(stringResource(R.string.cloud_theme_creator_clear_draft_message)) },
            dismissButton = {
                TextButton(onClick = { clearDraftDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearDraftDialog = false
                        creatorRepository.clearDraft()
                        draft = CloudThemeSubmissionDraft(
                            githubLogin = draft.githubLogin,
                            authorName = draft.authorName,
                            authorBio = draft.authorBio,
                            authorProfileUrl = draft.authorProfileUrl,
                            authorAvatarUrl = draft.authorAvatarUrl,
                            minManagerVersionCodeText = BuildConfig.VERSION_CODE.toString(),
                        )
                    },
                ) {
                    Text(stringResource(R.string.cloud_theme_creator_clear_draft_action))
                }
            },
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.cloud_theme_creator_operation_failed)) },
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
private fun CreatorQualificationPage(
    draft: CloudThemeSubmissionDraft,
    registrySnapshot: CloudThemeCreatorRegistrySnapshot?,
    applicationStatus: CloudThemeCreatorApplicationStatus,
    applicationUrl: String?,
    registryLoading: Boolean,
    activityLoading: Boolean,
    onGithubLoginChange: (String) -> Unit,
    onCreatorNameChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onApply: () -> Unit,
    onOpenApplication: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CreatorStatusCard(
                status = applicationStatus,
                loading = registryLoading || activityLoading,
                offline = registrySnapshot?.offline == true,
            )
        }
        item {
            CreatorSurface {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CreatorSectionTitle(
                        icon = Icons.Rounded.Person,
                        title = stringResource(R.string.cloud_theme_creator_identity_title),
                    )
                    OutlinedTextField(
                        value = draft.githubLogin,
                        onValueChange = onGithubLoginChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.cloud_theme_creator_github_login)) },
                        supportingText = {
                            Text(stringResource(R.string.cloud_theme_creator_github_login_hint))
                        },
                        isError = draft.githubLogin.isNotBlank() &&
                            !isValidCloudThemeGithubLogin(draft.githubLogin),
                    )
                    OutlinedTextField(
                        value = draft.authorName,
                        onValueChange = onCreatorNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.cloud_theme_creator_public_name)) },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = !registryLoading && !activityLoading &&
                                isValidCloudThemeGithubLogin(draft.githubLogin),
                            onClick = onRefresh,
                        ) {
                            if (registryLoading || activityLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.cloud_theme_creator_check_status))
                        }
                        if (
                            applicationStatus == CloudThemeCreatorApplicationStatus.NotApplied ||
                            applicationStatus == CloudThemeCreatorApplicationStatus.Rejected ||
                            applicationStatus == CloudThemeCreatorApplicationStatus.NeedsChanges
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = isValidCloudThemeGithubLogin(draft.githubLogin) &&
                                    draft.authorName.isNotBlank(),
                                onClick = onApply,
                            ) {
                                Icon(Icons.Rounded.Drafts, contentDescription = null)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(stringResource(R.string.cloud_theme_creator_apply))
                            }
                        }
                    }
                    applicationUrl?.let { url ->
                        TextButton(onClick = { onOpenApplication(url) }) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.cloud_theme_creator_open_application))
                        }
                    }
                }
            }
        }
        item {
            CreatorSurface {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CreatorIcon(Icons.AutoMirrored.Rounded.Rule)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.cloud_theme_creator_review_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = creatorTextColor(),
                        )
                        Text(
                            text = stringResource(
                                R.string.cloud_theme_creator_review_summary,
                                CLOUD_THEME_CREATOR_REVIEWER,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = creatorMutedColor(),
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CreatorSubmissionPage(
    approved: Boolean,
    approvedName: String?,
    draft: CloudThemeSubmissionDraft,
    knownCategories: List<CloudThemeCategory>,
    packageLoading: Boolean,
    remoteVerifying: Boolean,
    onDraftChange: (CloudThemeSubmissionDraft) -> Unit,
    onSelectPackage: () -> Unit,
    onCreatePackage: () -> Unit,
    onExportPackage: () -> Unit,
    onVerifyRemote: () -> Unit,
    onSaveDraft: () -> Unit,
    onClearDraft: () -> Unit,
    onSubmit: () -> Unit,
    onShowQualification: () -> Unit,
) {
    if (!approved) {
        CreatorLockedSubmission(onShowQualification)
        return
    }
    val selectedExistingCategory = knownCategories.firstOrNull {
        it.id == draft.categoryId.trim()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CreatorSurface {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CreatorIcon(Icons.Rounded.VerifiedUser)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = approvedName.orEmpty().ifBlank { draft.githubLogin },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = creatorTextColor(),
                            )
                            Text(
                                text = stringResource(R.string.cloud_theme_creator_approved_identity),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    HorizontalDivider(color = creatorMutedColor().copy(alpha = 0.18f))
                    OutlinedTextField(
                        value = draft.authorName,
                        onValueChange = { onDraftChange(draft.copy(authorName = it.take(64))) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.cloud_theme_creator_public_name)) },
                    )
                    OutlinedTextField(
                        value = draft.authorBio,
                        onValueChange = { onDraftChange(draft.copy(authorBio = it.take(512))) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        label = { Text(stringResource(R.string.cloud_theme_creator_author_bio)) },
                    )
                    OutlinedTextField(
                        value = draft.authorProfileUrl,
                        onValueChange = { onDraftChange(draft.copy(authorProfileUrl = it.take(768))) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.cloud_theme_creator_profile_url)) },
                        supportingText = {
                            Text(stringResource(R.string.cloud_theme_creator_profile_url_hint))
                        },
                    )
                    OutlinedTextField(
                        value = draft.authorAvatarUrl,
                        onValueChange = { onDraftChange(draft.copy(authorAvatarUrl = it.take(768))) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.cloud_theme_creator_avatar_url)) },
                    )
                }
            }
        }
        item {
            CreatorPackageCard(
                draft = draft,
                loading = packageLoading,
                onSelectPackage = onSelectPackage,
                onCreatePackage = onCreatePackage,
                onExportPackage = onExportPackage,
            )
        }
        item {
            CreatorSurface {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CreatorSectionTitle(
                        icon = Icons.Rounded.Description,
                        title = stringResource(R.string.cloud_theme_creator_theme_info),
                    )
                    OutlinedTextField(
                        value = draft.themeId,
                        onValueChange = { onDraftChange(draft.copy(themeId = it.take(80))) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.cloud_theme_creator_theme_id)) },
                    )
                    OutlinedTextField(
                        value = draft.themeName,
                        onValueChange = { onDraftChange(draft.copy(themeName = it.take(80))) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.cloud_theme_creator_theme_name)) },
                    )
                    OutlinedTextField(
                        value = draft.description,
                        onValueChange = { onDraftChange(draft.copy(description = it.take(1000))) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        label = { Text(stringResource(R.string.cloud_theme_creator_description)) },
                    )
                    OutlinedTextField(
                        value = draft.tagsText,
                        onValueChange = { onDraftChange(draft.copy(tagsText = it.take(512))) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.cloud_theme_creator_tags)) },
                        supportingText = {
                            Text(stringResource(R.string.cloud_theme_creator_tags_hint))
                        },
                    )
                }
            }
        }
        item {
            CreatorSurface {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CreatorSectionTitle(
                        icon = Icons.Rounded.Category,
                        title = stringResource(R.string.cloud_theme_creator_category_title),
                    )
                    if (knownCategories.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            knownCategories.take(12).forEach { category ->
                                FilterChip(
                                    selected = draft.categoryId == category.id,
                                    onClick = {
                                        onDraftChange(
                                            draft.copy(
                                                categoryId = category.id,
                                                categoryName = category.name,
                                            )
                                        )
                                    },
                                    label = { Text(category.name) },
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = draft.categoryId,
                            onValueChange = { rawId ->
                                val nextId = rawId
                                    .lowercase()
                                    .filter { character ->
                                        character in 'a'..'z' ||
                                            character in '0'..'9' ||
                                            character == '_' ||
                                            character == '-'
                                    }
                                    .take(40)
                                val nextExisting = knownCategories.firstOrNull { it.id == nextId }
                                onDraftChange(
                                    draft.copy(
                                        categoryId = nextId,
                                        categoryName = nextExisting?.name ?: if (
                                            selectedExistingCategory != null
                                        ) {
                                            ""
                                        } else {
                                            draft.categoryName
                                        },
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(stringResource(R.string.cloud_theme_creator_category_id)) },
                        )
                        OutlinedTextField(
                            value = draft.categoryName,
                            onValueChange = { onDraftChange(draft.copy(categoryName = it.take(48))) },
                            modifier = Modifier.weight(1f),
                            enabled = selectedExistingCategory == null,
                            singleLine = true,
                            label = { Text(stringResource(R.string.cloud_theme_creator_category_name)) },
                        )
                    }
                    Text(
                        text = stringResource(R.string.cloud_theme_creator_custom_category_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = creatorMutedColor(),
                    )
                }
            }
        }
        item {
            CreatorVersionCard(draft = draft, onDraftChange = onDraftChange)
        }
        item {
            CreatorMediaCard(draft = draft, onDraftChange = onDraftChange)
        }
        item {
            CreatorRemotePackageCard(
                draft = draft,
                verifying = remoteVerifying,
                onDraftChange = onDraftChange,
                onVerify = onVerifyRemote,
            )
        }
        item {
            CreatorSurface {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = !packageLoading,
                            onClick = onSaveDraft,
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.cloud_theme_creator_save_draft))
                        }
                        TextButton(
                            enabled = !packageLoading,
                            onClick = onClearDraft,
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(stringResource(R.string.cloud_theme_creator_clear_draft_action))
                        }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = draft.isRemoteVerified && !remoteVerifying && !packageLoading,
                        onClick = onSubmit,
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.cloud_theme_creator_submit_review))
                    }
                    Text(
                        text = stringResource(
                            R.string.cloud_theme_creator_submit_review_notice,
                            CLOUD_THEME_CREATOR_REVIEWER,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = creatorMutedColor(),
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CreatorPackageCard(
    draft: CloudThemeSubmissionDraft,
    loading: Boolean,
    onSelectPackage: () -> Unit,
    onCreatePackage: () -> Unit,
    onExportPackage: () -> Unit,
) {
    CreatorSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CreatorSectionTitle(
                icon = Icons.AutoMirrored.Rounded.InsertDriveFile,
                title = stringResource(R.string.cloud_theme_creator_local_package),
            )
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (draft.hasInspectedPackage) {
                Text(
                    text = draft.packageName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = creatorTextColor(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.cloud_theme_creator_package_meta,
                        formatCreatorBytes(draft.packageSizeBytes),
                        draft.packageVersion,
                        draft.packageResourceCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = creatorMutedColor(),
                )
                Text(
                    text = "SHA-256  ${draft.packageSha256}",
                    style = MaterialTheme.typography.labelSmall,
                    color = creatorMutedColor(),
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    onClick = onExportPackage,
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.cloud_theme_creator_export_package))
                }
            } else {
                Text(
                    text = stringResource(R.string.cloud_theme_creator_no_package),
                    style = MaterialTheme.typography.bodyMedium,
                    color = creatorMutedColor(),
                )
            }
            Text(
                text = stringResource(R.string.cloud_theme_creator_package_rules),
                style = MaterialTheme.typography.bodySmall,
                color = creatorMutedColor(),
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                onClick = onCreatePackage,
            ) {
                Icon(Icons.Rounded.Drafts, contentDescription = null)
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.cloud_theme_creator_create_package))
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                onClick = onSelectPackage,
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Rounded.InsertDriveFile, contentDescription = null)
                }
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.cloud_theme_creator_choose_package))
            }
        }
    }
}

@Composable
private fun CreatorVersionCard(
    draft: CloudThemeSubmissionDraft,
    onDraftChange: (CloudThemeSubmissionDraft) -> Unit,
) {
    CreatorSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CreatorSectionTitle(
                icon = Icons.AutoMirrored.Rounded.Rule,
                title = stringResource(R.string.cloud_theme_creator_version_title),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = draft.versionName,
                    onValueChange = { onDraftChange(draft.copy(versionName = it.take(40))) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.cloud_theme_creator_version_name)) },
                )
                OutlinedTextField(
                    value = draft.versionCodeText,
                    onValueChange = {
                        onDraftChange(draft.copy(versionCodeText = it.filter(Char::isDigit).take(18)))
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.cloud_theme_creator_version_code)) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = draft.minManagerVersionCodeText,
                    onValueChange = {
                        onDraftChange(
                            draft.copy(
                                minManagerVersionCodeText = it.filter(Char::isDigit).take(18)
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.cloud_theme_creator_min_manager)) },
                )
                OutlinedTextField(
                    value = draft.maxManagerVersionCodeText,
                    onValueChange = {
                        onDraftChange(
                            draft.copy(
                                maxManagerVersionCodeText = it.filter(Char::isDigit).take(18)
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.cloud_theme_creator_max_manager)) },
                )
            }
            OutlinedTextField(
                value = draft.license,
                onValueChange = { onDraftChange(draft.copy(license = it.take(48))) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.cloud_theme_creator_license)) },
            )
            OutlinedTextField(
                value = draft.changelog,
                onValueChange = { onDraftChange(draft.copy(changelog = it.take(4000))) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 6,
                label = { Text(stringResource(R.string.cloud_theme_creator_changelog)) },
            )
        }
    }
}

@Composable
private fun CreatorMediaCard(
    draft: CloudThemeSubmissionDraft,
    onDraftChange: (CloudThemeSubmissionDraft) -> Unit,
) {
    CreatorSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CreatorSectionTitle(
                icon = Icons.Rounded.Description,
                title = stringResource(R.string.cloud_theme_creator_media_title),
            )
            OutlinedTextField(
                value = draft.coverUrl,
                onValueChange = { onDraftChange(draft.copy(coverUrl = it.take(768))) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.cloud_theme_creator_cover_url)) },
            )
            OutlinedTextField(
                value = draft.screenshotUrlsText,
                onValueChange = {
                    onDraftChange(draft.copy(screenshotUrlsText = it.take(8 * 768 + 8)))
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
                label = { Text(stringResource(R.string.cloud_theme_creator_screenshot_urls)) },
                supportingText = {
                    Text(stringResource(R.string.cloud_theme_creator_screenshot_urls_hint))
                },
            )
        }
    }
}

@Composable
private fun CreatorRemotePackageCard(
    draft: CloudThemeSubmissionDraft,
    verifying: Boolean,
    onDraftChange: (CloudThemeSubmissionDraft) -> Unit,
    onVerify: () -> Unit,
) {
    CreatorSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CreatorSectionTitle(
                icon = Icons.Rounded.CloudUpload,
                title = stringResource(R.string.cloud_theme_creator_remote_package),
            )
            OutlinedTextField(
                value = draft.packageUrl,
                onValueChange = {
                    onDraftChange(
                        draft.copy(packageUrl = it.take(768)).invalidateRemoteVerification()
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.cloud_theme_creator_package_url)) },
                supportingText = {
                    Text(stringResource(R.string.cloud_theme_creator_package_url_hint))
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = if (draft.isRemoteVerified) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.WarningAmber
                    },
                    contentDescription = null,
                    tint = if (draft.isRemoteVerified) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                )
                Text(
                    text = stringResource(
                        if (draft.isRemoteVerified) {
                            R.string.cloud_theme_creator_remote_match
                        } else {
                            R.string.cloud_theme_creator_remote_unverified
                        }
                    ),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = creatorMutedColor(),
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = draft.hasInspectedPackage && draft.packageUrl.isNotBlank() && !verifying,
                onClick = onVerify,
            ) {
                if (verifying) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                }
                Spacer(modifier = Modifier.size(7.dp))
                Text(stringResource(R.string.cloud_theme_creator_verify_remote))
            }
        }
    }
}

@Composable
private fun CreatorLockedSubmission(onShowQualification: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CreatorSurface {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CreatorIcon(Icons.Rounded.Lock, size = 52)
                Text(
                    text = stringResource(R.string.cloud_theme_creator_submission_locked),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = creatorTextColor(),
                )
                Text(
                    text = stringResource(R.string.cloud_theme_creator_submission_locked_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = creatorMutedColor(),
                )
                Button(onClick = onShowQualification) {
                    Text(stringResource(R.string.cloud_theme_creator_go_qualification))
                }
            }
        }
    }
}

@Composable
private fun CreatorRecordsPage(
    githubLogin: String,
    approved: Boolean,
    applicationStatus: CloudThemeCreatorApplicationStatus,
    activity: CloudThemeCreatorActivity?,
    loading: Boolean,
    onRefresh: () -> Unit,
    onOpenIssue: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            CreatorSurface {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CreatorIcon(Icons.Rounded.History)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = githubLogin.ifBlank {
                                stringResource(R.string.cloud_theme_creator_no_github_login)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = creatorTextColor(),
                        )
                        Text(
                            text = stringResource(
                                if (approved) {
                                    R.string.cloud_theme_creator_records_approved
                                } else {
                                    creatorApplicationStatusLabel(applicationStatus)
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = creatorMutedColor(),
                        )
                    }
                    MiuixIconButton(
                        enabled = !loading && isValidCloudThemeGithubLogin(githubLogin),
                        onClick = onRefresh,
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(19.dp), strokeWidth = 2.dp)
                        } else {
                            MiuixIcon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(
                                    R.string.cloud_theme_creator_check_status
                                ),
                            )
                        }
                    }
                }
            }
        }
        if (loading && activity == null) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        val submissions = activity?.submissions.orEmpty()
        if (!loading && submissions.isEmpty()) {
            item {
                CreatorEmptyRecords()
            }
        } else {
            items(submissions, key = CloudThemeSubmissionReview::issueNumber) { review ->
                CreatorReviewCard(review = review, onClick = { onOpenIssue(review.url) })
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CreatorReviewCard(review: CloudThemeSubmissionReview, onClick: () -> Unit) {
    val statusRes = when (review.status) {
        CloudThemeSubmissionReviewStatus.Pending -> R.string.cloud_theme_creator_review_pending
        CloudThemeSubmissionReviewStatus.Approved -> R.string.cloud_theme_creator_review_approved
        CloudThemeSubmissionReviewStatus.NeedsChanges -> R.string.cloud_theme_creator_review_changes
        CloudThemeSubmissionReviewStatus.Rejected -> R.string.cloud_theme_creator_review_rejected
        CloudThemeSubmissionReviewStatus.Published -> R.string.cloud_theme_creator_review_published
    }
    val tint = when (review.status) {
        CloudThemeSubmissionReviewStatus.Published -> MaterialTheme.colorScheme.primary
        CloudThemeSubmissionReviewStatus.Approved -> MaterialTheme.colorScheme.secondary
        CloudThemeSubmissionReviewStatus.NeedsChanges -> MaterialTheme.colorScheme.tertiary
        CloudThemeSubmissionReviewStatus.Rejected -> MaterialTheme.colorScheme.error
        CloudThemeSubmissionReviewStatus.Pending -> creatorMutedColor()
    }
    CreatorSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(tint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when (review.status) {
                        CloudThemeSubmissionReviewStatus.Published -> Icons.Rounded.CheckCircle
                        CloudThemeSubmissionReviewStatus.Rejected -> Icons.Rounded.ErrorOutline
                        CloudThemeSubmissionReviewStatus.NeedsChanges -> Icons.Rounded.WarningAmber
                        else -> Icons.Rounded.History
                    },
                    contentDescription = null,
                    tint = tint,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = review.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = creatorTextColor(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.cloud_theme_creator_review_meta,
                        review.issueNumber,
                        review.updatedAt.substringBefore('T'),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = creatorMutedColor(),
                )
                Text(
                    text = stringResource(statusRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = creatorMutedColor(),
            )
        }
    }
}

@Composable
private fun CreatorEmptyRecords() {
    CreatorSurface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CreatorIcon(Icons.Rounded.History, size = 50)
            Text(
                text = stringResource(R.string.cloud_theme_creator_records_empty),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = creatorTextColor(),
            )
            Text(
                text = stringResource(R.string.cloud_theme_creator_records_empty_summary),
                style = MaterialTheme.typography.bodySmall,
                color = creatorMutedColor(),
            )
        }
    }
}

@Composable
private fun CreatorStatusCard(
    status: CloudThemeCreatorApplicationStatus,
    loading: Boolean,
    offline: Boolean,
) {
    val icon = when (status) {
        CloudThemeCreatorApplicationStatus.Approved -> Icons.Rounded.VerifiedUser
        CloudThemeCreatorApplicationStatus.Pending -> Icons.Rounded.History
        CloudThemeCreatorApplicationStatus.NeedsChanges -> Icons.Rounded.WarningAmber
        CloudThemeCreatorApplicationStatus.Rejected -> Icons.Rounded.ErrorOutline
        CloudThemeCreatorApplicationStatus.RegistryPending -> Icons.Rounded.Refresh
        CloudThemeCreatorApplicationStatus.NotApplied -> Icons.Rounded.Person
    }
    val tint = when (status) {
        CloudThemeCreatorApplicationStatus.Approved -> MaterialTheme.colorScheme.primary
        CloudThemeCreatorApplicationStatus.Pending -> MaterialTheme.colorScheme.secondary
        CloudThemeCreatorApplicationStatus.NeedsChanges -> MaterialTheme.colorScheme.tertiary
        CloudThemeCreatorApplicationStatus.Rejected -> MaterialTheme.colorScheme.error
        CloudThemeCreatorApplicationStatus.RegistryPending -> MaterialTheme.colorScheme.tertiary
        CloudThemeCreatorApplicationStatus.NotApplied -> creatorMutedColor()
    }
    CreatorSurface {
        Column {
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(tint.copy(alpha = 0.13f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = stringResource(creatorApplicationStatusLabel(status)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = creatorTextColor(),
                    )
                    Text(
                        text = stringResource(creatorApplicationStatusSummary(status)),
                        style = MaterialTheme.typography.bodySmall,
                        color = creatorMutedColor(),
                    )
                    if (offline) {
                        Text(
                            text = stringResource(R.string.cloud_theme_creator_registry_offline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}

@StringRes
private fun creatorApplicationStatusLabel(status: CloudThemeCreatorApplicationStatus): Int =
    when (status) {
        CloudThemeCreatorApplicationStatus.NotApplied -> R.string.cloud_theme_creator_status_not_applied
        CloudThemeCreatorApplicationStatus.Pending -> R.string.cloud_theme_creator_status_pending
        CloudThemeCreatorApplicationStatus.NeedsChanges -> R.string.cloud_theme_creator_status_changes
        CloudThemeCreatorApplicationStatus.Rejected -> R.string.cloud_theme_creator_status_rejected
        CloudThemeCreatorApplicationStatus.RegistryPending -> R.string.cloud_theme_creator_status_registry_pending
        CloudThemeCreatorApplicationStatus.Approved -> R.string.cloud_theme_creator_status_approved
    }

@StringRes
private fun creatorApplicationStatusSummary(status: CloudThemeCreatorApplicationStatus): Int =
    when (status) {
        CloudThemeCreatorApplicationStatus.NotApplied -> R.string.cloud_theme_creator_status_not_applied_summary
        CloudThemeCreatorApplicationStatus.Pending -> R.string.cloud_theme_creator_status_pending_summary
        CloudThemeCreatorApplicationStatus.NeedsChanges -> R.string.cloud_theme_creator_status_changes_summary
        CloudThemeCreatorApplicationStatus.Rejected -> R.string.cloud_theme_creator_status_rejected_summary
        CloudThemeCreatorApplicationStatus.RegistryPending -> {
            R.string.cloud_theme_creator_status_registry_pending_summary
        }
        CloudThemeCreatorApplicationStatus.Approved -> R.string.cloud_theme_creator_status_approved_summary
    }

@Composable
private fun CreatorSectionTitle(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CreatorIcon(icon)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = creatorTextColor(),
        )
    }
}

@Composable
private fun CreatorIcon(icon: ImageVector, size: Int = 42) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size((size * 0.5f).dp),
        )
    }
}

@Composable
private fun CreatorSurface(content: @Composable () -> Unit) {
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
private fun creatorTextColor(): Color =
    if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproColors.Text
    } else {
        MaterialTheme.colorScheme.onSurface
    }

@Composable
private fun creatorMutedColor(): Color =
    if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproColors.Muted
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
private fun CreatorBackButton(onClick: () -> Unit) {
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
            contentDescription = stringResource(R.string.close),
            tint = Color.White,
        )
    }
}

private fun formatCreatorBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024.0) return "%.1f KiB".format(kib)
    return "%.1f MiB".format(kib / 1024.0)
}
