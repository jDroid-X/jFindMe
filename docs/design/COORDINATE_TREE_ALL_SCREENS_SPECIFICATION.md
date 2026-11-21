# 🌳 Master Coordinate Tree Architecture Specification (ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)
## Enterprise Dual-Ecosystem Blueprint: 32 Android Native Screens & Web Mission Control
> **System:** `jFindMe` / `jButterFly` Enterprise Ecosystem  
> **Codename:** `jDroid-X` | **Operator:** `JEET` | **Date:** 2026-11-02T07:45:00+05:30  
> **Standard:** Antigravity AI Enterprise Development Standard (V5.0)  
> **Architecture Pattern:** Strict OOPS MVC / Coordinate Tree (`ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB`)  
> **Completeness Level:** Exhaustive Forensic Parity — Every Layout Element, Affordance, Card, Input & Metric  

---

## 📑 Table of Contents

1. [Architectural Axioms & Coordinate Tree Methodology](#1-architectural-axioms--coordinate-tree-methodology)
2. [Section A: 32 Android Native Screens Specification (Exhaustive Element Tree)](#section-a-32-android-native-screens-specification)
   - [Domain 1: Boot, Identity & Session Governance (Screens 1 to 4)](#domain-1-boot-identity--session-governance)
   - [Domain 2: Real-Time Tactical Telemetry, Navigation & Radar (Screens 5 to 8)](#domain-2-real-time-tactical-telemetry-navigation--radar)
   - [Domain 3: Core Diagnostics, Hardware Vitals & Closed-Loop Engine (Screens 9 to 12)](#domain-3-core-diagnostics-hardware-vitals--closed-loop-engine)
   - [Domain 4: Breadcrumb Chronology, Geofence & Emergency Beacons (Screens 13 to 16)](#domain-4-breadcrumb-chronology-geofence--emergency-beacons)
   - [Domain 5: P2P Mesh, Circles & Enterprise Hierarchy (Screens 17 to 20)](#domain-5-p2p-mesh-circles--enterprise-hierarchy)
   - [Domain 6: Storage, Security, OTA & Design System (Screens 21 to 32)](#domain-6-storage-security-ota--design-system)
3. [Section B: Web Mission Control Screens Specification (11 Views)](#section-b-web-mission-control-screens-specification)
4. [Section C: Android Native App Visual Mindmap (32 Screens)](#section-c-android-native-app-visual-mindmap)
5. [Section D: Web Mission Control Visual Mindmap](#section-d-web-mission-control-visual-mindmap)
6. [Section E: Android App Graphify Interaction & Dataflow Architecture](#section-e-android-app-graphify-interaction--dataflow-architecture)
7. [Section F: Web Mission Control Graphify Architecture](#section-f-web-mission-control-graphify-architecture)

---

## 1. Architectural Axioms & Coordinate Tree Methodology

The **Coordinate Tree** (`ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB`) represents the rigorous mathematical and functional mapping connecting human operator affordances down to byte-level SQLite V80 tables and Firebase Cloud SSOT nodes:

$$	ext{ROOT} \supset 	ext{BR (Business Rules)} \supset 	ext{EL (UI Elements)} \supset 	ext{FE (Client Behaviors)} \supset 	ext{BE (Engine Logics)} \supset 	ext{DB (Physical Storage)}$$

### Coordinate Notation Semantics:
- **`ROOT`**: Application / Activity Master Execution Scope and Lifecycle Boundary.
- **`[BR]` (Business Rule / Domain Policy)**: Architectural invariant, enterprise security policy, and operational constraints.
- **`[EL]` (Element Affordance)**: Physical screen region, interactive card, input field, switch, floating button, or dialog component.
- **`[FE]` (Frontend Component & UI Validation)**: Reactive UI widget, layout geometry, click/change listener, client validation rules (`FR₁ ... FRₙ`), and deterministic Pass/Fail test scenarios.
- **`[BE]` (Backend Service & Engine Logic)**: Thread-isolated controller, background worker, cryptographic verifier, IPC binder, engine validation rules (`BFR₁ ... BFRₙ`), and deterministic Pass/Fail test logic.
- **`[DB]` (Database SSOT & Persistence)**: Physical SQLite V80 table, column, index, foreign key, `system_registry.xml` key, or Firebase Realtime Database path.

---

## Section A: 32 Android Native Screens Specification


## Section A: 32 Android Native Screens Specification

### Domain 1: Boot, Identity & Session Governance

#### 1. `SplashActivity` — Boot Verification, Security Attestation & Navigation Router
```text
ROOT: SPLASH_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.base.SplashActivity)
│
├── 💼 [BR]₁: Boot Security, Silicon Attestation & Environment Integrity
│    ├── 🖱️ [EL]₁․₁: Animated Splash Butterfly Emblem Canvas
│    │    ├── 🖥️ [FE]₁․₁․₁: Vector Butterfly Logo with Scaling & Alpha Interpolation (180dp to 240dp)
│    │    │    ├── (FR1.1): Scales dynamically using AccelerateDecelerateInterpolator over 1200ms; enforces 60 FPS.
│    │    │    │    ├── Pass Scenario: Display DPI = 420 (xhdpi) -> Emblem renders at 210dp; frame rate >= 58 FPS.
│    │    │    │    └── Fail Scenario: Canvas dimension zero or DPI <= 120 -> Clamps to 180dp safe default.
│    │    │    └── ⚙️ [BE]₁․₁․₁․₁: RenderThread Hardware Acceleration Watcher
│    ├── 🖱️ [EL]₁․₂: Application Title & Enterprise Brand Label
│    │    ├── 🖥️ [FE]₁․₂․₁: High-Contrast Brand Header ("jFindMe Enterprise", Typeface.BOLD, 28sp)
│    │    │    └── ⚙️ [BE]₁․₂․₁․₁: UiArchitect Typography Matrix Provider
│    ├── 🖱️ [EL]₁․₃: Subtitle & Operational Identity Banner
│    │    ├── 🖥️ [FE]₁․₃․₁: Subtitle Text ("Secure Tactical Telemetry & Mesh Network", alpha 0.85f, 13sp)
│    │    │    └── ⚙️ [BE]₁․₃․₁․₁: Localization Resource Loader (`R.string.splash_subtitle`)
│    ├── 🖱️ [EL]₁․₄: Boot Status Text & Dynamic Step Indicator
│    │    ├── 🖥️ [FE]₁․₄․₁: Live Text View ("Attesting Silicon Root...", "Verifying SQLite V80 Schema...")
│    │    │    ├── (FR1.2): Updates synchronously with background initialization sequence steps.
│    │    │    └── ⚙️ [BE]₁․₄․₁․₁: BootSequenceProgressObservable
│    ├── 🖱️ [EL]₁․₅: Root & Tamper Attestation Guard
│    │    ├── 🖥️ [FE]₁․₅․₁: Hardware Security Micro-Badge (Green Shield = Untampered, Red Skull = Rooted)
│    │    │    └── ⚙️ [BE]₁․₅․₁․₁: SecurityReport.evaluateRootStatus Engine
│    │    │         ├── (BFR1.1): Scans /system/bin/su, /system/xbin/su, /sbin/su, and Magisk socket mounts.
│    │    │         │    ├── Pass Scenario: Zero su binaries and build tags == "release-keys" -> isRooted = false.
│    │    │         │    └── Fail Scenario: /system/xbin/su detected -> isRooted = true; flags security event to SQLite.
│    │    │         └── 🗄️ [DB]₁․₅․₁․₁․₁: `device_vitals.is_rooted` (INTEGER NOT NULL DEFAULT 0)
│    ├── 🖱️ [EL]₁․₆: Mock Location Provider Integrity Guard
│    │    ├── 🖥️ [FE]₁․₆․₁: Mock GPS Warning Badge
│    │    │    └── ⚙️ [BE]₁․₆․₁․₁: LocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) & Test Provider Check
│    │    │         ├── (BFR1.2): Inspects Settings.Secure.ALLOW_MOCK_LOCATION and mock app packages.
│    │    │         │    ├── Pass Scenario: Test provider list empty -> isMockGps = false.
│    │    │         │    └── Fail Scenario: Mock provider detected -> isMockGps = true; marks telemetry payload TAINTED.
│    │    │         └── 🗄️ [DB]₁․₆․₁․₁․₁: `device_vitals.is_mock_gps` (INTEGER NOT NULL DEFAULT 0)
│    ├── 🖱️ [EL]₁․₇: Version Code & Release Signature Footer
│    │    ├── 🖥️ [FE]₁․₇․₁: Monospace Footer Label ("v3.1.0 (Build 76) • SHA-256: 8A2F...C4")
│    │    │    └── ⚙️ [BE]₁․₇․₁․₁: BuildConfig Version Provider & SignatureVerifier
│    └── 🖱️ [EL]₁․₈: Dispatch Router & Navigation Transition Intent
│         ├── 🖥️ [FE]₁․₈․₁: Intent Transition Controller
│         │    ├── (FR1.3): If auth session valid & onboarding complete -> Route to MapActivity; else Route to AuthActivity.
│         │    │    ├── Pass Scenario: FirebaseAuth.currentUser != null & hasCompletedOnboarding -> Intent(MapActivity).
│         │    │    └── Fail Scenario: FirebaseAuth.currentUser == null -> Intent(AuthActivity) dispatched after 1200ms.
│         │    └── ⚙️ [BE]₁․₈․₁․₁: Session Cryptographic Token & Local Registry Validator
│         │         ├── 🗄️ [DB]₁․₈․₁․₁․₁: `user_identity.uid_1` (TEXT PRIMARY KEY) & `user_identity.token_hash` (TEXT)
│         │         └── 🗄️ [DB]₁․₈․₁․₁․₂: `system_registry.xml` (`auth_session_active`, `agent_id`)
```

---

#### 2. `AuthActivity` — Operator Authentication, QR Hardware Pairing & Account Setup
```text
ROOT: AUTH_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.auth.AuthActivity)
│
├── 💼 [BR]₁: Operator Provisioning, Credential Verification & Node Pairing
│    ├── 🖱️ [EL]₂․₁: Header Branding Card & Shield Icon
│    │    ├── 🖥️ [FE]₂․₁․₁: Gradient Hero Header (Title "OPERATOR AUTHENTICATION", Subtitle "Cryptographic Identity Node")
│    │    │    └── ⚙️ [BE]₂․₁․₁․₁: BaseActivityJava.applyGlobalHeaderGeometry
│    ├── 🖱️ [EL]₂․₂: Authentication Mode Segmented Tabs
│    │    ├── 🖥️ [FE]₂․₂․₁: Tab Switcher: [Email Sign In], [QR Code Hardware Pairing], [Demo Session]
│    │    │    ├── (FR2.1): Smooth cross-fade between email input form and camera QR viewfinder within 200ms.
│    │    │    └── ⚙️ [BE]₂․₂․₁․₁: ViewPager2 / ViewFlipper Mode Controller
│    ├── 🖱️ [EL]₂․₃: Email Input Field & Real-Time Micro-Badge
│    │    ├── 🖥️ [FE]₂․₃․₁: Outlined Material EditText with Floating Hint and Instant Regex Validator
│    │    │    ├── (FR2.2): Must match RFC 5322 regex; trim whitespace; show green micro-badge upon valid domain syntax.
│    │    │    │    ├── Pass Scenario: "operator@enterprise.com" -> Valid badge green; error micro-badge hidden.
│    │    │    │    └── Fail Scenario: "operator@invalid" -> Inline error "Invalid email domain format"; disable submit.
│    │    │    ├── ⚙️ [BE]₂․₃․₁․₁: Email Normalization & Disposable Domain Filter
│    │    │    │    ├── (BFR2.1): Normalizes domain to lowercase; rejects disposable email providers.
│    │    │    │    └── 🗄️ [DB]₂․₃․₁․₁․₁: `user_profile.email` (TEXT UNIQUE)
│    │    │    └── ⚙️ [BE]₂․₃․₁․₂: Firebase Authentication Bridge (signInWithEmailAndPassword)
│    │    │         └── 🗄️ [DB]₂․₃․₁․₂․₁: Cloud Firebase `/users/{auth_uid}/credentials`
│    ├── 🖱️ [EL]₂․₄: Password Input Field & Visibility Toggle
│    │    ├── 🖥️ [FE]₂․₄․₁: Masked Password Field with Eye Visibility Icon Toggle
│    │    │    ├── (FR2.3): Min 8 characters, min 1 uppercase, min 1 numeric, min 1 special symbol.
│    │    │    └── ⚙️ [BE]₂․₄․₁․₁: Argon2id Client-Side Key Derivation Service
│    │    │         └── 🗄️ [DB]₂․₄․₁․₁․₁: Encrypted Keystore Storage `user_identity.auth_token`
│    ├── 🖱️ [EL]₂․₅: Password Complexity Strength Meter
│    │    ├── 🖥️ [FE]₂․₅․₁: 4-Segment Dynamic Progress Bar (Red: Weak, Orange: Fair, Blue: Good, Emerald: Secure)
│    │    │    └── ⚙️ [BE]₂․₅․₁․₁: ZxcvbnPasswordStrengthEvaluator
│    ├── 🖱️ [EL]₂․₆: "Remember Credentials" Checkbox Switch
│    │    ├── 🖥️ [FE]₂․₆․₁: Checkbox Tile ("Persist Authenticated Session on Silicon Node")
│    │    │    └── ⚙️ [BE]₂․₆․₁․₁: SharedPreferences Session Persistence Manager
│    │    │         └── 🗄️ [DB]₂․₆․₁․₁․₁: `system_registry.xml` (`pref_remember_me`)
│    ├── 🖱️ [EL]₂․₇: "Forgot Password" Action Link Button
│    │    ├── 🖥️ [FE]₂․₇․₁: Text Button launching Password Reset Dialog
│    │    │    └── ⚙️ [BE]₂․₇․₁․₁: FirebaseAuth.sendPasswordResetEmail Dispatcher
│    ├── 🖱️ [EL]₂․₈: Primary Sign In Action Button
│    │    ├── 🖥️ [FE]₂․₈․₁: Material Elevated Button (`btn`) with Embedded Progress Spinner
│    │    │    ├── (FR2.4): Disables on submit to prevent duplicate auth requests; displays spinner during network handshake.
│    │    │    └── ⚙️ [BE]₂․₈․₁․₁: AuthViewModel.authenticateOperator Dispatcher
│    ├── 🖱️ [EL]₂․₉: Hardware QR Code Camera Viewfinder Card
│    │    ├── 🖥️ [FE]₂․₉․₁: ZXing BarcodeView with Laser Aim Reticle
│    │    │    ├── (FR2.5): Decodes base64 QR payload; validates CRC32 checksum; extracts hardware Agent ID and invite nonce.
│    │    │    │    ├── Pass Scenario: Payload "JFND:v1:HW_9A2F4B:NONCE_88123" -> Auto-populates pairing review dialog.
│    │    │    │    └── Fail Scenario: Corrupted QR or expired nonce -> Alerts "Invalid or expired node pairing code".
│    │    │    └── ⚙️ [BE]₂․₉․₁․₁: Handshake Verification & Pairing Controller
│    │    │         ├── 🗄️ [DB]₂․₉․₁․₁․₁: `device_pairing` (device_a_id, device_b_id, status)
│    │    │         └── 🗄️ [DB]₂․₉․₁․₁․₂: Cloud Firebase `/devices/{agentId}/handshake`
│    ├── 🖱️ [EL]₂․₁₀: Pairing Nonce Manual Input Field & Paste Action
│    │    ├── 🖥️ [FE]₂․₁₀․₁: Outlined Monospace Field with "Paste Clipboard" Icon Button
│    │    │    └── ⚙️ [BE]₂․₁₀․₁․₁: ClipboardManager String Extractor
│    └── 🖱️ [EL]₂․₁₁: Error Micro-Badge & Alert Overlay Banner
│         ├── 🖥️ [FE]₂․₁₁․₁: Slide-Down Crimson Alert Banner for Network Timeouts / Invalid Credentials
│         │    └── ⚙️ [BE]₂․₁₁․₁․₁: AuthErrorMapper (Translates FirebaseAuthException into user-friendly diagnostic text)
```

---

#### 3. `ProfileActivity` — Operator Identity, Silicon Key & QR Code Exporter
```text
ROOT: PROFILE_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.settings.ProfileActivity)
│
├── 💼 [BR]₁: Operator Identity Management, Biometric Binding & Node Presentation
│    ├── 🖱️ [EL]₃․₁: Global Header Bar & Navigation Back Button
│    │    ├── 🖥️ [FE]₃․₁․₁: Back Button (`backBtn`), Title "Identity & Integrity Hub", Subtitle
│    │    │    └── ⚙️ [BE]₃․₁․₁․₁: BaseActivityJava Layout Header Geometry
│    ├── 🖱️ [EL]₃․₂: Header Avatar Card & Camera Edit Overlay
│    │    ├── 🖥️ [FE]₃․₂․₁: Circular Avatar Frame (`lensCard`) with Camera Icon Badge (`icon`)
│    │    │    ├── (FR3.1): Tapping camera icon launches bottom sheet: [Choose from Gallery], [Capture Camera], [Vector Editor].
│    │    │    └── ⚙️ [BE]₃․₂․₁․₁: AvatarSourceSelector & ImageIntentLauncher
│    │    │         └── 🗄️ [DB]₃․₂․₁․₁․₁: `user_profile.avatar_local_uri`
│    ├── 🖱️ [EL]₃․₃: Section 1 Header: `SYSTEM INTEGRITY & HARDWARE LINKS`
│    │    ├── 🖥️ [FE]₃․₃․₁: Category Section Divider Header
│    │    │    └── ⚙️ [BE]₃․₃․₁․₁: BaseActivityJava.createSectionHeader
│    ├── 🖱️ [EL]₃․₄: Permanent Silicon Hardware Agent ID Tile
│    │    ├── 🖥️ [FE]₃․₄․₁: Monospace ID Text ("HW_2T929153") with Copy Button (`row`, `iv`)
│    │    │    ├── (FR3.2): Read-only silicon anchor; clicking copy places string in clipboard and displays toast.
│    │    │    └── ⚙️ [BE]₃․₄․₁․₁: DeviceIdentityHelper.getHardwareUid
│    │    │         └── 🗄️ [DB]₃․₄․₁․₁․₁: `user_identity.uid_1` & `system_registry.xml` (`agent_id`)
│    ├── 🖱️ [EL]₃․₅: Android Keystore Alias Tile & Key Hash
│    │    ├── 🖥️ [FE]₃․₅․₁: Key Alias Display ("alias/jfindme_master_key • RSA-4096")
│    │    │    └── ⚙️ [BE]₃․₅․₁․₁: KeyStoreManager.getCertificateDetails
│    ├── 🖱️ [EL]₃․₆: Hardware Serial & Dual-SIM IMEI Display Tile
│    │    ├── 🖥️ [FE]₃․₆․₁: Masked IMEI & Hardware Serial Badge
│    │    │    └── ⚙️ [BE]₃․₆․₁․₁: DeviceIdentityHelper.getHardwareSerial
│    │    │         └── 🗄️ [DB]₃․₆․₁․₁․₁: `user_identity.hardware_serial`
│    ├── 🖱️ [EL]₃․₇: Hardware Pairing QR Code Generator Card
│    │    ├── 🖥️ [FE]₃․₇․₁: "Generate Pairing QR Code" Tile launching high-res 512x512 QR dialog
│    │    │    └── ⚙️ [BE]₃․₇․₁․₁: ZXing QRCodeWriter & Cryptographic Payload Signer
│    ├── 🖱️ [EL]₃․₈: Section 2 Header: `PERSONAL IDENTITY & SECURITY`
│    │    └── 🖥️ [FE]₃․₈․₁: Section Divider Title
│    ├── 🖱️ [EL]₃․₉: Operator Full Name Field
│    │    ├── 🖥️ [FE]₃․₉․₁: Outlined Material EditText with character counter (2-50 chars)
│    │    │    └── ⚙️ [BE]₃․₉․₁․₁: NameFormatValidator
│    │    │         └── 🗄️ [DB]₃․₉․₁․₁․₁: `user_profile.name`
│    ├── 🖱️ [EL]₃․₁₀: Registered Phone Number Field
│    │    ├── 🖥️ [FE]₃․₁₀․₁: International Phone Input with Country Calling Code (+91 / +1)
│    │    │    └── ⚙️ [BE]₃․₁₀․₁․₁: PhoneNumberUtil.isValidNumber
│    │    │         └── 🗄️ [DB]₃․₁₀․₁․₁․₁: `user_profile.phone`
│    ├── 🖱️ [EL]₃․₁₁: Date of Birth Picker Field
│    │    ├── 🖥️ [FE]₃․₁₁․₁: Read-only EditText with Calendar Icon (`dobField`) launching DatePickerDialog
│    │    │    └── ⚙️ [BE]₃․₁₁․₁․₁: DatePickerFragment Controller
│    │    │         └── 🗄️ [DB]₃․₁₁․₁․₁․₁: `user_profile.dob`
│    ├── 🖱️ [EL]₃․₁₂: Gender Selector Dropdown
│    │    ├── 🖥️ [FE]₃․₁₂․₁: Material Exposed Dropdown Menu (Male, Female, Other, Prefer Not to Say)
│    │    │    └── ⚙️ [BE]₃․₁₂․₁․₁: GenderEnumMapper
│    │    │         └── 🗄️ [DB]₃․₁₂․₁․₁․₁: `user_profile.gender`
│    ├── 🖱️ [EL]₃․₁₃: State & City Dynamic Spinners
│    │    ├── 🖥️ [FE]₃․₁₃․₁: State Spinner (`stateSpinner`) and Dependent City Spinner (`citySpinner`)
│    │    │    ├── (FR3.3): Changing State dynamically repopulates City adapter with matching geographic nodes.
│    │    │    └── ⚙️ [BE]₃․₁₃․₁․₁: GeographicLookupRepository (`res/raw/cities_by_state.json`)
│    │    │         ├── 🗄️ [DB]₃․₁₃․₁․₁․₁: `user_profile.state`
│    │    │         └── 🗄️ [DB]₃․₁₃․₁․₁․₂: `user_profile.city`
│    ├── 🖱️ [EL]₃․₁₄: Pincode / Postal Code Input Field
│    │    ├── 🖥️ [FE]₃․₁₄․₁: Numeric Input Field (6-digit mask with auto-geocode lookup)
│    │    │    └── ⚙️ [BE]₃․₁₄․₁․₁: PincodeValidator
│    │    │         └── 🗄️ [DB]₃․₁₄․₁․₁․₁: `user_profile.pincode`
│    ├── 🖱️ [EL]₃․₁₅: Section 3 Header: `SYSTEM ACTIONS & SECURITY`
│    │    └── 🖥️ [FE]₃․₁₅․₁: Section Divider
│    ├── 🖱️ [EL]₃․₁₆: "Save Profile Changes" Primary Tile
│    │    ├── 🖥️ [FE]₃․₁₆․₁: Elevated Tile (`customTile`) with Save Icon
│    │    │    ├── (FR3.4): Atomically commits changes to SQLite `user_profile` and Firebase `/devices/{agentId}/profile`.
│    │    │    └── ⚙️ [BE]₃․₁₆․₁․₁: ProfileViewModel.saveProfile & FirebaseComm.updateProfileAtomic
│    │    │         ├── 🗄️ [DB]₃․₁₆․₁․₁․₁: `user_profile` (all columns updated)
│    │    │         └── 🗄️ [DB]₃․₁₆․₁․₁․₂: Cloud Firebase `/devices/{agentId}/profile`
│    └── 🖱️ [EL]₃․₁₇: "Delete Account / De-provision Node" Danger Action
│         ├── 🖥️ [FE]₃․₁₇․₁: Crimson Outline Button (`deleteBtn`) launching FactoryResetActivity
│         │    └── ⚙️ [BE]₃․₁₇․₁․₁: Intent Launcher for FactoryResetActivity
```

---

#### 4. `AvatarEditorActivity` — Identity Shield Vector Avatar Designer
```text
ROOT: AVATAR_EDITOR_MASTER_PLAN (com.jdroidx.jfindme.ui.settings.AvatarEditorActivity)
│
├── 💼 [BR]₁: Identity Shield Vector Avatar Customization & Mesh Propagation
│    ├── 🖱️ [EL]₄․₁: Global Header Bar & Back Button
│    │    ├── 🖥️ [FE]₄․₁․₁: Back Button (`backBtn`), Title "Identity Mirror & Genetic Blueprint Engine"
│    │    │    └── ⚙️ [BE]₄․₁․₁․₁: BaseActivityJava Layout Header Geometry
│    ├── 🖱️ [EL]₄․₂: Live Vector Butterfly Emblem Viewport Canvas
│    │    ├── 🖥️ [FE]₄․₂․₁: High-Performance Canvas rendering layered SVG vector paths (Wings, Antennae, Core)
│    │    │    ├── (FR4.1): Real-time redraw within 16ms upon color palette selection; maintains 60 FPS.
│    │    │    └── ⚙️ [BE]₄․₂․₁․₁: VectorAvatarRenderEngine
│    │    │         └── 🗄️ [DB]₄․₂․₁․₁․₁: Memory Cache `avatar_preview_bitmap`
│    ├── 🖱️ [EL]₄․₃: Layer Tab Switcher Header
│    │    ├── 🖥️ [FE]₄․₃․₁: Horizontal Tabs (`tab`): [Wing Accents], [Antennae Core], [Aura Glow], [Motion FX]
│    │    │    └── ⚙️ [BE]₄․₃․₁․₁: LayerTabSelectionController
│    ├── 🖱️ [EL]₄․₄: Enterprise Color Swatch Grid
│    │    ├── 🖥️ [FE]₄․₄․₁: Grid of 12 Color Swatches (`chip`, `row`: Emerald, Cyan, Gold, Crimson, Violet, Cobalt)
│    │    │    ├── (FR4.2): Active color chip shows animated white checkmark; updates wing tint immediately.
│    │    │    └── ⚙️ [BE]₄․₄․₁․₁: ColorPaletteManager
│    │    │         └── 🗄️ [DB]₄․₄․₁․₁․₁: `user_profile.avatar_color_hex`
│    ├── 🖱️ [EL]₄․₅: Custom Hex Color Input Field
│    │    ├── 🖥️ [FE]₄․₅․₁: Text Field with "#" Prefix and Live Color Square Preview
│    │    │    └── ⚙️ [BE]₄․₅․₁․₁: HexColorParser & Sanitizer
│    ├── 🖱️ [EL]₄․₆: Wing Texture / Pattern Style Cards
│    │    ├── 🖥️ [FE]₄․₆․₁: Selection Cards (`card`): [Solid Neon], [Radial Gradient], [Circuit Trace], [Tactical Hex]
│    │    │    └── ⚙️ [BE]₄․₆․₁․₁: ShaderMaskGenerator (BitmapShader / LinearGradient)
│    ├── 🖱️ [EL]₄․₇: Motion Animation State Toggle Button
│    │    ├── 🖥️ [FE]₄․₇․₁: Action Button (`animBtn`) cycling: [Static 0Hz], [Gentle Flutter 1Hz], [Alert Pulse 3Hz]
│    │    │    └── ⚙️ [BE]₄․₇․₁․₁: ValueAnimator Loop Controller
│    ├── 🖱️ [EL]₄․₈: Reset to Default Theme Button
│    │    ├── 🖥️ [FE]₄․₈․₁: Ghost Button (`resetBtn`) reverting colors to default Cyber Cyan (`#00E5FF`)
│    │    │    └── ⚙️ [BE]₄․₈․₁․₁: DefaultThemeProfileLoader
│    └── 🖱️ [EL]₄․₉: Save & Fleet Identity Shield Propagation Action
│         ├── 🖥️ [FE]₄․₉․₁: Floating Action Button (`applyBtn`) "Apply Avatar Across Fleet"
│         │    ├── (FR4.3): Enforces Identity Shield mandate: updates avatar across network without overwriting name/phone.
│         │    └── ⚙️ [BE]₄․₉․₁․₁: FirebaseComm.updateProfileAtomic Avatar Synchronizer
│         │         ├── 🗄️ [DB]₄․₉․₁․₁․₁: `user_profile.avatar_id` & `user_profile.avatar_color_hex`
│         │         └── 🗄️ [DB]₄․₉․₁․₁․₂: Cloud Firebase `/devices/{agentId}/profile/avatar_id`
```

---

### Domain 2: Real-Time Tactical Telemetry, Navigation & Radar

#### 5. `MapActivity` — Primary Tactical Map, Real-Time Fleet Telemetry & SOS Beacon
```text
ROOT: APPLICATION MASTER PLAN (com.jdroidx.jfindme.ui.map.MapActivity)
│
├── 💼 [BR]₁: Host Device Governance & Self-Affordances (Clicked on Host Device)
│    ├── 🖱️ [EL]₁․₁: Host Map Avatar & Marker Affordance
│    │    ├── 🖥️ [FE]₁․₁․₁: Host Marker Single-Tap Trigger (`showSelfActionSheet`)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₁․₁․₁․₁: Target ID matches Host Firebase UID or local `DeviceIdentityHelper.getOrGenerateAgentId()`.
│    │    │    │    └── FR₁․₁․₁․₂: Bottom action sheet displays current privacy tier status pill (`🟢 PRECISE`, `🟡 FUZZY`, `🔵 FROZEN`).
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Tapping host marker opens `👑 YOU (HOST DEVICE)` rich bottom sheet in < 100ms.
│    │    │    │    └── Fail Scenario: If host coordinates null/unacquired, falls back to stored last-known location or displays acquiring toast.
│    │    │    └── ⚙️ [BE]₁․₁․₁․₁: Self-Profile & Action Sheet Controller (`MapActivity.showSelfActionSheet`)
│    │    │         ├── BFR₁․₁․₁․₁: Evaluates `PrivacyEngine.getPrivacyTier(this)` and binds active state string.
│    │    │         │    ├── Logic for Testing Pass Scenario: Returns active tier enum, formats tier label into action sheet header.
│    │    │         │    └── Logic for Testing Fail Scenario: If tier unreadable, defaults safely to `PrivacyTier.PRECISE`.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₁․₁․₁․₁․₁: `/devices/{host_id}/profile` ⟷ SQLite `user_identity` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₁․₁․₁․₁․₁․₂: `device_vitals` Table (battery, charging, lat, lng, bearing)
│    │    │         │    │    └── 🗄️ [Transaction DB]₁․₁․₁․₁․₁․₁․₂: `sys_log` Table (`UI_CLICK_SELF_ACTION_SHEET`)
│    │    ├── 🖥️ [FE]₁․₁․₂: Option 1 — `👤 My Profile & 3D Avatar`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₁․₁․₂․₁: Valid user session and local identity must exist.
│    │    │    │    └── FR₁․₁․₂․₂: Dispatches explicit intent to `ProfileActivity.class`.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Profile editing screen opens with populated alias, 3D avatar preview, and callsign.
│    │    │    │    └── Fail Scenario: Invalid identity redirects to `AuthActivity` with session error flag.
│    │    │    └── ⚙️ [BE]₁․₁․₂․₁: Profile Intent Dispatcher & Identity Resolver
│    │    │         ├── BFR₁․₁․₂․₁: Reads cached operator metadata from `DatabaseHelper.getUserIdentity()`.
│    │    │         │    ├── Logic for Testing Pass Scenario: Record found -> populates alias, bio, avatarId.
│    │    │         │    └── Logic for Testing Fail Scenario: Record missing -> returns empty template with guest avatar.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₁․₁․₂․₁․₁: Firebase `/devices/{host_id}/profile` ⟷ SQLite `users` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₁․₁․₂․₁․₁․₂: `user_profile` Table (`custom_avatar_id`, `status_bio`)
│    │    │         │    │    └── 🗄️ [Transaction DB]₁․₁․₂․₁․₁․₁․₂: `change_log` Table (`PROFILE_VIEW_DISPATCH`)
│    │    ├── 🖥️ [FE]₁․₁․₃: Option 2 — `🔒 Privacy & Ghost Mode [🟢 PRECISE / 🟡 FUZZY / 🔵 FROZEN]`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₁․₁․₃․₁: Must present modal selector dialog with 3 radio tiers: Precise (0m offset), Fuzzy (~500m pseudo-random Gaussian jitter), Frozen (holds fixed last-known coordinate).
│    │    │    │    └── FR₁․₁․₃․₂: Immediate visual badge update on host avatar border upon selection.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Selecting "Fuzzy" offsets live telemetry by ±500m on cloud and marks local state `FUZZY`.
│    │    │    │    └── Fail Scenario: If network fails during cloud sync, caches selection to SQLite and queues in `outbox_queue`.
│    │    │    └── ⚙️ [BE]₁․₁․₃․₁: Privacy Tier Controller & Coordinate Obfuscation Engine (`PrivacyEngine`)
│    │    │         ├── BFR₁․₁․₃․₁: Computes GPS obfuscation transformation and commits to `system_registry.xml`.
│    │    │         │    ├── Logic for Testing Pass Scenario: In fuzzy mode, adds deterministic pseudorandom delta; in frozen mode, suppresses new lat/lng updates.
│    │    │         │    └── Logic for Testing Fail Scenario: Invalid tier enum caught by guard; falls back to precise.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₁․₁․₃․₁․₁: Firebase `/devices/{host_id}/privacy_tier` ⟷ SQLite `system_registry.xml`
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₁․₁․₃․₁․₁․₂: `device_vitals.privacy_tier` Column
│    │    │         │    │    └── 🗄️ [Transaction DB]₁․₁․₃․₁․₁․₁․₂: `audit_log` Table (`PRIVACY_TIER_TRANSITION`)
│    │    ├── 🖥️ [FE]₁․₁․₄: Option 3 — `📍 Recenter Map on My GPS`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₁․₁․₄․₁: Live GNSS fix must be valid (`lastKnownLocation != null` and lat/lng != 0.0).
│    │    │    │    └── FR₁․₁․₄․₂: Animates camera to zoom level 17.5f centered on host coordinates with bearing align.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Camera glides to host position within 450ms using `CameraUpdateFactory.newCameraPosition()`.
│    │    │    │    └── Fail Scenario: If GNSS fix unacquired, triggers `Toast("Acquiring GPS fix...")` and activates high-accuracy listener.
│    │    │    └── ⚙️ [BE]₁․₁․₄․₁: Viewport Recenter Controller (`MapActivity.recenterCamera`)
│    │    │         ├── BFR₁․₁․₄․₁: Queries `FusedLocationProviderClient` for fresh low-latency location payload.
│    │    │         │    ├── Logic for Testing Pass Scenario: FusedLocation returns valid fix -> triggers camera animation.
│    │    │         │    └── Logic for Testing Fail Scenario: GPS disabled -> prompts user to enable location provider.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₁․₁․₄․₁․₁: Firebase `/devices/{host_id}/device/live` ⟷ SQLite `location_event` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₁․₁․₄․₁․₁․₂: `tracking_history` Table (latest host GPS row)
│    │    │         │    │    └── 🗄️ [Transaction DB]₁․₁․₄․₁․₁․₁․₂: `sys_log` Table (`CAMERA_RECENTER_HOST`)
│    │    ├── 🖥️ [FE]₁․₁․₅: Option 4 — `🚨 Emergency SOS Sentinel`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₁․₁․₅․₁: Prompts immediate 5-second countdown dialog with loud auditory tick and cancel button.
│    │    │    │    └── FR₁․₁․₅․₂: Upon expiry, triggers siren and dispatches critical SOS beacon to all guardian channels.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Releasing countdown sends SOS payload to Firebase `/relationships/{group}/signals/sos` and launches `AcousticSirenActivity`.
│    │    │    │    └── Fail Scenario: Tapping cancel within 5000ms aborts broadcast without writing SOS state.
│    │    │    └── ⚙️ [BE]₁․₁․₅․₁: Emergency Distress Dispatcher (`MapActivity.triggerEmergencySosDialog`)
│    │    │         ├── BFR₁․₁․₅․₁: Commits SOS flag to cloud and triggers local `AcousticSirenActivity` strobe/audio.
│    │    │         │    ├── Logic for Testing Pass Scenario: Writes `is_sos_active=1` to RTDB and SQLite `alert_history`.
│    │    │         │    └── Logic for Testing Fail Scenario: Network down -> writes to SQLite `outbox_queue` for instant sync upon reconnect.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₁․₁․₅․₁․₁: Firebase `/relationships/{group}/signals/sos` ⟷ SQLite `alert_history` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₁․₁․₅․₁․₁․₂: `device_vitals.is_sos_active` (1)
│    │    │         │    │    └── 🗄️ [Transaction DB]₁․₁․₅․₁․₁․₁․₂: `outbox_queue` Table (`DISPATCH_SOS_BROADCAST`)
│    │    └── 🖥️ [FE]₁․₁․₆: Option 5 — `📋 Copy My GPS Coordinates`
│    │         ├── Validation & Conditional Rules:
│    │         │    ├── FR₁․₁․₆․₁: `lastKnownLocation` must be non-null.
│    │         │    └── FR₁․₁․₆․₂: Formats string as `%.6f, %.6f` and copies to `ClipboardManager`.
│    │         ├── Testing Scenarios:
│    │         │    ├── Pass Scenario: Clipboard contains `18.578912, 73.876543`; toast notification confirms copy.
│    │         │    └── Fail Scenario: If fix null, toast warns user GPS is acquiring.
│    │         └── ⚙️ [BE]₁․₁․₆․₁: System Clipboard Integrator & Coordinate Formatter
│    │              ├── BFR₁․₁․₆․₁: Extracts double precision latitude/longitude and sets clip data.
│    │              │    ├── Logic for Testing Pass Scenario: Non-null location formatted to 6 decimal places.
│    │              │    └── Logic for Testing Fail Scenario: Null location handled without throwing NullPointerException.
│    │              ├── 🗄️ [Master DB [Online/Offline]]₁․₁․₆․₁․₁: SQLite `location_event` (latest row)
│    │              │    └── 🗄️ [Sub Master/Fact DB]₁․₁․₆․₁․₁․₂: Android `ClipboardManager` buffer
│    │              │    │    └── 🗄️ [Transaction DB]₁․₁․₆․₁․₁․₁․₂: `sys_log` Table (`CLIPBOARD_COPY_GPS`)
│    │
│    └── 🖱️ [EL]₁․₂: Host Quick Action Dock & Self Vitals Widget
│         ├── 🖥️ [FE]₁․₂․₁: Single-Tap Camera Recenter Affordance
│         │    ├── Validation & Conditional Rules:
│         │    │    ├── FR₁․₂․₁․₁: Tapping recenter action or host card executes `recenterCamera()`.
│         │    ├── Testing Scenarios:
│         │    │    ├── Pass Scenario: Map pans smoothly to Host GPS cursor.
│         │    │    └── Fail Scenario: If map camera animating, cancels prior animation and targets host.
│         │    └── ⚙️ [BE]₁․₂․₁․₁: Host Camera Pan Action (`MapActivity.recenterCamera`)
│         │         ├── BFR₁․₂․₁․₁: Animates camera to host position.
│         │         ├── 🗄️ [Master DB [Online/Offline]]₁․₂․₁․₁․₁: SQLite `location_event` Table
│         │         │    └── 🗄️ [Sub Master/Fact DB]₁․₂․₁․₁․₁․₂: `tracking_history` Table
│         │         │    │    └── 🗄️ [Transaction DB]₁․₂․₁․₁․₁․₁․₂: `sys_log` Table (`DASHBOARD_HOST_TAP`)
│         └── 🖥️ [FE]₁․₂․₂: Long-Press Action Sheet Trigger
│              ├── Validation & Conditional Rules:
│              │    ├── FR₁․₂․₂․₁: Continuous 500ms tactile hold invokes `performHapticFeedback` and calls `showSelfActionSheet()`.
│              ├── Testing Scenarios:
│              │    ├── Pass Scenario: Rich action sheet `👑 YOU (HOST DEVICE)` appears.
│              │    └── Fail Scenario: Releasing before 500ms acts as tap only.
│              └── ⚙️ [BE]₁․₂․₂․₁: Host Action Sheet Trigger (`MapActivity.showSelfActionSheet`)
│                   ├── BFR₁․₂․₂․₁: Instantiates and displays bottom sheet with 5 host options.
│                   ├── 🗄️ [Master DB [Online/Offline]]₁․₂․₂․₁․₁: SQLite `user_identity` Table
│                   │    └── 🗄️ [Sub Master/Fact DB]₁․₂․₂․₁․₁․₂: `device_vitals` Table
│                   │    │    └── 🗄️ [Transaction DB]₁․₂․₂․₁․₁․₁․₂: `sys_log` Table (`DASHBOARD_HOST_LONG_TAP`)
│
├── 💼 [BR]₂: Connected Peer Device Governance & Tactical Operations (Clicked on Connected Device)
│    ├── 🖱️ [EL]₂․₁: Connected Device Map Avatar & Marker Affordance
│    │    ├── 🖥️ [FE]₂․₁․₁: Marker Single-Tap Trigger (`showTacticalTargetActionSheet`)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₁․₁: Target ID cannot be empty or null. If empty, raises `Target Identity Missing` validation dialog.
│    │    │    │    ├── FR₂․₁․₁․₂: Resolves peer callsign from `DevicePresentationResolver.resolveDisplayName(context, deviceId)` via `DeviceRepository`.
│    │    │    │    └── FR₂․₁․₁․₃: Extracts family/friend role tags (`[Child]`, `[Guardian]`, `[Spouse]`).
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Tapping connected peer marker displays `🎯 TARGET: [NAME] [ROLE]` action sheet with 11 tactical operations.
│    │    │    │    └── Fail Scenario: If target ID matches host ID, automatically diverts to `showSelfActionSheet()`.
│    │    │    ├── ⚙️ [BE]₂․₁․₁․₁: Tactical Target Dossier Resolver & Action Dispatcher (`TacticalActionDispatcher.showTacticalActionSheet`)
│    │    │    │    ├── BFR₂․₁․₁․₁: Assembles peer identity using `TacticalActionDispatcher.TacticalTarget.fromUserLocation` + `DevicePresentationResolver`.
│    │    │    │    │    ├── Logic for Testing Pass Scenario: Assembles options array with 11 tactical items; enforces Anti-Null-Island coordinates validation gate (`Math.abs >= 0.001`).
│    │    │    │    │    └── Logic for Testing Fail Scenario: Offline targets trigger operator alert on acoustic siren dispatch instead of silent failure.
│    │    │    │    ├── 🗄️ [Master DB [Online/Offline]]₂․₁․₁․₁․₁: Firebase `/devices/{target_uid}/profile` ⟷ SQLite `devices` Table
│    │    │    │    │    └── 🗄️ [Sub Master/Fact DB]₂․₁․₁․₁․₁․₂: `MASTER_ROSTER` Table (`role_family`, `role_friend`)
│    │    │    │    │    │    └── 🗄️ [Transaction DB]₂․₁․₁․₁․₁․₁․₂: `sys_log` Table (`PEER_ACTION_SHEET_OPEN`)
│    │    │    │    │    │    └── 🗄️ [Registry SSOT]₂․₁․₁․₁․₁․₁․₃: `system_registry.xml` (`pref_siren_duration_sec`, `pref_map_expanded`, `map_dual_pane_active`)
│    │    │    └── ⚙️ [BE]₂․₁․₁․₂: Throttled Spatial Clustering & Multi-Marker Pipeline (`scheduleThrottledMarkerRender` + `DeviceRepository`)
│    │    │         ├── BFR₂․₁․₁․₂: Debounces all marker rendering calls to single 60fps window (60ms) via `scheduleThrottledMarkerRender`.
│    │    │         │    ├── Logic for Testing Pass Scenario: 5 concurrent sync triggers collapse into 1 render pass without frame drops.
│    │    │         │    └── Logic for Testing Fail Scenario: Zero-location payload safely handled without NPE.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₂․₁․₁․₂․₁: `DeviceRepository` SSOT (decoupled from direct UI SQLite access)
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₂․₁․₁․₂․₁․₂: `MASTER_ROSTER` ⟷ `device_vitals` via `DeviceRepository`
│    │    │         │    │    └── 🗄️ [Transaction DB]₂․₁․₁․₂․₁․₁․₂: `sys_log` (`MARKER_RENDER_THROTTLED`)
│    │    ├── 🖥️ [FE]₂․₁․₂: Option 1 — `📍 Find Current Location (Center & Track Live)`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₂․₁: Centers viewport on target's coordinates and locks camera tracking mode.
│    │    │    │    └── FR₂․₁․₂․₂: Initiates remote locate state machine and sends live poll packet.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Map pans smoothly to target marker (`zoom: 17.5f`), initiates tracking beacon.
│    │    │    │    └── Fail Scenario: Target coordinates (0,0) triggers offline fallback banner with latest SQLite history record.
│    │    │    └── ⚙️ [BE]₂․₁․₂․₁: Remote Locate State Machine & Ping Dispatcher (`initiateRemoteLocateStateMachine`)
│    │    │         ├── BFR₂․₁․₂․₁: Sends wake-up ping to Firebase `/devices/{target_uid}/device/pokes` with timestamp.
│    │    │         │    ├── Logic for Testing Pass Scenario: RTDB poke accepted; listener wakes up remote device.
│    │    │         │    └── Logic for Testing Fail Scenario: Peer unreachable -> displays last-known location marker with age badge.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₂․₁․₂․₁․₁: Firebase `/devices/{target_uid}/device/live` ⟷ SQLite `tracking_history` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₂․₁․₂․₁․₁․₂: `devices.last_lat`, `devices.last_lng` Columns
│    │    │         │    │    └── 🗄️ [Transaction DB]₂․₁․₂․₁․₁․₁․₂: `sys_log` Table (`REMOTE_LOCATE_DISPATCH`)
│    │    ├── 🖥️ [FE]₂․₁․₃: Option 2 — `🧭 Turn-by-Turn Navigation`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₃․₁: Valid latitude and longitude != 0.0.
│    │    │    │    └── FR₂․₁․₃․₂: Dispatches explicit URI intent: `google.navigation:q=lat,lng`.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: External Google Maps app opens in turn-by-turn routing mode to target's position.
│    │    │    │    └── Fail Scenario: If Google Maps not installed, falls back to web browser URL `https://maps.google.com/?q=lat,lng`.
│    │    │    └── ⚙️ [BE]₂․₁․₃․₁: External Navigation Intent Bridge (`MapActivity.navigateToTarget`)
│    │    │         ├── BFR₂․₁․₃․₁: Verifies PackageManager intent resolution before dispatching.
│    │    │         │    ├── Logic for Testing Pass Scenario: ResolveActivity != null -> dispatches nav intent.
│    │    │         │    └── Logic for Testing Fail Scenario: ResolveActivity == null -> falls back to browser URL intent.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₂․₁․₃․₁․₁: SQLite `tracking_history` (target latest coordinate)
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₂․₁․₃․₁․₁․₂: Android OS Intent Registry
│    │    │         │    │    └── 🗄️ [Transaction DB]₂․₁․₃․₁․₁․₁․₂: `sys_log` Table (`NAVIGATION_DISPATCH`)
│    │    ├── 🖥️ [FE]₂․₁․₄: Option 3 — `⏱️ View Location History`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₄․₁: Passes `target_uid` and `target_name` as intent extras to `HistoryActivity.class`.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: `HistoryActivity` opens showing polyline trail, breadcrumb stops, and speed analysis.
│    │    │    │    └── Fail Scenario: If no historical records exist, displays empty state graphic with "No Breadcrumbs Logged".
│    │    │    └── ⚙️ [BE]₂․₁․₄․₁: History Activity Launcher & Data Route Provider
│    │    │         ├── BFR₂․₁․₄․₁: Queries local SQLite `tracking_history` table partitioned by `uid` and date.
│    │    │         │    ├── Logic for Testing Pass Scenario: Historical points mapped into List<LatLng> chronologically.
│    │    │         │    └── Logic for Testing Fail Scenario: Corrupted rows ignored; valid rows render polyline.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₂․₁․₄․₁․₁: Firebase `/partitioned_location_trace/{uid}` ⟷ SQLite `tracking_history` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₂․₁․₄․₁․₁․₂: `device_locations` Table (cached GPS breadcrumbs)
│    │    │         │    │    └── 🗄️ [Transaction DB]₂․₁․₄․₁․₁․₁․₂: `change_log` Table (`HISTORY_VIEW_DISPATCH`)
│    │    ├── 🖥️ [FE]₂․₁․₅: Option 4 — `📱 Device Sharing (Connect / Roles)`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₅․₁: Opens Device Sharing 4-in-1 wizard (`showDeviceSharingWizard`):
│    │    │    │    │    ├── Sub-mode A: `Connect with Device C (Family/Friends)` -> `showConnectBToCDialog()`
│    │    │    │    │    ├── Sub-mode B: `Add Co-Parent / Guardian` -> `showAddAnotherParentDialog()`
│    │    │    │    │    ├── Sub-mode C: `Multi-Device Fleet Link (Tickboxes)` -> `showConnectAnotherDeviceDialog()`
│    │    │    │    │    └── Sub-mode D: `Generate Pairing Code (Network Gateway)` -> `generateMultiDevicePairingCode()`
│    │    │    │    └── FR₂․₁․₅․₂: Displays angel loader progress banner with 15-second watchdog timer.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Successful handshake writes bidirectional pairing record in Firebase and SQLite.
│    │    │    │    └── Fail Scenario: Timeout after 15s triggers closed-loop retry banner with non-destructive fallback.
│    │    │    └── ⚙️ [BE]₂․₁․₅․₁: Multi-Device Pairing Engine (`PairingRepository.kt` & `DevicePairingTable`)
│    │    │         ├── BFR₂․₁․₅․₁: Enforces symmetric pairing in `/relationships/{group}/device_pairings`.
│    │    │         │    ├── Logic for Testing Pass Scenario: Atomically updates pairing keys in both directions.
│    │    │         │    └── Logic for Testing Fail Scenario: Offline -> queues payload in `outbox_queue` for batch execution.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₂․₁․₅․₁․₁: Firebase `/relationships/{group}/device_pairings` ⟷ SQLite `device_pairings` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₂․₁․₅․₁․₁․₂: `group_members` Table (`role`, `status`)
│    │    │         │    │    └── 🗄️ [Transaction DB]₂․₁․₅․₁․₁․₁․₂: `outbox_queue` Table (`PAIRING_REQUEST_DISPATCH`)
│    │    ├── 🖥️ [FE]₂․₁․₆: Option 5 — `🦋 Butterfly Point (Rendezvous & Proximity)`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₆․₁: Opens `ButterflyPointDialog.showForTarget()` pre-populated with target device as recipient.
│    │    │    │    └── FR₂․₁․₆․₂: Allows setting meeting coordinate via Pin, Current GPS, Place Search, or Midpoint.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Rendezvous point created with 100m arrival proximity halo and shared to target node.
│    │    │    │    └── Fail Scenario: Missing coordinate aborts with validation message "Select valid destination coordinate".
│    │    │    └── ⚙️ [BE]₂․₁․₆․₁: Butterfly Point Rendezvous Engine (`ButterflyPointEngine` & `ButterflyPointViewModel`)
│    │    │         ├── BFR₂․₁․₆․₁: Stores rendezvous entity in Firebase `/butterfly_points/{point_id}`.
│    │    │         │    ├── Logic for Testing Pass Scenario: Computes Haversine distance, speed, and ETA relative to rendezvous center.
│    │    │         │    └── Logic for Testing Fail Scenario: Node leaves circle -> invalidates invite safely.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₂․₁․₆․₁․₁: Firebase `/butterfly_points/{id}` ⟷ SQLite `butterfly_point` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₂․₁․₆․₁․₁․₂: `butterfly_point_device_map` Table
│    │    │         │    │    └── 🗄️ [Transaction DB]₂․₁․₆․₁․₁․₁․₂: `alert_history` Table (`BUTTERFLY_POINT_SHARED`)
│    │    ├── 🖥️ [FE]₂․₁․₇: Option 6 — `🛡️ Assign / Create Safe Zone`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₇․₁: Launches `GeofenceActivity.class` pre-filling `target_uid`, `target_name`, `lat`, `lng`.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Geofence creation wizard opens with circular boundary overlay centered on target's position.
│    │    │    │    └── Fail Scenario: Target coordinates null defaults geofence center to host's GPS fix.
│    │    │    └── ⚙️ [BE]₂․₁․₇․₁: Safe Zone Manager Bridge & Geofencing Registration
│    │    │         ├── BFR₂․₁․₇․₁: Registers circular geofence with Android OS `GeofencingClient`.
│    │    │         │    ├── Logic for Testing Pass Scenario: Sets radius, dwell time, and enters pending broadcast monitor.
│    │    │         │    └── Logic for Testing Fail Scenario: Location permission revoked -> displays warning banner.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₂․₁․₇․₁․₁: Firebase `/geofences/{target_uid}` ⟷ SQLite `safe_zones` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₂․₁․₇․₁․₁․₂: `safe_zone_user_map` Table
│    │    │         │    │    └── 🗄️ [Transaction DB]₂․₁․₇․₁․₁․₁․₂: `alert_history` Table (`GEOFENCE_ASSIGNED`)
│    │    ├── 🖥️ [FE]₂․₁․₈: Option 7 — `🔊 Ring Device (Acoustic Siren)`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₈․₁: Queries `RegistryManager.getSirenDurationSec(this)` (default: 30s).
│    │    │    │    └── FR₂․₁․₈․₂: Displays acoustic trigger confirmation dialog before sounding alarm on peer phone.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Transmits acoustic siren payload to Firebase `/devices/{target_uid}/signals/siren`.
│    │    │    │    └── Fail Scenario: If offline, queues payload in `outbox_queue` for automatic burst upon connectivity.
│    │    │    └── ⚙️ [BE]₂․₁․₈․₁: Acoustic Ping Sentinel (`MapActivity.sendAcousticPing`)
│    │    │         ├── BFR₂․₁․₈․₁: Commits siren command with duration and timestamp to Firebase RTDB.
│    │    │         │    ├── Logic for Testing Pass Scenario: Writes duration and timestamp; receives ACK payload.
│    │    │         │    └── Logic for Testing Fail Scenario: Connection drop -> stored in local queue.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₂․₁․₈․₁․₁: Firebase `/devices/{target_uid}/signals/siren` ⟷ SQLite `devices` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₂․₁․₈․₁․₁․₂: `device_vitals` Table (`siren_active=1`)
│    │    │         │    │    └── 🗄️ [Transaction DB]₂․₁․₈․₁․₁․₁․₂: `alert_history` Table (`ACOUSTIC_SIREN_TRIGGERED`)
│    │    ├── 🖥️ [FE]₂․₁․₉: Option 8 — `👤 View Member Details & Roles`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₉․₁: Launches `PersonDetailsActivity.class` passing target node ID, ACLs, and roles.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Person Details screen renders hardware specs, battery graph, network RTT, and role selector.
│    │    │    │    └── Fail Scenario: Unpaired device opens read-only profile dossier.
│    │    │    └── ⚙️ [BE]₂․₁․₉․₁: Member Dossier Provider (`PersonDetailsViewModel`)
│    │    │         ├── BFR₂․₁․₉․₁: Queries SQLite `user_profile` and `MASTER_ROSTER` for detailed identity metadata.
│    │    │         │    ├── Logic for Testing Pass Scenario: Returns joined dataset of hardware specs and telemetry.
│    │    │         │    └── Logic for Testing Fail Scenario: Empty profile returns basic identity card with hardware ID.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₂․₁․₉․₁․₁: Firebase `/devices/{target_uid}/profile` ⟷ SQLite `user_profile` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₂․₁․₉․₁․₁․₂: `MASTER_ROSTER` Table (`role_family`, `is_child`)
│    │    │         │    │    └── 🗄️ [Transaction DB]₂․₁․₉․₁․₁․₁․₂: `change_log` Table (`DOSSIER_ACCESSED`)
│    │    ├── 🖥️ [FE]₂․₁․₁₀: Option 9 — `🔄 Refresh Latest Profile Data`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₁₀․₁: Forces network query to Cloud Firebase SSOT `/devices/{target_uid}/profile`.
│    │    │    │    └── FR₂․₁․₁₀․₂: Shows spinning progress pill and updates local cached name, avatar, and roles.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Refreshes avatar and callsign on map marker in < 500ms; displays "Profile Synchronized".
│    │    │    │    └── Fail Scenario: Network error displays "Sync Failed — Using Cached Telemetry" without breaking marker.
│    │    │    └── ⚙️ [BE]₂․₁․₁₀․₁: Cloud SSOT Profile Syncer (`MapActivity.refreshTargetProfileData`)
│    │    │         ├── BFR₂․₁․₁₀․₁: Writes updated cloud snapshot directly to local SQLite `users` and `devices` tables.
│    │    │         │    ├── Logic for Testing Pass Scenario: Cloud snapshot parsed -> updates SQLite avatar_url and user_name.
│    │    │         │    └── Logic for Testing Fail Scenario: Timeout preserves existing cached profile record.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₂․₁․₁₀․₁․₁: Firebase `/devices/{target_uid}/profile` ⟷ SQLite `users` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₂․₁․₁₀․₁․₁․₂: `devices` Table (`avatar_url`, `user_name`)
│    │    │         │    │    └── 🗄️ [Transaction DB]₂․₁․₁₀․₁․₁․₁․₂: `change_log` Table (`PROFILE_FORCE_SYNC`)
│    │    ├── 🖥️ [FE]₂․₁․₁₁: Option 10 — `📋 Copy GPS Coordinates`
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₁₁․₁: Target latitude and longitude must be valid floating point values.
│    │    │    │    └── FR₂․₁․₁₁․₂: Formats string as `%.6f, %.6f` and copies to `ClipboardManager`.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Clipboard contains target coordinates; toast confirms copy.
│    │    │    │    └── Fail Scenario: Coordinates (0,0) copies string with warning note.
│    │    │    └── ⚙️ [BE]₂․₁․₁₁․₁: Target Coordinate Clipboard Formatter
│    │    │         ├── BFR₂․₁․₁₁․₁: Copies target lat/lng to clipboard service.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₂․₁․₁₁․₁․₁: SQLite `tracking_history` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₂․₁․₁₁․₁․₁․₂: Android `ClipboardManager` buffer
│    │    │         │    │    └── 🗄️ [Transaction DB]₂․₁․₁₁․₁․₁․₁․₂: `sys_log` Table (`CLIPBOARD_COPY_TARGET_GPS`)
│    │    └── 🖥️ [FE]₂․₁․₁₂: Option 11 — `❌ Unlink / Remove Device`
│    │         ├── Validation & Conditional Rules:
│    │         │    ├── FR₂․₁․₁₂․₁: Shows confirmation dialog with device name and explicit red "UNLINK" button.
│    │         │    └── FR₂․₁․₁₂․₂: Guarded by Gate 2 confirmation.
│    │         ├── Testing Scenarios:
│    │         │    ├── Pass Scenario: Confirmed unlink removes pairing from Firebase RTDB and local SQLite; marker disappears.
│    │         │    └── Fail Scenario: Canceling dialog leaves pairing intact without data mutation.
│    │         └── ⚙️ [BE]₂․₁․₁₂․₁: Device Uncoupling Sentinel (`MapActivity.showUnlinkDeviceDialog`)
│    │              ├── BFR₂․₁․₁₂․₁: Deletes pairing record from `/relationships/{group}/device_pairings/{target_uid}`.
│    │              │    ├── Logic for Testing Pass Scenario: Removes cloud pairing node and local SQLite row.
│    │              │    └── Logic for Testing Fail Scenario: Offline -> queues uncoupling transaction in `outbox_queue`.
│    │              ├── 🗄️ [Master DB [Online/Offline]]₂․₁․₁₂․₁․₁: Firebase `/relationships/{group}/device_pairings` ⟷ SQLite `device_pairings` Table
│    │              │    └── 🗄️ [Sub Master/Fact DB]₂․₁․₁₂․₁․₁․₂: `MASTER_ROSTER` Table (marks record unlinked)
│    │              │    │    └── 🗄️ [Transaction DB]₂․₁․₁₂․₁․₁․₁․₂: `audit_log` Table (`DEVICE_UNLINK_EVENT`)
│    │
│    └── 🖱️ [EL]₂․₂: Connected Peer Tactical Marker & Direct Action Affordance
│         ├── 🖥️ [FE]₂․₂․₁: Single-Tap Camera Pan & Marquee Update
│         │    ├── Validation & Conditional Rules:
│         │    │    ├── FR₂․₂․₁․₁: Pans and zooms map camera to target position (`zoom: 17.5f`).
│         │    │    └── FR₂․₂․₁․₂: Updates bottom address marquee with target's resolved reverse-geocoded address.
│         │    ├── Testing Scenarios:
│         │    │    ├── Pass Scenario: Smooth camera glide to peer; address marquee refreshes.
│         │    │    └── Fail Scenario: If peer coordinates invalid, camera holds current position with warning toast.
│         │    └── ⚙️ [BE]₂․₂․₁․₁: Tactical Peer Camera Focus Controller
│         │         ├── BFR₂․₂․₁․₁: Coordinates camera glide and reverse geocode worker.
│         │         ├── 🗄️ [Master DB [Online/Offline]]₂․₂․₁․₁․₁: SQLite `tracking_history` Table
│         │         │    └── 🗄️ [Sub Master/Fact DB]₂․₂․₁․₁․₁․₂: `devices` Table
│         │         │    │    └── 🗄️ [Transaction DB]₂․₂․₁․₁․₁․₁․₂: `sys_log` Table (`PEER_MARKER_TAP`)
│         └── 🖥️ [FE]₂․₂․₂: Direct Marker Long-Press Options Dialog (`showPeerTacticalDialog`)
│              ├── Validation & Conditional Rules:
│              │    ├── FR₂․₂․₂․₁: Opens modal dialog with options: `📡 SEND PING`, `📜 HISTORY`, `CLOSE`.
│              ├── Testing Scenarios:
│              │    ├── Pass Scenario: Selecting `📡 SEND PING` transmits wake-up packet; selecting `📜 HISTORY` launches `HistoryActivity`.
│              │    └── Fail Scenario: Dismissing dialog keeps dashboard state intact.
│              └── ⚙️ [BE]₂․₂․₂․₁: Peer Node Management Controller (`MapActivity.showPeerTacticalDialog`)
│                   ├── BFR₂․₂․₂․₁: Invokes `FirebaseComm.pokeDevice(deviceId)` and shows toast feedback.
│                   │    ├── Logic for Testing Pass Scenario: Appends timestamped poke entry to peer's inbound queue.
│                   │    └── Logic for Testing Fail Scenario: Offline -> queues poke in local outbox table.
│                   ├── 🗄️ [Master DB [Online/Offline]]₂․₂․₂․₁․₁: Firebase `/devices/{id}/device/pokes` ⟷ SQLite `devices` Table
│                   │    └── 🗄️ [Sub Master/Fact DB]₂․₂․₂․₁․₁․₂: `tracking_history` Table
│                   │    │    └── 🗄️ [Transaction DB]₂․₂․₂․₁․₁․₁․₂: `outbox_queue` Table (`POKE_DISPATCH`)
│
├── 💼 [BR]₃: Left Floating Distant Device HUD (> Threshold Distance Proximity Gate, Default 50m)
│    ├── 🖱️ [EL]₃․₁: Proximity Boundary Threshold Configurator (`SettingsActivity` Card 8 & `MapActivity` HUD)
│    │    ├── 🖥️ [FE]₃․₁․₁: Distance Threshold Radio Selector Dialog & Settings Tile
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₃․₁․₁․₁: Allows choosing boundary distance: `25m (Immediate)`, `50m (Standard Default)`, `100m`, `200m`, `500m`, `1000m`, or `Custom`.
│    │    │    │    ├── FR₃․₁․₁․₂: Persists choice into `RegistryManager.setDistantDeviceThresholdMeters(context, chosen)` (`pref_distant_device_threshold_meters`).
│    │    │    │    ├── FR₃․₁․₁․₃: Editable under `SettingsActivity` -> `CARD 8: 🗺️ MAP DISPLAY & GLOBAL AESTHETICS (PRIORITY 8)`.
│    │    │    │    └── FR₃․₁․₁․₄: Invalidation Trigger: On threshold change or `MapActivity.onResume()`, immediately clears `distantDeviceHysteresisSet` and re-evaluates active nodes.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Setting threshold to 100m immediately causes devices between 50m and 100m to move from left HUD to map canvas spiderweb.
│    │    │    │    └── Fail Scenario: Zero or negative custom input rejected with validation error; retains previous valid setting.
│    │    │    └── ⚙️ [BE]₃․₁․₁․₁: Threshold Boundary Sentinel (`RegistryManager.getDistantDeviceThresholdMeters`)
│    │    │         ├── BFR₃․₁․₁․₁: Reads and updates threshold key `pref_distant_device_threshold_meters`.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₃․₁․₁․₁․₁: SQLite `system_registry.xml`
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₃․₁․₁․₁․₁․₂: In-memory Hysteresis Filter State (`distantDeviceHysteresisSet`)
│    │    │         │    │    └── 🗄️ [Transaction DB]₃․₁․₁․₁․₁․₁․₂: `sys_log` Table (`THRESHOLD_UPDATE_EVENT`)
│    │    ├── 🖥️ [FE]₃․₁․₂: Redundant Dashboard Strip Pipeline Gate
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₃․₁․₂․₁: If `isMapExpanded == true` (Full-Screen Live Tactical Map), suppresses execution of `updateDashboardDevices(locations)` to eliminate duplicate CPU and View inflations.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: In full-screen mode, 0 CPU cycles or View objects allocated for hidden `dashboardDeviceStrip`.
│    │    │    │    └── Fail Scenario: If map collapsed to short mode (`isMapExpanded == false`), `updateDashboardDevices` resumes seamlessly.
│    │    │    └── ⚙️ [BE]₃․₁․₂․₁: Pipeline Execution Filter (`MapActivity.updateDashboardDevices`)
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₃․₁․₂․₁․₁: In-memory View Hierarchy
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₃․₁․₂․₁․₁․₂: `binding.dashboardGridZone` Visibility State
│    │    │         │    │    └── 🗄️ [Transaction DB]₃․₁․₂․₁․₁․₁․₂: `sys_log` Table (`DASHBOARD_PIPELINE_GATED`)
│    ├── 🖱️ [EL]₃․₂: State 1 — Compact Circular Avatar (Default Standard Size, No Doubling)
│    │    ├── 🖥️ [FE]₃․₂․₁: Circular Avatar Single-Tap Trigger (Direct Camera Focus + Card Expand)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₃․₂․₁․₁: Standard 45dp circular container (36dp avatar). No artificial doubling.
│    │    │    │    ├── FR₃․₂․₁․₂: Single tap immediately animates camera to `newLatLngZoom(target, 17.5f)` (`LOCK_PEER`), streams reverse-geocoded address into marquee, and expands card into State 2.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Single tap on circular avatar flies camera to distant device and presents metadata card in < 250ms.
│    │    │    │    └── Fail Scenario: If device has (0,0) coordinates, camera does not fly to Null Island; displays "GPS Acquiring".
│    │    │    └── ⚙️ [BE]₃․₂․₁․₁: Camera Flight & Expansion Controller (`MapActivity.updateFloatingDevicePanel`)
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₃․₂․₁․₁․₁: In-memory `expandedFloatingDeviceIds: Set<String>`
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₃․₂․₁․₁․₁․₂: SQLite `devices` (cached avatar image path)
│    │    │         │    │    └── 🗄️ [Transaction DB]₃․₂․₁․₁․₁․₁․₂: `sys_log` Table (`HUD_EXPAND_AND_FOCUS`)
│    │    └── 🖥️ [FE]₃․₂․₂: Circular Avatar Long-Press Trigger
│    │         ├── Validation & Conditional Rules:
│    │         │    ├── FR₃․₂․₂․₁: Continuous 500ms tactile hold opens `showTacticalTargetActionSheet(ul)` directly.
│    │         ├── Testing Scenarios:
│    │         │    ├── Pass Scenario: Tactical Action Sheet opens for distant peer without expanding card first.
│    │         └── ⚙️ [BE]₃․₂․₂․₁: Tactical Action Sheet Forwarder (`TacticalActionDispatcher.showTacticalActionSheet`)
│    │              ├── 🗄️ [Master DB [Online/Offline]]₃․₂․₂․₁․₁: SQLite `devices` Table
│    │              │    └── 🗄️ [Sub Master/Fact DB]₃․₂․₂․₁․₁․₂: `MASTER_ROSTER` Table
│    │              │    │    └── 🗄️ [Transaction DB]₃․₂․₂․₁․₁․₁․₂: `sys_log` Table (`HUD_LONG_PRESS_ACTION_SHEET`)
│    └── 🖱️ [EL]₃․₃: State 2 — Expanded Metadata Card (Distance, Duration, Battery)
│         ├── 🖥️ [FE]₃․₃․₁: Card Body Click Camera Re-Focus Trigger
│         │    ├── Validation & Conditional Rules:
│         │    │    ├── FR₃․₃․₁․₁: Single click on rectangle card body or text column delegates to `TacticalCameraController.focusOnPeer(ul)`.
│         │    │    ├── FR₃․₃․₁․₂: Calls `DevicePresentationResolver.resolveTruncatedName(context, id, name, 10)` for SSOT callsign.
│         │    │    └── FR₃․₃․₁․₃: Delegates to `ReverseGeocodeEngine.resolveAddress(context, lat, lng)` with 2-tier LRU+SQLite caching.
│         │    ├── Testing Scenarios:
│         │    │    ├── Pass Scenario: Camera flies to distant device; bottom marquee streams full address and battery status in < 150ms.
│         │    └── ⚙️ [BE]₃․₃․₁․₁: Camera Flight & Reverse Geocode Sentinel (`TacticalCameraController` + `ReverseGeocodeEngine`)
│         │         ├── 🗄️ [Master DB [Online/Offline]]₃․₃․₁․₁․₁: In-memory LRU Cache (100 entries) ⟷ SQLite `reverse_geocode_cache`
│         │         │    └── 🗄️ [Sub Master/Fact DB]₃․₃․₁․₁․₁․₂: `MASTER_ROSTER` / `devices` Table
│         │         │    │    └── 🗄️ [Transaction DB]₃․₃․₁․₁․₁․₁․₂: `sys_log` Table (`HUD_CAMERA_FOCUS`)
│         ├── 🖥️ [FE]₃․₃․₂: Card Avatar & Close [✕] Click Trigger (Collapse to State 1)
│         │    ├── Validation & Conditional Rules:
│         │    │    ├── FR₃․₃․₂․₁: Clicking thumbnail avatar or [✕] button removes ID from `expandedFloatingDeviceIds` and collapses card.
│         │    ├── Testing Scenarios:
│         │    │    ├── Pass Scenario: Rectangle card cleanly folds back into compact circle avatar.
│         │    └── ⚙️ [BE]₃․₃․₂․₁: HUD Collapse Controller
│         │         ├── 🗄️ [Master DB [Online/Offline]]₃․₃․₂․₁․₁: In-memory `expandedFloatingDeviceIds`
│         │         │    └── 🗄️ [Sub Master/Fact DB]₃․₃․₂․₁․₁․₂: `system_registry.xml`
│         │         │    │    └── 🗄️ [Transaction DB]₃․₃․₂․₁․₁․₁․₂: `sys_log` Table (`HUD_COLLAPSE_CARD`)
│         └── 🖥️ [FE]₃․₃․₃: Card Long-Press Trigger
│              ├── Validation & Conditional Rules:
│              │    ├── FR₃․₃․₃․₁: Long-press on expanded card opens `showTacticalTargetActionSheet(ul)`.
│              ├── Testing Scenarios:
│              │    ├── Pass Scenario: Tactical Action Sheet opens smoothly.
│              └── ⚙️ [BE]₃․₃․₃․₁: Tactical Action Sheet Forwarder (`TacticalActionDispatcher.showTacticalActionSheet`)
│                   ├── 🗄️ [Master DB [Online/Offline]]₃․₃․₃․₁․₁: SQLite `devices` Table
│                   │    └── 🗄️ [Sub Master/Fact DB]₃․₃․₃․₁․₁․₂: `MASTER_ROSTER` Table
│                   │    │    └── 🗄️ [Transaction DB]₃․₃․₃․₁․₁․₁․₂: `sys_log` Table (`HUD_EXPANDED_LONG_PRESS`)
│
├── 💼 [BR]₄: Right-Side Tactical Action Dock Governance (`binding.sideActionPanel`)
│    ├── 🖱️ [EL]₄․₁: Recenter GPS FAB (`ACTION_LOCATE`)
│    │    ├── 🖥️ [FE]₄․₁․₁: Tactile Crosshair Button
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₄․₁․₁․₁: Single click centers camera on operator GPS coordinates.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Camera locks onto Host location; compass bearing aligns.
│    │    │    │    └── Fail Scenario: No GPS fix displays "Acquiring GPS fix...".
│    │    │    └── ⚙️ [BE]₄․₁․₁․₁: Host Camera Re-Center Controller (`MapActivity.recenterCamera`)
│    │    │         ├── BFR₄․₁․₁․₁: Coordinates camera glide to operator GPS position.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₄․₁․₁․₁․₁: SQLite `location_event` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₄․₁․₁․₁․₁․₂: `tracking_history` Table
│    │    │         │    │    └── 🗄️ [Transaction DB]₄․₁․₁․₁․₁․₁․₂: `sys_log` Table (`ACTION_LOCATE_CLICK`)
│    ├── 🖱️ [EL]₄․₂: Device Sharing Hub FAB (`ACTION_DEVICE_SHARING`)
│    │    ├── 🖥️ [FE]₄․₂․₁: Device Sharing Single-Tap & Long-Press Dual Affordance
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₄․₂․₁․₁: Single click opens `showDeviceSharingHubDialog()` (consolidated 4-in-1 wizard).
│    │    │    │    └── FR₄․₂․₁․₂: Long-press (500ms) triggers `frameAllDevices()` auto-fitting bounding box of all active nodes.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Single tap opens Device Sharing dialog; long press smoothly frames all fleet nodes on screen.
│    │    │    │    └── Fail Scenario: Long press with 0 peer devices alerts "No remote devices to frame".
│    │    │    └── ⚙️ [BE]₄․₂․₁․₁: Fleet Bounding Box & Sharing Hub Dispatcher (`MapActivity.frameAllDevices`)
│    │    │         ├── BFR₄․₂․₁․₁: Computes `LatLngBounds.Builder` including all online/offline node coordinates.
│    │    │         │    ├── Logic for Testing Pass Scenario: Computes bounding box with 100dp padding; animates camera in 600ms.
│    │    │         │    └── Logic for Testing Fail Scenario: Single device falls back to centering that device at zoom 16.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₄․₂․₁․₁․₁: Firebase `/relationships/{group}/device_pairings` ⟷ SQLite `device_pairings` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₄․₂․₁․₁․₁․₂: `MASTER_ROSTER` Table
│    │    │         │    │    └── 🗄️ [Transaction DB]₄․₂․₁․₁․₁․₁․₂: `sys_log` Table (`FRAME_ALL_DEVICES_DISPATCH`)
│    ├── 🖱️ [EL]₄․₃: Matrix Refresh & Cloud Sync FAB (`ACTION_REFRESH`)
│    │    ├── 🖥️ [FE]₄․₃․₁: Tactile Cloud Sync Button
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₄․₃․₁․₁: Forces `viewModel.refreshLocations()`, reconnects Firebase RTDB via `goOnline()`.
│    │    │    │    └── FR₄․₃․₁․₂: Displays sliding slide-notification banner "MATRIX REFRESHED" and updates all markers.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Cloud listeners re-established; peer telemetry updated in < 300ms.
│    │    │    │    └── Fail Scenario: Offline state displays cached SQLite data without dropping existing markers.
│    │    │    └── ⚙️ [BE]₄․₃․₁․₁: Matrix Refresh Dispatcher & LiveData Trigger
│    │    │         ├── BFR₄․₃․₁․₁: Invokes `MultiMarkerEngine.processViewModelLocations` with latest LiveData value.
│    │    │         │    ├── Logic for Testing Pass Scenario: Maps updated location list to marker overlay within 200ms.
│    │    │         │    └── Logic for Testing Fail Scenario: Empty list maintains existing markers until fresh batch arrives.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₄․₃․₁․₁․₁: Firebase RTDB Connection State ⟷ SQLite `location_event` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₄․₃․₁․₁․₁․₂: `tracking_history` Table
│    │    │         │    │    └── 🗄️ [Transaction DB]₄․₃․₁․₁․₁․₁․₂: `sys_log` Table (`MATRIX_REFRESH_TRIGGER`)
│    ├── 🖱️ [EL]₄․₄: Camera Zoom In FAB (`ACTION_ADD`)
│    │    ├── 🖥️ [FE]₄․₄․₁: Magnification Increment Button
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₄․₄․₁․₁: Increments current map zoom by +1.0 using `CameraUpdateFactory.zoomIn()`.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Smooth camera zoom animation executed.
│    │    │    │    └── Fail Scenario: GoogleMap uninitialized shows "Map engine initializing...".
│    │    │    └── ⚙️ [BE]₄․₄․₁․₁: Google Maps Camera Zoom Controller
│    │    │         ├── BFR₄․₄․₁․₁: Coordinates camera zoom animation.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₄․₄․₁․₁․₁: SQLite `system_registry.xml` (`pref_map_zoom`)
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₄․₄․₁․₁․₁․₂: In-memory GoogleMap Viewport State
│    │    │         │    │    └── 🗄️ [Transaction DB]₄․₄․₁․₁․₁․₁․₂: `sys_log` Table (`ZOOM_IN_CLICK`)
│    ├── 🖱️ [EL]₄․₅: Camera Zoom Out FAB (`ACTION_MINUS`)
│    │    ├── 🖥️ [FE]₄․₅․₁: Magnification Decrement Button
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₄․₅․₁․₁: Decrements current map zoom by -1.0 using `CameraUpdateFactory.zoomOut()`.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Smooth camera zoom out animation executed.
│    │    │    │    └── Fail Scenario: GoogleMap uninitialized shows "Map engine initializing...".
│    │    │    └── ⚙️ [BE]₄․₅․₁․₁: Google Maps Camera Zoom Controller
│    │    │         ├── BFR₄․₅․₁․₁: Coordinates camera zoom animation.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₄․₅․₁․₁․₁: SQLite `system_registry.xml` (`pref_map_zoom`)
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₄․₅․₁․₁․₁․₂: In-memory GoogleMap Viewport State
│    │    │         │    │    └── 🗄️ [Transaction DB]₄․₅․₁․₁․₁․₁․₂: `sys_log` Table (`ZOOM_OUT_CLICK`)
│    ├── 🖱️ [EL]₄․₆: Tactical Map Layers Selector FAB (`ACTION_LAYERS`)
│    │    ├── 🖥️ [FE]₄․₆․₁: Map Style Modal Selector Button
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₄․₆․₁․₁: Opens modal layer selector dialog with 4 styles: `NORMAL`, `SATELLITE`, `TERRAIN`, `DARK OLED`.
│    │    │    │    └── FR₄․₆․₁․₂: Persists chosen layer preference in `system_registry.xml`.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Selecting Satellite applies satellite tiles; selecting Dark OLED loads custom JSON map style.
│    │    │    │    └── Fail Scenario: Corrupted custom JSON style falls back gracefully to standard Dark or Normal tiles.
│    │    │    └── ⚙️ [BE]₄․₆․₁․₁: Map Styling Engine & Tile Layer Controller (`MapStyleManager`)
│    │    │         ├── BFR₄․₆․₁․₁: Configures `GoogleMap.setMapType` or `GoogleMap.setMapStyle`.
│    │    │         │    ├── Logic for Testing Pass Scenario: Layer index updates tile renderer and persists key.
│    │    │         │    └── Logic for Testing Fail Scenario: Missing asset falls back to MAP_TYPE_NORMAL.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₄․₆․₁․₁․₁: SQLite `system_registry.xml` (`pref_map_style`)
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₄․₆․₁․₁․₁․₂: Assets JSON Map Style Definition
│    │    │         │    │    └── 🗄️ [Transaction DB]₄․₆․₁․₁․₁․₁․₂: `sys_log` Table (`MAP_STYLE_CHANGED`)
│    ├── 🖱️ [EL]₄․₇: Emergency SOS Beacon FAB (`ACTION_ALERT`)
│    │    ├── 🖥️ [FE]₄․₇․₁: Emergency Distress Beacon Button
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₄․₇․₁․₁: Single click triggers `triggerEmergencySosDialog()`.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Emergency 5-second countdown dialog appears with sirens and SOS broadcast.
│    │    │    │    └── Fail Scenario: Premature cancel aborts broadcast.
│    │    │    └── ⚙️ [BE]₄․₇․₁․₁: Emergency SOS Sentinel Controller
│    │    │         ├── BFR₄․₇․₁․₁: Broadcasts emergency distress payload.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₄․₇․₁․₁․₁: Firebase `/relationships/{group}/signals/sos` ⟷ SQLite `alert_history` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₄․₇․₁․₁․₁․₂: `device_vitals.is_sos_active`
│    │    │         │    │    └── 🗄️ [Transaction DB]₄․₇․₁․₁․₁․₁․₂: `outbox_queue` Table (`SOS_TRIGGER_DISPATCH`)
│    ├── 🖱️ [EL]₄․₈: Butterfly Point Rendezvous & Ghost Hub FAB (`ACTION_GHOST`)
│    │    ├── 🖥️ [FE]₄․₈․₁: Ghost Icon Dual Affordance (Single Tap ⟷ Long Press)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₄․₈․₁․₁: Single tap opens `ButterflyPointDialog.show(this)` to manage rendezvous points.
│    │    │    │    └── FR₄․₈․₁․₂: Long-press (500ms) with haptic tick triggers legacy `ButterflyPointDialog.showGhostPrivacyModeToggle(this)`.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Single click opens Butterfly Point Sheet in < 150ms; long press opens Ghost Mode Selector.
│    │    │    │    └── Fail Scenario: Uninitialized dialog displays fallback toast.
│    │    │    └── ⚙️ [BE]₄․₈․₁․₁: Butterfly Point Rendezvous & Privacy Mode Controller
│    │    │         ├── BFR₄․₈․₁․₁: Manages rendezvous entities and privacy tier transitions.
│    │    │         │    ├── Logic for Testing Pass Scenario: Syncs rendezvous coordinate and notifies circle.
│    │    │         │    └── Logic for Testing Fail Scenario: Offline -> saves to SQLite `butterfly_point` table.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₄․₈․₁․₁․₁: Firebase `/butterfly_points/{id}` ⟷ SQLite `butterfly_point` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₄․₈․₁․₁․₁․₂: `butterfly_point_device_map` Table
│    │    │         │    │    └── 🗄️ [Transaction DB]₄․₈․₁․₁․₁․₁․₂: `alert_history` Table (`BUTTERFLY_POINT_ACCESSED`)
│    ├── 🖱️ [EL]₄․₉: Tactical Target Search FAB (`ACTION_SEARCH`)
│    │    ├── 🖥️ [FE]₄․₉․₁: Connected Device Search Dialog Button
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₄․₉․₁․₁: Opens `showTacticalTargetSearchDialog()` with search edit text and filtered device list.
│    │    │    │    └── FR₄․₉․₁․₂: Real-time text watcher filters connected devices by callsign, hardware ID, or role.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Typing name filters list dynamically; tapping result closes search and centers camera on device.
│    │    │    │    └── Fail Scenario: Typing non-matching query displays "No devices match filter".
│    │    │    └── ⚙️ [BE]₄․₉․₁․₁: Target Search Filter Engine (`MapActivity.showTacticalTargetSearchDialog`)
│    │    │         ├── BFR₄․₉․₁․₁: Filters in-memory device list from `MapViewModel.getUserLocations()`.
│    │    │         │    ├── Logic for Testing Pass Scenario: Substring match on name, agent_id, or role tags returns filtered list.
│    │    │         │    └── Logic for Testing Fail Scenario: Empty list shows zero-state label without crashing adapter.
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₄․₉․₁․₁․₁: SQLite `devices` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₄․₉․₁․₁․₁․₂: `MASTER_ROSTER` Table
│    │    │         │    │    └── 🗄️ [Transaction DB]₄․₉․₁․₁․₁․₁․₂: `sys_log` Table (`TACTICAL_SEARCH_QUERY`)
│    └── 🖱️ [EL]₄․₁₀: Viewport Expand / Dashboard Toggle FAB (`ACTION_EXPAND`)
│         ├── 🖥️ [FE]₄․₁₀․₁: Fullscreen ⟷ Split Dashboard Toggle Button (`binding.expandIcon`)
│         │    ├── Validation & Conditional Rules:
│         │    │    ├── FR₄․₁₀․₁․₁: Toggles `isMapExpanded = !isMapExpanded` and triggers `AutoTransition` animation (350ms).
│         │    │    ├── FR₄․₁₀․₁․₂: In Fullscreen mode: Hides `#dashboardGridZone`, zeroes map margins (0,0,0,0).
│         │    │    ├── FR₄․₁₀․₁․₃: In Split mode: Displays `#dashboardGridZone`, sets 4dp tactical padding.
│         │    │    └── FR₄․₁₀․₁․₄: Persists state to `pref_map_expanded` in `system_registry.xml`.
│         │    ├── Testing Scenarios:
│         │    │    ├── Pass Scenario: Tapping expand smoothly transitions viewport between fullscreen map and split dashboard in 350ms.
│         │    │    └── Fail Scenario: Rapid tapping debounced via transition manager.
│         │    └── ⚙️ [BE]₄․₁₀․₁․₁: Viewport Dimension & Transition Controller (`MapActivity.updateExpandIconState`)
│         │         ├── BFR₄․₁₀․₁․₁: Updates SharedPreferences key `pref_map_expanded` asynchronously.
│         │         │    ├── Logic for Testing Pass Scenario: Commits boolean state; updates icon drawable orientation.
│         │         │    └── Logic for Testing Fail Scenario: Corrupted preference file repaired automatically.
│         │         ├── 🗄️ [Master DB [Online/Offline]]₄․₁₀․₁․₁․₁: SQLite `system_registry.xml` (`pref_map_expanded`)
│         │         │    └── 🗄️ [Sub Master/Fact DB]₄․₁₀․₁․₁․₁․₂: In-memory Viewport Layout Parameters
│         │         │    │    └── 🗄️ [Transaction DB]₄․₁₀․₁․₁․₁․₁․₂: `sys_log` Table (`VIEWPORT_EXPAND_TOGGLE`)
│
├── 💼 [BR]₅: Top Navigation Header, Picture-in-Picture & System Sentinels
│    ├── 🖱️ [EL]₅․₁: Top Header Avatar Affordance (`headerAvatar`)
│    │    ├── 🖥️ [FE]₅․₁․₁: Operator Profile Shortcut Button
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₅․₁․₁․₁: Tapping `headerAvatar` dispatches Intent to `ProfileActivity.class`.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Profile screen opens with operator avatar preview and identity details.
│    │    │    └── ⚙️ [BE]₅․₁․₁․₁: Operator Identity Launcher
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₅․₁․₁․₁․₁: SQLite `user_identity` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₅․₁․₁․₁․₁․₂: `user_profile` Table
│    │    │         │    │    └── 🗄️ [Transaction DB]₅․₁․₁․₁․₁․₁․₂: `sys_log` Table (`HEADER_AVATAR_CLICK`)
│    ├── 🖱️ [EL]₅․₂: Slide-Over Drawer Hamburger (`menuIcon`)
│    │    ├── 🖥️ [FE]₅․₂․₁: Navigation Drawer Trigger Button
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₅․₂․₁․₁: Opens navigation drawer smoothly with 15 functional items across 5 domains.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: Drawer slides out from left; scrim dims background map.
│    │    │    └── ⚙️ [BE]₅․₂․₁․₁: DrawerLayout Controller (`drawerLayout.openDrawer(Gravity.START)`)
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₅․₂․₁․₁․₁: SQLite `master_roster` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₅․₂․₁․₁․₁․₂: `group_members` Table
│    │    │         │    │    └── 🗄️ [Transaction DB]₅․₂․₁․₁․₁․₁․₂: `sys_log` Table (`DRAWER_OPENED`)
│    ├── 🖱️ [EL]₅․₃: Butterfly Logo & Active Card PiP Mode (`binding.logo`, `binding.activeCard`)
│    │    ├── 🖥️ [FE]₅․₃․₁: Picture-in-Picture Trigger
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₅․₃․₁․₁: Tapping logo or card invokes `launchPictureInPictureMode()`.
│    │    │    │    └── FR₅․₃․₁․₂: Sets aspect ratio (16:9), hides all overlays except map canvas and floating close button.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass Scenario: App transitions into compact floating floating window over device home screen.
│    │    │    │    └── Fail Scenario: OS versions < Android 8.0 (Oreo) notify "PiP Mode Requires Android 8.0+".
│    │    │    └── ⚙️ [BE]₅․₃․₁․₁: OS PiP Controller (`enterPictureInPictureMode`)
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₅․₃․₁․₁․₁: Android Activity Framework
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₅․₃․₁․₁․₁․₂: `PictureInPictureParams`
│    │    │         │    │    └── 🗄️ [Transaction DB]₅․₃․₁․₁․₁․₁․₂: `sys_log` Table (`PIP_ENTER_EVENT`)
│    ├── 🖱️ [EL]₅․₄: PiP Close Button (`binding.btnPipClose`)
│    │    ├── 🖥️ [FE]₅․₄․₁: Picture-in-Picture Close Action
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₅․₄․₁․₁: Visible only in PiP mode; tapping it closes activity or restores full view.
│    │    │    └── ⚙️ [BE]₅․₄․₁․₁: PiP Exit Controller
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₅․₄․₁․₁․₁: Android Window Manager
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₅․₄․₁․₁․₁․₂: Viewport State
│    │    │         │    │    └── 🗄️ [Transaction DB]₅․₄․₁․₁․₁․₁․₂: `sys_log` Table (`PIP_EXIT_EVENT`)
│    ├── 🖱️ [EL]₅․₅: Clean Address Marquee HUD (`tvAddressMarquee`)
│    │    ├── 🖥️ [FE]₅․₅․₁: Real-Time Marquee Stream
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₅․₅․₁․₁: Formats telemetry as: `📍 [Name] • [Clean Address] • 🔋 [Battery]% • [Speed] • [Time]`.
│    │    │    │    └── FR₅․₅․₁․₂: Horizontal marquee autoscrolls when text exceeds screen width.
│    │    │    └── ⚙️ [BE]₅․₅․₁․₁: Speed & Telemetry Formatter (`SpeedTelemetryFormatter.kt`)
│    │    │         ├── 🗄️ [Master DB [Online/Offline]]₅․₅․₁․₁․₁: SQLite `tracking_history` Table
│    │    │         │    └── 🗄️ [Sub Master/Fact DB]₅․₅․₁․₁․₁․₂: `device_vitals` Table
│    │    │         │    │    └── 🗄️ [Transaction DB]₅․₅․₁․₁․₁․₁․₂: `sys_log` Table (`MARQUEE_UPDATE`)
│    └── 🖱️ [EL]₅․₆: Version & Silent OTA Footer (`tvVersionFooter`)
│         ├── 🖥️ [FE]₅․₆․₁: OTA Update Check Trigger
│         │    ├── Validation & Conditional Rules:
│         │    │    ├── FR₅․₆․₁․₁: Tapping version footer dispatches Intent to `SystemUpdateActivity.class`.
│         │    ├── Testing Scenarios:
│         │    │    ├── Pass Scenario: System Update screen opens, checks GitHub/Firebase SSOT for latest APK build.
│         │    └── ⚙️ [BE]₅․₆․₁․₁: Silent OTA Sentinel (`SystemUpdateScheduler` & `ApkInstallerHelper`)
│         │         ├── 🗄️ [Master DB [Online/Offline]]₅․₆․₁․₁․₁: Firebase `/system_updates/latest_version`
│         │         │    └── 🗄️ [Sub Master/Fact DB]₅․₆․₁․₁․₁․₂: SQLite `system_registry.xml`
│         │         │    │    └── 🗄️ [Transaction DB]₅․₆․₁․₁․₁․₁․₂: `change_log` Table (`OTA_CHECK_DISPATCH`)
│
└── 💼 [BR]₆: Zero-Node Empty State & Network Gateway Onboarding
     ├── 🖱️ [EL]₆․₁: Empty Fleet Pair First Device FAB (`btnPair`)
     │    ├── 🖥️ [FE]₆․₁․₁: Network Gateway Launcher
     │    │    ├── Validation & Conditional Rules:
     │    │    │    ├── FR₆․₁․₁․₁: Rendered when active fleet count == 0; tapping opens `NetworkGatewayActivity.class`.
     │    │    ├── Testing Scenarios:
     │    │    │    ├── Pass Scenario: QR Scan & Pairing Matrix opens for operator.
     │    │    └── ⚙️ [BE]₆․₁․₁․₁: Gateway Dispatcher
     │    │         ├── 🗄️ [Master DB [Online/Offline]]₆․₁․₁․₁․₁: SQLite `device_pairings` Table
     │    │         │    └── 🗄️ [Sub Master/Fact DB]₆․₁․₁․₁․₁․₂: `MASTER_ROSTER` Table
     │    │         │    │    └── 🗄️ [Transaction DB]₆․₁․₁․₁․₁․₁․₂: `sys_log` Table (`ZERO_NODE_PAIR_CLICK`)
     ├── 🖱️ [EL]₆․₂: Empty Fleet Frame Devices FAB (`btnFrame`)
     │    ├── 🖥️ [FE]₆․₂․₁: Auto-Frame Trigger
     │    │    ├── Validation & Conditional Rules:
     │    │    │    ├── FR₆․₂․₁․₁: Triggers `frameAllDevices()`.
     │    │    └── ⚙️ [BE]₆․₂․₁․₁: Camera Bounding Box Controller
     │    │         ├── 🗄️ [Master DB [Online/Offline]]₆․₂․₁․₁․₁: SQLite `tracking_history` Table
     │    │         │    └── 🗄️ [Sub Master/Fact DB]₆․₂․₁․₁․₁․₂: In-memory Viewport Bounds
     │    │         │    │    └── 🗄️ [Transaction DB]₆․₂․₁․₁․₁․₁․₂: `sys_log` Table (`ZERO_NODE_FRAME_CLICK`)
     └── 🖱️ [EL]₆․₃: Dashboard "+ Add Device" Card (`createDashboardAddDeviceCard`)
          ├── 🖥️ [FE]₆․₃․₁: Horizontal Scroller Add Card
          │    ├── Validation & Conditional Rules:
          │    │    ├── FR₆․₃․₁․₁: Displayed at end of dashboard fleet card row; tapping launches `NetworkGatewayActivity.class`.
          │    ├── Testing Scenarios:
          │    │    ├── Pass Scenario: Seamless transition to device pairing hub.
          │    └── ⚙️ [BE]₆․₃․₁․₁: Gateway Dispatcher
          │         ├── 🗄️ [Master DB [Online/Offline]]₆․₃․₁․₁․₁: SQLite `device_pairings` Table
          │         │    └── 🗄️ [Sub Master/Fact DB]₆․₃․₁․₁․₁․₂: `group_members` Table
          │         │    │    └── 🗄️ [Transaction DB]₆․₃․₁․₁․₁․₁․₂: `sys_log` Table (`DASHBOARD_ADD_DEVICE_CLICK`)
```

---

#### 6. `LiveMapDashboardActivity` — Tactical Multi-Node Fleet Radar & Telemetry Grid
```text
ROOT: LIVE_MAP_DASHBOARD_MASTER_PLAN (com.jdroidx.jfindme.ui.map.LiveMapDashboardActivity)
│
├── 💼 [BR]₁: Multi-Asset Fleet State Synchronization, Radar Sweeping & Spatial Filtering
│    ├── 🖱️ [EL]₆․₁: Fleet Summary KPI Metrics Header
│    │    ├── 🖥️ [FE]₆․₁․₁: 4 Metric Badges: [Total Assets: N], [Active Live: N], [Standby: N], [SOS Active: N]
│    │    │    └── ⚙️ [BE]₆․₁․₁․₁: FleetMetricsAggregator
│    │    │         └── 🗄️ [DB]₆․₁․₁․₁․₁: SQLite Aggregation over `master_roster` and `device_vitals`
│    ├── 🖱️ [EL]₆․₂: Tactical Multi-Node Fleet Radar Canvas
│    │    ├── 🖥️ [FE]₆․₂․₁: Circular Radar Scope View with Rotating Azimuth Sweep Line & Polar Node Blips
│    │    │    ├── (FR6.1): Blip angle = relative bearing from operator; blip distance = logarithmic scale of range.
│    │    │    └── ⚙️ [BE]₆․₂․₁․₁: PolarCoordinateTransformer & AzimuthBearingCalculator
│    ├── 🖱️ [EL]₆․₃: Fleet Node Card Recycler Grid
│    │    ├── 🖥️ [FE]₆․₃․₁: 2-Column Grid of Asset Cards: Member Name, Velocity Gauge, Battery Bar, Last-Seen
│    │    │    ├── (FR6.2): Card border color indicates state: Green (< 5m), Amber (5m-1h), Gray (> 1h), Red (SOS).
│    │    │    └── ⚙️ [BE]₆․₃․₁․₁: MasterRosterRepository Telemetry LiveData Stream
│    │    │         └── 🗄️ [DB]₆․₃․₁․₁․₁: `master_roster` joined with `device_vitals`
│    ├── 🖱️ [EL]₆․₄: Range Segmented Filter Control
│    │    ├── 🖥️ [FE]₆․₄․₁: Segmented Filter Chips: [1 km], [5 km], [25 km], [Global Fleet]
│    │    │    ├── (FR6.3): Filters visible radar nodes using Great-Circle Haversine distance formula.
│    │    │    └── ⚙️ [BE]₆․₄․₁․₁: HaversineSpatialFilter
│    ├── 🖱️ [EL]₆․₅: Fleet Sort & Search Toolbar
│    │    ├── 🖥️ [FE]₆․₅․₁: Search EditText and Sort Spinner (Velocity, Distance, Battery, Alphabetical)
│    │    │    └── ⚙️ [BE]₆․₅․₁․₁: FleetCollectionSorter
│    └── 🖱️ [EL]₆․₆: Node Quick Switcher Floating Bar
│         ├── 🖥️ [FE]₆․₆․₁: Bottom Bar displaying currently selected node with "Inspect Deep Telemetry" action
│         │    └── ⚙️ [BE]₆․₆․₁․₁: Intent Launcher for PersonDetailsActivity
```

---

#### 7. `PersonDetailsActivity` — Member Deep-Dive, Sensor Stream & Silicon Node Inspector
```text
ROOT: PERSON_DETAILS_MASTER_PLAN (com.jdroidx.jfindme.ui.group.PersonDetailsActivity)
│
├── 💼 [BR]₁: Deep Node Telemetry Audit, Sensor Inspection & Remote Commands
│    ├── 🖱️ [EL]₇․₁: Header Bar with Subject Avatar & Online Status
│    │    ├── 🖥️ [FE]₇․₁․₁: Avatar View (`headerAvatar`), Edit Button (`editBtn`), Member Name, Online Status Pill
│    │    │    └── ⚙️ [BE]₇․₁․₁․₁: MasterRosterRepository.getMemberByAgentId
│    │    │         └── 🗄️ [DB]₇․₁․₁․₁․₁: `master_roster` (member_name, avatar_id)
│    ├── 🖱️ [EL]₇․₂: Section 1: `PERSONAL IDENTITY & SECURITY`
│    │    ├── 🖥️ [FE]₇․₂․₁: Identity Card: Monospace Hardware ID, Phone Number, Emergency Contact Tag
│    │    │    └── ⚙️ [BE]₇․₂․₁․₁: IdentityDetailsProvider
│    │    │         └── 🗄️ [DB]₇․₂․₁․₁․₁: `master_roster.agent_id` & `master_roster.phone_number`
│    ├── 🖱️ [EL]₇․₃: Section 2: `RESIDENCE & EMERGENCY PROTOCOL`
│    │    ├── 🖥️ [FE]₇․₃․₁: Protocol Card: Home Address, Safe Zone Binding (`branch`), DOB Field (`etDob`)
│    │    │    └── ⚙️ [BE]₇․₃․₁․₁: ResidenceProtocolProvider
│    │    │         └── 🗄️ [DB]₇․₃․₁․₁․₁: `user_profile.dob` & `safe_zones.assigned_agent_id`
│    ├── 🖱️ [EL]₇․₄: Section 3: `SAFE ZONE (GEOFENCE) RELATIONS`
│    │    ├── 🖥️ [FE]₇․₄․₁: Safe Zones List Card: Active perimeter adherence score %, breach counter
│    │    │    └── ⚙️ [BE]₇․₄․₁․₁: GeofenceHistoryAggregator
│    │    │         └── 🗄️ [DB]₇․₄․₁․₁․₁: `alert_history` (alert_type="GEOFENCE_BREACH")
│    ├── 🖱️ [EL]₇․₅: Section 4: `ENTERPRISE ACCESS CONTROL (ACL)`
│    │    ├── 🖥️ [FE]₇․₅․₁: ACL Card: Role Selector (`actv`), Permissions Checkboxes (`cb`), Owner Tile (`ownerTile`)
│    │    │    └── ⚙️ [BE]₇․₅․₁․₁: AccessControlManager
│    │    │         └── 🗄️ [DB]₇․₅․₁․₁․₁: `master_roster.permissions_mask`
│    ├── 🖱️ [EL]₇․₆: Section 5: `GLOBAL CLOUD PRIVACY`
│    │    ├── 🖥️ [FE]₇․₆․₁: Privacy Card: Telemetry precision badge, Stealth mode state, Baseband privacy toggle
│    │    │    └── ⚙️ [BE]₇․₆․₁․₁: PrivacyStateInspector
│    │    │         └── 🗄️ [DB]₇․₆․₁․₁․₁: `device_vitals.is_stealth`
│    ├── 🖱️ [EL]₇․₇: Section 6: `UNIVERSAL DEVICE NETWORK`
│    │    ├── 🖥️ [FE]₇․₇․₁: Technical Diagnostics: OS version, battery level bar, signal dBm, IP address
│    │    │    └── ⚙️ [BE]₇․₇․₁․₁: DatabaseHelper.getDeviceVitals
│    │    │         └── 🗄️ [DB]₇․₇․₁․₁․₁: `device_vitals` (battery_level, signal_dbm, network_type)
│    ├── 🖱️ [EL]₇․₈: Action Button 1: "Navigate to Subject"
│    │    ├── 🖥️ [FE]₇․₈․₁: Navigation FAB launching geo: URI intent to Google Maps / Waze
│    │    │    └── ⚙️ [BE]₇․₈․₁․₁: NavigationIntentLauncher
│    ├── 🖱️ [EL]₇․₉: Action Button 2: "Wakeup GPS Poke"
│    │    ├── 🖥️ [FE]₇․₉․₁: Action Button dispatching instant GPS location refresh command
│    │    │    └── ⚙️ [BE]₇․₉․₁․₁: FirebaseComm.pokeLocationUpdate
│    │    │         └── 🗄️ [DB]₇․₉․₁․₁․₁: Cloud Firebase `/devices/{agentId}/commands/poke`
│    └── 🖱️ [EL]₇․₁₀: Action Button 3: "Trigger Remote Siren"
│         ├── 🖥️ [FE]₇․₁₀․₁: Action Button triggering acoustic alarm on remote lost node
│         │    └── ⚙️ [BE]₇․₁₀․₁․₁: FirebaseComm.publishAlert(agentId, "REMOTE_SIREN")
│         │         └── 🗄️ [DB]₇․₁₀․₁․₁․₁: Cloud Firebase `/devices/{agentId}/commands/siren`
```

---

#### 8. `AvatarCropActivity` — Hardware Vector Biometric Cropper
```text
ROOT: AVATAR_CROP_MASTER_PLAN (com.jdroidx.jfindme.ui.settings.AvatarCropActivity)
│
├── 💼 [BR]₁: Biometric Image Geometry Normalization & Lossless Compression
│    ├── 🖱️ [EL]₈․₁: Global Header Bar & Back Button
│    │    ├── 🖥️ [FE]₈․₁․₁: Back Button, Title "CROP BIOMETRIC AVATAR"
│    │    │    └── ⚙️ [BE]₈․₁․₁․₁: BaseActivityJava Layout Header Geometry
│    ├── 🖱️ [EL]₈․₂: Circular Viewport Mask Canvas
│    │    ├── 🖥️ [FE]₈․₂․₁: Transparent Viewport with Semi-Opaque Dark Scrim & Circular 1:1 Cutout
│    │    │    └── ⚙️ [BE]₈․₂․₁․₁: PorterDuffXfermode CLEAR Mask Renderer
│    ├── 🖱️ [EL]₈․₃: Pinch-to-Zoom & Pan Gesture Controller
│    │    ├── 🖥️ [FE]₈․₃․₁: Multi-Touch SurfaceView with Gesture Scale Bounds Clamping
│    │    │    └── ⚙️ [BE]₈․₃․₁․₁: ScaleGestureDetector & Matrix Transform Engine
│    ├── 🖱️ [EL]₈․₄: Rotate 90° Clockwise Action Button
│    │    ├── 🖥️ [FE]₈․₄․₁: Rotate Action Button (`rotateBtn`) animating 90° rotation
│    │    │    └── ⚙️ [BE]₈․₄․₁․₁: Matrix.postRotate(90f)
│    ├── 🖱️ [EL]₈․₅: Aspect Ratio Selector Control
│    │    ├── 🖥️ [FE]₈․₅․₁: Segmented Control: [1:1 Circular Avatar] vs [1:1 Square Token]
│    │    │    └── ⚙️ [BE]₈․₅․₁․₁: CropShapeModeSelector
│    └── 🖱️ [EL]₈․₆: Save & Compress Primary Action FAB
│         ├── 🖥️ [FE]₈․₆․₁: "Save Biometric Token" FAB (`saveBtn`) with Processing Spinner
│         │    ├── (FR8.1): Crops to 512x512 JPEG; compresses file size to <= 120KB to preserve bandwidth.
│         │    └── ⚙️ [BE]₈․₆․₁․₁: LosslessJpegCompressor & Internal Storage Writer
│         │         ├── 🗄️ [DB]₈․₆․₁․₁․₁: Internal Storage File `app_data/avatars/{agent_id}.jpg`
│         │         └── 🗄️ [DB]₈․₆․₁․₁․₂: `user_profile.avatar_local_uri`
```


### Domain 3: Core Diagnostics, Hardware Vitals & Closed-Loop Engine

#### 9. `HealthActivity` — Hardware Vitals, Closed-Loop Latency & Cellular Diagnostics
```text
ROOT: HEALTH_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.management.HealthActivity)
│
├── 💼 [BR]₁: Physical Hardware Diagnostics, Closed-Loop Latency & Cell Tower Telemetry
│    ├── 🖱️ [EL]₉․₁: Global Header Bar & Back Button
│    │    ├── 🖥️ [FE]₉․₁․₁: Back Button (`backBtn`), Title "DEVICE HEALTH", Subtitle ("Hardware Diagnostics" or Member Node ID)
│    │    │    └── ⚙️ [BE]₉․₁․₁․₁: BaseActivityJava Layout Header Geometry
│    ├── 🖱️ [EL]₉․₂: Actionable Transport Fix Alert Banner
│    │    ├── 🖥️ [FE]₉․₂․₁: Blocking Condition Alert Card (`containerActionableFix`, `tvActionableFixMessage`, `btnActionableFix`)
│    │    │    ├── (FR9.1): Visible only when SimTransportValidator reports canShareViaSim == false; click triggers one-click fix.
│    │    │    └── ⚙️ [BE]₉․₂․₁․₁: SimTransportValidator.auditTransportHealth Engine
│    │    │         └── 🗄️ [DB]₉․₂․₁․₁․₁: `audit_log.transport_health_check`
│    ├── 🖱️ [EL]₉․₃: Section 1 Header: `SYSTEM STATUS`
│    │    └── 🖥️ [FE]₉․₃․₁: Section Divider Header
│    ├── 🖱️ [EL]₉․₄: Battery Level & Charging Status Tile
│    │    ├── 🖥️ [FE]₉․₄․₁: Battery Metric Card: Level % Bar, Charging Bolt Icon, Battery Temperature °C, Health Status
│    │    │    └── ⚙️ [BE]₉․₄․₁․₁: BatteryManager Broadcast Receiver
│    │    │         └── 🗄️ [DB]₉․₄․₁․₁․₁: `device_vitals.battery_level` & `device_vitals.is_charging`
│    ├── 🖱️ [EL]₉․₅: Section 2 Header: `CELLULAR TOWER & FAILOVER ROUTING`
│    │    └── 🖥️ [FE]₉․₅․₁: Section Divider Header
│    ├── 🖱️ [EL]₉․₆: Serving Cell Tower & Baseband Inspector Tile
│    │    ├── 🖥️ [FE]₉․₆․₁: Cellular Metric Card: Operator Name, MCC-MNC, CID, LAC, Signal dBm Bar (`tvCellTower`)
│    │    │    ├── (FR9.2): Signal bar: >= -85 dBm (Emerald), -86 to -105 dBm (Amber), <= -106 dBm (Crimson).
│    │    │    └── ⚙️ [BE]₉․₆․₁․₁: CellTowerSentinel.extractServingCellInfo & cell.getOperator()
│    │    │         └── 🗄️ [DB]₉․₆․₁․₁․₁: `sim_transactions` (operator, cid, lac, signal_dbm)
│    ├── 🖱️ [EL]₉․₇: Closed-Loop Active Failover Route Tile
│    │    ├── 🖥️ [FE]₉․₇․₁: Active Route Indicator (`tvClosedLoopRoute`): Cyan Monospace Text (e.g. "N1_CELLULAR_FAST")
│    │    │    └── ⚙️ [BE]₉․₇․₁․₁: DatabaseHelper.getDeviceVitals (reads `active_route`)
│    │    │         └── 🗄️ [DB]₉․₇․₁․₁․₁: `device_vitals.active_route`
│    ├── 🖱️ [EL]₉․₈: Closed-Loop Server ACK RTT Status Tile
│    │    ├── 🖥️ [FE]₉․₈․₁: Real-Time Verification Tile (`tvClosedLoopAck`): "CONFIRMED (RTT: N ms)" or "STANDBY"
│    │    │    ├── (FR9.3): Subscribes to ACTION_CLOSED_LOOP_ACK broadcast; updates RTT live on UI thread.
│    │    │    └── ⚙️ [BE]₉․₈․₁․₁: TelemetryClosedLoopSentinel Broadcast Receiver
│    │    │         └── 🗄️ [DB]₉․₈․₁․₁․₁: `device_vitals.ack_timestamp` & `device_vitals.ack_latency_ms`
│    ├── 🖱️ [EL]₉․₉: Section 3 Header: `OPERATIONAL STATUS`
│    │    └── 🖥️ [FE]₉․₉․₁: Section Divider Header
│    ├── 🖱️ [EL]₉․₁₀: Operating Duty Cycle Mode Tile
│    │    ├── 🖥️ [FE]₉․₁₀․₁: Status Tile showing active duty mode: Active Motion (5s), Stationary (60s), Dormant (300s)
│    │    │    └── ⚙️ [BE]₉․₁₀․₁․₁: TrackingDutyCycleManager
│    ├── 🖱️ [EL]₉․₁₁: GPS Precision & Satellite Fix Age Tile
│    │    ├── 🖥️ [FE]₉․₁₁․₁: GPS Metrics: Accuracy radius in meters, locked satellite count, fix age in seconds
│    │    │    └── ⚙️ [BE]₉․₁₁․₁․₁: GnssStatus.Callback & LocationManager
│    ├── 🖱️ [EL]₉․₁₂: Section 4 Header: `TRACKING PULSE`
│    │    └── 🖥️ [FE]₉․₁₂․₁: Section Divider Header
│    ├── 🖱️ [EL]₉․₁₃: Tracking Frequency Tuning Card
│    │    ├── 🖥️ [FE]₉․₁₃․₁: Clickable Card (`freqCard`) launching interval dialog picker (5s to 300s)
│    │    │    └── ⚙️ [BE]₉․₁₃․₁․₁: RegistryManager.setTrackingIntervalMs
│    │    │         └── 🗄️ [DB]₉․₁₃․₁․₁․₁: `system_registry.xml` (`pref_tracking_interval`)
│    ├── 🖱️ [EL]₉․₁₄: Section 5 Header: `DISPLAY UNITS`
│    │    └── 🖥️ [FE]₉․₁₄․₁: Section Divider Header
│    ├── 🖱️ [EL]₉․₁₅: Metric vs Imperial Unit Toggle Tile
│    │    ├── 🖥️ [FE]₉․₁₅․₁: Switch Tile: Metric (km/h, meters) vs Imperial (mph, feet)
│    │    │    └── ⚙️ [BE]₉․₁₅․₁․₁: UnitConversionManager
│    │    │         └── 🗄️ [DB]₉․₁₅․₁․₁․₁: `system_registry.xml` (`pref_unit_metric`)
│    ├── 🖱️ [EL]₉․₁₆: Section 6 Header: `DEEP HARDWARE VITALS`
│    │    └── 🖥️ [FE]₉․₁₆․₁: Section Divider Header
│    ├── 🖱️ [EL]₉․₁₇: CPU, RAM & Storage Diagnostics Tile
│    │    ├── 🖥️ [FE]₉․₁₇․₁: Hardware Specs: ABI (arm64-v8a), RAM total/free MB, Internal Storage available GB
│    │    │    └── ⚙️ [BE]₉․₁₇․₁․₁: ActivityManager.MemoryInfo & StatFs
│    └── 🖱️ [EL]₉․₁₈: Security Attestation Badges (Root & Mock GPS)
│         ├── 🖥️ [FE]₉․₁₈․₁: Dual Badges: Root Status (Green/Red) and Mock GPS Status (Green/Red)
│         │    └── ⚙️ [BE]₉․₁₈․₁․₁: SecurityReport.evaluateRootStatus
│         │         ├── 🗄️ [DB]₉․₁₈․₁․₁․₁: `device_vitals.is_rooted`
│         │         └── 🗄️ [DB]₉․₁₈․₁․₁․₂: `device_vitals.is_mock_gps`
```

---

#### 10. `WatchDogAuditActivity` — Heartbeat Watchdog, Battery Drain & ANR Forensic
```text
ROOT: WATCHDOG_AUDIT_MASTER_PLAN (com.jdroidx.jfindme.ui.management.WatchDogAuditActivity)
│
├── 💼 [BR]₁: Service Uptime Assurance, Watchdog Pings & WakeLock Audit
│    ├── 🖱️ [EL]₁₀․₁: Global Header Bar & Back Button
│    │    ├── 🖥️ [FE]₁₀․₁․₁: Header Bar with Back Button and Title "WATCHDOG SERVICE AUDIT"
│    │    │    └── ⚙️ [BE]₁₀․₁․₁․₁: BaseActivityJava Header Setup
│    ├── 🖱️ [EL]₁₀․₂: 24-Hour Service Uptime Radial Gauge
│    │    ├── 🖥️ [FE]₁₀․₂․₁: Animated Circular Progress Indicator (% service availability)
│    │    │    ├── (FR10.1): Fills green for >= 98% uptime, amber for 90-97%, red for < 90%.
│    │    │    └── ⚙️ [BE]₁₀․₂․₁․₁: LocationWatchdogWorker.getHeartbeatLog
│    │    │         └── 🗄️ [DB]₁₀․₂․₁․₁․₁: `system_registry.xml` (`last_watchdog_heartbeat`)
│    ├── 🖱️ [EL]₁₀․₃: Scheduled Heartbeat Ping Recycler List
│    │    ├── 🖥️ [FE]₁₀․₃․₁: Chronological Ping Items (15-min intervals, status OK/RECOVERED)
│    │    │    └── ⚙️ [BE]₁₀․₃․₁․₁: HeartbeatHistoryProvider
│    ├── 🖱️ [EL]₁₀․₄: Battery Drain Impact Trend Line Chart
│    │    ├── 🖥️ [FE]₁₀․₄․₁: Hourly Battery Consumption Curve (%/hr during foreground tracking)
│    │    │    └── ⚙️ [BE]₁₀․₄․₁․₁: BatteryStatsReader
│    │    │         └── 🗄️ [DB]₁₀․₄․₁․₁․₁: `device_vitals` (battery_level, timestamp)
│    ├── 🖱️ [EL]₁₀․₅: Partial WakeLock Duration Counter
│    │    ├── 🖥️ [FE]₁₀․₅․₁: WakeLock Metrics: Total ms held, background execution efficiency score
│    │    │    └── ⚙️ [BE]₁₀․₅․₁․₁: WakeLockTracker
│    └── 🖱️ [EL]₁₀․₆: Emergency Core Restart Action Button
│         ├── 🖥️ [FE]₁₀․₆․₁: "Restart Core Tracking Engine" Force-Run Button
│         │    └── ⚙️ [BE]₁₀․₆․₁․₁: TrackingService.restartServiceForcefully
│         │         └── 🗄️ [DB]₁₀․₆․₁․₁․₁: `audit_log.watchdog_force_restart`
```

---

#### 11. `SystemLogActivity` — Live Kernel, Baseband & Telemetry Stream Log
```text
ROOT: SYSTEM_LOG_MASTER_PLAN (com.jdroidx.jfindme.ui.management.SystemLogActivity)
│
├── 💼 [BR]₁: Runtime Logcat Stream Inspection & Diagnostic Buffer Management
│    ├── 🖱️ [EL]₁₁․₁: Global Header Bar & Log Count Badge
│    │    ├── 🖥️ [FE]₁₁․₁․₁: Title "SYSTEM AUDIT LOGS", Subtitle "7-Day Forensic Log Matrix", Total Log Count
│    │    │    └── ⚙️ [BE]₁₁․₁․₁․₁: BaseActivityJava Header
│    ├── 🖱️ [EL]₁₁․₂: Log Level Filter Segmented Pills
│    │    ├── 🖥️ [FE]₁₁․₂․₁: Filter Pills (`pill`): [ALL], [DEBUG], [INFO], [WARN], [ERROR]
│    │    │    └── ⚙️ [BE]₁₁․₂․₁․₁: SystemLogger In-Memory Predicate Filter
│    ├── 🖱️ [EL]₁₁․₃: Subsystem Tag Search Field
│    │    ├── 🖥️ [FE]₁₁․₃․₁: Outlined Search EditText filtering tags (TrackingService, Sentinel, CellTower, Router)
│    │    │    └── ⚙️ [BE]₁₁․₃․₁․₁: TagStringMatcher
│    ├── 🖱️ [EL]₁₁․₄: High-Performance Monospace Terminal Console
│    │    ├── 🖥️ [FE]₁₁․₄․₁: Monospace ScrollView with Color-Coded Log Lines (Green=D, Blue=I, Yellow=W, Red=E)
│    │    │    └── ⚙️ [BE]₁₁․₄․₁․₁: SystemLogger Ring Buffer (Max 2,000 in-memory lines)
│    │    │         └── 🗄️ [DB]₁₁․₄․₁․₁․₁: Circular Buffer `system_logger_ring_buffer`
│    ├── 🖱️ [EL]₁₁․₅: Auto-Scroll to Bottom Toggle Switch
│    │    ├── 🖥️ [FE]₁₁․₅․₁: Switch toggling auto-scrolling on new log ticks
│    │    │    └── ⚙️ [BE]₁₁․₅․₁․₁: ScrollViewStateController
│    └── 🖱️ [EL]₁₁․₆: Action Toolbar (Copy / Clear / Share)
│         ├── 🖥️ [FE]₁₁․₆․₁: 3 Action Icons: [Copy to Clipboard], [Clear Buffer], [Share Log File]
│         │    └── ⚙️ [BE]₁₁․₆․₁․₁: ClipboardManager & DiagnosticLogFileWriter
│         │         └── 🗄️ [DB]₁₁․₆․₁․₁․₁: Cache File `cache/system_log_{epoch}.txt`
```

---

#### 12. `BackgroundJobsConfigDialog` — 13-Worker Scheduler & Runtime Frequency Tuning
```text
ROOT: BACKGROUND_JOBS_MASTER_PLAN (com.jdroidx.jfindme.ui.settings.BackgroundJobsConfigDialog)
│
├── 💼 [BR]₁: WorkManager 13-Worker Orchestration & Dynamic Frequency Tuning
│    ├── 🖱️ [EL]₁₂․₁: Dialog Header Bar & Close Action
│    │    ├── 🖥️ [FE]₁₂․₁․₁: Title "BACKGROUND JOBS & SENTINELS", Close Button (`closeBtn`)
│    │    │    └── ⚙️ [BE]₁₂․₁․₁․₁: DialogFragment Header
│    ├── 🖱️ [EL]₁₂․₂: Registered 13-Worker Recycler Deck
│    │    ├── 🖥️ [FE]₁₂․₂․₁: 13 Job Cards:
│    │    │    │    1. `JOB_TRACKING_SERVICE` (Foreground Telemetry Engine)
│    │    │    │    2. `JOB_LOCATION_WATCHDOG` (Heartbeat Uptime Sentinel)
│    │    │    │    3. `JOB_GEOFENCE_SENTINEL` (Perimeter Breach Engine)
│    │    │    │    4. `JOB_SIM_FORENSIC` (Baseband & SIM Swap Sentinel)
│    │    │    │    5. `JOB_SECURITY_RECEIVER` (Tamper & Root Guard)
│    │    │    │    6. `JOB_ACOUSTIC_SIREN` (Emergency Audio Beacon)
│    │    │    │    7. `JOB_PRESENCE_PULSE` (Online Presence Heartbeat)
│    │    │    │    8. `JOB_OFFLINE_SYNC` (Store-and-Forward Buffer Flusher)
│    │    │    │    9. `JOB_FCM_MESSAGING` (Cloud Downlink Push Router)
│    │    │    │    10. `JOB_LIVE_MAP_STREAM` (Real-Time Mesh Streamer)
│    │    │    │    11. `JOB_SILENT_UPDATE` (OTA Binary Signature Check)
│    │    │    │    12. `JOB_APP_UPGRADE_WATCH` (Database Migration Sentinel)
│    │    │    │    13. `JOB_STORAGE_PRUNE` (Rolling Storage Prune & VACUUM)
│    │    │    └── ⚙️ [BE]₁₂․₂․₁․₁: BackgroundJobManager.getRegisteredJobs() Provider
│    │    │         └── 🗄️ [DB]₁₂․₂․₁․₁․₁: `system_registry.xml` (`pref_*_active`, `pref_last_run_*`)
│    ├── 🖱️ [EL]₁₂․₃: Per-Job Runtime Toggle Switch
│    │    ├── 🖥️ [FE]₁₂․₃․₁: Switch (`toggleSwitch`) enabling/disabling worker execution at runtime
│    │    │    └── ⚙️ [BE]₁₂․₃․₁․₁: BackgroundJobManager Worker Cancellation / Rescheduling Engine
│    ├── 🖱️ [EL]₁₂․₄: Per-Job Frequency Edit Action
│    │    ├── 🖥️ [FE]₁₂․₄․₁: Edit Button (`editBtn`) launching interval picker dialog (e.g. 15m, 30m, 60m, 120m)
│    │    │    ├── (FR12.1): Clamps periodic intervals to Android WorkManager 15-minute system minimum.
│    │    │    └── ⚙️ [BE]₁₂․₄․₁․₁: BackgroundJobManager.rescheduleWorker
│    ├── 🖱️ [EL]₁₂․₅: Per-Job "Run Now" Manual Force Trigger Action
│    │    ├── 🖥️ [FE]₁₂․₅․₁: Action Button (`runNowBtn`) dispatching immediate OneTimeWorkRequest
│    │    │    └── ⚙️ [BE]₁₂․₅․₁․₁: BackgroundJobManager.triggerJobNow(jobId)
│    │    │         └── 🗄️ [DB]₁₂․₅․₁․₁․₁: WorkManager SQLite `androidx.work.workdatabase`
│    └── 🖱️ [EL]₁₂․₆: WorkManager Execution State Micro-Badge
│         ├── 🖥️ [FE]₁₂․₆․₁: Status Pill (ENQUEUED = Blue, RUNNING = Emerald, FAILED = Red, BLOCKED = Amber)
│         │    └── ⚙️ [BE]₁₂․₆․₁․₁: WorkManager.getInstance(context).getWorkInfosByTagLiveData
```

---

### Domain 4: Breadcrumb Chronology, Geofence & Emergency Beacons

#### 13. `HistoryActivity` — Chronological Breadcrumbs, Temporal Replay & Heatmap
```text
ROOT: HISTORY_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.management.HistoryActivity)
│
├── 💼 [BR]₁: Forensic Breadcrumb Trail Reconstruction, Replay & Heatmap Analytics
│    ├── 🖱️ [EL]₁₃․₁: Global Header Bar & Back Button
│    │    ├── 🖥️ [FE]₁₃․₁․₁: Back Button, Title "Historical GPS Breadcrumbs & Action Replay"
│    │    │    └── ⚙️ [BE]₁₃․₁․₁․₁: BaseActivityJava Header
│    ├── 🖱️ [EL]₁₃․₂: Target Subject Selector Dropdown (OOPS MVC SSOT)
│    │    ├── 🖥️ [FE]₁₃․₂․₁: Dropdown Card (`targetSelectorWrap`) with Member Avatar & Name
│    │    ├── 🖥️ [FE]₁₃․₂․₂: Rich Options Selection Dialog (`UiArchitect.showRichOptionsDialog`)
│    │    │    ├── (FR13.1): Host Device (Self) anchored dynamically at index 0 (`👑 My Device (Host)`).
│    │    │    ├── (FR13.2): Hierarchical Role Priority: Protected Ward / Child (`4379A761` / Riddhi) prioritized first, followed by Guardian, Family, and Peers.
│    │    │    ├── (FR13.3): Strict SSOT Scoping: Excludes `UNLINKED`, `DELETED`, `HW_%` synthetic prefixes, and decommissioned ghost nodes.
│    │    │    ├── (FR13.4): Anti-Pollution Invariant: Zero blind scanning of cloud `/devices` tree; resolves strictly from local SSOT relations.
│    │    │    ├── Pass Scenario: Dialog lists exactly Host + active connected circle members with clean roles; index 0 selects host track.
│    │    │    └── Fail Scenario: Corrupted or unlinked device ID detected -> filtered out before UI rendering.
│    │    │    └── ⚙️ [BE]₁₃․₂․₁․₁: `HistoryViewModel.fetchTrackingTargets(context)` OOPS MVC Target Resolver
│    │    │         ├── 🗄️ [DB]₁₃․₂․₁․₁․₁: `user_identity` Table (`status NOT IN ('DELETED', 'UNLINKED') AND uid_1 NOT LIKE 'HW_%'`)
│    │    │         ├── 🗄️ [DB]₁₃․₂․₁․₁․₂: `device_pairing` Table (`pair_status IN ('PAIRED', 'ACTIVE')`)
│    │    │         └── 🗄️ [DB]₁₃․₂․₁․₁․₃: `group_members` Table (`status NOT IN ('DELETED', 'UNLINKED') AND agent_id NOT LIKE 'HW_%'`)
│    ├── 🖱️ [EL]₁₃․₃: Temporal Date-Range Selector Header
│    │    ├── 🖥️ [FE]₁₃․₃․₁: Preset Filter Chips (`tile`: Today, Yesterday, 7 Days, Custom Date Picker `etDateRange`)
│    │    │    └── ⚙️ [BE]₁₃․₃․₁․₁: DateRangeEpochCalculator
│    ├── 🖱️ [EL]₁₃․₄: Section 1 Header: `REPLAY ENGINE CONTROLS`
│    │    └── 🖥️ [FE]₁₃․₄․₁: Section Divider Header
│    ├── 🖱️ [EL]₁₃․₅: Timeline Scrubber Slider
│    │    ├── 🖥️ [FE]₁₃․₅․₁: Smooth SeekBar (`seekBarTimeline`) scrubbing through chronological waypoints
│    │    │    └── ⚙️ [BE]₁₃․₅․₁․₁: TimelineScrubberController
│    ├── 🖱️ [EL]₁₃․₆: Play / Pause Replay Action Button
│    │    ├── 🖥️ [FE]₁₃․₆․₁: Primary FAB (`playBtn`) toggling smooth automated waypoint animation
│    │    │    └── ⚙️ [BE]₁₃․₆․₁․₁: TimelinePlaybackAnimator (16ms Choreographer loop)
│    ├── 🖱️ [EL]₁₃․₇: Stop & Reset Action Button
│    │    ├── 🖥️ [FE]₁₃․₇․₁: Secondary Action Button (`stopBtn`) resetting scrubber to waypoint index 0
│    │    │    └── ⚙️ [BE]₁₃․₇․₁․₁: AnimatorResetController
│    ├── 🖱️ [EL]₁₃․₈: Speed Multiplier Stepper Button
│    │    ├── 🖥️ [FE]₁₃․₈․₁: Speed Button (`btnSpd`) cycling replay playback speed: [1x], [2x], [5x], [10x]
│    │    │    └── ⚙️ [BE]₁₃․₈․₁․₁: PlaybackSpeedMultiplier
│    ├── 🖱️ [EL]₁₃․₉: Animated Replay Moving Target Marker
│    │    ├── 🖥️ [FE]₁₃․₉․₁: Map Marker with Directional Heading Arrow animating along historical path
│    │    │    └── ⚙️ [BE]₁₃․₉․₁․₁: LatLngInterpolator.Spherical
│    ├── 🖱️ [EL]₁₃․₁₀: Gradient Velocity Polyline Map Layer
│    │    ├── 🖥️ [FE]₁₃․₁₀․₁: Map Polyline color-coded by velocity (Green: 0-20km/h, Amber: 21-60km/h, Red: >60km/h)
│    │    │    └── ⚙️ [BE]₁₃․₁₀․₁․₁: PolylineSimplifier (Ramer-Douglas-Peucker algorithm)
│    │    │         └── 🗄️ [DB]₁₃․₁₀․₁․₁․₁: `tracking_history` (latitude, longitude, speed, timestamp)
│    ├── 🖱️ [EL]₁₃․₁₁: Waypoint Hover / Tap Popup Card
│    │    ├── 🖥️ [FE]₁₃․₁₁․₁: Information Window: Exact Timestamp, Speed, Accuracy, Reverse-Geocoded Landmark
│    │    │    └── ⚙️ [BE]₁₃․₁₁․₁․₁: GeocodingCacheManager.getReverseGeocode
│    │    │         └── 🗄️ [DB]₁₃․₁₁․₁․₁․₁: `reverse_geocode_cache`
│    ├── 🖱️ [EL]₁₃․₁₂: Section 2 Header: `DETAILED LOCATION LOGS`
│    │    └── 🖥️ [FE]₁₃․₁₂․₁: Section Divider Header
│    ├── 🖱️ [EL]₁₃․₁₃: Chronological Waypoint Recycler List
│    │    ├── 🖥️ [FE]₁₃․₁₃․₁: Recycler List displaying waypoint time cards, speed, and accuracy
│    │    │    └── ⚙️ [BE]₁₃․₁₃․₁․₁: LocationRepository.getHistoryBetweenEpochs
│    │    │         └── 🗄️ [DB]₁₃․₁₃․₁․₁․₁: `tracking_history` (ORDER BY timestamp ASC)
│    └── 🖱️ [EL]₁₃․₁₄: Export History Action FAB
│         ├── 🖥️ [FE]₁₃․₁₄․₁: "Export Track" FAB launching format picker: [GPX], [KML], [CSV]
│         │    └── ⚙️ [BE]₁₃․₁₄․₁․₁: TrackFileExporter (GpxSerializer / KmlSerializer)
│         │         └── 🗄️ [DB]₁₃․₁₄․₁․₁․₁: Cache File `export/track_{epoch}.gpx`
```

---

#### 14. `GeofenceActivity` — Safe-Zone Perimeter Architect & Multi-Polygon Builder
```text
ROOT: GEOFENCE_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.management.GeofenceActivity)
│
├── 💼 [BR]₁: Safe-Zone Geospatial Perimeter Definition & Autonomous Boundary Governance
│    ├── 🖱️ [EL]₁₄․₁: Global Header Bar & Back Button
│    │    ├── 🖥️ [FE]₁₄․₁․₁: Back Button, Title "SAFE ZONES", Subtitle "Perimeters & Radar Boundaries"
│    │    │    └── ⚙️ [BE]₁₄․₁․₁․₁: BaseActivityJava Header
│    ├── 🖱️ [EL]₁₄․₂: Mode Switcher Tabs
│    │    ├── 🖥️ [FE]₁₄․₂․₁: Top Tabs: [Map Perimeter Architect] vs [Active Zone Roster]
│    │    │    └── ⚙️ [BE]₁₄․₂․₁․₁: TabViewSwitcher
│    ├── 🖱️ [EL]₁₄․₃: Section 1 Header: `ESTABLISH SAFE ZONE DOMAIN`
│    │    └── 🖥️ [FE]₁₄․₃․₁: Section Divider Header
│    ├── 🖱️ [EL]₁₄․₄: Safe Zone Name Input Field
│    │    ├── 🖥️ [FE]₁₄․₄․₁: Outlined EditText (`etZoneName`) with Character Limit (2-40 chars)
│    │    │    └── ⚙️ [BE]₁₄․₄․₁․₁: SafeZoneNameValidator
│    │    │         └── 🗄️ [DB]₁₄․₄․₁․₁․₁: `safe_zones.name`
│    ├── 🖱️ [EL]₁₄․₅: Perimeter Radius Slider & Stepper
│    │    ├── 🖥️ [FE]₁₄․₅․₁: Discrete Slider & Value Display (`btnR`): Range 50m to 5000m
│    │    │    ├── (FR14.1): Minimum radius clamped to 50m to prevent GPS jitter bounce triggers.
│    │    │    └── ⚙️ [BE]₁₄․₅․₁․₁: RadiusValueChangeListener
│    │    │         └── 🗄️ [DB]₁₄․₅․₁․₁․₁: `safe_zones.radius_meters`
│    ├── 🖱️ [EL]₁₄․₆: Category / Icon Picker
│    │    ├── 🖥️ [FE]₁₄․₆․₁: Icon Palette (`ivEdit`): [Home], [School], [Office], [Hospital], [Custom]
│    │    │    └── ⚙️ [BE]₁₄․₆․₁․₁: CategoryIconMapper
│    │    │         └── 🗄️ [DB]₁₄․₆․₁․₁․₁: `safe_zones.icon_type`
│    ├── 🖱️ [EL]₁₄․₇: Active Schedule Time Picker
│    │    ├── 🖥️ [FE]₁₄․₇․₁: Schedule Dialog (`btnSched`, start time, end time `edtEnd`, active days selector)
│    │    │    └── ⚙️ [BE]₁₄․₇․₁․₁: ScheduleTimeValidator
│    │    │         └── 🗄️ [DB]₁₄․₇․₁․₁․₁: `safe_zones.active_schedule_json`
│    ├── 🖱️ [EL]₁₄․₈: Target Member Assignment Card
│    │    ├── 🖥️ [FE]₁₄․₈․₁: Member Selector Dropdown (`targetNodeCard`): [All Circle Members] vs [Specific Member]
│    │    │    └── ⚙️ [BE]₁₄․₈․₁․₁: MasterRosterMemberAdapter
│    │    │         └── 🗄️ [DB]₁₄․₈․₁․₁․₁: `safe_zones.assigned_agent_id`
│    ├── 🖱️ [EL]₁₄․₉: Interactive Map Viewport & Radius Handle
│    │    ├── 🖥️ [FE]₁₄․₉․₁: Draggable Center Marker with Dynamic Translucent Circle Overlay and Drag Handle
│    │    │    └── ⚙️ [BE]₁₄․₉․₁․₁: GoogleMap / Leaflet CircleOverlay Controller
│    ├── 🖱️ [EL]₁₄․₁₀: Section 2 Header: `ACTIVE RADAR BOUNDARIES`
│    │    └── 🖥️ [FE]₁₄․₁₀․₁: Section Divider Header
│    ├── 🖱️ [EL]₁₄․₁₁: Active Safe Zones Recycler List
│    │    ├── 🖥️ [FE]₁₄․₁₁․₁: Zone Card (`itemRow`): Zone Title, Radius, Bound Member, Active Toggle Switch
│    │    │    └── ⚙️ [BE]₁₄․₁₁․₁․₁: GeofenceRepository.getAllSafeZones LiveData
│    │    │         └── 🗄️ [DB]₁₄․₁₁․₁․₁․₁: `safe_zones` (zone_id, name, latitude, longitude, radius_meters, is_active)
│    ├── 🖱️ [EL]₁₄․₁₂: Edit Zone Action Button
│    │    ├── 🖥️ [FE]₁₄․₁₂․₁: Edit Icon (`ivEdit`) loading safe zone into architect viewport for modification
│    │    │    └── ⚙️ [BE]₁₄․₁₂․₁․₁: GeofenceEditLoader
│    ├── 🖱️ [EL]₁₄․₁₃: Delete Zone Danger Action Button
│    │    ├── 🖥️ [FE]₁₄․₁₃․₁: Trash Icon (`ivDel`) prompting deletion confirmation dialog
│    │    │    └── ⚙️ [BE]₁₄․₁₃․₁․₁: GeofenceManager.unregisterGeofence & DatabaseHelper.deleteSafeZone
│    │    │         ├── 🗄️ [DB]₁₄․₁₃․₁․₁․₁: `safe_zones` (DELETE WHERE zone_id = ?)
│    │    │         └── 🗄️ [DB]₁₄․₁₃․₁․₁․₂: Cloud Firebase `/safe_zones/{zone_id}`
│    ├── 🖱️ [EL]₁₄․₁₄: Search & Filter Toolbar
│    │    ├── 🖥️ [FE]₁₄․₁₄․₁: Search Bar (`btnSearch`) with Clear Icon (`btnSearchClear`)
│    │    │    └── ⚙️ [BE]₁₄․₁₄․₁․₁: SafeZoneSearchFilter
│    └── 🖱️ [EL]₁₄․₁₅: Purge Stale Geofences Action
│         ├── 🖥️ [FE]₁₄․₁₅․₁: Purge Button (`btnPurge`, `btnLogs`) deleting unassigned or expired safe-zones
│         │    └── ⚙️ [BE]₁₄․₁₅․₁․₁: GeofencePurgeService
```

---

#### 15. `GeofenceNotificationActivity` — Perimeter Breach Alert & Dormancy Latch Handler
```text
ROOT: GEOFENCE_NOTIFICATION_MASTER_PLAN (com.jdroidx.jfindme.ui.management.GeofenceNotificationActivity)
│
├── 💼 [BR]₁: Breach Incident Triage, Audio Warning & Silicon Latch Verification
│    ├── 🖱️ [EL]₁₅․₁: Header Bar & Batch Action Buttons
│    │    ├── 🖥️ [FE]₁₅․₁․₁: Back Button (`btnBack`, `back`), Title "MISSION NOTIFICATIONS", Clear All (`btnClear`), Export (`btnExport`)
│    │    │    └── ⚙️ [BE]₁₅․₁․₁․₁: BaseActivityJava Header
│    ├── 🖱️ [EL]₁₅․₂: Category Filter Tabs
│    │    ├── 🖥️ [FE]₁₅․₂․₁: Horizontal Tabs (`tab`): [All Alerts], [Perimeter Breaches], [SOS Panic], [Hardware Vitals]
│    │    │    └── ⚙️ [BE]₁₅․₂․₁․₁: NotificationCategoryFilter
│    ├── 🖱️ [EL]₁₅․₃: Breach Incident Card Recycler
│    │    ├── 🖥️ [FE]₁₅․₃․₁: Incident Item (`row`): Pulsing Badge, Transition Tag (ENTER = Emerald, EXIT = Crimson), Subject Name, Zone Name, Timestamp
│    │    │    ├── (FR15.1): Breach notifications debounced by 180s via GeofenceBroadcastReceiver silicon latch key.
│    │    │    └── ⚙️ [BE]₁₅․₃․₁․₁: GeofenceBroadcastReceiver.processGeofenceTransition
│    │    │         ├── 🗄️ [DB]₁₅․₃․₁․₁․₁: `alert_history` (alert_id, zone_id, agent_id, transition_type, epoch_ms)
│    │    │         └── 🗄️ [DB]₁₅․₃․₁․₁․₂: `system_registry.xml` (`GEOFENCE_LATCH_STATE_*`)
│    ├── 🖱️ [EL]₁₅․₄: "Locate on Map" Action Button
│    │    ├── 🖥️ [FE]₁₅․₄․₁: Map Target Icon (`ivMap`) launching MapActivity centered on exact breach coordinates
│    │    │    └── ⚙️ [BE]₁₅․₄․₁․₁: Intent Launcher for MapActivity with extra `target_lat`, `target_lon`
│    └── 🖱️ [EL]₁₅․₅: Silence Audio & Acknowledge Alert Button
│         ├── 🖥️ [FE]₁₅․₅․₁: "Acknowledge" Button stopping alert chime and marking notification as read
│         │    └── ⚙️ [BE]₁₅․₅․₁․₁: AlertHistoryRepository.markAsRead
│         │         └── 🗄️ [DB]₁₅․₅․₁․₁․₁: `alert_history.is_read` (UPDATE is_read = 1)
```

---

#### 16. `AcousticSirenActivity` — Maximum-Volume Emergency Audio Beacon & Strobe
```text
ROOT: ACOUSTIC_SIREN_MASTER_PLAN (com.jdroidx.jfindme.ui.alert.AcousticSirenActivity)
│
├── 💼 [BR]₁: Emergency Audio Beacon, High-Intensity Visual Strobe & Secure Disarm
│    ├── 🖱️ [EL]₁₆․₁: Full-Screen Alternating Strobe Canvas
│    │    ├── 🖥️ [FE]₁₆․₁․₁: High-Contrast Alternating Screen Flash (Pure Crimson / Pure White at 4Hz)
│    │    │    ├── (FR16.1): Sets WindowManager.LayoutParams.screenBrightness to 1.0f (maximum).
│    │    │    └── ⚙️ [BE]₁₆․₁․₁․₁: ScreenStrobeAnimationController
│    ├── 🖱️ [EL]₁₆․₂: Camera Flashlight Torch Strobe
│    │    ├── 🖥️ [FE]₁₆․₂․₁: Physical Hardware Flashlight pulsing synchronously with screen flash
│    │    │    └── ⚙️ [BE]₁₆․₂․₁․₁: CameraManager.setTorchMode(cameraId, true/false) Looper
│    ├── 🖱️ [EL]₁₆․₃: Maximum-Decibel Acoustic Siren Player
│    │    ├── 🖥️ [FE]₁₆․₃․₁: Pulsing Decibel Waveform Indicator
│    │    │    ├── (FR16.2): Sets AudioManager.STREAM_ALARM to 100% volume; overrides DND mode.
│    │    │    └── ⚙️ [BE]₁₆․₃․₁․₁: MediaPlayer Emergency Sweep Looper (`R.raw.emergency_siren_sweep`)
│    ├── 🖱️ [EL]₁₆․₄: Initiator Emergency Alert Banner
│    │    ├── 🖥️ [FE]₁₆․₄․₁: Banner Card displaying: Initiator Name, Initiator Hardware ID, Trigger Epoch, Active Duration
│    │    │    └── ⚙️ [BE]₁₆․₄․₁․₁: Intent Extra Payload Parser (initiator, durationSec)
│    ├── 🖱️ [EL]₁₆․₅: 4-Digit Security Disarm PIN Keypad
│    │    ├── 🖥️ [FE]₁₆․₅․₁: 4 Circle Pin Dots and Numeric Keypad (0-9)
│    │    │    ├── (FR16.3): Siren continues indefinitely until correct master PIN is verified or session expires.
│    │    │    └── ⚙️ [BE]₁₆․₅․₁․₁: MasterPinVerifier
│    │    │         └── 🗄️ [DB]₁₆․₅․₁․₁․₁: `system_registry.xml` (`master_pin_hash`)
│    └── 🖱️ [EL]₁₆․₆: Dismiss / Disarm Siren Action Button
│         ├── 🖥️ [FE]₁₆․₆․₁: Dismiss Action Button (`btnDismiss`) enabled only upon valid PIN entry
│         │    └── ⚙️ [BE]₁₆․₆․₁․₁: AcousticSirenEngine.stopSiren & TorchMode Deactivator
│         │         └── 🗄️ [DB]₁₆․₆․₁․₁․₁: `audit_log.emergency_siren_dismissed`
```


### Domain 5: Fleet Governance, Enterprise Organization & Multi-Node Mesh

#### 17. `GroupActivity` — Legacy Group Router & Roster Controller
```text
ROOT: GROUP_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.group.GroupActivity)
│
├── 💼 [BR]₁: Legacy Group Traffic Routing & Fleet Controller Disptach
│    ├── 🖱️ [EL]₁₇․₁: Group Gateway Dispatch Controller
│    │    ├── 🖥️ [FE]₁₇․₁․₁: Transparent Router Shell (evaluates saved group state and routes to unified roster)
│    │    │    ├── (FR17.1): If family/friends active in local DB, dispatch Intent -> FamilyFriendsActivity; else -> NetworkGatewayActivity.
│    │    │    └── ⚙️ [BE]₁₇․₁․₁․₁: Intent Router & Session Dispatcher
│    │    │         └── 🗄️ [DB]₁₇․₁․₁․₁․₁: `system_registry.xml` (`KEY_ACTIVE_GROUP_ID`)
│    └── 🖱️ [EL]₁₇․₂: Fail-Safe Group Recovery Bridge
│         └── 🖥️ [FE]₁₇․₂․₁: Autonomous Activity Finish & Lifecycle Cleanup
│              └── ⚙️ [BE]₁₇․₂․₁․₁: LifecycleManager.destroyRedundantActivity
```
*Testing Pass/Fail Scenario (FR17.1 / BFR17.1)*:
- **PASS**: When `GroupActivity` launches with valid `KEY_ACTIVE_GROUP_ID`, intent cleanly transitions to `FamilyFriendsActivity` within 100ms with no UI flicker.
- **FAIL**: Launching with null or corrupt group ID throws an unhandled `ActivityNotFoundException` or leaves a blank transparent screen.

---

#### 18. `FamilyFriendsActivity` — Unified Social Fleet, Privacy Tiers & Member Node Management
```text
ROOT: FAMILY_FRIENDS_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.group.FamilyFriendsActivity)
│
├── 💼 [BR]₁: Social Fleet Hierarchy & Circle Partitioning
│    ├── 🖱️ [EL]₁₈․₁: Global Navigation Bar & Back Button
│    │    ├── 🖥️ [FE]₁₈․₁․₁: Back Chevron (`backBtn`), Screen Title "FLEET & CIRCLE ROSTER", Subtitle "Social Topology"
│    │    │    └── ⚙️ [BE]₁₈․₁․₁․₁: BaseActivity Navigation Lifecycle
│    ├── 🖱️ [EL]₁₈․₂: Privacy Tier Selector Action Button (`btnPrivacy`)
│    │    ├── 🖥️ [FE]₁₈․₂․₁: Shield Icon Button & Dialog Launcher (`showPrivacyTierDialog`)
│    │    │    ├── (FR18.1): Opens Modal Dialog displaying 3 Privacy Tiers: STEALTH (fuzzed 500m), BALANCED (100m), PRECISION (raw GPS).
│    │    │    └── ⚙️ [BE]₁₈․₂․₁․₁: PrivacyManager.setGlobalCirclePrivacyTier
│    │    │         └── 🗄️ [DB]₁₈․₂․₁․₁․₁: `system_registry.xml` (`KEY_PRIVACY_TIER`)
│    ├── 🖱️ [EL]₁₈․₃: Manual Cloud Sync Action Button (`btnSync`)
│    │    ├── 🖥️ [FE]₁₈․₃․₁: Rotating Arrows Icon Button; pulses emerald on tap during active network transmission
│    │    │    └── ⚙️ [BE]₁₈․₃․₁․₁: FirebaseComm.pullLatestCircleRoster
│    │    │         └── 🗄️ [DB]₁₈․₃․₁․₁․₁: Firebase RTDB `/groups/{groupId}/members`
│    ├── 🖱️ [EL]₁₈․₄: Add Member Action Button (`btnAdd`)
│    │    ├── 🖥️ [FE]₁₈․₄․₁: Plus Circle Icon Button (`showAddMemberOptionsDialog`)
│    │    │    └── ⚙️ [BE]₁₈․₄․₁․₁: Intent -> NetworkGatewayActivity (Tab 2 or Tab 3)
│    ├── 🖱️ [EL]₁₈․₅: Real-Time Member Search Bar & Clear Affordance
│    │    ├── 🖥️ [FE]₁₈․₅․₁: Text Input Box (`etSearchMember`) with search icon and clear button (`ivClearSearch`)
│    │    │    ├── (FR18.2): Real-time substring filter on Member Name, Alias, and Node UID. TextWatcher triggers filter in <= 50ms.
│    │    │    └── ⚙️ [BE]₁₈․₅․₁․₁: FamilyFriendsAdapter.filterRosterDataset
│    │    └── 🖥️ [FE]₁₈․₅․₂: Roster Sort Selector Icon (`sortIcon`)
│    │         └── ⚙️ [BE]₁₈․₅․₂․₁: Comparator Dispatcher (Sort by: Name, Battery Level, Last Seen RTT)
│    ├── 🖱️ [EL]₁₈․₆: Circle Partition Tab Bar (FAMILY vs FRIENDS)
│    │    ├── 🖥️ [FE]₁₈․₆․₁: Segmented Control Tabs (`tabFamily`, `tabFriends`) with neon cyan active underline indicator
│    │    │    ├── (FR18.3): Clicking `tabFamily` filters list to role == "FAMILY"; `tabFriends` filters to role == "FRIENDS".
│    │    │    └── ⚙️ [BE]₁₈․₆․₁․₁: CircleViewModel.setActivePartitionTab
│    │    │         └── 🗄️ [DB]₁₈․₆․₁․₁․₁: `group_members.role` & `group_members.circle_type`
│    ├── 🖱️ [EL]₁₈․₇: Unified Pairing Action Strip (`btnPairUnified`)
│    │    ├── 🖥️ [FE]₁₈․₇․₁: Full-Width Gradient Action Tile "PAIR NEW NODE / DEVICE"
│    │    │    └── ⚙️ [BE]₁₈․₇․₁․₁: Intent -> NetworkGatewayActivity
│    ├── 🖱️ [EL]₁₈․₈: Roster Reclamation Action Strip (`btnReclaimSubtle`)
│    │    ├── 🖥️ [FE]₁₈․₈․₁: Subtle Text Button "Reclaim Legacy Roster from Cloud"
│    │    │    └── ⚙️ [BE]₁₈․₈․₁․₁: CircleViewModel.triggerRosterReclamation
│    │    │         └── 🗄️ [DB]₁₈․₈․₁․₁․₁: `audit_log.roster_reclamation`
│    ├── 🖱️ [EL]₁₈․₉: Operator Self-Profile Card (`rootCard`)
│    │    ├── 🖥️ [FE]₁₈․₉․₁: Hero Card: Avatar Image, Display Name, Role Badge ("ADMIN"), Current Telemetry Status
│    │    │    └── ⚙️ [BE]₁₈․₉․₁․₁: Intent -> ProfileActivity
│    │    │         └── 🗄️ [DB]₁₈․₉․₁․₁․₁: `users.display_name`, `users.avatar_blob`
│    └── 🖱️ [EL]₁₈․₁₀: Dynamic Member Node Cards List (`rvMemberList`)
│         ├── 🖥️ [FE]₁₈․₁₀․₁: Member Row Card: Avatar Badge, Online Indicator (Green/Gray), Name, Last Known Location & Battery %
│         ├── 🖥️ [FE]₁₈․₁₀․₂: Tactical Map Locate Button (`locateBtn`)
│         │    └── ⚙️ [BE]₁₈․₁₀․₂․₁: Intent -> LiveMapDashboardActivity (Focus on Node Target Lat/Lng)
│         ├── 🖥️ [FE]₁₈․₁₀․₃: Node Context Action Button (`moreBtn`)
│         │    ├── (FR18.4): Opens Node Actions Dialog: Call, Acoustic Siren Trigger, Change Role, Device Sharing, Remove Member.
│         │    └── ⚙️ [BE]₁₈․₁₀․₃․₁: CircleViewModel.modifyMemberPermissions / purgeMemberNode
│         │         └── 🗄️ [DB]₁₈․₁₀․₃․₁․₁: `group_members` (DELETE or UPDATE role)
│         ├── 🖥️ [FE]₁₈․₁₀․₄: Triadic Device Sharing Action ("📱 Connect B to C")
│         │    ├── (FR18.5): Master A orchestrates connection between Device B and Device C with reciprocal role assignment (`Friend`, `Parent`, `Child`, `Spouse`, `Member`). If already linked, displays Re-sync vs Unlink triage dialog.
│         │    └── ⚙️ [BE]₁₈․₁₀․₄․₁: `PairingRepository.executeClosedLoopDeviceSharing` & `linkDevicesWithRoles`
│         │         ├── 🗄️ [Master DB [Online]]₁₈․₁₀․₄․₁․₁: Firebase RTDB `/relationships/{groupId}/direct_pairs/{B}/{C}`
│         │         ├── 🗄️ [Master DB [Offline]]₁₈․₁₀․₄․₁․₂: SQLite `device_sharing` (Status: `ACTIVE`, `is_active` = 1)
│         │         │    └── 🗄️ [Sub Master/Fact DB]₁₈․₁₀․₄․₁․₃: SQLite `device_pairing` (`PAIR_{B}_{C}`, `PAIR_{C}_{B}`)
│         │         │    │    └── 🗄️ [TransactionDB]₁₈․₁₀․₄․₁․₄: SQLite `device_sharing_transactions` (`TX_NONE_TO_REQUESTED`, `TX_REQUESTED_TO_ACTIVE`)
│         └── 🖥️ [FE]₁₈․₁₀․₅: Symmetrical Peer Revocation Action ("🗑️ Remove Device B from Device C")
│              ├── (FR18.6): Password-protected Master revocation of shared peer pairing without affecting Master's own connection or protected child ward.
│              └── ⚙️ [BE]₁₈․₁₀․₅․₁: `PairingRepository.unlinkPeerDevices`
│                   ├── 🗄️ [Master DB [Online]]₁₈․₁₀․₅․₁․₁: Firebase RTDB Direct Pair Nullification & Remote UNLINK Broadcast
│                   ├── 🗄️ [Master DB [Offline]]₁₈․₁₀․₅․₁․₂: SQLite `device_sharing` (Status: `REVOKED`, `is_active` = 0)
│                   │    └── 🗄️ [Sub Master/Fact DB]₁₈․₁₀․₅․₁․₃: SQLite `device_pairing` (Status: `UNLINKED`)
│                   │    │    └── 🗄️ [TransactionDB]₁₈․₁₀․₅․₁․₄: SQLite `device_sharing_transactions` (`TX_ACTIVE_TO_REVOKED`, Reason: "Device sharing unlinked by Master")
```
*Testing Pass/Fail Scenario (FR18.2 / BFR18.2)*:
- **PASS**: Entering "Alice" in `etSearchMember` filters 25 nodes down to 1 matching node in < 20ms without layout stutter.
- **FAIL**: Clearing search bar with `ivClearSearch` leaves adapter list in an empty or desynchronized state.

---

#### 19. `NetworkGatewayActivity` — Enterprise Multi-Mode Pairing & Mesh Topology Hub
```text
ROOT: NETWORK_GATEWAY_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.group.NetworkGatewayActivity)
│
├── 💼 [BR]₁: Multi-Transport Fleet Pairing & Cryptographic Handshake
│    ├── 🖱️ [EL]₁₉․₁: Global Navigation Bar & Network Sync Action
│    │    ├── 🖥️ [FE]₁₉․₁․₁: Back Chevron (`backBtn`), Screen Title "NETWORK GATEWAY", Sync Button (`btnSync`)
│    │    │    └── ⚙️ [BE]₁₉․₁․₁․₁: GatewayViewModel.syncAllTransports
│    ├── 🖱️ [EL]₁₉․₂: Master Mode Segmented Tabs (3-Tab Controller)
│    │    ├── 🖥️ [FE]₁₉․₂․₁: Tab 1: "ACTIVE ROSTER" (`tabRosterBtn`)
│    │    ├── 🖥️ [FE]₁₉․₂․₂: Tab 2: "REMOTE CLOUD" (`tabRemoteBtn`)
│    │    └── 🖥️ [FE]₁₉․₂․₃: Tab 3: "NEARBY P2P" (`tabNearbyBtn`)
│    │         ├── (FR19.1): Tab switching hides and shows relevant sub-view containers with zero state loss.
│    │         └── ⚙️ [BE]₁₉․₂․₃․₁: GatewayViewModel.switchToTab(tabIndex)
│    ├── 🖱️ [EL]₁₉․₃: Tab 2 (Remote Cloud) Host Engine Panel
│    │    ├── 🖥️ [FE]₁₉․₃․₁: "GENERATE PAIRING PIN" Action Button (`btnGenSquare`)
│    │    │    ├── (FR19.2): Generates secure 6-digit numeric PIN; spins up ephemeral Firebase session node.
│    │    │    └── ⚙️ [BE]₁₉․₃․₁․₁: FirebasePairingEngine.spinupHostSession(pin, ttlSeconds)
│    │    │         └── 🗄️ [DB]₁₉․₃․₁․₁․₁: Firebase RTDB `/pairing_sessions/{pin}`
│    │    ├── 🖥️ [FE]₁₉․₃․₂: Generated PIN Display Box (`tvCode`) with Monospace Font & Large Tracking
│    │    ├── 🖥️ [FE]₁₉․₃․₃: Copy PIN to Clipboard Button (`btnCopy`)
│    │    ├── 🖥️ [FE]₁₉․₃․₄: Share PIN via Android Sharesheet (`btnShare`)
│    │    └── 🖥️ [FE]₁₉․₃․₅: Session TTL Chip Indicator & Selector (`tvTtlChip`)
│    │         ├── (FR19.3): Displays remaining countdown (e.g. "04:59"); tap opens TTL selector dialog [5m, 15m, 1h].
│    │         └── ⚙️ [BE]₁₉․₃․₅․₁: CountDownTimer & SessionLifecycleManager
│    ├── 🖱️ [EL]₁₉․₄: Tab 2 (Remote Cloud) Join Engine Panel
│    │    ├── 🖥️ [FE]₁₉․₄․₁: Remote PIN Input Field (`etCode`) with 6-digit limit and auto-formatting
│    │    ├── 🖥️ [FE]₁₉․₄․₂: "LINK DEVICE" Action Button (`btnLink`)
│    │    │    ├── (FR19.4): Validates PIN format; queries Firebase RTDB; launches Role Selection Dialog [ADMIN, MEMBER, OBSERVER].
│    │    │    └── ⚙️ [BE]₁₉․₄․₂․₁: FirebasePairingEngine.consumeSession(pin, role)
│    │    │         └── 🗄️ [DB]₁₉․₄․₂․₁․₁: `group_members` (INSERT verified record)
│    │    └── 🖥️ [FE]₁₉․₄․₃: QR / Barcode Scanner Action Button (`btnScan`)
│    │         └── ⚙️ [BE]₁₉․₄․₃․₁: Intent -> ZXing / ML Kit Barcode Scanner
│    ├── 🖱️ [EL]₁₉․₅: Tab 3 (Nearby P2P) Local Engine Panel
│    │    ├── 🖥️ [FE]₁₉․₅․₁: "GENERATE LOCAL TOKEN" Button (`btnGenLocalSquare`)
│    │    ├── 🖥️ [FE]₁₉․₅․₂: Local Token Display Box (`tvLocalCode`) (8-character alphanumeric)
│    │    ├── 🖥️ [FE]₁₉․₅․₃: Local Copy & Share Buttons (`btnLocalCopy`, `btnLocalShare`)
│    │    ├── 🖥️ [FE]₁₉․₅․₄: Local TTL Chip Indicator (`tvLocalTtlChip`)
│    │    ├── 🖥️ [FE]₁₉․₅․₅: Local Token Entry Box (`etLocalCode`) & "LINK LOCAL" Button
│    │    └── 🖥️ [FE]₁₉․₅․₆: Local Barcode Scanner Button
│    │         └── ⚙️ [BE]₁₉․₅․₆․₁: WifiDirectSentinel / BluetoothLeAdvertiser
│    └── 🖱️ [EL]₁₉․₆: Tab 1 (Active Roster) Member Management Panel
│         ├── 🖥️ [FE]₁₉․₆․₁: Roster Sub-Tabs: "FAMILY" (`subTabFamilyBtn`) vs "FRIENDS" (`subTabFriendsBtn`)
│         ├── 🖥️ [FE]₁₉․₆․₂: "RESTORE ARCHIVED GROUP" Button (`btnRestore`)
│         │    └── ⚙️ [BE]₁₉․₆․₂․₁: CloudBackupManager.restoreArchivedGroupRoster
│         └── 🖥️ [FE]₁₉․₆․₃: Member Hierarchy Card List: Info Button, Role Selector, Purge Button (`delBtn`)
│              ├── (FR19.5): Deleting member requires 2-step confirmation dialog; immediately removes UID from circle sync.
│              └── ⚙️ [BE]₁₉․₆․₃․₁: GroupRepository.purgeMember(uid)
│                   └── 🗄️ [DB]₁₉․₆․₃․₁․₁: `group_members` (DELETE WHERE `uid` = ?)
│    ├── 🖱️ [EL]₁₉․₇: Device Sharing Audit History Action Button (`btnHistory`) & Modal
│    │    ├── 🖥️ [FE]₁₉․₇․₁: History Action Button in Header (`btnHistory`) invoking audit dialog (`showDeviceSharingHistoryDialog`)
│    │    │    ├── (FR19.6): Asynchronously queries SQLite sharing audit logs; displays actor, recipient, role, status, timestamp, and notes.
│    │    │    ├── Pass Scenario: Modal renders chronologically sorted transaction cards within 80ms.
│    │    │    └── Fail Scenario: Empty history displays empty state card ("No sharing transactions recorded").
│    │    │    └── ⚙️ [BE]₁₉․₇․₁․₁: `DeviceRepository.getDeviceSharingHistory()`
│    │    │         ├── 🗄️ [DB]₁₉․₇․₁․₁․₁: `device_sharing` Table
│    │    │         └── 🗄️ [DB]₁₉․₇․₁․₁․₂: `device_sharing_transactions` Table
│    └── 🖱️ [EL]₁₉․₈: Unified 6-Digit Silent Auto-Handshake Engine
│         ├── 🖥️ [FE]₁₉․₈․₁: Device A Selection Matrix (Selects B + C + Roles -> Auto-Generate 6-Digit PIN)
│         │    ├── (FR19.7): Generates single 6-digit numeric PIN; transmits silently via `DEVICE_SHARING_SYNC` remote command payload.
│         │    └── ⚙️ [BE]₁₉․₈․₁․₁: `PairingRepository.generatePairingCode()` & Remote Command Dispatcher
│         │         └── 🗄️ [DB]₁₉․₈․₁․₁․₁: Firebase RTDB `/pairing_sessions/{code}`
│         └── 🖥️ [FE]₁₉․₈․₂: Recipient Silent Consumer (Zero Manual Prompts)
│              ├── (FR19.8): Recipient receives command with PIN; executes silent handshake via `executeFamilyHandshake()`.
│              ├── Pass Scenario: Recipient pairs with Device C and adopts specified role without user intervention.
│              └── Fail Scenario: Expired PIN or invalid peer triggers fallback to standard manual pairing prompt.
│              └── ⚙️ [BE]₁₉․₈․₂․₁: `TrackingService` Command Ingress & `PairingRepository.executeFamilyHandshake()`
│                   └── 🗄️ [DB]₁₉․₈․₂․₁․₁: `device_pairing` & `group_members` Tables
```
*Testing Pass/Fail Scenario (FR19.4 / BFR19.4 / FR19.7)*:
- **PASS**: Entering valid 6-digit active cloud PIN joins target circle, sets role, and inserts member record into `group_members` in < 500ms; Device Sharing auto-handshake silently pairs recipient in < 1000ms.
- **FAIL**: Submitting expired or non-existent PIN displays clear error toast ("PIN Expired or Invalid") and does not alter local DB.

---

#### 20. `OrgActivity` — Enterprise Organization, Fleet Hierarchy & Domain Governance
```text
ROOT: ORG_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.settings.OrgActivity)
│
├── 💼 [BR]₁: Enterprise Organization Hierarchy & Multi-Tenant Governance
│    ├── 🖱️ [EL]₂₀․₁: Global Navigation Bar & Back Button
│    │    ├── 🖥️ [FE]₂₀․₁․₁: Back Chevron (`backBtn`), Screen Title "ENTERPRISE ORGANIZATION", Subtitle "Fleet Tenant Governance"
│    │    │    └── ⚙️ [BE]₂₀․₁․₁․₁: BaseActivity Navigation Handler
│    ├── 🖱️ [EL]₂₀․₂: Enterprise Organization Summary Hero Card
│    │    ├── 🖥️ [FE]₂₀․₂․₁: Org Logo Icon, Organization Name ("JDROID-X ENTERPRISE"), Domain ID, License Tier Badge ("TIER-1 GOV")
│    │    │    └── ⚙️ [BE]₂₀․₂․₁․₁: OrgRepository.loadOrganizationMetadata
│    │    │         └── 🗄️ [DB]₂₀․₂․₁․₁․₁: `system_registry.xml` (`KEY_ENTERPRISE_ORG_NAME`, `KEY_ENTERPRISE_TIER`)
│    ├── 🖱️ [EL]₂₀․₃: Organizational Hierarchy Structure Card
│    │    ├── 🖥️ [FE]₂₀․₃․₁: Multi-Tier Tree Visualizer: Division -> Fleet Squadron -> Node Unit
│    │    │    └── ⚙️ [BE]₂₀․₃․₁․₁: OrgHierarchyTreeBuilder
│    ├── 🖱️ [EL]₂₀․₄: Enterprise Policy Compliance Card
│    │    ├── 🖥️ [FE]₂₀․₄․₁: Policy List: Mandatory GPS Always-On, Geofence Enforcement, Local Encryption (AES-256) Status Badges
│    │    │    └── ⚙️ [BE]₂₀․₄․₁․₁: EnterprisePolicyValidator.auditCompliance
│    └── 🖱️ [EL]₂₀․₅: Organization Network & Gateway Configuration Strip
│         ├── 🖥️ [FE]₂₀․₅․₁: Enterprise Relay Host URL, Port, TLS Certificate Fingerprint Display Box
│         └── ⚙️ [BE]₂₀․₅․₁․₁: NetworkSecurityConfig.getEnterpriseTlsFingerprint
```
*Testing Pass/Fail Scenario (FR20.1 / BFR20.1)*:
- **PASS**: Opening `OrgActivity` in enterprise mode loads valid tenant ID and displays active compliance badges (Green: PASS) in < 150ms.
- **FAIL**: Missing enterprise credentials defaults to "STANDALONE_COMMUNITY" tier with disabled policy enforcement tiles.

---

#### 21. `DatabaseManagementActivity` — Forensic SQLite Engine, Schema v76 Audit & Maintenance
```text
ROOT: DATABASE_MANAGEMENT_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.management.DatabaseManagementActivity)
│
├── 💼 [BR]₁: Local Forensic Database Integrity, Pruning & Maintenance
│    ├── 🖱️ [EL]₂₁․₁: Global Navigation Bar & Back Button
│    │    ├── 🖥️ [FE]₂₁․₁․₁: Back Chevron (`backBtn`), Screen Title "DATABASE MANAGEMENT", Subtitle "SQLite Engine v76"
│    │    │    └── ⚙️ [BE]₂₁․₁․₁․₁: BaseActivity Navigation Lifecycle
│    ├── 🖱️ [EL]₂₁․₂: Database Engine Health & Storage Metric Hero Card
│    │    ├── 🖥️ [FE]₂₁․₂․₁: File Size Gauge (MB), Page Count, Page Size (4096 B), Journal Mode ("WAL"), Integrity Status ("OK")
│    │    │    └── ⚙️ [BE]₂₁․₂․₁․₁: DatabaseHelper.getDatabaseForensicMetrics
│    │    │         └── 🗄️ [DB]₂₁․₂․₁․₁․₁: PRAGMA page_count, PRAGMA page_size, PRAGMA integrity_check
│    ├── 🖱️ [EL]₂₁․₃: Forensic Table Catalog & Row Count Matrix
│    │    ├── 🖥️ [FE]₂₁․₃․₁: Scrollable Table List with Row Counters: `locations` (N), `sim_transactions` (N), `audit_log` (N), `geofences` (N)
│    │    │    └── ⚙️ [BE]₂₁․₃․₁․₁: DatabaseHelper.getTableRecordCounts
│    │    └── 🖥️ [FE]₂₁․₃․₂: Table Options Gear Icon (`gear`)
│    │         └── ⚙️ [BE]₂₁․₃․₂․₁: Action: Prune specific table older than 7 days
│    ├── 🖱️ [EL]₂₁․₄: Database Maintenance Action Strip
│    │    ├── 🖥️ [FE]₂₁․₄․₁: "VACUUM & RE-INDEX DATABASE" Action Tile
│    │    │    ├── (FR21.1): Executes PRAGMA optimize, VACUUM, and REINDEX in background thread; shows indeterminate progress dialog.
│    │    │    └── ⚙️ [BE]₂₁․₄․₁․₁: DatabaseHelper.vacuumAndReindex
│    │    ├── 🖥️ [FE]₂₁․₄․₂: "EXPORT DATABASE ENCRYPTED BACKUP" Action Tile
│    │    │    └── ⚙️ [BE]₂₁․₄․₂․₁: DatabaseBackupExporter.exportToStorage
│    │    └── 🖥️ [FE]₂₁․₄․₃: "PURGE STALE TELEMETRY (>30 DAYS)" Action Tile
│    │         └── ⚙️ [BE]₂₁․₄․₃․₁: DatabaseMaintenanceWorker.pruneHistoricalRecords(30)
│    └── 🖱️ [EL]₂₁․₅: Danger Zone — Factory Erase Redirect Strip
│         ├── 🖥️ [FE]₂₁․₅․₁: Crimson Outline Action Tile "FACTORY RESET & WIPE" (`btnReset`)
│         └── ⚙️ [BE]₂₁․₅․₁․₁: Intent -> FactoryResetActivity
```
*Testing Pass/Fail Scenario (FR21.1 / BFR21.1)*:
- **PASS**: Triggering VACUUM reduces file fragmentation, reports "Integrity: OK", and decreases file size on disk without locking UI thread.
- **FAIL**: Attempting VACUUM with less than 20MB free internal disk space catches `SQLiteFullException` and displays toast warning.

---

#### 22. `FactoryResetActivity` — Cryptographic Erase, Cloud Recovery & Hard Wipe
```text
ROOT: FACTORY_RESET_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.management.FactoryResetActivity)
│
├── 💼 [BR]₁: Two-Stage Cryptographic Wipe & Cloud State Restoration
│    ├── 🖱️ [EL]₂₂․₁: Global Navigation Bar & Cloud Recovery Refresh Action
│    │    ├── 🖥️ [FE]₂₂․₁․₁: Back Chevron (`backBtn`), Screen Title "FACTORY RESET", Cloud Refresh Icon (`refreshIcon`)
│    │    │    └── ⚙️ [BE]₂₂․₁․₁․₁: FactoryResetViewModel.startCloudRecovery
│    ├── 🖱️ [EL]₂₂․₂: Warning Banner & Irreversible Notice Hero Card
│    │    └── 🖥️ [FE]₂₂․₂․₁: Crimson Triangular Warning Icon, "IRREVERSIBLE HARD WIPE", Detailed Data Loss Warning Notice
│    ├── 🖱️ [EL]₂₂․₃: Targeted Wipe Components Checklist
│    │    ├── 🖥️ [FE]₂₂․₃․₁: Checkboxes: Wipe SQLite DB (`cbWipeDb`), Clear Preferences (`cbWipePrefs`), Invalidate Auth Session (`cbWipeAuth`)
│    │    └── ⚙️ [BE]₂₂․₃․₁․₁: FactoryResetViewModel.setWipeTargets(mask)
│    ├── 🖱️ [EL]₂₂․₄: Stage-1 Confirmation Action Button (`btnTriggerReset`)
│    │    ├── 🖥️ [FE]₂₂․₄․₁: Crimson Button "EXECUTE FACTORY ERASE"
│    │    │    ├── (FR22.1): Triggers Stage-2 Modal Security Challenge Dialog; cannot be bypassed.
│    │    │    └── ⚙️ [BE]₂₂․₄․₁․₁: FactoryResetViewModel.promptSecurityChallenge
│    ├── 🖱️ [EL]₂₂․₅: Stage-2 Security Challenge Modal Dialog
│    │    ├── 🖥️ [FE]₂₂․₅․₁: Challenge Dialog: Requires user to input master purge safety phrase (`admin@1234`) into `etConfirmText`
│    │    │    ├── (FR22.2): "WIPE PERMANENTLY" button remains disabled until exact phrase matches (SHA-256 validated).
│    │    │    └── ⚙️ [BE]₂₂․₅․₁․₁: `RegistryManager.isPurgeSafetyPhraseValid(context, input)` verifying `admin@1234`
│    │    └── 🖥️ [FE]₂₂․₅․₂: Confirmed Wipe Button
│    │         ├── (FR22.3): Executes nuclear purge: drops all tables, wipes SharedPreferences, terminates background workers, kills process.
│    │         └── ⚙️ [BE]₂₂․₅․₂․₁: DatabaseHelper.nukeDatabase & AuthManager.logout
│    │              └── 🗄️ [DB]₂₂․₅․₂․₁․₁: Complete deletion of all SQLite databases and XML preference files
│    └── 🖱️ [EL]₂₂․₆: Cancel & Return Home Action Button (`btnHome`)
│         ├── 🖥️ [FE]₂₂․₆․₁: Ghost Secondary Button "CANCEL & RETURN"
│         └── ⚙️ [BE]₂₂․₆․₁․₁: Activity.finish()
```
*Testing Pass/Fail Scenario (FR22.2 / BFR22.2)*:
- **PASS**: Typing "admin@1234" enables wipe button; tapping it deletes local DB, clears prefs, and restarts application into `SplashActivity`.
- **FAIL**: Typing invalid phrase or leaving field blank keeps the execution button disabled, preventing accidental wipe.

---

#### 23. `FirebaseCloudMatrixActivity` — Cloud RTDB Real-Time Sync & WebSocket Monitor
```text
ROOT: FIREBASE_CLOUD_MATRIX_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.management.FirebaseCloudMatrixActivity)
│
├── 💼 [BR]₁: Firebase Realtime Database Topology & Active Connection Monitor
│    ├── 🖱️ [EL]₂₃․₁: Global Navigation Bar & Back Button
│    │    ├── 🖥️ [FE]₂₃․₁․₁: Back Chevron (`backBtn`), Screen Title "FIREBASE CLOUD MATRIX", Subtitle "RTDB Connection Inspector"
│    │    │    └── ⚙️ [BE]₂₃․₁․₁․₁: BaseActivity Navigation Lifecycle
│    ├── 🖱️ [EL]₂₃․₂: Real-Time Connection Status Hero Tile
│    │    ├── 🖥️ [FE]₂₃․₂․₁: Status Badge (`CONNECTED`, `CONNECTING`, `DISCONNECTED`), Ping Latency (ms), Active Firebase Host URL
│    │    │    ├── (FR23.1): Listens to `.info/connected` path; updates status badge and ping gauge live.
│    │    │    └── ⚙️ [BE]₂₃․₂․₁․₁: FirebaseComm.observeConnectionState
│    │    │         └── 🗄️ [DB]₂₃․₂․₁․₁․₁: Firebase RTDB `/.info/connected`
│    ├── 🖱️ [EL]₂₃․₃: Active Realtime Listener Nodes Matrix
│    │    ├── 🖥️ [FE]₂₃․₃․₁: Table of active event listeners: `/locations`, `/groups`, `/alerts`, `/pairing_sessions`
│    │    │    └── ⚙️ [BE]₂₃․₃․₁․₁: FirebaseComm.getActiveListenersCount
│    ├── 🖱️ [EL]₂₃․₄: Cloud Sync Latency Benchmark Action Strip
│    │    ├── 🖥️ [FE]₂₃․₄․₁: "RUN CLOUD LATENCY BENCHMARK" Action Tile
│    │    │    ├── (FR23.2): Writes timestamp to `/ping_benchmarks/{uid}`; measures round-trip time (RTT); displays result in ms.
│    │    │    └── ⚙️ [BE]₂₃․₄․₁․₁: FirebaseComm.benchmarkRoundTripLatency
│    │    │         └── 🗄️ [DB]₂₃․₄․₁․₁․₁: Firebase RTDB `/ping_benchmarks`
│    └── 🖱️ [EL]₂₃․₅: Cloud Connection Kill-Switch Strip
│         ├── 🖥️ [FE]₂₃․₅․₁: Toggle Switch: "Force Offline (Local Loop Only)"
│         └── ⚙️ [BE]₂₃․₅․₁․₁: FirebaseDatabase.getInstance().goOffline() / goOnline()
```
*Testing Pass/Fail Scenario (FR23.1 / BFR23.1)*:
- **PASS**: When internet is disconnected, `.info/connected` event triggers in < 500ms, shifting UI badge to crimson "DISCONNECTED".
- **FAIL**: Restoring internet connectivity fails to trigger auto-reconnect, leaving the matrix in a frozen or stale state.

---

#### 24. `ForensicVaultExplorerActivity` — Encrypted Forensic Vault & Raw Audit Log Reader
```text
ROOT: FORENSIC_VAULT_EXPLORER_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.management.ForensicVaultExplorerActivity)
│
├── 💼 [BR]₁: Encrypted Audit Vault Forensics & Security Log Inspection
│    ├── 🖱️ [EL]₂₄․₁: Global Navigation Bar & Export Action
│    │    ├── 🖥️ [FE]₂₄․₁․₁: Back Chevron (`backBtn`), Screen Title "FORENSIC VAULT EXPLORER", Subtitle "Tamper-Evident Audit Vault"
│    │    └── 🖥️ [FE]₂₄․₁․₂: JSON Export Button (`btnExportVault`)
│    │         └── ⚙️ [BE]₂₄․₁․₂․₁: ForensicVaultManager.exportEncryptedVaultJson
│    ├── 🖱️ [EL]₂₄․₂: Filter Chip Bar (Multi-Category Selector)
│    │    ├── 🖥️ [FE]₂₄․₂․₁: Filter Chips: `ALL`, `SECURITY`, `TELEMETRY`, `GEOFENCE`, `TRANSPORT_FAILOVER`, `ERRORS`
│    │    │    ├── (FR24.1): Clicking chip filters the audit records list dynamically without re-querying the raw database disk.
│    │    │    └── ⚙️ [BE]₂₄․₂․₁․₁: AuditFilterAdapter.filterByEventType
│    ├── 🖱️ [EL]₂₄․₃: Forensic Search Input Bar
│    │    ├── 🖥️ [FE]₂₄․₃․₁: Search Input Box (`etSearchAudit`) with debounce filter (150ms)
│    │    │    └── ⚙️ [BE]₂₄․₃․₁․₁: ForensicSearchEngine.filterAuditStream
│    ├── 🖱️ [EL]₂₄․₄: Raw Audit Event Stream Matrix (`rvAuditLogs`)
│    │    ├── 🖥️ [FE]₂₄․₄․₁: Monospace Event Card: Timestamp ISO-8601, Event Type, SHA-256 Hash Digest, Payload JSON Key-Values
│    │    │    └── ⚙️ [BE]₂₄․₄․₁․₁: DatabaseHelper.getAuditLogsCursor
│    │    │         └── 🗄️ [DB]₂₄․₄․₁․₁․₁: `audit_log` (id, event_type, details, timestamp, hash_digest)
│    └── 🖱️ [EL]₂₄․₅: Tamper-Evident Integrity Verification Strip
│         ├── 🖥️ [FE]₂₄․₅․₁: Status Tile: "BLOCKCHAIN HASH CHAIN: VERIFIED" (Emerald Shield)
│         └── ⚙️ [BE]₂₄․₅․₁․₁: ForensicVaultManager.verifyHashChainIntegrity
```
*Testing Pass/Fail Scenario (FR24.1 / BFR24.1)*:
- **PASS**: Selecting chip `SECURITY` filters 1,500 audit events down to security-related events (Logins, PIN verifications, Key rotations) in < 30ms.
- **FAIL**: Corrupt hash digest in `audit_log` triggers warning card "TAMPER DETECTED: Hash Mismatch at Log #412".


### Domain 6: Tactical Alerts, Privacy Shields, OTA & Unified System Configuration

#### 25. `NotifActivity` — Tactical Notification Center & Alert Dispatch Ledger
```text
ROOT: NOTIF_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.management.NotifActivity)
│
├── 💼 [BR]₁: Tactical Notification Ingestion, Categorization & Dismissal
│    ├── 🖱️ [EL]₂₅․₁: Global Navigation Bar & Action Strip
│    │    ├── 🖥️ [FE]₂₅․₁․₁: Back Chevron (`backBtn`), Screen Title "NOTIFICATION CENTER", Subtitle "Tactical Alerts Ledger"
│    │    └── 🖥️ [FE]₂₅․₁․₂: "CLEAR ALL" Action Button (`btnClearAll`)
│    │         ├── (FR25.1): Prompts confirmation dialog; clears all read alerts; updates badge count on launcher icon.
│    │         └── ⚙️ [BE]₂₅․₁․₂․₁: NotificationRepository.dismissAllAlerts
│    │              └── 🗄️ [DB]₂₅․₁․₂․₁․₁: `notifications` (UPDATE `is_dismissed` = 1)
│    ├── 🖱️ [EL]₂₅․₂: Notification Priority Filter Segmented Bar
│    │    ├── 🖥️ [FE]₂₅․₂․₁: Category Chips: `ALL`, `CRITICAL_SOS`, `GEOFENCE_BREACH`, `BATTERY_LOW`, `SYSTEM`
│    │    │    └── ⚙️ [BE]₂₅․₂․₁․₁: NotificationFilterAdapter.filterBySeverity
│    └── 🖱️ [EL]₂₅․₃: Notification Event Cards Stream (`rvNotifs`)
│         ├── 🖥️ [FE]₂₅․₃․₁: Alert Card: Severity Icon (Crimson Pulse for SOS, Amber for Geofence), Title, Body, Timestamp
│         ├── 🖥️ [FE]₂₅․₃․₂: Inline Action Button 1: "LOCATE ON MAP"
│         │    └── ⚙️ [BE]₂₅․₃․₂․₁: Intent -> LiveMapDashboardActivity (Focus coordinates)
│         └── 🖥️ [FE]₂₅․₃․₃: Inline Action Button 2: "DISMISS / ACK"
│              └── ⚙️ [BE]₂₅․₃․₃․₁: NotificationManager.acknowledgeAlert(notifId)
│                   └── 🗄️ [DB]₂₅․₃․₃․₁․₁: `notifications` (UPDATE `is_acknowledged` = 1 WHERE `id` = ?)
```
*Testing Pass/Fail Scenario (FR25.1 / BFR25.1)*:
- **PASS**: Tapping "CLEAR ALL" marks 40 notifications as dismissed, clears badge count, and shows empty-state vector graphic in < 100ms.
- **FAIL**: Database lock during dismissal fails to update records and leaves stale cards on screen.

---

#### 26. `PrivacyActivity` — Autonomous Ghost Mode, Privacy Tiers & Location Obfuscation
```text
ROOT: PRIVACY_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.management.PrivacyActivity)
│
├── 💼 [BR]₁: Privacy Tier Enforcement, Spatial Fuzzing & Ghost Mode Governance
│    ├── 🖱️ [EL]₂₆․₁: Global Navigation Bar & Back Button
│    │    ├── 🖥️ [FE]₂₆․₁․₁: Back Chevron (`backBtn`), Screen Title "PRIVACY & OBFUSCATION", Subtitle "Data Shield"
│    │    │    └── ⚙️ [BE]₂₆․₁․₁․₁: BaseActivity Navigation Lifecycle
│    ├── 🖱️ [EL]₂₆․₂: Global Ghost Mode Master Toggle Strip
│    │    ├── 🖥️ [FE]₂₆․₂․₁: Master Switch: "GHOST MODE (ZERO CLOUD UPLOADS)" (`swGhostMode`)
│    │    │    ├── (FR26.1): Enabling Ghost Mode immediately terminates all cloud dispatchers; GPS logs strictly to local SQLite.
│    │    │    └── ⚙️ [BE]₂₆․₂․₁․₁: PrivacyManager.setGhostModeEnabled(boolean)
│    │    │         └── 🗄️ [DB]₂₆․₂․₁․₁․₁: `system_registry.xml` (`KEY_GHOST_MODE_ENABLED`)
│    ├── 🖱️ [EL]₂₆․₃: Privacy Tier Selector Radio Card Deck
│    │    ├── 🖥️ [FE]₂₆․₃․₁: Tier 1: `PRECISION` (Raw GPS, 1-3m accuracy, intended for tactical navigation)
│    │    ├── 🖥️ [FE]₂₆․₃․₂: Tier 2: `BALANCED` (Truncates Lat/Lng to 3 decimals, ~110m obfuscation radius)
│    │    └── 🖥️ [FE]₂₆․₃․₃: Tier 3: `STEALTH` (Centroid fuzzing to neighborhood / postal sector ~1km)
│    │         └── ⚙️ [BE]₂₆․₃․₃․₁: SpatialObfuscator.applySpatialFuzzing(lat, lng, tier)
│    ├── 🖱️ [EL]₂₆․₄: Dynamic Spatial Fuzzing Radius Stepper Card
│    │    ├── 🖥️ [FE]₂₆․₄․₁: Slider / Stepper: "Custom Obfuscation Radius: 0m to 1000m" (Default: 100m)
│    │    └── ⚙️ [BE]₂₆․₄․₁․₁: SharedPreferences -> `KEY_PRIVACY_FUZZ_RADIUS_METERS`
│    └── 🖱️ [EL]₂₆․₅: Autonomous Tracking Schedule Policy Card
│         ├── 🖥️ [FE]₂₆․₅․₁: Schedule Switch: "Enable Scheduled Privacy Window" (`swPrivacySchedule`)
│         └── 🖥️ [FE]₂₆․₅․₂: Time Pickers: "Active Window Start (22:00) — End (07:00)"
│              └── ⚙️ [BE]₂₆․₅․₂․₁: AlarmManager / WorkManager PrivacyScheduleEnforcer
```
*Testing Pass/Fail Scenario (FR26.1 / BFR26.1)*:
- **PASS**: Enabling Ghost Mode halts Firebase RTDB and SIM SMS dispatch within 10ms; local GPS coordinates continue writing to SQLite `locations`.
- **FAIL**: Disabling Ghost Mode fails to resume cloud listeners, leaving node permanently detached from circle telemetry.

---

#### 27. `SettingsActivity` — Master Enterprise Configuration Engine (All 7 Priority Sections)
```text
ROOT: SETTINGS_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.settings.SettingsActivity)
│
├── 💼 [BR]₁: Master Enterprise System Governance (7 Priority Domains)
│    ├── 🖱️ [EL]₂₇․₁: Global Navigation Bar & Search Affordance
│    │    ├── 🖥️ [FE]₂₇․₁․₁: Back Chevron (`backBtn`), Screen Title "SYSTEM SETTINGS", Subtitle "Enterprise Master Engine"
│    │    └── ⚙️ [BE]₂₇․₁․₁․₁: BaseActivityJava Universal Settings Engine
│    │
│    ├── 💼 [BR]₂: Priority Section 1: Tracking Engine & Cadence
│    │    ├── 🖱️ [EL]₂₇․₂: Section Header 1: `🛰️ TRACKING ENGINE & CADENCE (PRIORITY 1)`
│    │    ├── 🖱️ [EL]₂₇․₃: Adaptive GPS Frequency Stepper Tile
│    │    │    ├── 🖥️ [FE]₂₇․₃․₁: Setting Tile: "ACTIVE TRACKING INTERVAL" (Options: 5s, 10s, 30s, 60s, 300s)
│    │    │    └── ⚙️ [BE]₂₇․₃․₁․₁: BackgroundJobManager.updateTrackingCadence
│    │    │         └── 🗄️ [DB]₂₇․₃․₁․₁․₁: `system_registry.xml` (`KEY_TRACKING_INTERVAL_SEC`)
│    │    ├── 🖱️ [EL]₂₇․₄: Stationary Dormancy Timeout Stepper Tile
│    │    │    ├── 🖥️ [FE]₂₇․₄․₁: Setting Tile: "STATIONARY SLEEP CADENCE" (Options: 60s, 300s, 900s)
│    │    │    └── ⚙️ [BE]₂₇․₄․₁․₁: MovementSentinel.setStationaryTimeout
│    │    ├── 🖱️ [EL]₂₇․₅: High-Accuracy GPS Mode Switch
│    │    │    ├── 🖥️ [FE]₂₇․₅․₁: Switch: "Dual-Frequency GNSS & Continuous Lock" (`swHighAccuracy`)
│    │    │    └── ⚙️ [BE]₂₇․₅․₁․₁: LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
│    │    └── 🖱️ [EL]₂₇․₆: Motion-Triggered Wake Switch
│    │         ├── 🖥️ [FE]₂₇․₆․₁: Switch: "Accelerometer Significant Motion Trigger" (`swMotionWake`)
│    │         └── ⚙️ [BE]₂₇․₆․₁․₁: SignificantMotionTriggerSensorListener
│    │
│    ├── 💼 [BR]₃: Priority Section 2: Network Dispatch & Failover
│    │    ├── 🖱️ [EL]₂₇․₇: Section Header 2: `📡 NETWORK DISPATCH & FAILOVER (PRIORITY 2)`
│    │    ├── 🖱️ [EL]₂₇․₈: Autonomous Transport Failover Toggle Switch
│    │    │    ├── 🖥️ [FE]₂₇․₈․₁: Switch: "Automated Multi-Channel Failover (WiFi -> Cell -> SMS)"
│    │    │    └── ⚙️ [BE]₂₇․₈․₁․₁: NetworkTransportManager.setFailoverEnabled
│    │    ├── 🖱️ [EL]₂₇․₉: Primary Cloud Transport Selector Tile
│    │    │    ├── 🖥️ [FE]₂₇․₉․₁: Setting Tile: "PRIMARY DISPATCH ENGINE" [FIREBASE_WEBSOCKET, REST_API, P2P_MESH]
│    │    │    └── ⚙️ [BE]₂₇․₉․₁․₁: TransportRouter.setPrimaryTransport
│    │    ├── 🖱️ [EL]₂₇․₁₀: HTTP Payload Compression Switch
│    │    │    ├── 🖥️ [FE]₂₇․₁₀․₁: Switch: "GZIP / Deflate Telemetry Compression"
│    │    │    └── ⚙️ [BE]₂₇․₁₀․₁․₁: OkHttpClient.Builder with GzipInterceptor
│    │    └── 🖱️ [EL]₂₇․₁₁: Server Ping Heartbeat Interval Tile
│    │         ├── 🖥️ [FE]₂₇․₁₁․₁: Setting Tile: "KEEPALIVE HEARTBEAT" (Options: 30s, 60s, 120s)
│    │         └── ⚙️ [BE]₂₇․₁₁․₁․₁: FirebaseComm.setHeartbeatInterval
│    │
│    ├── 💼 [BR]₄: Priority Section 3: Sensor Filtering & Smoothing
│    │    ├── 🖱️ [EL]₂₇․₁₂: Section Header 3: `🧭 SENSOR FILTERING & SMOOTHING (PRIORITY 3)`
│    │    ├── 🖱️ [EL]₂₇․₁₃: Road Alignment & 2D Kalman Filter Switch
│    │    │    ├── 🖥️ [FE]₂₇․₁₃․₁: Switch: "Road Alignment & 2D Kalman Filter" (`roadSmoothingEn`)
│    │    │    └── ⚙️ [BE]₂₇․₁₃․₁․₁: KalmanFilter2D.setEnabled
│    │    ├── 🖱️ [EL]₂₇․₁₄: Kalman Sensitivity & Heading EMA Stepper Tile
│    │    │    ├── 🖥️ [FE]₂₇․₁₄․₁: Setting Tile: "KALMAN SENSITIVITY & HEADING EMA" (Alpha: 0.1 to 0.9)
│    │    │    └── ⚙️ [BE]₂₇․₁₄․₁․₁: HeadingEmaSmoother.setAlpha
│    │    └── 🖱️ [EL]₂₇․₁₅: Acceleration Noise Filter Switch
│    │         ├── 🖥️ [FE]₂₇․₁₅․₁: Switch: "Jitter Noise Gate (Reject < 0.2m Movement)"
│    │         └── ⚙️ [BE]₂₇․₁₅․₁․₁: MovementSentinel.setNoiseGateThreshold
│    │
│    ├── 💼 [BR]₅: Priority Section 4: Geofencing Sentinel & Perimeter Boundaries
│    │    ├── 🖱️ [EL]₂₇․₁₆: Section Header 4: `🌐 GEOFENCING SENTINEL & PERIMETER BOUNDARIES (PRIORITY 4)`
│    │    ├── 🖱️ [EL]₂₇․₁₇: Perimeter Jitter Filter Stepper Tile
│    │    │    ├── 🖥️ [FE]₂₇․₁₇․₁: Setting Tile: "PERIMETER JITTER FILTER" (Options: 5m, 10m, 25m)
│    │    │    └── ⚙️ [BE]₂₇․₁₇․₁․₁: GeofenceSentinel.setJitterFilterMeters
│    │    ├── 🖱️ [EL]₂₇․₁₈: Dormancy State Latch Switch (Anti-Duplicate)
│    │    │    ├── 🖥️ [FE]₂₇․₁₈․₁: Switch: "Dormancy State Latch (Anti-Duplicate)" (`latchEnabled`)
│    │    │    └── ⚙️ [BE]₂₇․₁₈․₁․₁: GeofenceLatchManager.setAntiDuplicateLatchEnabled
│    │    ├── 🖱️ [EL]₂₇․₁₉: Snap GPS to Safe Zone Centroid Switch
│    │    │    ├── 🖥️ [FE]₂₇․₁₉․₁: Switch: "Snap GPS to Safe Zone Centroid on Entry" (`snapEnabled`)
│    │    │    └── ⚙️ [BE]₂₇․₁₉․₁․₁: GeofenceSnapEngine.setSnapEnabled
│    │    ├── 🖱️ [EL]₂₇․₂₀: Local Geofence Sound & Banner Switch
│    │    │    ├── 🖥️ [FE]₂₇․₂₀․₁: Switch: "Local Device Geofence Sound & Banner" (`localNotifEnabled`)
│    │    │    └── ⚙️ [BE]₂₇․₂₀․₁․₁: NotificationHelper.setGeofenceNotificationEnabled
│    │    ├── 🖱️ [EL]₂₇․₂₁: Cloud FCM Guardian Push Broadcast Switch
│    │    │    ├── 🖥️ [FE]₂₇․₂₁․₁: Switch: "Cloud FCM Guardian Push Broadcast" (`fcmEnabled`)
│    │    │    └── ⚙️ [BE]₂₇․₂₁․₁․₁: FcmDispatcher.setGeofenceBroadcastEnabled
│    │    └── 🖱️ [EL]₂₇․₂₂: Default Safe Zone Radius Stepper Tile
│    │         ├── 🖥️ [FE]₂₇․₂₂․₁: Setting Tile: "DEFAULT SAFE ZONE RADIUS" (Options: 50m, 100m, 250m, 500m)
│    │         └── ⚙️ [BE]₂₇․₂₂․₁․₁: GeofenceRepository.setDefaultRadius
│    │
│    ├── 💼 [BR]₆: Priority Section 5: RAG Telemetry & Alert Thresholds
│    │    ├── 🖱️ [EL]₂₇․₂₃: Section Header 5: `🚦 RAG TELEMETRY & ALERT THRESHOLDS (PRIORITY 5)`
│    │    ├── 🖱️ [EL]₂₇․₂₄: Low Battery Alert Threshold Stepper Tile
│    │    │    ├── 🖥️ [FE]₂₇․₂₄․₁: Setting Tile: "LOW BATTERY ALERT THRESHOLD" (Options: 10%, 15%, 20%, 25%)
│    │    │    └── ⚙️ [BE]₂₇․₂₄․₁․₁: BatterySentinel.setLowBatteryThreshold
│    │    ├── 🖱️ [EL]₂₇․₂₅: Inactive Stale Threshold Stepper Tile (Amber)
│    │    │    ├── 🖥️ [FE]₂₇․₂₅․₁: Setting Tile: "INACTIVE STALE THRESHOLD (AMBER)" (Options: 5m, 15m, 30m)
│    │    │    └── ⚙️ [BE]₂₇․₂₅․₁․₁: RAGTelemetryEngine.setStaleThresholdMinutes
│    │    └── 🖱️ [EL]₂₇․₂₆: Acoustic Siren Trigger Threshold Tile
│    │         ├── 🖥️ [FE]₂₇․₂₆․₁: Setting Tile: "AUTO-SIREN ALARM THRESHOLD" (Off, Panic Only, Critical Breach)
│    │         └── ⚙️ [BE]₂₇․₂₆․₁․₁: AcousticSirenSentinel.setTriggerMode
│    │
│    ├── 💼 [BR]₇: Priority Section 6: Forensic Storage & Pruning
│    │    ├── 🖱️ [EL]₂₇․₂₇: Section Header 6: `🗄️ FORENSIC STORAGE & PRUNING (PRIORITY 6)`
│    │    ├── 🖱️ [EL]₂₇․₂₈: Local Database Retention Days Stepper Tile
│    │    │    ├── 🖥️ [FE]₂₇․₂₈․₁: Setting Tile: "LOCATION HISTORY RETENTION" (Options: 7d, 14d, 30d, 90d, Forever)
│    │    │    └── ⚙️ [BE]₂₇․₂₈․₁․₁: DatabaseMaintenanceWorker.setRetentionDays
│    │    ├── 🖱️ [EL]₂₇․₂₉: Auto-Vacuum on Launch Switch
│    │    │    ├── 🖥️ [FE]₂₇․₂₉․₁: Switch: "Automated SQLite VACUUM on Clean Startup"
│    │    │    └── ⚙️ [BE]₂₇․₂₉․₁․₁: DatabaseHelper.setAutoVacuumEnabled
│    │    └── 🖱️ [EL]₂₇․₃₀: Storage Prune Periodic Worker Interval Tile
│    │         ├── 🖥️ [FE]₂₇․₃₀․₁: Setting Tile: "MAINTENANCE WORKER CYCLE" (Options: 12h, 24h, 48h)
│    │         └── ⚙️ [BE]₂₇․₃₀․₁․₁: BackgroundJobManager.rescheduleStoragePruneWorker
│    │
│    └── 💼 [BR]₈: Priority Section 7: Security, Vault & Enterprise
│         ├── 🖱️ [EL]₂₇․₃₁: Section Header 7: `🔒 SECURITY, VAULT & ENTERPRISE (PRIORITY 7)`
│         ├── 🖱️ [EL]₂₇․₃₂: Local Database AES-256 Encryption Switch
│         │    ├── 🖥️ [FE]₂₇․₃₂․₁: Switch: "SQLCipher Full-Disk Database Encryption"
│         │    └── ⚙️ [BE]₂₇․₃₂․₁․₁: SecurityManager.setEncryptionEnabled
│         ├── 🖱️ [EL]₂₇․₃₃: Biometric Lock on Launch Switch
│         │    ├── 🖥️ [FE]₂₇․₃₃․₁: Switch: "Biometric / Fingerprint Screen Lock"
│         │    └── ⚙️ [BE]₂₇․₃₃․₁․₁: BiometricPromptHelper.setEnforced
│         └── 🖱️ [EL]₂₇․₃₄: Master Factory Erase Redirect Tile
│              ├── 🖥️ [FE]₂₇․₃₄․₁: Action Tile: "FACTORY RESET SYSTEM & PURGE DATA"
│              └── ⚙️ [BE]₂₇․₃₄․₁․₁: Intent -> FactoryResetActivity
```
*Testing Pass/Fail Scenario (FR27.18 / BFR27.18)*:
- **PASS**: Toggling "Dormancy State Latch" immediately writes state to `system_registry.xml`, updates live `GeofenceLatchManager`, and suppresses redundant exit broadcasts.
- **FAIL**: Writing settings without holding thread lock causes concurrent modification exception in SharedPreferences commit.

---

#### 28. `AboutActivity` — Application Branding, Legal Licenses & Build Provenance
```text
ROOT: ABOUT_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.settings.AboutActivity)
│
├── 💼 [BR]₁: Application Identity, Build Provenance & Open Source Compliance
│    ├── 🖱️ [EL]₂₈․₁: Global Navigation Bar & Back Button
│    │    ├── 🖥️ [FE]₂₈․₁․₁: Back Chevron (`backBtn`), Screen Title "ABOUT SYSTEM", Subtitle "jFindMe Core Engine"
│    │    └── ⚙️ [BE]₂₈․₁․₁․₁: BaseActivity Navigation Lifecycle
│    ├── 🖱️ [EL]₂₈․₂: Application Hero Branding Card
│    │    ├── 🖥️ [FE]₂₈․₂․₁: App Icon (Butterfly Silhouette), Title "jFindMe Mission Control", Version "v2.4.0 (Build 76)"
│    │    └── ⚙️ [BE]₂₈․₂․₁․₁: BuildConfig.VERSION_NAME & BuildConfig.VERSION_CODE
│    ├── 🖱️ [EL]₂₈․₃: Git Provenance & Commit Hash Tile
│    │    ├── 🖥️ [FE]₂₈․₃․₁: Monospace Git Commit Hash, Branch Name, Build Timestamp ISO
│    │    └── ⚙️ [BE]₂₈․₃․₁․₁: BuildConfig.GIT_COMMIT_HASH
│    ├── 🖱️ [EL]₂₈․₄: Open Source Licenses Action Row (`rowLicenses`)
│    │    ├── 🖥️ [FE]₂₈․₄․₁: Interactive Tile "OPEN SOURCE LICENSES & NOTICES"
│    │    │    ├── (FR28.1): Opens Modal Dialog rendering Apache 2.0, MIT, and BSD notices for Osmdroid, OkHttp, and Firebase SDKs.
│    │    │    └── ⚙️ [BE]₂₈․₄․₁․₁: LicenseAssetReader.readLicenseTextAsset
│    └── 🖱️ [EL]₂₈․₅: Developer & Enterprise Credits Card
│         ├── 🖥️ [FE]₂₈․₅․₁: "Engineered by jDroid-X Autonomous AI Systems for Operator JEET"
│         └── ⚙️ [BE]₂₈․₅․₁․₁: Static Enterprise Metadata Binding
```
*Testing Pass/Fail Scenario (FR28.1 / BFR28.1)*:
- **PASS**: Tapping `rowLicenses` renders all open source notices in < 50ms without UI freezing.
- **FAIL**: Missing license asset file throws `FileNotFoundException` or shows empty blank dialog.

---

#### 29. `HelpSupportActivity` — Operator Support, Diagnostic Bundler & Knowledge Base
```text
ROOT: HELP_SUPPORT_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.settings.HelpSupportActivity)
│
├── 💼 [BR]₁: Operator Guidance, FAQ Accordion & Diagnostic Bundle Transmission
│    ├── 🖱️ [EL]₂₉․₁: Global Navigation Bar & Back Button
│    │    ├── 🖥️ [FE]₂₉․₁․₁: Back Chevron (`backBtn`), Screen Title "HELP & SUPPORT", Subtitle "Operator Guidance"
│    │    └── ⚙️ [BE]₂₉․₁․₁․₁: BaseActivity Navigation Lifecycle
│    ├── 🖱️ [EL]₂₉․₂: Searchable FAQ Expandable Accordion List
│    │    ├── 🖥️ [FE]₂₉․₂․₁: Search Bar: "Filter Support Topics"
│    │    ├── 🖥️ [FE]₂₉․₂․₂: FAQ Accordion 1: "How to Configure Offline SMS Failover" (Expandable Card)
│    │    ├── 🖥️ [FE]₂₉․₂․₃: FAQ Accordion 2: "Troubleshooting SIM Multi-Slot Switch" (Expandable Card)
│    │    └── 🖥️ [FE]₂₉․₂․₄: FAQ Accordion 3: "Geofence Sentinel & Battery Optimization" (Expandable Card)
│    │         └── ⚙️ [BE]₂₉․₂․₄․₁: FaqAdapter.toggleAccordion(position)
│    ├── 🖱️ [EL]₂₉․₃: Diagnostic Log Bundle Attachment Card
│    │    ├── 🖥️ [FE]₂₉․₃․₁: Switch: "Attach Encrypted Diagnostic Telemetry Logs" (`swAttachLogs`)
│    │    └── ⚙️ [BE]₂₉․₃․₁․₁: DiagnosticLogBundler.compileSanitizedDiagnosticsZip
│    └── 🖱️ [EL]₂₉․₄: Send Support Request Action Button
│         ├── 🖥️ [FE]₂₉․₄․₁: Primary Action Button "DISPATCH OPERATOR REPORT"
│         └── ⚙️ [BE]₂₉․₄․₁․₁: Intent -> ACTION_SENDTO (mailto:) or Encrypted Direct HTTP Post
```
*Testing Pass/Fail Scenario (FR29.1 / BFR29.1)*:
- **PASS**: Tapping FAQ item expands explanation card with smooth height animation in <= 200ms.
- **FAIL**: Bundling diagnostic zip with missing storage permissions throws unhandled security exception.

---

#### 30. `SystemUpdateActivity` — Multi-Source OTA Update, SHA-256 Verifier & Package Installer
```text
ROOT: SYSTEM_UPDATE_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.settings.SystemUpdateActivity)
│
├── 💼 [BR]₁: Multi-Source OTA Update Retrieval, Cryptographic Verification & Package Staging
│    ├── 🖱️ [EL]₃₀․₁: Global Navigation Bar & Refresh Action
│    │    ├── 🖥️ [FE]₃₀․₁․₁: Back Chevron (`backBtn`), Screen Title "SYSTEM UPDATE (OTA)", Refresh Button (`btnRefresh`)
│    │    │    └── ⚙️ [BE]₃₀․₁․₁․₁: SystemUpdateViewModel.executeUpdate(this)
│    ├── 🖱️ [EL]₃₀․₂: Multi-Source Repository Segmented Tabs (3 Sources)
│    │    ├── 🖥️ [FE]₃₀․₂․₁: Tab 1: `GOOGLE DRIVE OTA` (`tabDrive`)
│    │    ├── 🖥️ [FE]₃₀․₂․₂: Tab 2: `GITHUB RELEASES` (`tabGithub`)
│    │    └── 🖥️ [FE]₃₀․₂․₃: Tab 3: `LOCAL SYNC HTTP` (`tabLocalSync`)
│    │         ├── (FR30.1): Tab selection shifts active target endpoint configuration and manifest parser.
│    │         └── ⚙️ [BE]₃₀․₂․₃․₁: UpdateSourceController.setActiveSource(sourceIndex)
│    ├── 🖱️ [EL]₃₀․₃: Source Endpoint Configuration Strip
│    │    ├── 🖥️ [FE]₃₀․₃․₁: Google Drive Link Editor Button (`btnEditDrive`) -> `showEditLinkDialog`
│    │    ├── 🖥️ [FE]₃₀․₃․₂: GitHub Releases Repo Editor Button (`btnEditGh`) -> `showEditGithubLinkDialog`
│    │    └── 🖥️ [FE]₃₀․₃․₃: Local HTTP Sync IP Editor Button (`btnEditSync`) -> `showEditLocalSyncEndpointDialog`
│    │         └── ⚙️ [BE]₃₀․₃․₃․₁: SharedPreferences -> `KEY_OTA_DRIVE_URL`, `KEY_OTA_GITHUB_REPO`, `KEY_OTA_LOCAL_ENDPOINT`
│    ├── 🖱️ [EL]₃₀․₄: Current vs Remote Version Diff Hero Card
│    │    ├── 🖥️ [FE]₃₀․₄․₁: Current Installed: `v2.4.0 (Build 76)`
│    │    ├── 🖥️ [FE]₃₀․₄․₂: Remote Discovered: `v2.4.1 (Build 77)` (Green Badge: UPDATE AVAILABLE)
│    │    └── 🖥️ [FE]₃₀․₄․₃: SHA-256 Checksum Fingerprint Match Box
│    │         └── ⚙️ [BE]₃₀․₄․₃․₁: SystemUpdateViewModel.compareManifestVersions
│    ├── 🖱️ [EL]₃₀․₅: Changelog Markdown Expandable Card
│    │    ├── 🖥️ [FE]₃₀․₅․₁: Summary Header Row (`summaryRow`), Content Box (`itemBox`) with toggle animation
│    │    └── ⚙️ [BE]₃₀․₅․₁․₁: MarkdownRenderer.renderChangelog
│    └── 🖱️ [EL]₃₀․₆: Download, Verify & Install Action Strip
│         ├── 🖥️ [FE]₃₀․₆․₁: Progress Bar: Indeterminate Download & Byte Counter (`tvBytesDownloaded`)
│         └── 🖥️ [FE]₃₀․₆․₂: "INSTALL UPDATE" Button
│              ├── (FR30.2): Verifies SHA-256 match; stages APK in `FileProvider`; launches PackageInstaller intent.
│              └── ⚙️ [BE]₃₀․₆․₂․₁: OtaPackageInstaller.stageAndInstallApk(apkUri)
```
*Testing Pass/Fail Scenario (FR30.2 / BFR30.2)*:
- **PASS**: Downloaded APK with matching SHA-256 hash launches Android system package installer screen automatically.
- **FAIL**: Corrupted download with mismatched SHA-256 aborts installation, deletes temp APK, and displays error banner.

---

#### 31. `UiConfigActivity` — Universal UI Scaling Matrix, Master Themes & Elevation Lab
```text
ROOT: UI_CONFIG_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.settings.UiConfigActivity)
│
├── 💼 [BR]₁: Unified UI Geometry, Scaling Matrix & Master Theme Architecture
│    ├── 🖱️ [EL]₃₁․₁: Global Navigation Bar & Back Button
│    │    ├── 🖥️ [FE]₃₁․₁․₁: Back Chevron (`backBtn`), Screen Title "UI ARCHITECT & CONFIG", Subtitle "L7 Matrix"
│    │    └── ⚙️ [BE]₃₁․₁․₁․₁: BaseActivityJava Universal Scaling Engine
│    ├── 🖱️ [EL]₃₁․₂: Universal Stage Scaler Slider Card
│    │    ├── 🖥️ [FE]₃₁․₂․₁: Section Header: `SYSTEM STAGE (UNIVERSAL SCALING)`
│    │    ├── 🖥️ [FE]₃₁․₂․₂: Slider: Scale Factor (0.70x to 1.40x) with Real-Time Multiplier Preview (e.g. `1.15x`)
│    │    │    ├── (FR31.1): Slider change dynamically alters `UiArchitect.setScaleFactor()`; triggers hot-reload of current activity.
│    │    │    └── ⚙️ [BE]₃₁․₂․₂․₁: UiArchitect.setScaleFactor(float)
│    │    │         └── 🗄️ [DB]₃₁․₂․₂․₁․₁: `system_registry.xml` (`KEY_UI_SCALE_FACTOR`)
│    ├── 🖱️ [EL]₃₁․₃: UI Scaling Matrix Table Card
│    │    ├── 🖥️ [FE]₃₁․₃․₁: Section Header: `UI SCALING MATRIX TABLE (L7 ARCHITECT)`
│    │    └── 🖥️ [FE]₃₁․₃․₂: Forensic Dimension Table: Base DP vs Scaled DP for Margins, Paddings, Corner Radii, Elevation
│    ├── 🖱️ [EL]₃₁․₄: Shadow & Elevation Depth Preview Lab
│    │    ├── 🖥️ [FE]₃₁․₄․₁: Section Header: `SHADOW VISIBILITY LAB (DEPTH PREVIEW)`
│    │    └── 🖥️ [FE]₃₁․₄․₂: Forensic Depth Stack: Cards rendered at 0dp, 2dp, 4dp, 8dp, 16dp elevation
│    ├── 🖱️ [EL]₃₁․₅: Structural Geometry (Corner Radii) Card
│    │    ├── 🖥️ [FE]₃₁․₅․₁: Section Header: `STRUCTURAL GEOMETRY (CORNER RADII)`
│    │    └── 🖥️ [FE]₃₁․₅․₂: Corner Radii Stepper: Compact (8dp), Modern (16dp), Full Bubble (28dp)
│    ├── 🖱️ [EL]₃₁․₆: Master Theme Palette Selector Deck
│    │    ├── 🖥️ [FE]₃₁․₆․₁: Section Header: `AESTHETIC MONITOR (MASTER THEMES)`
│    │    ├── 🖥️ [FE]₃₁․₆․₂: Follow System Theme Toggle (`systemThemeCard`, `cbSystemTheme`)
│    │    └── 🖥️ [FE]₃₁․₆․₃: Master Theme Tiles: `CYBER_STEALTH`, `HIGH_CONTRAST_MONO`, `EMERALD_SECURITY`, `TITANIUM_DARK`
│    │         └── ⚙️ [BE]₃₁․₆․₃․₁: ThemeEngine.applyMasterTheme(themeId)
│    │              └── 🗄️ [DB]₃₁․₆․₃․₁․₁: `system_registry.xml` (`KEY_MASTER_THEME_ID`)
│    └── 🖱️ [EL]₃₁․₇: Typography & Component Density Matrix Card
│         ├── 🖥️ [FE]₃₁․₇․₁: Section Header: `UNIFIED TYPOGRAPHY TIERS (UI ARCHITECT)`
│         └── 🖥️ [FE]₃₁․₇․₂: Typography Preview: Header L1 (22sp), Header L2 (18sp), Body (14sp), Micro Caption (11sp)
```
*Testing Pass/Fail Scenario (FR31.1 / BFR31.1)*:
- **PASS**: Dragging scale slider to 1.25x re-scales all cards, headers, fonts, and paddings immediately with no clipped text.
- **FAIL**: Setting scale factor < 0.5x or > 2.0x crashes UI with arithmetic overflow.

---

#### 32. `UiGalleryActivity` — Enterprise UI Component Design System & Widget Showcase
```text
ROOT: UI_GALLERY_ACTIVITY_MASTER_PLAN (com.jdroidx.jfindme.ui.settings.UiGalleryActivity)
│
├── 💼 [BR]₁: Component Design System Showcase & Widget Sandbox
│    ├── 🖱️ [EL]₃₂․₁: Global Navigation Bar & Back Button
│    │    ├── 🖥️ [FE]₃₂․₁․₁: Back Chevron (`backBtn`), Screen Title "UI COMPONENT GALLERY", Subtitle "Design System Catalog"
│    │    └── ⚙️ [BE]₃₂․₁․₁․₁: BaseActivity Navigation Lifecycle
│    ├── 🖱️ [EL]₃₂․₂: Interactive Button Showcase Matrix
│    │    ├── 🖥️ [FE]₃₂․₂․₁: Buttons: Primary Elevated, Secondary Ghost, Danger Crimson, Disabled State
│    │    └── ⚙️ [BE]₃₂․₂․₁․₁: UiArchitect.createButton & createActionButton
│    ├── 🖱️ [EL]₃₂․₃: Interactive Form Input & Card Showcase
│    │    ├── 🖥️ [FE]₃₂․₃․₁: Text Input Fields with Floating Labels, Error Micro-Badges, Prefix/Suffix Icons
│    │    └── ⚙️ [BE]₃₂․₃․₁․₁: UiArchitect.createInputCard
│    ├── 🖱️ [EL]₃₂․₄: Switch, Slider & Toggle Matrix
│    │    ├── 🖥️ [FE]₃₂․₄․₁: Switches with Dual-State Accent Color Indicators (Emerald Checked, Slate Unchecked)
│    │    └── ⚙️ [BE]₃₂․₄․₁․₁: UiArchitect.createSwitch
│    └── 🖱️ [EL]₃₂․₅: System Stage Tester Action Strip (`stageBtn`)
│         ├── 🖥️ [FE]₃₂․₅․₁: "STAGE IN MULTI-DEVICE SIMULATOR" Action Button
│         └── ⚙️ [BE]₃₂․₅․₁․₁: ComponentSimulator.runAllInteractionsBenchmark
```
*Testing Pass/Fail Scenario (FR32.1 / BFR32.1)*:
- **PASS**: Clicking any button in gallery triggers expected ripple effect, state change, and toast feedback without ANR.
- **FAIL**: Rendering complex nested cards on low-memory device triggers OOM exception.


## Section B: 11 Web Mission Control Screens Specification

### 1. `index.html` — Master Web Mission Control SPA Frame & Shell
```text
ROOT: WEB_SPA_SHELL_MASTER_PLAN (jFindMeWeb/index.html)
│
├── 💼 [BR]₁: Web Mission Control Master Frame & Lifecycle Control
│    ├── 🖱️ [EL]₁․₁: Global Brand Identity & Navigation Topbar
│    │    ├── 🖥️ [FE]₁․₁․₁: Application Logo ("jFindMe Mission Control"), Version Chip ("v2.4.0-web"), Security Seal
│    │    └── ⚙️ [BE]₁․₁․₁․₁: WebShellController.initApplicationShell
│    ├── 🖱️ [EL]₁․₂: WebSocket Real-Time Telemetry Status Banner
│    │    ├── 🖥️ [FE]₁․₂․₁: Pulse Indicator: Green (Connected), Amber (Reconnecting), Crimson (Offline)
│    │    │    ├── (FR-W1.1): Updates state within 100ms of WebSocket event; shows tooltip with active socket URL.
│    │    │    └── ⚙️ [BE]₁․₂․₁․₁: FirebaseRealtimeSocketClient.onConnectionChange
│    │    │         └── 🗄️ [DB]₁․₂․₁․₁․₁: Firebase RTDB `/.info/connected`
│    ├── 🖱️ [EL]₁․₃: Master Navigation Sidebar & View Switcher
│    │    ├── 🖥️ [FE]₁․₃․₁: Nav Links: Dashboard, Map, Devices, History, Geofencing, Eagle Analytics, Sync Settings
│    │    │    └── ⚙️ [BE]₁․₃․₁․₁: Router.navigateTo(routePath)
│    └── 🖱️ [EL]₁․₄: SPA Dynamic Content Container (`#app-content`)
│         └── 🖥️ [FE]₁․₄․₁: View Mount Anchor: Renders active JavaScript View component with clean DOM replacement
```
*Testing Pass/Fail Scenario (FR-W1.1 / BFR-W1.1)*:
- **PASS**: Simulating network drop changes socket badge to Crimson "DISCONNECTED" in < 150ms.
- **FAIL**: Restoring connection fails to re-subscribe to active telemetry streams.

---

### 2. `DashboardView.js` — Operations Dashboard & High-Level Fleet Telemetry
```text
ROOT: DASHBOARD_VIEW_MASTER_PLAN (jFindMeWeb/js/views/DashboardView.js)
│
├── 💼 [BR]₁: Fleet Overview & High-Level Operational Metrics
│    ├── 🖱️ [EL]₂․₁: Operational Health Summary KPI Deck (4 Metric Cards)
│    │    ├── 🖥️ [FE]₂․₁․₁: Card 1: Active Nodes Count (`activeDevicesCount`)
│    │    ├── 🖥️ [FE]₂․₁․₂: Card 2: Critical Alerts Count (`criticalAlertsCount`)
│    │    ├── 🖥️ [FE]₂․₁․₃: Card 3: Geofence Breaches (Last 24h) (`geofenceBreachCount`)
│    │    └── 🖥️ [FE]₂․₁․₄: Card 4: Average Fleet Ping Latency (`avgLatencyMs`)
│    │         └── ⚙️ [BE]₂․₁․₄․₁: TelemetryAggregator.computeDashboardMetrics
│    │              └── 🗄️ [DB]₂․₁․₄․₁․₁: SQLite `locations`, `audit_log`, `notifications`
│    ├── 🖱️ [EL]₂․₂: Real-Time Fleet Movement Activity Feed
│    │    ├── 🖥️ [FE]₂․₂․₁: Live Scrolling Feed: "Device #X entered Safe Zone Y", "Node #Z Low Battery (12%)"
│    │    └── ⚙️ [BE]₂․₂․₁․₁: ActivityFeedController.subscribeLiveEvents
│    └── 🖱️ [EL]₂․₃: Quick-Action Tactical Launchpad
│         ├── 🖥️ [FE]₂․₃․₁: "JUMP TO FULL TACTICAL MAP" Button -> Navigates to `MapView.js`
│         └── 🖥️ [FE]₂․₃․₂: "AUDIT STALE DEVICES" Button -> Navigates to `DeviceListView.js?filter=stale`
```
*Testing Pass/Fail Scenario (FR-W2.1 / BFR-W2.1)*:
- **PASS**: Receiving new telemetry point instantly updates "Active Nodes Count" and adds row to live feed in < 50ms.
- **FAIL**: Rapid burst of 100 events causes UI freeze or browser memory leak.

---

### 3. `MapView.js` — Interactive Mission Tactical Map & Device Tracking
```text
ROOT: MAP_VIEW_MASTER_PLAN (jFindMeWeb/js/views/MapView.js)
│
├── 💼 [BR]₁: Real-Time Geospatial Visualization & Node Trajectory Mapping
│    ├── 🖱️ [EL]₃․₁: Leaflet / OpenStreetMap Canvas Viewport (`#map-container`)
│    │    ├── 🖥️ [FE]₃․₁․₁: Full-Screen Map Container with Layer Control (Satellite, Tactical Dark, Streets)
│    │    └── ⚙️ [BE]₃․₁․₁․₁: LeafletMapEngine.initMap
│    ├── 🖱️ [EL]₃․₂: Real-Time Fleet Device Markers Layer
│    │    ├── 🖥️ [FE]₃․₂․₁: Custom CSS Pulsing Markers (Green: Online, Amber: Stale, Crimson: SOS Alert)
│    │    │    ├── (FR-W3.1): Marker click opens Rich Popup: Node Name, Lat/Lng, Speed (km/h), Altitude, Battery %, Last Ping.
│    │    │    └── ⚙️ [BE]₃․₂․₁․₁: MarkerClusterGroup.updateMarkerPosition
│    ├── 🖱️ [EL]₃․₃: Geofence Boundaries Overlay Layer
│    │    ├── 🖥️ [FE]₃․₃․₁: Polygons and Circles (Emerald: Safe Zone, Crimson: Danger/Restricted Zone)
│    │    └── ⚙️ [BE]₃․₃․₁․₁: GeofenceLayerManager.renderPolygons
│    └── 🖱️ [EL]₃․₄: Tactical Map Control Strip
│         ├── 🖥️ [FE]₃․₄․₁: "FIT FLEET TO EXTENTS" Button
│         ├── 🖥️ [FE]₃․₄․₂: "FOLLOW SELECTED DEVICE" Toggle
│         └── 🖥️ [FE]₃․₄․₃: "HEATMAP DENSITY OVERLAY" Toggle
```
*Testing Pass/Fail Scenario (FR-W3.1 / BFR-W3.1)*:
- **PASS**: Clicking device marker opens popup with real-time speed, battery, and address in < 25ms.
- **FAIL**: Moving node leaves phantom marker or unclosed popup on map.

---

### 4. `DeviceListView.js` — Fleet & Device Registry Matrix
```text
ROOT: DEVICE_LIST_VIEW_MASTER_PLAN (jFindMeWeb/js/views/DeviceListView.js)
│
├── 💼 [BR]₁: Fleet Node Registry, Pagination, Filtering & State Audit
│    ├── 🖱️ [EL]₄․₁: Fleet Search & Filter Action Bar
│    │    ├── 🖥️ [FE]₄․₁․₁: Search Input Box (`#search-device-input`): Name, IMEI, Phone, UID
│    │    ├── 🖥️ [FE]₄․₁․₂: Status Filter Dropdown (`#filter-status`): `ALL`, `ONLINE`, `STALE`, `SOS`
│    │    └── ⚙️ [BE]₄․₁․₂․₁: DeviceFilterEngine.applyFilter
│    ├── 🖱️ [EL]₄․₂: Interactive Device Matrix Data Table (`#devices-table`)
│    │    ├── 🖥️ [FE]₄․₂․₁: Columns: Status Badge, Device Name, Role, Battery Level %, Last Ping, GPS Coordinates, Actions
│    │    └── ⚙️ [BE]₄․₂․₁․₁: DeviceTableRenderer.renderRows
│    └── 🖱️ [EL]₄․₃: Row Context Action Buttons
│         ├── 🖥️ [FE]₄․₃․₁: "EDIT" Button (`.btn-edit-device`) -> Opens `DeviceEditModalView.js`
│         ├── 🖥️ [FE]₄․₃․₂: "DELETE" Button (`.btn-delete-device`) -> Opens `DeviceDeleteModalView.js`
│         └── 🖥️ [FE]₄․₃․₃: "HISTORY" Button (`.btn-history-device`) -> Navigates to `HistoryTrailView.js`
```
*Testing Pass/Fail Scenario (FR-W4.1 / BFR-W4.1)*:
- **PASS**: Selecting "STALE" filter hides online devices and displays only devices with last ping > 15 minutes.
- **FAIL**: Sorting by battery percentage crashes when encountering node with null battery payload.

---

### 5. `DeviceEditModalView.js` — Device Configuration Modal
```text
ROOT: DEVICE_EDIT_MODAL_VIEW_MASTER_PLAN (jFindMeWeb/js/views/DeviceEditModalView.js)
│
├── 💼 [BR]₁: Device Configuration, Alias Assignment & Role Governance
│    ├── 🖱️ [EL]₅․₁: Modal Shell & Header
│    │    ├── 🖥️ [FE]₅․₁․₁: Modal Title: "CONFIGURE FLEET NODE", Close 'X' Button (`#btn-close-edit-modal`)
│    │    └── ⚙️ [BE]₅․₁․₁․₁: ModalController.openModal
│    ├── 🖱️ [EL]₅․₂: Device Metadata Form Fields
│    │    ├── 🖥️ [FE]₅․₂․₁: Device Name Input (`#input-device-name`)
│    │    ├── 🖥️ [FE]₅․₂․₂: Phone Number Input (`#input-device-phone`)
│    │    ├── 🖥️ [FE]₅․₂․₃: Role Selector (`#select-device-role`): `ADMIN`, `MEMBER`, `OBSERVER`
│    │    └── 🖥️ [FE]₅․₂․₄: Tracking Interval Dropdown (`#select-tracking-interval`)
│    └── 🖱️ [EL]₅․₃: Form Submission Action Buttons
│         ├── 🖥️ [FE]₅․₃․₁: "SAVE CONFIGURATION" Button (`#btn-save-device`)
│         │    ├── (FR-W5.1): Validates phone format and non-empty name; sends PUT request to API / Firebase RTDB.
│         │    └── ⚙️ [BE]₅․₃․₁․₁: DeviceRepository.updateDeviceConfig
│         │         └── 🗄️ [DB]₅․₃․₁․₁․₁: Firebase RTDB `/devices/{id}/config`
│         └── 🖥️ [FE]₅․₃․₂: "CANCEL" Button (`#btn-cancel-edit`)
```
*Testing Pass/Fail Scenario (FR-W5.1 / BFR-W5.1)*:
- **PASS**: Updating device alias to "Patrol Alpha" saves to cloud and updates Device Table immediately.
- **FAIL**: Submitting empty string as device name bypasses validation and corrupts record.

---

### 6. `DeviceDeleteModalView.js` — Device De-registration Modal
```text
ROOT: DEVICE_DELETE_MODAL_VIEW_MASTER_PLAN (jFindMeWeb/js/views/DeviceDeleteModalView.js)
│
├── 💼 [BR]₁: Cryptographic De-registration & Fleet Node Removal
│    ├── 🖱️ [EL]₆․₁: Danger Modal Shell & Notice
│    │    ├── 🖥️ [FE]₆․₁․₁: Warning Header: "DE-REGISTER FLEET NODE", Device Identifier Label
│    │    └── ⚙️ [BE]₆․₁․₁․₁: ModalController.openDangerModal
│    ├── 🖱️ [EL]₆․₂: Historical Data Retention Policy Checkbox
│    │    ├── 🖥️ [FE]₆․₂․₁: Checkbox: "Purge all historical GPS breadcrumbs permanently" (`#cb-purge-history`)
│    │    └── ⚙️ [BE]₆․₂․₁․₁: DeviceDeleteController.setPurgeHistoricalData
│    └── 🖱️ [EL]₆․₃: Confirmation Action Buttons
│         ├── 🖥️ [FE]₆․₃․₁: Crimson Button: "CONFIRM DE-REGISTRATION" (`#btn-confirm-delete`)
│         │    ├── (FR-W6.1): Removes node from `/devices` and `/groups/{groupId}/members`; logs to audit ledger.
│         │    └── ⚙️ [BE]₆․₃․₁․₁: DeviceRepository.deleteDevice
│         └── 🖥️ [FE]₆․₃․₂: "ABORT / CANCEL" Button (`#btn-cancel-delete`)
```
*Testing Pass/Fail Scenario (FR-W6.1 / BFR-W6.1)*:
- **PASS**: Confirming de-registration removes node from active fleet and cleans up map marker in < 50ms.
- **FAIL**: Aborting modal dismisses dialog without deleting or modifying device state.

---

### 7. `DupDeviceView.js` — Duplicate Device Remediation Matrix
```text
ROOT: DUP_DEVICE_VIEW_MASTER_PLAN (jFindMeWeb/js/views/DupDeviceView.js)
│
├── 💼 [BR]₁: Multi-Node Conflict Resolution & Duplicate Telemetry Merging
│    ├── 🖱️ [EL]₇․₁: Duplicate Detection Action Header
│    │    ├── 🖥️ [FE]₇․₁․₁: Title: "DUPLICATE DEVICE REMEDIATION", Subtitle: "Conflict Resolver"
│    │    └── 🖥️ [FE]₇․₁․₂: "RUN DUPLICATE SCAN" Action Button (`#btn-scan-dups`)
│    │         └── ⚙️ [BE]₇․₁․₂․₁: DuplicateDeviceScanner.findDuplicateIdentities
│    ├── 🖱️ [EL]₇․₂: Duplicate Collision Pairs Matrix Table
│    │    ├── 🖥️ [FE]₇․₂․₁: Columns: Primary Node UID, Colliding Duplicate UID, Match Heuristic (IMEI / Phone), Last Active
│    │    └── ⚙️ [BE]₇․₂․₁․₁: DupTableRenderer.renderCollisions
│    └── 🖱️ [EL]₇․₃: Resolution Action Buttons Strip
│         ├── 🖥️ [FE]₇․₃․₁: "MERGE HISTORICAL TELEMETRY" Button (`.btn-merge-dup`)
│         │    └── ⚙️ [BE]₇․₃․₁․₁: TelemetryMerger.mergeDeviceBreadcrumbs(primaryUid, dupUid)
│         └── 🖥️ [FE]₇․₃․₂: "PURGE DUPLICATE RECORD" Button (`.btn-purge-dup`)
│              └── ⚙️ [BE]₇․₃․₂․₁: DeviceRepository.purgeDuplicateNode(dupUid)
```
*Testing Pass/Fail Scenario (FR-W7.1 / BFR-W7.1)*:
- **PASS**: Merging duplicate node transfers 450 location records to primary UID and deletes duplicate stub record.
- **FAIL**: Network timeout during merge leaves orphaned records in SQLite.

---

### 8. `HistoryTrailView.js` — Historical Breadcrumb & Trajectory Engine
```text
ROOT: HISTORY_TRAIL_VIEW_MASTER_PLAN (jFindMeWeb/js/views/HistoryTrailView.js)
│
├── 💼 [BR]₁: Historical Breadcrumb Replay, Time-Series Scrubber & Speed Analytics
│    ├── 🖱️ [EL]₈․₁: Trajectory Filter & Date Range Picker
│    │    ├── 🖥️ [FE]₈․₁․₁: Target Device Selector Dropdown (`#select-history-device`)
│    │    ├── 🖥️ [FE]₈․₁․₂: Start DateTime Picker (`#input-start-time`) & End DateTime Picker (`#input-end-time`)
│    │    └── 🖥️ [FE]₈․₁․₃: "LOAD TRAIL" Action Button (`#btn-load-history`)
│    │         └── ⚙️ [BE]₈․₁․₃․₁: HistoryRepository.queryHistoricalTrail(uid, start, end)
│    ├── 🖱️ [EL]₈․₂: Interactive Time Scrubber & Replay Controller Strip
│    │    ├── 🖥️ [FE]₈․₂․₁: Play / Pause Button (`#btn-play-pause`), Stop Button (`#btn-stop-replay`)
│    │    ├── 🖥️ [FE]₈․₂․₂: Playback Speed Selector (`#select-playback-speed`): `1x`, `5x`, `20x`, `60x`
│    │    └── 🖥️ [FE]₈․₂․₃: Time Timeline Scrubber Slider (`#slider-trail-scrubber`)
│    │         ├── (FR-W8.1): Dragging slider updates the position of simulated device marker along the polyline path live.
│    │         └── ⚙️ [BE]₈․₂․₃․₁: ReplayEngine.seekToFraction(fraction)
│    └── 🖱️ [EL]₈․₃: Trajectory Statistics Deck
│         ├── 🖥️ [FE]₈․₃․₁: Total Distance Traveled (km), Top Speed Recorded (km/h), Total Stationary Duration
│         └── ⚙️ [BE]₈․₃․₁․₁: AnalyticsEngine.calculateRouteStats
```
*Testing Pass/Fail Scenario (FR-W8.1 / BFR-W8.1)*:
- **PASS**: Pressing "Play" animates vehicle marker along polyline at 5x speed, updating speedometer gauge live.
- **FAIL**: Querying range with 0 recorded points displays empty state instead of crashing Leaflet polyline builder.

---

### 9. `GeofencingView.js` — Geofencing Boundary Builder & Perimeter Sentinel
```text
ROOT: GEOFENCING_VIEW_MASTER_PLAN (jFindMeWeb/js/views/GeofencingView.js)
│
├── 💼 [BR]₁: Geofence Creation, Polygon Drawing & Perimeter Breach Policy
│    ├── 🖱️ [EL]₉․₁: Perimeter Drawing Toolbar
│    │    ├── 🖥️ [FE]₉․₁․₁: "DRAW CIRCLE SAFE ZONE" Tool Button (`#btn-draw-circle`)
│    │    ├── 🖥️ [FE]₉․₁․₂: "DRAW POLYGON RESTRICTED ZONE" Tool Button (`#btn-draw-polygon`)
│    │    └── 🖥️ [FE]₉․₁․₃: "EDIT / RESHAPE BOUNDARY" Tool Button (`#btn-edit-boundary`)
│    │         └── ⚙️ [BE]₉․₁․₃․₁: LeafletDraw.enableDrawMode
│    ├── 🖱️ [EL]₉․₂: Boundary Metadata & Alert Policy Form Card
│    │    ├── 🖥️ [FE]₉․₂․₁: Zone Name Input (`#input-zone-name`)
│    │    ├── 🖥️ [FE]₉․₂․₂: Zone Classification (`#select-zone-type`): `SAFE_ZONE`, `RESTRICTED_ZONE`, `HAZARD`
│    │    ├── 🖥️ [FE]₉․₂․₃: Alert Triggers: Checkboxes: "Alert on Entry", "Alert on Exit", "Alert on Dwell > 10m"
│    │    └── 🖥️ [FE]₉․₂․₄: "SAVE GEOFENCE BOUNDARY" Button (`#btn-save-geofence`)
│    │         ├── (FR-W9.1): Saves polygon vertices (GeoJSON) and alert rules to backend and broadcasts to fleet nodes.
│    │         └── ⚙️ [BE]₉․₂․₄․₁: GeofenceRepository.saveGeofence
│    │              └── 🗄️ [DB]₉․₂․₄․₁․₁: Firebase RTDB `/geofences/{id}`
│    └── 🖱️ [EL]₉․₃: Active Geofence Catalog List
│         ├── 🖥️ [FE]₉․₃․₁: Catalog Card List: Zone Name, Radius / Area, Active Devices Inside, Toggle Active Switch
│         └── ⚙️ [BE]₉․₃․₁․₁: GeofenceListRenderer.renderCatalog
```
*Testing Pass/Fail Scenario (FR-W9.1 / BFR-W9.1)*:
- **PASS**: Drawing circular safe zone and saving commits GeoJSON to Firebase; connected devices receive fence within 200ms.
- **FAIL**: Attempting to save polygon with fewer than 3 vertices displays validation error.

---

### 10. `EagleAnalyticsTreeView.js` — Eagle Analytics Deep-Dive Tree
```text
ROOT: EAGLE_ANALYTICS_TREE_VIEW_MASTER_PLAN (jFindMeWeb/js/views/EagleAnalyticsTreeView.js)
│
├── 💼 [BR]₁: Deep-Dive Forensic Analytics, Cluster Trees & Anomaly Detection
│    ├── 🖱️ [EL]₁₀․₁: Global Time-Horizon Selector
│    │    ├── 🖥️ [FE]₁₀․₁․₁: Range Buttons: `TODAY`, `LAST 7 DAYS`, `LAST 30 DAYS`, `CUSTOM RANGE`
│    │    └── ⚙️ [BE]₁₀․₁․₁․₁: AnalyticsController.setTimeHorizon
│    ├── 🖱️ [EL]₁₀․₂: Hierarchical Cluster Tree Visualizer (`#eagle-tree-container`)
│    │    ├── 🖥️ [FE]₁₀․₂․₁: Expandable Nodes: Fleet Squadron -> Member Unit -> Route Leg -> Stopping Point
│    │    │    └── ⚙️ [BE]₁₀․₂․₁․₁: D3TreeRenderer.renderHierarchy
│    ├── 🖱️ [EL]₁₀․₃: Anomaly & Risk Detection Matrix
│    │    ├── 🖥️ [FE]₁₀․₃․₁: Cards: Speeding Incidents Count, Geofence Infiltration Alerts, Battery Depletion Curve
│    │    └── ⚙️ [BE]₁₀․₃․₁․₁: AnomalyDetector.computeOutliers
│    └── 🖱️ [EL]₁₀․₄: Forensic Export Strip
│         ├── 🖥️ [FE]₁₀․₄․₁: "EXPORT PDF MISSION REPORT" Button
│         └── 🖥️ [FE]₁₀․₄․₂: "EXPORT CSV TELEMETRY DUMP" Button
```
*Testing Pass/Fail Scenario (FR-W10.1 / BFR-W10.1)*:
- **PASS**: Expanding Squadron node reveals 5 child devices and their sub-clusters with smooth D3 collapsible tree animation.
- **FAIL**: Querying 100,000 points without pagination causes browser memory crash.

---

### 11. `AlertBannerView.js` & `database-sync-settings.html` — Database Synchronization & Cloud Control
```text
ROOT: DATABASE_SYNC_SETTINGS_MASTER_PLAN (jFindMeWeb/database-sync-settings.html & AlertBannerView.js)
│
├── 💼 [BR]₁: Cloud Sync Engine, Local SQLite Bridge & Emergency Broadcast
│    ├── 🖱️ [EL]₁₁․₁: Global Critical Emergency Alert Banner (`AlertBannerView.js`)
│    │    ├── 🖥️ [FE]₁₁․₁․₁: Crimson Floating Banner: "SOS BROADCAST: Node #4 Panic Button Triggered!", Dismiss 'X'
│    │    │    ├── (FR-W11.1): Appears immediately on top of any active web view; pulses crimson until acknowledged.
│    │    │    └── ⚙️ [BE]₁₁․₁․₁․₁: AlertBannerController.subscribeEmergencyBroadcasts
│    │    │         └── 🗄️ [DB]₁₁․₁․₁․₁․₁: Firebase RTDB `/alerts/sos`
│    ├── 🖱️ [EL]₁₁․₂: Cloud Database Synchronization Card (`database-sync-settings.html`)
│    │    ├── 🖥️ [FE]₁₁․₂․₁: Sync Status Badge (`SYNCHRONIZED`, `PENDING_CHANGES`, `SYNC_ERROR`)
│    │    ├── 🖥️ [FE]₁₁․₂․₂: "FORCE BIDIRECTIONAL SYNC" Button (`#btn-force-sync`)
│    │    │    └── ⚙️ [BE]₁₁․₂․₂․₁: SyncEngine.executeBidirectionalReconciliation
│    │    └── 🖥️ [FE]₁₁․₂․₃: Auto-Sync Cadence Stepper (Options: 30s, 60s, 300s)
│    └── 🖱️ [EL]₁₁․₃: Local SQLite Bridge Endpoint Configuration
│         ├── 🖥️ [FE]₁₁․₃․₁: Bridge Host URL Input (`#input-bridge-url`), API Key Input (`#input-bridge-key`)
│         └── 🖥️ [FE]₁₁․₃․₂: "TEST BRIDGE CONNECTION" Action Button
```
*Testing Pass/Fail Scenario (FR-W11.1 / BFR-W11.1)*:
- **PASS**: Receiving emergency alert triggers banner pulse, plays alert tone, and centers map on transmitting node.
- **FAIL**: Acknowledging alert banner fails to update `/alerts/sos` state, causing banner to reappear on refresh.


## Section C: Android Native App Visual Mindmap (32 Screens)

```mermaid
flowchart LR
    Root["jFindMe Enterprise Android Architecture (32 Screens)"]

    %% Domain 1: Boot & Identity
    Root --> D1["1. Boot & Identity Domain"]
    D1 --> S01["01. SplashActivity (Attestation & Integrity)"]
    D1 --> S02["02. AuthActivity (PIN & OTP Auth)"]
    D1 --> S03["03. ProfileActivity (Operator Identity)"]
    D1 --> S04["04. AvatarEditorActivity (Vector Avatar Shield)"]
    D1 --> S08["08. AvatarCropActivity (Biometric Image Cropper)"]

    %% Domain 2: Tactical Navigation & Fleet Dashboards
    Root --> D2["2. Tactical Navigation & Fleet Dashboards"]
    D2 --> S05["05. MapActivity (Live Tactical GPS & SOS)"]
    D2 --> S06["06. LiveMapDashboardActivity (Fleet Radar Grid)"]
    D2 --> S07["07. PersonDetailsActivity (Silicon Node Inspector)"]

    %% Domain 3: Core Diagnostics & Closed-Loop Engine
    Root --> D3["3. Hardware Diagnostics & Closed-Loop Engine"]
    D3 --> S09["09. HealthActivity (Cell Tower & Closed-Loop RTT)"]
    D3 --> S10["10. WatchDogAuditActivity (Dead-Mans Switch)"]
    D3 --> S11["11. SystemLogActivity (Real-Time Logcat Terminal)"]
    D3 --> S12["12. BackgroundJobsConfigDialog (WorkManager Tuner)"]

    %% Domain 4: Geospatial Boundary & Acoustic Sirens
    Root --> D4["4. Geospatial Boundary & Acoustic Sirens"]
    D4 --> S13["13. HistoryActivity (Breadcrumb Polyline Scrubber)"]
    D4 --> S14["14. GeofenceActivity (Safe Zone Perimeter Studio)"]
    D4 --> S15["15. GeofenceNotificationActivity (Breach Alert HUD)"]
    D4 --> S16["16. AcousticSirenActivity (105dB Strobe Alarm)"]

    %% Domain 5: Fleet Governance & Multi-Node Mesh
    Root --> D5["5. Fleet Governance & Multi-Node Mesh"]
    D5 --> S17["17. GroupActivity (Legacy Group Router)"]
    D5 --> S18["18. FamilyFriendsActivity (Unified Social Fleet)"]
    D5 --> S19["19. NetworkGatewayActivity (Cloud/P2P Pairing Hub)"]
    D5 --> S20["20. OrgActivity (Enterprise Multi-Tenant Tenant)"]
    D5 --> S21["21. DatabaseManagementActivity (SQLite Schema V80)"]
    D5 --> S22["22. FactoryResetActivity (2-Stage Crypto Wipe)"]
    D5 --> S23["23. FirebaseCloudMatrixActivity (WebSocket RTDB Monitor)"]
    D5 --> S24["24. ForensicVaultExplorerActivity (Audit Vault Reader)"]

    %% Domain 6: Tactical Alerts, Privacy & UI Architecture
    Root --> D6["6. Tactical Alerts, Privacy & UI Architecture"]
    D6 --> S25["25. NotifActivity (Tactical Alert Ledger)"]
    D6 --> S26["26. PrivacyActivity (Ghost Mode & Spatial Fuzzing)"]
    D6 --> S27["27. SettingsActivity (7-Domain Master Config Engine)"]
    D6 --> S28["28. AboutActivity (Open Source Licenses & Build Provenance)"]
    D6 --> S29["29. HelpSupportActivity (FAQ & Diagnostic Bundler)"]
    D6 --> S30["30. SystemUpdateActivity (OTA Multi-Source Verifier)"]
    D6 --> S31["31. UiConfigActivity (Universal Stage Scaling Matrix)"]
    D6 --> S32["32. UiGalleryActivity (UI Component Design System)"]
