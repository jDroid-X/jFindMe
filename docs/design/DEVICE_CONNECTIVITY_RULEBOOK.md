# 📖 Device Connectivity & Topology Rule Book
## Multi-Device Pairing, Category Access & SafeZone Rule Matrix

---

## 1. Step-by-Step Device Lifecycle & Connection Pipeline

Every device added to the network follows a mandatory 5-phase onboarding and pairing protocol:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        5-PHASE DEVICE ONBOARDING WORKFLOW                              │
└────────────────────────────────────────────────────────────────────────────────────────┘

 ┌─────────────────────────┐     ┌─────────────────────────┐     ┌─────────────────────────┐
 │ Phase 1: Registration   │ ──► │ Phase 2: Pairing        │ ──► │ Phase 3: Category Role  │
 │ • Hardware Anchor ID    │     │ • QR Handshake          │     │ • Family (Parent/Child) │
 │ • Nickname & Avatar     │     │ • 3-Way Cryptographic   │     │ • Friend / Colleague    │
 └─────────────────────────┘     └─────────────────────────┘     └─────────────────────────┘
                                                                              │
                                                                              ▼
 ┌─────────────────────────┐     ┌─────────────────────────┐     ┌─────────────────────────┐
 │ Phase 5: Live Tracking  │ ◄── │ Phase 4: SafeZone Gate  │ ◄───┘                         │
 │ • Scoped Marker Render  │     │ • Geofence Assignment   │                               │
 │ • Closed-Loop Telemetry │     │ • Dwell & Breach Alarms │                               │
 └─────────────────────────┘     └─────────────────────────┘
```

### Phase 1: Device Registration & Hardware Identity Anchor
1. System reads the physical silicon key or generates canonical 8-character ID (e.g. `2T929153`).
2. Registers `/devices/{id}/identity` and `/devices/{id}/profile` in Firebase Cloud SSOT.
3. Initializes local SQLite `user_identity` and `user_profile` tables with the anchor node.

### Phase 2: Cryptographic Pairing Handshake
1. **Initiator (Host)** presents a secure QR code containing `{agent_id, active_group, public_key, timestamp}`.
2. **Scanner (Member)** scans QR code and transmits a transient pairing request into `/pairing_requests/{host_id}`.
3. **Host Node** verifies signature, approves pairing, and writes atomic record into `/relationships/{group_id}/devices/{member_id}`.

### Phase 3: Category & Role Assignment
1. **Family Circle Roles**: `Parent` (Admin), `Spouse` (Admin), `Child / Ward` (Restricted Tracking), `Elder` (Protected).
2. **Friend / Work Roles**: `Friend`, `Colleague`, `Peer`, `Neighbor`.
3. System applies **Role-Based Access Controls (RBAC)**: Friends and Members cannot view other members unless explicitly multi-paired.

### Phase 4: Safe Zone Creation & Boundary Assignment
1. Safe zones are created under `/safe_zones/{group_id}/{zone_id}` with center coordinates and radius or polygon perimeter.
2. Specific target devices are assigned to the zone.
3. System compiles hardware geofence requests (`GeofenceHelper`) and background ray-casting monitors.

### Phase 5: Scoped Live Telemetry & Spatial Visibility
1. When active, devices publish high-frequency coordinates to `/devices/{id}/device/live`.
2. When screen is off, background heartbeat publishes to `/devices/{id}/telemetry` on adaptive 5–15 min intervals.
3. Local map view renders **ONLY** authorized paired nodes.

---

## 2. Master Multi-Device Topology Rule Matrix

| Step / Attribute | Host Device (`Redmi N7`) | Member Device 1 (`Redmi 5`) | Member Device 2 (`Riddhi`) | Member Device 3 (`Pushpa`) |
| :--- | :--- | :--- | :--- | :--- |
| **Hardware ID** | `2T929153` | `2C811042` | `4379A761` | `F8CAFC67` |
| **Device Model** | Redmi Note 7 Pro | Redmi Note 5 Pro | Test Node Alpha | Test Node Beta |
| **Circle Anchor Group** | `GRP_2T929153` | `GRP_2C811042` | `GRP_4379A761` | `GRP_F8CAFC67` |
| **Circle Membership Role** | **ADMIN / HOST** | **MEMBER (Friend)** | **MEMBER (Child / Ward)** | **ADMIN (Spouse)** |
| **Connected Peers (Cloud SSOT)** | `2C811042`, `4379A761`, `F8CAFC67` | **`2T929153` ONLY** | **`2T929153` ONLY** | **`2T929153` ONLY** |
| **Can See Redmi N7?** | Self (Root Anchor) | ✅ Yes (Friend Anchor) | ✅ Yes (Father Anchor) | ✅ Yes (Spouse Anchor) |
| **Can See Redmi 5?** | ✅ Yes | Self | ❌ **NO (Blocked)** | ❌ **NO (Blocked)** |
| **Can See Riddhi?** | ✅ Yes | ❌ **NO (Blocked)** | Self | ❌ **NO (Blocked)** |
| **Can See Pushpa?** | ✅ Yes | ❌ **NO (Blocked)** | ❌ **NO (Blocked)** | Self |
| **SafeZone Creation Rights** | ✅ Full Creation & Edit | ❌ View Assigned Only | ❌ View Assigned Only | ✅ Full Creation & Edit |
| **Emergency SOS Broadcast** | Broadcasts to ALL 3 Nodes | Dispatches to Redmi N7 Only | Dispatches to Redmi N7 Only | Dispatches to Redmi N7 Only |
| **Remote Siren Trigger** | Authorized on all 3 Nodes | Authorized on Host Only | ❌ Disabled | Authorized on Host Only |
| **Offline SQLite Scope** | 4 Authorized Roster Nodes | Exactly 2 Nodes (`Self` + `Host`) | Exactly 2 Nodes (`Self` + `Host`) | Exactly 2 Nodes (`Self` + `Host`) |

---

## 3. Strict Data Isolation & Closed-Loop Anti-Leak Rules

1. **Zero Cross-Member Leakage**:
   - Member nodes (e.g., `Redmi 5`) must never receive, cache, or render telemetry for other non-paired circle members (e.g., `Riddhi` or `Pushpa`).
2. **Local SQLite Scoping**:
   - Local queries on `getOfflineRoster()` must strictly filter by the active device's verified paired list. Any stale rows from prior test pairings are automatically pruned.
3. **Ghost Node Elimination**:
   - When a node is unlinked in Firebase Cloud, the closed-loop synchronization engine immediately deletes the corresponding entries from local `device_pairings` and `user_identity` tables.
