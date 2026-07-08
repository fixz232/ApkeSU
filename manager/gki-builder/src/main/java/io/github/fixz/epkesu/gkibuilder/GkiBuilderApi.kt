package io.github.fixz.apkesu.gkibuilder

import org.json.JSONObject
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.lang.Thread.sleep
import android.util.Base64

const val DEFAULT_OWNER = "fixz232"
const val DEFAULT_REPO = "ApkeSU"
const val DEFAULT_REF = "main"
const val DEFAULT_WORKFLOW = "gki-custom.yml"
const val DEFAULT_GITHUB_CLIENT_ID = "Ov23li8skGo6AFPBeSTh"
const val GITHUB_OAUTH_REDIRECT_URI = "abk://oauth"

data class BuilderForm(
    val owner: String = DEFAULT_OWNER,
    val repo: String = DEFAULT_REPO,
    val ref: String = DEFAULT_REF,
    val workflowFile: String = DEFAULT_WORKFLOW,
    val token: String = "",
    val githubClientId: String = DEFAULT_GITHUB_CLIENT_ID,
    val androidVersion: String = "android15",
    val kernelVersion: String = "6.6",
    val sublevel: String = "X",
    val osPatchLevel: String = "latest",
    val featureSet: String = "ApkeSU+SUSFS",
    val useRepo: Boolean = true,
    val useLatestSusfs: Boolean = true,
    val buildBypass: Boolean = false,
    val buildTime: String = "Sun Dec 01 08:10:00 UTC 2024",
    val useZram: Boolean = false,
    val zramFullAlgo: Boolean = false,
    val zramExtraAlgos: String = "",
    val useNtsync: Boolean = false,
    val useNetworking: Boolean = false,
    val virtualizationSupport: String = "off",
    val useBbg: Boolean = false,
    val useDdk: Boolean = false,
    val useKpm: Boolean = false,
    val useRekernel: Boolean = false,
    val uploadAuxArtifacts: Boolean = true,
    val oplusPatchMode: String = "off",
    val oplusReferenceRef: String = "main",
) {
    fun normalized(): BuilderForm = copy(
        owner = owner.trim(),
        repo = repo.trim(),
        ref = ref.trim(),
        workflowFile = workflowFile.trim(),
        token = token.trim(),
        githubClientId = githubClientId.trim(),
        androidVersion = androidVersion.trim(),
        kernelVersion = kernelVersion.trim(),
        sublevel = sublevel.trim(),
        osPatchLevel = osPatchLevel.trim(),
        featureSet = featureSet.trim(),
        buildTime = buildTime.trim(),
        zramExtraAlgos = zramExtraAlgos.trim(),
        virtualizationSupport = virtualizationSupport.trim(),
        oplusPatchMode = oplusPatchMode.trim(),
        oplusReferenceRef = oplusReferenceRef.trim(),
    )
}

data class DispatchResult(
    val htmlUrl: String,
    val runId: Long? = null,
    val status: String? = null,
)

data class DeviceCodeResult(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val verificationUriComplete: String,
    val expiresIn: Int,
    val interval: Int,
)

data class OAuthTokenResult(
    val accessToken: String,
    val scope: String,
    val tokenType: String,
)

data class RepositoryInfo(
    val owner: String,
    val name: String,
    val fullName: String,
    val defaultBranch: String,
    val privateRepo: Boolean,
)

data class GitHubUserInfo(
    val login: String,
)

data class RepositoryAccessInfo(
    val owner: String,
    val name: String,
    val fullName: String,
    val defaultBranch: String,
    val canPush: Boolean,
    val canMaintain: Boolean,
    val canAdmin: Boolean,
) {
    val canBuild: Boolean
        get() = canPush || canMaintain || canAdmin
}

data class WorkflowRunInfo(
    val id: Long,
    val htmlUrl: String,
    val status: String,
    val conclusion: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String,
)

data class ArtifactInfo(
    val id: Long,
    val name: String,
    val sizeInBytes: Long,
    val expired: Boolean,
    val archiveDownloadUrl: String,
)

data class WorkflowInfo(
    val id: Long,
    val name: String,
    val path: String,
    val state: String,
) {
    val fileName: String
        get() = path.substringAfterLast('/').ifBlank { path }
}

data class WorkflowAssetFile(
    val assetPath: String,
    val repoPath: String,
    val content: ByteArray,
)

data class WorkflowInstallResult(
    val form: BuilderForm,
    val uploaded: Int,
    val skipped: Int,
)

