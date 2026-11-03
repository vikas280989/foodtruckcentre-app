"""
Installs the APKs on the running emulator, launches the app and walks through every
main screen using the on-screen labels, taking screenshots and checking for crashes.
Usage: device_test.py <release.apk> <debug.apk> <out_dir>
"""
import os, re, subprocess, sys, time, xml.etree.ElementTree as ET

REL, DBG, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
PKG = 'au.com.foodtruckcentre.app'
os.makedirs(OUT, exist_ok=True)
log = open(os.path.join(OUT, 'device-test-report.txt'), 'w')
failures = []

def adb(*a, check=True, binary=False):
    r = subprocess.run(['adb', *a], capture_output=True, timeout=120)
    if check and r.returncode != 0:
        raise RuntimeError(f'adb {a} failed: {r.stderr.decode(errors="replace")}')
    return r.stdout if binary else r.stdout.decode(errors='replace')

def note(ok, msg):
    line = ('PASS ' if ok else 'FAIL ') + msg
    print(line); log.write(line + '\n'); log.flush()
    if not ok: failures.append(msg)

def shot(name):
    data = adb('exec-out', 'screencap', '-p', binary=True)
    open(os.path.join(OUT, f'{name}.png'), 'wb').write(data)

def ui():
    for _ in range(3):
        adb('shell', 'uiautomator', 'dump', '/sdcard/ui.xml', check=False)
        x = adb('shell', 'cat', '/sdcard/ui.xml', check=False)
        if '<hierarchy' in x:
            return ET.fromstring(x[x.index('<?xml') if '<?xml' in x else x.index('<hierarchy'):])
        time.sleep(1)
    return None

def find(text, exact=False):
    root = ui()
    if root is None: return None
    for n in root.iter('node'):
        for attr in ('text', 'content-desc'):
            v = n.get(attr) or ''
            if (v == text) if exact else (text.lower() in v.lower()):
                b = re.findall(r'\d+', n.get('bounds', ''))
                if len(b) == 4:
                    x1, y1, x2, y2 = map(int, b)
                    return ((x1 + x2) // 2, (y1 + y2) // 2)
    return None

def tap_text(text, exact=False, wait=10):
    end = time.time() + wait
    while time.time() < end:
        p = find(text, exact)
        if p:
            adb('shell', 'input', 'tap', str(p[0]), str(p[1])); time.sleep(1.5); return True
        time.sleep(1)
    return False

def visible(text, wait=10):
    end = time.time() + wait
    while time.time() < end:
        if find(text): return True
        time.sleep(1)
    return False

def alive():
    return adb('shell', 'pidof', PKG, check=False).strip() != ''

def crashed():
    return 'FATAL EXCEPTION' in adb('logcat', '-d', '-b', 'crash', check=False)

adb('logcat', '-c', check=False)
# 1. Install both APKs
for apk in (REL, DBG):
    out = adb('install', '-r', apk, check=False)
    note('Success' in out, f'install {os.path.basename(apk)}')
pk = adb('shell', 'pm', 'list', 'packages', 'au.com.foodtruckcentre')
note(f'package:{PKG}\n' in pk + '\n' or f'package:{PKG}\r' in pk, 'release package installed')
note(f'package:{PKG}.debug' in pk, 'debug package installed')
perms = adb('shell', 'dumpsys', 'package', PKG)
requested = re.findall(r'(android\.permission\.[A-Z_]+)', perms.split('requested permissions:')[1].split('install permissions:')[0]) if 'requested permissions:' in perms else []
note(set(requested) <= {'android.permission.INTERNET'}, f'only INTERNET permission requested ({", ".join(sorted(set(requested))) or "none"})')

# 2. Launch release build
adb('shell', 'monkey', '-p', PKG, '-c', 'android.intent.category.LAUNCHER', '1')
time.sleep(4); shot('01-launch-intro')
note(alive(), 'app launched and is running')
note(visible('Book Australia', 20), 'home screen shows hero "Book Australia’s Best Food Trucks"')
shot('02-home')

# 3. Scroll home
for i in range(3):
    adb('shell', 'input', 'swipe', '540', '1800', '540', '700', '400'); time.sleep(1.2)
shot('03-home-scrolled')
note(visible('Catering for every occasion', 5) or visible('Corporate Catering', 5) or visible('How to Book', 5), 'home sections render')

# 4. Tabs
for label, expect, name in [
    ('Services', 'Corporate Catering', '04-services'),
    ('Featuring', 'Featuring', '05-featuring'),
    ('Contact', 'Contact Us', '06-contact'),
    ('Request a Quote', 'Select Food Truck Cuisines', '07-quote'),
]:
    ok = tap_text(label, exact=True) or tap_text(label)
    time.sleep(2)
    note(ok and visible(expect, 15), f'navigate to {label} tab')
    shot(name)

# 5. Quote form: validation and cuisine limit
note(visible('Burgers / American'), 'cuisine list shows website categories')
for c in ['Burgers / American', 'Mexican / Latin', 'Asian Fusion', 'Vietnamese']:
    tap_text(c, exact=True)
note(visible('maximum of three cuisines', 5), 'fourth cuisine blocked with "maximum of three" message')
shot('08-quote-cuisine-limit')
note(visible('3/3', 3), 'cuisine counter shows 3/3')
# Scroll to submit and press it with empty fields
for i in range(12):
    adb('shell', 'input', 'swipe', '540', '1900', '540', '400', '250'); time.sleep(0.4)
tap_text('Send Quote Request', exact=True)
time.sleep(2)
note(visible('highlighted field', 8), 'empty required fields are rejected with a message')
shot('09-quote-validation')

# 6. Legal page + back
tap_text('Home', exact=True); time.sleep(2)
for i in range(10):
    adb('shell', 'input', 'swipe', '540', '1900', '540', '400', '250'); time.sleep(0.4)
if tap_text('Privacy Policy', exact=True):
    time.sleep(4)
    note(visible('Privacy', 10), 'privacy policy screen opens')
    shot('10-privacy')
    adb('shell', 'input', 'keyevent', '4'); time.sleep(1.5)

# 7. Rotate/background/foreground resilience
adb('shell', 'input', 'keyevent', '3'); time.sleep(2)
adb('shell', 'monkey', '-p', PKG, '-c', 'android.intent.category.LAUNCHER', '1'); time.sleep(3)
note(alive(), 'app resumes after going to background')

# 8. Debug build launches too
adb('shell', 'monkey', '-p', PKG + '.debug', '-c', 'android.intent.category.LAUNCHER', '1'); time.sleep(6)
note(adb('shell', 'pidof', PKG + '.debug', check=False).strip() != '', 'debug build launches')
shot('11-debug-build')

crash_log = adb('logcat', '-d', '-b', 'crash', check=False)
open(os.path.join(OUT, 'crash-log.txt'), 'w').write(crash_log)
note('FATAL EXCEPTION' not in crash_log, 'no crashes recorded during the whole test')
open(os.path.join(OUT, 'logcat.txt'), 'w').write(adb('logcat', '-d', '-t', '3000', check=False))

log.write(f'\n{len(failures)} failure(s)\n'); log.close()
print(f'{len(failures)} failure(s)')
sys.exit(1 if failures else 0)
