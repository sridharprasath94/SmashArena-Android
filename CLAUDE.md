# SmashArena

Android sports facility booking app. Package: `com.flash.smasharena` | Min SDK 24 | Target SDK 35

## Build
```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```
**Critical:** Do NOT add `alias(libs.plugins.kotlin.android)` to `app/build.gradle.kts` — AGP 9.x bundles Kotlin already.

## Stack
Kotlin · XML + View Binding (`dev.androidbroadcast.vbpd`) + Material 3 · MVVM + Clean Arch · Hilt · Nav Component + Safe Args · Coroutines + StateFlow · Firebase Auth + Firestore · Room (unused, keep)

## Navigation
login → home → (slots → payment | myBookings | membership → membershipPayment). Cross-fragment results via `savedStateHandle` (`booking_result`, `membership_updated`).

## Domain Logic

**Facilities:** `badminton_court_1`, `cricket_net_1` — hours 5–21; doc ID: `{facilityId}_{date}_{hour}`
Pricing: Badminton ₹300/₹200/₹300 | Cricket ₹400/₹300/₹400 (early/mid/late)

**Membership:** RALLY ₹1k (8+2 slots), SMASH ₹1.5k (12+3), ACE ₹2k (16+4) — 30-day expiry; upgrade pays diff only; downgrade blocked.

**Booking window** (`BookingWindowUtils.effectiveStatus()`):
- `>7 days` → LOCKED | `6–7` → MEMBER_HOLD | `0–5` → open | `<0` → LOCKED
- Max 2 consecutive slots/day/user

**Firestore:** `/slots/{facilityId}_{date}_{hour}` · `/users/{uid}` — missing slot doc = available.

## Key Patterns

**View Binding:** `private val binding by viewBinding(FooBinding::bind)` — no `_binding`, no `onDestroyView` null-out.

**Error flow:** Repository throws `AppError` → ViewModel `runCatching` → `toUserMessage(context)` → Snackbar. Never raw exceptions in UiState.

**Firebase Task bridge** (no `kotlinx-coroutines-play-services`):
```kotlin
suspendCancellableCoroutine { cont ->
    task.addOnSuccessListener { cont.resume(Unit) }
        .addOnFailureListener { cont.resumeWithException(it) }
}
```

**Dialogs:** Confirmations → `ConfirmationDialog` (BOOK/CANCEL/LOGOUT/INFO/CANCEL_MEMBERSHIP). Success → `BookingResultDialog`. Never `MaterialAlertDialogBuilder` or Snackbar for success.

**Payment:** Both payment VMs use `delay(1500L)` simulation before repository call.

## Constraints
- XML + VBPD only — no Compose
- `viewModelScope` only — no GlobalScope
- `java.util.Calendar` / `SimpleDateFormat` — no `java.time` (API 24 compat)
- Room: additive migrations only — no `fallbackToDestructiveMigration`
- Never commit `google-services.json`
- No `setupActionBarWithNavController` in `MainActivity` — each fragment calls `setSupportActionBar`
