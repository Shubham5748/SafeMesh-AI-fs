# SafeMesh AI — Native Android Architecture Documentation

This document describes the architectural design and structural layers of the native Android application located in `android/`.

---

## 1. Architectural Pattern: Clean MVVM (Model-View-ViewModel)

The application follows the official Android Architecture recommendations using **Jetpack Compose** for UI, **StateFlow** for reactive unidirectional data flow, and **Kotlin Coroutines** for asynchronous operations.

```
                      ┌──────────────────────────────────────┐
                      │             UI Layer                 │
                      │  (Jetpack Compose + Material 3)      │
                      │  - HomeScreen                        │
                      │  - SafetyScreen                      │
                      │  - EmergencyOverlayScreen            │
                      │  - EmergencyContactsScreen           │
                      │  - ProfileScreen                     │
                      │  - PermissionManagerScreen           │
                      └──────────────────┬───────────────────┘
                                         │ Observes StateFlow
                                         │ Dispatches Events
                                         ▼
                      ┌──────────────────────────────────────┐
                      │          ViewModel Layer             │
                      │        (EmergencyViewModel)          │
                      │  - sosState: StateFlow<SosUiState>   │
                      │  - contacts: StateFlow<List>         │
                      │  - 10s Countdown Timer               │
                      └──────────────────┬───────────────────┘
                                         │ Calls
                                         ▼
                      ┌──────────────────────────────────────┐
                      │          Repository Layer            │
                      │       (EmergencyRepository)          │
                      │  - Coordinates Data & Network        │
                      └──────────┬───────────────────┬───────┘
                                 │                   │
                  ┌──────────────┴────────┐  ┌───────┴───────────────┐
                  ▼                       ▼  ▼                       ▼
       ┌─────────────────────┐ ┌───────────────────┐ ┌─────────────────────┐
       │   Location Layer    │ │   Local Storage   │ │      API Layer      │
       │  (LocationService)  │ │(PreferencesManager│ │ (Retrofit + OkHttp) │
       │  - Google Play Fused│ │ - Jetpack DataStore│ │ - ApiService        │
       │    Location Provider│ │ - 'sos_contacts'  │ │ - POST /api/        │
       └─────────────────────┘ └───────────────────┘ │   emergency         │
                                                     └─────────────────────┘
```

---

## 2. Detailed Architectural Layers

### A. UI Layer (`com.safemesh.ai.ui`)
- **Theme (`ui.theme`):**
  - Dark aesthetic tokens matching the web prototype: `BgDark` (`#0B132B`), `CardDark` (`#1C2541`), `AccentRed` (`#EF4444`), `AccentBlue` (`#3B82F6`), `AccentGreen` (`#22C55E`).
- **Components (`ui.components`):**
  - `GlassPanel`: Custom Compose container providing translucent background, rounded borders, and inner padding matching web glassmorphism.
- **Screens (`ui.screens`):**
  - `SplashScreen`: Animated scaling Shield logo and progressive loading bar.
  - `HomeScreen`: Status indicators, user greetings, and large pulsing red SOS button.
  - `SafetyScreen`: Real-time status cards (Fall Detection, Motion Analysis, Voice Distress) and a test fall trigger button.
  - `EmergencyOverlayScreen`: Fullscreen modal with 10-second countdown, pulsing red shield, immediate cancel/send actions, and live delivery counter with clickable Google Maps link.
  - `EmergencyContactsScreen`: Contact manager list with Add Contact form (Name, Phone, Relation) and deletion controls.
  - `ProfileScreen`: User badge, Medical emergency card (Blood group, allergies), navigation to Contacts/Permissions, and configurable Backend URL editor.
  - `PermissionManagerScreen`: User-friendly permission explainer with individual grant buttons for Location, Microphone, Notifications, and Bluetooth.
- **Navigation (`ui.navigation`):**
  - Single Activity (`MainActivity`) with Compose `NavHost` and `NavigationBar`.
  - Global `EmergencyOverlayScreen` renders over any active screen when `sosState !is SosUiState.Idle`.

### B. ViewModel Layer (`com.safemesh.ai.ui.viewmodel`)
- **`EmergencyViewModel`:**
  - Manages the emergency lifecycle through a sealed class `SosUiState`:
    - `Idle`: Standby mode.
    - `CountingDown(secondsRemaining, reason)`: 10-second cancel window active.
    - `Dispatching(reason, locationStatus)`: Requesting GPS and contacting the server.
    - `Delivered(response, locationData)`: SOS confirmed by server, displaying dispatch statistics.
    - `Error(message)`: Clear error message when backend or network fails.
  - Handles the 10-second countdown coroutine (`countdownJob`).
  - Implements cancellation (`cancelSos`), instant bypass (`sendNow`), and reset.

### C. Repository Layer (`com.safemesh.ai.data.repository`)
- **`EmergencyRepository`:**
  - Single point of truth coordinating local preferences, GPS location, and remote HTTP requests.
  - Exposes `contactsFlow`, `userNameFlow`, and `backendUrlFlow`.
  - Performs validation: refuses to dispatch if no emergency contacts exist.
  - Maps domain models into the backend payload schema.

### D. Location Layer (`com.safemesh.ai.data.location`)
- **`LocationService`:**
  - Integrates Google Play Services `FusedLocationProviderClient`.
  - Implements high-accuracy GPS requests with coroutine `suspendCancellableCoroutine`.
  - Checks recent cached location ($< 60\text{s}$) first, falling back to a fresh high-accuracy location request with a 10-second timeout.
  - Returns `Result<LocationData>` with precise error diagnostics on failure.

### E. Local Storage Layer (`com.safemesh.ai.data.local`)
- **`PreferencesManager`:**
  - Powered by AndroidX Jetpack DataStore (Preferences).
  - Persists:
    - `'sos_contacts'`: JSON array matching the web contact schema (`id`, `name`, `phone`, `relation`).
    - `'user_name'`: Current user's name.
    - `'backend_url'`: Configurable server address (`http://10.0.2.2:3001/` default).

### F. API Layer (`com.safemesh.ai.data.api`)
- **`ApiService` & `ApiClient`:**
  - Retrofit 2 + OkHttp 3 with Gson converter and `HttpLoggingInterceptor`.
  - Dynamically updates Retrofit instance if the user customizes the backend URL.
  - Strictly follows the backend contract for `POST /api/emergency` and `GET /api/health`.
  - Connect and read timeouts set to 12-15 seconds.

---

## 3. Error Handling Architecture

1. **No Emergency Contacts:**
   - Intercepted before making a network call. Sets `SosUiState.Error("No emergency contacts configured. Please add contacts in SOS Contacts.")`.
2. **GPS Unavailable or Timed Out:**
   - Does not abort emergency dispatch; safely falls back to `{ location: null }` so SOS alerts can still go through.
3. **Backend Unreachable / Network Offline:**
   - Catches `ConnectException` and `SocketTimeoutException`. Displays actionable message indicating the server could not be reached at the configured URL.
4. **HTTP Server Error:**
   - Reads `response.errorBody()` and reports the exact backend error code and message.
   - **Never shows fake delivery states on failure.**
