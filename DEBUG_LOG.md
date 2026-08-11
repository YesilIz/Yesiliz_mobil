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

## DBG-009 — Password reset promised undelivered mail and logged its bearer secret

- Severity: P1 — account security / product truthfulness
- Areas: `backend/app/uclar/hesap.py`, `RetrofitHesapDeposu`, backend documentation
- Evidence: No SMTP/e-mail provider exists, yet production returned 202 and Android said a reset link was sent. The only usable password-reset token was written verbatim to provider-collected server logs.
- Expected: A reusable password-reset secret never enters logs; an unavailable delivery channel must not be reported as success.
- Actual: Log readers could take over an account using the token, while ordinary users waited for an e-mail that could not arrive.
- Status: Fixed. Tokens are never logged. Production returns 503 before generating one until a provider exists; Android maps that to a visible general service-unavailable message. Development/test contract remains testable through the database. Focused Android test and 19 account backend tests pass.

## DBG-010 — Completed health-permission screen still warned as if permission were denied

- Severity: P3 — permission-state UX correctness
- Area: `SaglikIzinEkran`, `SaglikIzinDurum`
- Evidence: On the physical API 31 phone, after granting Health Connect Steps/Exercise, background reading, and Android activity recognition, the success card and “İzin vermezsen…” warning appeared together.
- Expected: The denial consequence note is shown only while an available required permission remains missing.
- Actual: A fully granted state presented contradictory guidance.
- Status: Fixed and physically validated. Derived missing-permission state has two JVM regressions; the refreshed side-by-side package showed the completed state without the denial warning.

## DBG-011 — Accounts with a reward choice could not be deleted on PostgreSQL

- Severity: P1 — account deletion / privacy
- Area: `backend/app/uclar/hesap.py`
- Evidence: `OdulSecimi.kullanici_id` has a foreign key to `Kullanici`, but the explicit account-deletion transaction omitted `odul_secimi` while deleting every other child table. PostgreSQL therefore rejects deletion for any account that selected a reward.
- Expected: “Hesabımı kalıcı sil” removes every user-owned server row in foreign-key-safe order.
- Actual: The request fails after a legitimate reward selection; the account and server data remain.
- Status: Fixed. Reward selections are deleted before the user. The account-deletion regression now creates a reward choice and verifies both it and the account disappear; backend suite passes.

## DBG-012 — UTC server calendar disagreed with the Turkish phone for three hours nightly

- Severity: P1 — league and parental-summary correctness
- Areas: `backend/app/uclar/lig.py`, `backend/app/uclar/ebeveyn.py`, backend settings/time helper
- Evidence: At physical-device time `2026-08-11 00:55 +03`, the Docker API process reported `2026-08-10 21:55 +00` and `date.today()` returned 10 August. League week/group membership and the parental seven-day window used that process-local date.
- Expected: Product calendar rules share the configured Turkish business day; UTC remains appropriate only for instants/timestamps.
- Actual: Each local midnight–03:00 interval used yesterday; early Monday hours also used the previous weekly league.
- Status: Fixed. Calendar boundaries use validated `YESILIZ_SAAT_DILIMI` (default `Europe/Istanbul`) through one helper. A fixed UTC Sunday-night regression proves it becomes local Monday; the running UTC container now reports local 11 August and all 84 backend tests pass.

## DBG-013 — Long-running screens kept yesterday's date after local midnight

- Severity: P1 — daily data correctness
- Areas: `MainActivity`, daily dashboard/target/activity/statistics/risk/category/notification/reward ViewModels
- Evidence: Several singleton or navigation-scoped objects captured `LocalDate.now()` only when constructed. If the app remained alive across midnight, reads and writes continued against the previous date until Android recreated those objects.
- Expected: The active local calendar date changes at midnight without requiring a process restart; user actions always write to their actual day.
- Actual: A retained app could show yesterday and attribute new mood/task/activity data to it.
- Status: Fixed. One app-wide current-day source advances at the next local midnight and refreshes on resume; writes resolve the date at call time. JVM regressions cover an active collector crossing Istanbul midnight and Berlin's 23-hour DST day.

