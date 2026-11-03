"""Automated API tests for app-api. Starts PHP's built-in server with a log-only mail driver."""
import json, os, subprocess, sys, tempfile, time, uuid, urllib.request, shutil, glob, datetime

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'app-api'))
results = []

def start(rl=1000, port=8765):
    priv = tempfile.mkdtemp()
    shutil.copy(os.path.join(os.path.dirname(__file__), 'test_config.php'), os.path.join(priv, 'config.php'))
    storage = os.path.join(priv, 'storage'); os.makedirs(storage)
    env = dict(os.environ, FTC_PRIVATE_DIR=priv, FTC_TEST_STORAGE=storage, FTC_TEST_RL=str(rl), FTC_ALLOW_HTTP_FOR_TESTS='1')
    env.pop('HTTPS_PROXY', None); env.pop('https_proxy', None)
    p = subprocess.Popen(['php', '-S', f'127.0.0.1:{port}', '-t', ROOT], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(0.8)
    return p, storage, port

opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
def req(port, path, body=None, method=None, ctype='application/json', raw=None):
    data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
    r = urllib.request.Request(f'http://127.0.0.1:{port}{path}', data=data, method=method or ('POST' if data is not None else 'GET'))
    if data is not None: r.add_header('Content-Type', ctype)
    try:
        resp = opener.open(r, timeout=10); return resp.status, json.loads(resp.read() or b'{}')
    except urllib.error.HTTPError as e:
        txt = e.read()
        try: return e.code, json.loads(txt)
        except Exception: return e.code, {'raw': txt[:200].decode(errors='replace')}

def quote(**over):
    d = {'submission_id': str(uuid.uuid4()), 'elapsed_seconds': 60, 'website': '',
         'name': 'Jane Citizen', 'email': 'jane@example.com', 'phone': '0412 345 678',
         'event_date': (datetime.date.today() + datetime.timedelta(days=30)).isoformat(),
         'food_time': '12:00 pm', 'event_address': '1 George St, Sydney NSW', 'postcode': '2000',
         'event_type': 'Office Celebration', 'meals': 120, 'paying': 'Organiser', 'power': 'Yes',
         'private_land': 'No', 'entrance': 'Yes', 'cuisines': ['Pizza / Italian', 'Vietnamese'],
         'additional_info': 'Vegetarian options please <script>alert(1)</script>'}
    d.update(over); return d

def contact(**over):
    d = {'submission_id': str(uuid.uuid4()), 'elapsed_seconds': 30, 'website': '', 'name': 'Sam Lee',
         'email': 'sam@example.com', 'topic': 'General enquiry', 'message': 'Hello, do you cover Newcastle?'}
    d.update(over); return d

def check(name, cond, detail=''):
    results.append((name, bool(cond), detail)); print(('PASS ' if cond else 'FAIL ') + name + ('' if cond else f'  -> {detail}'))

def outbox(storage): return sorted(glob.glob(os.path.join(storage, 'outbox', '*.json')), key=os.path.getmtime)

p, st, port = start()
try:
    s, b = req(port, '/v1/app-config.php'); check('config endpoint returns contact email', s == 200 and b.get('contact_email') == 'vikas.kumar617@gmail.com', b)
    s, b = req(port, '/v1/quote.php', quote()); check('valid quote accepted', s == 200 and b.get('ok') and b['enquiry_id'].startswith('FTC-'), (s, b))
    mails = outbox(st); m = json.load(open(mails[-1])) if mails else {}
    check('quote email sent to configured recipient', m.get('to') == 'vikas.kumar617@gmail.com', m.get('to'))
    exp_date = (datetime.date.today() + datetime.timedelta(days=30)).strftime('%a %-d %b %Y')
    check('subject format', m.get('subject') == f'New Food Truck Catering Enquiry – Jane Citizen – {exp_date}', m.get('subject'))
    check('HTML escapes user input (XSS)', '<script>' not in m.get('html', '') and '&lt;script&gt;' in m.get('html', ''))
    check('email contains all fields', all(x in m.get('html', '') for x in ['Jane Citizen', 'jane@example.com', '0412 345 678', '2000', 'Office Celebration', '120', 'Organiser', 'Pizza / Italian, Vietnamese', '12:00 pm', '1 George St']))
    check('reply-to is customer', m.get('reply_to_email') == 'jane@example.com')

    q = quote(); s1, b1 = req(port, '/v1/quote.php', q); n = len(outbox(st)); s2, b2 = req(port, '/v1/quote.php', q)
    check('same submission_id is not emailed twice', s2 == 200 and b2.get('duplicate') and len(outbox(st)) == n and b1['enquiry_id'] == b2['enquiry_id'], (b1, b2))
    q2 = dict(q, submission_id=str(uuid.uuid4())); n = len(outbox(st)); s3, b3 = req(port, '/v1/quote.php', q2)
    check('identical content with new id is not emailed twice', s3 == 200 and b3.get('duplicate') and len(outbox(st)) == n, b3)

    for label, over, field in [
        ('empty name', {'name': ''}, 'name'), ('invalid email', {'email': 'not-an-email'}, 'email'),
        ('header injection in email', {'email': 'a@b.com\nBcc: x@y.com'}, 'email'),
        ('invalid phone', {'phone': '12ab'}, 'phone'), ('meals below 50', {'meals': 49}, 'meals'),
        ('meals not a number', {'meals': 'fifty'}, 'meals'),
        ('more than three cuisines', {'cuisines': ['Seafood', 'Vietnamese', 'Asian Fusion', 'Pizza / Italian']}, 'cuisines'),
        ('unknown cuisine', {'cuisines': ['Sushi']}, 'cuisines'), ('past event date', {'event_date': '2020-01-01'}, 'event_date'),
        ('invalid date', {'event_date': '2026-02-30'}, 'event_date'), ('bad postcode', {'postcode': '20'}, 'postcode'),
        ('event type not on website', {'event_type': 'Wedding'}, 'event_type'), ('missing paying', {'paying': ''}, 'paying'),
        ('missing power', {'power': 'Maybe'}, 'power'), ('missing private land', {'private_land': None}, 'private_land'),
        ('missing entrance', {'entrance': ''}, 'entrance'),
        ('too many links', {'additional_info': 'http://a http://b http://c http://d'}, 'additional_info')]:
        s, b = req(port, '/v1/quote.php', quote(**over))
        check(f'rejects {label}', s == 422 and field in b.get('fields', {}), (s, b))

    s, b = req(port, '/v1/quote.php', quote(website='http://spam')); check('honeypot blocks bots', s == 400 and b.get('error') == 'rejected', (s, b))
    s, b = req(port, '/v1/quote.php', quote(elapsed_seconds=1)); check('too-fast submission blocked', s == 400, (s, b))
    s, b = req(port, '/v1/quote.php', quote(submission_id='abc')); check('invalid submission id rejected', s == 400, (s, b))
    s, b = req(port, '/v1/quote.php', method='GET'); check('GET not allowed on quote', s == 405, s)
    s, b = req(port, '/v1/quote.php', raw=b'name=x', ctype='application/x-www-form-urlencoded'); check('non-JSON rejected', s == 415, s)
    s, b = req(port, '/v1/quote.php', raw=b'[1,2]'); check('JSON array rejected', s == 400, s)
    s, b = req(port, '/v1/quote.php', raw=json.dumps(quote(additional_info='x' * 20000)).encode()); check('oversized body rejected', s == 413, s)
    s, b = req(port, '/v1/quote.php', quote(name="Robert'); DROP TABLE x;--", email='bob@example.com', phone='0400000000'))
    check('SQL-like input handled as plain text (no DB used)', s == 200, (s, b))

    s, b = req(port, '/v1/contact.php', contact()); m = json.load(open(outbox(st)[-1]))
    check('contact message delivered', s == 200 and b.get('ok') and m['to'] == 'vikas.kumar617@gmail.com' and 'Sam Lee' in m['html'], (s, b))
    s, b = req(port, '/v1/contact.php', contact(message='')); check('contact requires message', s == 422 and 'message' in b['fields'], b)
    s, b = req(port, '/v1/contact.php', contact(topic='Hack')); check('contact rejects unknown topic', s == 422, b)
    s, b = req(port, '/v1/contact.php', contact(topic='App support', app_version='1.0.0', device='Pixel 8')); check('app support enquiry delivered', s == 200, b)

    # Delivery failure: must NOT report success, must preserve the enquiry, and retry must work.
    open(os.path.join(st, 'outbox', 'FAIL'), 'w').close()
    q = quote(name='Fail Test', email='fail@example.com'); s, b = req(port, '/v1/quote.php', q)
    failed = glob.glob(os.path.join(st, 'failed', '*.json'))
    check('mail failure returns error (no false success)', s == 502 and b.get('ok') is False, (s, b))
    check('failed enquiry preserved on server', len(failed) == 1)
    os.remove(os.path.join(st, 'outbox', 'FAIL'))
    s, b = req(port, '/v1/quote.php', q)
    check('retry after failure succeeds and clears preserved copy', s == 200 and b.get('ok') and not glob.glob(os.path.join(st, 'failed', '*.json')), (s, b))

    for path in ['/private/config.php', '/lib/bootstrap.php']:
        s, b = req(port, path); check(f'{path} does not leak config when run directly', 'recipient_email' not in json.dumps(b) and 'password' not in json.dumps(b), b)
finally:
    p.terminate()

# Rate limiting with a low limit
p, st, port = start(rl=3, port=8766)
try:
    codes = [req(port, '/v1/contact.php', contact(message=f'msg number {i}'))[0] for i in range(5)]
    check('rate limit kicks in after 3 per 10 minutes', codes[:3] == [200, 200, 200] and codes[3:] == [429, 429], codes)
finally:
    p.terminate()

# HTTPS enforcement (no test override)
priv = tempfile.mkdtemp(); shutil.copy(os.path.join(os.path.dirname(__file__), 'test_config.php'), os.path.join(priv, 'config.php')); os.makedirs(priv + '/storage')
env = dict(os.environ, FTC_PRIVATE_DIR=priv, FTC_TEST_STORAGE=priv + '/storage')
p = subprocess.Popen(['php', '-S', '127.0.0.1:8767', '-t', ROOT], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL); time.sleep(0.8)
try:
    s, b = req(8767, '/v1/quote.php', quote()); check('plain HTTP requests refused', s == 403 and b.get('error') == 'https_required', (s, b))
finally:
    p.terminate()

passed = sum(1 for r in results if r[1]); print(f'\n{passed}/{len(results)} passed')
json.dump([{'test': n, 'passed': ok} for n, ok, _ in results], open(os.path.join(os.path.dirname(__file__), 'last_results.json'), 'w'), indent=1)
sys.exit(0 if passed == len(results) else 1)
