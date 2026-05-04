# Smash Arena

A native Android app for booking badminton courts.

## Features
- Sign in as a regular or **Premium** member.
- Browse available courts (seeded on first launch).
- Book a 30 / 60 / 90 / 120 minute slot.
- **Daily cap**: each user can hold at most **2 hours** of bookings per calendar day across all courts.
- **Peak hours (6:00 PM – 9:00 PM, local time)**: Premium members get priority. If a Premium member has already claimed a peak-window slot on a court, non-premium users can no longer take overlapping peak time on that court.
- View and cancel upcoming bookings.

## Tech stack
- Kotlin, Jetpack Compose with Material 3
- MVVM with `StateFlow` and `viewModelScope`
- Room for local persistence
- Compose Navigation
- JUnit unit tests for the booking rules

## Project layout
```
app/src/main/java/com/smasharena/
├── SmashArenaApplication.kt   # manual DI container
├── MainActivity.kt            # NavHost
├── data/                      # Room entities, DAOs, repository (rules)
├── ui/
│   ├── theme/
│   ├── login/
│   ├── courts/
│   ├── booking/
│   └── mybookings/
├── viewmodel/
└── util/
```

## Build / run
1. Open the project in Android Studio (Hedgehog or newer).
2. Let Gradle sync — the wrapper is configured for Gradle 8.7 and AGP 8.5.2.
3. Run the `app` configuration on an emulator or device with API 24+.

To generate the Gradle wrapper jar locally if it's missing, run:
```
gradle wrapper --gradle-version 8.7
```

## Tests
The booking rules are covered by `BookingRepositoryTest`. Run with:
```
./gradlew :app:testDebugUnitTest
```

## Where the rules live
All booking logic — the 2-hour cap, peak-window priority, overlap detection, and the in-the-past guard — lives in `BookingRepository.book(...)`. The function returns a typed `BookingResult` so the UI can map each rejection reason to a specific user-facing string. Because the rule code is pure Kotlin (no Android imports), it's straightforward to unit-test with in-memory DAO fakes.
