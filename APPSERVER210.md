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

Version appserver210.2 automatically downloads the account's accessible
`/daily-bible/audio/daily-bible-review.mp3` after the native file screen opens.
WorkManager checks it hourly while connected, including metered mobile networks.
Android may defer background work during Doze, battery restrictions, or a force stop;
this is not an exact-time alarm. Opening the file screen requests a check again.

The worker uses the existing account credentials, compares the remote ETag and
file size, and uses the native download operation and account-specific storage.
It does not upload local changes, download archives, or modify server files.
Unchanged audio is not downloaded again; interrupted or failed checks retry with
exponential backoff starting at 15 minutes. The existing Android account remains
installed when updating this APK with the same signing key.

## Build and signing

`.github/workflows/appserver210.yml` builds the Generic release, runs link-policy
unit tests, signs with a dedicated PKCS12 key stored in GitHub Actions Secrets,
and publishes an artifact with an APK, SHA-256 checksum, and certificate report.
The signing key is never committed or uploaded as an artifact. Keep the original
key securely to sign future updates with the same identity.

Code changes were assisted by Codex. No upstream pull request is submitted.
