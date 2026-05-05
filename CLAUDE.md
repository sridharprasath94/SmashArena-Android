# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# SmashArena – Claude Working Brief

Android sports facility booking app. Users sign in, browse a badminton court and cricket net, check real-time slot availability on a 14-day calendar, and make hourly bookings. Members get early access and free sessions based on their plan tier.

**Package:** `com.flash.smasharena`
**Min SDK:** 24 | **Target SDK:** 35

---

## Build & Test Commands

The project has no `gradlew` wrapper committed. Generate it first if missing:
```
gradle wrapper --gradle-version 8.13
```

| Task | Command |
|---|---|
| Build debug APK | `./gradlew :app:assembleDebug` |
| Run unit tests | `./gradlew :app:testDebugUnitTest` |
| Run a single test class | `./gradlew :app:testDebugUnitTest --tests "com.smasharena.data.BookingRepositoryTest"` |
| Lint | `./gradlew :app:lintDebug` |

Alternatively, use **Build → Make Project** / **Run** in Android Studio (Meerkat or newer).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | XML layouts, View Binding via VBPD, Material 3 |
| Architecture | MVVM + Clean Architecture (domain / data / presentation) |
| DI | Hilt |
| Local DB | Room (entities exist but not used in current features — kept for future use) |
| Navigation | Navigation Component + Safe Args |
| Async | Coroutines + StateFlow |
| Auth | Firebase Authentication (email/password + Google Sign-In) |
| Remote DB | Firebase Firestore (real-time `callbackFlow` snapshot listeners) |
| View Binding | `dev.androidbroadcast.vbpd` — all fragments use `by viewBinding(XxxBinding::bind)` |

---

## Gradle Versions (libs.versions.toml)

| Dependency | Version |
|---|---|
| AGP | 9.0.1 |
| Kotlin | 2.3.10 |
| KSP | 2.3.6 |
| Hilt | 2.59.2 |
| Room | 2.8.4 |
| Navigation | 2.9.7 |
| Firebase BOM | 33.8.0 |
| Google Services plugin | 4.4.2 |
| VBPD | 2.0.4 |
| Material | 1.13.0 |

**Critical:** Do NOT add `alias(libs.plugins.kotlin.android)` to `app/build.gradle.kts`. AGP 9.x already bundles Kotlin on the classpath — re-declaring it causes a build error ("plugin already on the classpath with an unknown version").

---

## Facilities & Slot Pricing

| Enum | ID | Display Name |
|---|---|---|
| `BADMINTON_COURT` | `badminton_court_1` | Badminton Court |
| `CRICKET_NET` | `cricket_net_1` | Cricket Net |

- Operating hours: **05:00 – 22:00** daily (hours 5–21 = 17 slots)
- Each slot is exactly **1 hour**
- Firestore doc ID: `{facilityId}_{date}_{hour}` e.g. `badminton_court_1_2026-05-10_8`

**Slot pricing** (`Facility.priceForHour(hour: Int): Int`):

| Facility | Hours 5–9 | Hours 10–15 | Hours 16–21 |
|---|---|---|---|
| Badminton Court | ₹300 | ₹200 | ₹300 |
| Cricket Net | ₹400 | ₹300 | ₹400 |

---

## Membership Plans

| Enum | Plan | Price | Badminton sessions/month | Cricket sessions/month |
|---|---|---|---|---|
| `RALLY` | Rally | ₹1,000 | 8 | 2 |
| `SMASH` | Smash | ₹1,500 | 12 | 3 |
| `ACE` | Ace | ₹2,000 | 16 | 4 |

- Monthly membership (30-day expiry), stored in Firestore under `/users/{uid}`
- All tiers get the same **7-day early booking window** (days 7–6 before the slot)
- Non-members can book during days 5–0 only
- Members get free slot bookings up to their `sessionsQuota`; payment required only when `sessionsUsed >= sessionsQuota`
- Upgrade path: pay only the price difference to move to a higher tier
- Cancellation: resets `membershipTier` to NONE and clears quota/expiry

---

## Booking Window Rules