## DBG-014 — Equal-time rows produced unstable lists

- Severity: P2 — deterministic UI/data behavior
- Areas: Room list queries; backend friend and league-group ordering
- Evidence: Queries ordered only by non-unique timestamps, dates, scores, or sequence values. Rows with ties could change order across SQLite/PostgreSQL plans and cause Compose list movement or inconsistent selections.
- Expected: Every user-visible ordered result has a stable, unique tie-breaker.
- Actual: Equal-time notifications, usage, tasks, rewards, friends, and sessions could swap positions; a tied league group lookup was nondeterministic.
- Status: Fixed. DAO queries and backend selections now add deterministic secondary keys. New Room and backend regressions pass; updated connected suites pass on API 31 and API 36.

## DBG-015 — Custom server URLs silently discarded their path prefix

- Severity: P2 — self-hosted/server configuration
- Area: `AgModulu`
- Evidence: Runtime URL rewriting retained only scheme, host, and port from a configured URL. A legitimate base such as `https://example.org/api/v1/` therefore sent requests to `/auth/...` instead of `/api/v1/auth/...`.
- Expected: A configured base-path prefix is prepended while each request path/query remains intact.
- Actual: Reverse-proxy deployments mounted below a path could never be reached.
- Status: Fixed. URL composition preserves normalized base paths and request queries; focused security/network unit regression passes.

## DBG-016 — Upgrading a completed guest account restarted onboarding

- Severity: P1 — account conversion / data continuity
- Area: `YesilizGezinme`
- Evidence: On the physical API 31 phone, a completed guest successfully registered through the local API (HTTP 201), then landed on “Adım 1/4”. The route check compared `profil` with the parameterized destination `profil?acilacakSayfa=...`, so it could never recognize the profile flow.
- Expected: A completed guest keeps local history and returns to Profile after registering or signing in; only first-run account flows continue onboarding.
- Actual: The app appeared to restart setup immediately after successful conversion.
- Status: Fixed and physically validated. The graph now remembers its initial destination instead of resetting when the session changes; post-authentication navigation is based on persisted onboarding state and rebuilds a clean `Ana Sayfa → Profil` stack. Registration retained the guest's 38 points, a cold relaunch returned to Ana Sayfa, logout/login returned to Profil, and Back returned to Ana Sayfa rather than the stale login screen. Two focused navigation regressions pass.

## DBG-017 — Offline league failure was presented as a real empty league

- Severity: P2 — network recovery / truthful UI
- Area: `LigEkran`
- Evidence: On the physical API 31 phone, removing the API tunnel and reopening a server-backed league produced a connection failure and `cevrimdisi=true`, but the empty-state branch returned before rendering the offline notice. It instead said “Henüz kimseyle yarışmıyorsun” and offered “Lige katıl” even though this account was already in Fidan Ligi. In release that action is reduced to a silent no-op while offline.
- Expected: Failure to retrieve league data is distinguished from a genuinely empty league; unavailable network actions are not offered as if they can succeed.
- Actual: A temporary outage looked like lost league membership and invited a misleading repeat action.
- Status: Fixed and physically validated. The offline-empty state now says the league is temporarily unavailable and hides join/invite actions. Two focused regressions pass. Restoring the tunnel and reopening the tab returned the real Fidan Ligi and preserved the authenticated session.

## DBG-018 — Concurrent/revoked step-sensor reads could lose daily steps or crash work

- Severity: P1 — activity/risk data integrity
- Area: `AdimDeposu`
- Evidence: MainActivity, the nightly worker, and the walking service can sample the cumulative hardware counter concurrently. Each independently read the old Room row and then wrote a derived total, so the later write could overwrite a newer sample. Permission could also be revoked after the availability check but before listener registration, allowing `SecurityException` out of the repository.
- Expected: The sensor-read→state-read→write sequence is serialized; permission revocation is a no-write outcome and coroutine cancellation still propagates.
- Actual: Interleavings could undercount/temporarily regress steps; a normal permission race could fail a foreground/background caller.
- Status: Fixed. A repository mutex protects the whole state transition and the permission race is classified explicitly. Nine focused tests cover serialization, accumulated total, revocation, restoration, day/time-zone rollover, process-counter reset, idempotence, and cancellation.

