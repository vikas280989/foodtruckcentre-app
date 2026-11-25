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

TOKEN = {'v': ''}
def req(path, body=None, method=None, ctype='application/json', raw=None, token=None):
    data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
    r = urllib.request.Request(f'http://127.0.0.1:{PORT}{path}', data=data, method=method or ('POST' if data is not None else 'GET'))
    if data is not None: r.add_header('Content-Type', ctype)
    tok = TOKEN['v'] if token is None else token
    if tok: r.add_header('X-FTC-Token', tok)
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

def reset_rate_limits():
    wpcli('global $wpdb; $wpdb->query("DELETE FROM {$wpdb->options} WHERE option_name LIKE \'%ftc_app_rl_%\'");')

def outbox(): return sorted(glob.glob(os.path.join(OUTBOX, '*.json')), key=os.path.getmtime)
def last_mail(): f = outbox(); return json.load(open(f[-1])) if f else {}

for f in glob.glob(os.path.join(OUTBOX, '*')): os.remove(f)
wpcli('require_once ABSPATH."wp-admin/includes/user.php"; foreach (get_users(["role"=>"ftc_customer"]) as $u) wp_delete_user($u->ID); global $wpdb; $wpdb->query("DELETE FROM ".ftc_app_tokens_table()); $wpdb->query("DELETE FROM ".ftc_app_table()); global $wpdb; $wpdb->query("DELETE FROM {$wpdb->options} WHERE option_name LIKE \'%ftc_app_rl_%\'"); update_option("ftc_app_rate_10min", 1000); update_option("ftc_app_rate_day", 5000);')
env = dict(os.environ); env.pop('HTTPS_PROXY', None)
srv = subprocess.Popen(['php', '-S', f'127.0.0.1:{PORT}', ROUTER], cwd=os.path.dirname(ROUTER), env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
time.sleep(1.5)
try:
    for base in ['/app-api/v1/app-config.php', '/wp-json/ftc/v1/app-config']:
        s, b = req(base); check(f'config {base}', s == 200 and b.get('ok') and b.get('contact_email') == 'info@foodtruckcentre.com.au', (s, b))

    # ---------- Accounts ----------
    s, b = req('/app-api/v1/quote.php', quote(), token=''); check('quote without login refused (401, update message)', s == 401 and b.get('error') == 'login_required' and 'update' in b.get('message', ''), (s, b))
    s, b = req('/app-api/v1/contact.php', contact(), token=''); check('contact without login refused', s == 401, (s, b))
    s, b = req('/app-api/v1/my-enquiries.php', token=''); check('my-enquiries without login refused', s == 401, (s, b))
    reg = {'name': 'Jane Citizen', 'email': 'Jane@Example.com', 'phone': '0412 345 678', 'password': 'Secret123'}
    for label, over, field in [('short password', {'password': 'abc1'}, 'password'), ('password without number', {'password': 'abcdefgh'}, 'password'),
                               ('bad email', {'email': 'x'}, 'email'), ('bad phone', {'phone': '1'}, 'phone'), ('empty name', {'name': ''}, 'name')]:
        s, b = req('/app-api/v1/auth-register.php', dict(reg, **over), token=''); check(f'register rejects {label}', s == 422 and field in b.get('fields', {}), (s, b))
    reset_rate_limits()
    s, b = req('/app-api/v1/auth-register.php', reg, token='')
    check('register creates account and returns token + profile', s == 200 and len(b.get('token', '')) == 64 and b.get('profile', {}).get('email') == 'jane@example.com' and b['profile'].get('phone') == '0412 345 678', (s, b))
    TOKEN['v'] = b.get('token', '')
    role = wpcli('$u = get_user_by("email","jane@example.com"); echo implode(",", $u->roles), "|", user_can($u, "edit_posts") ? "can-edit" : "no-edit";')
    check('account is a WordPress user with Customer role only (no editing rights)', role == 'ftc_customer|no-edit', role)
    stored = wpcli('global $wpdb; echo $wpdb->get_var("SELECT COUNT(*) FROM ".ftc_app_tokens_table()." WHERE token_hash = \'" . hash("sha256", "' + TOKEN['v'] + '") . "\'"), "|", $wpdb->get_var("SELECT COUNT(*) FROM ".ftc_app_tokens_table()." WHERE token_hash = \'' + TOKEN['v'] + '\'");')
    check('only the hash of the token is stored', stored == '1|0', stored)
    s, b = req('/app-api/v1/auth-register.php', reg, token=''); check('duplicate email refused', s == 422 and 'already exists' in b.get('fields', {}).get('email', ''), (s, b))
    s, b = req('/app-api/v1/auth-login.php', {'email': 'jane@example.com', 'password': 'Wrong1234'}, token=''); check('wrong password refused with generic message', s == 401 and b.get('message') == 'Email or password is incorrect.', (s, b))
    s, b = req('/app-api/v1/auth-login.php', {'email': 'nobody@example.com', 'password': 'Wrong1234'}, token=''); check('unknown email gives the same message', s == 401 and b.get('message') == 'Email or password is incorrect.', (s, b))
    s, b = req('/app-api/v1/auth-login.php', {'email': 'JANE@example.com', 'password': 'Secret123'}, token=''); check('login works (email case-insensitive)', s == 200 and len(b.get('token', '')) == 64, (s, b))
    login_token = b.get('token', '')
    s, b = req('/app-api/v1/me.php'); check('me returns profile', s == 200 and b.get('profile', {}).get('name') == 'Jane Citizen', (s, b))
    s, b = req('/app-api/v1/me.php', {'name': 'Jane Q Citizen', 'phone': '0499 000 111'}); check('profile update', s == 200 and b['profile']['name'] == 'Jane Q Citizen' and b['profile']['phone'] == '0499 000 111', (s, b))
    req('/app-api/v1/me.php', {'name': 'Jane Citizen', 'phone': '0412 345 678'})
    s, b = req('/app-api/v1/me.php', {'name': '', 'phone': 'x'}); check('profile update validated', s == 422, (s, b))
    s, b = req('/app-api/v1/auth-logout.php', {}, token=login_token); s2, b2 = req('/app-api/v1/me.php', token=login_token)
    check('logout invalidates that session only', s == 200 and s2 == 401 and req('/app-api/v1/me.php')[0] == 200, (s, s2))
    s, b = req('/app-api/v1/auth-forgot.php', {'email': 'jane@example.com'}, token=''); fm = last_mail()
    check('forgot password sends WordPress reset email', s == 200 and fm.get('to') == 'jane@example.com' and 'wp-login.php?' in fm.get('message', '') and 'rp' in fm.get('message', ''), (s, b, fm.get('subject')))
    n = len(outbox()); s, b = req('/app-api/v1/auth-forgot.php', {'email': 'nobody@example.com'}, token='')
    check('forgot password gives the same answer for unknown emails, sends nothing', s == 200 and len(outbox()) == n, (s, b))
    admin_try = wpcli('$u=get_user_by("email","jane@example.com"); echo user_can($u,"manage_options") ? "admin" : "customer";')
    check('customer cannot manage the site', admin_try == 'customer', admin_try)

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

    linked = wpcli(f'global $wpdb; $u=get_user_by("email","jane@example.com"); echo $wpdb->get_var($wpdb->prepare("SELECT user_id FROM ".ftc_app_table()." WHERE enquiry_ref=%s", "{ref}")) == $u->ID ? "linked" : "no";')
    check('enquiry linked to the customer account', linked == 'linked', linked)
    s, b = req('/app-api/v1/my-enquiries.php'); items = b.get('enquiries', [])
    check('my-enquiries lists it with stage New', s == 200 and items and items[0]['enquiry_id'] == ref and items[0]['stage'] == 'new' and items[0]['stage_label'] == 'New' and 'Office Celebration' in items[0]['summary'], (s, b))
    wpcli(f'global $wpdb; $wpdb->update(ftc_app_table(), ["stage"=>"quoted"], ["enquiry_ref"=>"{ref}"]);')
    s, b = req('/app-api/v1/my-enquiries.php'); check('stage set by admin is shown to the customer', b.get('enquiries', [{}])[0].get('stage_label') == 'Quoted', b)
    s, b = req('/app-api/v1/auth-register.php', {'name': 'Other Person', 'email': 'other@example.com', 'phone': '0400 111 222', 'password': 'Other1234'}, token='')
    s, b = req('/app-api/v1/my-enquiries.php', token=b.get('token')); check("customers can't see each other's enquiries", s == 200 and b.get('enquiries') == [], (s, b))

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
    users_before = wpcli('echo count_users()["total_users"];')
    s, b = req('/app-api/v1/quote.php', quote(name="Robert'); DROP TABLE wp_users;--", email='bob@example.com', phone='0400000000'))
    users = wpcli('echo count_users()["total_users"];')
    check('SQL injection attempt stored as plain text, users table intact', s == 200 and users == users_before and users != '', (s, b, users_before, users))

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

    reset_rate_limits()
    for i in range(5): req('/app-api/v1/auth-login.php', {'email': 'other@example.com', 'password': f'Bad{i}xxxx1'}, token='')
    s, b = req('/app-api/v1/auth-login.php', {'email': 'other@example.com', 'password': 'Other1234'}, token=''); check('account locked after 5 wrong passwords', s == 429 and b.get('error') == 'locked', (s, b))
    s, b = req('/app-api/v1/me-delete.php', {'password': 'wrong'}); check('delete account needs the right password', s == 422, (s, b))
    s, b = req('/app-api/v1/me-delete.php', {'password': 'Secret123'})
    gone = wpcli('echo get_user_by("email","jane@example.com") ? "exists" : "deleted";')
    kept = wpcli('global $wpdb; echo $wpdb->get_var("SELECT COUNT(*) FROM ".ftc_app_table()." WHERE name=\'Jane Citizen\' AND user_id=0");')
    check('delete account removes the user and session, keeps enquiries unlinked', s == 200 and gone == 'deleted' and int(kept or 0) >= 1 and req('/app-api/v1/me.php')[0] == 401, (s, gone, kept))

    s, b = req('/app-api/v1/other.php'); check('other /app-api paths are left to WordPress (not handled by the plugin)', 'ok' not in b and 'error' not in b, (s, b))
finally:
    srv.terminate()

passed = sum(1 for _, ok in results if ok); print(f'\n{passed}/{len(results)} passed')
sys.exit(0 if passed == len(results) else 1)
