# YEŞİLİZ Debug Mission State

MISSION_START: 2026-08-11 00:09:10 +03 (Europe/Istanbul)
MISSION_START_EPOCH: 1786396150
TARGET_END: 2026-08-11 07:09:10 +03 (Europe/Istanbul)
TARGET_END_EPOCH: 1786421350
CURRENT_PHASE: Parent/privacy, authentication-secret, lifecycle and minSdk fixes complete; continued device/release/adversarial audit
LAST_CHECKPOINT: 2026-08-11 02:31:48 +03
CURRENT_BRANCH: fix/turan-debug
LAST_VALIDATED_COMMIT: 0de1757 (first critical-fix wave; JVM/backend/API 36 tests green)
CURRENT_DEVICE: Samsung SM-A217F (serial RF8NA115KCD), Android 12 / API 31, 720x1600 @280dpi; API 36 emulator also connected
BUILD_STATUS: PASS — forced debug assemble and Android lint (zero errors)
UNIT_TEST_STATUS: PASS — latest forced full run: 30 core-risk + 200 app JVM tests
DEVICE_TEST_STATUS: PASS — latest API 36 suite: 40 passed, 4 permission-precondition skips. Earlier physical API 31 suite: 42 passed, no skip/failure; live account/invite/network recovery passed. Latest parent/service/minSdk changes await refreshed physical run.
BACKEND_TEST_STATUS: PASS — latest full suite: 89 passed; PostgreSQL migrations at `f65a92c7d814` head
TOP_OPEN_BUGS: Physical parent/revocation/account-deletion validation; remaining migration/release/security/performance audit; mission account/artifact cleanup
CURRENT_HYPOTHESES: Process-death handling in other in-memory activity state and backend write concurrency may expose further races; release shrinking/manifest may diverge from debug validation
NEXT_ACTIONS: Checkpoint coherent second wave; physically validate parent role, permission/service lifecycle and account deletion; continue untouched high-risk audit; run signed release and final full suites
BLOCKERS: None. Pre-existing user deletion of Dökümanlar/Yesiliz_Kapsam_ve_Sinirlar_v3.pdf is preserved and excluded from mission changes.

## Checkpoints

- 2026-08-11 00:09:10 +03 — Mission started on `dev` at `71a3ab1`; `dev` matched `origin/dev` after fetch.
- 2026-08-11 00:09:34 +03 — Created and switched to `fix/turan-debug` from `dev`; pre-existing PDF deletion preserved.
- 2026-08-11 00:10:50 +03 — Detected physical Samsung SM-A217F on Android 12/API 31 and an Android 16/API 36 emulator; app package is installed on the phone.
- 2026-08-11 00:12:40 +03 — Forced clean-path baseline passed: 30 core-risk and 170 app JVM tests; debug APK assembled.
- 2026-08-11 00:14:05 +03 — Physical phone cold-started the pre-installed build; daily worker completed successfully and focused crash/Room/Health Connect log scan was clean. Current APK could not replace it because developer signatures differ, so device data was preserved.
- 2026-08-11 00:17:20 +03 — API 36 emulator connected suite passed: 44 tests discovered, 40 passed, 4 usage-permission tests skipped by their documented precondition.
- 2026-08-11 00:23:30 +03 — Patched debug secret logging, refresh 5xx session loss, non-retryable account deletion, worker partial-day completion, and cancellation/fallback classification; focused tests green.
- 2026-08-11 00:25:55 +03 — Made each day's raw-event/summary/risk/points writes atomic and added an on-device rollback regression test.
- 2026-08-11 00:27:45 +03 — Found the implemented `yer_degis` risk signal was never wired and therefore always persisted as zero; wired the seven-day series and added a regression test.
- 2026-08-11 00:28:06 +03 — User-supplied password safely validated the configured release keystore. Installed physical app uses a different historical debug certificate, so existing data remains untouched; side-by-side mission package planned.
- 2026-08-11 00:31:35 +03 — Post-fix forced full suites passed: 30 core-risk + 181 app JVM tests, 82 backend pytest, and 41 API 36 connected tests; 4 permission-precondition tests skipped.
- 2026-08-11 00:33:44 +03 — Repaired the missing real invite-code acceptance flow; valid/invalid/self-code regression tests pass and debug examples no longer send names to the backend code endpoint.
- 2026-08-11 00:36:30 +03 — Created local commit `0de1757` for the first coherent critical-fix wave; pre-existing PDF deletion remained excluded.
- 2026-08-11 00:39:30 +03 — Installed `tr.org.yesilay.yesiliz.mission` beside the historically signed original app, preserving original user data. Completed guest onboarding through permission, skipped assessment, target, and main dashboard on physical API 31.
- 2026-08-11 00:43:30 +03 — Physical primary tabs (Ana Sayfa, İstatistik, Lig, Aktiviteler) rendered correctly; focused runtime log scan found no app fatal/Room/security exception.
- 2026-08-11 00:52:45 +03 — Physical Health Connect matrix passed: provider present; request contained only Exercise and Steps; denial, foreground grant/background denial, background grant, hardware-counter denial and later grant all returned to a truthful UI without crash. Hardware Step Counter registration was observed.
- 2026-08-11 00:58:44 +03 — Found and fixed reset-token logging/false production success, reward-linked account deletion failure, a completed-permission UI contradiction, and the UTC-vs-Istanbul server calendar drift. Focused Android tests and all 84 backend tests pass. Approximately 6h10m useful mission time remains.
- 2026-08-11 01:08:30 +03 — Replaced construction-time dates with an app-wide current-day flow that advances at local midnight and refreshes on resume; Istanbul-midnight and DST regressions pass. Full forced run reached 30 core-risk + 183 app JVM tests.
- 2026-08-11 01:17:30 +03 — Added stable tie-breakers to Room/backend lists and preserved custom server URL path prefixes. Room regression passed on API 31/API 36; backend league focus passed 17 tests.
- 2026-08-11 01:23:30 +03 — Physical guest registration reached the local API successfully and revealed that completed onboarding restarted because a parameterized Profile route failed an exact-string comparison.
- 2026-08-11 01:30:33 +03 — Replaced the fragile route heuristic with persisted startup-state navigation; both first-run and completed-guest paths pass focused JVM regressions. Approximately 5h39m mission time remains.
- 2026-08-11 01:58:00 +03 — Physically validated retained points, cold relaunch, logout/login, clean Profile→Ana back navigation, real league join/invite, and network loss/recovery. Fixed the newly exposed offline-empty league state so it no longer looks like lost membership or offers no-op actions. Approximately 5h11m mission time remains.
- 2026-08-11 02:31:48 +03 — Closed concurrent/revoked step sampling, external-route crash, atomic task limits/active-day proofs, remote parent unlink, fake parent identity/data, invalid offline pairing codes, ambiguous multi-pairing, dated parent bars, plaintext reusable server tokens, sticky walk resurrection, overlay revocation, and four minSdk/lint defects. Forced suites pass: 30 core-risk + 200 app JVM + 89 backend; API 36 connected 40 pass/4 documented skips; Android lint has zero errors. Approximately 4h37m mission time remains.
