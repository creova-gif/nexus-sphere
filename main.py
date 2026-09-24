from flask import Flask, make_response, request, jsonify
import os
import logging
from guardrails import order_guard as _order_guard_fn, MAX_ORDER_CAD as _MAX_CAD

# ── Lazy AI client imports ─────────────────────────────────────────────────────
_openai_client = None
_anthropic_client = None

def _get_openai():
    global _openai_client
    if _openai_client is None:
        from openai import OpenAI
        _openai_client = OpenAI(
            base_url=os.environ.get('AI_INTEGRATIONS_OPENAI_BASE_URL'),
            api_key=os.environ.get('AI_INTEGRATIONS_OPENAI_API_KEY'),
        )
    return _openai_client

def _get_anthropic():
    global _anthropic_client
    if _anthropic_client is None:
        import anthropic
        _anthropic_client = anthropic.Anthropic(
            base_url=os.environ.get('AI_INTEGRATIONS_ANTHROPIC_BASE_URL'),
            api_key=os.environ.get('AI_INTEGRATIONS_ANTHROPIC_API_KEY'),
        )
    return _anthropic_client

logging.basicConfig(level=logging.INFO)
log = logging.getLogger(__name__)

def _order_guard(units, price, action):
    """Wrapper around guardrails.order_guard using module-level env constants."""
    _order_guard_fn(units, price, action)

app = Flask(__name__)
app.config['SEND_FILE_MAX_AGE_DEFAULT'] = 0

@app.after_request
def add_cors(response):
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.headers['Access-Control-Allow-Headers'] = 'Content-Type,Authorization'
    response.headers['Access-Control-Allow-Methods'] = 'GET,POST,OPTIONS'
    return response

def _safe_err(msg):
    """Return a generic error message for user-facing responses."""
    return str(msg)[:200] if msg else 'Internal error'

# ── Lazy SnapTrade import ─────────────────────────────────────────────────────
# snaptrade_client takes ~6 seconds to import. We defer it until the first API
# call so Flask is ready to serve health checks and pages immediately on start.
_st = None

def _get_st():
    global _st
    if _st is None:
        import snaptrade_client as st
        _st = st
    return _st

def get_snap_client():
    st = _get_st()
    cid = os.environ.get('SNAPTRADE_CLIENT_ID', '').strip()
    ckey = os.environ.get('SNAPTRADE_CONSUMER_KEY', '').strip()
    if not cid or not ckey:
        return None
    return st.SnapTrade(consumer_key=ckey, client_id=cid)

def nocache_file(path):
    try:
        with open(path, 'rb') as f:
            content = f.read()
        resp = make_response(content)
        resp.headers['Content-Type'] = 'text/html; charset=utf-8'
        resp.headers['Cache-Control'] = 'no-store, no-cache, must-revalidate, max-age=0'
        resp.headers['Pragma'] = 'no-cache'
        resp.headers['Expires'] = '0'
        return resp
    except FileNotFoundError:
        return make_response(f"<h1>File not found: {path}</h1>", 404)
    except Exception as e:
        log.error(f"Error serving {path}: {e}")
        return make_response("<h1>Server error</h1>", 500)

def snap_err(e):
    st = _get_st()
    log.error(f"SnapTrade error: {e}")
    if isinstance(e, st.ApiException):
        body = e.body if isinstance(e.body, dict) else {'error': str(e.body)[:300]}
        return jsonify(body), e.status
    return jsonify({'error': str(e)[:300]}), 500

# ── App routes ────────────────────────────────────────────────────────────────

@app.route('/')
def nexussphere():
    return nocache_file('static_nexussphere.html')

@app.route('/bigmac')
def bigmac():
    return nocache_file('static_bigmac.html')

@app.route('/health')
def health():
    return 'OK'

@app.route('/favicon.ico')
def favicon():
    return make_response('', 204)

@app.route('/snap-callback')
def snap_callback():
    return nocache_file('static_nexussphere.html')

# ── SnapTrade auth & portfolio proxy ─────────────────────────────────────────

