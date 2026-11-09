# jFindMe Enterprise Rules & Standards (The Constitution V3.0)

> **Mandatory Reference Order**: `Workflow (.agent/rules/workflow.md)` ➔ `Rules (.agent/rules/rules.md)` ➔ `Agents (.agent/rules/agents.md)` ➔ `Skills (.agent/rules/skills.md)`

---

## Tier 1: Universal Rules (Apply to ALL Changes)

Every modification, update, bug fix, or new feature—regardless of target file or screen—must pass these universal gates first:

### 1. Pre-Change Declaration & Post-Change Verification
Before modifying any line of code, first declare:
$$\text{[Variables]} \quad\vert\quad \text{[Constants]} \quad\vert\quad \text{[Dependencies]} \quad\vert\quad \text{[Integration Points]}$$
* **Post-Change Verification**: Check all cross-references, missing parameters, imported packages, and caller signatures across both modules (`app` and `jButterFly`).
* **Basic Error Zero-Tolerance**: Unresolved references, missing parameters, and missing imports are strictly forbidden.

### 2. Dual-Module Parity (`app` ⟷ `jButterFly`)
* Every change made to files in `app/src/main/...` (Java, Kotlin, XML layouts, drawables, manifests) must have its exact twin synchronized in `jButterFly/src/main/...`.
* No feature or patch may be committed with module drift. Both modules must maintain 100% byte/line parity.

### 3. Non-Destructive Code Evolution (Deprecate or Merge)
* **Never delete earlier working code outright**: Either deprecate gracefully (`@Deprecated`) with fallback bridges or add/merge the new logic alongside existing call-sites.
* Preserve existing parameters and return structures via method overloads (`@JvmOverloads`) to avoid breaking existing callers.
* Follow dynamic OOPS-based MVC/MVVM standards: re-use existing utility methods and singleton engines via call-by-reference rather than duplicating code.

### 4. Hardware Root Anchor & SSOT
* The physical silicon serial or Android ID (`DeviceIdentityHelper.getRawAndroidId`) is the **only immutable Single Source of Truth (SSOT)** for device identity.
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

---

## Tier 2: Screen-Dependent Subset Rules

If Tier 1 universal checks pass, identify the target screen/domain and apply its specific subset rules:

### Subset A: Live Tactical Map (`MapActivity`, `MultiMarkerEngine`, `activity_map.xml`)
1. **Lightweight Marker Standard**:
   * Map markers must only render: Top Name Callout Pill (Role border + RAG font) + Direct Avatar + Status Dot + Motion Heading Chevron.
   * Avoid heavy canvas glow layers and map-pin battery badges directly on the map viewport; surface detailed telemetry in bottom sheets or radar dialogs.
2. **Render Throttling**:
   * Marker recalculations and spiderfication updates must be throttled via MainLooper `Handler` ($\ge 250\text{ ms}$) to prevent UI frame drops during high-frequency location streams.
3. **Floating Distant Device Panel**:
   * Peer devices strictly $> 50\text{ m}$ away appear in the top-left floating carousel (`@+id/floating_device_scroll` / `@+id/floating_device_panel`). Clicking a card smoothly pans camera and updates address marquee.
4. **PiP Isolation**:
   * In Picture-in-Picture mode (`onPictureInPictureModeChanged`), hide all headers, marquees, floating side panels, and dashboards (`View.GONE`), displaying only the map and the circular close CTA (`btn_pip_close`). Restore conditionally on exit.
5. **ViewBinding Parity**:
   * Every ID referenced in `MapActivity.java` via `binding.<id>` must be declared in both `app/src/main/res/layout/activity_map.xml` and `jButterFly/src/main/res/layout/activity_map.xml`.

### Subset B: User Profile & Identity Hub (`ProfileActivity`, `ProfileViewModel`, `AuthViewModel`)
1. **Tri-Tier Profile Rehydration**:
   * **Tier 1**: Fetch local SQLite forensic dossier (`getLocalDossier`).
   * **Tier 2**: Fall back to `SharedPreferences` (`system_registry`) and hardware identity.
   * **Tier 3**: Deep cloud fallback from `/devices/{id}/profile`.
2. **Offline-Online Preference Closure**:
   * Any save operation (`verifyAndSaveProfile`) must atomically write all fields (`name`, `phone`, `dob`, `gender`, `address`, `city`, `state`, `country`, `pincode`, `org_name`, `dept_name`) to `SharedPreferences`, local SQLite (`executeTieredHandshake`), and Firebase (`syncForensicHeartbeat`).
3. **Mandatory Field Indicators**:
   * All mandatory fields must render with a red asterisk (`*`) in both Label and Hint using `Html.fromHtml`.
4. **Slide-Out Right Drawer for Sub-Settings**:
   * Edits and supplementary profile dialogs must slide in from the right edge with a 2-column compact layout and smooth vertical scrolling.

