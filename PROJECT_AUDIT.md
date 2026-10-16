# SafeMesh AI — Codebase Audit Report

**Date:** 2026-10-13  
**Repository Audited:** `https://github.com/Shubham5748/SafeMesh-AI-fs` (Local Directory: `C:\Users\user\.gemini\antigravity\scratch\SafeMesh-AI-2`)  
**Scope:** Complete Codebase Architecture & Mobile Porting Assessment  

---

## 1. Complete Frontend Structure

The frontend is a single-page web application bootstrapped with **Vite 5** and **React 18** (SPA without React Router). It simulates a mobile app layout inside a dark-mode responsive shell (`app-container` in `src/index.css`).

```
SafeMesh-AI-2/
├── index.html                   # HTML entry point with Outfit font & responsive viewport meta
├── vite.config.js               # Vite config with React plugin, allowedHosts: true, /api proxy
├── package.json                 # Dependencies & scripts
└── src/
    ├── main.jsx                 # React root render
    ├── App.jsx                  # Main router state (tab navigation, auth state, splash, overlay)
    ├── index.css                # Base theme variables (colors, glow, container layout)
    ├── screens.css              # Styling for all screens (cards, SOS pulse button, grid)
    ├── components/
    │   └── EmergencyOverlay.jsx # 10-second countdown, GPS fetch, SOS API dispatch
    └── screens/
        ├── Auth.jsx             # Mock authentication screen (Email/Password, Google, Biometric)
        ├── Home.jsx             # Dashboard with status cards, big SOS button, nearby relays mock
        ├── Safety.jsx           # Sensor screen: fall detection, live magnitude, mic/voice listener
        ├── Mesh.jsx             # Visual simulation of BLE mesh nodes and packet relay
        ├── History.jsx          # Mock emergency event timeline
        ├── Profile.jsx          # Mock user & medical card, navigation to emergency contacts
        ├── LiveMap.jsx          # Mock tactical safe route map UI (CSS-based grid & markers)
        └── EmergencyContacts.jsx# Real CRUD manager for emergency contacts stored in localStorage
```

---

## 2. Complete Backend Structure

The backend is a standalone Node.js service located in `server.js` using **Express 5** and the **Twilio SDK**.

```
SafeMesh-AI-2/
├── server.js                    # Express app listening on port 3001 (or process.env.PORT)
└── .env                         # Twilio credentials (TWILIO_ACCOUNT_SID, AUTH_TOKEN, PHONE_NUMBER)
```

**Architecture & Middleware:**
- `cors()` enabled for cross-origin requests.
- `express.json()` for parsing JSON payloads.
- `dotenv.config()` for loading Twilio environment variables.
- Dual-mode operation:
  - If Twilio credentials are valid in `.env` -> initializes `twilio(accountSid, authToken)` and operates in **LIVE** mode.
  - If credentials are missing -> falls back to **SIMULATION MODE**, mock-logging the dispatch to standard output without executing network calls.

---

## 3. Existing React Components

| Component | File | Type | Description |
| :--- | :--- | :--- | :--- |
| `App` | `src/App.jsx` | Container / State Router | Controls `showSplash`, `isAuthenticated`, `currentTab` ('home' \| 'safety' \| 'mesh' \| 'history' \| 'profile' \| 'map' \| 'contacts'), and manages `showEmergency` state. |
| `SplashScreen` | `src/App.jsx` | Presentational | 2.5-second animated loading screen with `Shield` logo and progress bar. |
| `Navigation` | `src/App.jsx` | Nav Bar | Fixed bottom navigation glass bar with icons (`Home`, `Shield`, `Share2`, `Clock`, `User`). |
| `Auth` | `src/screens/Auth.jsx` | Screen | UI form for login, registration, password reset, simulated Google and Biometric buttons. |
| `Home` | `src/screens/Home.jsx` | Screen | Dashboard showing system status pills, big central pulse-animated SOS button, and nearby relay card. |
| `Safety` | `src/screens/Safety.jsx` | Screen | Real-time sensor processing: calculates accelerometer vector magnitude, runs fall detection state machine, handles audio volume monitoring, and listens for distress keywords. |
| `ModuleCard` | `src/screens/Safety.jsx` | Sub-component | Card displaying icon, title, sensor reading/score, and dynamic colored status pill. |
| `EmergencyOverlay`| `src/components/EmergencyOverlay.jsx` | Modal Overlay | Fullscreen modal triggered by SOS. Handles 10-second cancel window, GPS acquisition, `/api/emergency` fetch, and display of dispatch results (SMS/Call counters + Google Maps link). |
| `EmergencyContacts` | `src/screens/EmergencyContacts.jsx` | Screen | Contact management UI: list of contacts, form to add new contacts (`name`, `phone`, `relation`), delete button, syncing to `localStorage`. |
| `LiveMap` | `src/screens/LiveMap.jsx` | Screen | Static mockup of a tactical HUD map with user marker, police station marker, danger zone, and safe route ETA. |
| `Mesh` | `src/screens/Mesh.jsx` | Screen | Simulated packet animation moving across virtual BLE nodes ("You", "Relay A", "Relay B", "Cloud"). |
| `Node` | `src/screens/Mesh.jsx` | Sub-component | Visual node in the mesh with ping animation. |
| `History` | `src/screens/History.jsx` | Screen | Static timeline displaying past hardcoded emergency events (`EMG-9021`, etc.). |
| `StatusBadge` | `src/screens/History.jsx` | Sub-component | Badge indicator for event statuses (`Delivered`, `Cancelled`). |
| `Profile` | `src/screens/Profile.jsx` | Screen | Profile view with mock medical info (Blood group, allergies) and navigation link to Emergency Contacts. |
| `SettingItem` | `src/screens/Profile.jsx` | Sub-component | Settings row with icon and chevron. |

