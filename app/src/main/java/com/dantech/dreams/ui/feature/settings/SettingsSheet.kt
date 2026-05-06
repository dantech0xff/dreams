package com.dantech.dreams.ui.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dantech.dreams.data.prefs.UserPrefs
import com.dantech.dreams.data.prefs.UserPrefsRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    prefsRepo: UserPrefsRepository = koinInject(),
) {
    val prefs by prefsRepo.prefsFlow.collectAsStateWithLifecycle(initialValue = UserPrefs.DEFAULT)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reduce motion", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Skip transitions and shared element morphs.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = prefs.reducedMotionOverride,
                    onCheckedChange = { v ->
                        scope.launch { prefsRepo.setReducedMotion(v) }
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
