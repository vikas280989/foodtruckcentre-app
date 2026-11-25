"""
End-to-end device test on the Android emulator (GitHub Actions).

- Release APK: installs, launches, shows the login screen, requests only INTERNET, no crash.
- E2E debug APK (same code, pointed at a local test WordPress with the FTC plugin on the runner):
  create account -> browse every tab -> request a quote (real form, date picker) -> success screen ->
  My enquiries shows it -> server has it linked to the account -> log out -> log back in.

Usage: device_test.py <release.apk> <e2e-debug.apk> <out_dir>
Env:   WP_DIR, WP_CLI  (local WordPress used to verify what the server stored)
"""
import datetime, os, re, subprocess, sys, time, xml.etree.ElementTree as ET

REL, DBG, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
PKG = 'au.com.foodtruckcentre.app'
DPKG = PKG + '.debug'
WP_DIR, WP_CLI = os.environ.get('WP_DIR', ''), os.environ.get('WP_CLI', '')
os.makedirs(OUT, exist_ok=True)
log = open(os.path.join(OUT, 'device-test-report.txt'), 'w')
failures = []
STAMP = str(int(time.time()))
EMAIL = f'e2e{STAMP}@example.com'
PASSWORD = 'Secret123'
NAME = 'Eve Tester'


def adb(*a, check=True, binary=False, timeout=120):
    r = subprocess.run(['adb', *a], capture_output=True, timeout=timeout)
    if check and r.returncode != 0:
        raise RuntimeError(f'adb {a} failed: {r.stderr.decode(errors="replace")}')
    return r.stdout if binary else r.stdout.decode(errors='replace')


def wpcli(code):
    if not (WP_DIR and WP_CLI):
        return ''
    return subprocess.run(['php', WP_CLI, '--allow-root', f'--path={WP_DIR}', 'eval', code], capture_output=True, text=True).stdout.strip()


def shot(name):
    open(os.path.join(OUT, f'{name}.png'), 'wb').write(adb('exec-out', 'screencap', '-p', binary=True))


# ---------- UI helpers (uiautomator) ----------

def raw_ui():
    for _ in range(3):
        adb('shell', 'uiautomator', 'dump', '/sdcard/ui.xml', check=False)
        x = adb('shell', 'cat', '/sdcard/ui.xml', check=False)
        if '<hierarchy' in x:
            return ET.fromstring(x[x.index('<?xml') if '<?xml' in x else x.index('<hierarchy'):])
        time.sleep(1)
    return None


def dismiss_system_dialogs(root):
    """CI emulators sometimes show 'System UI isn't responding'. Tap 'Wait' and carry on."""
    if root is None:
        return False
    texts = [n.get('text') or '' for n in root.iter('node')]
    if not any("isn't responding" in t or 'keeps stopping' in t for t in texts):
        return False
    for n in root.iter('node'):
        if (n.get('text') or '') == 'Wait':
            x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds', '')))
            adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2)); time.sleep(2)
            print('     (dismissed a system "not responding" dialog)')
            return True
    return False


def ui():
    for _ in range(5):
        root = raw_ui()
        if root is not None and dismiss_system_dialogs(root):
            continue
        if root is not None:
            return root
        time.sleep(1)
    return None