@app.route('/api/snap/status')
def snap_status():
    cid = os.environ.get('SNAPTRADE_CLIENT_ID', '').strip()
    ckey = os.environ.get('SNAPTRADE_CONSUMER_KEY', '').strip()
    return jsonify({'configured': bool(cid and ckey)})

@app.route('/api/snap/register', methods=['POST'])
def snap_register():
    client = get_snap_client()
    if not client:
        return jsonify({'error': 'SnapTrade not configured'}), 503
    data = request.get_json(silent=True) or {}
    user_id = data.get('userId', '').strip()
    if not user_id:
        return jsonify({'error': 'userId required'}), 400
    try:
        resp = client.authentication.register_snap_trade_user(user_id=user_id)
        return jsonify(resp.body)
    except Exception as e:
        return snap_err(e)

@app.route('/api/snap/connect', methods=['POST'])
def snap_connect():
    client = get_snap_client()
    if not client:
        return jsonify({'error': 'SnapTrade not configured'}), 503
    data = request.get_json(silent=True) or {}
    user_id = data.get('userId', '').strip()
    user_secret = data.get('userSecret', '').strip()
    broker = data.get('broker', 'WEALTHSIMPLE').strip()
    if not user_id or not user_secret:
        return jsonify({'error': 'userId and userSecret required'}), 400
    domains = os.environ.get('REPLIT_DOMAINS', '')
    domain = domains.split(',')[0].strip() if domains else ''
    callback = f'https://{domain}/snap-callback' if domain else None
    try:
        resp = client.authentication.login_snap_trade_user(
            user_id=user_id, user_secret=user_secret,
            broker=broker, immediate_redirect=False,
            custom_redirect=callback
        )
        return jsonify(resp.body)
    except Exception as e:
        return snap_err(e)

def _creds_from_request():
    """Extract userId/userSecret from POST body (preferred) or GET params."""
    if request.method == 'POST':
        data = request.get_json(silent=True) or {}
    else:
        data = {}
    uid = data.get('userId') or request.args.get('userId', '')
    usec = data.get('userSecret') or request.args.get('userSecret', '')
    return uid.strip(), usec.strip(), data

@app.route('/api/snap/accounts', methods=['GET', 'POST'])
def snap_accounts():
    client = get_snap_client()
    if not client:
        return jsonify({'error': 'SnapTrade not configured'}), 503
    uid, usec, _ = _creds_from_request()
    if not uid or not usec:
        return jsonify({'error': 'userId and userSecret required'}), 400
    try:
        resp = client.account_information.list_user_accounts(user_id=uid, user_secret=usec)
        return jsonify(resp.body)
    except Exception as e:
        return snap_err(e)

@app.route('/api/snap/positions', methods=['GET', 'POST'])
def snap_positions():
    client = get_snap_client()
    if not client:
        return jsonify({'error': 'SnapTrade not configured'}), 503
    uid, usec, _ = _creds_from_request()
    if not uid or not usec:
        return jsonify({'error': 'userId and userSecret required'}), 400
    try:
        resp = client.account_information.get_all_user_holdings(user_id=uid, user_secret=usec)
        return jsonify(resp.body)
    except Exception as e:
        return snap_err(e)

@app.route('/api/snap/balances', methods=['GET', 'POST'])
def snap_balances():
    client = get_snap_client()
    if not client:
        return jsonify({'error': 'SnapTrade not configured'}), 503
    uid, usec, data = _creds_from_request()
    if not uid or not usec:
        return jsonify({'error': 'userId and userSecret required'}), 400
    acct_id = (data.get('accountId') or request.args.get('accountId', '')).strip()
    try:
        if acct_id:
            resp = client.account_information.get_user_account_balance(
                user_id=uid, user_secret=usec, account_id=acct_id)
        else:
            resp = client.account_information.list_user_accounts(user_id=uid, user_secret=usec)
        return jsonify(resp.body)
    except Exception as e:
        return snap_err(e)