object GkiBuilderApi {
    fun dispatch(form: BuilderForm): DispatchResult {
        val target = form.normalized()
        return runCatching {
            dispatchOnce(target, returnRunDetails = true)
        }.getOrElse { error ->
            if (error is ReturnRunDetailsUnsupportedException) {
                dispatchOnce(target, returnRunDetails = false)
            } else {
                throw error
            }
        }
    }

    fun workflowUrl(form: BuilderForm): String {
        val target = form.normalized()
        return "https://github.com/${target.owner}/${target.repo}/actions/workflows/${target.workflowFile}"
    }

    fun prepareBuildForm(form: BuilderForm): BuilderForm {
        val writable = ensureWritableRepository(form)
        return runCatching {
            resolveWorkflow(writable, enableDisabled = true)
        }.getOrElse { error ->
            if (!isNoUsableWorkflow(error) || form.owner.equals(DEFAULT_OWNER, ignoreCase = true)) {
                throw error
            }
            val defaultSource = form.copy(
                owner = DEFAULT_OWNER,
                repo = DEFAULT_REPO,
                ref = DEFAULT_REF,
                workflowFile = DEFAULT_WORKFLOW,
            )
            resolveWorkflow(ensureWritableRepository(defaultSource), enableDisabled = true)
        }
    }

    fun ensureGkiWorkflowBundle(
        form: BuilderForm,
        files: List<WorkflowAssetFile>,
    ): WorkflowInstallResult {
        val writable = ensureWritableRepository(form)
        val resolved = runCatching { resolveWorkflow(writable, enableDisabled = true) }.getOrNull()
        if (resolved != null) {
            return WorkflowInstallResult(form = resolved, uploaded = 0, skipped = files.size)
        }

        var uploaded = 0
        var skipped = 0
        files.forEach { file ->
            if (putRepositoryFileIfChanged(writable, file.repoPath, file.content)) {
                uploaded += 1
            } else {
                skipped += 1
            }
        }

        val resolvedAfterInstall = resolveWorkflow(writable, enableDisabled = true)
        return WorkflowInstallResult(form = resolvedAfterInstall, uploaded = uploaded, skipped = skipped)
    }

    fun resolveWorkflow(
        form: BuilderForm,
        enableDisabled: Boolean = true,
    ): BuilderForm {
        val target = form.normalized()

        val workflows = listWorkflows(target, retryWhenEmpty = enableDisabled)
        val current = workflows.firstOrNull {
            it.fileName.equals(target.workflowFile.substringAfterLast('/'), ignoreCase = true)
        }
        if (current != null) {
            if (enableDisabled) ensureWorkflowEnabled(target, current)
            return target.copy(workflowFile = current.fileName)
        }

        val selected = selectBestWorkflow(target, workflows)
            ?: selectExistingWorkflowFile(target)
            ?: throw IOException(NO_USABLE_GKI_WORKFLOW)
        if (enableDisabled) ensureWorkflowEnabled(target, selected)
        return target.copy(workflowFile = selected.fileName)
    }

    fun getAuthenticatedUser(token: String): GitHubUserInfo {
        val response = request(
            url = "https://api.github.com/user",
            token = token,
            method = "GET",
            body = null,
        )
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        val login = JSONObject(response.body).optString("login")
        if (login.isBlank()) throw IOException("GitHub did not return the current user.")
        return GitHubUserInfo(login)
    }

    fun getRepositoryAccess(form: BuilderForm): RepositoryAccessInfo? {
        val target = form.normalized()
        val response = request(
            url = "https://api.github.com/repos/${target.owner}/${target.repo}",
            token = target.token,
            method = "GET",
            body = null,
        )
        if (response.code == 404) return null
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        return parseRepositoryAccess(JSONObject(response.body))
    }

    fun listRepositories(token: String): List<RepositoryInfo> {
        val perPage = 100
        val repositories = mutableListOf<RepositoryInfo>()
        for (page in 1..5) {
            val pageItems = listRepositoriesPage(token, page, perPage)
            repositories += pageItems
            if (pageItems.size < perPage) break
        }
        return repositories
    }

