package com.dantech.dreams.ui.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dantech.dreams.data.prefs.UserPrefs
import com.dantech.dreams.data.prefs.UserPrefsRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val GITHUB_URL = "https://github.com/dantech0xff/dreams"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefsRepo: UserPrefsRepository = koinInject(),
) {
    val prefs by prefsRepo.prefsFlow.collectAsStateWithLifecycle(initialValue = UserPrefs.DEFAULT)
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var aboutOpen by remember { mutableStateOf(false) }
    var licenseOpen by remember { mutableStateOf(false) }

    val versionName = remember(ctx) {
        runCatching {
            @Suppress("DEPRECATION")
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        }.getOrNull() ?: "?"
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            DisplaySettingsSection(
                themeMode = prefs.themeMode,
                reducedMotion = prefs.reducedMotionOverride,
                useDynamicColor = prefs.useDynamicColor,
                onThemeMode = { mode -> scope.launch { prefsRepo.setThemeMode(mode) } },
                onReducedMotion = { v -> scope.launch { prefsRepo.setReducedMotion(v) } },
                onDynamicColor = { v -> scope.launch { prefsRepo.setUseDynamicColor(v) } },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader("About")
            LinkRow(title = "App version", trailing = versionName, onClick = null)
            LinkRow(title = "About AGSL", trailing = null, onClick = { aboutOpen = true })
            LinkRow(
                title = "GitHub",
                trailing = "github.com/dantech0xff/dreams",
                onClick = {
                    runCatching {
                        ctx.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )
            LinkRow(title = "License", trailing = "MIT", onClick = { licenseOpen = true })

            Spacer(Modifier.height(24.dp))
        }
    }

    if (aboutOpen) AboutAgslSheet(onDismiss = { aboutOpen = false })
    if (licenseOpen) LicenseSheet(onDismiss = { licenseOpen = false })
}

@Composable
private fun LinkRow(
    title: String,
    trailing: String?,
    onClick: (() -> Unit)?,
) {
    val rowMod = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        modifier = rowMod.padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp)) {
            Text("License", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "MIT — see the LICENSE file in the repository for the full text.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
