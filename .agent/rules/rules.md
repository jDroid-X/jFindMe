# jFindMe Enterprise Rules & Standards (The Constitution V5.0)

> **Mandatory Reference Pipeline**: `Workflow (.agent/rules/workflow.md)` ➔ `Rules (.agent/rules/rules.md)` ➔ `Agents (.agent/rules/agents.md)` ➔ `Skills (.agent/rules/skills.md)` ➔ `Branding (.agent/rules/branding.md)`

---

## 🏛️ Tier 1: Universal Rules (Apply to ALL Changes)

Every modification, update, bug fix, or new feature—regardless of target file or screen—must pass these universal gates first:

### 1. Pre-Change Declaration & Post-Change Verification
Before modifying any line of code, first declare:
$$\text{[Variables]} \quad\vert\quad \text{[Constants]} \quad\vert\quad \text{[Dependencies]} \quad\vert\quad \text{[Integration Points]}$$
* **Post-Change Verification**: Check all cross-references, missing parameters, imported packages, and caller signatures across both modules (`app` and `jButterFly`).
* **Basic Error Zero-Tolerance**: Unresolved references, missing parameters, and missing imports are strictly forbidden.

### 2. Dual-Module Parity (`app` ⟷ `jButterFly`)
* Every change made to files in `app/src/main/...` (Java, Kotlin, XML layouts, drawables, manifests) must have its exact twin synchronized in `jButterFly/src/main/...`.
* No feature or patch may be committed with module drift. Both modules must maintain 100% byte/line parity across all 14 twin pairs.

### 3. Non-Destructive Code Evolution (Deprecate or Merge)
* **Never delete earlier working code outright**: Either deprecate gracefully (`@Deprecated`) with fallback bridges or add/merge the new logic alongside existing call-sites.
* Preserve existing parameters and return structures via method overloads (`@JvmOverloads`) to avoid breaking existing callers.
* Follow dynamic OOPS-based MVC/MVVM standards: re-use existing utility methods and singleton engines via call-by-reference rather than duplicating code.

### 4. Hardware Root Anchor & SSOT
* The physical silicon serial or Android ID (`DeviceIdentityHelper.getRawAndroidId`, `ro.ril.oem.sno`, `ro.ril.oem.psno`) is the **only immutable Single Source of Truth (SSOT)** for device identity.
* Software UUIDs, unverified temporary tokens, or Firebase push keys must never overwrite physical identity anchors.
* On Android 10+ (API 29+), `Settings.Secure.ANDROID_ID` is used directly with thread-safe caching to avoid SELinux audit denials.

### 5. Master Temporal Query Invariant (Latest-Data-First SSOT)
* Every database read (SQLite and Firebase RTDB) across all screens, view models, and workers must order by descending timestamp:
  ```sql
  ORDER BY timestamp DESC -- or updated_at DESC, rowid DESC
  ```
* All 13 Zero-Loss Compatibility Views (`devices`, `safe_zones`, `geofence_events`, `sys_logs`, `security_evasion_audit_log`, `watchdog_resurrection_ledger`, `device_policy`, `device_pairings`, `alerts`, `MASTER_ROSTER`, `master_roster`, `organizations`, `sim_inventory`) must preserve explicit `ORDER BY ... DESC`.
* Forward-chronological order (`ORDER BY timestamp ASC`) is strictly reserved for linear trajectory playback, breadcrumb animation, or audit recovery flows.

### 6. Standardized Centralized RAG Status Engine
All status dots, labels, and marker badges must use these unified thresholds:
* 🟢 **ACTIVE (LIVE)**: Telemetry `< 15 min` (`#22C55E` Emerald Green).
* 🟡 **INACTIVE (< 6H)**: Telemetry `15m – 6h` OR Battery $\le 20\%$ (`#F59E0B` Amber Orange).
* 🔴 **INACTIVE (> 6H)**: Telemetry `> 6 hours` (`#EF4444` Rose Red).
* 🚨 **EMERGENCY SOS**: `isSos == true` (`#EF4444` Pulsing Crimson).
* ⚪ **UNLINKED / GHOST**: Deleted, unlinked, or Ghost Mode (`#64748B` Slate Gray).