    private fun listRepositoriesPage(
        token: String,
        page: Int,
        perPage: Int,
    ): List<RepositoryInfo> {
        val response = request(
            url = "https://api.github.com/user/repos?visibility=all&affiliation=owner,collaborator,organization_member" +
                "&sort=updated&per_page=$perPage&page=$page",
            token = token,
            method = "GET",
            body = null,
        )
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        val repos = org.json.JSONArray(response.body)
        return buildList {
            for (index in 0 until repos.length()) {
                val repo = repos.optJSONObject(index) ?: continue
                val owner = repo.optJSONObject("owner")?.optString("login").orEmpty()
                val name = repo.optString("name")
                val fullName = repo.optString("full_name")
                if (owner.isBlank() || name.isBlank() || fullName.isBlank()) continue
                add(
                    RepositoryInfo(
                        owner = owner,
                        name = name,
                        fullName = fullName,
                        defaultBranch = repo.optString("default_branch").ifBlank { DEFAULT_REF },
                        privateRepo = repo.optBoolean("private", false),
                    ),
                )
            }
        }
    }

    fun workflowExists(form: BuilderForm): Boolean {
        val target = form.normalized()
        val response = request(
            url = "https://api.github.com/repos/${target.owner}/${target.repo}/actions/workflows/${workflowId(target.workflowFile)}",
            token = target.token,
            method = "GET",
            body = null,
        )
        if (response.code == 404) return false
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        return true
    }

    fun workflowFileExists(form: BuilderForm): Boolean {
        return workflowFileExists(form.normalized(), form.workflowFile.substringAfterLast('/'))
    }

    fun refExists(form: BuilderForm): Boolean {
        val target = form.normalized()
        val response = request(
            url = "https://api.github.com/repos/${target.owner}/${target.repo}/commits/${urlEncode(target.ref)}",
            token = target.token,
            method = "GET",
            body = null,
        )
        if (response.code == 404) return false
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        return true
    }

    fun listWorkflows(
        form: BuilderForm,
        retryWhenEmpty: Boolean = false,
    ): List<WorkflowInfo> {
        val target = form.normalized()
        repeat(if (retryWhenEmpty) 8 else 1) { attempt ->
            val workflows = listWorkflowsOnce(target)
            if (workflows.isNotEmpty() || !retryWhenEmpty) return workflows
            if (attempt < 7) sleep(2500L)
        }
        return emptyList()
    }

    private fun listWorkflowsOnce(target: BuilderForm): List<WorkflowInfo> {
        val response = request(
            url = "https://api.github.com/repos/${target.owner}/${target.repo}/actions/workflows?per_page=100",
            token = target.token,
            method = "GET",
            body = null,
        )
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        val workflows = JSONObject(response.body).optJSONArray("workflows") ?: return emptyList()
        return buildList {
            for (index in 0 until workflows.length()) {
                val workflow = workflows.optJSONObject(index) ?: continue
                val path = workflow.optString("path")
                if (path.isBlank()) continue
                add(
                    WorkflowInfo(
                        id = workflow.optLong("id", 0L),
                        name = workflow.optString("name"),
                        path = path,
                        state = workflow.optString("state"),
                    ),
                )
            }
        }
    }

    private fun selectExistingWorkflowFile(target: BuilderForm): WorkflowInfo? {
        val candidates = listOf(
            target.workflowFile.substringAfterLast('/'),
            DEFAULT_WORKFLOW,
            "gki-custom.yml",
            "gki-abk-main.yml",
            "gki-main.yml",
            "gki-oneplus-realme.yml",
        ).map { it.trim() }.filter { it.isNotBlank() }.distinct()
        candidates.forEach { fileName ->
            if (workflowFileExists(target, fileName)) {
                return WorkflowInfo(
                    id = 0L,
                    name = fileName,
                    path = ".github/workflows/$fileName",
                    state = "active",
                )
            }
        }
        return null
    }

    private fun workflowFileExists(
        target: BuilderForm,
        fileName: String,
    ): Boolean {
        val response = request(
            url = "https://api.github.com/repos/${target.owner}/${target.repo}/contents/.github/workflows/${urlEncode(fileName)}?ref=${urlEncode(target.ref)}",
            token = target.token,
            method = "GET",
            body = null,
        )
        if (response.code == 404) return false
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        return true
    }

    private fun putRepositoryFileIfChanged(
        form: BuilderForm,
        path: String,
        content: ByteArray,
    ): Boolean {
        val existing = getRepositoryFile(form, path)
        if (existing?.decodedContent?.contentEquals(content) == true) return false

        val payload = JSONObject()
            .put("message", "gki: install ApkeSU GKI workflow")
            .put("content", Base64.encodeToString(content, Base64.NO_WRAP))
            .put("branch", form.ref)
        existing?.sha?.takeIf { it.isNotBlank() }?.let { payload.put("sha", it) }

        val response = request(
            url = "https://api.github.com/repos/${form.owner}/${form.repo}/contents/${urlEncodePath(path)}",
            token = form.token,
            method = "PUT",
            body = payload.toString(),
        )
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        return true
    }

