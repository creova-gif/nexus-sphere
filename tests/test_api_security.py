"""Security tests for AI and trade routes.

These tests must never place an order or call a brokerage or model API.
Provider clients are replaced with mocks, and credential env vars are
removed for the duration of the test so a missed mock cannot authenticate.
"""

import inspect
import os
import unittest
from unittest import mock

import main
import security_controls
from security_controls import (
    SlidingWindowRateLimiter,
    clamp_max_tokens,
    path_requires_auth,
    redact_secrets,
    user_id_for_token,
)

_CREDENTIAL_ENV = (
    'AI_INTEGRATIONS_OPENAI_API_KEY',
    'AI_INTEGRATIONS_OPENAI_BASE_URL',
    'AI_INTEGRATIONS_ANTHROPIC_API_KEY',
    'AI_INTEGRATIONS_ANTHROPIC_BASE_URL',
    'SNAPTRADE_CLIENT_ID',
    'SNAPTRADE_CONSUMER_KEY',
    'OPENAI_API_KEY',
    'ANTHROPIC_API_KEY',
)
_TEST_TOKEN = 'test-token-not-a-secret'


def _claude_response(text='ok'):
    return mock.Mock(
        id='msg_test',
        model='claude-sonnet-4-5',
        stop_reason='end_turn',
        content=[mock.Mock(text=text)],
        usage=mock.Mock(input_tokens=3, output_tokens=4),
    )


def _openai_response(text='ok'):
    return mock.Mock(
        model='gpt-4o',
        choices=[mock.Mock(message=mock.Mock(content=text))],
        usage=mock.Mock(prompt_tokens=3, completion_tokens=4),
    )