```
daysUntil > 7   → LOCKED for everyone (too far ahead)
daysUntil 6–7   → MEMBER_HOLD: members see AVAILABLE, non-members see LOCKED
daysUntil 0–5   → Open for everyone — use Firestore status as-is
daysUntil < 0   → LOCKED (past)
```

Implemented in `BookingWindowUtils.effectiveStatus()` (pure function, easy to unit test).
Also enforced in Firestore security rules (server-side source of truth).

---

## Consecutive Booking Limit

Max **2 back-to-back slots** per account per day. Enforced client-side in `SlotsViewModel.wouldExceedConsecutiveLimit()` — checks if adding the selected hour would create a run of 3+ consecutive MY_BOOKING hours. Shows `ConfirmationDialog.limitReached()` (INFO type, no fragment result) when breached.

---

## Firestore Data Model

```
/slots/{facilityId}_{date}_{hour}
  facilityId  : string    — "badminton_court_1" | "cricket_net_1"
  date        : string    — "YYYY-MM-DD"
  hour        : int       — 5..21
  status      : string    — "available" | "member_hold" | "booked"
  bookedBy    : string?   — Firebase Auth UID (null if unbooked)
  bookedAt    : timestamp?

/users/{uid}
  displayName       : string
  email             : string
  membershipTier    : string   — "NONE" | "RALLY" | "SMASH" | "ACE"
  membershipExpiry  : long?    — epoch millis (30 days from purchase)
  sessionsUsed      : int      — incremented by FieldValue.increment(1) on each free booking
  sessionsQuota     : int      — 0 | 8 | 12 | 16 (set by tier on purchase/upgrade)
```

Slot documents are only written when a booking happens. `observeSlots` generates all 17 slots client-side and overlays Firestore data on top — so a missing document = available.

---