## DBG-019 — An untrusted external route extra crashed MainActivity

- Severity: P1 — exported-component crash
- Area: `MainActivity`
- Evidence: On the physical phone, `am start ... --es hedef_rota not/a/route` produced `FATAL EXCEPTION main` / `IllegalArgumentException: Navigation destination ... cannot be found`. MainActivity is exported and forwarded an arbitrary string extra directly to Navigation.
- Expected: Only destinations intentionally exposed through immutable app PendingIntents are accepted; unknown external strings are ignored.
- Actual: Any app could repeatedly crash Yeşilİz by launching its exported Activity with a fabricated extra.
- Status: Fixed and physically validated. Only Nefes/Yürüyüş are accepted from the extra; Health Connect actions map independently to the permission screen. Three JVM regressions pass; the invalid physical launch no longer crashes and a valid `nefes` launch still opens the timer.

## DBG-020 — Daily task limits and material-reward active days were not trustworthy

- Severity: P1 — points/reward integrity
- Areas: `GorevTamamlamaDao`, `PuanGunlukDao`, `PuanDeposu`
- Evidence: The daily limit was a separate COUNT followed by INSERT. Widget, service, and screen calls could all observe room below the limit and then exceed it. Material reward eligibility counted any day with positive points, despite `spec-puan.md` requiring both a completed task and real usage for an active day.
- Expected: Limit admission is one atomic database operation; an active day has points, a task completion, and a real-usage summary.
- Actual: Concurrent completion could over-award points, and claim-only/empty days could advance 30/45-day material rewards.
- Status: Fixed. `INSERT ... SELECT ... WHERE COUNT < limit` runs inside one Room transaction; eligibility requires both related proofs. API 36 instrumentation verifies eight concurrent attempts create exactly two allowed completions and exercises the active-day proof matrix (12/12 database tests pass).

## DBG-021 — Removing a parent pairing only hid it on one phone

- Severity: P1 — child privacy / authorization revocation
- Areas: Android parent repository/ViewModel/screen; `DELETE /ebeveyn/eslesme`
- Evidence: The confirmation text promised to cut the two-device link, but `EbeveynViewModel.eslesmeyiKaldir()` only cleared a local DataStore flag. `EbeveynBaglanti` remained on the server, so the parent account retained authorized summary access.
- Expected: Either participant can revoke the server relationship; the local flag clears only after the server confirms it.
- Actual: The UI reported removal while remote access survived.
- Status: Fixed. The idempotent authenticated endpoint deletes parent- or child-side links. Android keeps the confirmation sheet open through progress/error and clears local state only on success. Backend regressions prove either side immediately loses summary access.

## DBG-022 — Parent panel fabricated a named child, age, history, and alerts

- Severity: P1 — privacy identity/truthfulness
- Areas: `EbeveynDeposu`, `EbeveynViewModel`, `EbeveynEkran`
- Evidence: On process reopen no summary refresh ran. A null/failed response silently became a hard-coded Elif, age 14, 22–28 July graph, risk band, and notifications. A phone paired in the child role displayed the same fake parent dashboard. The API has no age field, so even successful responses fabricated age 14.
- Expected: Parent-role screens fetch authenticated data on every entry; child-role screens explain sharing only; unavailable fields and network failures are not invented.
- Actual: The app attributed sensitive behavior and risk information to a person who might not exist.
- Status: Fixed. Reopen triggers a refresh, explicit loading/error/retry states replace mock fallback, child role has a separate privacy screen, and unknown age is omitted. A remote 404 also clears a stale local pairing.

## DBG-023 — Offline pairing displayed an unusable random code as server-valid

- Severity: P2 — pairing truthfulness
- Area: `EbeveynDeposu` and pairing wizard
- Evidence: `kodUret` generated a random local six-digit number after any network/authentication failure, while the screen unconditionally said it was valid for ten minutes. The other device could never verify that number because it did not exist on the server; the returned server/local flag was ignored.
- Expected: A code appears only after successful authenticated server creation; failures distinguish sign-in, server, and connectivity cases and can be retried.
- Actual: Users were asked to exchange a guaranteed-invalid code.
- Status: Fixed. Local code generation was removed; the wizard shows loading, a truthful classified error, and an explicit retry action.

