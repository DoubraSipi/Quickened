package com.quickened.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed class DrawerAction(val symbol: String, val label: String) {
    data object Home : DrawerAction("✦", "Today's Verse")
    data object Begin : DrawerAction("▶", "Begin moment")
    data object Fresh : DrawerAction("✨", "Fresh word")
    data object History : DrawerAction("◷", "History")
    data object Favorites : DrawerAction("★", "Favorites")
    data object Export : DrawerAction("⤴", "Export moments")
    data object Reminder : DrawerAction("◔", "Reminder")
    data object Settings : DrawerAction("⚙", "Settings")
    data object Voices : DrawerAction("♪", "Download voices")
    data object ShareApp : DrawerAction("♡", "Share Quickened")
}

@Composable
fun QuickenedDrawer(
    headerLine: String,
    onAction: (DrawerAction) -> Unit
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Quickened",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SerifHeadings),
                color = MaterialTheme.colorScheme.primary
            )
            Text(headerLine, style = MaterialTheme.typography.bodySmall)
        }
        DrawerSection("Moments", listOf(
            DrawerAction.Home, DrawerAction.Begin, DrawerAction.Fresh, DrawerAction.History
        ), onAction)
        HorizontalDivider()
        DrawerSection("Yours", listOf(
            DrawerAction.Favorites, DrawerAction.Export, DrawerAction.Reminder
        ), onAction)
        HorizontalDivider()
        DrawerSection("App", listOf(
            DrawerAction.Settings, DrawerAction.Voices, DrawerAction.ShareApp
        ), onAction)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DrawerSection(title: String, items: List<DrawerAction>, onAction: (DrawerAction) -> Unit) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary
    )
    items.forEach { item ->
        NavigationDrawerItem(
            label = { Text(item.label) },
            icon = { Text(item.symbol) },
            selected = false,
            onClick = { onAction(item) },
            modifier = Modifier.padding(horizontal = 12.dp),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun DrawerTopBar(title: String, onMenu: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        androidx.compose.material3.IconButton(onClick = onMenu) {
            Text("☰", style = MaterialTheme.typography.titleLarge)
        }
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifHeadings))
    }
}
