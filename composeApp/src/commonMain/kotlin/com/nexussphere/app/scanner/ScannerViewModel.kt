package com.nexussphere.app.scanner

import com.nexussphere.data.repository.DemoMarketDataRepository
import com.nexussphere.data.repository.MarketDataRepository
import com.nexussphere.domain.Signal
import com.nexussphere.domain.Symbol
import com.nexussphere.models.SignalScore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScannerState {
    object Loading : ScannerState()
    data class Success(val signals: List<Signal>, val isDemo: Boolean) : ScannerState()
    data class Error(val message: String) : ScannerState()
}

class ScannerViewModel(
    private val repository: MarketDataRepository = DemoMarketDataRepository(),
    private val isDemo: Boolean = true,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<ScannerState>(ScannerState.Loading)
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            _state.value = ScannerState.Loading
            runCatching {
                val snapshots = repository.getSnapshots(Symbol.TFSA_HOLDINGS)
                snapshots.entries
                    .mapNotNull { (_, result) -> result.getOrNull() }
                    .map { snap -> SignalScore.evaluate(snap) }
                    .sortedByDescending { it.score }
            }.onSuccess { signals ->
                _state.value = ScannerState.Success(signals, isDemo)
            }.onFailure { err ->
                _state.value = ScannerState.Error(err.message ?: "Failed to load signals")
            }
        }
    }
}