## DBG-024 — A device could enter multiple ambiguous parent relationships

- Severity: P1 — summary authorization / identity integrity
- Area: backend parent model and pairing endpoint
- Evidence: The database constrained only the exact `(parent, child)` pair. The singular UI and summary endpoint nevertheless select one child, without deterministic identity semantics, while removal deleted every relationship. One account could therefore see an arbitrary child's panel or unexpectedly revoke several links.
- Expected: This one-device/one-panel product permits one active parent relationship per participant and rejects collisions, including races.
- Actual: The schema allowed states the API/UI could not represent truthfully.
- Status: Fixed. Endpoint checks both participant identities, returns 409 for an existing link, and new PostgreSQL unique constraints protect parent/child columns. Migration keeps the newest non-conflicting links. A regression proves a second child is rejected and the original child's summary remains selected.

## DBG-025 — Parent chart assigned missing dates to the wrong weekdays

- Severity: P2 — behavioral summary accuracy
- Areas: parent summary API/DTO/presentation mapping
- Evidence: The server returned only minute values for dates with rows. Android labeled list index zero as Monday regardless of the row's actual date. A sole Tuesday summary therefore appeared under Monday; gaps shifted every following bar.
- Expected: Every bar is paired with its actual calendar date and the displayed range derives from those dates.
- Actual: Sparse weekly data was visually misdated.
- Status: Fixed. The API returns an aligned `gunlukTarihler` list; Android derives Turkish weekday labels and the range from those ISO dates. Backend contract regression passes.

## DBG-026 — Database theft exposed reusable session and password-reset secrets

- Severity: P1 — account security
- Areas: backend token issuance/storage/rotation; Alembic migration
- Evidence: Opaque refresh tokens (30-day lifetime) and password-reset tokens were stored verbatim. Anyone able to read a database snapshot could replay them directly. Rotation also selected a token without a row lock, allowing two concurrent refresh requests to consume the same one.
- Expected: Only one-way token digests are stored; single-use consumption is serialized.
- Actual: Database contents were bearer credentials, and server concurrency weakened the one-use promise.
- Status: Fixed. SHA-256 digests are stored/looked up, existing live rows were migrated in place without invalidating client-held raw tokens, and consumption uses row locking before rotation/reset. All 20 account tests pass; the live database reports only 64-character digests.

## DBG-027 — Android could resurrect a killed walk as a new zero-time session

- Severity: P2 — foreground-service lifecycle/data correctness
- Area: `YuruyusServisi`
- Evidence: The in-progress walk exists only in an in-memory singleton, but the service returned `START_STICKY`. After process death Android may recreate it with a null Intent and a reset repository; the service then called `surdur()` and began a fresh walk the user did not start.
- Expected: A non-persisted session is not automatically resurrected.
- Actual: Restart semantics contradicted the storage model and could create false activity.
- Status: Fixed. Running and paused paths use `START_NOT_STICKY`; explicit user starts continue to work normally.

## DBG-028 — Revoking overlay permission during monitoring could crash the app process

- Severity: P1 — permission lifecycle
- Area: `NudgeOverlayService`
- Evidence: Permission was checked only by MainActivity before starting the sticky service. Revoking it later left the service alive; its next `WindowManager.addView` could throw `SecurityException`. A sticky recreation also started without rechecking permission.
- Expected: Service start and every overlay insertion tolerate permission revocation.
- Actual: A normal Settings change could become a fatal exception.
- Status: Fixed. Service creation and insertion both recheck; the check→add race catches only the expected security exception, stops cleanly, and does not record an overlay notification that was never displayed.

## DBG-029 — The declared API 26 minimum contained four runtime incompatibilities