```

---

## Section D: Android Native App Graphify Interaction Flowchart

```mermaid
flowchart TD
    %% Entry Point
    START([System Boot / App Launch]) --> Splash["01. SplashActivity<br/>(Security Attestation & DB v76 Check)"]

    %% Auth Branch
    Splash -->|Auth Valid| Map["05. MapActivity<br/>(Tactical GPS & Live Radar)"]
    Splash -->|Auth Required| Auth["02. AuthActivity<br/>(PIN / OTP Verification)"]
    Auth -->|Authenticated| Map

    %% Main Tactical Hub
    Map -->|Fleet Radar Button| LiveMap["06. LiveMapDashboardActivity<br/>(Multi-Node Realtime Grid)"]
    Map -->|Emergency SOS Button| Siren["16. AcousticSirenActivity<br/>(105dB Strobe & SMS Broadcast)"]
    Map -->|Drawer / Menu| Fleet["18. FamilyFriendsActivity<br/>(Unified Social Circle)"]
    Map -->|Vitals Header| Health["09. HealthActivity<br/>(Closed-Loop RTT & Tower Telemetry)"]
    Map -->|Settings Gear| Settings["27. SettingsActivity<br/>(7 Priority Domains)"]

    %% Fleet Governance Sub-Flow
    Fleet -->|Tap Member Card| Person["07. PersonDetailsActivity<br/>(Node Telemetry Inspector)"]
    Fleet -->|Pair New Device| Gateway["19. NetworkGatewayActivity<br/>(Cloud PIN & P2P Token Pairing)"]
    Fleet -->|Profile Header| Profile["03. ProfileActivity<br/>(Operator Identity)"]
    Profile -->|Edit Avatar| AvatarEdit["04. AvatarEditorActivity"]
    AvatarEdit -->|Crop Image| AvatarCrop["08. AvatarCropActivity"]

    %% Diagnostics Sub-Flow
    Health -->|WatchDog Status| WatchDog["10. WatchDogAuditActivity<br/>(Dead-Mans Heartbeat)"]
    Health -->|System Logs| Logcat["11. SystemLogActivity<br/>(Live Terminal)"]
    Health -->|Tune Cadence| Jobs["12. BackgroundJobsConfigDialog<br/>(WorkManager Tuner)"]

    %% Geofence & History Sub-Flow
    Map -->|History Trajectory| History["13. HistoryActivity<br/>(Time Scrubber Replay)"]
    Map -->|Geofence Editor| Geofence["14. GeofenceActivity<br/>(Safe Zone Perimeter Studio)"]
    Geofence -->|Perimeter Breach Alert| GeofenceNotif["15. GeofenceNotificationActivity<br/>(Breach Warning HUD)"]

    %% Settings & Admin Sub-Flow
    Settings -->|Priority 6: Storage| DbMgmt["21. DatabaseManagementActivity<br/>(Vacuum & Table Audit)"]
    DbMgmt -->|Danger Zone| Reset["22. FactoryResetActivity<br/>(2-Stage Cryptographic Wipe)"]
    Settings -->|Priority 7: Audit| Vault["24. ForensicVaultExplorerActivity<br/>(Audit Vault & Hash Chain)"]
    Settings -->|Priority 2: Cloud| CloudMat["23. FirebaseCloudMatrixActivity<br/>(RTDB WebSocket Monitor)"]
    Settings -->|Enterprise Policy| Org["20. OrgActivity<br/>(Multi-Tenant Governance)"]
    Settings -->|Privacy Shield| Privacy["26. PrivacyActivity<br/>(Ghost Mode & Spatial Fuzzing)"]
    Settings -->|Alert Center| Notifs["25. NotifActivity<br/>(Tactical Notification Ledger)"]
    Settings -->|UI Architect| UiConfig["31. UiConfigActivity<br/>(Stage Scaler & Themes)"]
    UiConfig -->|Component Lab| UiGallery["32. UiGalleryActivity<br/>(Design System Showcase)"]
    Settings -->|OTA Updates| OtaUpdate["30. SystemUpdateActivity<br/>(Drive/GitHub/Local OTA)"]
    Settings -->|Help & FAQ| Help["29. HelpSupportActivity<br/>(Diagnostic Bundler)"]
    Settings -->|About App| About["28. AboutActivity<br/>(Licenses & Git Commit Hash)"]
