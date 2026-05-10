# SmashArena

An Android sports facility booking app for badminton courts and cricket nets. Sign in, pick a date and time, book a slot — members get early access and bonus sessions.

---

## Features

- **Firebase sign-in** — email/password authentication
- **Two facilities** — badminton court and cricket net, bookable 5 AM – 9 PM
- **Dynamic pricing** — rates vary by time of day (early / mid / late)
- **Booking window** — members get a 6–7 day head start; open to all within 5 days
- **Consecutive slot cap** — max 2 back-to-back slots per user per day
- **My Bookings** — view and cancel upcoming reservations
- **Membership plans** — RALLY, SMASH, ACE with included + bonus sessions
- **Upgrade / downgrade / cancel** — upgrade pays the difference; changes to a lower plan take effect next cycle
- **Real-time profile** — home screen reflects membership and session counts live via Firestore listeners

---

## Facilities & Pricing

| Facility | Early (5–10h) | Mid (10–17h) | Late (17–21h) |
|---|---|---|---|
| 🏸 Badminton Court | ₹300 | ₹200 | ₹300 |
| 🏏 Cricket Net | ₹400 | ₹300 | ₹400 |

Bookable hours: **5:00 AM – 9:00 PM**

---

## Membership Plans

| Plan | Price | Included Sessions | Bonus Sessions | Validity |
|---|---|---|---|---|
| RALLY | ₹1,000 | 8 | +2 | 30 days |
| SMASH | ₹1,500 | 12 | +3 | 30 days |
| ACE | ₹2,000 | 16 | +4 | 30 days |

- **Upgrade** — pay only the difference; takes effect immediately
- **Downgrade / Cancel** — scheduled for the next cycle; no charge or refund

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | XML layouts + View Binding (VBPD), Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Navigation | Navigation Component + Safe Args |
| Async | Coroutines + StateFlow |
| Backend | Firebase Auth + Firestore |
| Local DB | Room |

---

## How It Works

### Booking window

```
Days until the slot
    │
    ├─► > 7 days  → LOCKED       (too far ahead — no one can book)
    ├─► 6–7 days  → MEMBER HOLD  (active members only)
    ├─► 0–5 days  → Open         (all signed-in users)
    └─► Past      → LOCKED
```

### Slot availability

A slot document in Firestore is only created when someone books it — a missing document means the slot is available. The app checks for conflicts on the fly and enforces the 2-consecutive-slot cap before writing.

### Payment simulation

Both booking and membership payment screens simulate a 1.5-second processing delay before confirming the transaction against Firestore.

---

## Architecture

```
presentation/      Fragments + ViewModels + UI state (StateFlow)
    │
domain/            Pure Kotlin — use cases, models, repository interfaces
    │
data/
    ├── local/     Room (additive migrations only)
    └── remote/    Firebase Auth + Firestore repositories
```

Navigation follows a single-activity pattern:
```
Login → Home → Slots → Payment
              → My Bookings
              → Membership → Membership Payment
```

Cross-fragment results (booking confirmation, membership changes) are passed via `savedStateHandle`.

---

## Setup

### Prerequisites

- Android Studio Meerkat or later
- A Firebase project with **Authentication** (email/password) and **Firestore** enabled

### Steps

1. Clone the repository
2. Add `google-services.json` to `app/` (Firebase Console → Project settings → Your apps)
3. Sync Gradle and run on an API 24+ emulator or device

### Build

```bash
./gradlew clean :app:assembleDebug --no-build-cache
```

Always run `clean` with `--no-build-cache` — the Gradle build cache can produce a corrupted intermediates APK that causes deploy to fail.

### Tests

```bash
./gradlew :app:testDebugUnitTest
```