## Firestore Security Rules (current)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
    match /slots/{slotId} {
      allow read: if request.auth != null;
      allow create, update: if request.auth != null
                            && request.resource.data.bookedBy == request.auth.uid;
    }
  }
}
```

---

## Project Structure

```
app/src/main/java/com/flash/smasharena/
├── SmashArenaApplication.kt          — @HiltAndroidApp
├── MainActivity.kt                   — AppCompatActivity, setContentView only
├── data/
│   ├── Booking.kt, BookingDao.kt, BookingRepository.kt   — legacy Room files (unused, keep)
│   ├── Court.kt, CourtDao.kt                             — legacy Room files (unused, keep)
│   ├── User.kt, UserDao.kt                               — legacy Room files (unused, keep)
│   ├── SmashArenaDatabase.kt                             — Room DB (entities: User, Court, Booking)
│   └── repository/
│       ├── AuthRepositoryImpl.kt       — Firebase Auth (email + Google); checks connectivity before each call
│       ├── SlotRepositoryImpl.kt       — Firestore slots (callbackFlow + transaction); checks connectivity before writes
│       └── UserRepositoryImpl.kt       — Firestore user profile; get-or-create, purchase/upgrade/cancel membership, incrementSessionsUsed
├── di/
│   ├── DatabaseModule.kt             — Provides Room DB + DAOs
│   ├── FirebaseModule.kt             — Provides FirebaseAuth, FirebaseFirestore, GoogleSignInClient
│   └── RepositoryModule.kt           — @Binds Auth/User/SlotRepository
├── domain/
│   ├── model/
│   │   ├── AppError.kt               — Sealed class: NoInternet, NotSignedIn, SlotAlreadyBooked, auth errors, etc.
│   │   ├── Facility.kt               — Enum: BADMINTON_COURT, CRICKET_NET (id, nameRes, imageRes, priceForHour())
│   │   ├── MembershipTier.kt         — Enum: NONE, RALLY, SMASH, ACE (price, quota, cricketQuota)
│   │   ├── Slot.kt                   — Data class (docId and timeLabel as computed properties)
│   │   ├── SlotStatus.kt             — Enum: AVAILABLE, MEMBER_HOLD, BOOKED, LOCKED, MY_BOOKING
│   │   └── UserProfile.kt            — Data class (isMember, sessionsRemaining as computed)
│   └── repository/
│       ├── AuthRepository.kt
│       ├── SlotRepository.kt         — observeSlots(), getMyBookings(), bookSlot(), cancelBooking()
│       └── UserRepository.kt         — getOrCreateProfile(), purchaseMembership(), upgradeMembership(), cancelMembership(), incrementSessionsUsed()
├── presentation/
│   ├── login/
│   │   ├── LoginFragment.kt
│   │   ├── LoginViewModel.kt
│   │   └── LoginUiState.kt           — generalError: AppError?
│   ├── home/
│   │   ├── HomeFragment.kt           — observes membership_updated from savedStateHandle → refreshProfile()
│   │   ├── HomeViewModel.kt          — loadProfile() on init, refreshProfile() public method
│   │   └── HomeUiState.kt
│   ├── slots/
│   │   ├── SlotsFragment.kt
│   │   ├── SlotsViewModel.kt         — facilityId is public (val); wouldExceedConsecutiveLimit(), isFreeSession(), bookSelectedSlotFree()
│   │   ├── SlotsUiState.kt           — DateItem, DisplaySlot, BookingResultInfo, ResultType, SlotsUiState (isCancelling, isBookingFree)
│   │   ├── DateAdapter.kt            — Horizontal date strip (ListAdapter)
│   │   ├── SlotAdapter.kt            — 3-column slot grid (ListAdapter)
│   │   ├── BookingResultDialog.kt    — Animated result dialog (booking confirmed / cancelled)
│   │   └── ConfirmationDialog.kt     — Animated confirmation dialog; types: BOOK, CANCEL, LOGOUT, INFO, CANCEL_MEMBERSHIP
│   ├── payment/
│   │   ├── PaymentFragment.kt        — Slot payment; pops back to SlotsFragment via savedStateHandle["booking_result"]
│   │   ├── PaymentViewModel.kt       — Reads facilityId/date/hour from SavedStateHandle; calls slotRepository.bookSlot()
│   │   ├── PaymentUiState.kt         — PaymentMethod enum + PaymentUiState
│   │   ├── MembershipPaymentFragment.kt  — Membership payment; pops to homeFragment with savedStateHandle["membership_updated"]
│   │   └── MembershipPaymentViewModel.kt — Reads tierName/isUpgrade from SavedStateHandle; calls userRepository.purchase/upgradeMembership()
│   ├── membership/
│   │   ├── MembershipFragment.kt     — Plan comparison + payment navigation + cancel membership
│   │   ├── MembershipViewModel.kt    — onCardTapped(), onGetStarted(item), cancelMembership(); emits navigateToPayment event
│   │   ├── MembershipUiState.kt      — PlanItem (upgradePrice?), NavigateToMembershipPayment, MembershipUiState
│   │   └── PlanAdapter.kt            — Shows "Get Started", "Upgrade · ₹X", or "Current Plan" button
│   └── mybookings/
│       ├── MyBookingsFragment.kt
│       ├── MyBookingsViewModel.kt
│       ├── MyBookingsUiState.kt      — BookingItem, MyBookingsUiState (error: AppError?)
│       └── BookingAdapter.kt
└── util/
    ├── AppErrorExt.kt                — AppError.toUserMessage(context): String
    ├── BookingWindowUtils.kt         — effectiveStatus(), isDateBrowsable()
    ├── DateTimeUtils.kt              — today(), dateWithOffset(), displayDate(), daysUntil(), etc.
    ├── ErrorMapper.kt                — Throwable.toAppError(): AppError (maps Firebase exceptions by class)
    ├── NetworkMonitor.kt             — @Singleton; isConnected() via ConnectivityManager + NET_CAPABILITY_VALIDATED
    └── TimeFormat.kt
```

---

## Navigation Graph

```
loginFragment (start)
  └─► homeFragment  (popUpTo loginFragment inclusive)
        ├─► slotsFragment  (args: facilityId, facilityName)
        │     └─► paymentFragment  (args: facilityId, facilityName, date, dateLabel, hour, timeLabel, amount)
        ├─► myBookingsFragment
        ├─► membershipFragment
        │     └─► membershipPaymentFragment  (args: tierName, amount, isUpgrade)
        └─► loginFragment  (logout — popUpTo homeFragment inclusive)