### 7. File Size Ceiling (Ponytail 96% Rule — 1,920 Lines)
* Any executable file (ViewModel, Activity, Service, Controller) that reaches $\ge 1,920$ lines must automatically branch into modular OOPS sub-controllers or component models via call-by-reference.
* For brownfield files exceeding 1,920 lines (e.g., `server.js`), use the **Strangler Adapter Pattern**: extract target sub-routes without modifying legacy caller signatures.
* **Bounded Output Contract**: Automated test suites, audits, and telemetry tools must enforce bounded output schemas: summary metrics + maximum 10-record sample slices ($\le 100\text{ KB}$). Never dump unbounded payloads into repository files.

### 8. Strict Build Quarantine Protocol
* **NEVER execute Gradle builds (`./gradlew`, `./gradlew.bat installDebug`) autonomously.**
* Code generation, structural auditing, and refactoring are decoupled from build tasks until the user explicitly commands a compilation and deployment.

### 9. Deterministic Navigation (Zero Dead Ends)
* The Back Button (`onBackPressed` / `OnBackInvokedCallback`) must return deterministically to the parent interface without resetting or destroying parent state.

### 10. Coordinate Tree Protocol `(ROOT⊃BR⊃EL⊃FE⊃BE⊃DB)`
All multi-component features, device-to-device pairing handshakes, and role-scoped views must define and enforce the 6-layer coordinate tree:
- **ROOT**: Primary Subsystem Context (e.g. `jFindMe Core Engine`)
- **BR**: Business Rules & Reciprocal Role Inversion (e.g. Multi-Device Single Pairing Code Attachment, Scoped Isolation)
- **EL**: Event Listeners, Dialog Interceptors & State Triggers (e.g. Marker click, compound checkbox commit, RTDB socket listener)
- **FE**: Frontend UI/UX Components (e.g. UiArchitect Compound Dialog, Target Action Sheet, Role Pills, Slide Notification Banner)
- **BE**: Backend ViewModels, Repositories & Handlers (e.g. PairingRepository, LocationRepository, MapViewModel)
- **DB**: Dual Storage SSOT (SQLite Normalized Tables & Compatibility Views ↔ Firebase RTDB Mesh)

### 11. Target Resolution & Audit Fast-Exit Circuit Breaker (Cold-Context Guard)
* **Pre-Flight Scoping Gate**: When an audit, RAG report, or test scenario is requested without an explicit screen or module:
  1. **Check Warm Context:** If an active screen/file was evaluated or modified in the current or immediate prior turn ($\le 3$ turns), scope execution *strictly* to that anchor.
  2. **Check Git Delta:** If context is cold, run `git status -s` ($\le 1$ sec). If files were modified, scope the audit *exclusively* to those modified files.
  3. **Cold Baseline Fallback (Zero Speculative Crawling):** If no files are modified and no target screen is named in the prompt, **agents are STRICTLY FORBIDDEN from crawling the filesystem, scanning ASTs, or reading files across `app` and `jButterFly`**. Invert all untouched screens to `[N/A]` upfront. Either run the targeted test runner or immediately exit in 1 turn asking for the target screen/module ($\le 1$ tool call).
* **Strict Folder Exclusion**: Search tools must explicitly exclude `.gradle/`, `.idea/`, `build/`, `bin/`, `out/`, `node_modules/`, `.git/`.
* **Exception-Focused RAG Table**: Collapse GREEN (passing) into a single aggregate count line (`✅ N tests passed`). Detail ONLY RED (critical failures) and AMBER (degraded risks).

### 12. Red-Line Hard Gate: Monitoring Priority, Circle Scoping & Temporal Invariant Protection
* **STRICT PROHIBITION**: The following architectural invariants are locked:
  1. **Prime Objective Monitoring Hierarchy**: Child / Ward / Dependent (`4379A761` / Riddhi) guaranteed Top Position 0 sorting across Map, Flipper HUD, and Family & Friends.
  2. **Connected Device Scoping**: Unconditional inclusion of all active circle members, bidirectional pairings, and ward targets in `DatabaseHelper.getConnectedDeviceIds`.
  3. **Real-time Target Ingestion**: Cloud listener subscription loops in `LocationRepository.updateTargetListeners()`.
  4. **Master Temporal Invariant**: Descending chronological ordering (`ORDER BY timestamp DESC`).
* **MANDATORY CONFIRMATION GATE**: These rules MUST NOT be changed without explicit user permission. If any request or modification threatens to touch or alter them, it must be exclusively marked in **RED** and halted with the confirmation prompt:
  > **`Confirming - "Violating Prime Directive Rules"`**

