package com.nexussphere.app.portfolio

import com.nexussphere.data.repository.DemoPortfolioRepository
import com.nexussphere.data.repository.PortfolioRepository
import com.nexussphere.domain.Portfolio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PortfolioState {
    object Loading : PortfolioState()
    data class Success(val portfolio: Portfolio, val isDemo: Boolean) : PortfolioState()
    data class Error(val message: String) : PortfolioState()
}

/**
 * Platform-neutral state holder — no lifecycle dependency.
 * Screens observe [state] and call [load] / [refresh].
 */
class PortfolioViewModel(
    private val repository: PortfolioRepository = DemoPortfolioRepository(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _state = MutableStateFlow<PortfolioState>(PortfolioState.Loading)
    val state: StateFlow<PortfolioState> = _state

    /** Load portfolio. Uses demo data when userId/userSecret are blank. */
    fun load(userId: String = "", userSecret: String = "") {
        _state.value = PortfolioState.Loading
        scope.launch {
            val isDemo = userId.isBlank() || userSecret.isBlank()
            repository.getPortfolio(userId, userSecret)
                .onSuccess { _state.value = PortfolioState.Success(it, isDemo) }
                .onFailure { _state.value = PortfolioState.Error(it.message ?: "Unknown error") }
        }
    }

    fun refresh(userId: String, userSecret: String) = load(userId, userSecret)
}