@app.route('/api/snap/activities', methods=['GET', 'POST'])
def snap_activities():
    client = get_snap_client()
    if not client:
        return jsonify({'error': 'SnapTrade not configured'}), 503
    uid, usec, data = _creds_from_request()
    if not uid or not usec:
        return jsonify({'error': 'userId and userSecret required'}), 400
    start = (data.get('startDate') or request.args.get('startDate', '')).strip()
    try:
        kwargs = {'user_id': uid, 'user_secret': usec}
        if start:
            kwargs['start_date'] = start
        resp = client.transactions_and_reporting.get_activities(**kwargs)
        return jsonify(resp.body)
    except Exception as e:
        return snap_err(e)

# ── SnapTrade symbol search ───────────────────────────────────────────────────

@app.route('/api/snap/symbols', methods=['GET', 'POST'])
def snap_symbols():
    client = get_snap_client()
    if not client:
        return jsonify({'error': 'SnapTrade not configured'}), 503
    uid, usec, data = _creds_from_request()
    query = (data.get('q') or request.args.get('q', '')).strip()
    acct_id = (data.get('accountId') or request.args.get('accountId', '')).strip()
    if not query:
        return jsonify([])
    try:
        if uid and usec and acct_id:
            resp = client.reference_data.symbol_search_user_account(
                user_id=uid, user_secret=usec,
                account_id=acct_id, substring=query)
        else:
            resp = client.reference_data.get_symbols(substring=query)
        results = resp.body if isinstance(resp.body, list) else []
        filtered = [r for r in results if r.get('exchange', {}).get('code') in ('NASDAQ','NYSE','TSX','ARCA')]
        return jsonify(filtered[:10])
    except Exception as e:
        return snap_err(e)

# ── SnapTrade trading ─────────────────────────────────────────────────────────

@app.route('/api/snap/order/impact', methods=['POST'])
def snap_order_impact():
    """Preview an order — returns estimated cost, units, trade_id for confirmation."""
    client = get_snap_client()
    if not client:
        return jsonify({'error': 'SnapTrade not configured'}), 503
    data = request.get_json(silent=True) or {}
    uid = data.get('userId', '').strip()
    usec = data.get('userSecret', '').strip()
    acct_id = data.get('accountId', '').strip()
    action = data.get('action', 'BUY')
    order_type = data.get('orderType', 'Market')
    symbol_id = data.get('symbolId', '').strip()
    units = data.get('units')
    price = data.get('price')
    tif = data.get('timeInForce', 'Day')

    try:
        _order_guard(units, price, action)
    except ValueError as e:
        return jsonify({'error': str(e)}), 400

    try:
        kwargs = dict(
            user_id=uid, user_secret=usec,
            account_id=acct_id, action=action,
            order_type=order_type, time_in_force=tif,
            universal_symbol_id=symbol_id,
            units=float(units) if units else None,
        )
        if price:
            kwargs['price'] = float(price)
        resp = client.trading.get_order_impact(**kwargs)
        return jsonify(resp.body)
    except Exception as e:
        return snap_err(e)

@app.route('/api/snap/order/place', methods=['POST'])
def snap_order_place():
    """Confirm and place a previewed order using trade_id."""
    client = get_snap_client()
    if not client:
        return jsonify({'error': 'SnapTrade not configured'}), 503
    data = request.get_json(silent=True) or {}
    uid = data.get('userId', '').strip()
    usec = data.get('userSecret', '').strip()
    trade_id = data.get('tradeId', '').strip()
    if not trade_id:
        return jsonify({'error': 'tradeId required'}), 400
    try:
        resp = client.trading.place_order(
            user_id=uid, user_secret=usec,
            trade_id=trade_id, wait_to_confirm=True
        )
        log.info(f"Order placed: {data.get('action')} {data.get('symbolId')} by {uid}")
        return jsonify(resp.body)
    except Exception as e:
        return snap_err(e)