```

---

## Completed Screens

### Login (`LoginFragment`)
- Email + password fields (TextInputLayout outlined)
- Sign in / Create account / Sign in with Google buttons
- `ActivityResultLauncher` for Google Sign-In intent
- Validation errors on TextInputLayout fields; Firebase auth errors as Snackbar
- `generalError: AppError?` in UiState — mapped via `AppError.toUserMessage()`

### Home (`HomeFragment`)
- Two facility cards (Badminton Court, Cricket Net) — tap → SlotsFragment
- Membership banner: shows tier + sessions remaining or "No membership · View plans →"
- Toolbar menu: **Cloud Synced** (shows last sync time) | **My Bookings** | **Log out**
- Observes `currentBackStackEntry.savedStateHandle["membership_updated"]` — calls `viewModel.refreshProfile()` when returning from Membership payment or cancellation
- Log out uses `ConfirmationDialog` (LOGOUT type)

### Slots (`SlotsFragment`)
- 14-day horizontal date strip; non-browsable dates are dimmed
- 3-column slot grid colored by status (green=available, grey=booked, amber=member-hold, dark=locked, blue=mine)
- Tapping an available slot → "Book HH:00" button (green); tapping MY_BOOKING → "Cancel HH:00" button (rose)
- Book button logic (in order):
  1. MY_BOOKING → show cancel confirmation
  2. Would exceed 2 consecutive → show `ConfirmationDialog.limitReached()` (INFO type)
  3. Member with free sessions remaining → show `ConfirmationDialog.bookFree()` → `bookSelectedSlotFree()`
  4. Otherwise → navigate to `PaymentFragment`
- `PaymentFragment` returns result via `savedStateHandle["booking_result"]` bundle → `BookingResultDialog` shown
- `bookSelectedSlotFree()`: calls `bookSlot()` + `incrementSessionsUsed()` (best-effort) + refreshes userProfile
- Real-time Firestore updates; `slotObserverJob` restarted on date change

### Payment (`PaymentFragment`)
- Reuses `fragment_payment.xml` layout
- Order summary card: facility name, date · time, total amount (green)
- Payment method radio group: UPI / Net Banking / Debit · Credit Card
- 1.5s simulated delay then `slotRepository.bookSlot()`
- On success: writes `booking_result` bundle to `previousBackStackEntry.savedStateHandle` → `popBackStack()`

### My Bookings (`MyBookingsFragment`)
- Lists all upcoming bookings for the current user (today and later)
- Real-time Firestore listener (`callbackFlow` with `whereEqualTo("bookedBy", uid)`)
- Client-side filtered and sorted; no composite Firestore index needed
- Cancel confirmation uses `ConfirmationDialog`; docId travels in result Bundle

### Membership (`MembershipFragment`)
- Plan comparison with `PlanAdapter` (RALLY / SMASH / ACE cards)
- Tapping a higher-tier card selects it; tapping current plan or a lower tier does nothing
- Selected card shows "Get Started" (new purchase) or "Upgrade · ₹X" (upgrade — difference only)
- "Current Plan" button on the active plan card (disabled)
- "Cancel Membership" rose text button at bottom — visible only when user has an active plan
- Cancel uses `ConfirmationDialog.cancelMembership()` (CANCEL_MEMBERSHIP type)
- Navigates to `MembershipPaymentFragment` on Get Started / Upgrade

### Membership Payment (`MembershipPaymentFragment`)
- Reuses `fragment_payment.xml`; toolbar title = "{Tier} Plan"
- Order summary: plan name, "Monthly membership · N sessions/month" or "Upgrade · N sessions/month"
- 1.5s simulated delay then `userRepository.purchaseMembership()` or `upgradeMembership()`
- On success: writes `membership_updated = true` to `homeFragment`'s savedStateHandle → `popBackStack(R.id.homeFragment, false)`

---

## Key Patterns

### Firebase Task → Coroutine bridge
```kotlin
suspendCancellableCoroutine { cont ->
    task
        .addOnSuccessListener { cont.resume(Unit) }
        .addOnFailureListener { cont.resumeWithException(it) }
}
```
No `kotlinx-coroutines-play-services` dependency needed.

### Connectivity check (repositories)
```kotlin
if (!networkMonitor.isConnected()) throw AppError.NoInternet
```
Called at the start of every write operation (`bookSlot`, `cancelBooking`, all auth calls, all membership writes).

### Error flow
```
Repository            → throws AppError (via toAppError() or directly)
ViewModel             → runCatching { }.onFailure { e -> _uiState.update { it.copy(error = e.toAppError()) } }
Fragment              → error.toUserMessage(requireContext()) → Snackbar
```
`AppError` is a sealed class in `domain/model/`. `ErrorMapper.toAppError()` matches Firebase exception classes first; `is AppError -> this` pass-through handles directly-thrown AppErrors.

### Cross-fragment result via savedStateHandle
Used for passing results back across the nav back stack:
```kotlin
// Writer (child fragment) — write before popping
findNavController().previousBackStackEntry?.savedStateHandle?.set("key", value)
findNavController().popBackStack()