- Severity: P1 — supported-device compatibility
- Areas: activity persistence, usage permission, reminder worker, theme resources
- Evidence: Android lint failed with four errors. `LocalDate.ofInstant` is unavailable before API 34 without core desugaring; `unsafeCheckOpNoThrow` is unavailable before API 29; `windowLightNavigationBar` was in an unqualified API 26 resource despite requiring API 27; notification permission could be revoked between check and notify.
- Expected: Every API used on the advertised minSdk 26 path is available or guarded.
- Actual: Specific activity/permission/UI paths could throw or misinflate on supported older devices.
- Status: Fixed. Compatible date conversion and AppOps fallback are used, the theme item moved to `values-v27`, notification send explicitly tolerates revocation, and backup/device-transfer exclusion rules now explicitly protect behavioral data and tokens. `lintDebug` passes with zero errors.

## DBG-030 — Parent summary and pairing success could remain hidden behind stale UI state

- Severity: P1 — major flow deadlock/truthfulness
- Areas: `EbeveynViewModel`, `EbeveynEkran`
- Evidence: A successfully constructed parent summary inherited `yukleniyor = true`, so the real 200 response could leave the screen on its spinner forever. Separately, setting `eslesmisMi = true` immediately dismissed the pairing sheet, making its success step unreachable.
- Expected: Every terminal summary result leaves loading, and a completed pairing visibly confirms success before the user dismisses it.
- Actual: Valid server outcomes could be invisible.
- Status: Fixed and physically validated. Terminal state construction explicitly clears loading, and the sheet remains open for the success step. Two pure UI-state regressions pass; the physical phone displayed the authenticated child summary with real dates.

## DBG-031 — Account deletion confirmation rejected a natural Turkish lowercase entry

- Severity: P2 — destructive-account flow accessibility
- Area: `ProfilEkran`
- Evidence: The dialog demanded exact uppercase `SİL`, which is brittle on mobile keyboards and rejected the semantically identical Turkish lowercase `sil`.
- Expected: Ignore surrounding whitespace and apply Turkish-aware case normalization while still requiring the complete confirmation word.
- Actual: A safe confirmation could be impossible or needlessly difficult depending on keyboard state.
- Status: Fixed and physically validated. Turkish-locale uppercase normalization accepts only the intended word; two regressions pass and lowercase `sil` completed deletion on the Samsung.

## DBG-032 — Backend accepted impossible summaries and raced unique business operations

- Severity: P1 — remote data and reward integrity
- Areas: backend schemas; summary, league, reward, parent and account endpoints
- Evidence: DTOs allowed negative/non-finite minutes, ratios outside 0..1, unbounded date ranges and implausible counters. Several read-before-write operations had database uniqueness but no row serialization, so concurrent requests could surface integrity errors or double-admit a one-time reward.
- Expected: Reject impossible/unbounded payloads at the API boundary and serialize per-account/group state transitions.
- Actual: Malformed clients could store corrupt analytics or amplify work; concurrent valid requests could return unstable results.
- Status: Fixed and live-database validated. Bounded Pydantic contracts, date-window limits, reward-option whitelists and consistently ordered row locks now guard critical transitions. All 96 backend tests pass. Sixteen-way PostgreSQL probes preserved one code, league, summary, heartbeat and reward; only the single reward winner returned 201. A second probe placed eight different simultaneous users into one capacity-appropriate group, admitted exactly one of seven pair-code consumers, and revoked all 12 original/rotated refresh tokens across a logout race. All temporary accounts were removed.

## DBG-033 — Switching accounts exposed the previous local user's behavioral history

- Severity: P0 — cross-account privacy boundary
- Areas: account repositories, DataStore, Room, UsageStats import and daily worker
- Evidence: Login/logout changed authentication tokens but left local usage, risk, points, categories and summaries intact. A different person signing in on the same phone could inherit the prior person's dashboard, while a same-day UsageStats rescan could re-import events from before the ownership change even after clearing Room.
- Expected: Preserve guest data only for intentional guest→account conversion; otherwise erase prior local identity data and never query before the exact ownership boundary.
- Actual: Sensitive on-device behavioral history crossed account identities.
- Status: Fixed and device validated. A persisted owner plus exact start instant drives an atomic boundary reset, category reseed and query lower bound. Upgrade migration recovers an existing signed-in/guest owner and treats logged-out legacy data conservatively as unknown. Three JVM and three Room/DataStore device regressions pass, including a query window wholly before the boundary returning empty on API 31/API 36.

