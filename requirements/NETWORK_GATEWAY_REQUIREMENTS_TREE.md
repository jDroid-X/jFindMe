# Network Gateway & Device Pairing Ecosystem — Master Requirement Specification
## Coordinate Tree Architecture: ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB
### Enterprise L5 Lifecycle Standard (20-Year Senior Developer Quality)
> **System:** `jFindMe` / `jButterFly` Enterprise Ecosystem  
> **Codename:** `jDroid-X` | **Operator:** `JEET` | **Date:** 2026-11-24T00:10:00+05:30  
> **Pattern:** Dynamic OOPS-based MVC / Tri-Tier Master-Fact-Transaction Database Architecture  
> **Subsystem:** Unified Network Gateway, Symmetrical 2x2 Matrix, QR Matrix Scanner, Nearby P2P Offline Mesh, and Bilateral Pairing Protocol  

---

## 1. Executive Mission & System Purpose

The **Network Gateway & Device Pairing Subsystem** ([`NetworkGatewayActivity.java`](file:///c:/Users/jiten/jAnitGravity/jFindMe/jButterFly/src/main/java/com/jdroidx/jfindme/ui/group/NetworkGatewayActivity.java)) is the foundational mesh onboarding, trust-establishment, and mutual authentication gateway of the `jFindMe` platform. It governs how mobile hardware devices securely discover, authenticate, negotiate roles, and maintain bilateral encrypted telemetry parity across both cloud-connected and air-gapped/offline environments.

---

## 2. Master Coordinate Tree Architecture (ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB)

```
ROOT: APPLICATION MASTER PLAN
│
├── 💼 [BR]₁: Zero-Node Onboarding & Fleet Entry Router
│    ├── 🖱️ [EL]₁․₁: Cold-Start Fleet Roster State Observer
│    │    ├── 🖥️ [FE]₁․₁․₁: MapActivity Zero-Node Onboarding Card (`btnPair`, `btnFrame`)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₁․₁․₁․₁: When total active roster count in SQLite `device_pairings` == 0 (excluding self-device), render high-visibility "+ Add First Device" pulsating onboarding card above map canvas.
│    │    │    │    ├── FR₁․₁․₁․₂: Tapping "+ Add First Device" or header `btnPair` must dispatch explicit intent to `NetworkGatewayActivity` with `SELECT_TAB = TAB_REMOTE_CLOUD`.
│    │    │    │    └── FR₁․₁․₁․₃: If total active roster > 0, suppress onboarding card and render standard Floating Left Rail HUD.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass: Fresh install / empty DB launches MapActivity; "+ Add First Device" displays in < 150ms; click launches NetworkGatewayActivity.
│    │    │    │    └── Fail: Database has 2 paired devices; onboarding card incorrectly displayed over map markers.
│    │    │    └── ⚙️ [BE]₁․₁․₁․₁: Roster Presence & Fleet Sentinel (`MapActivity.checkZeroNodeState`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₁․₁․₁․₁․₁: Query SQLite `device_pairings` joined with `group_members` where `status = 'PAIRED'` and `is_deleted = 0`.
│    │    │         │    ├── BFR₁․₁․₁․₁․₂: Filter out self-device hardware ID (`cleanSelfUid` / `forensicAgentId`).
│    │    │         │    └── BFR₁․₁․₁․₁․₃: Cache boolean flag `isZeroNodeFleet` in ViewModel state to prevent database thrashing during pan/zoom events.
│    │    │         ├── Testing Scenarios:
│    │    │         │    ├── Pass: Return `true` when 0 non-self records exist; return `false` when >= 1 valid record exists.
│    │    │         │    └── Fail: Query throws SQLiteException on locked DB; fallback to SharedPreferences cached count.
│    │    │         ├── 🗄️ [Master DB [Offline]]₁․₁․₁․₁․₁: SQLite `device_pairings` (status = 'PAIRED')
│    │    │         ├── 🗄️ [Master DB [Online]]₁․₁․₁․₁․₂: Firebase RTDB `/relationships/{active_group}/devices`
│    │    │         ├── 🗄️ [Sub Master/Fact DB]₁․₁․₁․₁․₃: `group_members`
│    │    │         └── 🗄️ [Transaction DB]₁․₁․₁․₁․₄: `sys_log.onboarding_event`
│
├── 💼 [BR]₂: Symmetrical 2x2 Ephemeral Cloud PIN & Session TTL Gateway
│    ├── 🖱️ [EL]₂․₁: Cloud PIN Generation & TTL Selector Trigger
│    │    ├── 🖥️ [FE]₂․₁․₁: Host 2x2 Symmetrical Grid (`R1C1: Action Button`, `R1C2: QR Box`, `R2C1: Share/Copy`, `R2C2: Monospace Code`)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₂․₁․₁․₁: Clicking "⚡ GENERATE CLOUD PIN" triggers `SecurityDialogManager.showPinTtlSelectionDialog` offering 5m, 15m, 1h, 24h options.
│    │    │    │    ├── FR₂․₁․₁․₂: Upon TTL selection, display spinning loader (`pbCodeLoading`), disable button to prevent double-generation, and allocate 6-character uppercase alphanumeric PIN.
│    │    │    │    ├── FR₂․₁․₁․₃: Live reactive countdown timer ticks down in `tvSyncStatus`. When timer reaches 00:00, show non-intrusive alert prompting session renewal.
│    │    │    │    └── FR₂․₁․₁․₄: Share button formats clipboard/intent with both plaintext PIN and deep link: `jfindme://pair?code={PIN}`.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass: PIN generated in < 400ms, monospace text displays formatted `ABC 123`, countdown begins at selected TTL.
│    │    │    │    └── Fail: Network disconnected; display "NETWORK ALLOCATION TIMED OUT" in red without UI lockup.
│    │    │    └── ⚙️ [BE]₂․₁․₁․₁: Cloud Session Allocator & Expiry Daemon (`FirebasePairingManager.startHostPairingSession`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₂․₁․₁․₁․₁: Cryptographically generate 6-character entropy token excluding ambiguous characters (0, O, 1, I, L).
│    │    │         │    ├── BFR₂․₁․₁․₁․₂: Upsert `/pairing_requests/{inviteCode}` with `status = 'waiting'`, `host_id`, `host_name`, `expires_at = now + TTL_MS`.
│    │    │         │    ├── BFR₂․₁․₁․₁․₃: Attach real-time `ValueEventListener` to observe client join requests.
│    │    │         │    └── BFR₂․₁․₁․₁․₄: Automatically clean up expired sessions from Firebase when TTL expires.
│    │    │         ├── Testing Scenarios:
│    │    │         │    ├── Pass: RTDB node created with exact TTL timestamp; listener wakes on child arrival.
│    │    │         │    └── Fail: Firebase write permission denied; triggers graceful error callback to FE.
│    │    │         ├── 🗄️ [Master DB [Offline]]₂․₁․₁․₁․₁: SQLite `device_sharing_transactions`
│    │    │         ├── 🗄️ [Master DB [Online]]₂․₁․₁․₁․₂: Firebase RTDB `/pairing_requests/{inviteCode}`
│    │    │         ├── 🗄️ [Sub Master/Fact DB]₂․₁․₁․₁․₃: `system_registry.pairing_pin_ttl`
│    │    │         └── 🗄️ [Transaction DB]₂․₁․₁․₁․₄: `sys_log.cloud_pin_spinup`
│
├── 💼 [BR]₃: Universal ZXing Optical QR & Deep Link Handshake Engine
│    ├── 🖱️ [EL]₃․₁: Optical Camera Barcode Scanner & Intent Receiver
│    │    ├── 🖥️ [FE]₃․₁․₁: Scanner Launcher & Tactical Scan Overlay (`barcodeLauncher`, `handleDeepLinkIntent`)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₃․₁․₁․₁: Tapping "📷 SCAN QR CODE" launches ZXing scanner with camera permissions verification.
│    │    │    │    ├── FR₃․₁․₁․₂: Centralized `extractPairingCode(raw)` must parse custom schemes (`jfindme://pair?code=...`), web links (`https://.../pair/...`), query strings (`code=...`), or raw strings.
│    │    │    │    ├── FR₃․₁․₁․₃: If extracted token length == 6, auto-switch to `TAB_REMOTE_CLOUD` and prompt Role Selection Dialog.
│    │    │    │    ├── FR₃․₁․₁․₄: If extracted token length == 8, auto-switch to `TAB_NEARBY_P2P` and prompt Local Role Selection Dialog.
│    │    │    │    └── FR₃․₁․₁․₅: If extracted token is invalid, show `UiArchitect.showSlideNotificationBanner` warning without crashing.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass: Scanning `jfindme://pair?code=XYZ789` extracts `XYZ789` and opens Role Selection in < 100ms.
│    │    │    │    └── Fail: Scanning corrupt QR; displays format warning and leaves scanner ready.
│    │    │    └── ⚙️ [BE]₃․₁․₁․₁: Optical Matrix Encoder & Deep Link Dispatcher (`NetworkGatewayActivity.generateQrCode`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₃․₁․₁․₁․₁: MultiFormatWriter encodes 512x512 QR Bitmap on dedicated background thread.
│    │    │         │    ├── BFR₃․₁․₁․₁․₂: Sanitize code characters to uppercase alphanumeric (strip spaces, symbols, newlines).
│    │    │         │    └── BFR₃․₁․₁․₁․₃: Execute haptic vibration pulse (50ms) upon successful scan decode.
│    │    │         ├── Testing Scenarios:
│    │    │         │    ├── Pass: QR bitmap generated in < 80ms; displays in square proportional box.
│    │    │         │    └── Fail: Bitmap encoding error caught gracefully; placeholder remains visible.
│    │    │         ├── 🗄️ [Master DB [Offline]]₃․₁․₁․₁․₁: SharedPreferences `temp_qr_cache`
│    │    │         ├── 🗄️ [Master DB [Online]]₃․₁․₁․₁․₂: Firebase RTDB `/pairing_requests/{code}/qr_code`
│    │    │         ├── 🗄️ [Sub Master/Fact DB]₃․₁․₁․₁․₃: `user_identity.qr_code_ref`
│    │    │         └── 🗄️ [Transaction DB]₃․₁․₁․₁․₄: `sys_log.qr_scan_event`
│
├── 💼 [BR]₄: Nearby P2P Zero-Internet Radio Mesh Gateway
│    ├── 🖱️ [EL]₄․₁: Wi-Fi Direct / Nearby Radio Discovery Trigger
│    │    ├── 🖥️ [FE]₄․₁․₁: Local Option Panel (`TAB_NEARBY_P2P = 2`)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₄․₁․₁․₁: Clicking "BROADCAST LOCAL TOKEN" generates 8-character hardware silicon token based on local device hardware signature.
│    │    │    │    ├── FR₄․₁․₁․₂: Display 4-digit Short Authentication String (SAS) toast upon P2P connection to prevent Man-in-the-Middle attacks.
│    │    │    │    └── FR₄․₁․₁․₃: Air-gapped offline pairing must complete successfully with 0 internet connection.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass: Airplane mode with Wi-Fi/Bluetooth enabled pairs two phones in < 3s.
│    │    │    │    └── Fail: Nearby service unbinds; prompts user to toggle Wi-Fi / Location service.
│    │    │    └── ⚙️ [BE]₄․₁․₁․₁: Air-Gapped Mesh Binding Engine (`LocalPairingManager.finalizeLocalMergeWithMetadata`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₄․₁․₁․₁․₁: **Offline-First Persistence:** Immediately write bilateral pairing to SQLite `device_pairings`, `group_members`, and `user_profile` BEFORE attempting any cloud write.
│    │    │         │    ├── BFR₄․₁․₁․₁․₂: Exchange JSON payload (`CHILD_JOIN` and `HOST_META`) over Google Nearby Connections radio payload.
│    │    │         │    ├── BFR₄․₁․₁․₁․₃: Broadcast `ACTION_ROSTER_REFRESH` immediately upon SQLite commit.
│    │    │         │    └── BFR₄․₁․₁․₁․₄: Execute opportunistic background sync to Firebase RTDB if/when internet connectivity resumes.
│    │    │         ├── Testing Scenarios:
│    │    │         │    ├── Pass: SQLite records inserted immediately; onMergeComplete(true) fires even when offline.
│    │    │         │    └── Fail: SQLite disk full; returns false with error logging.
│    │    │         ├── 🗄️ [Master DB [Offline]]₄․₁․₁․₁․₁: SQLite `device_pairings` (pair_status = 'P2P PAIRED')
│    │    │         ├── 🗄️ [Master DB [Online]]₄․₁․₁․₁․₂: Firebase RTDB `/devices/{id}/relationships` (opportunistic)
│    │    │         ├── 🗄️ [Sub Master/Fact DB]₄․₁․₁․₁․₃: `group_members` (role = localRole)
│    │    │         └── 🗄️ [Transaction DB]₄․₁․₁․₁․₄: `sys_log.p2p_pairing_audit`
│
├── 💼 [BR]₅: Bilateral Role Negotiation & GDPR Consent Handshake Gate
│    ├── 🖱️ [EL]₅․₁: Client Role Submission & Host Approval Interceptor
│    │    ├── 🖥️ [FE]₅․₁․₁: Role Selection & GDPR Article 6 Disclosure Modal (`showRoleSelectionDialogBeforeJoin`, `promptGdprConsentAndJoin`)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₅․₁․₁․₁: Client selects candidate role: Father, Mother, Guardian, Spouse, Child, Friend.
│    │    │    │    ├── FR₅․₁․₁․₂: GDPR consent disclosure lists all 4 legal clauses (continuous GPS, battery telemetry, 30-day retention, one-click right to erasure).
│    │    │    │    ├── FR₅․₁․₁․₃: Declining GDPR disclosure aborts handshake cleanly with 0 data transmitted.
│    │    │    │    ├── FR₅․₁․₁․₄: Host is alerted with `renderHostApprovalDialog`. If node was previously paired, display "🚨 Node Already Paired" re-authorization prompt.
│    │    │    │    └── FR₅․₁․₁․₅: Host approves with reciprocal role (Father $\leftrightarrow$ Child; Guardian $\leftrightarrow$ Ward; Friend $\leftrightarrow$ Friend).
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass: Full handshake completes; both host and child update roles and route to MapActivity in < 2s.
│    │    │    │    └── Fail: Host clicks "REJECT REQUEST"; child receives rejection toast and aborts.
│    │    │    └── ⚙️ [BE]₅․₁․₁․₁: Bilateral ACL & Role Inversion Normalizer (`FirebasePairingManager.approveHandshake`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₅․₁․₁․₁․₁: Apply strict role inversion: Child role automatically matches Host Guardian/Parent role.
│    │    │         │    ├── BFR₅․₁․₁․₁․₂: Atomic multi-path Firebase update: update `/relationships`, `/devices`, `/pairing_requests`, and release handshake lock.
│    │    │         │    ├── BFR₅․₁․₁․₁․₃: SQLite SSOT commit: `db.saveRosterNode` + `db.logDevicePairing` + `db.updateCitizenRole`.
│    │    │         │    └── BFR₅․₁․₁․₁․₄: Dispatch `ACTION_ROSTER_REFRESH` local broadcast.
│    │    │         ├── Testing Scenarios:
│    │    │         │    ├── Pass: Atomic write succeeds; both devices establish bilateral trust rows.
│    │    │         │    └── Fail: Network dropped during multi-path update; rollback transaction and retry.
│    │    │         ├── 🗄️ [Master DB [Offline]]₅․₁․₁․₁․₁: SQLite `device_pairings` & `user_profile`
│    │    │         ├── 🗄️ [Master DB [Online]]₅․₁․₁․₁․₂: Firebase RTDB `/relationships/{group}/devices`
│    │    │         ├── 🗄️ [Sub Master/Fact DB]₅․₁․₁․₁․₃: `group_members` (is_admin, is_friend)
│    │    │         └── 🗄️ [Transaction DB]₅․₁․₁․₁․₄: `sys_log.handshake_approval`
│
├── 💼 [BR]₆: Concurrent 1-Code Multi-Device Fleet Attachment
│    ├── 🖱️ [EL]₆․₁: Secondary Device Join Request under Existing Session
│    │    ├── 🖥️ [FE]₆․₁․₁: Multi-Device Close Attachment Receiver (`executeChildJoinRequest`)
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₆․₁․₁․₁: A single 6-digit PIN must allow up to 5 family devices to join without invalidating the PIN after the first device joins.
│    │    │    │    ├── FR₆․₁․₁․₂: Secondary devices joining via `PairingRepository.joinMultiDevicePairingSession` attach to `/pairing_sessions/{code}/attached_devices`.
│    │    │    │    └── FR₆․₁․₁․₃: Joining devices save `active_group` into local `SharedPreferences` and SQLite roster automatically.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass: 3 devices sequentially enter the same PIN; all 3 successfully attach and appear on Host map.
│    │    │    │    └── Fail: 6th device attempts to join; rejected with `ERR_FLEET_LIMIT_EXCEEDED` (max 5).
│    │    │    └── ⚙️ [BE]₆․₁․₁․₁: Multi-Device Session Dispatcher (`PairingRepository.linkBatchDevicesWithRoles`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₆․₁․₁․₁․₁: Maintain list of `attached_devices` under `/pairing_sessions/{code}`.
│    │    │         │    ├── BFR₆․₁․₁․₁․₂: Propagate shared encryption token and circle group ID to all attached peers.
│    │    │         │    └── BFR₆․₁․₁․₁․₃: Auto-create bilateral pairing rows in SQLite between all participating devices in circle.
│    │    │         ├── Testing Scenarios:
│    │    │         │    ├── Pass: Batch write creates NxN mutual pairings across fleet.
│    │    │         │    └── Fail: Partial failure; rollback uncommitted pairings.
│    │    │         ├── 🗄️ [Master DB [Offline]]₆․₁․₁․₁․₁: SQLite `device_pairings` (batch inserted)
│    │    │         ├── 🗄️ [Master DB [Online]]₆․₁․₁․₁․₂: Firebase RTDB `/pairing_sessions/{code}/attached_devices`
│    │    │         ├── 🗄️ [Sub Master/Fact DB]₆․₁․₁․₁․₃: `group_members`
│    │    │         └── 🗄️ [Transaction DB]₆․₁․₁․₁․₄: `sys_log.batch_pairing_audit`
│
├── 💼 [BR]₇: Two-Stage Safe Unlink, Park & Purge Lifecycle
│    ├── 🖱️ [EL]₇․₁: Member Unlink Action Dispatcher (`promptPurgeMemberDialog`)
│    │    ├── 🖥️ [FE]₇․₁․₁: Unlink Method Selector Dialog ("🔘 PARK & NOTIFY" vs "🗑️ IMMEDIATE PURGE")
│    │    │    ├── Validation & Conditional Rules:
│    │    │    │    ├── FR₇․₁․₁․₁: "🔘 PARK & NOTIFY" sets device in Grey state (`st_session_state = 'PARKED_FOR_UNLINK'`) and sends push alert.
│    │    │    │    ├── FR₇․₁․₁․₂: Parked device can be restored ("↩️ RESTORE ACTIVE") or permanently purged ("✅ FINALIZE UNLINK").
│    │    │    │    ├── FR₇․₁․₁․₃: "🗑️ IMMEDIATE PURGE" deletes records immediately across SQLite and Firebase without grace period.
│    │    │    │    └── FR₇․₁․₁․₄: In all purge/unlink events, broadcast `ACTION_ROSTER_REFRESH` to update Map and Family screens.
│    │    │    ├── Testing Scenarios:
│    │    │    │    ├── Pass: Parking device dims card to grey; MapActivity suppresses alerts; unlinking purges records.
│    │    │    │    └── Fail: Database locked; handles gracefully with retry toast.
│    │    │    └── ⚙️ [BE]₇․₁․₁․₁: Cascading Purge Controller (`GroupViewModel.deleteMember`)
│    │    │         ├── Logic & Business Rules:
│    │    │         │    ├── BFR₇․₁․₁․₁․₁: Cascading delete across SQLite: `device_pairings`, `group_members`, `user_profile`, `device_vitals`.
│    │    │         │    ├── BFR₇․₁․₁․₁․₂: Remove peer from Firebase RTDB `/relationships/{group}/devices/{uid}` and `/direct_pairs`.
│    │    │         │    ├── BFR₇․₁․₁․₁․₃: Teardown active `ValueEventListener` in `liveDeviceListeners`.
│    │    │         │    └── BFR₇․₁․₁․₁․₄: Log transaction in SQLite `device_sharing_transactions` with status = 'REVOKED'.
│    │    │         ├── Testing Scenarios:
│    │    │         │    ├── Pass: All 4 tables cleaned; zero orphan records remaining.
│    │    │         │    └── Fail: Partial delete; transaction rolled back.
│    │    │         ├── 🗄️ [Master DB [Offline]]₇․₁․₁․₁․₁: SQLite `device_pairings` (DELETE CASCADE)
│    │    │         ├── 🗄️ [Master DB [Online]]₇․₁․₁․₁․₂: Firebase RTDB `/relationships/{group}/devices/{uid}` (null)
│    │    │         ├── 🗄️ [Sub Master/Fact DB]₇․₁․₁․₁․₃: `group_members` (deleted)
│    │    │         └── 🗄️ [Transaction DB]₇․₁․₁․₁․₄: `device_sharing_transactions` (status = 'REVOKED')
│
└── 💼 [BR]₈: Cloud Fleet Recovery & Historical Group Reclamation
     ├── 🖱️ [EL]₈․₁: Restore Old Group Trigger (`btnRestoreGroup`)
     │    ├── 🖥️ [FE]₈․₁․₁: Multi-Select Circle Recovery Dialog (`showRestoreOldGroupDialog`, `displayRestoreDialog`)
     │    │    ├── Validation & Conditional Rules:
     │    │    │    ├── FR₈․₁․₁․₁: Query `users/{uid}/joined_circles` and `/relationships` to detect prior circle memberships.
     │    │    │    ├── FR₈․₁․₁․₂: Present checkboxes for all discovered historical nodes with resolved names, roles, and avatar icons.
     │    │    │    ├── FR₈․₁․₁․₃: Clicking "RESTORE SELECTED" reclaims chosen members into current active circle.
     │    │    │    └── FR₈․₁․₁․₄: Restored nodes must be written to both `user_profile` AND `device_pairings` table with status = 'PAIRED'.
     │    │    ├── Testing Scenarios:
     │    │    │    ├── Pass: 3 lost nodes restored; all 3 appear immediately on Map and Roster.
     │    │    │    └── Fail: 0 historical circles found; displays informative dialog.
     │    │    └── ⚙️ [BE]₈․₁․₁․₁: Fleet Re-Sync & Ingress Bridge (`NetworkGatewayActivity.displayRestoreDialog`)
     │    │         ├── Logic & Business Rules:
     │    │         │    ├── BFR₈․₁․₁․₁․₁: Re-anchor Self as ADMIN/Guardian in restored cloud circle.
     │    │         │    ├── BFR₈․₁․₁․₁․₂: Upsert member identities into `/relationships/{group}/devices/{uid}`.
     │    │         │    ├── BFR₈․₁․₁․₁․₃: Insert bilateral pairing links in local SQLite `device_pairings` via `db.logDevicePairing`.
     │    │         │    └── BFR₈․₁․₁․₁․₄: Broadcast `ACTION_ROSTER_REFRESH` to update all active UI surfaces.
     │    │         ├── Testing Scenarios:
     │    │         │    ├── Pass: SQLite and Cloud fully reconciled; zero orphan profiles.
     │    │         │    └── Fail: Network dropped during multi-node restore; local records saved, cloud sync deferred.
     │    │         ├── 🗄️ [Master DB [Offline]]₈․₁․₁․₁․₁: SQLite `device_pairings` & `user_profile`
     │    │         ├── 🗄️ [Master DB [Online]]₈․₁․₁․₁․₂: Firebase RTDB `/relationships/{group}/devices`
     │    │         ├── 🗄️ [Sub Master/Fact DB]₈․₁․₁․₁․₃: `users/{uid}/joined_circles`
     │    │         └── 🗄️ [Transaction DB]₈․₁․₁․₁․₄: `sys_log.circle_restoration`
```

---

## 3. Comprehensive RCA & REA Across All Gateway Items

### Root Cause Analysis (RCA) & Reverse Engineering Analysis (REA):

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        COMPREHENSIVE RCA & REA MATRIX                                  │
├─────┬─────────────────────────────────┬────────────────────────────────────────────────┤
│ #   │ Failure Mode / Conflict Vector  │ Root Cause & Reverse-Engineered Mechanism      │
├─────┼─────────────────────────────────┼────────────────────────────────────────────────┤
│ C1  │ Cloud PIN vs Nearby P2P Session │ Active code collision when operator switches   │
│     │ State Race                      │ between Cloud and P2P tabs during spinup.      │
├─────┼─────────────────────────────────┼────────────────────────────────────────────────┤
│ C2  │ Role Inversion Asymmetry &      │ Child requests Guardian; naive assignment      │
│     │ Privilege Escalation            │ grants Admin without reciprocal child role.    │
├─────┼─────────────────────────────────┼────────────────────────────────────────────────┤
│ C3  │ Split-Brain Telemetry in        │ Local SQLite pairing created under local group │
│     │ Offline P2P Pairing             │ while Cloud expects different group ID.        │
├─────┼─────────────────────────────────┼────────────────────────────────────────────────┤
│ C4  │ Multi-Device Attachment Lock    │ Single-device handshake changes status from    │
│     │ Contention                      │ "waiting" to "pending", rejecting device #2-5. │
├─────┼─────────────────────────────────┼────────────────────────────────────────────────┤
│ C5  │ Parked Ghost Telemetry Leak     │ Parked device continues background GPS stream, │
│     │                                 │ rendering active markers on live map.          │
├─────┼─────────────────────────────────┼────────────────────────────────────────────────┤
│ C6  │ Re-Entrant Scanner Overlay      │ Camera scanning QR while prior dialog is open  │
│     │ Window Leak                     │ causes overlapping dialogs & BadTokenException.│
├─────┼─────────────────────────────────┼────────────────────────────────────────────────┤
│ C7  │ Incomplete Fleet Restoration    │ Restoring nodes populated `user_profile` but   │
│     │                                 │ omitted `device_pairings` bilateral rows.      │
└─────┴─────────────────────────────────┴────────────────────────────────────────────────┘
```

---

## 4. Conflict Mitigation Architecture: Sequential vs Parallel Pipelines

To achieve **maximum execution speed without race conditions or state corruption**, we divide operations into strict **Sequential Gates** (must run in order) and **Parallel Streams** (run concurrently):

### Pipeline Execution Architecture:

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                       CHRONOLOGICAL EXECUTION PIPELINE                         │
├────────────────────────────────────────────────────────────────────────────────┤
│ [STAGE 1: PRE-FLIGHT VALIDATION (SEQUENTIAL)]                                  │
│ 1. Verify Camera / Location Permissions                                        │
│ 2. Dismiss any existing active Dialogs (SecurityDialogManager)                 │
│ 3. Sanitize and Extract Pairing Code (extractPairingCode)                      │
│                                                                                │
│ [STAGE 2: PROTOCOL ROUTING GATE (SEQUENTIAL)]                                  │
│ ├── If 6-chars -> ROUTE TO CLOUD CHANNEL                                       │
│ └── If 8-chars -> ROUTE TO NEARBY P2P CHANNEL                                  │
│                                                                                │
│ [STAGE 3: PARALLEL DISPATCH (CONCURRENT ASYNC)]                                │
│ ├── Stream A: UI Haptic Pulse + Toast Notification                             │
│ ├── Stream B: Load Local Peer Dossier from SQLite Cache                        │
│ └── Stream C: Verify GDPR Consent State in SharedPreferences                   │
│                                                                                │
│ [STAGE 4: HUMAN-IN-THE-LOOP ROLE NEGOTIATION (SEQUENTIAL GATED)]               │
│ 1. Render Role Dialog -> User selects candidate role                           │
│ 2. Render GDPR Article 6 Disclosure -> User clicks AGREE                       │
│ 3. Transmit Handshake Packet to Host                                           │
│ 4. Host prompts with Reciprocal Role Inversion Dialog                          │
│                                                                                │
│ [STAGE 5: ATOMIC COMMIT & PARALLEL FAN-OUT (CONCURRENT)]                       │
│ ├── Stream 1 (MANDATORY SSOT): Write to SQLite device_pairings & user_profile   │
│ ├── Stream 2 (CLOUD MIRROR): updateChildren() on Firebase RTDB                 │
│ ├── Stream 3 (CROSS-SCREEN EVENT): sendBroadcast(ACTION_ROSTER_REFRESH)        │
│ └── Stream 4 (NAVIGATION): Transition to MapActivity and Fly-To Peer           │
└────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Scenario-Based Human-in-the-Loop SOP (Standard Operating Procedures)

### SOP-GW-01: Zero-Node First Device Pairing (Cold Start)
1. **Pre-condition:** App launched for the first time, 0 paired devices.
2. **User Action:** User taps "+ Add First Device" card on MapActivity.
3. **Gateway Action:** Opens NetworkGatewayActivity on `TAB_REMOTE_CLOUD`.
4. **Host SOP:**
   - Tap "⚡ GENERATE CLOUD PIN".
   - Select TTL (e.g., 15 Minutes).
   - Show QR Code to Partner/Child or tap "SHARE" to send WhatsApp/SMS link.
5. **Client SOP:**
   - Client taps link `jfindme://pair?code=ABC123` or taps "📷 SCAN QR CODE".
   - Select Role (e.g. "Child").
   - Review and Agree to GDPR Privacy Disclosure.
6. **Host Approval SOP:**
   - Host receives pop-up: "Target (Child Phone) requested role: Child".
   - Host selects reciprocal role: "👑 ACCEPT AS: FATHER".
7. **Resolution:** Both devices display AngelLoader for < 600ms, receive success toast, and land on MapActivity with bilateral location tracking active.

### SOP-GW-02: Offline Deep-Woods Pairing (Zero Cellular/Internet)
1. **Pre-condition:** Both devices have No Service (0 bars / airplane mode), but Bluetooth and Wi-Fi antennas are enabled.
2. **Host SOP:**
   - Switch to `NEARBY P2P` tab.
   - Tap "BROADCAST LOCAL TOKEN".
   - Display local 8-digit hardware QR code.
3. **Client SOP:**
   - Switch to `NEARBY P2P` tab.
   - Tap "📷 SCAN QR CODE" and scan Host's offline QR code.
   - Select role (e.g. "Friend").
4. **Verification:**
   - Both screens display matching 4-digit SAS security code (e.g. `7842`).
   - `LocalPairingManager` executes offline-first SQLite write immediately.
   - Both devices see each other in Active Roster without contacting Firebase.

### SOP-GW-03: Two-Stage Member Unlinking & Audit
1. **Host SOP:**
   - Navigate to Active Roster tab in Network Gateway.
   - Locate member card and tap "🗑️ UNLINK".
   - Choose:
     - **Option A ("🔘 PARK & NOTIFY"):** Soft park for unlinking (card turns grey, notification sent to member).
     - **Option B ("🗑️ IMMEDIATE PURGE"):** Hard delete across all 4 SQLite tables and Firebase nodes.
2. **Audit Verification:**
   - Tapping "📋 HISTORY" on header bar opens `DeviceSharingHistoryDialog` displaying transaction status (`REVOKED`), exact timestamp, and audit trail.

---

## 6. Code Deduplication & Strangler Migration Audit

To prevent code bloat and maintain 20-Year Senior Developer quality:
1. **QR & Code Parsing SSOT:** Replaced separate parsing logic in `barcodeLauncher` and `handleDeepLinkIntent` with a single canonical helper: `extractPairingCode(String raw)`.
2. **Role Inversion SSOT:** Replaced ad-hoc string comparisons with `PairingRepository.normalizeRoles(requestedRole, hostRole)`.
3. **Local P2P Convergence:** Eliminated redundant duplicate `saveRosterNode` code blocks in `LocalPairingManager` by routing through `finalizeLocalMergeWithMetadata`.
4. **Zero-Broken Contract:** All existing public/protected method signatures in `NetworkGatewayActivity` and `PairingRepository` remain backward-compatible.
