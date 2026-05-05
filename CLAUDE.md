# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# SmashArena – Claude Working Brief

Android sports facility booking app. Users sign in, browse a badminton court and cricket net, check real-time slot availability on a 14-day calendar, and make hourly bookings. Members get early access based on their plan tier.

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

## Facilities

| Enum | ID | Display Name |
|---|---|---|
| `BADMINTON_COURT` | `badminton_court_1` | Badminton Court |
| `CRICKET_NET` | `cricket_net_1` | Cricket Net |

- Operating hours: **05:00 – 22:00** daily (hours 5–21 = 17 slots)
- Each slot is exactly **1 hour**
- Firestore doc ID: `{facilityId}_{date}_{hour}` e.g. `badminton_court_1_2026-05-10_8`

---

## Membership Plans (Badminton)

| Enum | Plan | Price | Sessions/month |
|---|---|---|---|
| `RALLY` | Rally | ₹1,000 | 8 |
| `SMASH` | Smash | ₹1,500 | 12 |
| `ACE` | Ace | ₹2,000 | 16 |

- Monthly membership, stored in Firestore under `/users/{uid}`
- All tiers get the same **7-day early booking window** (days 7–6 before the slot)
- Non-members can book during days 5–0 only
- **Cricket net membership is separate** — plans TBD, not yet implemented
- Membership UI: subtle banner on Home screen → taps open `MembershipFragment` (plan comparison screen)
- No payment gateway — membership plan screen is UI-only

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
  membershipExpiry  : long?    — epoch millis
  sessionsUsed      : int
  sessionsQuota     : int      — 0 | 8 | 12 | 16
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

## Actual Project Structure

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
│       ├── AuthRepositoryImpl.kt     — Firebase Auth (email + Google); checks connectivity before each call
│       ├── SlotRepositoryImpl.kt     — Firestore slots (callbackFlow + transaction); checks connectivity before writes
│       └── UserRepositoryImpl.kt     — Firestore user profile (get-or-create)
├── di/
│   ├── DatabaseModule.kt             — Provides Room DB + DAOs
│   ├── FirebaseModule.kt             — Provides FirebaseAuth, FirebaseFirestore, GoogleSignInClient
│   └── RepositoryModule.kt           — @Binds Auth/User/SlotRepository
├── domain/
│   ├── model/
│   │   ├── AppError.kt               — Sealed class: NoInternet, NotSignedIn, SlotAlreadyBooked, auth errors, etc.
│   │   ├── Facility.kt               — Enum: BADMINTON_COURT, CRICKET_NET (id, nameRes, imageRes)
│   │   ├── MembershipTier.kt         — Enum: NONE, RALLY, SMASH, ACE (price, quota)
│   │   ├── Slot.kt                   — Data class (docId and timeLabel as computed properties)
│   │   ├── SlotStatus.kt             — Enum: AVAILABLE, MEMBER_HOLD, BOOKED, LOCKED, MY_BOOKING
│   │   └── UserProfile.kt            — Data class (isMember, sessionsRemaining as computed)
│   └── repository/
│       ├── AuthRepository.kt
│       ├── SlotRepository.kt         — observeSlots(), getMyBookings(), bookSlot(), cancelBooking()
│       └── UserRepository.kt
├── presentation/
│   ├── login/
│   │   ├── LoginFragment.kt
│   │   ├── LoginViewModel.kt
│   │   └── LoginUiState.kt           — generalError: AppError?
│   ├── home/
│   │   ├── HomeFragment.kt
│   │   ├── HomeViewModel.kt
│   │   └── HomeUiState.kt
│   ├── slots/
│   │   ├── SlotsFragment.kt
│   │   ├── SlotsViewModel.kt         — SavedStateHandle for facilityId/facilityName
│   │   ├── SlotsUiState.kt           — DateItem, DisplaySlot, BookingResultInfo, ResultType, SlotsUiState
│   │   ├── DateAdapter.kt            — Horizontal date strip (ListAdapter)
│   │   ├── SlotAdapter.kt            — 3-column slot grid (ListAdapter)
│   │   ├── BookingResultDialog.kt    — Animated result dialog (booking confirmed / cancelled)
│   │   └── ConfirmationDialog.kt     — Animated confirmation dialog (book / cancel / logout)
│   ├── membership/
│   │   ├── MembershipFragment.kt     — Plan comparison screen (UI-only, no payment)
│   │   ├── MembershipViewModel.kt
│   │   ├── MembershipUiState.kt
│   │   └── PlanAdapter.kt            — RecyclerView adapter for plan cards
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
        ├─► slotsFragment  (args: facilityId: String, facilityName: String)
        ├─► myBookingsFragment
        ├─► membershipFragment
        └─► loginFragment  (logout — popUpTo homeFragment inclusive)
