package com.nexussphere.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexussphere.app.auth.AuthScreen
import com.nexussphere.app.portfolio.PortfolioViewModel
import com.nexussphere.app.portfolio.PositionsScreen
import com.nexussphere.app.scanner.ScannerScreen
import com.nexussphere.app.scanner.ScannerViewModel

/** Top-level navigation destinations. */
enum class Screen(val label: String) {
    DASHBOARD("Dashboard"),
    POSITIONS("Positions"),
    SCANNER("Scanner"),
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

/** Session credentials — in-memory only for Phase 2. Phase 3 moves to secure storage. */
data class SessionCreds(val userId: String, val userSecret: String)

@Composable
fun NexusSphereApp() {
    var screen  by remember { mutableStateOf(Screen.DASHBOARD) }
    var creds   by remember { mutableStateOf<SessionCreds?>(null) }
    var showAuth by remember { mutableStateOf(false) }

    val portfolioVm = remember { PortfolioViewModel() }
    val scannerVm   = remember { ScannerViewModel() }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background   = NexusTheme.background,
            surface      = NexusTheme.surface,
            primary      = NexusTheme.accent,
            onBackground = NexusTheme.text,
            onSurface    = NexusTheme.text,
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().background(NexusTheme.background)) {
            if (showAuth) {
                AuthScreen(
                    onConnected = { uid, sec ->
                        creds    = SessionCreds(uid, sec)
                        showAuth = false
                        portfolioVm.load(uid, sec)
                    },
                    onDemoMode = {
                        showAuth = false
                        portfolioVm.load()
                    },
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    NexusTopBar(screen, creds, onConnectClick = { showAuth = true })
                    Row(modifier = Modifier.fillMaxSize()) {
                        NexusSideNav(current = screen, onSelect = { screen = it })
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            when (screen) {
                                Screen.DASHBOARD -> DashboardScreen(onGoToPositions = { screen = Screen.POSITIONS })
                                Screen.POSITIONS -> PositionsScreen(
                                    viewModel    = portfolioVm,
                                    userId       = creds?.userId ?: "",
                                    userSecret   = creds?.userSecret ?: "",
                                )
                                Screen.SCANNER   -> ScannerScreen(viewModel = scannerVm)
                                Screen.RESEARCH  -> ResearchScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NexusTopBar(screen: Screen, creds: SessionCreds?, onConnectClick: () -> Unit) {
    Surface(color = NexusTheme.surface, shadowElevation = 4.dp) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text("NEXUS SPHERE", color = NexusTheme.accent, style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(screen.label, color = NexusTheme.textMuted, style = MaterialTheme.typography.bodyMedium)
                if (creds == null) {
                    TextButton(onClick = onConnectClick) {
                        Text("Connect Broker", color = NexusTheme.accent, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    ConnectedPill()
                }
            }
        }
    }
}

@Composable
private fun ConnectedPill() {
    Surface(
        color = NexusTheme.profit.copy(alpha = 0.15f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    ) {
        Text(
            text     = "● LIVE",
            color    = NexusTheme.profit,
            style    = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun NexusSideNav(current: Screen, onSelect: (Screen) -> Unit) {
    Surface(
        color    = NexusTheme.surfaceAlt,
        modifier = Modifier.width(180.dp).fillMaxHeight(),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Screen.entries.forEach { s ->
                val selected = s == current
                TextButton(
                    onClick  = { onSelect(s) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.textButtonColors(
                        contentColor = if (selected) NexusTheme.accent else NexusTheme.textMuted,
                    ),
                ) { Text(s.label) }
            }
        }
    }
}

// ── Screens ──────────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(onGoToPositions: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Dashboard", color = NexusTheme.text, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onGoToPositions) {
            Text("View Positions →", color = NexusTheme.accent)
        }
        Text("Full dashboard coming in Phase 3 (live market data)", color = NexusTheme.textMuted)
    }
}

@Composable
fun ResearchScreen() = PlaceholderScreen("Research OS — hypothesis tracking in Phase 6")

@Composable
private fun PlaceholderScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = NexusTheme.textMuted, style = MaterialTheme.typography.bodyLarge)
    }
}
