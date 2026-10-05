"""Auth, AI rate limits, and trade-route policy for NexusSphere.

Sensitive routes fail closed unless ``NEXUS_API_TOKEN`` is set and the
request sends ``Authorization: Bearer <token>``. The rate limiter keys
buckets by a hash of that token so one authenticated principal cannot
consume another's allowance. The raw token is never stored or logged.
"""

import hashlib
import hmac
import os
import threading
import time

from flask import g, jsonify, request

AI_PATH_PREFIX = '/api/ai/'
TRADE_PATHS = frozenset({
    '/api/snap/order/impact',
    '/api/snap/order/place',
    '/api/snap/order/force',
    '/api/snap/order/cancel',
    '/api/snap/orders',
})
EQUITY_ACTIONS = frozenset({'BUY', 'SELL'})
_SECRET_ENV_NAMES = (
    'NEXUS_API_TOKEN',
    'SNAPTRADE_CLIENT_ID',
    'SNAPTRADE_CONSUMER_KEY',
    'AI_INTEGRATIONS_OPENAI_API_KEY',
    'AI_INTEGRATIONS_ANTHROPIC_API_KEY',
    'OPENAI_API_KEY',
    'ANTHROPIC_API_KEY',
)
_DEFAULT_AI_LIMIT = 30
_DEFAULT_AI_WINDOW_SECONDS = 60
_DEFAULT_MAX_TOKENS = 800
_DEFAULT_MAX_TOKENS_CEILING = 2048


def normalize_path(path):
    if path and path != '/' and path.endswith('/'):
        return path.rstrip('/')
    return path or '/'


def path_requires_auth(path):
    path = normalize_path(path)
    return path.startswith(AI_PATH_PREFIX) or path in TRADE_PATHS


def configured_api_token():
    return os.environ.get('NEXUS_API_TOKEN', '').strip()


def bearer_token():
    header = request.headers.get('Authorization', '')
    if not header:
        return ''
    scheme, _, rest = header.partition(' ')
    if scheme.lower() != 'bearer' or not rest.strip():
        return ''
    return rest.strip()


def tokens_match(presented, expected):
    if not presented or not expected:
        return False
    presented_hash = hashlib.sha256(presented.encode('utf-8')).digest()
    expected_hash = hashlib.sha256(expected.encode('utf-8')).digest()
    return hmac.compare_digest(presented_hash, expected_hash)


def user_id_for_token(token):
    """Stable per-token identity. Not reversible to the bearer token."""
    return hashlib.sha256(token.encode('utf-8')).hexdigest()[:32]


def normalize_equity_action(action):
    """Equity orders are BUY or SELL. Missing action defaults to BUY."""
    if action is None:
        return 'BUY'
    if not isinstance(action, str):
        return None
    value = action.strip().upper()
    if value not in EQUITY_ACTIONS:
        return None
    return value


def _positive_int_env(name, default):
    raw = os.environ.get(name, '')
    if raw.strip() == '':
        return default
    try:
        value = int(raw)
    except (TypeError, ValueError):
        return default
    return value if value >= 1 else default


def max_tokens_ceiling():
    return _positive_int_env('AI_MAX_TOKENS_CEILING', _DEFAULT_MAX_TOKENS_CEILING)


def clamp_max_tokens(value, default=_DEFAULT_MAX_TOKENS):
    ceiling = max_tokens_ceiling()
    try:
        parsed = int(value)
    except (TypeError, ValueError):
        parsed = default
    if parsed < 1:
        parsed = default
    return min(parsed, ceiling)


def redact_secrets(value):
    text = value if isinstance(value, str) else str(value)
    for name in _SECRET_ENV_NAMES:
        secret = os.environ.get(name) or ''
        if len(secret) >= 8:
            text = text.replace(secret, '[redacted]')
    return text


class SlidingWindowRateLimiter:
    """In-process sliding window. Buckets are independent per user id."""

    def __init__(self, limit=_DEFAULT_AI_LIMIT, window_seconds=_DEFAULT_AI_WINDOW_SECONDS):
        self.limit = limit
        self.window_seconds = window_seconds
        self._buckets = {}
        self._lock = threading.Lock()

    def _limit(self):
        return _positive_int_env('AI_RATE_LIMIT', self.limit)

    def _window(self):
        return _positive_int_env('AI_RATE_WINDOW_SECONDS', self.window_seconds)

    def reset(self):
        with self._lock:
            self._buckets.clear()

    def allow(self, user_id):
        now = time.monotonic()
        limit = self._limit()
        window = self._window()
        with self._lock:
            self._prune(now, window)
            hits = self._buckets.get(user_id, [])
            if len(hits) >= limit:
                retry_after = int(window - (now - hits[0])) + 1
                return False, max(1, retry_after)
            hits.append(now)
            self._buckets[user_id] = hits
            return True, 0

    def _prune(self, now, window):
        stale = []
        for key, hits in self._buckets.items():
            fresh = [stamp for stamp in hits if now - stamp < window]
            if fresh:
                self._buckets[key] = fresh
            else:
                stale.append(key)
        for key in stale:
            del self._buckets[key]


ai_rate_limiter = SlidingWindowRateLimiter()


def guard_sensitive_request():
    """before_request hook. Returns a Flask response to stop the request, or None."""
    if not path_requires_auth(request.path):
        return None

    expected = configured_api_token()
    if not expected:
        return jsonify({'error': 'API authentication is not configured'}), 503

    presented = bearer_token()
    if not tokens_match(presented, expected):
        return jsonify({'error': 'Unauthorized'}), 401

    g.nexus_user_id = user_id_for_token(presented)

    path = normalize_path(request.path)
    if not path.startswith(AI_PATH_PREFIX):
        return None

    allowed, retry_after = ai_rate_limiter.allow(g.nexus_user_id)
    if allowed:
        return None

    response = jsonify({'error': 'Rate limit exceeded'})
    response.status_code = 429
    response.headers['Retry-After'] = str(retry_after)
    return response
