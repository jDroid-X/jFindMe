# Security, Identity & Anti-Tamper Specification

## 1. Silicon Hardware Identity Anchor (SSOT)
* **Immutable Anchor**: Device identity is bound to physical hardware properties (`ro.ril.oem.sno`, `ro.ril.oem.psno`, OEM serials, or Android Keystore Hardware Agent ID).
* **Anti-Spoofing Rule**: Software UUIDs, temporary session tokens, or unverified Firebase auth UIDs are NEVER permitted to overwrite hardware identity keys.

---

## 2. 3-Way Hardware Silicon Pairing Protocol
1. **Initiation**: Guardian/Host generates a dynamic 8-character OTP combined with a cryptographically secure single-use session nonce (10-minute TTL).
2. **Scan & Verification**: Client/Node scans Host QR code, extracts hardware key, and transmits payload signed with local Keystore key.
3. **Cloud & Local Validation**: Firebase validates single-use nonce against Circle ACL. Upon 200 OK, node is injected into local SQLite `MASTER_ROSTER`.

---

## 3. Threat Model & Anti-Tamper Detection Matrix

| Threat / Tamper Vector | Detection Method | System Countermeasure |
|---|---|---|
| **Mock GPS / Location Spoofing** | `Location.isMock()` / `Location.isFromMockProvider()` | Reject fix, log security violation, set RAG status to `🚨 MOCK_LOCATION`, notify guardian. |
| **Root / Magisk Tampering** | Scans for `/system/xbin/su`, `/system/bin/su`, Magisk binaries, test-keys build tags. | Flag device profile with `isRooted: true`, elevate alert status. |
| **Developer Options / ADB** | `Settings.Secure.getInt(DEVELOPMENT_SETTINGS_ENABLED)` | Log vitals telemetry flag `devOptionsActive: true`. |
| **SIM Swap / Removal** | `TelephonyManager.getSimSerialNumber()` mismatch vs `sim_table` in SQLite. | Trigger immediate `SIM_CHANGED_ALERT` push broadcast to Circle guardians. |

---

## 4. Cryptographic Storage & Sanitization
* Local database tables encrypted using AES-256 GCM.
* Factory reset zeroizes cryptographic keys and executes multi-pass overwrite before file deletion.
