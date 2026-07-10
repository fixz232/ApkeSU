package io.github.fixz.apkesu.gkibuilder

import android.app.Activity
import android.content.ContentValues
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipFile
import android.util.Base64

class MainActivity : Activity() {
    private lateinit var store: FormStore
    private var form = BuilderForm()
    private var lastRunUrl = ""
    private var currentPage = PAGE_HOME
    private var pendingOAuthState = ""

    private lateinit var scrollView: ScrollView
    private lateinit var pageBody: LinearLayout
    private lateinit var navBar: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var openRunButton: Button

    private lateinit var tokenInput: EditText
    private lateinit var clientIdInput: EditText
    private lateinit var ownerInput: EditText
    private lateinit var repoInput: EditText
    private lateinit var refInput: EditText
    private lateinit var workflowInput: EditText
    private lateinit var buildTimeInput: EditText

    private lateinit var androidSpinner: Spinner
    private lateinit var kernelSpinner: Spinner
    private lateinit var sublevelSpinner: Spinner
    private lateinit var patchSpinner: Spinner
    private lateinit var featureSpinner: Spinner
    private lateinit var channelSpinner: Spinner
    private lateinit var virtualizationSpinner: Spinner
    private lateinit var oplusSpinner: Spinner

    private lateinit var repoSyncSwitch: Switch
    private lateinit var bypassSwitch: Switch
    private lateinit var zramSwitch: Switch
    private lateinit var ntsyncSwitch: Switch
    private lateinit var networkingSwitch: Switch
    private lateinit var latestSusfsSwitch: Switch
    private lateinit var oauthStatusText: TextView
    private lateinit var diagnosticStatusText: TextView
    private var selectedHistory: BuildHistory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = FormStore(this)
        form = store.read().asSimpleDefault()
        migrateStoredHistory()
        lastRunUrl = historyPrefs().getString(KEY_LAST_RUN_URL, "").orEmpty()
        setContentView(createShell())
        if (!handleOAuthIntent(intent)) {
            showPage(PAGE_HOME, syncBeforeRender = false)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun createShell(): View {
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
        }
        scrollView = ScrollView(this).apply {
            isFillViewport = false
            setBackgroundColor(BG)
        }
        pageBody = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(18))
        }
        scrollView.addView(pageBody, ViewGroup.LayoutParams(match(), wrap()))
        shell.addView(scrollView, LinearLayout.LayoutParams(match(), 0, 1f))

        navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(8))
            background = solid(NAV, 0)
        }
        shell.addView(navBar, LinearLayout.LayoutParams(match(), dp(74)))
        return shell
    }

    private fun showPage(
        page: Int,
        syncBeforeRender: Boolean = true,
    ) {
        if (syncBeforeRender) {
            syncFormFromUi()
        }
        currentPage = page
        pageBody.removeAllViews()
        when (page) {
            PAGE_HOME -> renderHome()
            PAGE_BUILD -> renderBuild()
            PAGE_RECORDS -> renderRecords()
            PAGE_HISTORY_DETAIL -> renderHistoryDetail()
            PAGE_TOOLS -> renderTools()
            PAGE_SETTINGS -> renderSettings()
        }
        renderNav()
        scrollView.post { scrollView.scrollTo(0, 0) }
    }

    private fun renderHome() {
        pageBody.addView(pageTitle(getString(R.string.home_title)))
        pageBody.addView(heroCard {
            addView(title(getString(R.string.home_ready), 23, Color.WHITE))
            addView(muted(getString(R.string.home_build_label, form.androidVersion, form.kernelVersion), HERO_TEXT))
            addView(chipRow(listOf(accountChip(), getString(R.string.chip_gki_flow), form.featureSet)))
        })

        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val firstRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(statusTile(getString(R.string.status_root), accountStatus()), weightedGrid())
            addView(statusTile(getString(R.string.status_fork), repoName()), weightedGrid())
        }
        val secondRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(statusTile(getString(R.string.status_kernel), kernelStatus()), weightedGrid())
            addView(statusTile(getString(R.string.status_build), lastBuildStatus()), weightedGrid())
        }
        grid.addView(firstRow)
        grid.addView(secondRow)
        pageBody.addView(grid)

        pageBody.addView(card {
            addView(title(getString(R.string.recent_kernel_build), 18))
            addView(muted(getString(R.string.recent_summary)))
            addView(progressBar(100))
            addView(muted(getString(R.string.target_summary, form.androidVersion, form.kernelVersion, form.osPatchLevel, form.featureSet)))
            addView(actionRow(
                primaryButton(getString(R.string.build_kernel)) { showPage(PAGE_BUILD) },
                secondaryButton(getString(R.string.view_last_run)) { openUrl(lastRunUrl.ifBlank { GkiBuilderApi.workflowUrl(form) }) },
            ))
        })

        pageBody.addView(card {
            addView(title(getString(R.string.device_repo), 18))
            addView(infoRow(getString(R.string.kernel_version), "${form.kernelVersion}.${form.sublevel}"))
            addView(infoRow(getString(R.string.feature_set), form.featureSet))
            addView(infoRow(getString(R.string.repository), repoName()))
        })

        pageBody.addView(historyPreviewCard())
    }

    private fun renderBuild() {
        pageBody.addView(pageTitle(getString(R.string.build_kernel)))
        pageBody.addView(heroCard {
            addView(title("${form.kernelVersion}.${form.sublevel} - ${form.androidVersion}", 21, Color.WHITE))
            addView(muted(getString(R.string.target_card_summary, form.osPatchLevel), HERO_TEXT))
            addView(chipRow(listOf(form.featureSet, if (form.useRepo) "repo" else "archive", channelLabel())))
        })

        if (form.token.isBlank()) {
            pageBody.addView(card {
                addView(title(getString(R.string.account), 18))
                tokenInput = editText(getString(R.string.github_token), password = true)
                tokenInput.setText(form.token)
                addView(tokenInput)
                addView(muted(getString(R.string.token_required_short)))
            })
        }

        pageBody.addView(card {
            addView(title(getString(R.string.build_target), 18))
            addView(muted(getString(R.string.build_scheme_summary)))
            addView(chipRow(listOf(getString(R.string.target_gki), getString(R.string.target_oneplus))))
        })

        pageBody.addView(card {
            addView(title(getString(R.string.recent_build), 18))
            progress = ProgressBar(this@MainActivity).apply {
                visibility = View.GONE
                isIndeterminate = true
            }
            addView(progress, matchWrap())
            statusText = muted(lastBuildStatus())
            addView(statusText)
            openRunButton = secondaryButton(getString(R.string.open_run)) {
                openUrl(lastRunUrl)
            }.apply {
                isEnabled = lastRunUrl.isNotBlank()
            }
            addView(openRunButton, matchWrap())
        })

        pageBody.addView(card {
            addView(title(getString(R.string.kernel_version_config), 18))
            androidSpinner = addSpinnerField(getString(R.string.android_version), ANDROID_OPTIONS)
            kernelSpinner = addSpinnerField(getString(R.string.kernel_version), KERNEL_OPTIONS)
            sublevelSpinner = addSpinnerField(getString(R.string.kernel_sublevel), SUBLEVEL_OPTIONS)
            patchSpinner = addSpinnerField(getString(R.string.os_patch_level), PATCH_OPTIONS)
            bindTargetSpinners()
        })

        pageBody.addView(card {
            addView(title(getString(R.string.apkesu_config), 18))
            featureSpinner = addSpinnerField(getString(R.string.apkesu_variant), FEATURE_OPTIONS)
            channelSpinner = addSpinnerField(getString(R.string.update_channel), CHANNEL_OPTIONS)
            latestSusfsSwitch = addSwitchField(getString(R.string.use_latest_susfs), getString(R.string.use_latest_susfs_summary))
            bindFeatureControls()
        })

        pageBody.addView(card {
            addView(title(getString(R.string.feature_switches), 18))
            repoSyncSwitch = addSwitchField(getString(R.string.use_repo_sync), getString(R.string.use_repo_sync_summary))
            zramSwitch = addSwitchField(getString(R.string.enable_zram_bundle), null)
            ntsyncSwitch = addSwitchField(getString(R.string.enable_ntsync), null)
            networkingSwitch = addSwitchField(getString(R.string.enable_networking_configs), null)
            bypassSwitch = addSwitchField(getString(R.string.build_bypass_image), null)
            virtualizationSpinner = addSpinnerField(getString(R.string.virtualization_support), VIRTUALIZATION_OPTIONS)
            oplusSpinner = addSpinnerField(getString(R.string.oplus_patch_mode), OPLUS_OPTIONS)
            bindFeatureSwitches()
        })

        pageBody.addView(card {
            addView(title(getString(R.string.optional_config), 18))
            buildTimeInput = editText(getString(R.string.custom_build_time))
            buildTimeInput.setText(form.buildTime)
            addView(buildTimeInput)
        })

        pageBody.addView(primaryButton(getString(R.string.submit_build)) { startBuild() }, matchWrapWithMargins(top = 8, bottom = 12))
    }

    private fun renderRecords() {
        pageBody.addView(pageTitle(getString(R.string.records)))
        pageBody.addView(historyPreviewCard())
        pageBody.addView(card {
            addView(title(getString(R.string.quick_actions), 18))
            addView(actionRow(
                primaryButton(getString(R.string.open_actions)) {
                    syncFormFromUi()
                    openUrl(GkiBuilderApi.workflowUrl(form))
                },
                secondaryButton(getString(R.string.open_repository)) {
                    syncFormFromUi()
                    openUrl("https://github.com/${form.owner}/${form.repo}")
                },
            ))
            addView(secondaryButton(getString(R.string.clear_history)) {
                historyPrefs().edit().remove(KEY_HISTORY).remove(KEY_LAST_RUN_URL).remove(KEY_LAST_STATUS).apply()
                lastRunUrl = ""
                selectedHistory = null
                showPage(PAGE_RECORDS)
            }, matchWrap())
        })
    }

    private fun renderTools() {
        pageBody.addView(pageTitle(getString(R.string.quick_actions)))
        pageBody.addView(card {
            addView(title(getString(R.string.workflow_progress), 18))
            addView(muted(getString(R.string.workflow_progress_summary)))
            diagnosticStatusText = muted(getString(R.string.diagnostic_not_run))
            addView(diagnosticStatusText)
            addView(primaryButton(getString(R.string.run_diagnostics)) {
                runDiagnostics()
            }, matchWrap())
            addView(secondaryButton(getString(R.string.install_workflow_bundle)) {
                installWorkflowBundle()
            }, matchWrap())
            addView(primaryButton(getString(R.string.open_actions)) {
                syncFormFromUi()
                openUrl(GkiBuilderApi.workflowUrl(form))
            }, matchWrap())
            addView(secondaryButton(getString(R.string.open_repository)) {
                syncFormFromUi()
                openUrl("https://github.com/${form.owner}/${form.repo}")
            }, matchWrap())
        })
        pageBody.addView(card {
            addView(title(getString(R.string.restore_defaults), 18))
            addView(muted(getString(R.string.restore_defaults_summary)))
            addView(secondaryButton(getString(R.string.restore_defaults)) {
                form = BuilderForm().asSimpleDefault()
                store.save(form)
                showPage(PAGE_HOME)
            }, matchWrap())
        })
    }

    private fun renderSettings() {
        pageBody.addView(pageTitle(getString(R.string.settings)))
        pageBody.addView(card {
            addView(title(getString(R.string.account), 18))
            addView(muted(getString(R.string.github_login_summary)))
            clientIdInput = editText(getString(R.string.github_client_id))
            tokenInput = editText(getString(R.string.github_token), password = true)
            ownerInput = editText(getString(R.string.github_owner))
            repoInput = editText(getString(R.string.repository_name))
            refInput = editText(getString(R.string.branch_or_tag))
            workflowInput = editText(getString(R.string.workflow_file))
            clientIdInput.setText(form.githubClientId)
            tokenInput.setText(form.token)
            ownerInput.setText(form.owner)
            repoInput.setText(form.repo)
            refInput.setText(form.ref)
            workflowInput.setText(form.workflowFile)
            addView(clientIdInput)
            oauthStatusText = muted(oauthStatus())
            addView(oauthStatusText)
            addView(primaryButton(getString(R.string.github_login)) {
                startGithubDeviceLogin()
            }, matchWrap())
            addView(secondaryButton(getString(R.string.github_browser_login)) {
                startGithubWebLogin()
            }, matchWrap())
            addView(secondaryButton(getString(R.string.load_repositories)) {
                loadRepositories()
            }, matchWrap())
            addView(tokenInput)
            addView(ownerInput)
            addView(repoInput)
            addView(refInput)
            addView(workflowInput)
            addView(secondaryButton(getString(R.string.check_workflow)) {
                checkWorkflow()
            }, matchWrap())
            addView(primaryButton(getString(R.string.save_settings)) {
                syncFormFromUi()
                showStatus(getString(R.string.settings_saved), false)
            }, matchWrap())
        })
        pageBody.addView(card {
            addView(title(getString(R.string.download_after_build), 18))
            addView(muted(getString(R.string.download_after_build_summary)))
        })
        pageBody.addView(card {
            addView(title(getString(R.string.about), 18))
            addView(infoRow(getString(R.string.app_name), getString(R.string.app_version_info)))
            addView(infoRow(getString(R.string.repository), repoName()))
        })
    }

    private fun renderNav() {
        navBar.removeAllViews()
        NAV_ITEMS.forEach { (page, labelRes) ->
            val selected = page == currentPage
            navBar.addView(TextView(this).apply {
                text = getString(labelRes)
                gravity = Gravity.CENTER
                textSize = 11f
                typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setTextColor(if (selected) Color.WHITE else MUTED)
                background = if (selected) solid(ACCENT, 28) else null
                setOnClickListener { showPage(page) }
            }, LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                setMargins(dp(2), 0, dp(2), 0)
            })
        }
    }

    private fun bindTargetSpinners() {
        setSpinner(androidSpinner, form.androidVersion)
        setSpinner(kernelSpinner, form.kernelVersion)
        setSpinner(sublevelSpinner, form.sublevel)
        setSpinner(patchSpinner, form.osPatchLevel)
    }

    private fun bindFeatureControls() {
        setSpinner(featureSpinner, form.featureSet)
        setSpinner(channelSpinner, channelLabel())
        latestSusfsSwitch.isChecked = form.useLatestSusfs
    }

    private fun bindFeatureSwitches() {
        repoSyncSwitch.isChecked = form.useRepo
        bypassSwitch.isChecked = form.buildBypass
        zramSwitch.isChecked = form.useZram
        ntsyncSwitch.isChecked = form.useNtsync
        networkingSwitch.isChecked = form.useNetworking
        setSpinner(virtualizationSpinner, form.virtualizationSupport)
        setSpinner(oplusSpinner, form.oplusPatchMode)
    }

    private fun syncFormFromUi() {
        val channel = if (::channelSpinner.isInitialized) {
            selected(channelSpinner, channelLabel())
        } else {
            channelLabel()
        }
        val useLatest = when {
            ::latestSusfsSwitch.isInitialized && isSwitchReady(latestSusfsSwitch) -> latestSusfsSwitch.isChecked
            channel == CHANNEL_LATEST -> true
            channel == CHANNEL_STABLE -> false
            else -> form.useLatestSusfs
        }
        form = form.copy(
            token = if (::tokenInput.isInitialized) textOf(tokenInput, form.token) else form.token,
            githubClientId = if (::clientIdInput.isInitialized) textOf(clientIdInput, form.githubClientId) else form.githubClientId,
            owner = if (::ownerInput.isInitialized) textOf(ownerInput, form.owner) else form.owner,
            repo = if (::repoInput.isInitialized) textOf(repoInput, form.repo) else form.repo,
            ref = if (::refInput.isInitialized) textOf(refInput, form.ref) else form.ref,
            workflowFile = if (::workflowInput.isInitialized) textOf(workflowInput, form.workflowFile) else form.workflowFile,
            androidVersion = if (::androidSpinner.isInitialized) selected(androidSpinner, form.androidVersion) else form.androidVersion,
            kernelVersion = if (::kernelSpinner.isInitialized) selected(kernelSpinner, form.kernelVersion) else form.kernelVersion,
            sublevel = if (::sublevelSpinner.isInitialized) selected(sublevelSpinner, form.sublevel) else form.sublevel,
            osPatchLevel = if (::patchSpinner.isInitialized) selected(patchSpinner, form.osPatchLevel) else form.osPatchLevel,
            featureSet = if (::featureSpinner.isInitialized) selected(featureSpinner, form.featureSet) else form.featureSet,
            useRepo = if (::repoSyncSwitch.isInitialized) checked(repoSyncSwitch, form.useRepo) else form.useRepo,
            useLatestSusfs = useLatest,
            buildBypass = if (::bypassSwitch.isInitialized) checked(bypassSwitch, form.buildBypass) else form.buildBypass,
            buildTime = if (::buildTimeInput.isInitialized) textOf(buildTimeInput, form.buildTime) else form.buildTime,
            useZram = if (::zramSwitch.isInitialized) checked(zramSwitch, form.useZram) else form.useZram,
            useNtsync = if (::ntsyncSwitch.isInitialized) checked(ntsyncSwitch, form.useNtsync) else form.useNtsync,
            useNetworking = if (::networkingSwitch.isInitialized) checked(networkingSwitch, form.useNetworking) else form.useNetworking,
            virtualizationSupport = if (::virtualizationSpinner.isInitialized) {
                selected(virtualizationSpinner, form.virtualizationSupport)
            } else {
                form.virtualizationSupport
            },
            oplusPatchMode = if (::oplusSpinner.isInitialized) selected(oplusSpinner, form.oplusPatchMode) else form.oplusPatchMode,
        )
        store.save(form)
    }

    private fun startBuild() {
        syncFormFromUi()
        val error = form.validationError()
        if (error != null) {
            showStatus(getString(error), true)
            return
        }
        setBusy(true)
        showStatus(getString(R.string.status_dispatching), false)
        val submitForm = form
        Thread {
            val buildResult = runCatching {
                val install = GkiBuilderApi.ensureGkiWorkflowBundle(submitForm, loadGkiWorkflowAssets())
                val buildForm = GkiBuilderApi.prepareBuildForm(install.form)
                buildForm to GkiBuilderApi.dispatch(buildForm)
            }
            runOnUiThread {
                buildResult
                    .onSuccess { (buildForm, result) ->
                        if (buildForm.owner != form.owner ||
                            buildForm.repo != form.repo ||
                            buildForm.ref != form.ref ||
                            buildForm.workflowFile != form.workflowFile
                        ) {
                            form = buildForm
                            store.save(form)
                            if (::ownerInput.isInitialized && ownerInput.isAttachedToWindow) ownerInput.setText(buildForm.owner)
                            if (::repoInput.isInitialized && repoInput.isAttachedToWindow) repoInput.setText(buildForm.repo)
                            if (::refInput.isInitialized && refInput.isAttachedToWindow) refInput.setText(buildForm.ref)
                            if (::workflowInput.isInitialized && workflowInput.isAttachedToWindow) {
                                workflowInput.setText(buildForm.workflowFile)
                            }
                        }
                        lastRunUrl = result.htmlUrl
                        openRunButtonOrNull()?.isEnabled = true
                        val status = result.status ?: getString(R.string.queued)
                        val historyItem = BuildHistory(
                            stamp = nowStamp(),
                            status = status,
                            target = targetLine(),
                            url = result.htmlUrl,
                            owner = buildForm.owner,
                            repo = buildForm.repo,
                            runId = result.runId ?: 0L,
                            conclusion = "",
                            artifacts = "",
                        )
                        appendHistory(historyItem)
                        watchRunProgress(historyItem)
                        showStatus(
                            getString(
                                R.string.status_started_with_target,
                                result.status ?: getString(R.string.queued),
                                "${buildForm.owner}/${buildForm.repo}",
                                buildForm.workflowFile,
                            ),
                            false,
                        )
                    }
                    .onFailure { error ->
                        val message = error.userVisibleMessage()
                        appendHistory(
                            BuildHistory(
                                stamp = nowStamp(),
                                status = getString(R.string.build_failure),
                                target = targetLine(),
                                url = "",
                                owner = submitForm.owner,
                                repo = submitForm.repo,
                                runId = 0L,
                                conclusion = message,
                                artifacts = "",
                            ),
                        )
                        showStatus(getString(R.string.status_failed, message), true)
                    }
                setBusy(false)
            }
        }.start()
    }

    private fun loadRepositories() {
        syncFormFromUi()
        if (form.token.isBlank()) {
            showOAuthStatus(getString(R.string.error_token), true)
            return
        }
        showOAuthStatus(getString(R.string.loading_repositories), false)
        Thread {
            val result = runCatching { GkiBuilderApi.listRepositories(form.token) }
            runOnUiThread {
                result
                    .onSuccess { repos ->
                        renderRepositoryPicker(repos)
                        showOAuthStatus(getString(R.string.repositories_loaded, repos.size), false)
                    }
                    .onFailure { error ->
                        showOAuthStatus(getString(R.string.repositories_load_failed, error.message ?: error.javaClass.simpleName), true)
                    }
            }
        }.start()
    }

    private fun renderRepositoryPicker(repositories: List<RepositoryInfo>) {
        if (currentPage != PAGE_SETTINGS) {
            showPage(PAGE_SETTINGS, syncBeforeRender = false)
        }
        pageBody.addView(card {
            addView(title(getString(R.string.select_repository), 18))
            if (repositories.isEmpty()) {
                addView(muted(getString(R.string.no_repositories)))
            } else {
                repositories.take(20).forEach { repo ->
                    addView(secondaryButton(repo.fullName) {
                        form = form.copy(
                            owner = repo.owner,
                            repo = repo.name,
                            ref = repo.defaultBranch,
                        )
                        store.save(form)
                        if (::ownerInput.isInitialized && ownerInput.isAttachedToWindow) ownerInput.setText(repo.owner)
                        if (::repoInput.isInitialized && repoInput.isAttachedToWindow) repoInput.setText(repo.name)
                        if (::refInput.isInitialized && refInput.isAttachedToWindow) refInput.setText(repo.defaultBranch)
                        showOAuthStatus(getString(R.string.repository_selected, repo.fullName), false)
                    }, matchWrap())
                }
            }
        })
    }

    private fun runDiagnostics() {
        syncFormFromUi()
        val error = form.validationError(requireToken = true, allowMissingTarget = true)
        if (error != null) {
            showDiagnosticStatus(getString(error), true)
            return
        }
        val target = form
        showDiagnosticStatus(getString(R.string.diagnostic_running), false)
        Thread {
            val result = runCatching {
                val lines = mutableListOf<String>()
                val user = GkiBuilderApi.getAuthenticatedUser(target.token)
                lines += getString(R.string.diagnostic_account_ok, user.login)

                val access = GkiBuilderApi.getRepositoryAccess(target)
                if (access == null) {
                    lines += getString(R.string.diagnostic_repo_missing, "${target.owner}/${target.repo}")
                } else {
                    lines += getString(R.string.diagnostic_repo_ok, access.fullName)
                    lines += if (access.canBuild) {
                        getString(R.string.diagnostic_permissions_ok)
                    } else {
                        getString(R.string.diagnostic_permissions_need_fork)
                    }
                }

                lines += if (GkiBuilderApi.refExists(target)) {
                    getString(R.string.diagnostic_ref_ok, target.ref)
                } else {
                    getString(R.string.diagnostic_ref_missing, target.ref)
                }

                val workflowFileExists = GkiBuilderApi.workflowFileExists(target)
                lines += if (workflowFileExists) {
                    getString(R.string.diagnostic_workflow_file_ok, target.workflowFile)
                } else {
                    getString(R.string.diagnostic_workflow_file_missing, target.workflowFile)
                }

                val workflows = GkiBuilderApi.listWorkflows(target)
                lines += getString(R.string.diagnostic_workflow_count, workflows.size)
                lines.joinToString("\n")
            }
            runOnUiThread {
                result
                    .onSuccess { text ->
                        showDiagnosticStatus(text, false)
                        copyToClipboard(getString(R.string.run_diagnostics), text)
                    }
                    .onFailure { error -> showDiagnosticStatus(error.userVisibleMessage(), true) }
            }
        }.start()
    }

    private fun installWorkflowBundle() {
        syncFormFromUi()
        val error = form.validationError(requireToken = true, allowMissingTarget = true)
        if (error != null) {
            showDiagnosticStatus(getString(error), true)
            return
        }
        val target = form
        showDiagnosticStatus(getString(R.string.workflow_installing), false)
        Thread {
            val result = runCatching {
                GkiBuilderApi.ensureGkiWorkflowBundle(target, loadGkiWorkflowAssets())
            }
            runOnUiThread {
                result
                    .onSuccess { install ->
                        form = install.form
                        store.save(form)
                        showDiagnosticStatus(
                            getString(R.string.workflow_installed, install.uploaded, install.skipped, install.form.workflowFile),
                            false,
                        )
                    }
                    .onFailure { error -> showDiagnosticStatus(error.userVisibleMessage(), true) }
            }
        }.start()
    }

    private fun checkWorkflow() {
        syncFormFromUi()
        val error = form.validationError(requireToken = true, allowMissingTarget = true)
        if (error != null) {
            showOAuthStatus(getString(error), true)
            return
        }
        showOAuthStatus(getString(R.string.checking_workflow), false)
        val checkForm = form
        Thread {
            val result = runCatching { GkiBuilderApi.resolveWorkflow(checkForm, enableDisabled = false) }
            runOnUiThread {
                result
                    .onSuccess { resolved ->
                        if (resolved.workflowFile != form.workflowFile) {
                            form = resolved
                            store.save(form)
                            if (::workflowInput.isInitialized && workflowInput.isAttachedToWindow) {
                                workflowInput.setText(resolved.workflowFile)
                            }
                            showOAuthStatus(getString(R.string.workflow_auto_selected, resolved.workflowFile), false)
                        } else {
                            showOAuthStatus(getString(R.string.workflow_exists), false)
                        }
                    }
                    .onFailure { error ->
                        showOAuthStatus(getString(R.string.workflow_check_failed, error.userVisibleMessage()), true)
                    }
            }
        }.start()
    }

    private fun refreshHistoryItem(item: BuildHistory) {
        if (item.runId == 0L) {
            showStatus(getString(R.string.run_id_missing), true)
            return
        }
        val runForm = form.copy(owner = item.owner, repo = item.repo)
        showStatus(getString(R.string.refreshing_status), false)
        Thread {
            val result = runCatching { fetchUpdatedHistory(item, runForm) }
            runOnUiThread {
                result
                    .onSuccess { updated ->
                        selectedHistory = updated
                        updateHistory(updated)
                        lastRunUrl = updated.url
                        showPage(PAGE_HISTORY_DETAIL)
                    }
                    .onFailure { error ->
                        showStatus(getString(R.string.refresh_failed, error.message ?: error.javaClass.simpleName), true)
                    }
            }
        }.start()
    }

    private fun watchRunProgress(item: BuildHistory) {
        if (item.runId == 0L) return
        val runForm = form.copy(owner = item.owner, repo = item.repo)
        Thread watcher@{
            var current = item
            repeat(240) { index ->
                SystemClock.sleep(if (index == 0) 6000L else 15000L)
                val result = runCatching { fetchUpdatedHistory(current, runForm) }
                result.onSuccess { updated ->
                    current = updated
                    runOnUiThread {
                        updateHistory(updated)
                        lastRunUrl = updated.url
                        if (selectedHistory?.runId == updated.runId) {
                            selectedHistory = updated
                        }
                        if (::statusText.isInitialized && statusText.isAttachedToWindow) {
                            statusText.text = updated.statusText()
                        }
                        if (currentPage == PAGE_HISTORY_DETAIL && selectedHistory?.runId == updated.runId) {
                            showPage(PAGE_HISTORY_DETAIL, syncBeforeRender = false)
                        }
                    }
                    if (updated.status == "completed") return@watcher
                }
            }
        }.start()
    }

    private fun fetchUpdatedHistory(
        item: BuildHistory,
        runForm: BuilderForm,
    ): BuildHistory {
        val run = GkiBuilderApi.getWorkflowRun(runForm, item.runId)
        val artifacts = if (run.status == "completed") {
            GkiBuilderApi.listRunArtifacts(runForm, item.runId)
        } else {
            emptyList()
        }
        return item.copy(
            status = run.status,
            conclusion = run.conclusion,
            url = run.htmlUrl.ifBlank { item.url },
            artifacts = artifacts.joinToString(";") {
                "${it.name}|${GkiBuilderApi.artifactDownloadUrl(item.owner, item.repo, it.id)}"
            },
        )
    }

    private fun openFailedLogs(item: BuildHistory) {
        if (item.runId == 0L) {
            showStatus(getString(R.string.run_id_missing), true)
            return
        }
        downloadFile(
            url = GkiBuilderApi.runLogsUrl(item.owner, item.repo, item.runId),
            fileName = "run-${item.runId}-logs.zip",
            verifyGki = false,
        )
    }

    private fun shareBuildResult(item: BuildHistory) {
        val artifacts = item.artifactList()
        val text = buildString {
            appendLine(getString(R.string.share_title))
            appendLine(getString(R.string.share_target, item.target))
            appendLine(getString(R.string.share_repo, "${item.owner}/${item.repo}"))
            appendLine(getString(R.string.share_status, item.statusText()))
            if (item.url.isNotBlank()) appendLine(getString(R.string.share_run, item.url))
            artifacts.forEach { artifact ->
                appendLine(getString(R.string.share_artifact, artifact.name, artifact.url))
            }
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, getString(R.string.share_result)))
    }

    private fun downloadFile(
        url: String,
        fileName: String,
        verifyGki: Boolean,
    ) {
        if (url.isBlank()) return
        syncFormFromUi()
        Toast.makeText(this, getString(R.string.download_started), Toast.LENGTH_LONG).show()
        Thread {
            val result = runCatching {
                val cacheFile = File(cacheDir, sanitizeFileName(fileName))
                FileOutputStream(cacheFile).use { output ->
                    GkiBuilderApi.downloadTo(url, form.token, output)
                }
                val verification = if (verifyGki) {
                    verifyApkeSuGkiArtifact(cacheFile)
                } else {
                    getString(R.string.download_saved_without_verify)
                }
                saveToDownloads(cacheFile, fileName)
                cacheFile.delete()
                verification
            }
            runOnUiThread {
                result
                    .onSuccess { message -> showStatus(getString(R.string.download_finished, message), false) }
                    .onFailure { error -> showStatus(getString(R.string.download_failed, error.userVisibleMessage()), true) }
            }
        }.start()
    }

    private fun saveToDownloads(
        source: File,
        fileName: String,
    ) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, sanitizeFileName(fileName))
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Android Downloads provider did not return a file uri.")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                FileInputStream(source).use { input -> input.copyTo(output) }
            } ?: throw IOException("Could not open Android Downloads output stream.")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun verifyApkeSuGkiArtifact(file: File): String {
        ZipFile(file).use { artifact ->
            artifact.getEntry("APKESU_GKI_INFO.json")?.let { entry ->
                val info = JSONObject(artifact.getInputStream(entry).bufferedReader().use { it.readText() })
                val project = info.optString("project")
                val artifactType = info.optString("artifact")
                val version = info.optString("apkesu_gki_version").ifBlank {
                    info.optString("ksu_version_override")
                }
                if (!project.equals("ApkeSU", ignoreCase = true) ||
                    !artifactType.equals("GKI", ignoreCase = true) ||
                    version != APKESU_GKI_VERSION
                ) {
                    throw IOException(getString(R.string.verify_gki_failed))
                }
                return getString(R.string.verify_gki_ok, version)
            }

            val names = artifact.entries().asSequence().map { it.name }.toList()
            val hasAnyKernelZip = names.any { it.endsWith("-AnyKernel3.zip") || it.endsWith("AnyKernel3.zip") }
            val mentionsApkeSu = names.any { it.contains("ApkeSU", ignoreCase = true) }
            if (hasAnyKernelZip && mentionsApkeSu) {
                return getString(R.string.verify_gki_legacy_ok)
            }
        }
        throw IOException(getString(R.string.verify_gki_missing_info))
    }

    private fun startGithubWebLogin() {
        syncFormFromUi()
        val clientId = form.githubClientId.trim().ifBlank { DEFAULT_GITHUB_CLIENT_ID }
        if (clientId.isBlank()) {
            showOAuthStatus(getString(R.string.error_github_client_id), true)
            return
        }
        if (clientId == DEFAULT_GITHUB_CLIENT_ID) {
            showOAuthStatus(getString(R.string.github_browser_login_needs_custom_client), true)
            return
        }
        pendingOAuthState = UUID.randomUUID().toString()
        val codeVerifier = createCodeVerifier()
        val codeChallenge = codeChallenge(codeVerifier)
        historyPrefs().edit()
            .putString(KEY_OAUTH_STATE, pendingOAuthState)
            .putString(KEY_OAUTH_VERIFIER, codeVerifier)
            .apply()
        form = form.copy(githubClientId = clientId)
        store.save(form)
        showOAuthStatus(getString(R.string.github_login_browser_opened), false)
        openUrl(GkiBuilderApi.oauthLoginUrl(clientId, pendingOAuthState, codeChallenge))
    }

    private fun handleOAuthIntent(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        if (uri.scheme != "apkesu" || uri.host != "oauth") return false
        val code = uri.getQueryParameter("code").orEmpty()
        val state = uri.getQueryParameter("state").orEmpty()
        val expectedState = historyPrefs().getString(KEY_OAUTH_STATE, "").orEmpty()
        val codeVerifier = historyPrefs().getString(KEY_OAUTH_VERIFIER, "").orEmpty()
        if (code.isBlank()) {
            val error = uri.getQueryParameter("error_description")
                ?: uri.getQueryParameter("error")
                ?: getString(R.string.github_login_no_code)
            showPage(PAGE_SETTINGS, syncBeforeRender = false)
            showOAuthStatus(getString(R.string.github_login_failed, error), true)
            return true
        }
        if (state.isBlank() || expectedState.isBlank() || state != expectedState || codeVerifier.isBlank()) {
            showPage(PAGE_SETTINGS, syncBeforeRender = false)
            showOAuthStatus(getString(R.string.github_login_state_mismatch), true)
            return true
        }
        val clientId = form.githubClientId.trim().ifBlank { DEFAULT_GITHUB_CLIENT_ID }
        showPage(PAGE_SETTINGS, syncBeforeRender = false)
        showOAuthStatus(getString(R.string.github_login_exchanging), false)
        Thread {
            val result = runCatching { GkiBuilderApi.exchangeOAuthCode(clientId, code, state, codeVerifier) }
            runOnUiThread {
                result
                    .onSuccess { token ->
                        form = form.copy(token = token.accessToken, githubClientId = clientId)
                        store.save(form)
                        historyPrefs().edit().remove(KEY_OAUTH_STATE).remove(KEY_OAUTH_VERIFIER).apply()
                        if (::tokenInput.isInitialized && tokenInput.isAttachedToWindow) {
                            tokenInput.setText(token.accessToken)
                        }
                        showOAuthStatus(getString(R.string.github_login_done), false)
                    }
                    .onFailure { error ->
                        showOAuthStatus(getString(R.string.github_login_failed, error.message ?: error.javaClass.simpleName), true)
                    }
            }
        }.start()
        return true
    }

    private fun startGithubDeviceLogin() {
        syncFormFromUi()
        val clientId = form.githubClientId.trim().ifBlank { DEFAULT_GITHUB_CLIENT_ID }
        if (clientId.isBlank()) {
            showOAuthStatus(getString(R.string.error_github_client_id), true)
            return
        }
        form = form.copy(githubClientId = clientId)
        store.save(form)
        showOAuthStatus(getString(R.string.github_login_starting), false)
        Thread {
            val loginResult = runCatching {
                val device = GkiBuilderApi.requestDeviceCode(clientId)
                runOnUiThread {
                    copyToClipboard(getString(R.string.github_login), device.userCode)
                    showOAuthStatus(getString(R.string.github_login_user_code, device.userCode), false)
                    openUrl(device.verificationUri)
                }
                pollGithubLogin(clientId, device)
            }
            runOnUiThread {
                loginResult
                    .onSuccess { result ->
                        form = form.copy(token = result.accessToken, githubClientId = clientId)
                        store.save(form)
                        if (::tokenInput.isInitialized && tokenInput.isAttachedToWindow) {
                            tokenInput.setText(result.accessToken)
                        }
                        showOAuthStatus(getString(R.string.github_login_done), false)
                    }
                    .onFailure { error ->
                        showOAuthStatus(getString(R.string.github_login_failed, error.githubLoginMessage()), true)
                    }
            }
        }.start()
    }

    private fun pollGithubLogin(
        clientId: String,
        device: DeviceCodeResult,
    ): OAuthTokenResult {
        var intervalMs = device.interval * 1000L
        val deadline = SystemClock.elapsedRealtime() + device.expiresIn * 1000L
        while (SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(intervalMs)
            try {
                return GkiBuilderApi.pollDeviceToken(clientId, device.deviceCode)
            } catch (error: OAuthPendingException) {
                when (error.code) {
                    "authorization_pending" -> Unit
                    "slow_down" -> intervalMs += 5000L
                    "expired_token" -> throw error
                    "access_denied" -> throw error
                    else -> throw error
                }
            }
        }
        throw java.io.IOException(getString(R.string.github_login_expired))
    }

    private fun createCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun BuilderForm.validationError(
        requireToken: Boolean = true,
        allowMissingTarget: Boolean = false,
    ): Int? {
        val target = normalized()
        return when {
            target.owner.isBlank() || target.repo.isBlank() -> R.string.error_repo
            target.ref.isBlank() -> R.string.error_ref
            target.workflowFile.isBlank() -> R.string.error_workflow
            requireToken && target.token.isBlank() -> R.string.error_token
            !allowMissingTarget && (
                target.androidVersion.isBlank() ||
                    target.kernelVersion.isBlank() ||
                    target.sublevel.isBlank() ||
                    target.osPatchLevel.isBlank() ||
                    target.featureSet.isBlank()
                ) -> R.string.error_target
            else -> null
        }
    }

    private fun BuilderForm.asSimpleDefault(): BuilderForm {
        return if (osPatchLevel == "2025-06" && sublevel == "89") {
            copy(sublevel = "X", osPatchLevel = "latest")
        } else {
            this
        }
    }

    private fun showStatus(
        text: String,
        error: Boolean,
    ) {
        if (::statusText.isInitialized) {
            if (statusText.isAttachedToWindow) {
                statusText.text = text
                statusText.setTextColor(if (error) WARNING else MUTED)
            } else {
                Toast.makeText(this, text, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }
    }

    private fun showOAuthStatus(
        text: String,
        error: Boolean,
    ) {
        if (::oauthStatusText.isInitialized && oauthStatusText.isAttachedToWindow) {
            oauthStatusText.text = text
            oauthStatusText.setTextColor(if (error) WARNING else MUTED)
        } else {
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }
    }

    private fun showDiagnosticStatus(
        text: String,
        error: Boolean,
    ) {
        if (::diagnosticStatusText.isInitialized && diagnosticStatusText.isAttachedToWindow) {
            diagnosticStatusText.text = text
            diagnosticStatusText.setTextColor(if (error) WARNING else MUTED)
        } else {
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }
    }

    private fun Throwable.userVisibleMessage(): String {
        val raw = message ?: javaClass.simpleName
        return when {
            raw.contains("Must have admin rights", ignoreCase = true) ||
                raw.contains("does not have build rights", ignoreCase = true) ->
                getString(R.string.error_repo_admin_rights)
            raw.contains("GitHub fork is still preparing", ignoreCase = true) ->
                getString(R.string.error_fork_preparing)
            raw.contains("No usable GKI workflow", ignoreCase = true) ->
                getString(R.string.error_no_gki_workflow)
            else -> raw
        }
    }

    private fun Throwable.githubLoginMessage(): String {
        val raw = message ?: javaClass.simpleName
        return when {
            raw.contains("404", ignoreCase = true) ->
                getString(R.string.github_login_404_hint)
            raw.contains("incorrect_client_credentials", ignoreCase = true) ||
                raw.contains("bad_verification_code", ignoreCase = true) ->
                getString(R.string.github_login_client_hint)
            else -> raw
        }
    }

    private fun setBusy(busy: Boolean) {
        if (::progress.isInitialized && progress.isAttachedToWindow) {
            progress.visibility = if (busy) View.VISIBLE else View.GONE
        }
    }

    private fun openRunButtonOrNull(): Button? {
        return if (::openRunButton.isInitialized && openRunButton.isAttachedToWindow) openRunButton else null
    }

    private fun openUrl(url: String) {
        if (url.isBlank()) return
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun copyToClipboard(
        label: String,
        text: String,
    ) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun appendHistory(item: BuildHistory) {
        val sanitized = sanitizeHistoryItem(item)
        val items = (listOf(sanitized) + readHistory()).take(20)
        historyPrefs().edit()
            .putString(KEY_HISTORY, items.joinToString("\n") { it.encode() })
            .putString(KEY_LAST_RUN_URL, sanitized.url)
            .putString(KEY_LAST_STATUS, sanitized.statusText())
            .apply()
    }

    private fun updateHistory(item: BuildHistory) {
        val sanitized = sanitizeHistoryItem(item)
        val items = readHistory().map { existing ->
            if (existing.runId != 0L && existing.runId == item.runId) sanitized else existing
        }
        historyPrefs().edit()
            .putString(KEY_HISTORY, items.joinToString("\n") { it.encode() })
            .putString(KEY_LAST_RUN_URL, sanitized.url)
            .putString(KEY_LAST_STATUS, sanitized.statusText())
            .apply()
    }

    private fun nowStamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

    private fun readHistory(): List<BuildHistory> {
        return historyPrefs().getString(KEY_HISTORY, "").orEmpty()
            .lineSequence()
            .mapNotNull { BuildHistory.decode(it) }
            .map { sanitizeHistoryItem(it) }
            .toList()
    }

    private fun historyPrefs() = getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)

    private fun migrateStoredHistory() {
        val prefs = historyPrefs()
        val rawHistory = prefs.getString(KEY_HISTORY, "").orEmpty()
        val items = rawHistory.lineSequence()
            .mapNotNull { BuildHistory.decode(it) }
            .map { sanitizeHistoryItem(it) }
            .toList()
        val edit = prefs.edit()
        if (rawHistory.isNotBlank()) {
            edit.putString(KEY_HISTORY, items.joinToString("\n") { it.encode() })
        }
        val lastStatus = items.firstOrNull()?.statusText()
            ?: prefs.getString(KEY_LAST_STATUS, null)?.let(::sanitizeStoredMessage)
        if (lastStatus != null) {
            edit.putString(KEY_LAST_STATUS, lastStatus)
        }
        edit.apply()
    }

    private fun sanitizeHistoryItem(item: BuildHistory): BuildHistory {
        return item.copy(
            status = sanitizeStoredMessage(item.status),
            conclusion = sanitizeStoredMessage(item.conclusion),
        )
    }

    private fun sanitizeStoredMessage(raw: String): String {
        return when {
            raw.contains("Must have admin rights", ignoreCase = true) ||
                raw.contains("does not have build rights", ignoreCase = true) ->
                getString(R.string.error_repo_admin_rights)
            raw.contains("GitHub fork is still preparing", ignoreCase = true) ->
                getString(R.string.error_fork_preparing)
            else -> raw
        }
    }

    private fun historyPreviewCard(): LinearLayout {
        return card {
            addView(title(getString(R.string.latest_builds), 18))
            val items = readHistory()
            if (items.isEmpty()) {
                addView(muted(getString(R.string.no_history)))
            } else {
                items.forEach { item ->
                    addView(historyRow(item))
                }
            }
        }
    }

    private fun historyRow(item: BuildHistory): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(7), 0, dp(7))
            addView(TextView(this@MainActivity).apply {
                text = item.target
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(TEXT)
            })
            addView(TextView(this@MainActivity).apply {
                text = getString(R.string.history_status, item.stamp, item.statusText())
                textSize = 13f
                setTextColor(if (item.status == getString(R.string.build_failure)) WARNING_MUTED else MUTED)
            })
            setOnClickListener {
                selectedHistory = item
                showPage(PAGE_HISTORY_DETAIL)
            }
        }
    }

    private fun renderHistoryDetail() {
        val item = selectedHistory
        pageBody.addView(pageTitle(getString(R.string.build_detail)))
        if (item == null) {
            pageBody.addView(card { addView(muted(getString(R.string.no_history))) })
            return
        }
        pageBody.addView(card {
            addView(title(item.target, 18))
            addView(infoRow(getString(R.string.repository), "${item.owner}/${item.repo}"))
            addView(infoRow(getString(R.string.status_build), item.statusText()))
            addView(infoRow(getString(R.string.run_id), if (item.runId == 0L) "-" else item.runId.toString()))
            addView(actionRow(
                primaryButton(getString(R.string.refresh_status)) { refreshHistoryItem(item) },
                secondaryButton(getString(R.string.open_run)) { openUrl(item.url) },
            ))
            addView(actionRow(
                secondaryButton(getString(R.string.view_failed_logs)) { openFailedLogs(item) },
                secondaryButton(getString(R.string.share_result)) { shareBuildResult(item) },
            ))
        })
        pageBody.addView(card {
            addView(title(getString(R.string.artifacts), 18))
            val artifacts = item.artifactList()
            if (artifacts.isEmpty()) {
                addView(muted(getString(R.string.no_artifacts)))
            } else {
                artifacts.forEach { artifact ->
                    addView(secondaryButton(getString(R.string.download_artifact, artifact.name)) {
                        downloadFile(artifact.url, "${artifact.name}.zip", verifyGki = true)
                    }, matchWrap())
                }
            }
        })
    }

    private fun pageTitle(text: String): TextView {
        return title(text, 29, TEXT).apply {
            setPadding(0, dp(20), 0, dp(16))
        }
    }

    private fun card(block: LinearLayout.() -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = solid(CARD, 8)
            layoutParams = matchWrapWithMargins(top = 6, bottom = 8)
            block()
        }
    }

    private fun heroCard(block: LinearLayout.() -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = solid(ACCENT, 8)
            layoutParams = matchWrapWithMargins(top = 4, bottom = 10)
            block()
        }
    }

    private fun statusTile(
        label: String,
        value: String,
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = solid(CARD, 8)
            addView(title(label, 16))
            addView(muted(value))
        }
    }

    private fun title(
        text: String,
        sp: Int,
        color: Int = TEXT,
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = sp.toFloat()
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color)
            includeFontPadding = true
        }
    }

    private fun muted(
        text: String,
        color: Int = MUTED,
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(color)
            setPadding(0, dp(4), 0, dp(4))
            setLineSpacing(dp(2).toFloat(), 1.0f)
        }
    }

    private fun infoRow(
        label: String,
        value: String,
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 14f
                setTextColor(MUTED)
            }, LinearLayout.LayoutParams(0, wrap(), 1f))
            addView(TextView(this@MainActivity).apply {
                text = value
                textSize = 14f
                gravity = Gravity.END
                setTextColor(TEXT)
            }, LinearLayout.LayoutParams(0, wrap(), 1.3f))
        }
    }

    private fun chipRow(items: List<String>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
            items.forEach { addView(chip(it)) }
        }
    }

    private fun chip(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(7), dp(12), dp(7))
            background = solid(CHIP, 8)
            layoutParams = LinearLayout.LayoutParams(wrap(), wrap()).apply {
                setMargins(0, 0, dp(8), 0)
            }
        }
    }

    private fun LinearLayout.addSpinnerField(
        label: String,
        items: List<String>,
    ): Spinner {
        addView(fieldLabel(label))
        val spinner = Spinner(this@MainActivity).apply {
            adapter = darkAdapter(items)
            backgroundTintList = ColorStateList.valueOf(OUTLINE)
            setPopupBackgroundDrawable(solid(CARD, 8))
        }
        addView(spinner, matchWrapWithMargins(bottom = 10))
        return spinner
    }

    private fun LinearLayout.addSwitchField(
        label: String,
        summary: String?,
    ): Switch {
        val row = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val textBox = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 15f
                setTextColor(TEXT)
            })
            if (summary != null) {
                addView(TextView(this@MainActivity).apply {
                    text = summary
                    textSize = 12f
                    setTextColor(MUTED)
                })
            }
        }
        row.addView(textBox, LinearLayout.LayoutParams(0, wrap(), 1f))
        val switch = Switch(this@MainActivity).apply {
            buttonTintList = ColorStateList.valueOf(ACCENT_SOFT)
        }
        row.addView(switch)
        addView(row)
        return switch
    }

    private fun editText(
        hint: String,
        password: Boolean = false,
    ): EditText {
        return EditText(this).apply {
            this.hint = hint
            setSingleLine(true)
            textSize = 15f
            setTextColor(TEXT)
            setHintTextColor(MUTED)
            backgroundTintList = ColorStateList.valueOf(OUTLINE)
            inputType = if (password) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT
            }
            layoutParams = matchWrapWithMargins(bottom = 8)
        }
    }

    private fun fieldLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(MUTED)
            setPadding(0, dp(8), 0, 0)
        }
    }

    private fun darkAdapter(items: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup,
            ): View = spinnerText(getItem(position).orEmpty(), false)

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup,
            ): View = spinnerText(getItem(position).orEmpty(), true)
        }
    }

    private fun spinnerText(
        text: String,
        dropdown: Boolean,
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(TEXT)
            setPadding(dp(12), if (dropdown) dp(14) else dp(10), dp(12), if (dropdown) dp(14) else dp(10))
            if (dropdown) background = solid(CARD, 0)
        }
    }

    private fun primaryButton(
        text: String,
        onClick: () -> Unit,
    ): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            backgroundTintList = ColorStateList.valueOf(ACCENT)
            setOnClickListener { onClick() }
        }
    }

    private fun secondaryButton(
        text: String,
        onClick: () -> Unit,
    ): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(TEXT)
            textSize = 14f
            backgroundTintList = ColorStateList.valueOf(SURFACE)
            setOnClickListener { onClick() }
        }
    }

    private fun actionRow(
        first: Button,
        second: Button,
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(first, weightedButton())
            addView(second, weightedButton())
        }
    }

    private fun progressBar(value: Int): ProgressBar {
        return ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = value
            progressTintList = ColorStateList.valueOf(ACCENT_SOFT)
            progressBackgroundTintList = ColorStateList.valueOf(SURFACE)
            layoutParams = matchWrapWithMargins(top = 8, bottom = 8)
        }
    }

    private fun accountChip(): String {
        return if (form.token.isBlank()) getString(R.string.chip_token_missing) else getString(R.string.chip_token_ready)
    }

    private fun accountStatus(): String {
        return if (form.token.isBlank()) getString(R.string.chip_token_missing) else getString(R.string.status_granted)
    }

    private fun oauthStatus(): String {
        return if (form.token.isBlank()) getString(R.string.github_login_not_ready) else getString(R.string.github_login_ready)
    }

    private fun kernelStatus(): String {
        return "${form.kernelVersion}.${form.sublevel} ${form.androidVersion}"
    }

    private fun repoName(): String = "${form.owner}/${form.repo}"

    private fun lastBuildStatus(): String {
        return historyPrefs().getString(KEY_LAST_STATUS, null) ?: getString(R.string.status_idle)
    }

    private fun targetLine(): String {
        return "${form.kernelVersion}.${form.sublevel} ${form.androidVersion} ${form.osPatchLevel}"
    }

    private fun channelLabel(): String {
        return if (form.useLatestSusfs) CHANNEL_LATEST else CHANNEL_STABLE
    }

    private fun selected(
        spinner: Spinner,
        fallback: String,
    ): String {
        return if (isSpinnerReady(spinner)) {
            spinner.selectedItem?.toString() ?: fallback
        } else {
            fallback
        }
    }

    private fun checked(
        switch: Switch,
        fallback: Boolean,
    ): Boolean {
        return if (isSwitchReady(switch)) switch.isChecked else fallback
    }

    private fun textOf(
        editText: EditText,
        fallback: String,
    ): String {
        return if (isEditReady(editText)) editText.text.toString() else fallback
    }

    private fun isSpinnerReady(spinner: Spinner): Boolean {
        return try {
            spinner.adapter != null && spinner.isAttachedToWindow
        } catch (_: UninitializedPropertyAccessException) {
            false
        }
    }

    private fun isSwitchReady(switch: Switch): Boolean {
        return try {
            switch.isAttachedToWindow
        } catch (_: UninitializedPropertyAccessException) {
            false
        }
    }

    private fun isEditReady(editText: EditText): Boolean {
        return try {
            editText.isAttachedToWindow
        } catch (_: UninitializedPropertyAccessException) {
            false
        }
    }

    private fun setSpinner(
        spinner: Spinner,
        value: String,
    ) {
        val adapter = spinner.adapter ?: return
        for (index in 0 until adapter.count) {
            if (adapter.getItem(index).toString() == value) {
                spinner.setSelection(index)
                return
            }
        }
        spinner.setSelection(0)
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "download.zip" }
    }

    private fun loadGkiWorkflowAssets(): List<WorkflowAssetFile> {
        return listAssetFiles("")
            .filter { path ->
                path.startsWith("workflows/gki-") ||
                    path.startsWith("actions/gki-") ||
                    path.startsWith("config/gki-")
            }
            .filterNot { it.endsWith(".pyc") || it.contains("/__pycache__/") }
            .map { path ->
                WorkflowAssetFile(
                    assetPath = path,
                    repoPath = ".github/$path",
                    content = assets.open(path).use { it.readBytes() },
                )
            }
    }

    private fun listAssetFiles(path: String): List<String> {
        val children = assets.list(path).orEmpty()
        if (children.isEmpty()) return if (path.isBlank()) emptyList() else listOf(path)
        return children.flatMap { child ->
            val childPath = if (path.isBlank()) child else "$path/$child"
            listAssetFiles(childPath)
        }
    }

    private fun solid(
        color: Int,
        radius: Int,
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun matchWrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(match(), wrap())
    }

    private fun matchWrapWithMargins(
        top: Int = 0,
        bottom: Int = 0,
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(match(), wrap()).apply {
            setMargins(0, dp(top), 0, dp(bottom))
        }
    }

    private fun weightedButton(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, wrap(), 1f).apply {
            setMargins(dp(3), dp(6), dp(3), dp(4))
        }
    }

    private fun weightedGrid(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, wrap(), 1f).apply {
            setMargins(dp(3), dp(4), dp(3), dp(6))
        }
    }

    private fun match() = LinearLayout.LayoutParams.MATCH_PARENT

    private fun wrap() = LinearLayout.LayoutParams.WRAP_CONTENT

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