### 13. The 4 Hard Gates (Enterprise Safety & Irreversibility)
* **Gate 1 (Minors Protection)**: Zero-tolerance for exposing telemetry or identity of minors outside the designated family circle.
* **Gate 2 (Irreversible Operations)**: Operations involving destructive file deletion, table drops, database overwrite, remote deployment, or cloud purge require exactly one confirmation prompt before execution.
* **Gate 3 (Live-Fire Target Guard)**: Immediate stop before executing live-fire network or telemetry transmissions against unverified production endpoints.
* **Gate 4 (CSAM Policy)**: Strict zero-tolerance lockdown.

### 14. Lifecycle Hardware & Software Pre-Flight Protocol (Install / Upgrade / Teardown)
All installation, in-place upgrade, and uninstallation workflows must comply strictly with the 7 Scenario-Based Human-In-The-Loop SOPs in `.agent/rules/lifecycle_rules.md`:
* **SOP-01 (Zero-State Onboarding)**: Hardware qualification $\to$ Silicon serial anchoring $\to$ 5-Stage permission ladder $\to$ OEM Auto-Start $\to$ SQLite seeding $\to$ Mesh enrollment.
* **SOP-02 (Silent OTA Upgrade)**: Battery $\ge 15\%$, storage $\ge 150\text{ MB}$, in-flight SHA-256, API 31+ `USER_ACTION_NOT_REQUIRED`, `ACTION_MY_PACKAGE_REPLACED` non-destructive SQLite PRAGMA migrate & service revive.
* **SOP-03 (HITL Settings Update)**: 1-Click check $\to$ Differential changelog card $\to$ "Install unknown apps" permission router $\to$ Glassmorphic bandwidth HUD $\to$ Installation handoff.
* **SOP-04 (Signature Mismatch Bridge)**: Pre-check intercepts key differences (Debug vs Release) $\to$ Automated 1-Click Vault export to `/Documents/jFindMe_Vault/` & Firebase $\to$ Guided uninstall/reinstall $\to$ Automatic Day-0 database restore.
* **SOP-05 (Rollback & Circuit Breaker)**: Instant cache eviction of corrupt `.apk` $\to$ 3-strike failure limit $\to$ 48h backoff & Firebase alert.
* **SOP-06 (Pre-Uninstall Teardown)**: Forensic table audit $\to$ Gate 2 JEET confirmation $\to$ Offline cloud telemetry flush $\to$ Forensic dump export $\to$ Sentinel termination $\to$ SQLite purge $\to$ Native OS `ACTION_DELETE`.
* **SOP-07 (OEM Battery Aggression)**: Telephony detection for Xiaomi HyperOS/MIUI, Samsung OneUI, Huawei EMUI $\to$ Deep-link auto-start routing $\to$ Sticky foreground notification with live GPS/Cellular badge.

---

## 📱 Tier 2: Screen-Dependent Subset Rules

If Tier 1 universal checks pass, identify the target screen/domain and apply its specific subset rules:

### Subset A: Live Tactical Map (`MapActivity`, `MultiMarkerEngine`, `activity_map.xml`)
1. **Lightweight Marker Standard**:
   - Map markers must only render: Top Name Callout Pill (Role border + RAG font) + Direct Avatar + Status Dot + Motion Heading Chevron.
   - Avoid heavy canvas glow layers and map-pin battery badges directly on the map viewport; surface detailed telemetry in bottom sheets or radar dialogs.
2. **Render Throttling**:
   - Marker recalculations and spiderfication updates must be throttled via MainLooper `Handler` ($\ge 250\text{ ms}$) to prevent UI frame drops during high-frequency location streams.
3. **Floating Distant Device Panel**:
   - Peer devices strictly $> 50\text{ m}$ away appear in the top-left floating panel (`@+id/floating_device_scroll` / `@+id/floating_device_panel`) as prominent circular Avatar Icons (zero floating text). Tapping an avatar smoothly pans camera and updates address marquee.
4. **PiP Isolation**:
   - PiP mode strips all overlays, floating panels, and action sheets, maintaining only the focused device marker and real-time breadcrumbs.

### Subset B: Device Sharing & Butterfly Point (`DeviceSharingHub`, `ButterflyPointHub`)
1. **Consolidated 4-in-1 Handshake**:
   - Device A initiates ➔ shares Device B ➔ to target Device C with mutual verification codes.
2. **Butterfly Point Protocol (`ACTION_GHOST`)**:
   - Ghost mode suppresses broadcasting while maintaining local encrypted trail in `butterfly_point` table.