def nodes(text, exact=False):
    root = ui()
    out = []
    if root is None:
        return out
    for n in root.iter('node'):
        for attr in ('text', 'content-desc'):
            v = n.get(attr) or ''
            if (v == text) if exact else (text.lower() in v.lower()):
                b = re.findall(r'\d+', n.get('bounds', ''))
                if len(b) == 4:
                    x1, y1, x2, y2 = map(int, b)
                    out.append(((x1 + x2) // 2, (y1 + y2) // 2))
                break
    return sorted(set(out), key=lambda p: (p[1], p[0]))


def visible_texts():
    root = ui()
    if root is None:
        return []
    out = []
    for n in root.iter('node'):
        for attr in ('text', 'content-desc'):
            v = (n.get(attr) or '').strip()
            if v and v not in out:
                out.append(v)
    return out


def visible(text, wait=10, exact=False):
    end = time.time() + wait
    while time.time() < end:
        if nodes(text, exact):
            return True
        time.sleep(1)
    return False


def tap_xy(p):
    adb('shell', 'input', 'tap', str(p[0]), str(p[1])); time.sleep(1.2)


def tap(text, exact=True, index=0, wait=10):
    end = time.time() + wait
    while time.time() < end:
        found = nodes(text, exact)
        if found:
            tap_xy(found[index if index >= 0 else len(found) + index])
            return True
        time.sleep(1)
    return False


def keyboard_shown():
    return 'mInputShown=true' in adb('shell', 'dumpsys', 'input_method', check=False)


def hide_keyboard():
    if keyboard_shown():
        adb('shell', 'input', 'keyevent', '4'); time.sleep(0.8)


def type_text(t):
    adb('shell', 'input', 'text', t.replace(' ', '%s')); time.sleep(0.6)


def fill(label, value):
    """Tap a text field by its label and type into it."""
    if not tap(label, exact=False):
        return False
    adb('shell', 'input', 'keyevent', 'KEYCODE_MOVE_END')
    type_text(value)
    return True


def swipe_up(times=1, fast=False):
    for _ in range(times):
        adb('shell', 'input', 'swipe', '540', '1750', '540', '850', '250' if fast else '450'); time.sleep(0.9)


def scroll_to(text, exact=True, max_swipes=14):
    for _ in range(max_swipes):
        found = nodes(text, exact)
        if found and 300 < found[0][1] < 1950:
            return True
        swipe_up()
    return bool(nodes(text, exact))


def note(ok, msg):
    line = ('PASS ' if ok else 'FAIL ') + msg
    print(line); log.write(line + '\n'); log.flush()
    if not ok:
        failures.append(msg)
        diag = ' | '.join(t.replace('\n', ' ')[:50] for t in visible_texts()[:40])
        print('     on screen: ' + diag); log.write('     on screen: ' + diag + '\n')


def alive(pkg):
    return adb('shell', 'pidof', pkg, check=False).strip() != ''


def launch(pkg):
    adb('shell', 'monkey', '-p', pkg, '-c', 'android.intent.category.LAUNCHER', '1'); time.sleep(4)


# ---------- Settle emulator ----------
for _ in range(60):
    if adb('shell', 'getprop', 'sys.boot_completed', check=False).strip() == '1':
        break
    time.sleep(2)
time.sleep(25)
adb('shell', 'am', 'broadcast', '-a', 'android.intent.action.CLOSE_SYSTEM_DIALOGS', check=False)
adb('shell', 'input', 'keyevent', '82', check=False)
ui()
adb('logcat', '-c', check=False)
adb('logcat', '-b', 'crash', '-c', check=False)

# ---------- 1. Install ----------
for apk in (REL, DBG):
    out = adb('install', '-r', apk, check=False)
    note('Success' in out, f'install {os.path.basename(apk)}')
perms = adb('shell', 'dumpsys', 'package', PKG)
requested = re.findall(r'(android\.permission\.[A-Z_]+)', perms.split('requested permissions:')[1].split('install permissions:')[0]) if 'requested permissions:' in perms else []
note(set(requested) <= {'android.permission.INTERNET'}, f'release requests only INTERNET ({", ".join(sorted(set(requested))) or "none"})')

# ---------- 2. Release build: launches to the login screen ----------
launch(PKG)
note(alive(PKG), 'release app launches')
note(visible('Welcome back', 20), 'release app opens on the login screen (login required)')
shot('01-release-login')
adb('shell', 'am', 'force-stop', PKG)

# ---------- 3. E2E: create account ----------
launch(DPKG)
note(visible('Welcome back', 20), 'debug app opens on the login screen')
tap('Create account', exact=True, index=0)
note(visible('Create your account', 8), 'switches to Create account')
fill('Full name', NAME)
fill('Email', EMAIL)
fill('Phone number', '0412 345 678')
fill('Password', PASSWORD)
hide_keyboard()
shot('02-signup-filled')
scroll_to('Accept terms', exact=True)
tap('Accept terms')
scroll_to('Create my account')
tap('Create my account')
note(visible('Book Australia', 25), 'account created and home screen shown')
shot('03-home-logged-in')
uid = wpcli(f'$u=get_user_by("email","{EMAIL}"); echo $u ? implode(",",$u->roles) : "none";')
note(uid == 'ftc_customer', f'server created WordPress user with Customer role ({uid})')

# ---------- 4. Browse tabs ----------
for label, expect, name in [('Services', 'Corporate Catering', '04-services'), ('Featuring', 'Featuring', '05-featuring'), ('Contact', 'Contact Us', '06-contact')]:
    ok = tap(label, exact=True, index=-1)
    note(ok and visible(expect, 15), f'navigate to {label}')
    shot(name)
note(visible(NAME, 5), 'contact form is pre-filled with the customer name')

# ---------- 5. Request a quote (real form) ----------
tap('Request a Quote', exact=True, index=-1)
note(visible('Select Food Truck Cuisines', 15), 'quote form opens')
for c in ['Pizza / Italian', 'Vietnamese']:
    scroll_to(c); tap(c)
note(visible('2/3', 5), 'two cuisines selected')
scroll_to('Your details')
note(visible(NAME, 5) and visible(EMAIL, 5), 'name and email pre-filled from the account')

scroll_to('When is the event *', exact=False)
tap('When is the event *', exact=False)
target = datetime.date.today() + datetime.timedelta(days=5)
if target.month != datetime.date.today().month:
    tap('Change to next month', exact=False)
picked = tap(str(target.day), exact=True)
tap('Confirm', exact=True)
note(picked and visible(target.strftime('%b %Y'), 5, exact=False), f'event date chosen with the date picker ({target})')
fill('Postcode', '2000')
hide_keyboard()
scroll_to('Office Celebration'); tap('Office Celebration')
scroll_to('How many meals', exact=False)
fill('How many meals', '120')
hide_keyboard()
scroll_to('Organiser'); tap('Organiser')
scroll_to('Is there power available', exact=False)
swipe_up()
for p in [p for p in nodes('Yes', exact=True) if 250 < p[1] < 2050]:
    tap_xy(p)
shot('07-quote-filled')
scroll_to('Send Quote Request')
tap('Send Quote Request')
ok = visible('Thank you', 25)
if not ok:
    # One of the Yes/No questions may still be unanswered: answer the highlighted ones and resend.
    for _ in range(4):
        warn = nodes('Please answer this question.', exact=True)
        if not warn:
            break
        yes = [p for p in nodes('Yes', exact=True) if p[1] < warn[0][1]]
        if yes: tap_xy(yes[-1])
    scroll_to('Send Quote Request'); tap('Send Quote Request')
    ok = visible('Thank you', 25)
note(ok, 'quote sent – success screen shown')
shot('08-quote-success')
ref = next((t for t in visible_texts() if re.match(r'^FTC-\d{6}-[A-F0-9]{6}$', t)), '')
note(bool(ref), f'enquiry reference shown ({ref})')
stored = wpcli(f'global $wpdb; $u=get_user_by("email","{EMAIL}"); echo $wpdb->get_var($wpdb->prepare("SELECT CONCAT(status,\'|\',user_id=%d) FROM ".ftc_app_table()." WHERE enquiry_ref=%s", $u->ID, "{ref}"));')
note(stored == 'sent|1', f'server stored the enquiry, emailed it, and linked it to the account ({stored})')

# ---------- 6. My enquiries ----------
tap('View my enquiries')
note(visible('My enquiries', 15) and visible('Catering quote', 15) and visible(ref, 5), 'My enquiries lists the new quote')
note(visible('New', 5, exact=True), 'status shown as New')
shot('09-my-enquiries')

# ---------- 7. Log out and log back in ----------
scroll_to('Log out')
tap('Log out', exact=True, index=0)
tap('Log out', exact=True, index=-1)   # confirm in dialog
note(visible('Welcome back', 15), 'log out returns to the login screen')
shot('10-logged-out')
fill('Email', EMAIL)
fill('Password', PASSWORD)
hide_keyboard()
tap('Log in', exact=True, index=-1)
note(visible('Book Australia', 25), 'log in again with email and password')
shot('11-logged-in-again')

# ---------- 8. Resilience + crashes ----------
adb('shell', 'input', 'keyevent', '3'); time.sleep(2)
launch(DPKG)
note(alive(DPKG) and visible('Book Australia', 15), 'app resumes logged in after going to background')

crash_log = adb('logcat', '-d', '-b', 'crash', check=False)
open(os.path.join(OUT, 'crash-log.txt'), 'w').write(crash_log)
app_crashes = [blk for blk in crash_log.split('FATAL EXCEPTION')[1:] if f'Process: {PKG}' in blk]
for blk in app_crashes:
    print('APP CRASH: FATAL EXCEPTION' + blk[:2500])
note(not app_crashes, 'no app crashes during the whole test')
open(os.path.join(OUT, 'logcat.txt'), 'w').write(adb('logcat', '-d', '-t', '3000', check=False))

log.write(f'\n{len(failures)} failure(s)\n'); log.close()
print(f'{len(failures)} failure(s)')
sys.exit(1 if failures else 0)
