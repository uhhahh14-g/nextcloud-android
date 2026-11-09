# SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
# SPDX-License-Identifier: AGPL-3.0-or-later
import json
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

package = 'kr.appserver210.nextcloud'
output = pathlib.Path('smoke-results')
output.mkdir(exist_ok=True)
results = []


def adb(*args, check=True):
    return subprocess.run(['adb', *args], check=check, capture_output=True, text=True, timeout=90)


def record(name, passed):
    results.append({'name': name, 'passed': bool(passed)})
    if not passed:
        raise RuntimeError(name)


try:
    adb('logcat', '-c')
    install = adb('install', '-r', 'dist/Nextcloud-210.apk')
    record('Signed release APK installs on Android 15', 'Success' in install.stdout)
    adb('shell', 'am', 'start', '-W', '-n', package + '/com.nmc.android.ui.LauncherActivity')
    time.sleep(25)
    log = adb('logcat', '-b', 'crash', '-d').stdout
    (output / 'crash-log.txt').write_text(log)
    record('App starts without a fatal crash', not re.search(r'Process: ' + re.escape(package) + r'(?:,|\s|$)', log))
    record('Native app process remains running', bool(adb('shell', 'pidof', package).stdout.strip()))
    # A fresh emulator has not completed Chrome's first-run flow yet.
    for attempt in range(30):
        adb('shell', 'uiautomator', 'dump', '/sdcard/window.xml', check=False)
        xml = adb('shell', 'cat', '/sdcard/window.xml', check=False).stdout
        (output / 'login-screen.xml').write_text(xml)
        if attempt in (0, 3, 10, 29):
            frame = subprocess.run(['adb', 'exec-out', 'screencap', '-p'], capture_output=True)
            (output / ('login-step-' + str(attempt) + '.png')).write_bytes(frame.stdout)
        if 'nextcloud.01084949031.kr' in xml and ('Log in' in xml or '로그인' in xml or 'Nextcloud' in xml):
            break
        try:
            nodes = ET.fromstring(xml).iter('node')
            for node in nodes:
                if node.get('text', '') in ('Use without an account', 'No thanks', 'Accept & continue', 'Got it'):
                    bounds = [int(n) for n in re.findall(r'\d+', node.get('bounds', ''))]
                    if len(bounds) == 4:
                        adb('shell', 'input', 'tap', str((bounds[0] + bounds[2]) // 2), str((bounds[1] + bounds[3]) // 2))
                        break
        except ET.ParseError:
            pass
        time.sleep(4)
    record('Existing Nextcloud login opens', 'nextcloud.01084949031.kr' in xml and ('Log in' in xml or '로그인' in xml or 'Nextcloud' in xml))
    screenshot = subprocess.run(['adb', 'exec-out', 'screencap', '-p'], capture_output=True, check=True)
    (output / 'login-screen.png').write_bytes(screenshot.stdout)
finally:
    diagnostic = adb('logcat', '-d', '-v', 'brief', check=False).stdout
    diagnostic = '\n'.join(line for line in diagnostic.splitlines() if re.search(r'ERR_|UnknownHost|SSLHandshake|SocketTimeout|net::|Connection refused|timeout', line, re.I))
    diagnostic = re.sub(r'https?://\S+', '[URL]', diagnostic)
    (output / 'network-errors.txt').write_text(diagnostic)
    crash = adb('logcat', '-b', 'crash', '-d', check=False).stdout
    (output / 'crash-log.txt').write_text(crash)
    screenshot = subprocess.run(['adb', 'exec-out', 'screencap', '-p'], capture_output=True)
    if screenshot.returncode == 0:
        (output / 'login-screen.png').write_bytes(screenshot.stdout)
    (output / 'results.json').write_text(json.dumps(results, indent=2))
    print(json.dumps(results, indent=2))
