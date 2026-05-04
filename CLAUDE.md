# SmashArena – Claude Working Brief

Android sports facility booking app for a local arena. Users sign in, browse one badminton court and one cricket net, check real-time availability on a calendar, and make hourly bookings. Members get early access to slots based on their plan.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | XML layouts, View Binding (VBPD), Material 3 |
| Architecture | MVVM, Clean Architecture (domain / data / presentation) |
| DI | Hilt |
| Local DB | Room |
| Navigation | Navigation Component + Safe Args |
| Async | Coroutines + Flow |
| Auth | Firebase Authentication (email/password + Google Sign-In) |
| Remote | Firebase Firestore (real-time snapshot listeners) |
| View Binding | `dev.androidbroadcast.vbpd:vbpd` — all fragments use `by viewBinding(XxxBinding::bind)` |

---

## Facilities

| ID | Name | Type |
|---|---|---|
| `badminton_court_1` | Badminton Court | Badminton |
| `cricket_net_1` | Cricket Net | Cricket |

- Operating hours: **05:00 – 22:00** daily (17 bookable slots per facility per day)
- Each slot is exactly **1 hour**
- Slot keys: `{facilityId}_{date}_{hour}` e.g. `badminton_court_1_2025-06-10_8`

---

## Membership Plans

| Plan | Price | Sessions/month | Booking window |
|---|---|---|---|
| **Rally** | ₹1,000 / month | 8 sessions | 7–6 days early access |
| **Smash** | ₹1,500 / month | 12 sessions | 7–6 days early access |
| **Ace** | ₹2,000 / month | 16 sessions | 7–6 days early access |

- All three tiers share the same **7-day early booking window**; the difference is session quota per month
- Membership is **monthly**, stored in Firestore under the user's UID
- Membership UI is presented as a **subtle, non-intrusive banner** on the home screen — no pop-ups, no aggressive CTAs
- Tapping it opens a plan comparison screen with "Become a Member" CTA (UI only — no payment gateway for now)
- **Cricket nets membership is separate from badminton membership** — plans, pricing, and quota are defined independently. Details TBD.

---

## Booking Window Rules

```
Day 7 to Day 6 before session
  → Member-only window (Rally / Smash / Ace)
  → Non-members cannot see or book these slots yet

Day 5 before session
  → Any member slot NOT booked by end of Day 6 is released to hourly booking

Day 5 to Day 3 before session
  → Hourly booking open to ALL users (members + non-members)

Day 3 to Day 0 (same day)
  → Hourly booking continues; first-come, first-served

Beyond Day 7
  → No booking possible for anyone
```

- Rules enforced **client-side** (disable/hide slots) AND in **Firestore security rules** (source of truth)
- Members are limited by their monthly session quota — the app checks remaining sessions before confirming a booking
- Session quota resets on the 1st of each month

---

## Firestore Data Model

```
/slots/{facilityId}_{date}_{hour}
  facilityId    : string   — "badminton_court_1" | "cricket_net_1"
  date          : string   — "YYYY-MM-DD"
  hour          : int      — 5..21 (5 = 05:00–06:00)
  status        : string   — "available" | "member_hold" | "booked"
  bookedBy      : string?  — Firebase Auth UID | null
  bookedAt      : timestamp?

/users/{uid}
  displayName       : string
  email             : string
  membershipTier    : string   — "none" | "rally" | "smash" | "ace"
  membershipExpiry  : timestamp?
  sessionsUsed      : int      — resets monthly
  sessionsQuota     : int      — 0 | 8 | 12 | 16
```

---

## Project Structure

```
app/src/main/java/com/smasharena/
├── data/
│   ├── local/
│   │   ├── SmashArenaDatabase.kt
│   │   ├── Converters.kt
│   │   ├── dao/
│   │   │   └── BookingDao.kt
│   │   └── entity/
│   │       └── BookingEntity.kt          # Cached local copy of user's own bookings
│   ├── remote/
│   │   ├── SlotRemoteDataSource.kt       # Firestore slot reads + snapshot listeners
│   │   └── UserRemoteDataSource.kt       # Firestore user profile reads/writes
│   └── repository/
│       ├── AuthRepositoryImpl.kt
│       ├── BookingRepositoryImpl.kt
│       └── UserRepositoryImpl.kt
├── di/
│   ├── DatabaseModule.kt
│   ├── FirebaseModule.kt
│   └── RepositoryModule.kt
├── domain/
│   ├── model/
│   │   ├── Facility.kt                   # Enum: BADMINTON_COURT, CRICKET_NET
│   │   ├── Slot.kt                       # Domain model for a 1-hour slot
│   │   ├── SlotStatus.kt                 # Enum: AVAILABLE, MEMBER_HOLD, BOOKED, LOCKED
│   │   ├── Booking.kt
│   │   ├── MembershipTier.kt             # Enum: NONE, RALLY, SMASH, ACE
│   │   └── UserProfile.kt
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   ├── BookingRepository.kt
│   │   └── UserRepository.kt
│   └── usecase/
│       ├── GetSlotsForDateUseCase.kt
│       ├── BookSlotUseCase.kt
│       ├── CancelBookingUseCase.kt
│       └── CheckBookingEligibilityUseCase.kt
├── presentation/
│   ├── MainActivity.kt
│   ├── login/
│   │   ├── LoginFragment.kt
│   │   └── LoginViewModel.kt
│   ├── home/
│   │   ├── HomeFragment.kt               # Lists facilities + membership banner
│   │   └── HomeViewModel.kt
│   ├── slots/
│   │   ├── SlotsFragment.kt              # Calendar + hourly slot grid for a facility
│   │   └── SlotsViewModel.kt
│   ├── mybookings/
│   │   ├── MyBookingsFragment.kt
│   │   └── MyBookingsViewModel.kt
│   └── membership/
│       ├── MembershipFragment.kt         # Plan comparison screen
│       └── MembershipViewModel.kt
└── util/
    ├── DateTimeUtils.kt
    └── BookingWindowUtils.kt             # Encapsulates the 7/5/3-day window logic
```

