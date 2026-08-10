# YEŞİLİZ Debug Mission State

MISSION_START: 2026-08-11 00:09:10 +03 (Europe/Istanbul)
MISSION_START_EPOCH: 1786396150
TARGET_END: 2026-08-11 07:09:10 +03 (Europe/Istanbul)
TARGET_END_EPOCH: 1786421350
CURRENT_PHASE: Static critical-path audit and first regression fixes
LAST_CHECKPOINT: 2026-08-11 00:33:44 +03
CURRENT_BRANCH: fix/turan-debug
LAST_VALIDATED_COMMIT: 71a3ab133fdda2aa3f4953ff081e21a7a1747cc2 (build, JVM tests, and API 36 connected tests green)
CURRENT_DEVICE: Samsung SM-A217F (serial RF8NA115KCD), Android 12 / API 31, 720x1600 @280dpi; API 36 emulator also connected
BUILD_STATUS: PASS — baseline and post-fix :app:assembleDebug
UNIT_TEST_STATUS: PASS — 30 core-risk + 181 app JVM tests after fixes
DEVICE_TEST_STATUS: PASS — API 36 emulator post-fix suite: 45 discovered, 41 passed, 4 permission-dependent skipped. Physical API 31 pre-installed app cold start and worker smoke passed; exact current build will use a side-by-side mission package.
BACKEND_TEST_STATUS: PASS — 82 pytest in Docker, 4.11s
TOP_OPEN_BUGS: Continue searching; first eight confirmed defects are patched locally and passing focused/full tests
CURRENT_HYPOTHESES: Time-window, lifecycle, permission, and device-layout edge cases may still expose sibling failures; verify statically and on both device classes
NEXT_ACTIONS: Run full JVM/backend suites; complete navigation/UI/time-window audit; build temporary side-by-side physical-device package; exercise API 31/API 36 lifecycle and permission paths
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
