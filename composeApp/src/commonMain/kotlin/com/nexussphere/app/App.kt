package com.nexussphere.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Top-level navigation destinations. Phase 1: shell only. */
enum class Screen(val label: String) {
    DASHBOARD("Dashboard"),
    SCANNER("Scanner"),
    POSITIONS("Positions"),
    RESEARCH("Research"),
}

/** NexusSphere design tokens — dark terminal theme. */
object NexusTheme {
    val background  = Color(0xFF0A0E1A)
    val surface     = Color(0xFF0F1525)
    val surfaceAlt  = Color(0xFF141B2D)
    val accent      = Color(0xFF00D4FF)
    val profit      = Color(0xFF00E676)
    val loss        = Color(0xFFFF1744)
    val warning     = Color(0xFFFFAB00)
    val text        = Color(0xFFE0E8F0)
    val textMuted   = Color(0xFF6B7FA3)
}

@Composable
fun NexusSphereApp() {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background   = NexusTheme.background,
            surface      = NexusTheme.surface,
            primary      = NexusTheme.accent,
            onBackground = NexusTheme.text,
            onSurface    = NexusTheme.text,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NexusTheme.background)
        ) {
            NexusTopBar(currentScreen)
            Row(modifier = Modifier.fillMaxSize()) {
                NexusSideNav(
                    current  = currentScreen,
                    onSelect = { currentScreen = it },
                )
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when (currentScreen) {
                        Screen.DASHBOARD  -> DashboardScreen()
                        Screen.SCANNER    -> ScannerScreen()
                        Screen.POSITIONS  -> PositionsScreen()
                        Screen.RESEARCH   -> ResearchScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun NexusTopBar(screen: Screen) {
    Surface(color = NexusTheme.surface, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("NEXUS SPHERE", color = NexusTheme.accent, style = MaterialTheme.typography.titleMedium)
            Text(screen.label, color = NexusTheme.textMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun NexusSideNav(current: Screen, onSelect: (Screen) -> Unit) {
    Surface(
        color     = NexusTheme.surfaceAlt,
        modifier  = Modifier.width(180.dp).fillMaxHeight(),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Screen.entries.forEach { screen ->
                val selected = screen == current
                TextButton(
                    onClick  = { onSelect(screen) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.textButtonColors(
                        contentColor = if (selected) NexusTheme.accent else NexusTheme.textMuted
                    ),
                ) {
                    Text(screen.label)
                }
            }
        }
    }
}

// ── Phase 1 placeholder screens ──────────────────────────────────────────────

@Composable
fun DashboardScreen() {
    PlaceholderScreen("Dashboard — portfolio summary coming in Phase 2")
}

@Composable
fun ScannerScreen() {
    PlaceholderScreen("Signal Scanner — live market data coming in Phase 3")
}

@Composable
fun PositionsScreen() {
    PlaceholderScreen("Positions — SnapTrade integration coming in Phase 2")
}

@Composable
fun ResearchScreen() {
    PlaceholderScreen("Research OS — hypothesis tracking coming in Phase 6")
}

@Composable
private fun PlaceholderScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(message, color = NexusTheme.textMuted, style = MaterialTheme.typography.bodyLarge)
    }
}
