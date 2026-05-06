package com.dantech.dreams.ui.feature.nav

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DreamsBottomBar(
    currentTab: TabKey,
    onTabSelected: (TabKey) -> Unit,
) {
    NavigationBar {
        TabKey.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == currentTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                alwaysShowLabel = true,
            )
        }
    }
}