private data class BuildHistory(
    val stamp: String,
    val status: String,
    val target: String,
    val url: String,
    val owner: String,
    val repo: String,
    val runId: Long,
    val conclusion: String,
    val artifacts: String,
) {
    fun encode(): String = listOf(
        stamp,
        status,
        target,
        url,
        owner,
        repo,
        runId.toString(),
        conclusion,
        artifacts,
    ).joinToString("\t") { it.replace("\t", " ") }

    fun statusText(): String {
        return if (conclusion.isBlank() || conclusion == "null") status else "$status / $conclusion"
    }

    fun artifactList(): List<ArtifactLink> {
        if (artifacts.isBlank()) return emptyList()
        return artifacts.split(';').mapNotNull { raw ->
            val parts = raw.split('|', limit = 2)
            if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) null else ArtifactLink(parts[0], parts[1])
        }
    }

    companion object {
        fun decode(value: String): BuildHistory? {
            val parts = value.split('\t', limit = 9)
            if (parts.size < 4) return null
            return BuildHistory(
                stamp = parts[0],
                status = parts[1],
                target = parts[2],
                url = parts[3],
                owner = parts.getOrNull(4).orEmpty().ifBlank { DEFAULT_OWNER },
                repo = parts.getOrNull(5).orEmpty().ifBlank { DEFAULT_REPO },
                runId = parts.getOrNull(6)?.toLongOrNull() ?: 0L,
                conclusion = parts.getOrNull(7).orEmpty(),
                artifacts = parts.getOrNull(8).orEmpty(),
            )
        }
    }
}

