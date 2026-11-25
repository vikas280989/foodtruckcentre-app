"""
Tests the WordPress plugin against the SAME request/response contract as backend/tests/run_tests.py,
using the exact URLs built into the released APK (/app-api/v1/*.php) plus the /wp-json/ftc/v1/* routes.
Needs a local WordPress at WP_DIR with the plugin active, the test mail mu-plugin, and
FTC_APP_ALLOW_HTTP_FOR_TESTS defined. Usage: run_plugin_tests.py <wp_dir> <router.php> <outbox_dir>
"""
import datetime, glob, json, os, subprocess, sys, time, urllib.request, uuid

WP, ROUTER, OUTBOX = sys.argv[1], sys.argv[2], sys.argv[3]
PORT = 8800
results = []
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

def wpcli(code):
    return subprocess.run(['php', os.environ.get('WP_CLI', '/tmp/wpt/wp-cli.phar'), '--allow-root', 'eval', code],
                          cwd=WP, capture_output=True, text=True).stdout.strip()

def req(path, body=None, method=None, ctype='application/json', raw=None):
    data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
    r = urllib.request.Request(f'http://127.0.0.1:{PORT}{path}', data=data, method=method or ('POST' if data is not None else 'GET'))
    if data is not None: r.add_header('Content-Type', ctype)
    try:
        resp = opener.open(r, timeout=30); t = resp.read()
        try: return resp.status, json.loads(t or b'{}')
        except Exception: return resp.status, {'raw': t[:200].decode(errors='replace')}
    except urllib.error.HTTPError as e:
        t = e.read()
        try: return e.code, json.loads(t)
        except Exception: return e.code, {'raw': t[:200].decode(errors='replace')}

def quote(**o):
    d = {'submission_id': str(uuid.uuid4()), 'elapsed_seconds': 60, 'website': '', 'name': 'Jane Citizen',
         'email': 'jane@example.com', 'phone': '0412 345 678',
         'event_date': (datetime.date.today() + datetime.timedelta(days=30)).isoformat(), 'food_time': '12:00pm',
         'event_address': '1 George St, Sydney NSW', 'postcode': '2000', 'event_type': 'Office Celebration',
         'meals': 120, 'paying': 'Organiser', 'power': 'Yes', 'private_land': 'No', 'entrance': 'Yes',
         'cuisines': ['Pizza / Italian', 'Vietnamese'], 'additional_info': 'Veg options please <script>alert(1)</script>'}
    d.update(o); return d

def contact(**o):
    d = {'submission_id': str(uuid.uuid4()), 'elapsed_seconds': 30, 'website': '', 'name': 'Sam Lee',
         'email': 'sam@example.com', 'topic': 'General enquiry', 'message': 'Hello, do you cover Newcastle?',
         'app_version': '1.0.0', 'device': ''}
    d.update(o); return d

def check(name, cond, detail=''):
    results.append((name, bool(cond))); print(('PASS ' if cond else 'FAIL ') + name + ('' if cond else f'  -> {detail}'))

def outbox(): return sorted(glob.glob(os.path.join(OUTBOX, '*.json')), key=os.path.getmtime)
def last_mail(): f = outbox(); return json.load(open(f[-1])) if f else {}

