package com.nexussphere.models

object TechnicalIndicators {

    fun sma(prices: List<Double>, period: Int): Double {
        if (prices.size < period) return Double.NaN
        return prices.takeLast(period).average()
    }

    fun ema(prices: List<Double>, period: Int): Double {
        if (prices.size < period) return Double.NaN
        val k = 2.0 / (period + 1)
        var ema = prices.take(period).average()
        for (price in prices.drop(period)) {
            ema = price * k + ema * (1 - k)
        }
        return ema
    }

    fun rsi(prices: List<Double>, period: Int = 14): Double {
        if (prices.size < period + 1) return Double.NaN
        val changes = prices.zipWithNext { a, b -> b - a }
        val gains = changes.map { if (it > 0) it else 0.0 }
        val losses = changes.map { if (it < 0) -it else 0.0 }

        var avgGain = gains.take(period).average()
        var avgLoss = losses.take(period).average()

        for (i in period until changes.size) {
            avgGain = (avgGain * (period - 1) + gains[i]) / period
            avgLoss = (avgLoss * (period - 1) + losses[i]) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    data class MacdResult(val macdLine: Double, val signalLine: Double, val histogram: Double)

    fun macd(
        prices: List<Double>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9,
    ): MacdResult? {
        if (prices.size < slowPeriod + signalPeriod) return null
        val fastEma = ema(prices, fastPeriod)
        val slowEma = ema(prices, slowPeriod)
        if (fastEma.isNaN() || slowEma.isNaN()) return null
        val macdLine = fastEma - slowEma

        // approximate signal line using last signalPeriod MACD values
        val macdHistory = buildList {
            for (end in slowPeriod..prices.size) {
                val slice = prices.subList(0, end)
                val f = ema(slice, fastPeriod)
                val s = ema(slice, slowPeriod)
                if (!f.isNaN() && !s.isNaN()) add(f - s)
            }
        }
        if (macdHistory.size < signalPeriod) return null
        val signalLine = ema(macdHistory, signalPeriod)
        return MacdResult(macdLine, signalLine, macdLine - signalLine)
    }
}
