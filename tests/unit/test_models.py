"""Unit tests for financial models used in NexusSphere signal scoring."""

import pytest


# ── Kelly Criterion ────────────────────────────────────────────────────────────

def kelly_fraction(win_rate, avg_win, avg_loss, risk_aversion=1.0):
    """Full Kelly formula: f = (p*b - q) / b, scaled by risk_aversion."""
    if avg_loss <= 0 or avg_win <= 0:
        return 0.0
    b = avg_win / avg_loss  # odds ratio
    q = 1 - win_rate
    raw = (win_rate * b - q) / b
    return max(0.0, raw * risk_aversion)


def kelly_position_size(win_rate, avg_win, avg_loss, capital, risk_aversion=1.0):
    f = kelly_fraction(win_rate, avg_win, avg_loss, risk_aversion)
    return round(capital * f, 2)


class TestKellyCriterion:
    def test_positive_edge(self):
        # 60% win, 2:1 reward/risk → f = (0.6*2 - 0.4)/2 = 0.4
        assert kelly_fraction(0.60, 2.0, 1.0) == pytest.approx(0.40, abs=1e-6)

    def test_zero_edge(self):
        # 50% win, 1:1 → f = 0
        assert kelly_fraction(0.50, 1.0, 1.0) == pytest.approx(0.0, abs=1e-6)

    def test_negative_edge_clamps_to_zero(self):
        # 30% win, 1:1 → raw = -0.4, clamped to 0
        assert kelly_fraction(0.30, 1.0, 1.0) == 0.0

    def test_half_kelly_reduces_size(self):
        full = kelly_fraction(0.60, 2.0, 1.0, risk_aversion=1.0)
        half = kelly_fraction(0.60, 2.0, 1.0, risk_aversion=0.5)
        assert half == pytest.approx(full * 0.5, abs=1e-6)

    def test_position_size_scales_with_capital(self):
        size_1k = kelly_position_size(0.60, 2.0, 1.0, capital=1000)
        size_2k = kelly_position_size(0.60, 2.0, 1.0, capital=2000)
        assert size_2k == pytest.approx(size_1k * 2, abs=0.01)

    def test_zero_avg_loss_returns_zero(self):
        assert kelly_fraction(0.60, 2.0, 0.0) == 0.0

    def test_zero_avg_win_returns_zero(self):
        assert kelly_fraction(0.60, 0.0, 1.0) == 0.0


# ── Sharpe Ratio ───────────────────────────────────────────────────────────────

def sharpe(returns, risk_free_rate=0.0, annualise=True, periods_per_year=252):
    """Compute Sharpe ratio from a list of period returns."""
    import statistics
    if len(returns) < 2:
        return 0.0
    mean_r = sum(returns) / len(returns)
    excess = mean_r - risk_free_rate / periods_per_year
    std = statistics.stdev(returns)
    if std == 0:
        return 0.0
    ratio = excess / std
    return ratio * (periods_per_year ** 0.5) if annualise else ratio


class TestSharpe:
    def test_positive_sharpe(self):
        returns = [0.001] * 252   # flat 0.1% daily, no vol
        # std is 0 → returns 0 (edge-case guard)
        assert sharpe(returns) == 0.0

    def test_negative_sharpe(self):
        # Losing strategy
        returns = [-0.002 + (0.001 if i % 3 == 0 else 0) for i in range(100)]
        assert sharpe(returns) < 0

    def test_higher_volatility_lowers_sharpe(self):
        steady  = [0.001] * 100 + [-0.0005] * 100
        choppy  = [0.005, -0.005] * 100
        assert sharpe(steady) > sharpe(choppy)

    def test_single_return_returns_zero(self):
        assert sharpe([0.01]) == 0.0


# ── Value at Risk ──────────────────────────────────────────────────────────────

def historical_var(returns, confidence=0.95):
    """Historical VaR: the loss not exceeded with the given confidence."""
    sorted_r = sorted(returns)
    idx = int((1 - confidence) * len(sorted_r))
    return -sorted_r[max(0, idx)]


class TestVaR:
    def test_95_var_basic(self):
        # 100 returns: 90 at +1%, 10 at -5% → 95th percentile loss is ~5%
        returns = [0.01] * 90 + [-0.05] * 10
        var = historical_var(returns, confidence=0.95)
        assert var == pytest.approx(0.05, abs=0.01)

    def test_var_increases_with_confidence(self):
        returns = [0.01 * (i - 50) / 50 for i in range(101)]
        var_95 = historical_var(returns, 0.95)
        var_99 = historical_var(returns, 0.99)
        assert var_99 >= var_95

    def test_all_positive_returns_var_near_zero(self):
        returns = [0.001 * i for i in range(1, 101)]
        assert historical_var(returns, 0.95) <= 0


# ── Signal Score Sanity ────────────────────────────────────────────────────────

def clamp(v, lo, hi):
    return max(lo, min(hi, v))


def combined_signal_score(rsi, macd_bull, trend_up, stage, dcf_upside_pct, fundamentals_ok, momentum_pct):
    """Python port of the frontend calcSignalScore weights for back-test validation."""
    score = 0
    # RSI component (max 20)
    if rsi < 30:
        score += 20
    elif rsi < 45:
        score += 14
    elif rsi < 55:
        score += 10
    elif rsi < 70:
        score += 6
    # MACD (max 15)
    score += 15 if macd_bull else 0
    # Trend (max 15)
    score += 15 if trend_up else 0
    # Accumulation stage (max 10)
    score += 10 if stage in (1, 2) else 0
    # DCF upside (max 20)
    score += clamp(int(dcf_upside_pct / 5), 0, 20)
    # Fundamentals (max 10)
    score += 10 if fundamentals_ok else 0
    # Momentum (max 10)
    if momentum_pct > 10:
        score += 10
    elif momentum_pct > 5:
        score += 7
    elif momentum_pct > 0:
        score += 3
    return clamp(score, 0, 100)


PROFIT_GATE = 65


class TestSignalScore:
    def test_strong_buy_exceeds_profit_gate(self):
        score = combined_signal_score(
            rsi=28, macd_bull=True, trend_up=True,
            stage=2, dcf_upside_pct=40, fundamentals_ok=True, momentum_pct=12
        )
        assert score >= PROFIT_GATE

    def test_weak_setup_below_profit_gate(self):
        score = combined_signal_score(
            rsi=72, macd_bull=False, trend_up=False,
            stage=4, dcf_upside_pct=0, fundamentals_ok=False, momentum_pct=-5
        )
        assert score < PROFIT_GATE

    def test_score_bounded_0_to_100(self):
        for _ in range(10):
            score = combined_signal_score(28, True, True, 1, 100, True, 20)
            assert 0 <= score <= 100

    def test_oversold_rsi_adds_most_points(self):
        base  = combined_signal_score(55, False, False, 3, 0, False, 0)
        over  = combined_signal_score(28, False, False, 3, 0, False, 0)
        assert over > base