    private fun getRepositoryFile(
        form: BuilderForm,
        path: String,
    ): RepositoryFile? {
        val response = request(
            url = "https://api.github.com/repos/${form.owner}/${form.repo}/contents/${urlEncodePath(path)}?ref=${urlEncode(form.ref)}",
            token = form.token,
            method = "GET",
            body = null,
        )
        if (response.code == 404) return null
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        val root = JSONObject(response.body)
        val encoded = root.optString("content").replace("\n", "")
        val decoded = if (encoded.isBlank()) ByteArray(0) else Base64.decode(encoded, Base64.DEFAULT)
        return RepositoryFile(
            sha = root.optString("sha"),
            decodedContent = decoded,
        )
    }

    private fun ensureWritableRepository(form: BuilderForm): BuilderForm {
        val target = form.normalized()
        getRepositoryAccess(target)?.let { access ->
            if (access.canBuild) return target.withRepoAccess(access)
        }

        val user = getAuthenticatedUser(target.token)
        if (!target.owner.equals(user.login, ignoreCase = true)) {
            val ownedForm = target.copy(owner = user.login)
            getRepositoryAccess(ownedForm)?.let { access ->
                if (access.canBuild) return ownedForm.withRepoAccess(access)
            }

            return createForkAndWait(target, user.login)
        }

        throw IOException(NO_REPOSITORY_BUILD_RIGHTS)
    }

    private fun createForkAndWait(
        source: BuilderForm,
        owner: String,
    ): BuilderForm {
        val forkForm = source.copy(owner = owner)
        val response = request(
            url = "https://api.github.com/repos/${source.owner}/${source.repo}/forks",
            token = source.token,
            method = "POST",
            body = "{}",
        )
        if (response.code !in 200..299 && response.code != 202 && response.code != 422) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }

