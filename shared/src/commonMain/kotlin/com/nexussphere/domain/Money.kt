package com.nexussphere.domain

import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Decimal-safe money representation.
 * Stored as cents (Long) to avoid floating-point rounding in financial calculations.
 * Use [Money.ofCad] / [Money.ofUsd] to construct; [toDouble] only for display.
 */
@Serializable
data class Money(
    val cents: Long,
    val currency: Currency,
) : Comparable<Money> {

    init {
        require(currency != Currency.UNKNOWN) { "Currency must be explicit" }
    }

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(cents = cents + other.cents)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(cents = cents - other.cents)
    }

    operator fun times(factor: Double): Money =
        copy(cents = (cents * factor).toLong())

    operator fun unaryMinus(): Money = copy(cents = -cents)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return cents.compareTo(other.cents)
    }

    fun isPositive(): Boolean = cents > 0
    fun isNegative(): Boolean = cents < 0
    fun isZero(): Boolean = cents == 0L
    fun abs(): Money = copy(cents = abs(cents))

    fun toDouble(): Double = cents / 100.0
    override fun toString(): String = "${currency.symbol}${"%.2f".format(toDouble())}"

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "Currency mismatch: $currency vs ${other.currency}"
        }
    }

    companion object {
        fun ofCad(dollars: Double): Money = Money((dollars * 100).toLong(), Currency.CAD)
        fun ofUsd(dollars: Double): Money = Money((dollars * 100).toLong(), Currency.USD)
        fun zeroCad(): Money = Money(0L, Currency.CAD)
        fun zeroUsd(): Money = Money(0L, Currency.USD)
    }
}

@Serializable
enum class Currency(val symbol: String) {
    CAD("$"),
    USD("$"),
    UNKNOWN("?"),
}

/** CAD/USD exchange rate used throughout the platform. */
const val CAD_USD_RATE: Double = 0.736
