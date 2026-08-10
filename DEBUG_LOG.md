# YEŞİLİZ Debug Mission Log

This file records evidence-backed issues investigated during the seven-hour mission.

## Mission baseline

- Start: 2026-08-11 00:09:10 +03
- Base commit: `71a3ab133fdda2aa3f4953ff081e21a7a1747cc2`
- Working branch: `fix/turan-debug`
- Preserved pre-existing user change: deletion of `Dökümanlar/Yesiliz_Kapsam_ve_Sinirlar_v3.pdf`

## DBG-001 — Debug HTTP logger records credentials and tokens

- Severity: P1 — privacy/security
- Area: `app/src/main/kotlin/tr/org/yesilay/yesiliz/di/AgModulu.kt`
- Evidence: The debug OkHttp client enables `HttpLoggingInterceptor.Level.BODY` after the authorization interceptor. Login/register JSON bodies therefore contain plaintext passwords, response bodies contain access/refresh JWTs, and authenticated request headers contain bearer tokens in logcat.
- Expected: Development diagnostics must never persist passwords or reusable authentication secrets.
- Actual: Any process with log access can read them from a debug session.
- Status: Fixed. Debug logging is BASIC only; reusable headers are additionally redacted. `AgGuvenligiTest` passes.

## DBG-002 — Temporary refresh-server errors destroy the session

- Severity: P1 — authentication/data continuity
- Area: `app/src/main/kotlin/tr/org/yesilay/yesiliz/data/uzak/JetonYenileyici.kt`
- Evidence: `RetrofitJetonYenileyici` maps every `HttpException`, including 429 and 5xx, to `YenilemeSonuc.Reddedildi`. `JetonAuthenticator` treats that result as proof the refresh token is invalid and clears the local session.
- Expected: Only an explicit authentication rejection may clear credentials; temporary server/rate-limit failures must preserve them for retry.
- Actual: A backend outage can log out a valid user.
- Status: Fixed. Only 400/401/403/422 prove rejection; 408/429/5xx preserve the session. Classifier and authenticator tests pass.

## DBG-003 — Failed remote account deletion clears the only retrying session

- Severity: P1 — account lifecycle/privacy truthfulness
- Areas: `RetrofitHesapDeposu.hesabiSil`, `YedekliHesapDeposu.hesabiSil`, `VeriYonetimiDeposu.hesabiSil`, `ProfilViewModel.uyariyiKapat`
- Evidence: The remote repository calls `cikisYap()` even when `DELETE /auth/hesap` fails; both higher layers then clear local preferences, Room, and JWT. Dismissing the warning navigates to login. The server account remains while its authenticated deletion retry is made impossible without signing in again.
- Expected: A failed server deletion must retain the session/local state and present a retryable, truthful failure.
- Actual: Local evidence and credentials are erased before the requested remote deletion is known to have succeeded.
- Status: Fixed. Remote failure now preserves JWT, session, Room, and preferences; the sheet truthfully offers retry without navigation. Four deletion safety tests pass.

## DBG-004 — Daily catch-up can permanently skip a partially processed day

- Severity: P1 — silent analytics/risk data loss
- Area: `app/src/main/kotlin/tr/org/yesilay/yesiliz/arkaplan/GunlukOzetWorker.kt`
- Evidence: Catch-up completeness is inferred only from `gunluk_ozet`. If usage summary succeeds but risk or score writing fails, the worker retries; on the next run the historical date is skipped because the summary exists, leaving risk/score missing indefinitely.
- Expected: A historical date is complete only after summary, risk, and points outputs all exist, or every stage must be safely rerun.
- Actual: A mid-chain failure creates a persistent hole without a visible error.
- Status: Fixed. Completion is the intersection of summary/risk/points dates; a focused regression test passes.

## DBG-005 — Coroutine cancellation and business errors are converted into fallback success

