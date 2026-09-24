"""Tests for server-side order guardrails (guardrails.py)."""

import os
import sys
import importlib
import pytest

sys.path.insert(0, '/home/user/NexusSphere')


def _load_guardrails(kill_switch='', max_order='2000'):
    """Reload guardrails module with patched env vars."""
    os.environ['NS_KILL_SWITCH'] = kill_switch
    os.environ['NS_MAX_ORDER_CAD'] = max_order
    if 'guardrails' in sys.modules:
        del sys.modules['guardrails']
    import guardrails
    importlib.reload(guardrails)
    return guardrails


class TestOrderGuard:
    def test_kill_switch_blocks_order(self):
        g = _load_guardrails(kill_switch='true')
        with pytest.raises(ValueError, match='kill switch'):
            g.order_guard(units=10, price=50, action='BUY')

    def test_kill_switch_off_allows_order(self):
        g = _load_guardrails(kill_switch='')
        g.order_guard(units=10, price=50, action='BUY')  # should not raise

    def test_units_zero_rejected(self):
        g = _load_guardrails()
        with pytest.raises(ValueError, match='positive'):
            g.order_guard(units=0, price=50, action='BUY')

    def test_units_negative_rejected(self):
        g = _load_guardrails()
        with pytest.raises(ValueError, match='positive'):
            g.order_guard(units=-5, price=50, action='BUY')

    def test_price_zero_rejected(self):
        g = _load_guardrails()
        with pytest.raises(ValueError, match='positive'):
            g.order_guard(units=10, price=0, action='BUY')

    def test_notional_at_cap_allowed(self):
        g = _load_guardrails(max_order='2000')
        g.order_guard(units=40, price=50, action='BUY')  # 40*50=2000, exactly at cap

    def test_notional_above_cap_blocked(self):
        g = _load_guardrails(max_order='2000')
        with pytest.raises(ValueError, match='exceeds maximum'):
            g.order_guard(units=41, price=50, action='BUY')  # 41*50=2050

    def test_custom_cap_respected(self):
        g = _load_guardrails(max_order='500')
        with pytest.raises(ValueError, match='exceeds maximum'):
            g.order_guard(units=11, price=50, action='BUY')  # 550 > 500

    def test_no_price_skips_notional_check(self):
        g = _load_guardrails(max_order='100')
        # Without a price we can't compute notional — should pass through
        g.order_guard(units=1000, price=None, action='BUY')

    def test_sell_order_also_guarded(self):
        g = _load_guardrails(max_order='100')
        with pytest.raises(ValueError, match='exceeds maximum'):
            g.order_guard(units=10, price=50, action='SELL')  # 500 > 100

    def test_explicit_override_ignores_env(self):
        # Even with kill_switch env='' we can pass explicit True
        g = _load_guardrails(kill_switch='')
        with pytest.raises(ValueError, match='kill switch'):
            g.order_guard(units=10, price=50, action='BUY', kill_switch=True)

    def test_explicit_cap_override(self):
        g = _load_guardrails(max_order='9999')
        with pytest.raises(ValueError, match='exceeds maximum'):
            g.order_guard(units=3, price=50, action='BUY', max_order_cad=100)
