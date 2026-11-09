# SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
# SPDX-License-Identifier: AGPL-3.0-or-later
import json
import pathlib
import subprocess
import time

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
    adb('shell', 'am', 'start', '-W', '-n', package + '/com.owncloud.android.ui.activity.FileDisplayActivity')
    time.sleep(25)
    log = adb('logcat', '-d').stdout
    record('App starts without a fatal crash', 'FATAL EXCEPTION' not in log or ('Process: ' + package) not in log)
    record('Native app process remains running', bool(adb('shell', 'pidof', package).stdout.strip()))
    adb('shell', 'uiautomator', 'dump', '/sdcard/window.xml', check=False)
    xml = adb('shell', 'cat', '/sdcard/window.xml', check=False).stdout
    record('Existing Nextcloud login opens', 'Nextcloud' in xml or 'nextcloud.01084949031.kr' in xml)
    screenshot = subprocess.run(['adb', 'exec-out', 'screencap', '-p'], capture_output=True, check=True)
    (output / 'login-screen.png').write_bytes(screenshot.stdout)
finally:
    (output / 'results.json').write_text(json.dumps(results, indent=2))
    print(json.dumps(results, indent=2))
