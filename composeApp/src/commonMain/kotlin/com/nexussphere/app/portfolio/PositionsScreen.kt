package com.nexussphere.app.portfolio

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexussphere.app.NexusTheme
import com.nexussphere.domain.Portfolio
import com.nexussphere.domain.Position

@Composable
fun PositionsScreen(
    viewModel: PortfolioViewModel = remember { PortfolioViewModel() },
    userId: String = "",
    userSecret: String = "",
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(userId, userSecret) {
        viewModel.load(userId, userSecret)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
            is PortfolioState.Loading -> LoadingView()
            is PortfolioState.Error   -> ErrorView(s.message) { viewModel.load(userId, userSecret) }
            is PortfolioState.Success -> PortfolioView(s.portfolio, s.isDemo)
        }
    }
}

@Composable
private fun PortfolioView(portfolio: Portfolio, isDemo: Boolean) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding      = PaddingValues(bottom = 16.dp),
    ) {
        item { PortfolioHeader(portfolio, isDemo) }
        items(portfolio.positions) { position ->
            PositionRow(position)
        }
    }
}

@Composable
private fun PortfolioHeader(portfolio: Portfolio, isDemo: Boolean) {
    Surface(
        color  = NexusTheme.surfaceAlt,
        shape  = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text  = portfolio.accountName,
                        color = NexusTheme.textMuted,
                        fontSize = 12.sp,
                    )
                    Text(
                        text       = portfolio.totalEquityCad.toString(),
                        color      = NexusTheme.text,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                if (isDemo) {
                    DemoBadge()
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StatPill(
                    label = "P&L",
                    value = portfolio.totalUnrealisedPnlCad.toString(),
                    color = if (portfolio.isOverallProfit) NexusTheme.profit else NexusTheme.loss,
                )
                StatPill(
                    label = "Return",
                    value = "${"%.2f".format(portfolio.totalUnrealisedPnlPct * 100)}%",
                    color = if (portfolio.isOverallProfit) NexusTheme.profit else NexusTheme.loss,
                )
                StatPill(
                    label = "Cash",
                    value = portfolio.cashCad.toString(),
                    color = NexusTheme.textMuted,
                )
            }
        }
    }
}

@Composable
private fun PositionRow(position: Position) {
    Surface(
        color  = NexusTheme.surface,
        shape  = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Symbol + units
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = position.symbol.value,
                    color      = NexusTheme.text,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 15.sp,
                )
                Text(
                    text     = "${"%.4f".format(position.units)} units",
                    color    = NexusTheme.textMuted,
                    fontSize = 11.sp,
                )
            }

            // Current price
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(
                    text       = position.currentPriceCad.toString(),
                    color      = NexusTheme.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 14.sp,
                )
                Text(
                    text     = "price",
                    color    = NexusTheme.textMuted,
                    fontSize = 10.sp,
                )
            }

            // Market value
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(
                    text       = position.marketValueCad.toString(),
                    color      = NexusTheme.text,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp,
                )
                Text(
                    text     = "mkt val",
                    color    = NexusTheme.textMuted,
                    fontSize = 10.sp,
                )
            }

            // P&L
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                val pnlColor = if (position.isProfit) NexusTheme.profit else NexusTheme.loss
                Text(
                    text       = position.unrealisedPnlCad.toString(),
                    color      = pnlColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 13.sp,
                )
                Text(
                    text     = "${"%.2f".format(position.unrealisedPnlPct * 100)}%",
                    color    = pnlColor,
                    fontSize = 11.sp,
                )
            }

            // Signal score badge (if available)
            position.signal?.let { sig ->
                Spacer(modifier = Modifier.width(8.dp))
                SignalBadge(sig.score, sig.profitGated)
            }
        }
    }
}

@Composable
private fun SignalBadge(score: Int, gated: Boolean) {
    val color = when {
        gated        -> NexusTheme.profit
        score >= 50  -> NexusTheme.warning
        else         -> NexusTheme.loss
    }
    Box(
        modifier          = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment  = Alignment.Center,
    ) {
        Text(
            text       = score.toString(),
            color      = color,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize   = 12.sp,
        )
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column {
        Text(text = label, color = NexusTheme.textMuted, fontSize = 10.sp)
        Text(
            text       = value,
            color      = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 14.sp,
        )
    }
}

@Composable
private fun DemoBadge() {
    Box(
        modifier         = Modifier
            .background(NexusTheme.warning.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text("DEMO", color = NexusTheme.warning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = NexusTheme.accent)
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = NexusTheme.loss, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors  = ButtonDefaults.buttonColors(containerColor = NexusTheme.accent),
        ) { Text("Retry") }
    }
}