---

## Key Architecture Decisions (Do Not Change Without Reason)

### Authentication flow
```
LoginFragment
  │
  ├─► Email/Password → FirebaseAuth.signInWithEmailAndPassword()
  ├─► Create Account → FirebaseAuth.createUserWithEmailAndPassword()
  └─► Google Sign-In → GoogleSignInClient → FirebaseAuth.signInWithCredential()
        └─► On success → navigate to HomeFragment (clear back stack)
        └─► On failure → show inline error message (never Toast — use TextInputLayout error)
```

- Firebase project is created by the user — do NOT add `google-services.json` to version control
- Logout: `FirebaseAuth.signOut()` + `GoogleSignIn.getClient().signOut()` → navigate to LoginFragment (clear back stack)
- Logout is in the top-right overflow menu (3-dot) on HomeFragment

### Slot availability (real-time)
- `SlotsFragment` opens a Firestore snapshot listener for all slots of a given `facilityId` + `date`
- Listener is started in `onStart` and removed in `onStop` via the ViewModel's `onCleared`
- Slot status changes (another user booking) are reflected immediately without a pull-to-refresh

### Booking window enforcement
- `BookingWindowUtils.getSlotStatus(date, hour, userTier)` returns the effective `SlotStatus` for display
- `CheckBookingEligibilityUseCase` runs before any booking write: checks window + quota
- Firestore security rules mirror these checks — client-side is UX, server-side is truth

### View Binding pattern (all fragments)
```kotlin
class HomeFragment : Fragment(R.layout.fragment_home) {
    private val binding by viewBinding(FragmentHomeBinding::bind)
    // NO _binding, NO onDestroyView { _binding = null }
}
```

### Login screen design
- Dark background (`#1A1A1A`)
- Email + Password `TextInputLayout` fields (outlined style)
- Primary button (pill-shaped, green accent `#B5D86C`): "Sign in"
- Secondary button (outlined, pill-shaped): "Create account"
- Google button (outlined, pill-shaped, Google logo): "Sign in with Google"

### Membership banner (HomeFragment)
- Small card at the bottom of the facilities list — muted styling, not a popup
- Shows current plan name (or "No membership") + CTA "View plans →"
- Tapping navigates to MembershipFragment

### Overflow menu (HomeFragment toolbar)
```
⋮
├── Cloud Synced  ✓  (Last synced: <timestamp>)
├── Backup
├── Share
└── Log out
```

---

## Firebase Setup

| Item | Value |
|---|---|
| Auth providers | Email/Password, Google Sign-In |
| Firestore collection | `slots`, `users` |
| `google-services.json` | Created by the user — NOT committed to git |
| Firestore rules | Client: read-only on slots, read-write own user doc; writes go through security rules |

---

## Room Database

- Version: **1**
- Entities: `BookingEntity` (local cache of the signed-in user's own past bookings)
- Migration policy: additive only — do NOT use `fallbackToDestructiveMigration`
- Timestamps stored as `Long` epoch millis — never `java.util.Date` in entities

---

## Gradle / Dependencies

- Version catalogue: `gradle/libs.versions.toml`
- Build files: Kotlin DSL (`build.gradle.kts`)
- VBPD: `dev.androidbroadcast.vbpd:vbpd` + `vbpd-reflection` (Maven Central)

| Dependency | Version |
|---|---|
| AGP | 8.7.3 |
| Kotlin | 2.1.0 |
| KSP | 2.1.0-1.0.29 |
| Hilt | 2.54 |
| Room | 2.7.0 |
| Navigation | 2.8.5 |
| Firebase BOM | 33.8.0 |
| Google Services plugin | 4.4.2 |
| VBPD | 2.0.4 |

---

## Known Pending Items

| # | Item | Priority |
|---|---|---|
| 1 | Migrate existing Compose screens to XML + VBPD fragments | High |
| 2 | Add Hilt to replace manual DI | High |
| 3 | Set up Firebase Auth (email/password + Google) | High |
| 4 | Restructure flat `data/` into Clean Architecture layers | High |
| 5 | Add `gradle/libs.versions.toml` version catalog | High |
| 6 | Add Navigation Component + Safe Args | High |
| 7 | Implement slot calendar + real-time Firestore listener | High |
| 8 | Implement booking window enforcement logic | High |
| 9 | Implement membership plan comparison screen | Medium |
| 10 | Payment gateway for membership purchase | Not in scope yet |

---

## What NOT to suggest

- Do not use Jetpack Compose — this project uses XML layouts with VBPD
- Do not use the old `_binding`/`onDestroyView` view binding pattern — all fragments use VBPD
- Do not put Firebase credentials in the codebase — `google-services.json` is gitignored
- Do not use `fallbackToDestructiveMigration` — proper migrations must be in place
- Do not use `GlobalScope` — always use `viewModelScope` in ViewModels
- Do not use Compose Navigation — use Navigation Component + Safe Args
- Do not add a payment gateway yet — membership plan screen is UI-only for now
- Do not increase slot granularity below 1 hour

---

## How to start a new session

Paste this file at the top of the conversation, then describe what you want to build next.
Example: *"Here's my CLAUDE.md. I want to add a [feature]."*