@app.route('/api/snap/order/force', methods=['POST'])
def snap_order_force():
    """Place an order directly without preview (market orders only)."""
    client = get_snap_client()
    if not client:
        return jsonify({'error': 'SnapTrade not configured'}), 503
    data = request.get_json(silent=True) or {}
    uid = data.get('userId', '').strip()
    usec = data.get('userSecret', '').strip()
    acct_id = data.get('accountId', '').strip()
    action = data.get('action', 'BUY')
    order_type = data.get('orderType', 'Market')
    symbol_id = data.get('symbolId', '').strip()
    units = data.get('units')
    price = data.get('price')
    tif = data.get('timeInForce', 'Day')

    try:
        _order_guard(units, price, action)
    except ValueError as e:
        return jsonify({'error': str(e)}), 400

    try:
        kwargs = dict(
            user_id=uid, user_secret=usec,
            account_id=acct_id, action=action,
            order_type=order_type, time_in_force=tif,
            universal_symbol_id=symbol_id,
            units=float(units) if units else None,
        )
        if price:
            kwargs['price'] = float(price)
        resp = client.trading.place_force_order(**kwargs)
        log.info(f"Force order placed: {action} {symbol_id} {units} by {uid}")
        return jsonify(resp.body)
    except Exception as e:
        return snap_err(e)

@app.route('/api/snap/kill-switch', methods=['GET'])
def snap_kill_switch_status():
    """Return current kill-switch and order-cap state for the UI."""
    from guardrails import KILL_SWITCH as _KS, MAX_ORDER_CAD as _CAP
    return jsonify({'killSwitch': _KS, 'maxOrderCAD': _CAP})

@app.route('/api/snap/order/cancel', methods=['POST'])
def snap_order_cancel():
    client = get_snap_client()
    if not client:
        return jsonify({'error': 'SnapTrade not configured'}), 503
    data = request.get_json(silent=True) or {}
    uid = data.get('userId', '').strip()
    usec = data.get('userSecret', '').strip()
    acct_id = data.get('accountId', '').strip()
    brokerage_order_id = data.get('brokerageOrderId', '').strip()
    if not brokerage_order_id:
        return jsonify({'error': 'brokerageOrderId required'}), 400
    try:
        resp = client.trading.cancel_user_account_order(
            user_id=uid, user_secret=usec,
            account_id=acct_id,
            brokerage_order_id=brokerage_order_id
        )
        return jsonify(resp.body if resp.body else {'status': 'cancelled'})
    except Exception as e:
        return snap_err(e)

@app.route('/api/snap/orders', methods=['GET', 'POST'])
def snap_orders():
    client = get_snap_client()
    if not client:
        return jsonify({'error': 'SnapTrade not configured'}), 503
    uid, usec, data = _creds_from_request()
    if not uid or not usec:
        return jsonify({'error': 'userId and userSecret required'}), 400
    acct_id = (data.get('accountId') or request.args.get('accountId', '')).strip()
    state = (data.get('state') or request.args.get('state', 'all')).strip()
    try:
        resp = client.account_information.get_user_account_orders(
            user_id=uid, user_secret=usec,
            account_id=acct_id, state=state
        )
        return jsonify(resp.body)
    except Exception as e:
        return snap_err(e)

@app.route('/api/snap/quote', methods=['GET', 'POST'])
def snap_quote():
    client = get_snap_client()
    if not client:
        return jsonify({'error': 'SnapTrade not configured'}), 503
    uid, usec, data = _creds_from_request()
    if not uid or not usec:
        return jsonify({'error': 'userId and userSecret required'}), 400
    acct_id = (data.get('accountId') or request.args.get('accountId', '')).strip()
    symbols = (data.get('symbols') or request.args.get('symbols', '')).strip()
    if not symbols or not acct_id:
        return jsonify({'error': 'symbols and accountId required'}), 400
    try:
        resp = client.trading.get_user_account_quotes(
            user_id=uid, user_secret=usec,
            account_id=acct_id, symbols=symbols
        )
        return jsonify(resp.body)
    except Exception as e:
        return snap_err(e)

# ── AI Proxy Routes ───────────────────────────────────────────────────────────

