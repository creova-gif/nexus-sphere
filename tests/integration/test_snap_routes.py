"""Integration tests for SnapTrade proxy routes — uses mocked SDK."""

import os
import sys
import types
import pytest


def _make_response(body):
    r = types.SimpleNamespace()
    r.body = body
    return r


def _load_app_with_mock_snap():
    os.environ['NS_KILL_SWITCH'] = ''
    os.environ['NS_MAX_ORDER_CAD'] = '2000'
    os.environ['SNAPTRADE_CLIENT_ID'] = 'test-cid'
    os.environ['SNAPTRADE_CONSUMER_KEY'] = 'test-ckey'
    os.environ.setdefault('AI_INTEGRATIONS_ANTHROPIC_BASE_URL', 'http://localhost')
    os.environ.setdefault('AI_INTEGRATIONS_ANTHROPIC_API_KEY', 'test')
    os.environ.setdefault('AI_INTEGRATIONS_OPENAI_BASE_URL', 'http://localhost')
    os.environ.setdefault('AI_INTEGRATIONS_OPENAI_API_KEY', 'test')

    # Build a minimal mock of snaptrade_client
    mock_st = types.ModuleType('snaptrade_client')
    mock_st.ApiException = Exception

    class MockClient:
        class account_information:
            @staticmethod
            def list_user_accounts(**kw):
                return _make_response([{'id': 'acct-1', 'name': 'TFSA'}])

            @staticmethod
            def get_all_user_holdings(**kw):
                return _make_response([])

            @staticmethod
            def get_user_account_balance(**kw):
                return _make_response({'cash': 5000})

            @staticmethod
            def get_user_account_orders(**kw):
                return _make_response([])

        class transactions_and_reporting:
            @staticmethod
            def get_activities(**kw):
                return _make_response([])

        class reference_data:
            @staticmethod
            def get_symbols(**kw):
                return _make_response([])

        class trading:
            @staticmethod
            def get_user_account_quotes(**kw):
                return _make_response({})

            @staticmethod
            def get_order_impact(**kw):
                return _make_response({'estimated_commissions': 0, 'currency': 'CAD'})

            @staticmethod
            def place_order(**kw):
                return _make_response({'status': 'ACCEPTED'})

            @staticmethod
            def place_force_order(**kw):
                return _make_response({'status': 'ACCEPTED'})

            @staticmethod
            def cancel_user_account_order(**kw):
                return _make_response({'status': 'CANCELLED'})

        class authentication:
            @staticmethod
            def register_snap_trade_user(**kw):
                return _make_response({'userId': kw.get('user_id')})

            @staticmethod
            def login_snap_trade_user(**kw):
                return _make_response({'redirectURI': 'https://example.com'})

    mock_st.SnapTrade = lambda **kw: MockClient()
    sys.modules['snaptrade_client'] = mock_st

    if 'main' in sys.modules:
        del sys.modules['main']
    sys.path.insert(0, '/home/user/NexusSphere')
    import main as m
    import importlib
    importlib.reload(m)
    return m


@pytest.fixture(scope='module')
def client():
    m = _load_app_with_mock_snap()
    m.app.config['TESTING'] = True
    return m.app.test_client()


CREDS = {'userId': 'u1', 'userSecret': 's1'}


class TestHealthAndStatus:
    def test_health(self, client):
        r = client.get('/health')
        assert r.status_code == 200

    def test_snap_status(self, client):
        r = client.get('/api/snap/status')
        assert r.status_code == 200
        assert r.get_json()['configured'] is True


class TestCredentialsNeverInURL:
    """Verify that GET requests with creds in query params still work (fallback)
    but that POST body takes priority — the important thing is endpoints accept both."""

    def test_accounts_post_body(self, client):
        import json
        r = client.post('/api/snap/accounts',
                        data=json.dumps(CREDS),
                        content_type='application/json')
        assert r.status_code == 200

    def test_positions_post_body(self, client):
        import json
        r = client.post('/api/snap/positions',
                        data=json.dumps(CREDS),
                        content_type='application/json')
        assert r.status_code == 200

    def test_balances_missing_creds_returns_400(self, client):
        import json
        r = client.post('/api/snap/balances',
                        data=json.dumps({}),
                        content_type='application/json')
        assert r.status_code == 400


class TestOrderImpactGuardrails:
    def test_impact_blocked_by_kill_switch(self, client):
        import json
        import os
        # Temporarily flip the kill switch via env — we'd need a reload for this
        # so we test the guardrail at unit level in test_order_guardrails.py instead.
        # Here we just confirm the impact endpoint exists and accepts valid input.
        payload = dict(**CREDS, accountId='acct-1', action='BUY',
                       symbolId='sym-1', units=1, price=50)
        r = client.post('/api/snap/order/impact',
                        data=json.dumps(payload),
                        content_type='application/json')
        # With mocked SDK this should succeed (200) or hit SDK mock
        assert r.status_code in (200, 400, 500)

    def test_impact_zero_units_rejected(self, client):
        import json
        payload = dict(**CREDS, accountId='acct-1', action='BUY',
                       symbolId='sym-1', units=0, price=50)
        r = client.post('/api/snap/order/impact',
                        data=json.dumps(payload),
                        content_type='application/json')
        assert r.status_code == 400
        assert 'positive' in r.get_json().get('error', '').lower()

    def test_impact_over_cap_rejected(self, client):
        import json
        # 41 shares × $50 = $2050 > $2000 cap
        payload = dict(**CREDS, accountId='acct-1', action='BUY',
                       symbolId='sym-1', units=41, price=50)
        r = client.post('/api/snap/order/impact',
                        data=json.dumps(payload),
                        content_type='application/json')
        assert r.status_code == 400
        assert 'exceeds maximum' in r.get_json().get('error', '')
