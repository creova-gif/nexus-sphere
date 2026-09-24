package com.nexussphere.risk

import com.nexussphere.domain.KillSwitchState
import com.nexussphere.domain.Money
import com.nexussphere.domain.OrderIntent

/**
 * Kotlin mirror of guardrails.py — deterministic server-side order policy.
 *
 * The Python server is the authority for live execution.
 * This class provides the same logic for:
 *   - Client-side pre-validation (advisory, not authoritative)
 *   - Unit testing policy in the shared module
 *   - Future Ktor server adoption
 *
 * AI agents may NOT bypass a REJECTED decision from this guard.
 */
object OrderGuard {

    data class Config(
        val maxOrderCad: Money,
        val killSwitch: KillSwitchState,
    )

    sealed class Result {
        object Approved : Result()
        data class Rejected(val reasons: List<String>) : Result()
    }

    fun evaluate(intent: OrderIntent, marketPrice: Money, config: Config): Result {
        val reasons = mutableListOf<String>()

        // Kill switch — always checked first
        if (config.killSwitch.anyActive) {
            val active = config.killSwitch.activeNames().joinToString()
            reasons += "Kill switch active: $active — all orders blocked"
        }

        // Units
        if (intent.units <= 0.0) {
            reasons += "Units must be positive (got ${intent.units})"
        }

        // Price
        if (marketPrice.isNegative() || marketPrice.isZero()) {
            reasons += "Market price must be positive"
        }

        // Notional cap
        if (intent.units > 0.0 && marketPrice.isPositive()) {
            val notional = marketPrice * intent.units
            if (notional > config.maxOrderCad) {
                reasons += "Order notional $notional exceeds maximum ${config.maxOrderCad}"
            }
        }

        return if (reasons.isEmpty()) Result.Approved else Result.Rejected(reasons)
    }
}
