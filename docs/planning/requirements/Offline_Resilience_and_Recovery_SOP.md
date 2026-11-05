# Offline Resilience, Network Recovery & Watchdog SOP

## 1. Zero-Data-Loss Offline Telemetry Pipeline
When connectivity is unavailable:
1. Every location fix, battery change, or geofence event is encapsulated into a `PendingSyncItem`.
2. Written immediately to SQLite table `pending_sync_queue`.
3. UI displays the slide-in **Offline Sync Pill** indicating cached point count.

---

## 2. Network Recovery & Replay Procedure
1. `ConnectivityManager.NetworkCallback` detects network connectivity (`NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED`).
2. `OfflineSyncManager` triggers batch retrieval from `pending_sync_queue` ordered by `timestamp ASC`.
3. Performs atomic Firebase Realtime Database multi-path update.
4. Upon successful cloud acknowledgment, purges synced records from SQLite.

---

## 3. Background Watchdog & Self-Healing Protocol
* **Watchdog Interval**: 15 minutes via `AlarmManager.setRepeating` or `setExactAndAllowWhileIdle`.
* **Health Check**:
  - Checks if `TrackingService` is running (`isServiceRunning(TrackingService::class.java)`).
  - Checks if last location fix timestamp is within expected threshold for current state.
* **Auto-Recovery**:
  - If service was terminated by Android OS (Doze mode, low memory killer, OEM battery management), the watchdog immediately starts `TrackingService` with `ContextCompat.startForegroundService`.
  - Emits telemetry error log into `change_log_table` for forensic diagnosis without prompting annoying ANR or crash dialogs to the user.