### Subset C: Circles, Family & Friends Roster (`FamilyFriendsActivity`, `FamilyFriendsViewModel`)
1. **Type-Safe Cloud Deserialization**:
   * All Firebase snapshot listeners for telemetry/heartbeats must use `safeLong()` and `safeInt()` helpers. Never call raw `snap.getValue(Long::class.java)` directly to prevent String-to-Number conversion crashes.
2. **Topology Scoping & RBAC**:
   * Enforce the 5-Phase Pairing Matrix: Members (Friends, Dependents) must only see nodes authorized by their parent/host circle. Never cross-expose unlinked peers.
3. **Avatar Cache Isolation**:
   * Remote avatars must be keyed per device (`REMOTE_HQ_{deviceId}_{avatarId}_...`) to prevent avatar bleeding between different members sharing identical avatar index numbers.

### Subset D: Co-Parent Linking & Pairing Gateway (`PairingRepository`, `NetworkGatewayActivity`)
1. **Co-Parent Link for Dependent Children (MANDATORY)**:
   * When a parent assigns a member the role `🧸 Dependent Child / Ward` (or inspects a child node in quick actions), the app MUST present the option:
     `👨‍👩‍👧 Link Other Parent / Co-Guardian` ("Assign a second parent/guardian, such as spouse, to co-manage this child").
   * Linking must use the exact same Child ID across both parents:
     - Writes reciprocal relationship under `/devices/{coParentId}/relationships/{childId}` (`role: Child`) and `/devices/{childId}/relationships/{coParentId}` (`role: GUARDIAN`).
     - Appends `childId` to the co-parent's `/devices/{coParentId}/identity/valid_linked_agents` CSV list.
     - Logs reciprocal pairing in local SQLite `device_pairing` (`DevicePairingTable.logPairing`).
     - Signals roster refresh via `/relationships/{groupId}/signals/refresh_roster`.
2. **3-Way Cryptographic Pairing**:
   * QR payload must contain `{agent_id, active_group, public_key, timestamp}` and commit atomically to `/relationships/{group_id}/devices/{member_id}`.
3. **Self-Healing Column Verification**:
   * Queries accessing `group_members` or `device_pairings` must be wrapped in self-healing column checks (`PRAGMA table_info`) before query execution.

### Subset E: SafeZones & Geofencing Engine (`SafeZoneActivity`, `GeofenceHelper`)
1. **Low-Latency Spatial Math**:
   * Polygon boundary detection must use ray-casting algorithms and Haversine distance without blocking the main UI thread.
2. **SafeZone Deduplication**:
   * SafeZone creation must use atomic `INSERT OR REPLACE` with unique index constraints (`idx_safe_zones_dedupe`) on `zone_id`.
3. **Dual Notification Dispatch**:
   * Breach/dwell events must dispatch both a local notification channel alert and a cloud alert node entry (`/alerts/{group_id}`).

### Subset F: Historical Trajectory & Forensic Vault (`HistoryActivity`, `ForensicVaultExplorerActivity`)
1. **Chronological Exception**:
   * Trajectory playback and breadcrumb animations are the **only** routines permitted to use `ORDER BY timestamp ASC`.
2. **Bounded Sample Slicing**:
   * Vault data exports and table audits must enforce bounded output schemas: summary metrics + maximum 10-record sample slices ($\le 100\text{ KB}$).

### Subset G: Background Sentinel, Telemetry & Watchdog (`TrackingService`, `BackgroundJobManager`)
1. **Adaptive Telemetry Polling**:
   * High-frequency updates ($15\text{s} - 30\text{s}$) during movement or `SOS`.
   * Throttled heartbeat ($5\text{m} - 15\text{m}$) when stationary or screen is off.
2. **Silent Failure & Doze Recovery**:
   * Background coroutine scopes must be wrapped in try-catch error boundaries.
   * Watchdog self-resurrection ledger must log restart events without crashing the host process.

### Subset H: Database & Cloud Synchronization (`DatabaseHelper`, `FirebaseComm`, `AvatarSyncEngine`)
1. **Selective Field Heartbeat Patching**:
   * Heartbeat payloads sent to Firebase must only patch non-empty fields to prevent erasing cloud dossiers during partial background syncs.
2. **Physical Table vs. View Integrity**:
   * Before dropping or replacing any view, verify via `SELECT type FROM sqlite_master` that the target is a `view` and **never a physical table**.
3. **Atomic Handshake**:
   * Local SQLite tables (`MASTER_ROSTER`, `SAFE_ZONES`, `TRACKING_HISTORY`, `DEVICE_METADATA`) must remain synchronized with Firebase Cloud SSOT (`/devices/{id}/profile`, `/locations/{id}`).
