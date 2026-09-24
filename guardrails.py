"""Server-side order guardrails — no Flask dependency so tests can import directly."""

import os

MAX_ORDER_CAD = float(os.environ.get('NS_MAX_ORDER_CAD', 2000))
KILL_SWITCH   = os.environ.get('NS_KILL_SWITCH', '').lower() in ('1', 'true', 'yes')


def order_guard(units, price, action, kill_switch=None, max_order_cad=None):
    """Raise ValueError if the order violates safety constraints.

    kill_switch and max_order_cad default to the module-level constants
    (read from env at import time).  Pass explicit values to override in tests.
    """
    ks  = KILL_SWITCH   if kill_switch    is None else kill_switch
    cap = MAX_ORDER_CAD if max_order_cad  is None else float(max_order_cad)

    if ks:
        raise ValueError('Trading kill switch is active — all orders blocked')
    if units is not None and float(units) <= 0:
        raise ValueError('Units must be positive')
    if price is not None and float(price) <= 0:
        raise ValueError('Price must be positive')
    if price is not None and units is not None:
        notional = float(units) * float(price)
        if notional > cap:
            raise ValueError(
                f'Order notional ${notional:,.2f} CAD exceeds maximum ${cap:,.2f} CAD'
            )
