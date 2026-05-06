# SmashArena

Android sports facility booking app. Package: `com.flash.smasharena` | Min SDK 24 | Target SDK 35

## Build

```bash
./gradlew :app:assembleDebug      # build
./gradlew :app:testDebugUnitTest  # unit tests
./gradlew :app:lintDebug          # lint
```
> If `gradlew` is missing: `gradle wrapper --gradle-version 8.13`

**Critical:** Do NOT add `alias(libs.plugins.kotlin.android)` to `app/build.gradle.kts` — AGP 9.x already bundles Kotlin, re-declaring causes build error.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | XML layouts + View Binding (`dev.androidbroadcast.vbpd`) + Material 3 |
| Architecture | MVVM + Clean Architecture (domain / data / presentation) |
| DI | Hilt |
| Navigation | Navigation Component + Safe Args |
| Async | Coroutines + StateFlow |
| Auth | Firebase Auth (email/password + Google Sign-In) |
| Remote DB | Firestore (real-time `callbackFlow` snapshot listeners) |
| Local DB | Room — entities exist but unused; keep for future use |

---

## Project Structure

```
app/src/main/java/com/flash/smasharena/
├── SmashArenaApplication.kt       — @HiltAndroidApp
├── MainActivity.kt                — setContentView only; fragments manage their own toolbar
├── data/
│   ├── repository/
│   │   ├── AuthRepositoryImpl.kt  — Firebase Auth; connectivity check before every call
│   │   ├── SlotRepositoryImpl.kt  — Firestore slots; callbackFlow + transaction
│   │   └── UserRepositoryImpl.kt  — user profile; purchase/upgrade/cancel membership
│   └── [Booking/Court/User]*.kt   — legacy Room files; unused, keep
├── di/                            — DatabaseModule, FirebaseModule, RepositoryModule
├── domain/
│   ├── model/                     — AppError, Facility, MembershipTier, Slot, SlotStatus, UserProfile
│   └── repository/                — AuthRepository, SlotRepository, UserRepository (interfaces)
├── presentation/
│   ├── login/                     — LoginFragment, LoginViewModel, LoginUiState
│   ├── home/                      — HomeFragment, HomeViewModel, HomeUiState
│   ├── slots/                     — SlotsFragment, SlotsViewModel, adapters, dialogs
│   ├── payment/                   — PaymentFragment/VM, MembershipPaymentFragment/VM
│   ├── membership/                — MembershipFragment, MembershipViewModel, PlanAdapter
│   └── mybookings/                — MyBookingsFragment, MyBookingsViewModel, BookingAdapter
└── util/
    ├── AppErrorExt.kt             — AppError.toUserMessage(context)
    ├── BookingWindowUtils.kt      — effectiveStatus(), isDateBrowsable()
    ├── DateTimeUtils.kt           — today(), dateWithOffset(), daysUntil(), etc.
    ├── ErrorMapper.kt             — Throwable.toAppError()
    └── NetworkMonitor.kt          — @Singleton; isConnected()
```

---

## Navigation

```
loginFragment (start)
  └─► homeFragment
        ├─► slotsFragment (facilityId, facilityName)
        │     └─► paymentFragment (facilityId, facilityName, date, dateLabel, hour, timeLabel, amount)
        ├─► myBookingsFragment
        ├─► membershipFragment
        │     └─► membershipPaymentFragment (tierName, amount, isUpgrade)
        └─► loginFragment (logout)
```

Cross-fragment results use `savedStateHandle` (keys: `booking_result`, `membership_updated`).

---

## Domain Logic

**Facilities:** `BADMINTON_COURT` (`badminton_court_1`), `CRICKET_NET` (`cricket_net_1`)
- Hours 5–21 (17 slots/day); Firestore doc ID: `{facilityId}_{date}_{hour}`
- Pricing: Badminton ₹300/₹200/₹300 | Cricket ₹400/₹300/₹400 (early/mid/late)

**Membership tiers:** RALLY ₹1k (8 badminton + 2 cricket), SMASH ₹1.5k (12+3), ACE ₹2k (16+4)
- 30-day expiry; upgrade pays price difference only
- Members get free bookings up to quota; `sessionsUsed` incremented via `FieldValue.increment(1)`

**Booking window:**
```
daysUntil > 7  → LOCKED (everyone)
daysUntil 6–7  → MEMBER_HOLD (members=AVAILABLE, non-members=LOCKED)
daysUntil 0–5  → open for everyone
daysUntil < 0  → LOCKED (past)
```
Implemented in `BookingWindowUtils.effectiveStatus()`. Max 2 consecutive slots/day/user.

**Firestore data model:**
```
/slots/{facilityId}_{date}_{hour}  — facilityId, date, hour, status, bookedBy?, bookedAt?
/users/{uid}                       — displayName, email, membershipTier, membershipExpiry, sessionsUsed, sessionsQuota
```
Missing slot doc = available (slots generated client-side, overlaid with Firestore data).

---

## Key Patterns

**View Binding** — `private val binding by viewBinding(FooBinding::bind)` — no `_binding`, no `onDestroyView` null-out.

**UI state collection:**
```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> /* update UI */ }
    }
}
```

**Error flow:** Repository throws `AppError` → ViewModel catches via `runCatching` → Fragment maps via `toUserMessage(context)` → Snackbar. Never raw exception messages in UiState.

**Firebase Task bridge** (no `kotlinx-coroutines-play-services`):
```kotlin
suspendCancellableCoroutine { cont ->
    task.addOnSuccessListener { cont.resume(Unit) }
        .addOnFailureListener { cont.resumeWithException(it) }
}
```

**Dialogs:**
- All confirmations → `ConfirmationDialog` (types: BOOK, CANCEL, LOGOUT, INFO, CANCEL_MEMBERSHIP). Never `MaterialAlertDialogBuilder`.
- Booking/cancellation success → `BookingResultDialog`. Never Snackbar for success.

**Payment:** Both `PaymentViewModel` and `MembershipPaymentViewModel` use `delay(1500L)` dummy simulation before the repository call.

---

## Constraints

- XML + VBPD only — no Jetpack Compose
- `viewModelScope` only — no `GlobalScope`
- `java.util.Calendar` / `SimpleDateFormat` — no `java.time` (API 24 compat)
- Room: additive migrations only — no `fallbackToDestructiveMigration`
- `google-services.json` is gitignored — never commit it
- No `setupActionBarWithNavController` in `MainActivity` — each fragment calls `setSupportActionBar` itself
- Membership downgrade blocked — `MembershipViewModel.onCardTapped()` ignores lower-tier taps
