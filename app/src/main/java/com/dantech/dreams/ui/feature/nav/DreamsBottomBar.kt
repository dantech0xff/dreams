package com.dantech.dreams.ui.feature.nav

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DreamsBottomBar(topLevel: TopLevelBackStack) {
    NavigationBar {
        TabKey.entries.forEach { tab ->
            val selected = topLevel.topLevelKey == tab.root
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (selected) topLevel.popToRoot() else topLevel.switchTopLevel(tab.root)
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                alwaysShowLabel = true,
            )
        }
    }
}