---

## 4. Existing APIs

The backend exposes two REST endpoints (`server.js`):

### 1. `GET /api/health`
- **Purpose:** Health check and environment probe.
- **Response:**
  ```json
  { "status": "ok", "simulationMode": false }
  ```

### 2. `POST /api/emergency`
- **Purpose:** Initiates dispatch of emergency SMS and voice calls.
- **Payload:**
  ```json
  {
    "contacts": [{ "name": "Mom", "phone": "9876543210" }],
    "reason": "Possible Fall Detected (High impact + stillness)",
    "userName": "Alex Chen",
    "location": { "latitude": 28.6335, "longitude": 77.4475 }
  }
  ```
- **Validation:** Returns `400 Bad Request` if `contacts` array is missing or empty.
- **Normalization:** Cleans non-digits/`+`; prepends `+91` if phone length is exactly 10 digits.
- **Response:**
  ```json
  {
    "success": true,
    "simulated": false,
    "smsSent": 1,
    "callsSent": 1,
    "location": true,
    "locationUrl": "https://www.google.com/maps?q=28.6335,77.4475",
    "results": [
      {
        "name": "Mom",
        "phone": "+919876543210",
        "smsSID": "SM...",
        "callSID": "CA...",
        "error": null
      }
    ]
  }
  ```

---

## 5. Existing Fall Detection Implementation

**Location:** `src/screens/Safety.jsx` (`handleMotion` callback and `useEffect`)

**Mechanism:**
1. **Sensor Listener:** Subscribes to window `devicemotion` event.
2. **Acceleration Computation:**
   - Evaluates `accelerationIncludingGravity` (with fallback to `acceleration`).
   - Calculates 3-axis Euclidean vector magnitude:
     $$\text{magnitude} = \sqrt{a_x^2 + a_y^2 + a_z^2}$$
   - Filters out `NaN` and `0` values. Live value displayed in UI (`liveMagnitude` in $\text{m/s}^2$).
3. **Thresholds:**
   - `IMPACT_THRESHOLD = 25` $\text{m/s}^2$ ($\approx 2.55\,g$)
   - `STILLNESS_THRESHOLD_MIN = 8` $\text{m/s}^2$
   - `STILLNESS_THRESHOLD_MAX = 11` $\text{m/s}^2$
   - `STILLNESS_DURATION = 2000` $\text{ms}$
4. **State Machine (`fallStateRef.current`):**
   - **`idle`:** If magnitude exceeds $25\,\text{m/s}^2$, transitions to `impact`, sets risk score to 60%, and schedules a 1000 ms timer.
   - **`stillness_check`:** After 1 second, checks if magnitude has settled into normal stillness gravity ($8\text{--}11\,\text{m/s}^2$). If yes, sets score to 85% and enters `confirming` for 2000 ms. If active motion resumes outside this band, cancels and resets to `idle`.
   - **`confirming`:** If stillness is sustained throughout the 2000 ms window, fires `triggerSos('Possible Fall Detected (High impact + stillness)')` and risk score hits 100%. If movement occurs during this window, timer is cleared and returns to `idle`.