## DBG-034 — Offline reward choices were never retried or restored on a new phone

- Severity: P1 — earned-reward durability
- Areas: `OdulDeposu`, reward DAO and backend reward validation
- Evidence: The app wrote a choice locally then attempted one POST. A transient failure was only logged; later synchronization never retried it, and server choices were not merged into an empty local database on a new device.
- Expected: The first irrevocable local choice survives offline operation, retries opportunistically, and an existing server choice restores local state.
- Actual: The documented K28 reward could disappear across devices or remain permanently absent remotely.
- Status: Fixed. Reconciliation first upserts authoritative remote selections and then retries locally pending choices. Two repository regressions cover both directions; backend now rejects reward/tier mismatches. The historical A7 limitation is closed.

## DBG-035 — Logout left reusable refresh sessions valid and protected auth routes could not self-refresh

- Severity: P1 — session security/authentication
- Areas: backend `/auth/cikis`, Android account repository, API interface and authenticator
- Evidence: Android logout only erased local tokens. Other refresh tokens for the account remained usable for 30 days. Meanwhile the authenticator excluded every path containing `auth/`, inadvertently treating protected `/auth/hesap` and `/auth/cikis` as public and refusing to refresh an expired access token.
- Expected: Logout revokes all current refresh rows and always clears local state; only login/register/reset/refresh endpoints bypass authentication recovery.
- Actual: Stolen/second-device refresh tokens survived logout, while legitimate protected account actions could fail on access-token expiry.
- Status: Fixed. The server revokes all active refresh rows under a user lock; Android calls it and clears local state in a non-cancellable `finally`. Exact normalized public-path matching preserves configured URL prefixes. Backend regression proves two refresh tokens both return 401 after logout; eight authenticator tests pass.

## DBG-036 — Debug demo league stopped falling back when the server returned 401

- Severity: P2 — debug/demo regression
- Area: `YedekliLigDeposu`
- Evidence: A fresh physical debug guest reached the development server but lacked credentials, receiving HTTP 401. Fallback was limited to transport exceptions, so the intentionally supported local demo league became inaccessible even though the equivalent offline path worked.
- Expected: Debug demo-code/join paths fall back for transport or unauthenticated demo use, but retain cancellation and real business-error semantics.
- Actual: A reachable development server was worse than an unreachable one for the demo flow.
- Status: Fixed. A narrowly scoped demo fallback accepts only I/O and 401, never coroutine cancellation or 4xx business conflicts. Five focused tests pass.

## DBG-037 — Onboarding could navigate away before its completion data was durable

- Severity: P1 — first-run/data persistence
- Areas: onboarding, assessment refresh and awareness-test ViewModels/navigation
- Evidence: The completion button launched Room/DataStore/risk writes in `viewModelScope`, then its caller immediately removed the onboarding graph. Clearing the ViewModel could cancel those writes midway. The sibling assessment flows also published success/result state before persistence returned.
- Expected: Required local writes finish successfully before the UI leaves the flow or exposes its terminal state; rapid taps start only one completion.
- Actual: A user could reach the dashboard with incomplete setup and see onboarding again after relaunch, or see a completed test whose result was not stored.
- Status: Fixed and live-UI validated. A shared persistence-before-progress primitive, per-flow active-job guard and completion callback enforce ordering. Tests prove navigation never precedes persistence and persistence failure does not advance. API 36 completed the real skipped-assessment onboarding path, reached the dashboard and retained it.

## DBG-038 — Rapid account-form taps could start duplicate remote operations

- Severity: P1 — account/session race
- Area: `HesapViewModel`
- Evidence: Login, registration and reset set `islemde` only inside a newly launched coroutine. Two taps in the same UI frame could both observe idle state and start two server requests; two login successes could rotate/store competing token pairs.
- Expected: A valid account action claims the in-flight state synchronously and atomically before launching work.
- Actual: Duplicate registration conflicts, multiple refresh sessions, stale terminal results or repeated navigation were possible.
- Status: Fixed. A compare-and-set claim admits exactly one action, including guest continuation. Focused regressions prove the first claim succeeds and subsequent simultaneous claims are rejected until terminal state.

