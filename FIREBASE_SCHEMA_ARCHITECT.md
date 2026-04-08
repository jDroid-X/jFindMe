# Enterprise Database Architecture: jFindMe Forensic Suite

## Architecture Design & Schema Specification (Firebase Realtime Database)

**Version:** 1.0.0 (Forensic Integrity)  
**Status:** ARCHITECT_REVIEWED  
**Principles:** Single Source of Truth (SSOT), Normalized Schemas, Role-Based Access Control (RBAC).

---

## 1. QA Architect Audit Scenarios

Before implementation, the following scenarios must be validated to ensure forensic parity and system stability.

| Scenario ID | Sub-Scenario | Validation Logic | Audit Reason |
| :--- | :--- | :--- | :--- |
| **AUD-ID-01** | Multi-Device Ownership | DID `D1` and `D2` map to `UID_A`. | Ensures one user can manage multiple forensic targets without ID collisions. |
| **AUD-GR-02** | Group Isolation | `Device_C` (Group 2) reads `Device_A` (Group 1). | **MUST FAIL.** Verification of bridge table security is critical for privacy. |
| **AUD-TX-03** | Data Duplication | Personal Name in `locations` node. | **MUST FAIL.** Prevents stale data and sync overhead. |
| **AUD-UX-04** | Form Accessibility | Tab/Enter logic in Pairing Screen. | `Enter` triggers "Request Join". `Tab` cycles focus through input fields. |

---

## 2. Core Schema Map (JSON Structure)

### 2.1 Master Data (Low Frequency)

Stored under isolated paths to reduce read bandwidth during tracking.

```json
{
  "users": {
    "$userId": {
      "display_name": "Agent 007",
      "email": "agent@jfindme.com",
      "profile_pic": "url_to_storage",
      "created_at": 1712560000000,
      "account_status": "ACTIVE"
    }
  },
  "devices": {
    "$deviceId": {
      "user_id": "$userId",
      "alias": "Field HQ - Pixel 8",
      "platform": "Android",
      "os_version": "14",
      "last_seen": 1712570000000
    }
  }
}
```

### 2.2 Relationship Data (The Bridge)

Handles pairing and visibility between distinct hardware entities.

```json
{
  "groups": {
    "$groupId": {
      "name": "Alpha Squad",
      "admin_did": "$deviceId",
      "invite_code": "JFM-4582",
      "created_at": 1712560000000
    }
  },
  "group_members": {
    "$groupId": {
      "$deviceId": {
        "joined_at": 1712565000000,
        "role": "MEMBER"
      }
    }
  }
}
```

### 2.3 Transactional Data (High Frequency)

High-density nodes optimized for shallow queries.

```json
{
  "locations": {
    "$deviceId": {
      "lat": 34.0522,
      "lng": -118.2437,
      "accuracy": 10.5,
      "speed": 1.2,
      "bearing": 90,
      "altitude": 140,
      "ts": 1712575000000,
      "status": "MOVING"
    }
  },
  "pairing_requests": {
    "$requestId": {
      "from_did": "$deviceIdA",
      "to_did": "$deviceIdB",
      "status": "PENDING",
      "ts": 1712574000000
    }
  }
}
```

### 2.4 History Data (Forensic Archival)

Time-series storage separated from live tracking to ensure `locations` node stays lightweight.

```json
{
  "location_history": {
    "$deviceId": {
      "$pushKey": {
        "lat": 34.0522,
        "lng": -118.2437,
        "ts": 1712570000000
      }
    }
  }
}
```

---

## 3. Firebase Security Rules (Architect Grade)

Strict isolation enforced at the server level. Only devices sharing a `groupId` in `group_members` can access each other's data.

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth.uid == $uid",
        ".write": "auth.uid == $uid"
      }
    },
    "devices": {
      "$did": {
        ".read": "auth != null",
        ".write": "data.child('user_id').val() == auth.uid || newData.child('user_id').val() == auth.uid"
      }
    },
    "group_members": {
      "$gid": {
        ".read": "root.child('group_members').child($gid).child(root.child('devices').child(auth.uid).child('active_did').val()).exists()",
        ".write": "auth != null"
      }
    },
    "locations": {
      "$did": {
        ".read": "root.child('group_members').child(query.groupId).child(auth.deviceId).exists()",
        ".write": "$did == auth.deviceId",
        ".validate": "newData.hasChildren(['lat', 'lng', 'ts'])"
      }
    }
  }
}
```

---

## 4. HOW EVERYTHING CONNECTS

Firebase Auth (Identity Authority)
   ↓
**users** (Master: Identity Metadata)
   ↓
**devices** (Master: Hardware Ownership)
   ↓
**groups** (Bridge: The Trust Anchor)
   ↓
**group_members** (Registry: Peer Visibility)
   ↓
**locations** (Transaction: High-Frequency Live Data)
   ↓
**Local Drive Backup (SQLite/Internal DB: Redundancy & Forensic Persistence)**

---

## 5. VALIDATION FLOW (CRITICAL)

1. **Authentication**: User logs in → Firebase Auth verifies UID.
2. **Mapping**: `userId` is mapped to the unique persistent `deviceId`.
3. **Bridge Verification**: Device joins a group and registers in `group_members`.
4. **Local Redundancy**: **Every Cloud Write MUST be mirrored to the Local SQLite/internal database** to ensure data continuity during network outages.
5. **Secure Propagation**: ONLY devices identified in the same `group_members` node can read each other's location streams.
6. **Audit Trail**: Local Drive Backup acts as the source of truth for forensic reconciliation if cloud data is purged or unavailable.

---

## 6. UI/UX Sequence (Tab & Enter Rule)

To ensure operational efficiency across all device types:

- **Pairing Dialog**:
  1. `Tab` [Input: Invite Code]
  2. `Enter` [Action: Verify Code]
  3. `Tab` [Input: Alias]
  4. `Enter` [Action: Send Pairing Request] (Confirmation Alert pop-up)

- **Navigation Flow**:
  - Global hotkeys (if hardware keyboard present) map to Enter for primary actions (SOS, Ping).

---

## Final Status: PASS

- **Minimal**: No redundant user fields in locations.
- **Scalable**: Flat structure allows independent scaling of history vs live data.
- **Secure**: Single bridge point (`group_members`) prevents unauthorized tracking.
- **Real-time**: Shallow `locations` node ensures low latency UI updates.
- **Resilient**: Mandatory Local Drive Backup rule ensures absolute data persistence.