# Model names confirmed working with Replit AI Integrations
_CLAUDE_MODEL = 'claude-sonnet-4-5'
_OPENAI_MODEL = 'gpt-4o'

@app.route('/api/ai/claude', methods=['POST'])
def ai_claude():
    """Proxy Claude (Anthropic) requests through Replit AI Integration."""
    data = request.get_json(silent=True) or {}
    messages = data.get('messages', [])
    max_tokens = int(data.get('max_tokens', 800))
    system_msg = data.get('system', '')

    if not messages:
        return jsonify({'error': 'messages required'}), 400

    try:
        client = _get_anthropic()
        kwargs = dict(
            model=_CLAUDE_MODEL,
            max_tokens=max_tokens,
            messages=messages,
        )
        if system_msg:
            kwargs['system'] = system_msg

        response = client.messages.create(**kwargs)
        # Return in same format as the Anthropic REST API so frontend parsing is unchanged
        return jsonify({
            'id': response.id,
            'type': 'message',
            'role': 'assistant',
            'content': [{'type': 'text', 'text': response.content[0].text}],
            'model': response.model,
            'stop_reason': response.stop_reason,
            'usage': {
                'input_tokens': response.usage.input_tokens,
                'output_tokens': response.usage.output_tokens,
            }
        })
    except Exception as e:
        log.error(f"Claude API error: {e}")
        return jsonify({'error': str(e)[:500]}), 500


@app.route('/api/ai/openai', methods=['POST'])
def ai_openai():
    """Proxy OpenAI requests through Replit AI Integration."""
    data = request.get_json(silent=True) or {}
    messages = data.get('messages', [])
    max_tokens = int(data.get('max_tokens', 800))
    system_msg = data.get('system', '')

    if not messages:
        return jsonify({'error': 'messages required'}), 400

    try:
        client = _get_openai()
        full_messages = []
        if system_msg:
            full_messages.append({'role': 'system', 'content': system_msg})
        full_messages.extend(messages)

        response = client.chat.completions.create(
            model=_OPENAI_MODEL,
            max_tokens=max_tokens,
            messages=full_messages,
        )
        text = response.choices[0].message.content
        # Return in Anthropic-compatible format so frontend works with both
        return jsonify({
            'content': [{'type': 'text', 'text': text}],
            'model': response.model,
            'usage': {
                'input_tokens': response.usage.prompt_tokens,
                'output_tokens': response.usage.completion_tokens,
            }
        })
    except Exception as e:
        log.error(f"OpenAI API error: {e}")
        return jsonify({'error': str(e)[:500]}), 500


@app.route('/api/ai/news-summary', methods=['POST'])
def ai_news_summary():
    """Summarize a list of news headlines and return market sentiment."""
    data = request.get_json(silent=True) or {}
    headlines = data.get('headlines', [])
    symbol = data.get('symbol', 'the market')

    if not headlines:
        return jsonify({'error': 'headlines required'}), 400

    prompt = (
        f"You are a market analyst. Given these recent news headlines for {symbol}, "
        f"provide a concise 2-3 sentence summary of the key narrative and overall sentiment. "
        f"Then give a JSON object with keys: summary (string), sentiment (bullish/bearish/neutral), "
        f"confidence (0-1).\n\nHeadlines:\n" + '\n'.join(f'- {h}' for h in headlines[:15]) +
        "\n\nRespond ONLY with JSON, no markdown."
    )

    try:
        client = _get_openai()
        response = client.chat.completions.create(
            model=_OPENAI_MODEL,
            max_tokens=300,
            messages=[{'role': 'user', 'content': prompt}],
        )
        text = response.choices[0].message.content.strip()
        text = text.replace('```json', '').replace('```', '').strip()
        import json
        parsed = json.loads(text)
        return jsonify({'success': True, 'data': parsed})
    except Exception as e:
        log.error(f"News summary error: {e}")
        return jsonify({'error': str(e)[:500]}), 500


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    log.info(f"Starting NexusSphere on port {port}")
    app.run(host='0.0.0.0', port=port, debug=False)