## DBG-039 — Walking timer and notification could finish one session twice

- Severity: P1 — activity/points integrity
- Area: `YuruyusServisi`
- Evidence: Timer completion and the notification's “Bitir” command can arrive concurrently. Both entered `bitir`, launched persistence work and stopped the service independently; duplicate activity rows and last-writer-wins `puanVerildi` state were possible.
- Expected: Exactly one caller owns finalization for a service instance.
- Actual: A boundary-time user action could double-record one walk or overwrite its awarded-points result.
- Status: Fixed. An atomic one-shot guard precedes all finalization work. A 32-caller concurrency regression admits exactly one owner.

## DBG-040 — Every newly started focus session could immediately mark itself failed

- Severity: P1 — core activity correctness
- Area: `OdakEkran` lifecycle observer
- Evidence: Adding a lifecycle observer to an already resumed owner makes Android synchronize the new observer through `ON_RESUME`. The code interpreted any `ON_RESUME` as the user reopening the phone, even though the observer was intentionally attached only after Start.
- Expected: Observer-state synchronization is ignored; only a resume after a real pause/stop invalidates the focus session.
- Actual: A session could show “Seans bozuldu” immediately, making the focus reward unobtainable.
- Status: Fixed and live-UI validated. A small state tracker requires an actual departure before accepting a return. Three JVM regressions cover initial synchronization, repeated departure events and single-consumption. On API 36, Start remained on “Odaklanıyorsun”; Home and return then changed it to “Seans bozuldu” as intended.

## DBG-041 — Release shipped the full extended icon graph and four DEX files

- Severity: P2 — download/startup footprint
- Area: release build configuration
- Evidence: The signed non-minified APK measured 24,225,698 bytes and carried four DEX files; the app directly uses a bounded subset of Material extended icons. Release already declared the optimized ProGuard baseline but explicitly disabled minification.
- Expected: Production removes unreachable application/library code and unused resources while debug remains readable.
- Actual: Every install/download paid for a large unreachable graph; the oversized DEX set also increased class-loading and distribution overhead.
- Status: Fixed and exact-release validated. Release-only R8 plus resource shrinking produces an 8,594,218-byte, single-DEX signed APK, a 64.5% reduction. The live-HTTPS/forbidden-host build guard, v2 signature verification and lint-vital pass. API 36 installed this artifact, completed onboarding, exercised the main tabs and cold-restarted through Hilt, Room, DataStore and WorkManager without a focused shrinker/runtime failure.

## DBG-042 — Focus and walking timers lost time while Android was asleep

- Severity: P1 — core activity correctness
- Areas: focus ViewModel and walking foreground service
- Evidence: Both paths treated each resumed one-second coroutine delay as exactly one elapsed second. Android may defer those callbacks in device idle/deep sleep. Focus explicitly requires the screen to remain off, while walking is designed for a pocketed phone, so the affected state is their primary use case. A delayed focus callback also let `ON_RESUME` mark the session failed before recognizing that its deadline had already passed.
- Expected: Elapsed active time comes from a monotonic clock that includes device sleep; focus return after the deadline completes successfully, while return before it still invalidates the session. Walking pauses exclude only the paused segment.
- Actual: A 15/25-minute activity could take materially longer or become impossible under normal sleep behavior.
- Status: Fixed and forced-idle validated. Both paths reconstruct time from `SystemClock.elapsedRealtime`; walking retains a fresh monotonic base for each resumed segment and catches skipped sampling/notification boundaries. Pure tests cover sub-second, deep-sleep jump and defensive negative input. API 36 forced deep idle for 15 seconds; focus advanced from 24:57 to 24:21 and walking from 0:05 to 0:40, including command overhead rather than losing the sleep interval.

## DBG-043 — The breathing session allowed the display to sleep mid-exercise