private data class ArtifactLink(
    val name: String,
    val url: String,
)

private const val PAGE_HOME = 0
private const val PAGE_BUILD = 1
private const val PAGE_RECORDS = 2
private const val PAGE_HISTORY_DETAIL = 3
private const val PAGE_TOOLS = 4
private const val PAGE_SETTINGS = 5

private const val HISTORY_PREFS = "gki_builder_history"
private const val KEY_HISTORY = "history"
private const val KEY_LAST_RUN_URL = "last_run_url"
private const val KEY_LAST_STATUS = "last_status"
private const val KEY_OAUTH_STATE = "oauth_state"
private const val KEY_OAUTH_VERIFIER = "oauth_verifier"
private const val APKESU_GKI_VERSION = "32645"

private val BG = Color.rgb(15, 18, 27)
private val NAV = Color.rgb(28, 31, 42)
private val CARD = Color.rgb(29, 32, 44)
private val SURFACE = Color.rgb(54, 58, 74)
private val TEXT = Color.rgb(232, 236, 248)
private val MUTED = Color.rgb(177, 183, 205)
private val HERO_TEXT = Color.rgb(221, 229, 255)
private val OUTLINE = Color.rgb(122, 128, 150)
private val ACCENT = Color.rgb(20, 87, 200)
private val ACCENT_SOFT = Color.rgb(171, 193, 255)
private val CHIP = Color.rgb(46, 105, 213)
private val WARNING = Color.rgb(255, 180, 171)
private val WARNING_MUTED = Color.rgb(224, 151, 148)