5. **iOS Compatibility:** Includes explicit check for `DeviceMotionEvent.requestPermission()` with a button to request permission.
6. **Test Simulation:** Includes a "Test Fall Impact Event" button that immediately forces fall detection state to 100% and triggers SOS.

---

## 6. Existing Voice / Distress Detection

**Location:** `src/screens/Safety.jsx` (`activateSensors`, `startSpeechRecognition`, `startAudioLevelMonitor`)

**Mechanism:**
1. **Dual Detection Engine:**
   - **A. Web Speech Recognition (`webkitSpeechRecognition` / `SpeechRecognition`):**
     - Configured with `continuous = true`, `interimResults = true`, `lang = 'en-US'`.
     - Scans real-time transcripts for distress keywords:
       `'help'`, `'emergency'`, `'save me'`, `'save'`, `'bachao'`, `'bachaao'`, `'madad'`, `'madat'`, `'stop'`, `'danger'`, `'accident'`, `'fire'`, `'police'`.
     - When matched, triggers `triggerSos('Voice Distress: heard "<keyword>"')`.
     - Automatically attempts restart on `onend` after a 600 ms backoff.
   - **B. Acoustic Volume Analyzer (`AudioContext` / `createAnalyser`):**
     - Connects active microphone `MediaStream` to an FFT Analyser (`fftSize: 256`).
     - Calculates average byte frequency data across bins.
     - Detects sudden high-volume spikes/screams ($> 75$ volume threshold).
     - Triggers `triggerSos('Acoustic Distress: High volume scream/noise detected')`.
2. **Permission Handling:**
   - Dedicated `activateSensors()` function prompts `navigator.mediaDevices.getUserMedia({ audio: true })`.
   - Displays real-time browser error messages (`err.name`, `err.message`) if blocked.

---

## 7. Existing 10-Second Emergency Verification

**Location:** `src/components/EmergencyOverlay.jsx`

**Mechanism:**
1. Triggered whenever `handleSosTrigger(reason)` is called in `App.jsx`.
2. Sets overlay state `status = 'counting'` with initial `countdown = 10`.
3. An active `useEffect` decreases countdown by 1 every 1000 ms.
4. **User Actions during Countdown:**
   - **Cancel SOS:** User can tap `<X size={20} /> Cancel SOS` to abort before dispatch. Calls `onCancel()`, returning safely to previous screen.
   - **Send Now:** User can bypass the remaining countdown and tap `Send Now` to dispatch immediately.
5. **Auto-Dispatch:** When `countdown === 0`, `handleDispatch()` executes automatically.
6. A 15-second global cooldown (`cooldownRef.current` in `Safety.jsx`) prevents duplicate triggers.

---

## 8. Existing GPS Implementation

**Location:** `src/components/EmergencyOverlay.jsx` (`getLocation`)

**Mechanism:**
1. Invoked on SOS dispatch via `navigator.geolocation.getCurrentPosition()`.
2. Implements a two-tier accuracy fallback:
   - **Tier 1 (High Accuracy):** Requests GPS with `{ enableHighAccuracy: true, timeout: 8000, maximumAge: 30000 }`.
   - **Tier 2 (Standard Accuracy):** If Tier 1 fails or times out, re-attempts with `{ enableHighAccuracy: false }`.
   - If both fail, gracefully falls back to `{ location: null }`.
3. When resolved, captures `{ latitude, longitude }`.
4. Transmitted in the POST payload to `/api/emergency`.
5. Frontend renders a clickable direct Google Maps URL:
   `https://www.google.com/maps?q=${latitude},${longitude}`.
6. Backend includes coordinates in the SMS body and converts them to formatted speech for the Twilio voice call.

---

## 9. Existing Emergency Contacts Implementation

**Location:** `src/screens/EmergencyContacts.jsx` and `src/components/EmergencyOverlay.jsx`

**Mechanism:**
1. **Persistence:** Saved in browser `localStorage` under the key `'sos_contacts'`.
2. **Schema:** Array of contact objects:
   ```json
   [
     {
       "id": "1697200000000",
       "name": "Mom",
       "phone": "+919876543210",
       "relation": "Mother"
     }
   ]
   ```
3. **CRUD Operations:**
   - **Create:** Form to add name, phone, relation. Generates unique ID via `Date.now().toString()`.
   - **Read:** Loads on mount into state.
   - **Delete:** Remove contact by ID via trash icon.
4. **Integration with SOS:**
   - `EmergencyOverlay.jsx` reads `'sos_contacts'` from `localStorage` on dispatch.
   - If array is empty, prevents network request and displays: `"No emergency contacts configured. Please add contacts in Profile > Contacts."`

