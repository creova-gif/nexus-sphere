package com.nexussphere.risk

import com.nexussphere.domain.Currency
import com.nexussphere.domain.KillSwitchState
import com.nexussphere.domain.Money
import com.nexussphere.domain.OrderAction
import com.nexussphere.domain.OrderIntent
import com.nexussphere.domain.OrderType
import com.nexussphere.domain.Symbol
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OrderGuardTest {

    private val clearKs = KillSwitchState.allClear()
    private val cap2000  = Money.ofCad(2000.0)

    private fun intent(units: Double = 10.0) = OrderIntent(
        symbol     = Symbol("AMD"),
        action     = OrderAction.BUY,
        units      = units,
        limitPrice = null,
        orderType  = OrderType.MARKET,
        strategyId = null,
        signalScore = null,
        createdAt  = Instant.fromEpochSeconds(0),
    )

    private fun config(
        ks: KillSwitchState = clearKs,
        cap: Money = cap2000,
    ) = OrderGuard.Config(maxOrderCad = cap, killSwitch = ks)

    @Test
    fun approvedWhenAllClear() {
        val result = OrderGuard.evaluate(
            intent = intent(units = 10.0),
            marketPrice = Money.ofCad(50.0),
            config = config(),
        )
        assertIs<OrderGuard.Result.Approved>(result)
    }

    @Test
    fun rejectedWhenGlobalKillSwitch() {
        val ks = clearKs.copy(global = true)
        val result = OrderGuard.evaluate(intent(), Money.ofCad(50.0), config(ks = ks))
        val rejected = assertIs<OrderGuard.Result.Rejected>(result)
        assertTrue(rejected.reasons.any { "kill switch" in it.lowercase() })
    }

    @Test
    fun rejectedWhenNotionalExceedsCap() {
        // 41 units × $50 = $2050 > $2000 cap
        val result = OrderGuard.evaluate(
            intent = intent(units = 41.0),
            marketPrice = Money.ofCad(50.0),
            config = config(cap = Money.ofCad(2000.0)),
        )
        assertIs<OrderGuard.Result.Rejected>(result)
    }

    @Test
    fun approvedWhenNotionalAtCap() {
        // 40 × $50 = $2000 — exactly at cap, should pass
        val result = OrderGuard.evaluate(
            intent = intent(units = 40.0),
            marketPrice = Money.ofCad(50.0),
            config = config(cap = Money.ofCad(2000.0)),
        )
        assertIs<OrderGuard.Result.Approved>(result)
    }

    @Test
    fun rejectedWhenUnitsZero() {
        val result = OrderGuard.evaluate(intent(units = 0.0), Money.ofCad(50.0), config())
        assertIs<OrderGuard.Result.Rejected>(result)
    }

    @Test
    fun rejectedWhenUnitsNegative() {
        val result = OrderGuard.evaluate(intent(units = -5.0), Money.ofCad(50.0), config())
        assertIs<OrderGuard.Result.Rejected>(result)
    }
}