- Severity: P1 — lifecycle correctness / false success
- Areas: `YedekliDepolar.agdanAl`, `YedekliHesapDeposu`, `GunlukOzetWorker`, `SenkronDeposu` and related broad `runCatching`/`catch (Exception)` sites
- Evidence: `CancellationException` is an `Exception`; the wrappers catch it and run local mutations, return `false`, or schedule retry. `agdanAl` also falls back for all Retrofit business errors despite the documented contract that only connectivity failures may fall back.
- Expected: Cancellation propagates immediately; only explicitly classified connectivity failures invoke offline behavior.
- Actual: Leaving a screen/stopping work can still produce local league/account effects, and server rejections can look successful.
- Status: Fixed at all suspending broad-catch sites found in fallback, account, parent, sync, Health Connect, reward, widget, and daily-worker paths. Offline fallback now accepts only `IOException`; cancellation and business errors propagate. Three fallback regression tests pass.

## DBG-006 — Reprocessing a day was not atomic

- Severity: P1 — silent historical data inconsistency
- Areas: `KullanimDeposu.gunuIsle`, `GunlukOzetWorker`
- Evidence: Reprocessing deleted old raw events, inserted new events, wrote the summary, risk, and points in independent database operations. Cancellation or a failure between them could combine data from different runs; because old completion rows might still exist, the next day's catch-up could consider the date complete.
- Expected: All persistent outputs for one date commit together or roll back together.
- Actual: Partial writes survived a failed chain.
- Status: Fixed. Each date's raw-event/summary/risk/points chain runs in a Room transaction. An API 36 instrumentation regression injects a mid-transaction failure and verifies the partial summary is rolled back (compiled; connected execution pending).

## DBG-007 — The `yer_degis` risk input was permanently zero

- Severity: P1 — systematic risk-score underestimation
- Areas: `KullanimDeposu`, `OruntuCikar`, `RiskDeposu`
- Evidence: `YerDegisHesapla` and its unit tests existed, but production references ended there. `OruntuCikar` deliberately emitted `yer_degis = 0.0`, `KullanimDeposu` persisted that zero, and `RiskDeposu` fed it into a coefficient of 0.4. No production call ever calculated the seven-day trend.
- Expected: The last seven calendar days' screen totals and step counts feed `YerDegisHesapla` before the daily summary/risk write.
- Actual: This behavior signal could never influence any user's score.
- Status: Fixed. Prior six daily summaries plus the current day now feed the signal; the persisted summary and intermediate risk product use it. Wiring regression test passes.

## DBG-008 — Real friend invite codes could not be accepted

- Severity: P1 — core league flow broken
- Areas: `ArkadasEkleEkran`, `ArkadasEkleViewModel`, `RetrofitLigDeposu`
- Evidence: The backend accepts `POST /arkadas/davet { kod }`, but the Android screen had no field or action for entering another person's code. Its only “Ekle” actions passed example display names to the `kod` JSON field, guaranteeing HTTP 404 whenever the backend was reachable.
- Expected: Users can enter a `YSZ-XXXX` code, receive format/self-code/server feedback, and see a truthful success state; debug-only example people never hit the real code endpoint.
- Actual: Sharing a code had no corresponding receiving action, while unrelated example actions called the endpoint with invalid payloads.
- Status: Fixed. Added code entry, normalization, format/self-code checks, visible progress/success/error states, and correct repository routing. Two focused input regression tests and the existing six domain code tests pass.

## Baseline validation

- Forced JVM/build run: 30 `core-risk` tests passed, 170 app JVM tests passed, debug APK assembled.
- API 36 emulator: connected suite passed (44 discovered; 40 passed; 4 permission-precondition skips).
- Physical Samsung API 31: installed build cold start and `GunlukOzetWorker` smoke passed; no focused fatal/Room/Health Connect exception found. Exact mission APK installation was refused by Android because its signing certificate differs; no uninstall, data clear, or destructive workaround was used.