- Severity: P2 — activity usability/timer continuity
- Area: `NefesEkran`
- Evidence: `EkranAcikTut` documents breathing as a consumer, but only the eye-break screen invoked it. The two-minute breathing session therefore relied on the user's screen-timeout setting and a delay-based main-thread timer.
- Expected: The visible guided animation keeps the display awake only while actively playing and releases the flag when paused, complete or disposed.
- Actual: Short display timeouts could blank the breathing guide before completion.
- Status: Fixed and device validated. The breathing screen now owns the existing scoped keep-screen-on helper while playing. API 36 window state showed `KEEP_SCREEN_ON`; pausing changed the control to “Oynat” and removed the flag.

## DBG-044 — Early walking finish falsely claimed a reward and blocked the next walk

- Severity: P1 — points truthfulness/core repeat flow
- Areas: walking service/session state/ViewModel/screen
- Evidence: Notification “Bitir” always set generic `tamamlandi=true` but never awarded points when early. The UI derived a partial-looking point value and unconditionally said it had been added. The result button only navigated away, leaving the singleton completed forever in the same process; reopening could not start a new walk.
- Expected: Session completion, target completion and actual daily point insertion are separate facts; early finish says no point, a repeated daily reward says already obtained, and dismissing the result resets the next session.
- Actual: The app could make a false reward claim and strand the walking feature until process restart.
- Status: Fixed and live-notification validated. The state model separates all three outcomes and result copy branches truthfully; the result button resets before navigating. Unit regressions cover early, completed-with-daily-limit and actually-awarded states. The API 36 notification action showed “puan yazılmadı”, stopped the service and the next entry opened at 0:00.

## DBG-045 — Walking crashed on Android 14+ when optional activity permission was denied

- Severity: P0 — deterministic core-flow crash
- Area: walking foreground-service manifest declaration
- Evidence: On API 36 with `ACTIVITY_RECOGNITION` deliberately absent, pressing Start threw `SecurityException` from `startForeground`: a `health` FGS requires one of the platform's health/activity runtime permissions. The app treats step permission as optional and hides the step count when absent, so the timer itself had no legitimate permission precondition. Android then restarted the crashing service and repeated the failure.
- Expected: The user-started 15-minute digital-wellbeing timer runs without optional step/Health Connect grants; absent permission removes only step data.
- Actual: The whole app process crashed on the supported modern Android path.
- Status: Fixed and API 36 validated. The timer now uses the declared `specialUse` subtype with an explicit Play-facing explanation; optional `ACTIVITY_RECOGNITION` remains solely for sensor data. With that permission still denied, the service started as a typed foreground service, counted, survived forced deep sleep, and stopped cleanly with no focused fatal/security log. Lint passes.

## Baseline validation

- Forced JVM/build run: 30 `core-risk` tests passed, 170 app JVM tests passed, debug APK assembled.
- API 36 emulator: connected suite passed (44 discovered; 40 passed; 4 permission-precondition skips).
- Physical Samsung API 31: installed build cold start and `GunlukOzetWorker` smoke passed; no focused fatal/Room/Health Connect exception found. Exact mission APK installation was refused by Android because its signing certificate differs; no uninstall, data clear, or destructive workaround was used.
- Physical Samsung API 31 exact source: a property-scoped `.mission` package was installed beside the original. Guest onboarding, all four primary tabs, profile, and the full available Health Connect/hardware-step permission matrix passed without an app exception; original package/data remained untouched.
- Updated connected suites: API 36 passed 42 tests with 4 documented permission-precondition skips; physical API 31 passed all 42 tests with no skip or failure.
- Physical live-backend flow: guest registration, retained local points, logout/login, league join, real invite-code acceptance, network loss, and network recovery all completed without a crash; the original installed package remained untouched.
- Latest forced suites: 30 `core-risk` and 200 app JVM tests passed; all 89 backend tests passed. API 36 connected suite reports 44 tests, 40 passed and 4 documented usage-permission skips. Android lint passes with zero errors.
- Current forced suites: 30 `core-risk` and 230 app JVM tests passed; all 96 backend tests passed. The latest physical API 31 and emulator API 36 suites each passed all 47 connected tests with no skip or failure; Android lint remains at zero errors. The shrunk signed release is 8,594,218 bytes/one DEX and passed an API 36 onboarding/tabs/cold-restart smoke.