for f in glob.glob(os.path.join(OUTBOX, '*')): os.remove(f)
wpcli('global $wpdb; $wpdb->query("DELETE FROM ".ftc_app_table()); global $wpdb; $wpdb->query("DELETE FROM {$wpdb->options} WHERE option_name LIKE \'%ftc_app_rl_%\'"); update_option("ftc_app_rate_10min", 1000); update_option("ftc_app_rate_day", 5000);')
env = dict(os.environ); env.pop('HTTPS_PROXY', None)
srv = subprocess.Popen(['php', '-S', f'127.0.0.1:{PORT}', ROUTER], cwd=os.path.dirname(ROUTER), env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
time.sleep(1.5)
try:
    for base in ['/app-api/v1/app-config.php', '/wp-json/ftc/v1/app-config']:
        s, b = req(base); check(f'config {base}', s == 200 and b.get('ok') and b.get('contact_email') == 'info@foodtruckcentre.com.au', (s, b))

    s, b = req('/app-api/v1/quote.php', quote()); check('valid quote accepted (APK URL)', s == 200 and b.get('ok') and b.get('enquiry_id', '').startswith('FTC-'), (s, b))
    ref = b.get('enquiry_id', '')
    check('enquiry_id is route-safe', ref and all(c.isalnum() or c == '-' for c in ref), ref)
    m = last_mail()
    check('quote emailed to configured recipient', m.get('to') == 'info@foodtruckcentre.com.au', m.get('to'))
    exp = (datetime.date.today() + datetime.timedelta(days=30)).strftime('%a %-d %b %Y')
    check('subject format', m.get('subject') == f'New Food Truck Catering Enquiry – Jane Citizen – {exp}', m.get('subject'))
    html = m.get('message', '')
    check('HTML escapes input (XSS)', '<script>' not in html and '&lt;script&gt;' in html)
    check('email contains all fields', all(x in html for x in ['Jane Citizen', 'jane@example.com', '0412 345 678', '2000', 'Office Celebration', '120', 'Organiser', 'Pizza / Italian, Vietnamese', '12:00pm', '1 George St', ref]))
    hdrs = ' '.join(m.get('headers') or [])
    check('reply-to is customer, HTML content type', 'Reply-To: "Jane Citizen" <jane@example.com>' in hdrs and 'text/html' in hdrs, hdrs)
    stored = wpcli(f'global $wpdb; echo $wpdb->get_var($wpdb->prepare("SELECT status FROM ".ftc_app_table()." WHERE enquiry_ref=%s", "{ref}"));')
    check('enquiry stored in WordPress as sent', stored == 'sent', stored)

    s, b = req('/wp-json/ftc/v1/quote', quote(name='Rest Route', email='rest@example.com')); check('valid quote accepted (REST URL)', s == 200 and b.get('ok'), (s, b))

    q = quote(name='Dup Test'); s1, b1 = req('/app-api/v1/quote.php', q); n = len(outbox()); s2, b2 = req('/app-api/v1/quote.php', q)
    check('same submission_id not emailed twice', s2 == 200 and b2.get('duplicate') and len(outbox()) == n and b1['enquiry_id'] == b2['enquiry_id'], (b1, b2))
    n = len(outbox()); s3, b3 = req('/app-api/v1/quote.php', dict(q, submission_id=str(uuid.uuid4())))
    check('identical content with new id not emailed twice', s3 == 200 and b3.get('duplicate') and len(outbox()) == n, b3)

    for label, over, field in [
        ('empty name', {'name': ''}, 'name'), ('invalid email', {'email': 'not-an-email'}, 'email'),
        ('header injection in email', {'email': 'a@b.com\nBcc: x@y.com'}, 'email'), ('invalid phone', {'phone': '12ab'}, 'phone'),
        ('meals below 50', {'meals': 49}, 'meals'), ('meals not a number', {'meals': 'fifty'}, 'meals'),
        ('more than three cuisines', {'cuisines': ['Seafood', 'Vietnamese', 'Asian Fusion', 'Pizza / Italian']}, 'cuisines'),
        ('unknown cuisine', {'cuisines': ['Sushi']}, 'cuisines'), ('past date', {'event_date': '2020-01-01'}, 'event_date'),
        ('invalid date', {'event_date': '2026-02-30'}, 'event_date'), ('bad postcode', {'postcode': '20'}, 'postcode'),
        ('event type not on website', {'event_type': 'Wedding'}, 'event_type'), ('missing paying', {'paying': ''}, 'paying'),
        ('bad power', {'power': 'Maybe'}, 'power'), ('missing private land', {'private_land': None}, 'private_land'),
        ('missing entrance', {'entrance': ''}, 'entrance'), ('too many links', {'additional_info': 'http://a http://b http://c http://d'}, 'additional_info')]:
        s, b = req('/app-api/v1/quote.php', quote(**over)); check(f'rejects {label}', s == 422 and field in b.get('fields', {}), (s, b))

    s, b = req('/app-api/v1/quote.php', quote(website='http://spam')); check('honeypot blocks bots', s == 400 and b.get('error') == 'rejected', (s, b))
    s, b = req('/app-api/v1/quote.php', quote(elapsed_seconds=1)); check('too-fast submission blocked', s == 400, (s, b))
    s, b = req('/app-api/v1/quote.php', quote(submission_id='abc')); check('invalid submission id rejected', s == 400, (s, b))
    s, b = req('/app-api/v1/quote.php', method='GET'); check('GET not allowed on quote', s == 405 and 'message' in b, (s, b))
    s, b = req('/app-api/v1/quote.php', raw=b'name=x', ctype='application/x-www-form-urlencoded'); check('non-JSON rejected', s == 415, s)
    s, b = req('/app-api/v1/quote.php', raw=b'[1,2]'); check('JSON array rejected', s == 400, s)
    s, b = req('/app-api/v1/quote.php', raw=json.dumps(quote(additional_info='x' * 20000)).encode()); check('oversized body rejected', s == 413, s)
    s, b = req('/app-api/v1/quote.php', quote(name="Robert'); DROP TABLE wp_users;--", email='bob@example.com', phone='0400000000'))
    users = wpcli('echo count_users()["total_users"];')
    check('SQL injection attempt stored as plain text, users table intact', s == 200 and users == '1', (s, b, users))

    s, b = req('/app-api/v1/contact.php', contact()); m = last_mail()
    check('contact message delivered', s == 200 and m.get('to') == 'info@foodtruckcentre.com.au' and 'Sam Lee' in m.get('message', ''), (s, b))
    s, b = req('/app-api/v1/contact.php', contact(message='')); check('contact requires message', s == 422 and 'message' in b.get('fields', {}), b)
    s, b = req('/app-api/v1/contact.php', contact(topic='Hack')); check('contact rejects unknown topic', s == 422, b)
    s, b = req('/app-api/v1/contact.php', contact(topic='App support', device='Pixel 8')); check('app support delivered with device', s == 200 and 'Pixel 8' in last_mail().get('message', ''), b)

    open(os.path.join(OUTBOX, 'FAIL'), 'w').close()
    q = quote(name='Fail Test', email='fail@example.com'); s, b = req('/app-api/v1/quote.php', q)
    check('mail failure returns error (no false success)', s == 502 and b.get('ok') is False and 'message' in b, (s, b))
    st = wpcli('global $wpdb; echo $wpdb->get_var("SELECT CONCAT(status,\'|\',attempts,\'|\',last_error) FROM ".ftc_app_table()." WHERE name=\'Fail Test\'");')
    check('failed enquiry kept in WordPress with reason', st.startswith('failed|1|') and 'simulated' in st, st)
    os.remove(os.path.join(OUTBOX, 'FAIL'))
    s, b = req('/app-api/v1/quote.php', q)
    st = wpcli('global $wpdb; echo $wpdb->get_var("SELECT CONCAT(status,\'|\',COUNT(*)) FROM ".ftc_app_table()." WHERE name=\'Fail Test\'");')
    check('retry with same id succeeds, one record only', s == 200 and b.get('ok') and st == 'sent|1', (s, b, st))

    open(os.path.join(OUTBOX, 'FAIL'), 'w').close()
    req('/app-api/v1/contact.php', contact(name='Cron Test', message='Retry me later please'))
    os.remove(os.path.join(OUTBOX, 'FAIL'))
    wpcli('ftc_app_retry_failed();')
    st = wpcli('global $wpdb; echo $wpdb->get_var("SELECT status FROM ".ftc_app_table()." WHERE name=\'Cron Test\'");')
    check('scheduled retry delivers failed enquiries', st == 'sent' and 'Cron Test' in last_mail().get('message', ''), st)

    wpcli('update_option("ftc_app_rate_10min", 3);')
    wpcli('global $wpdb; $wpdb->query("DELETE FROM {$wpdb->options} WHERE option_name LIKE \'%ftc_app_rl_%\'");')
    codes = [req('/app-api/v1/contact.php', contact(message=f'rate limit message {i}'))[0] for i in range(5)]
    check('rate limit after 3 per 10 minutes', codes[:3] == [200, 200, 200] and codes[3:] == [429, 429], codes)
    wpcli('update_option("ftc_app_rate_10min", 5); update_option("ftc_app_rate_day", 20);')

    s, b = req('/app-api/v1/other.php'); check('other /app-api paths are left to WordPress (not handled by the plugin)', 'ok' not in b and 'error' not in b, (s, b))
finally:
    srv.terminate()

passed = sum(1 for _, ok in results if ok); print(f'\n{passed}/{len(results)} passed')
sys.exit(0 if passed == len(results) else 1)