```

---

## Section E: Web Mission Control Visual Mindmap (11 Screens)

```mermaid
flowchart LR
    WebRoot["jFindMe Web Mission Control Architecture (11 Views)"]

    %% Core Views
    WebRoot --> W01["01. index.html (Master SPA Shell & Live Banner)"]
    WebRoot --> W02["02. DashboardView.js (Fleet Operations KPI Deck)"]
    WebRoot --> W03["03. MapView.js (Tactical Leaflet Map & Clusters)"]
    WebRoot --> W04["04. DeviceListView.js (Fleet Registry & Search)"]
    WebRoot --> W05["05. DeviceEditModalView.js (Device Configuration Modal)"]
    WebRoot --> W06["06. DeviceDeleteModalView.js (De-Registration Modal)"]
    WebRoot --> W07["07. DupDeviceView.js (Duplicate Conflict Resolver)"]
    WebRoot --> W08["08. HistoryTrailView.js (Historical Breadcrumb Replay)"]
    WebRoot --> W09["09. GeofencingView.js (Boundary Polygon Builder)"]
    WebRoot --> W10["10. EagleAnalyticsTreeView.js (D3 Deep-Dive Analytics)"]
    WebRoot --> W11["11. AlertBannerView.js & database-sync-settings.html (Cloud Sync)"]