        repeat(18) {
            getRepositoryAccess(forkForm)?.let { access ->
                if (access.canBuild) return forkForm.withRepoAccess(access)
            }
            sleep(2500L)
        }
        throw IOException("GitHub fork is still preparing. Try building again in a minute.")
    }

    private fun ensureWorkflowEnabled(
        form: BuilderForm,
        workflow: WorkflowInfo,
    ) {
        if (workflow.state.isBlank() || workflow.state == "active") return
        val response = request(
            url = "https://api.github.com/repos/${form.owner}/${form.repo}/actions/workflows/${workflowId(workflow.fileName)}/enable",
            token = form.token,
            method = "PUT",
            body = null,
        )
        if (response.code !in 200..299 && response.code != 204) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
    }

    fun getWorkflowRun(
        form: BuilderForm,
        runId: Long,
    ): WorkflowRunInfo {
        val target = form.normalized()
        val response = request(
            url = "https://api.github.com/repos/${target.owner}/${target.repo}/actions/runs/$runId",
            token = target.token,
            method = "GET",
            body = null,
        )
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        val run = JSONObject(response.body)
        return parseWorkflowRunInfo(run)
            ?: throw IOException("GitHub run response did not include a run id.")
    }

    fun listRunArtifacts(
        form: BuilderForm,
        runId: Long,
    ): List<ArtifactInfo> {
        val target = form.normalized()
        val response = request(
            url = "https://api.github.com/repos/${target.owner}/${target.repo}/actions/runs/$runId/artifacts?per_page=100",
            token = target.token,
            method = "GET",
            body = null,
        )
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        val artifacts = JSONObject(response.body).optJSONArray("artifacts") ?: return emptyList()
        return buildList {
            for (index in 0 until artifacts.length()) {
                val artifact = artifacts.optJSONObject(index) ?: continue
                val id = artifact.optLong("id").takeIf { it > 0L } ?: continue
                val name = artifact.optString("name").ifBlank { "artifact-$id" }
                add(
                    ArtifactInfo(
                        id = id,
                        name = name,
                        sizeInBytes = artifact.optLong("size_in_bytes", 0L),
                        expired = artifact.optBoolean("expired", false),
                        archiveDownloadUrl = artifact.optString("archive_download_url").ifBlank {
                            artifactDownloadUrl(target.owner, target.repo, id)
                        },
                    ),
                )
            }
        }
    }

    fun artifactDownloadUrl(
        owner: String,
        repo: String,
        artifactId: Long,
    ): String {
        return "https://api.github.com/repos/$owner/$repo/actions/artifacts/$artifactId/zip"
    }

    fun runLogsUrl(
        owner: String,
        repo: String,
        runId: Long,
    ): String {
        return "https://api.github.com/repos/$owner/$repo/actions/runs/$runId/logs"
    }

    fun downloadTo(
        url: String,
        token: String,
        output: OutputStream,
    ): Long {
        var currentUrl = url
        for (redirect in 0..5) {
            val connection = openDownloadConnection(currentUrl, token)
            val code = connection.responseCode
            if (code in 300..399) {
                val nextUrl = connection.getHeaderField("Location").orEmpty()
                connection.disconnect()
                if (nextUrl.isBlank()) {
                    throw IOException("Download redirect did not include a Location header.")
                }
                currentUrl = URL(URL(currentUrl), nextUrl).toString()
                continue
            }
            if (code !in 200..299) {
                val text = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                connection.disconnect()
                throw IOException(githubErrorMessage(code, text))
            }
            var total = 0L
            connection.inputStream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    total += read
                }
            }
            connection.disconnect()
            return total
        }
        throw IOException("Download redirected too many times.")
    }

    fun oauthLoginUrl(
        clientId: String,
        state: String,
        codeChallenge: String,
    ): String {
        val encodedRedirect = URLEncoder.encode(GITHUB_OAUTH_REDIRECT_URI, StandardCharsets.UTF_8.name())
        val encodedScope = URLEncoder.encode("repo workflow", StandardCharsets.UTF_8.name())
        val encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8.name())
        val encodedChallenge = URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8.name())
        return "https://github.com/login/oauth/authorize?client_id=${clientId.trim()}" +
            "&redirect_uri=$encodedRedirect&scope=$encodedScope&state=$encodedState" +
            "&code_challenge_method=S256&code_challenge=$encodedChallenge"
    }

    fun exchangeOAuthCode(
        clientId: String,
        code: String,
        state: String,
        codeVerifier: String,
    ): OAuthTokenResult {
        val response = request(
            url = "https://github.com/login/oauth/access_token",
            token = "",
            method = "POST",
            body = formBody(
                "client_id" to clientId.trim(),
                "code" to code,
                "redirect_uri" to GITHUB_OAUTH_REDIRECT_URI,
                "state" to state,
                "code_verifier" to codeVerifier,
            ),
            githubApi = false,
            contentType = FORM_CONTENT_TYPE,
        )
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        val root = JSONObject(response.body)
        val error = root.optString("error")
        if (error.isNotBlank()) {
            throw IOException(root.optString("error_description").ifBlank { error })
        }
        val accessToken = root.optString("access_token")
        if (accessToken.isBlank()) {
            throw IOException("GitHub did not return an access token.")
        }
        return OAuthTokenResult(
            accessToken = accessToken,
            scope = root.optString("scope"),
            tokenType = root.optString("token_type"),
        )
    }

    fun requestDeviceCode(clientId: String): DeviceCodeResult {
        val response = request(
            url = "https://github.com/login/device/code",
            token = "",
            method = "POST",
            body = formBody(
                "client_id" to clientId.trim(),
                "scope" to "repo workflow",
            ),
            githubApi = false,
            contentType = FORM_CONTENT_TYPE,
        )
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        val root = JSONObject(response.body)
        val deviceCode = root.optString("device_code")
        val userCode = root.optString("user_code")
        val verificationUri = root.optString("verification_uri")
        val verificationUriComplete = root.optString("verification_uri_complete")
        if (deviceCode.isBlank() || userCode.isBlank() || verificationUri.isBlank()) {
            throw IOException("GitHub device login did not return a usable code.")
        }
        return DeviceCodeResult(
            deviceCode = deviceCode,
            userCode = userCode,
            verificationUri = verificationUri,
            verificationUriComplete = verificationUriComplete,
            expiresIn = root.optInt("expires_in", 900),
            interval = root.optInt("interval", 5).coerceAtLeast(5),
        )
    }

    fun pollDeviceToken(
        clientId: String,
        deviceCode: String,
    ): OAuthTokenResult {
        val response = request(
            url = "https://github.com/login/oauth/access_token",
            token = "",
            method = "POST",
            body = formBody(
                "client_id" to clientId.trim(),
                "device_code" to deviceCode,
                "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
            ),
            githubApi = false,
            contentType = FORM_CONTENT_TYPE,
        )
        if (response.code !in 200..299) {
            throw IOException(githubErrorMessage(response.code, response.body))
        }
        val root = JSONObject(response.body)
        val error = root.optString("error")
        if (error.isNotBlank()) {
            throw OAuthPendingException(error, root.optString("error_description"))
        }
        val accessToken = root.optString("access_token")
        if (accessToken.isBlank()) {
            throw IOException("GitHub did not return an access token.")
        }
        return OAuthTokenResult(
            accessToken = accessToken,
            scope = root.optString("scope"),
            tokenType = root.optString("token_type"),
        )
    }

    private fun dispatchOnce(
        form: BuilderForm,
        returnRunDetails: Boolean,
    ): DispatchResult {
        val previousRunId = latestWorkflowRun(form)?.runId
        val url = "https://api.github.com/repos/${form.owner}/${form.repo}/actions/workflows/${workflowId(form.workflowFile)}/dispatches"
        val payload = JSONObject()
            .put("ref", form.ref)
            .put("inputs", form.toWorkflowInputs())
        if (returnRunDetails) {
            payload.put("return_run_details", true)
        }

        val response = request(
            url = url,
            token = form.token,
            method = "POST",
            body = payload.toString(),
        )
        if (response.code in 200..299) {
            if (response.body.isNotBlank()) {
                parseRunDetails(response.body, form)?.let { return it }
            }
            return waitForLatestWorkflowRun(form, previousRunId) ?: DispatchResult(htmlUrl = workflowUrl(form))
        }

        if (returnRunDetails && response.code == 422 && response.body.contains("return_run_details", ignoreCase = true)) {
            throw ReturnRunDetailsUnsupportedException()
        }
        throw IOException(githubErrorMessage(response.code, response.body))
    }

    private fun latestWorkflowRun(form: BuilderForm): DispatchResult? {
        return latestWorkflowRun(form, branchFilter = true)
            ?: latestWorkflowRun(form, branchFilter = false)
    }

    private fun latestWorkflowRun(
        form: BuilderForm,
        branchFilter: Boolean,
    ): DispatchResult? {
        val branch = URLEncoder.encode(form.ref, StandardCharsets.UTF_8.name())
        val branchQuery = if (branchFilter) "branch=$branch&" else ""
        val url = "https://api.github.com/repos/${form.owner}/${form.repo}/actions/workflows/${workflowId(form.workflowFile)}/runs" +
            "?${branchQuery}event=workflow_dispatch&per_page=1"
        val response = request(url = url, token = form.token, method = "GET", body = null)
        if (response.code !in 200..299 || response.body.isBlank()) return null
        val runs = JSONObject(response.body).optJSONArray("workflow_runs") ?: return null
        val run = runs.optJSONObject(0) ?: return null
        return parseRunObject(run)
    }

    private fun waitForLatestWorkflowRun(
        form: BuilderForm,
        previousRunId: Long?,
    ): DispatchResult? {
        repeat(10) { attempt ->
            latestWorkflowRun(form)?.let { run ->
                val runId = run.runId
                if (previousRunId == null || (runId != null && runId > previousRunId)) {
                    return run
                }
            }
            sleep(1500L + attempt * 500L)
        }
        return null
    }

    private fun parseRunDetails(
        body: String,
        form: BuilderForm,
    ): DispatchResult? {
        val root = JSONObject(body)
        val run = root.optJSONObject("workflow_run")
            ?: root.optJSONObject("run")
            ?: root
        parseRunObject(run)?.let { return it }

        val runId = root.optLong("workflow_run_id").takeIf { it > 0L }
            ?: root.optLong("run_id").takeIf { it > 0L }
        return if (runId != null) {
            DispatchResult(
                htmlUrl = "https://github.com/${form.owner}/${form.repo}/actions/runs/$runId",
                runId = runId,
                status = root.optString("status").takeIf { it.isNotBlank() },
            )
        } else {
            DispatchResult(htmlUrl = workflowUrl(form))
        }
    }

    private fun parseRunObject(run: JSONObject): DispatchResult? {
        val htmlUrl = run.optString("html_url").takeIf { it.isNotBlank() } ?: return null
        val runId = run.optLong("id").takeIf { it > 0L }
        val status = run.optString("status").takeIf { it.isNotBlank() }
        return DispatchResult(htmlUrl = htmlUrl, runId = runId, status = status)
    }

    private fun parseWorkflowRunInfo(run: JSONObject): WorkflowRunInfo? {
        val id = run.optLong("id").takeIf { it > 0L } ?: return null
        return WorkflowRunInfo(
            id = id,
            htmlUrl = run.optString("html_url"),
            status = run.optString("status"),
            conclusion = run.optString("conclusion"),
            name = run.optString("name"),
            createdAt = run.optString("created_at"),
            updatedAt = run.optString("updated_at"),
        )
    }

    private fun BuilderForm.toWorkflowInputs(): JSONObject {
        val workflow = workflowFile.substringAfterLast('/').lowercase()
        return when (workflow) {
            "gki-main.yml" -> mainGkiWorkflowInputs()
            "gki-abk-main.yml" -> abkMainWorkflowInputs()
            "gki-oneplus-realme.yml" -> onePlusWorkflowInputs()
            else -> customGkiWorkflowInputs()
        }
    }

    private fun BuilderForm.customGkiWorkflowInputs(): JSONObject {
        return JSONObject()
            .put("android_version", androidVersion)
            .put("kernel_version", kernelVersion)
            .put("sublevel", sublevel)
            .put("os_patch_level", osPatchLevel)
            .put("feature_set", featureSet)
            .put("use_repo", useRepo)
            .put("use_latest_susfs", useLatestSusfs)
            .put("build_bypass", buildBypass)
            .put("build_time", buildTime)
            .put("use_zram", useZram)
            .put("zram_full_algo", zramFullAlgo)
            .put("zram_extra_algos", zramExtraAlgos)
            .put("use_ntsync", useNtsync)
            .put("use_networking", useNetworking)
            .put("virtualization_support", virtualizationSupport)
            .put("use_bbg", useBbg)
            .put("use_ddk", useDdk)
            .put("use_kpm", useKpm)
            .put("use_rekernel", useRekernel)
            .put("upload_aux_artifacts", uploadAuxArtifacts)
            .put("oplus_patch_mode", oplusPatchMode)
            .put("oplus_reference_ref", oplusReferenceRef)
    }

    private fun BuilderForm.mainGkiWorkflowInputs(): JSONObject {
        return JSONObject()
            .put("kernel_build_version", kernelBuildVersion())
            .put("feature_set", featureSet)
            .put("os_patch_level", osPatchLevel)
            .put("use_repo", useRepo)
            .put("use_latest_susfs", useLatestSusfs)
            .put("build_bypass", buildBypass)
    }

    private fun BuilderForm.abkMainWorkflowInputs(): JSONObject {
        return mainGkiWorkflowInputs()
            .put("build_time", buildTime)
            .put("use_zram", useZram)
            .put("zram_full_algo", zramFullAlgo)
            .put("zram_extra_algos", zramExtraAlgos)
            .put("use_ntsync", useNtsync)
            .put("use_networking", useNetworking)
            .put("virtualization_support", virtualizationSupport)
            .put("use_bbg", useBbg)
            .put("use_ddk", useDdk)
            .put("use_kpm", useKpm)
            .put("use_rekernel", useRekernel)
            .put("upload_aux_artifacts", uploadAuxArtifacts)
            .put("oplus_patch_mode", oplusPatchMode)
            .put("oplus_reference_ref", oplusReferenceRef)
    }

    private fun BuilderForm.onePlusWorkflowInputs(): JSONObject {
        return JSONObject()
            .put("target_chip", onePlusTargetChip())
            .put("feature_set", featureSet)
            .put("os_patch_level", osPatchLevel)
            .put("use_repo", useRepo)
            .put("use_latest_susfs", useLatestSusfs)
            .put("build_bypass", buildBypass)
            .put("oplus_patch_mode", oplusPatchMode)
            .put("oplus_reference_ref", oplusReferenceRef)
    }

    private fun BuilderForm.kernelBuildVersion(): String {
        return "$androidVersion-$kernelVersion"
    }

    private fun BuilderForm.onePlusTargetChip(): String {
        return when ("$androidVersion-$kernelVersion") {
            "android14-6.1" -> "sm8650"
            "android15-6.6" -> "sm8750"
            "android16-6.12" -> "sm8850"
            else -> "sm8750"
        }
    }

    private fun parseRepositoryAccess(repo: JSONObject): RepositoryAccessInfo {
        val permissions = repo.optJSONObject("permissions") ?: JSONObject()
        val owner = repo.optJSONObject("owner")?.optString("login").orEmpty()
        val name = repo.optString("name")
        return RepositoryAccessInfo(
            owner = owner,
            name = name,
            fullName = repo.optString("full_name").ifBlank { "$owner/$name" },
            defaultBranch = repo.optString("default_branch").ifBlank { DEFAULT_REF },
            canPush = permissions.optBoolean("push", false),
            canMaintain = permissions.optBoolean("maintain", false),
            canAdmin = permissions.optBoolean("admin", false),
        )
    }

    private fun BuilderForm.withRepoAccess(access: RepositoryAccessInfo): BuilderForm {
        return copy(
            owner = access.owner.ifBlank { owner },
            repo = access.name.ifBlank { repo },
            ref = ref.ifBlank { access.defaultBranch },
        )
    }

    private fun request(
        url: String,
        token: String,
        method: String,
        body: String?,
        githubApi: Boolean = true,
        contentType: String = JSON_CONTENT_TYPE,
    ): HttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("Accept", if (githubApi) "application/vnd.github+json" else "application/json")
            if (githubApi) {
                setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
            }
            if (token.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            setRequestProperty("User-Agent", "ApkeSU-GKI-Builder")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType)
            }
        }
        if (body != null) {
            connection.outputStream.use { output ->
                output.write(body.toByteArray(StandardCharsets.UTF_8))
            }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        return HttpResponse(code, text)
    }

    private fun openDownloadConnection(
        url: String,
        token: String,
    ): HttpURLConnection {
        val target = URL(url)
        val isGithubApi = target.host.equals("api.github.com", ignoreCase = true)
        return (target.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = false
            connectTimeout = 20_000
            readTimeout = 120_000
            setRequestProperty("User-Agent", "ApkeSU-GKI-Builder")
            if (isGithubApi) {
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            } else {
                setRequestProperty("Accept", "application/octet-stream")
            }
        }
    }

    private fun githubErrorMessage(
        code: Int,
        body: String,
    ): String {
        val message = runCatching { JSONObject(body).optString("message") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
        return if (message != null) {
            "GitHub API $code: $message"
        } else {
            "GitHub API $code"
        }
    }

    private fun isNoUsableWorkflow(error: Throwable): Boolean {
        return error.message?.contains(NO_USABLE_GKI_WORKFLOW, ignoreCase = true) == true
    }

    private fun formBody(vararg values: Pair<String, String>): String {
        return values.joinToString("&") { (key, value) ->
            "${urlEncode(key)}=${urlEncode(value)}"
        }
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    }

    private fun urlEncodePath(path: String): String {
        return path.split('/').joinToString("/") { urlEncode(it) }
    }

    private fun workflowId(workflowFile: String): String {
        return urlEncode(workflowFile.substringAfterLast('/').ifBlank { workflowFile })
    }

    private fun selectBestWorkflow(
        form: BuilderForm,
        workflows: List<WorkflowInfo>,
    ): WorkflowInfo? {
        return selectBestWorkflowFrom(form, workflows.filter { it.state.isBlank() || it.state == "active" })
            ?: selectBestWorkflowFrom(form, workflows)
    }

    private fun selectBestWorkflowFrom(
        form: BuilderForm,
        workflows: List<WorkflowInfo>,
    ): WorkflowInfo? {
        val byFile = workflows.associateBy { it.fileName.lowercase() }
        val candidates = listOf(
            form.workflowFile.substringAfterLast('/'),
            DEFAULT_WORKFLOW,
            "gki-custom.yml",
            "gki-abk-main.yml",
            "gki-main.yml",
            "gki-oneplus-realme.yml",
        ).map { it.lowercase() }.distinct()
        candidates.forEach { candidate ->
            byFile[candidate]?.let { return it }
        }
        return workflows.firstOrNull { workflow ->
            val file = workflow.fileName.lowercase()
            file.startsWith("gki-") && file.endsWith(".yml") && "prepare" !in file && "build" !in file
        }
    }
}

private const val JSON_CONTENT_TYPE = "application/json; charset=utf-8"
private const val FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8"
private const val NO_REPOSITORY_BUILD_RIGHTS =
    "Current GitHub account does not have build rights for this repository. Use your own fork or repository."
private const val NO_USABLE_GKI_WORKFLOW =
    "No usable GKI workflow found in this repository."

private data class HttpResponse(
    val code: Int,
    val body: String,
)

private data class RepositoryFile(
    val sha: String,
    val decodedContent: ByteArray,
)

private class ReturnRunDetailsUnsupportedException : IOException()

class OAuthPendingException(
    val code: String,
    description: String,
) : IOException(description.ifBlank { code })