class ApiSecurityTests(unittest.TestCase):
    def setUp(self):
        self._saved_env = {name: os.environ.get(name) for name in _CREDENTIAL_ENV}
        self._saved_env['NEXUS_API_TOKEN'] = os.environ.get('NEXUS_API_TOKEN')
        self._saved_env['AI_RATE_LIMIT'] = os.environ.get('AI_RATE_LIMIT')
        self._saved_env['AI_RATE_WINDOW_SECONDS'] = os.environ.get('AI_RATE_WINDOW_SECONDS')
        self._saved_env['AI_MAX_TOKENS_CEILING'] = os.environ.get('AI_MAX_TOKENS_CEILING')
        for name in _CREDENTIAL_ENV:
            os.environ.pop(name, None)
        os.environ['NEXUS_API_TOKEN'] = _TEST_TOKEN
        os.environ['AI_RATE_LIMIT'] = '30'
        os.environ['AI_RATE_WINDOW_SECONDS'] = '60'
        os.environ.pop('AI_MAX_TOKENS_CEILING', None)

        security_controls.ai_rate_limiter.reset()
        self.client = main.app.test_client()
        self.auth = {'Authorization': 'Bearer ' + _TEST_TOKEN}

        self.broker = mock.Mock()
        self.broker.trading.place_order.side_effect = AssertionError('unexpected place_order')
        self.broker.trading.place_force_order.side_effect = AssertionError('unexpected place_force_order')
        self.broker.trading.get_order_impact.side_effect = AssertionError('unexpected get_order_impact')
        self.broker.trading.cancel_user_account_order.side_effect = AssertionError('unexpected cancel')
        self.broker.trading.get_user_account_quotes.side_effect = AssertionError('unexpected quotes')
        self.broker.account_information.get_user_account_orders.side_effect = AssertionError('unexpected orders')

        self.openai = mock.Mock()
        self.openai.chat.completions.create.side_effect = AssertionError('unexpected openai call')
        self.anthropic = mock.Mock()
        self.anthropic.messages.create.side_effect = AssertionError('unexpected anthropic call')

        self._patches = [
            mock.patch('main.get_snap_client', return_value=self.broker),
            mock.patch('main._get_openai', return_value=self.openai),
            mock.patch('main._get_anthropic', return_value=self.anthropic),
        ]
        for patcher in self._patches:
            patcher.start()

    def tearDown(self):
        for patcher in self._patches:
            patcher.stop()
        security_controls.ai_rate_limiter.reset()
        for name, value in self._saved_env.items():
            if value is None:
                os.environ.pop(name, None)
            else:
                os.environ[name] = value

    def test_health_and_status_stay_public(self):
        health = self.client.get('/health')
        self.assertEqual(health.status_code, 200)
        status = self.client.get('/api/snap/status')
        self.assertEqual(status.status_code, 200)
        self.assertEqual(status.get_json(), {'configured': False})
        main.get_snap_client.assert_not_called()

    def test_quote_is_not_covered_by_trade_auth(self):
        self.assertFalse(path_requires_auth('/api/snap/quote'))
        response = self.client.get('/api/snap/quote')
        self.assertEqual(response.status_code, 400)
        self.broker.trading.get_user_account_quotes.assert_not_called()

    def test_ai_and_trade_paths_require_auth(self):
        self.assertTrue(path_requires_auth('/api/ai/claude'))
        self.assertTrue(path_requires_auth('/api/ai/openai'))
        self.assertTrue(path_requires_auth('/api/ai/news-summary'))
        self.assertTrue(path_requires_auth('/api/snap/order/force/'))
        self.assertFalse(path_requires_auth('/api/snap/register'))
        self.assertFalse(path_requires_auth('/api/snap/positions'))

    def test_ai_routes_reject_missing_or_wrong_token(self):
        bodies = {
            '/api/ai/claude': {'messages': [{'role': 'user', 'content': 'hi'}]},
            '/api/ai/openai': {'messages': [{'role': 'user', 'content': 'hi'}]},
            '/api/ai/news-summary': {'headlines': ['Headline']},
        }
        for path, body in bodies.items():
            missing = self.client.post(path, json=body)
            self.assertEqual(missing.status_code, 401, path)
            wrong = self.client.post(path, json=body, headers={'Authorization': 'Bearer wrong-token'})
            self.assertEqual(wrong.status_code, 401, path)
            query = self.client.post(path + '?api_key=' + _TEST_TOKEN, json=body)
            self.assertEqual(query.status_code, 401, path)
        self.openai.chat.completions.create.assert_not_called()
        self.anthropic.messages.create.assert_not_called()

    def test_trade_routes_reject_unauthenticated_calls(self):
        for path in (
            '/api/snap/order/impact',
            '/api/snap/order/place',
            '/api/snap/order/force',
            '/api/snap/order/cancel',
            '/api/snap/orders',
        ):
            posted = self.client.post(path, json={'userSecret': 'do-not-log', 'units': 1})
            self.assertEqual(posted.status_code, 401, path)
            fetched = self.client.get(path + '?userSecret=do-not-log')
            self.assertEqual(fetched.status_code, 401, path)
        main.get_snap_client.assert_not_called()
        self.broker.trading.place_force_order.assert_not_called()
        self.broker.trading.place_order.assert_not_called()

    def test_sensitive_routes_fail_closed_when_token_unset(self):
        os.environ.pop('NEXUS_API_TOKEN', None)
        ai = self.client.post('/api/ai/claude', json={'messages': [{'role': 'user', 'content': 'hi'}]}, headers=self.auth)
        trade = self.client.post('/api/snap/order/place', json={'tradeId': 't'}, headers=self.auth)
        self.assertEqual(ai.status_code, 503)
        self.assertEqual(trade.status_code, 503)
        self.anthropic.messages.create.assert_not_called()
        main.get_snap_client.assert_not_called()

    def test_force_order_is_disabled_and_does_not_call_broker(self):
        source = inspect.getsource(main.snap_order_force)
        self.assertNotIn('place_force_order', source)
        self.assertNotIn('get_snap_client', source)

        response = self.client.post('/api/snap/order/force', json={
            'userId': 'user-1',
            'userSecret': 'secret',
            'accountId': 'acct',
            'action': 'BUY',
            'symbolId': 'sym',
            'units': 10,
            'confirmPreview': True,
        }, headers=self.auth)
        self.assertEqual(response.status_code, 403)
        self.assertIn('disabled', response.get_json()['error'].lower())
        main.get_snap_client.assert_not_called()
        self.broker.trading.place_force_order.assert_not_called()

    def test_place_requires_trade_id_and_uses_mock(self):
        missing = self.client.post('/api/snap/order/place', json={
            'userId': 'user-1',
            'userSecret': 'secret',
        }, headers=self.auth)
        self.assertEqual(missing.status_code, 400)
        main.get_snap_client.assert_not_called()

        self.broker.trading.place_order.side_effect = None
        self.broker.trading.place_order.return_value = mock.Mock(body={'status': 'accepted'})
        placed = self.client.post('/api/snap/order/place', json={
            'userId': 'user-1',
            'userSecret': 'secret',
            'tradeId': 'trade-123',
            'action': 'BUY',
            'symbolId': 'sym',
        }, headers=self.auth)
        self.assertEqual(placed.status_code, 200)
        self.assertEqual(placed.get_json(), {'status': 'accepted'})
        self.broker.trading.place_order.assert_called_once()
        called = self.broker.trading.place_order.call_args.kwargs
        self.assertEqual(called['trade_id'], 'trade-123')
        self.assertNotIn('userSecret', called)
        self.broker.trading.place_force_order.assert_not_called()

    def test_impact_rejects_non_equity_actions_and_previews_buy(self):
        rejected = self.client.post('/api/snap/order/impact', json={
            'action': 'SELL_TO_OPEN',
            'units': 1,
            'symbolId': 'sym',
        }, headers=self.auth)
        self.assertEqual(rejected.status_code, 400)
        self.broker.trading.get_order_impact.assert_not_called()

        self.broker.trading.get_order_impact.side_effect = None
        self.broker.trading.get_order_impact.return_value = mock.Mock(body={'trade': {'id': 'trade-1'}})
        preview = self.client.post('/api/snap/order/impact', json={
            'userId': 'user-1',
            'userSecret': 'secret',
            'accountId': 'acct',
            'action': 'buy',
            'symbolId': 'sym',
            'units': 2,
        }, headers=self.auth)
        self.assertEqual(preview.status_code, 200)
        called = self.broker.trading.get_order_impact.call_args.kwargs
        self.assertEqual(called['action'], 'BUY')
        self.broker.trading.place_force_order.assert_not_called()
        self.broker.trading.place_order.assert_not_called()

    def test_impact_rejects_non_positive_units_without_broker_call(self):
        response = self.client.post('/api/snap/order/impact', json={'units': 0, 'action': 'BUY'}, headers=self.auth)
        self.assertEqual(response.status_code, 400)
        main.get_snap_client.assert_not_called()

    def test_claude_and_openai_use_mocks_and_clamp_max_tokens(self):
        self.anthropic.messages.create.side_effect = None
        self.anthropic.messages.create.return_value = _claude_response('claude-text')
        self.openai.chat.completions.create.side_effect = None
        self.openai.chat.completions.create.return_value = _openai_response('openai-text')

        claude = self.client.post('/api/ai/claude', json={
            'messages': [{'role': 'user', 'content': 'hi'}],
            'max_tokens': 10 ** 9,
            'system': 'custom',
        }, headers=self.auth)
        self.assertEqual(claude.status_code, 200)
        self.assertEqual(claude.get_json()['content'][0]['text'], 'claude-text')
        self.assertEqual(self.anthropic.messages.create.call_args.kwargs['max_tokens'], 2048)

        openai = self.client.post('/api/ai/openai', json={
            'messages': [{'role': 'user', 'content': 'hi'}],
            'max_tokens': 'nope',
        }, headers=self.auth)
        self.assertEqual(openai.status_code, 200)
        self.assertEqual(self.openai.chat.completions.create.call_args.kwargs['max_tokens'], 800)

    def test_news_summary_uses_mock(self):
        self.openai.chat.completions.create.side_effect = None
        self.openai.chat.completions.create.return_value = _openai_response(
            '{"summary":"steady","sentiment":"neutral","confidence":0.4}'
        )
        response = self.client.post('/api/ai/news-summary', json={
            'headlines': ['Markets quiet'],
            'symbol': 'TEST',
        }, headers=self.auth)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json()['data']['sentiment'], 'neutral')
        self.openai.chat.completions.create.assert_called_once()

    def test_ai_rate_limit_is_per_authenticated_user(self):
        os.environ['AI_RATE_LIMIT'] = '2'
        security_controls.ai_rate_limiter.reset()
        self.anthropic.messages.create.side_effect = None
        self.anthropic.messages.create.return_value = _claude_response()
        self.openai.chat.completions.create.side_effect = None
        self.openai.chat.completions.create.return_value = _openai_response()
        body = {'messages': [{'role': 'user', 'content': 'hi'}]}

        self.assertEqual(self.client.post('/api/ai/claude', json=body, headers=self.auth).status_code, 200)
        self.assertEqual(self.client.post('/api/ai/openai', json=body, headers=self.auth).status_code, 200)
        blocked = self.client.post('/api/ai/news-summary', json={'headlines': ['x']}, headers=self.auth)
        self.assertEqual(blocked.status_code, 429)
        self.assertIn('Retry-After', blocked.headers)
        self.assertEqual(self.anthropic.messages.create.call_count, 1)
        self.assertEqual(self.openai.chat.completions.create.call_count, 1)

        user_id = user_id_for_token(_TEST_TOKEN)
        self.assertEqual(set(security_controls.ai_rate_limiter._buckets), {user_id})
        self.assertNotIn('127.0.0.1', security_controls.ai_rate_limiter._buckets)
        self.assertNotIn(_TEST_TOKEN, str(security_controls.ai_rate_limiter._buckets))

        denied = self.client.post('/api/ai/claude', json=body, headers={'Authorization': 'Bearer someone-else'})
        self.assertEqual(denied.status_code, 401)
        self.assertEqual(self.anthropic.messages.create.call_count, 1)

    def test_rate_limiter_isolates_user_buckets(self):
        os.environ.pop('AI_RATE_LIMIT', None)
        limiter = SlidingWindowRateLimiter(limit=1, window_seconds=60)
        self.assertTrue(limiter.allow('user-a')[0])
        self.assertFalse(limiter.allow('user-a')[0])
        allowed, _retry = limiter.allow('user-b')
        self.assertTrue(allowed)

    def test_rejected_auth_does_not_consume_rate_limit(self):
        os.environ['AI_RATE_LIMIT'] = '1'
        security_controls.ai_rate_limiter.reset()
        self.anthropic.messages.create.side_effect = None
        self.anthropic.messages.create.return_value = _claude_response()
        body = {'messages': [{'role': 'user', 'content': 'hi'}]}
        self.assertEqual(
            self.client.post('/api/ai/claude', json=body, headers={'Authorization': 'Bearer nope'}).status_code,
            401,
        )
        self.assertEqual(self.client.post('/api/ai/claude', json=body, headers=self.auth).status_code, 200)
        self.anthropic.messages.create.assert_called_once()

    def test_clamp_and_redaction_helpers(self):
        self.assertEqual(clamp_max_tokens(50), 50)
        self.assertEqual(clamp_max_tokens(0), 800)
        os.environ['AI_MAX_TOKENS_CEILING'] = '100'
        self.assertEqual(clamp_max_tokens(500), 100)
        os.environ['NEXUS_API_TOKEN'] = _TEST_TOKEN
        redacted = redact_secrets('failed with ' + _TEST_TOKEN)
        self.assertNotIn(_TEST_TOKEN, redacted)
        self.assertIn('[redacted]', redacted)


if __name__ == '__main__':
    unittest.main()