```

---

## Section F: Web Mission Control Graphify Interaction Flowchart

```mermaid
flowchart TD
    %% Entry & Shell
    WebStart([Operator Browser Launch]) --> WebShell["01. index.html<br/>(Master Web SPA Shell & Telemetry Banner)"]

    %% View Routing
    WebShell --> Dashboard["02. DashboardView.js<br/>(Operations Overview & Fleet KPI Deck)"]
    WebShell --> TacticalMap["03. MapView.js<br/>(Leaflet Interactive Fleet Map & Geofence Layers)"]
    WebShell --> DeviceList["04. DeviceListView.js<br/>(Device Registry & Real-Time Filter Table)"]
    WebShell --> HistoryTrail["08. HistoryTrailView.js<br/>(Time-Series Scrubber & Speed Analytics)"]
    WebShell --> GeofenceStudio["09. GeofencingView.js<br/>(Polygon & Circle Boundary Studio)"]
    WebShell --> EagleAnalytics["10. EagleAnalyticsTreeView.js<br/>(D3 Collapsible Cluster Hierarchy)"]
    WebShell --> CloudSync["11. database-sync-settings.html<br/>(Bidirectional Cloud Sync Engine)"]

    %% Modal Interactions
    DeviceList -->|Click Edit| EditModal["05. DeviceEditModalView.js<br/>(Alias, Role & Cadence Form)"]
    DeviceList -->|Click Delete| DeleteModal["06. DeviceDeleteModalView.js<br/>(Cryptographic Purge Confirmation)"]
    DeviceList -->|Audit Duplicates| DupMatrix["07. DupDeviceView.js<br/>(Conflict Detection & Merge Engine)"]

    %% Tactical Linkages
    Dashboard -->|Jump to Map| TacticalMap
    TacticalMap -->|Click Device Popup| HistoryTrail
    TacticalMap -->|View Boundary Rules| GeofenceStudio
    Dashboard -->|Investigate Outliers| EagleAnalytics

    %% Global Emergency Floating Overlay
    EmergencyEvent([Cloud SOS Emergency Event]) -.-> EmergencyBanner["11. AlertBannerView.js<br/>(Global Pulsing Crimson Warning Banner)"]
    EmergencyBanner -.->|Center on Threat| TacticalMap
```