// Writer (grandchild, popping multiple levels)
findNavController().getBackStackEntry(R.id.homeFragment).savedStateHandle["key"] = value
findNavController().popBackStack(R.id.homeFragment, false)

// Reader (parent fragment) — in onViewCreated
findNavController().currentBackStackEntry?.savedStateHandle
    ?.getLiveData<T>("key")
    ?.observe(viewLifecycleOwner) { value -> /* handle */ }
```

### Slot observation (callbackFlow)
```kotlin
val listener = query.addSnapshotListener { snapshot, error ->
    if (error != null) { close(error.toAppError()); return@addSnapshotListener }
    trySend(/* mapped list */)
}
awaitClose { listener.remove() }
```

### Booking (Firestore transaction)
```kotlin
firestore.runTransaction { tx ->
    val snap = tx.get(docRef)
    if (snap.exists() && snap.getString("status") == "booked") throw AppError.SlotAlreadyBooked
    tx.set(docRef, mapOf(...))
}
```

### ConfirmationDialog (all confirmation flows)
All confirmation dialogs use `ConfirmationDialog` — never `MaterialAlertDialogBuilder`.

Types: `BOOK`, `CANCEL`, `LOGOUT`, `INFO` (single-button, no result), `CANCEL_MEMBERSHIP`

Factory methods and their request keys:
| Factory | Request key | Type |
|---|---|---|
| `book(facility, date, time)` | `REQUEST_BOOK` | BOOK |
| `bookFree(facility, date, time)` | `REQUEST_BOOK_FREE` | BOOK |
| `cancelSlot(facility, date, time)` | `REQUEST_CANCEL_SLOT` | CANCEL |
| `cancelBooking(facility, date, time, docId)` | `REQUEST_CANCEL_BOOKING` | CANCEL |
| `logout(message)` | `REQUEST_LOGOUT` | LOGOUT |
| `limitReached(message)` | — (no result) | INFO |
| `cancelMembership(tierName)` | `REQUEST_CANCEL_MEMBERSHIP` | CANCEL_MEMBERSHIP |

The docId for My Bookings cancellation travels inside the result `Bundle` via `KEY_DOC_ID`.

### BookingResultDialog (success feedback)
Shown after successful booking or cancellation — never Snackbar for success.
```kotlin
state.bookingResultInfo?.let { info ->
    viewModel.onResultShown()
    BookingResultDialog.newInstance(info).show(childFragmentManager, "booking_result")
}
```

### Animated dialog style (both dialog types)
Both `ConfirmationDialog` and `BookingResultDialog` use:
- `setStyle(STYLE_NO_TITLE, R.style.Theme_SmashArena_ResultDialog)` — transparent window, `dialog_enter`/`dialog_exit` animations
- `dialog?.window?.setLayout((widthPixels * 0.88).toInt(), WRAP_CONTENT)` in `onStart()`
- Icon circle animated with `OvershootInterpolator(2.8f)` scaled 0→1 after 80ms delay

### View Binding (all fragments including DialogFragments)
```kotlin
class FooFragment : Fragment(R.layout.fragment_foo) {
    private val binding by viewBinding(FooBinding::bind)
    // No _binding, no onDestroyView null-out
}
```

### UI state collection
```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> /* update UI */ }
    }
}
```

### Dummy payment pattern
Both `PaymentViewModel` and `MembershipPaymentViewModel` use a 1.5s `delay()` to simulate a real payment gateway, then call the relevant repository method. Replace the `delay()` + repository call block when a real gateway is integrated.

---

## Theme & Colors

| Token | Hex | Usage |
|---|---|---|
| `background` | `#1A1A1A` | Screen backgrounds |
| `surface` | `#242424` | Cards, dialog backgrounds |
| `accent_green` | `#B5D86C` | Buttons, selected state, booking time text |
| `text_primary` | `#FFFFFF` | Primary text |
| `text_secondary` | `#B0B0B0` | Labels, secondary info |
| `outline` | `#3A3A3A` | Card borders |
| `cancel_action` | `#CF6679` | Cancel/logout button background, destructive actions |
| `slot_available` | `#1E3A1E` | Available slot card |
| `slot_selected` | `#2D5A2D` | Selected available slot card |
| `slot_booked` | `#2E2E2E` | Booked slot card |
| `slot_member_hold` | `#3E2800` | Member-only slot card |
| `slot_locked` | `#1E1E1E` | Locked/unavailable slot card |
| `slot_my_booking` | `#0D2744` | User's own booking card |
| `slot_my_booking_selected` | `#1A4A80` | User's own booking card (selected) |
| `icon_bg_success` | `#33B5D86C` | Icon circle background for success/book dialogs |
| `icon_bg_cancel` | `#33CF6679` | Icon circle background for cancel/logout dialogs |

