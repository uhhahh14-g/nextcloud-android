<!--
SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
SPDX-License-Identifier: AGPL-3.0-or-later
-->
# Nextcloud 210

Based on the official Nextcloud Android stable-35.0.x branch (35.0.2).

The Generic flavor is customized as `kr.appserver210.nextcloud`, with separate
account and provider authorities so the official app can remain installed.
Android 9 or newer is required. File operations and synchronization use the
upstream native implementation.

## Login and apps

The server is preset to https://nextcloud.01084949031.kr using Nextcloud Login
Flow v2. Sign in with an existing account and authorize access. Passwords and
passkeys are handled by the existing Nextcloud web login in the system browser;
the APK contains no account passwords or tokens.

The drawer adds 집회준비, 심층연구 and 성구. They open the existing server apps in
Android Custom Tabs, retaining the browser's existing session and passkey support.
Closing the tab returns to native files. If the web session expired, sign in again.
Native accounts and browser accounts are independent: when switching native
accounts, check the web account and sign out of the browser session if necessary.

No server app, user data, or authentication policy is changed by this build.

## Daily Bible audio synchronization

Version appserver210.3 uses UnifiedPush with the separately installed ntfy Android distributor.
Set its default server to https://ntfy.01084949031.kr, sign in with the phone-only
read credential shown to root in Daily Bible settings, and enable instant delivery.
Anonymous access is denied; this build does not use Firebase for Daily Bible delivery.
Opening native files registers the distributor. Server completion publishes a content-free
ready signal. WorkManager then authenticates with the native Nextcloud account and checks
the Asia/Seoul date, expected byte length and SHA-256 before replacing local audio.
Only accounts with access to the original daily-bible audio can register or fetch it.
The server records a successful device acknowledgement; delivery alone is not a save.

Audio is stored in the native account-specific app storage. No archive is downloaded.
Hourly network-constrained checks recover missed signals. Android can delay background
work during Doze or battery restrictions; force-stopped apps must be opened again.
For the phone, allow background battery use for ntfy and Nextcloud210. Closing the UI
is supported; force stop, disabled notifications, offline devices and powered-off phones
cannot be treated as guaranteed immediate delivery. Tasker storage access is separate.

## Build and signing

`.github/workflows/appserver210.yml` builds the Generic release, runs link-policy
unit tests, signs with a dedicated PKCS12 key stored in GitHub Actions Secrets,
and publishes an artifact with an APK, SHA-256 checksum, and certificate report.
The signing key is never committed or uploaded as an artifact. Keep the original
key securely to sign future updates with the same identity.

Code changes were assisted by Codex. No upstream pull request is submitted.
