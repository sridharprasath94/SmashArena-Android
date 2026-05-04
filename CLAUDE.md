# SmashArena – Claude Working Brief

Android sports court booking app. Users sign in, browse available courts, make bookings, and manage their reservations. Firebase Auth handles authentication (email/password + Google Sign-In).

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
| Remote | Firebase Firestore |
| View Binding | `dev.androidbroadcast.vbpd:vbpd` — all fragments use `by viewBinding(XxxBinding::bind)` |

---

## Project Structure

```
app/src/main/java/com/smasharena/
├── data/
│   ├── local/
│   │   ├── SmashArenaDatabase.kt
│   │   ├── Converters.kt
│   │   ├── dao/
│   │   │   ├── BookingDao.kt
│   │   │   └── CourtDao.kt
│   │   └── entity/
│   │       ├── BookingEntity.kt
│   │       └── CourtEntity.kt
│   ├── remote/
│   │   └── FirestoreRepository.kt
│   └── repository/
│       ├── AuthRepositoryImpl.kt
│       ├── BookingRepositoryImpl.kt
│       └── CourtRepositoryImpl.kt
├── di/
│   ├── DatabaseModule.kt
│   ├── FirebaseModule.kt
│   └── RepositoryModule.kt
├── domain/
│   ├── model/
│   │   ├── Court.kt
│   │   ├── Booking.kt
│   │   └── User.kt
│   ├── repository/                   # Interfaces
│   │   ├── AuthRepository.kt
│   │   ├── BookingRepository.kt
│   │   └── CourtRepository.kt
│   └── usecase/                      # One class per use case
├── presentation/
│   ├── MainActivity.kt
│   ├── login/      LoginFragment + LoginViewModel
│   ├── home/       HomeFragment + HomeViewModel
│   ├── booking/    BookingFragment + BookingViewModel
│   └── mybookings/ MyBookingsFragment + MyBookingsViewModel
└── util/
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
        └─► On failure → show inline error message
```

- Firebase project must be created by the user — do NOT add `google-services.json` to version control.
- `google-services.json` is in `.gitignore`.
- Logout is accessible from the top-right overflow menu (3-dot) on the home screen.
- On logout → `FirebaseAuth.getInstance().signOut()` + `GoogleSignIn.getClient().signOut()` + navigate back to LoginFragment (clear back stack).

### View Binding pattern (all fragments)
```kotlin
// Constructor supplies the layout — no onCreateView needed
class HomeFragment : Fragment(R.layout.fragment_home) {
    private val binding by viewBinding(FragmentHomeBinding::bind)
    // NO _binding, NO onDestroyView { _binding = null }
}
```

### Login screen design
- Dark background (`#1A1A1A`)
- Email + Password `TextInputLayout` fields (outlined style)
- Primary button (pill-shaped, green accent): "Sign in"
- Secondary button (outlined, pill-shaped): "Create account"
- Google button (outlined, pill-shaped, Google logo): "Sign in with Google"

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
| Firestore | Courts and bookings data |
| `google-services.json` | Created by user — NOT committed to git |

---

## Room Database

- Entities: `BookingEntity`, `CourtEntity`
- Migration policy: additive only — do NOT use `fallbackToDestructiveMigration`
- Store timestamps as `Long` epoch millis — never `java.util.Date` in entities

---

## Gradle / Dependencies

- Version catalogue: `gradle/libs.versions.toml`
- Build files: Kotlin DSL (`build.gradle.kts`)
- VBPD: `dev.androidbroadcast.vbpd:vbpd` + `vbpd-reflection` (Maven Central)

---

## Known Pending Items

| # | Item | Priority |
|---|---|---|
| 1 | Migrate existing Compose screens to XML + VBPD fragments | High |
| 2 | Add Hilt to replace manual DI | High |
| 3 | Add Firebase Auth (email/password + Google) | High |
| 4 | Restructure flat `data/` into Clean Architecture layers | High |
| 5 | Add `gradle/libs.versions.toml` version catalog | Medium |
| 6 | Add Navigation Component + Safe Args | High |
| 7 | Add remaining business requirements (TBD by user) | TBD |

---

## What NOT to suggest

- Do not use Jetpack Compose — this project uses XML layouts with VBPD
- Do not use the old `_binding`/`onDestroyView` view binding pattern — all fragments use VBPD
- Do not put Firebase credentials in the codebase — `google-services.json` is gitignored
- Do not use `fallbackToDestructiveMigration` — proper migrations must be in place
- Do not use `GlobalScope` — always use `viewModelScope` in ViewModels
- Do not use Compose Navigation — use Navigation Component + Safe Args

---

## How to start a new session

Paste this file at the top of the conversation, then describe what you want to build next.
Example: *"Here's my CLAUDE.md. I want to add a [feature]."*