---

## 10. Existing SOS Flow

```
[Trigger Source]
   ├── Manual SOS Button (Home.jsx)
   ├── Fall Detection State Machine (Safety.jsx)
   ├── Test Fall Event Button (Safety.jsx)
   ├── Voice Keyword Match (Safety.jsx)
   └── Acoustic Volume Spike (Safety.jsx)
          │
          ▼
   handleSosTrigger(reason) [App.jsx]
          │
          ▼
   Mount EmergencyOverlay.jsx
          │
          ├── 10-Second Countdown (User can "Cancel" or "Send Now")
          │
          ▼
   handleDispatch() [EmergencyOverlay.jsx]
          │
          ├── 1. Fetch GPS Coordinates (High Accuracy -> Low Accuracy)
          ├── 2. Read Contacts from localStorage ('sos_contacts')
          │       └── If empty -> Show Error state & abort
          ├── 3. Send POST /api/emergency (with fallback to direct port 3001)
          │
          ▼
   server.js handles request
          │
          ├── Simulation Mode (if no Twilio keys) -> Log to console
          └── Live Mode (if Twilio keys present) -> Dispatch SMS & Calls
          │
          ▼
   Return JSON Response
          │
          ▼
   EmergencyOverlay displays "Delivered" screen:
          ├── SMS Counter
          ├── Call Counter
          └── Direct Clickable Google Maps Link
```

---

## 11. Existing Twilio Implementation

**Location:** `server.js`

- **Initialization:** Reads `TWILIO_ACCOUNT_SID` and `TWILIO_AUTH_TOKEN`.
- **SMS Dispatch:**
  - Uses `twilioClient.messages.create()`.
  - Body format:
    ```
    🚨 SAFEMESH AI SOS
    Emergency detected for {userName}
    Type: {reason}
    Location: https://www.google.com/maps?q={lat},{lng}
    Please check on them immediately.
    ```
- **Voice Call Dispatch:**
  - Uses `twilioClient.calls.create()`.
  - Uses `process.env.TWILIO_TWIML_URL` (or falls back to `http://demo.twilio.com/docs/voice.xml`).
  - Prepares TwiML string with voice `"alice"`, announcing user name, emergency reason, and spoken coordinates.
- **Trial Limitations Encountered:**
  - Trial accounts require recipient numbers to be pre-verified on Twilio Console.
  - SMS messages in trial accounts are restricted to predefined templates.

---

## 12. Existing Environment Variables

Found in `.env`:

| Variable | Current Value / Format | Purpose |
| :--- | :--- | :--- |
| `TWILIO_ACCOUNT_SID` | `AC****************************` (redacted) | Twilio Account Identifier |
| `TWILIO_AUTH_TOKEN` | `****************************` (redacted) | Twilio Authentication Token |
| `TWILIO_PHONE_NUMBER` | `+1XXXXXXXXXX` (redacted) | Sender Twilio Virtual Phone Number |
| `PORT` | *(Optional, defaults to 3001)* | Express server port |
| `TWILIO_TWIML_URL` | *(Optional in code, not currently in .env)* | Hosted TwiML URL for voice instructions |

---

## 13. Existing localStorage Usage

Only one key is currently stored and consumed across the frontend:

| Key | Format | Read By | Written By | Description |
| :--- | :--- | :--- | :--- | :--- |
| `'sos_contacts'` | JSON Array of Objects | `EmergencyContacts.jsx`, `EmergencyOverlay.jsx` | `EmergencyContacts.jsx` | Stores the user's emergency contacts list (`id`, `name`, `phone`, `relation`). |

---

## 14. Existing Dependencies

### Frontend & Backend Root (`package.json`)

**Runtime Dependencies:**
- `express` (`^5.2.1`): HTTP REST API server.
- `cors` (`^2.8.6`): Cross-Origin Resource Sharing middleware.
- `dotenv` (`^18.0.4`): Loads environment variables from `.env`.
- `twilio` (`^6.1.2`): Twilio Node.js helper library for SMS and Voice.
- `react` (`^18.2.0`): Core React library.
- `react-dom` (`^18.2.0`): DOM rendering for React.
- `lucide-react` (`^0.292.0`): SVG icon library used across all screens.

**Development Dependencies:**
- `vite` (`^5.0.0`): Development server and build tool.
- `@vitejs/plugin-react` (`^4.2.1`): Babel/Babel-SWC fast refresh plugin.
- `@vitejs/plugin-basic-ssl` (`^1.2.0`): Self-signed HTTPS certificate generation for local development.

