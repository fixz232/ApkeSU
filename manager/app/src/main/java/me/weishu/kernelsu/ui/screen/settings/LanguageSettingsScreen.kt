package me.weishu.kernelsu.ui.screen.settings

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.material.SegmentedColumn
import me.weishu.kernelsu.ui.component.material.SegmentedRadioItem
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.AppLanguageManager
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LanguageSettingsScreen() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }
    val saveFailedMessage = stringResource(R.string.settings_language_save_failed)
    var selectedLanguage by remember(context) {
        mutableStateOf(AppLanguageManager.getSelectedLanguage(context))
    }

    Scaffold(
        containerColor = Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_language),
                color = Color.Transparent,
                titleColor = MiuixTheme.colorScheme.onSurface,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MiuixTheme.colorScheme.onBackground,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        SegmentedColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            title = stringResource(R.string.settings_language_summary),
            content = AppLanguageManager.supportedLanguages.map { language ->
                {
                    val selected = language.languageTag == selectedLanguage.languageTag
                    SegmentedRadioItem(
                        title = stringResource(language.displayNameRes),
                        summary = if (selected) stringResource(R.string.language_selected) else null,
                        selected = selected,
                        onClick = {
                            if (selected) return@SegmentedRadioItem
                            if (!AppLanguageManager.setSelectedLanguage(context, language.languageTag)) {
                                Toast.makeText(
                                    context,
                                    saveFailedMessage,
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@SegmentedRadioItem
                            }
                            selectedLanguage = language
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                activity?.recreate()
                            }
                        },
                    )
                }
            },
        )
    }
}