3. **Timeout Watchdog**:
   - Pairing handshakes enforce a 15s timeout with `AngelLoader` progress feedback and fallback to offline QR/SMS beacon.

### Subset C: Family & Friends & Master Roster (`FamilyFriendsActivity`, `PersonDetailsActivity`)
1. **Master Roster SSOT**:
   - Multi-tier group listing (`MASTER_ROSTER`) sorted with Wards/Children at Position 0, followed by active guardians, then friends.
2. **Profile Parity**:
   - Profile edits (Avatar, Name, Role) immediately trigger `ACTION_PROFILE_SYNCED` broadcast across SQLite and Firebase RTDB.

### Subset D: Geofence & Safe Zones (`GeofenceActivity`, `GeofenceNotificationActivity`)
1. **Mathematical Containment**:
   - Ray-Casting algorithm for polygonal fences, Haversine for circular fences.
2. **Deduplication Engine**:
   - Prevent overlapping identical zones via `idx_safe_zones_dedupe`.
3. **Event Latching**:
   - State transition events (`ENTER` / `EXIT`) must persist state latch in `geofence_events` with timestamp descending order.

### Subset E: History & Trajectory Playback (`HistoryActivity`)
1. **Linear Trajectory Mode**:
   - The *only* legitimate exception to the descending query invariant: time-series replay uses `ORDER BY timestamp ASC`.
2. **Interpolation Smoothing**:
   - Filter noisy GPS spikes ($> 150\text{ km/h}$ delta without altitude acceleration) via 2D Kalman smoothing.

### Subset F: Settings, Themes & UI Config (`SettingsActivity`, `UiConfigActivity`, `UiGalleryActivity`)
1. **Dynamic 12-Theme Selection**:
   - Modulo calculation uses `ThemeManager.THEMES.length` (never hardcode 8).
   - Zero hardcoded colors; read from `theme.getSurfaceBackground()`, `theme.getGradientStart()`, `theme.getGradientEnd()`.
2. **Universal 5-Stage Scaling**:
   - Validate layouts against LIBERAL (25dp), STANDARD (20dp), COMPACT (15dp), SLIM (10dp), ULTRA_SLIM (5dp).

### Subset G: Device Health, Cellular Triangulation & Telemetry (`HealthActivity`)
1. **Baseband Cell Tower Sentinel**:
   - Collect and display cellular carrier vitals (MCC/MNC/LAC/CID) for dead-reckoning fallback.
2. **14-Condition Connectivity Audit**:
   - Screen tests for Doze restrictions, background data throttling, carrier APN blocks, and GPS accuracy.

### Subset H: Forensic Vault, Logs & Administrative Maintenance
1. **Encrypted SQLite Explorer (`ForensicVaultExplorerActivity`)**:
   - Read-only inspection of 27 normalized tables and 13 compatibility views.
2. **Rolling System Logs (`SystemLogActivity`)**:
   - Circular buffer capped at 5,000 entries; older rows trimmed automatically on SQLite transaction boundary.
3. **Silent In-Place Update & Pre-Uninstall Teardown (`SystemUpdateActivity`, `FactoryResetActivity`)**:
   - Strictly enforces the Tri-Phase Lifecycle Protocol (`.agent/rules/lifecycle_rules.md`).
   - Hardware pre-flight (Battery $\ge 15\%$, Storage $\ge 150\text{ MB}$, Silicon ID).
   - Software pre-flight (Keystore signature match, version code monotonicity, API 31+ unattended install).
   - Pre-uninstall clean teardown: stop all background services, flush offline queues to cloud, offer forensic SQLite dump, then launch OS `ACTION_DELETE` intent. Zero orphaned background processes.

### 15. Sovereign Web Workspace Anchor (`jFindMeWeb`)
* **MANDATORY SOVEREIGN WEB PATH**: All website, HTML, CSS, client-side JS, web dashboard, and web server development MUST strictly reference and operate within:
  `C:\Users\jiten\jAnitGravity\jFindMeWeb\`
* **Zero Web Files in Android Root**: Do NOT create standalone HTML web files inside `C:\Users\jiten\jAnitGravity\jFindMe\` root. All web presentation (`index.html`, `database-sync-settings.html`, `jButterflyGithubSync_v2.html`, `launcher.html`) and backend proxy routes (`js/server.js`, `js/static_server.js`) reside strictly in `jFindMeWeb`.