---

## 15. Existing Bugs or Technical Limitations

1. **Web Browser Lifecycle (Background Execution):**
   - In a web browser (mobile Chrome/Safari), once the phone screen turns off or the user switches tabs, `DeviceMotionEvent` and `SpeechRecognition` are paused or throttled by mobile OS power-saving policies. Continuous 24/7 background protection does not work in a browser.
2. **Microphone Permission Requirements:**
   - Mobile browsers strictly forbid microphone access on plain HTTP (`Insecure Context`).
   - Web Speech API requires an active user gesture (`click`/`tap`) to initiate and cannot start silently on boot.
3. **Hardcoded / Simulated Screens:**
   - **LiveMap (`LiveMap.jsx`):** Purely decorative CSS mock. Does not integrate Leaflet, Google Maps SDK, Mapbox, or live GPS tracking.
   - **Mesh Network (`Mesh.jsx`):** Purely decorative setInterval animation. There is no actual BLE (Bluetooth Low Energy) advertising, scanning, or Web Bluetooth mesh relay protocol implemented.
   - **History (`History.jsx`):** Hardcoded static array. Events are not persisted to a database or localStorage when an actual SOS occurs.
   - **Auth (`Auth.jsx`):** Mock login screen. Clicking "Sign In", "Google", or "Biometrics" simply flips `isAuthenticated = true` without validation or JWT tokens.
4. **Twilio SMS Template Restriction:**
   - As observed in console logs during tests, Twilio trial accounts reject custom SMS bodies with: `Invalid template name. Trial accounts can only use predefined SMS templates.`

---

## 16. Which Functionality Can Be Reused in an Android Application

If building an Android application (either via React Native/Capacitor or Native Kotlin/Java):

1. **Backend Integration (`/api/emergency`):**
   - The entire Express API structure, request payload contract (`contacts`, `reason`, `location`, `userName`), Twilio dispatch loop, and phone number normalization can be reused as-is.
2. **Emergency Payload Schema:**
   - Data models for Emergency Contacts (`name`, `phone`, `relation`, `id`) and GPS location object (`latitude`, `longitude`).
3. **UI/UX Layouts and Styling Assets:**
   - Complete CSS design tokens (`#ff4d4d`, `#22c55e`, `#3b82f6`, glassmorphic styling, pulse ring keyframe animations, countdown overlay logic).
4. **Emergency Verification Workflow:**
   - The 10-second countdown cancellation logic, instant dispatch bypass, and post-dispatch delivery report screen.
5. **Detection Algorithm Logic:**
   - The 3-state fall detection logic (High Impact $> 25\,\text{m/s}^2$ -> Wait 1s -> Verify Stillness between $8\text{--}11\,\text{m/s}^2$ for 2s) can be directly translated to Kotlin/Java or Native Sensor Event listeners.
   - Keyword list for Indian distress context (`help`, `bachao`, `madad`, `emergency`).

---

## 17. Which Functionality Must Be Rewritten Using Native Android APIs

To function as a true, production-grade safety application on mobile:

1. **Background Service & Sensor Monitoring:**
   - **Must use:** Android **Foreground Service** (`android.app.Service`) with a persistent notification.
   - **Must use:** Native `SensorManager` and `SensorEventListener` for `TYPE_ACCELEROMETER` / `TYPE_LINEAR_ACCELERATION` to detect falls while the screen is locked and app is in background.
   - **Must use:** Android `WakeLock` / `PartialWakeLock` to prevent the CPU from sleeping during safety monitoring.
2. **Native Voice & Audio Recognition:**
   - **Must use:** Android `AudioRecord` or on-device speech recognizer / Vosk / TensorFlow Lite keyword spotting (`SpeechRecognizer` API with offline language pack) to run without needing an open browser window.
3. **Hardware Fallback SOS (Direct Device SMS/Call):**
   - **Must use:** Android `SmsManager` (`sendTextMessage`) with `SEND_SMS` permission so the phone can send direct SMS even when internet data / WiFi is unavailable (crucial for safety).
   - **Must use:** `Intent(Intent.ACTION_CALL)` with `CALL_PHONE` permission to dial directly.
4. **Native Geolocation:**
   - **Must use:** Google Play Services `FusedLocationProviderClient` with `ACCESS_FINE_LOCATION` and `ACCESS_BACKGROUND_LOCATION` to get real GPS fixes in background.
