package com.nexussphere.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class RiskOutcome { APPROVED, REJECTED, REQUIRES_REVIEW }

/**
 * Deterministic server-side risk policy result.
 * AI agents may NOT override a REJECTED decision.
 */
@Serializable
data class RiskDecision(
    val outcome: RiskOutcome,
    val rejectionReasons: List<String>,
    val warnings: List<String>,
    val evaluatedAt: Instant,
    val policyVersion: String,
)

/** All server-side kill-switch dimensions. */
@Serializable
data class KillSwitchState(
    val global: Boolean,
    val model: Boolean,
    val strategy: Boolean,
    val asset: Boolean,
    val broker: Boolean,
    val news: Boolean,
    val dataQuality: Boolean,
    val volatility: Boolean,
    val drawdown: Boolean,
    val latency: Boolean,
) {
    val anyActive: Boolean get() =
        global || model || strategy || asset || broker ||
        news || dataQuality || volatility || drawdown || latency

    fun activeNames(): List<String> = buildList {
        if (global)      add("GLOBAL")
        if (model)       add("MODEL")
        if (strategy)    add("STRATEGY")
        if (asset)       add("ASSET")
        if (broker)      add("BROKER")
        if (news)        add("NEWS")
        if (dataQuality) add("DATA_QUALITY")
        if (volatility)  add("VOLATILITY")
        if (drawdown)    add("DRAWDOWN")
        if (latency)     add("LATENCY")
    }

    companion object {
        fun allClear(): KillSwitchState = KillSwitchState(
            global = false, model = false, strategy = false,
            asset = false, broker = false, news = false,
            dataQuality = false, volatility = false,
            drawdown = false, latency = false,
        )
    }
}