```

---

## Completed Screens

### Login (`LoginFragment`)
- Email + password fields (TextInputLayout outlined)
- Sign in / Create account / Sign in with Google buttons
- `ActivityResultLauncher` for Google Sign-In intent
- Validation errors shown on TextInputLayout fields; Firebase auth errors shown as Snackbar
- `generalError: AppError?` in UiState — mapped to strings via `AppError.toUserMessage()`

### Home (`HomeFragment`)
- Two facility cards (Badminton Court, Cricket Net) — tap → SlotsFragment
- Membership banner at bottom — shows tier + sessions remaining (or "No membership · View plans →")
- Toolbar menu: **Cloud Synced** (disabled, shows last sync time) | **My Bookings** | **Log out**
- `setSupportActionBar(binding.toolbar)` called here (not in MainActivity)
- Log out uses `ConfirmationDialog` (type LOGOUT) — result via `setFragmentResult(REQUEST_LOGOUT)`

### Slots (`SlotsFragment`)
- 14-day horizontal date strip; non-browsable dates are dimmed
- 3-column slot grid colored by status (green=available, grey=booked, amber=member-hold, dark=locked, blue=mine)
- Tapping an available slot selects it → floating "Book HH:00 – HH:00" button appears (green)
- Tapping a MY_BOOKING slot selects it → floating "Cancel HH:00 – HH:00" button appears (rose)
- Book/cancel both show a `ConfirmationDialog` before acting
- After success, a `BookingResultDialog` is shown (animated icon + facility + date/time + Done button)
- Firestore listener restarted when date changes (`slotObserverJob?.cancel()`)
- Real-time updates (another user booking reflects immediately)
- Errors shown as Snackbar using `AppError.toUserMessage()`

### My Bookings (`MyBookingsFragment`)
- Lists all upcoming bookings for the current user (today and later)
- Real-time Firestore listener (`callbackFlow` with `whereEqualTo("bookedBy", uid)`)
- Client-side filtered and sorted (date asc, hour asc) — no composite Firestore index needed
- Empty state text when no upcoming bookings
- Cancel confirmation uses `ConfirmationDialog` — docId travels through the result Bundle
- `SlotRepository.cancelBooking(docId)` verifies ownership in a Firestore transaction, then deletes

### Membership (`MembershipFragment`)
- Plan comparison screen with `PlanAdapter` (RecyclerView, LinearLayoutManager)
- UI-only — no payment flow
- Navigated to from the membership banner on Home

---

## Key Patterns

### Firebase Task → Coroutine bridge
```kotlin
suspendCancellableCoroutine { cont ->
    task
        .addOnSuccessListener { cont.resume(Unit) }
        .addOnFailureListener { cont.resumeWithException(it.toAppError()) }
}
```
No `kotlinx-coroutines-play-services` dependency needed.

### Connectivity check (repositories)
```kotlin
if (!networkMonitor.isConnected()) throw AppError.NoInternet
```
Called at the start of every write operation (`bookSlot`, `cancelBooking`, all auth calls).

### Error flow
```
Repository            → throws AppError (via toAppError() or directly)
ViewModel.onFailure   → _uiState.update { it.copy(error = e.toAppError()) }
Fragment              → error.toUserMessage(requireContext()) → Snackbar
```
`AppError` is a sealed class in `domain/model/`. `ErrorMapper.toAppError()` matches Firebase exception classes first, then falls back to message-string matching.

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
```kotlin
// Show
ConfirmationDialog.book(facilityName, dateLabel, timeLabel)
    .show(childFragmentManager, "confirm_book")

// Listen (in onViewCreated)
childFragmentManager.setFragmentResultListener(ConfirmationDialog.REQUEST_BOOK, viewLifecycleOwner) { _, _ ->
    viewModel.bookSelectedSlot()
}
```
Factory methods: `book()`, `cancelSlot()`, `cancelBooking(docId)`, `logout(message)`.  
Request keys: `REQUEST_BOOK`, `REQUEST_CANCEL_SLOT`, `REQUEST_CANCEL_BOOKING`, `REQUEST_LOGOUT`.  
The docId for My Bookings cancellation travels inside the result `Bundle` via `KEY_DOC_ID`.

### BookingResultDialog (success feedback)
Shown after a successful booking or cancellation — never use Snackbar for success.
```kotlin
// ViewModel emits BookingResultInfo in SlotsUiState
bookingResultInfo = BookingResultInfo(type = ResultType.BOOKED, facilityName, dateLabel, timeLabel)

// Fragment shows dialog
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
Also works for `DialogFragment(R.layout.xxx)`.

### UI state collection
```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> /* update UI */ }
    }
}
```

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
- Do NOT implement a payment gateway — membership screens are UI-only for now
- Do NOT use `java.time` APIs — use `java.util.Calendar` / `SimpleDateFormat` for API 24 compatibility
- Do NOT commit `google-services.json` — it is gitignored
- Do NOT use `MaterialAlertDialogBuilder` for confirmation dialogs — use `ConfirmationDialog` instead
- Do NOT show Snackbar for booking/cancellation success — use `BookingResultDialog` instead
- Do NOT put raw exception messages in UiState — map through `Throwable.toAppError()` first
- Do NOT put user-facing strings in ViewModels — map `AppError` to strings in the Fragment via `toUserMessage(context)`

---

## Pending Features

| # | Feature | Notes |
|---|---|---|
| 1 | Cricket net membership plans | Separate from badminton. Plans/pricing TBD. |
| 2 | Session quota enforcement on booking | Check `sessionsUsed < sessionsQuota` before writing to Firestore. |
| 3 | Replace placeholder facility images | `img_badminton_court.xml` and `img_cricket_net.xml` are vector placeholders. |
| 4 | My Bookings screen — past bookings tab | Currently only shows upcoming (date >= today). |

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
