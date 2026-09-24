package com.nexussphere.app.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexussphere.app.NexusTheme
import com.nexussphere.domain.Signal
import com.nexussphere.domain.SignalDirection

@Composable
fun ScannerScreen(viewModel: ScannerViewModel = remember { ScannerViewModel() }) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Signal Scanner", color = NexusTheme.text, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { viewModel.refresh() }) {
                Text("Refresh", color = NexusTheme.accent, fontSize = 13.sp)
            }
        }

        when (val s = state) {
            is ScannerState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NexusTheme.accent)
                }
            }
            is ScannerState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = NexusTheme.loss, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text("Retry", color = NexusTheme.accent)
                        }
                    }
                }
            }
            is ScannerState.Success -> {
                if (s.isDemo) {
                    DemoBanner()
                    Spacer(Modifier.height(8.dp))
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.signals) { signal -> SignalCard(signal) }
                }
            }
        }
    }
}

@Composable
private fun DemoBanner() {
    Surface(
        color = NexusTheme.warning.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "DEMO — signals computed from static demo data, not live market prices",
            color = NexusTheme.warning,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SignalCard(signal: Signal) {
    Surface(
        color = NexusTheme.surfaceAlt,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    signal.symbol.value,
                    color = NexusTheme.text,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DirectionChip(signal.direction)
                    Text(
                        "${signal.score}",
                        color = scoreColor(signal.score),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            ScoreBar(score = signal.score)

            Spacer(Modifier.height(8.dp))

            ComponentGrid(signal)
        }
    }
}

@Composable
private fun ScoreBar(score: Int) {
    val fraction = score / 100f
    val color = scoreColor(score)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(NexusTheme.surface, RoundedCornerShape(3.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .background(color, RoundedCornerShape(3.dp)),
        )
        // profit gate marker at 65%
        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(6.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(Modifier.width(1.dp).fillMaxHeight().background(NexusTheme.warning))
        }
    }
}

@Composable
private fun ComponentGrid(signal: Signal) {
    val components = listOf(
        "RSI" to signal.rsiScore,
        "MACD" to signal.macdScore,
        "Trend" to signal.trendScore,
        "Stage" to signal.stageScore,
        "DCF" to signal.dcfScore,
        "Funds" to signal.fundamentalsScore,
        "Mom" to signal.momentumScore,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        components.forEach { (label, pts) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, color = NexusTheme.textMuted, fontSize = 9.sp)
                Text("$pts", color = NexusTheme.text, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun DirectionChip(direction: SignalDirection) {
    val (label, bg) = when (direction) {
        SignalDirection.BUY  -> "BUY"  to NexusTheme.profit
        SignalDirection.SELL -> "SELL" to NexusTheme.loss
        SignalDirection.HOLD -> "HOLD" to NexusTheme.warning
    }
    Surface(
        color = bg.copy(alpha = 0.18f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            label,
            color = bg,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 65 -> Color(0xFF00E676) // profit green
    score >= 45 -> Color(0xFFFFAB00) // warning amber
    else        -> Color(0xFFFF1744) // loss red
}