5. **Real Offline Bluetooth Low Energy (BLE) Mesh:**
   - **Must use:** Android `BluetoothLeScanner` and `BluetoothLeAdvertiser` (`android.bluetooth.le`) or an established BLE Mesh library (e.g. Nordic nRF Mesh or Nearby Connections API) to actually relay emergency packets device-to-device without internet.
6. **Device Power Management & Autostart:**
   - **Must use:** `RECEIVE_BOOT_COMPLETED` broadcast receiver and Battery Optimization Exemption (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) so the app isn't killed by OEM battery savers.

---

## 18. Which Functionality is Currently NOT Implemented

1. **Real Bluetooth Low Energy (BLE) Mesh Communication:**
   - The current `Mesh.jsx` is purely visual CSS/SVG animation. No BLE advertising, scanning, or peer-to-peer packet transfer exists.
2. **Real Interactive Maps & Routing:**
   - `LiveMap.jsx` is a static grid illustration. There is no Mapbox/Google Maps/Leaflet map rendering, no real-time route calculation, and no safe-haven geocoding.
3. **Database & Persistent SOS History:**
   - SOS dispatches are not saved to a database (MongoDB, SQLite, or Room). The History screen shows hardcoded dummy entries.
4. **User Authentication & Cloud Sync:**
   - Auth screen does not connect to Firebase, Supabase, or any database. Contacts saved on one phone do not sync to a cloud profile.
5. **Device Hardware Permissions / Background Wake:**
   - Cannot run when phone screen is locked or browser is minimized.

---

## Summary Classification

### A. CURRENTLY WORKING
- **Emergency Verification System:** 10-second countdown with immediate "Send Now" and "Cancel SOS" controls.
- **GPS Coordinates Acquisition:** Dual-tier fallback (`enableHighAccuracy: true` -> `false`) via Geolocation API, generating clickable Google Maps links.
- **Emergency Contacts CRUD:** Adding, deleting, and storing contacts in browser `localStorage`.
- **Backend Dispatch Service (`/api/emergency`):** Express REST endpoint parsing payloads, handling contacts, formatting SMS and TwiML, and connecting to Twilio.
- **Twilio Integration (Backend):** Functional Twilio SDK client sending live SMS and voice calls (subject to Twilio account verification rules).
- **Fall Detection (Active Browser Session):** Live accelerometer magnitude calculations with 3-phase impact-and-stillness state machine.
- **Acoustic / Distress Volume Detection:** Web Audio API AnalyserNode detecting high-volume scream/spike events.

### B. PARTIALLY WORKING
- **Voice Keyword Distress Detection:** Works via Web Speech API in desktop Chrome and supported Android browsers over HTTPS, but fails on HTTP and cannot run in the background.
- **Twilio SMS / Voice Calls on Trial Account:** Fails when recipient numbers are not pre-verified or when SMS templates don't match Twilio trial rules.

### C. NOT IMPLEMENTED
- **Real BLE Mesh Network:** (Mesh screen is purely mock UI animation).
- **Interactive Live Map & Real Safe Zones:** (LiveMap screen is static CSS mock).
- **Real User Authentication & Session Management:** (Auth screen is mock UI).
- **Persistent SOS Event History:** (History screen is hardcoded array).
- **Cloud Database:** (No backend database; state is local only).

### D. MUST BE REBUILT FOR NATIVE ANDROID
- **Continuous 24/7 Sensor Monitoring:** Android Foreground Service with `SensorEventListener`.
- **Background Wake & Lock:** `WAKE_LOCK` and Battery Optimization exemption.
- **Direct Cellular SMS & Calling:** Native `SmsManager` and `TelecomManager` for offline SOS.
- **Native Offline Voice Recognition:** Android SpeechRecognizer or on-device keyword model (Vosk/TFLite).
- **Background GPS Tracking:** Google Play Services `FusedLocationProviderClient`.
- **Real Device-to-Device Mesh:** Android BLE Advertising/Scanning (`BluetoothLeAdvertiser`) or Google Nearby Connections API.

### E. CAN BE REUSED FROM EXISTING BACKEND
- **`server.js` Core Architecture:** Express 5 app, CORS, environment configuration.
- **`POST /api/emergency` Endpoint:** Payload format, contact phone number normalization (`+91`), validation logic.
- **Twilio Dispatch Engine:** SMS template message builder, TwiML voice alert generator, dispatch status response JSON.
- **Health Check Endpoint (`GET /api/health`):** Diagnostic service.
- **All Data Contracts & JSON Schemas:** Emergency contacts, location objects, dispatch result structures.