private const val CHANNEL_STABLE = "Stable"
private const val CHANNEL_LATEST = "Latest"

private val NAV_ITEMS = listOf(
    PAGE_HOME to R.string.nav_home,
    PAGE_BUILD to R.string.nav_build,
    PAGE_RECORDS to R.string.nav_records,
    PAGE_TOOLS to R.string.nav_tools,
    PAGE_SETTINGS to R.string.nav_settings,
)

private val ANDROID_OPTIONS = listOf("android16", "android15", "android14", "android13", "android12")
private val KERNEL_OPTIONS = listOf("6.12", "6.6", "6.1", "5.15", "5.10")
private val SUBLEVEL_OPTIONS = listOf("X", "89", "77", "74", "62", "45", "39", "30")
private val PATCH_OPTIONS = listOf("latest", "2026-07", "2026-06", "2026-05", "2025-06", "2025-03", "2024-12", "lts")
private val FEATURE_OPTIONS = listOf("ApkeSU+SUSFS", "ApkeSU")
private val CHANNEL_OPTIONS = listOf(CHANNEL_STABLE, CHANNEL_LATEST)
private val VIRTUALIZATION_OPTIONS = listOf("off", "on", "678", "123", "345")
private val OPLUS_OPTIONS = listOf("off", "compat", "zram")