---

## What NOT to do

- Do NOT use Jetpack Compose — XML + VBPD only
- Do NOT use `_binding` + `onDestroyView` null pattern — use `by viewBinding(...)`
- Do NOT add `alias(libs.plugins.kotlin.android)` to `app/build.gradle.kts` — causes build error
- Do NOT use `GlobalScope` — use `viewModelScope`
- Do NOT use `kotlinx-coroutines-play-services` — bridge Firebase Tasks manually with `suspendCancellableCoroutine`
- Do NOT add `setupActionBarWithNavController` in `MainActivity` — each fragment calls `setSupportActionBar` itself
- Do NOT use `fallbackToDestructiveMigration` in Room — additive migrations only
- Do NOT implement a real payment gateway — all payment flows use a 1.5s dummy delay
- Do NOT use `java.time` APIs — use `java.util.Calendar` / `SimpleDateFormat` for API 24 compatibility
- Do NOT commit `google-services.json` — it is gitignored
- Do NOT use `MaterialAlertDialogBuilder` for confirmation dialogs — use `ConfirmationDialog` instead
- Do NOT show Snackbar for booking/cancellation/membership success — use `BookingResultDialog` or navigate back
- Do NOT put raw exception messages in UiState — map through `Throwable.toAppError()` first
- Do NOT put user-facing strings in ViewModels — map `AppError` to strings in the Fragment via `toUserMessage(context)`
- Do NOT allow card selection for lower-tier plans when user has an active membership — downgrade is blocked in `MembershipViewModel.onCardTapped()`

---

## Pending Features

| # | Feature | Notes |
|---|---|---|
| 1 | Cricket net membership plans | Separate from badminton. Plans/pricing TBD. |
| 2 | Replace placeholder facility images | `img_badminton_court.xml` and `img_cricket_net.xml` are vector placeholders. |
| 3 | My Bookings — past bookings tab | Currently only shows upcoming (date >= today). |
| 4 | Real payment gateway | Replace `delay(1500L)` in `PaymentViewModel` and `MembershipPaymentViewModel` with UPI/gateway SDK calls. |
| 5 | Membership expiry enforcement | `membershipExpiry` is stored but not checked client-side or in Firestore rules. |

---

## Firebase Setup

- Auth providers: Email/Password, Google Sign-In
- Firestore collections: `slots`, `users`
- `google-services.json` placed by the user — NOT in git
- SHA-1 fingerprint added to Firebase project by user
- `default_web_client_id` is read from `google-services.json` at build time by the Google Services plugin

---

## How to Start a New Session

Share this file at the start of the conversation, then say what you want to build next.
Example: *"Here's my CLAUDE.md. I want to add [feature]."*
