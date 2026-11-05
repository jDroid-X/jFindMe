# RCA: Live Map Slowness & Hang

## ✅ Phase 2 Changes Applied (2026-10-04)

| Change | File | What was done |
|--------|------|---------------|
| RTDB subscription cap | `LocationRepository.kt` | `updateTargetListeners()` now caps concurrent peer subscriptions at `MAX_RTDB_LISTENERS = 25` (self always live). Peers beyond the cap no longer open a fresh `live` + `profile` listener pair, eliminating the listener-storm on large rosters; they fall back to last-known SQLite values. |
| `bindSecureFamilyMesh` idempotency | `MapViewModel.kt` | Added `lastBoundMeshAgentId` guard + a `force` param. Resumes skip the detach + full-node re-pull when the same agent is already bound. `refreshLocations()` (manual "REFRESH SYNC") passes `force = true` to keep the full re-subscribe. |

---

## ✅ Phase 1 Changes Applied (2026-10-04)

| Change | File | What was done |
|--------|------|---------------|
| Parallel Safe-Zone fetch | `MapViewModel.kt` | `loadSafeZones()` now fires all 3 RTDB zone sources **in parallel** via `suspendCancellableCoroutine` + `coroutineScope{ async }` on `Dispatchers.IO`. No more 3-tier sequential waterfall. |
| Lightweight `refreshOnResume` | `MapViewModel.kt` + `MapActivity.java` | New `refreshOnResume()` skips `invalidateRoleAndNameCaches()`, `bindSecureFamilyMesh()` re-attach, and `fetchCurrentUserRole()`. `MapActivity.onResume` now calls this instead of the full `refreshLocations()`. Full refresh is reserved for the manual "REFRESH SYNC" button. |
| Offload per-packet DB to IO | `LocationRepository.kt` | `processIncomingLocation()` SQLite reads/writes now run on a scoped `CoroutineScope(SupervisorJob()+Dispatchers.IO)`, re-entering `Dispatchers.Main` only for the `latestLocations` mutation + dispatch. The unstructured per-packet `saveHistoryPoint` coroutine is now in-scope and cancelled via `ioScope.cancel()` in `stopListening()`. |
| GPS timeout | `MapActivity.java` | `fusedLocationClient.getCurrentLocation(100, null)` now uses a `CancellationTokenSource` auto-cancelled after **10s** via `Handler.postDelayed`, preventing the unbounded HIGH_ACCURACY wait. `getLastLocation()` still serves as fallback. |

### Verified False Alarms (no change needed)

| # | Claim | Finding |
|---|-------|---------|
| C5 | `TrackingService` creates a duplicate `LocationRepository` that re-runs `startListening` | **False.** `TrackingService` only calls `repository.updateMyLocation()` — it never calls `startListening()`. No duplicate RTDB subscriptions. |
| C9 | `AvatarLoader` re-decodes bitmaps from disk on every GPS fix | **False.** `AvatarLoader` already has a `LruCache<String, Bitmap>` (40 slots). Bitmaps are decoded once and cached by `(deviceId, size)` key. |

---

## Overview

The Live Map (`MapActivity`) is a composite of **5 interlocking engines** all firing at `onCreate`/`onResume`/`onMapReady`/`onResume` time:

1. `MapViewModel.init` (fires in `onCreate` before `super.onCreate`)
2. `MapActivity.setupLogic` (fires after layout inflated)
3. `LocationRepository.startListening` (via `MapViewModel.init`)
4. `TrackingService.onCreate` (launched from `getCurrentLocation` inside `onMapReady`)
5. `onResume` cascade (refresh, marker render, radar, geocoding)

Every one of these is doing **main-thread SQLite I/O, Firebase RTDB subscription attach, bitmap decoding, and Python VM warmup simultaneously**, creating a main-thread contention storm that blocks the map renderer until all of them complete.

---

## Full List of Delay/Hang Culprits

### CRITICAL — Direct Main-Thread Blockers

| # | Location | What it does | Why it's slow |
|---|----------|-------------|--------------|
| C1 | `MapViewModel.init` → `loadSafeZones()` | 3 chained sequential `addListenerForSingleValueEvent` Firebase queries, each only starts when the previous returns **empty** | Worst-case = 3 × network RTT (~1–3s each) = 3–9s of idle waiting for Safe Zone data before map overlay renders. SQLite `getActiveSafeZones()` also blocks the main thread. |
| C2 | `MapViewModel.init` → `bindSecureFamilyMesh()` | Detaches old listener, re-attaches new RTDB listener on `devices/{id}` and `relationships/{group}`. Each attach triggers a **full node sync** from the Firebase server. | Two large-node RTDB subscriptions fire before the map is even ready. |
| C3 | `LocationRepository.updateTargetListeners()` | For **every** UID in the roster: reads SQLite (`resolveAgentIdLocally`, `isDeviceDeleted`, `getAllPairedDeviceIds`, `getOfflineRoster`), then attaches **2 RTDB listeners** (`live` + `profile`). Called from `startListening`, group listener `onDataChange`, friend listener `onDataChange`. | With N paired devices, each node's initial RTDB sync is a network fetch. All N × 2 listeners stack up before the first location packet is delivered. Also called on every group/friend matrix change (each re-renders the full roster). |
| C4 | `LocationRepository.processIncomingLocation` | Runs on the **main thread** (Firebase listener callback). Per packet: `db.getLocalDossier`, `db.getDeviceCentricName`, `db.getDeviceCentricAvatar`, `db.resolveAgentIdLocally`, `db.updateDeviceIdentityAndAvatar` (a **write**), and fires an unstructured `CoroutineScope(Dispatchers.IO).launch` for `db.saveHistoryPoint`. | Multiple SQLite reads + 1 write on the main thread **per location packet**. With multiple devices updating simultaneously, main-thread SQLite lock contention causes jank. The unstructured coroutine scope has no cancellation — they accumulate. |
| C5 | `TrackingService.onCreate` → `new LocationRepository(this)` | TrackingService **creates its own `LocationRepository` instance**, which re-runs `startListening` → all the same RTDB + SQLite work as MapActivity's `MapViewModel`. | **Double subscription**: every RTDB listener is attached **twice** (once by MapActivity, once by TrackingService). SQLite reads are also doubled. This alone can double perceived latency and RTDB bandwidth. |
| C6 | `TrackingService.onCreate` → `setupBackgroundSyncListener` / `setupRemoteCommandListener` / `setupLiveVersionUpdateListener` / `setupConnectedDevicesAvatarSyncListener` | Each attaches a **separate RTDB `ValueEventListener`** on a different node. | 4 additional RTDB subscriptions all attached during the same window as the map is loading. |
| C7 | `BaseActivityJava.onCreate` → `new Thread { SpatialProcessor.init(...) }` → `Python.start(AndroidPlatform(context))` | Initializes the **Chaquopy Python VM** (loads CPython shared library, inits interpreter, imports native modules). | While on a background thread, it competes for CPU with the main thread during the exact window where the map is rendering. On low-end devices this can cause a multi-second CPU spike. |
| C8 | `MultiMarkerEngine.processViewModelLocations` | Called on every location update via the LiveData observer. Per device: `db.isDeviceDeleted` (15s cache), `db.resolveAgentIdLocally` (no cache), `RoadSmoothingEngine.processLocation` (Kalman filter), `db.getDeviceCentricName`/`Avatar` (cache miss only). Plus full O(N²) BFS collision resolution on every frame. Runs on **main thread**. | For N devices: N × multiple SQLite reads per update + O(N²) collision math. With 10+ devices updating every 5–10s, main thread is saturated. |
| C9 | `AvatarLoader.getCurrentUserAvatarBitmap(this, 256)` | `BitmapFactory.decodeFile` on the **main thread**, called on every `updateUserPositionOnMap` (every self-location fix) for the "You" marker. | Decoding a 256×256 bitmap from disk on the main thread per location update. The bitmap should be cached once; instead it's re-decoded every time. |
| C10 | `onResume` → `refreshLocations()` → `bindSecureFamilyMesh()` | **Re-tears-down and re-attaches** RTDB listeners on every `onResume`. Every time the user grants a permission, returns from PiP, or the GPS settings dialog is dismissed. | RTDB re-subscription cost fires on every resume. The initial snapshot for the re-attached listeners triggers a full node re-pull before the map shows updated data. |

### HIGH — Indirect / Compounding Delays

| # | Location | What it does | Why it compounds |
|---|----------|-------------|-----------------|
| H1 | `MapActivity.onMapReady` → `checkLocationPermission` → `enableLocationFeatures` → `FirebaseComm.syncDeviceAccessPermissions` + `syncVersionUpdateMetadata` | Two `setValue`/`updateChildren` RTDB writes queued on the main thread during map ready. | Adds 2 RTDB write round-trips before the map camera is positioned. |
| H2 | `MapActivity.getCurrentLocation` → `fusedLocationClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, null)` | `priority=100` is HIGH_ACCURACY, but **no timeout** is set (second arg is `null` CancellationToken). | The call waits for a GPS fix indefinitely before falling back to `getLastLocation()`. On slow-GPS devices this can be 10–30s before the camera animates to the user position. |
| H3 | `MapActivity.setupLogic` → `getCurrentLocation` → `startForegroundService(TrackingService)` | Starts TrackingService from the same call that's trying to position the camera. | The service's `onCreate` work (C5, C6) happens **before** the camera animation completes. |
| H4 | `updateRadarScanner` | Called from: `onResume` (if locations cached), `MultiMarkerEngine` observer, `updateUserPositionOnMap`. Rebuilds the `headerFlipper` cards with a full sort + view creation/destruction cycle. | 3 trigger points with overlapping calls = redundant view churn on every location update. |
| H5 | `updateAddressMarquee` → `Geocoder.getFromLocation` | Triggers a network/geocode lookup for the marquee address text. | Each camera-target lock triggers a geocode. On Android <13 this blocks on `geocodeExecutor`; on 13+ the callback is async but still adds a perceptible delay to the marquee text update. |
| H6 | `MapViewModel.loadSafeZones` → `onCloudZonesLoaded` → writes to SQLite (`SafeZoneTable.save`) | For every cloud zone, a SQLite **write** is performed inside the listener callback. | SQLite writes inside a listener callback add I/O latency per zone. With many zones, the safe-zone overlay render is delayed by the write cost. |
| H7 | `MapViewModel.init` → `repository.startListening()` → `resolveHardwareIdentity` (async RTDB lookup) | The `resolveHardwareIdentity` callback is what triggers `updateTargetListeners()` for cloud-based targets. | Until this async lookup completes, only the locally-known roster listeners are active. The cloud targets are added **later**, causing a second wave of RTDB attaches after the initial one. This two-wave pattern doubles initial latency. |
| H8 | `MapActivity.onCreate` → `MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, ...)` | Synchronous renderer selection before `super.onCreate`. | On some device configurations this can block while the Map SDK initializes the native renderer. |
| H9 | `MapActivity.createLayout` → Lottie `butterfly_angel` animation (infinite, 1.25× speed) | A continuously-animating LottieView on the map header. | Competes with the map's GPU renderer for the same hardware-accelerated canvas. On low-end devices this causes dropped frames during map tile loading. |
| H10 | `AngelLoader.show(this)` (when `EXTRA_FROM_SPLASH` is set) | Blocks user input for **1000ms** with a splash overlay after Map loads. | Masks the actual load time and creates a perception of a blank/slow screen. |

### MEDIUM — Recurring Cost

| # | Location | What it does | Why it matters |
|---|----------|-------------|--------------|
| M1 | `LocationRepository` per-packet `CoroutineScope(Dispatchers.IO).launch` (saveHistoryPoint) | Unstructured coroutine with no owner/cancellation. Each location packet spawns a new coroutine. | Coroutines accumulate over session lifetime. No back-pressure. |
| M2 | `LocationRepository.updateMyLocation` → `Geocoder` (new instance every call) | `Geocoder(context, Locale.getDefault())` is constructed every call, not cached. | Repeatedly instantiating the Geocoder object adds overhead. |
| M3 | `deviceDeletedCache` (15s TTL) in `MultiMarkerEngine` | `db.isDeviceDeleted` is called for every device on every render; cached for 15s. | After 15s the cache expires and the next render re-hits SQLite for all devices. |
| M4 | `MapViewModel.refreshLocations()` in `onResume` clears `MultiMarkerEngine` caches via `invalidateRoleAndNameCaches()` | Every resume wipes `iconCache`, `deviceNameCache`, `deviceAvatarCache`, `deviceDeletedCache`. | Forces a full re-resolve on the next `processViewModelLocations` call, causing a spike of SQLite reads + bitmap decodes. |

---

## Call-Flow Map (First Launch)

```
MapActivity.onCreate
  └─ MapViewModel.init            [fires before super.onCreate]
       ├─ syncLocalMeshIds()         SQLite: getAllPairedDeviceIds + getOfflineRoster
       ├─ repository.startListening()
       │    └─ updateTargetListeners()  SQLite × N + RTDB × 2N attaches
       │    └─ resolveHardwareIdentity → updateTargetListeners (second wave, async)
       ├─ fetchCurrentUserRole()     RTDB
       ├─ bindSecureFamilyMesh()      RTDB listener attach × 2
       ├─ bindPendingRequestsListener RTDB
       └─ loadSafeZones()            SQLite + RTDB 3-tier waterfall
  └─ distantDeviceViewModel.updateUserContext
  └─ super.onCreate → BaseActivityJava.onCreate
       └─ new Thread { SpatialProcessor.init() }  ← Python VM warmup
       └─ createLayout()             ← MapActivity.createLayout (Lottie, flipper, drawer)
  └─ setupLogic()
       ├─ SupportMapFragment.getMapAsync  ← Google Map renderer init
       ├─ LocationEngine init           ← FusedLocationClient + EngineRegistry observer
       └─ observers (userLocations → MultiMarkerEngine.processViewModelLocations)
  └─ onMapReady
       ├─ checkLocationPermission → enableLocationFeatures
       │    ├─ getCurrentLocation       ← no-timeout fused GPS call
       │    ├─ FirebaseComm.syncDeviceAccessPermissions  (RTDB write)
       │    └─ FirebaseComm.syncVersionUpdateMetadata     (RTDB write)
       └─ startForegroundService(TrackingService)
            └─ onCreate: new LocationRepository + 4× RTDB listeners + LocationEngine + ...

onResume (fires every time Map becomes visible)
  ├─ EngineRegistry.activate
  ├─ triggerCalloutRefresh → updateUserPositionOnMap → MultiMarkerEngine + geocode
  ├─ validateLocationHardwareStatus (may show dialog)
  ├─ OEMPowerWhitelistManager.checkAndPromptWhitelisting (may show dialog)
  ├─ viewModel.refreshLocations()  ← full re-subscribe + cache wipe
  └─ renderSafeZonesOnMap + MultiMarkerEngine.processViewModelLocations
```

---

## Recommended Fix Priority

### Phase 1 — Highest impact, lowest risk (target: 50–70% latency reduction)

1. **De-duplicate TrackingService's LocationRepository.** Make `TrackingService` reuse the singleton `LocationRepository` from `MapViewModel`'s application-scoped ViewModel instead of creating a new instance. This eliminates **all** duplicate RTDB subscriptions and SQLite reads (fixes C5, C6 partially).

2. **Move `loadSafeZones` to a background thread / Coroutine.** Replace the 3-tier sequential RTDB waterfall with a single `Coroutine` on `Dispatchers.IO`. Fire all three lookups in parallel with `async` + `await` instead of chaining. Fall back to the SQLite cache immediately, then update from cloud in the background (fixes C1, H6).

3. **Cache the self avatar bitmap.** In `AvatarLoader`, add a `synchronized` in-memory `Bitmap` cache keyed by `(avatarId, width)`. Avoid re-decoding from disk on every location update (fixes C9).

4. **Move `processIncomingLocation` DB work off the main thread.** Wrap the entire DB access block in a `Dispatchers.IO` coroutine and `postValue` the result back. This unblocks the main thread from SQLite contention (fixes C4).

5. **Set a timeout on `getCurrentLocation`.** Use `fusedLocationClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, CancellationTokenSource(10000).token)` with a 10-second timeout instead of indefinite wait (fixes H2).

### Phase 2 — Medium effort, significant latency improvement

6. **Don't wipe `MultiMarkerEngine` caches in `onResume`.** Only invalidate when a specific device's data actually changes. `refreshLocations` in `onResume` currently forces a full cache miss every time the Map becomes visible (fixes M4).

7. **Lazy-attach RTDB listeners per-UID only when the Map is visible AND the user has actively selected to track that peer.** Keep a global "always-subscribed" list for Self + recently-active peers, and on-demand subscribe for the rest (fixes C3, H7).

8. **Move `bindSecureFamilyMesh` re-attach to `onResume` only when the active group actually changed** (compare `activeGrp` string to what was last used) (fixes C10).

### Phase 3 — Polish / Perception

9. **Remove the 1-second `AngelLoader` overlay** (H10) — it masks real load time and adds perceived latency.

10. **Debounce `updateRadarScanner`** to fire at most once per 500ms using a single `Handler.postDelayed` guard, instead of being triggered from 3 overlapping call sites (fixes H4).

11. **Cache `Geocoder` instance** as a `@Singleton` in the `Application` rather than constructing it per call (fixes M2).

12. **Replace unstructured `CoroutineScope(Dispatchers.IO).launch`** in `processIncomingLocation` with a structured `CoroutineScope(SupervisorJob() + Dispatchers.IO)` owned by the `LocationRepository` instance, cancelled in `stopListening` (fixes M1).

---

## Files That Need Changes

| File | Changes |
|------|---------|
| `app/src/main/java/com/jdroidx/jfindme/service/TrackingService.kt` | Remove `new LocationRepository(this)`; use shared singleton or app-scoped instance |
| `app/src/main/java/com/jdroidx/jfindme/ui/map/MapViewModel.kt` | `loadSafeZones` → parallel coroutines + immediate SQLite fallback; `bindSecureFamilyMesh` → only re-attach on group change |
| `app/src/main/java/com/jdroidx/jfindme/data/repository/LocationRepository.kt` | Move `processIncomingLocation` DB work to IO thread; add `@Singleton` or application-scope instance; cancel coroutines in `stopListening` |
| `app/src/main/java/com/jdroidx/jfindme/ui/map/MapActivity.java` | `getCurrentLocation` → add `CancellationTokenSource(10_000)`; remove `refreshLocations` cache wipe from `onResume`; debounce `updateRadarScanner` |
| `app/src/main/java/com/jdroidx/jfindme/engine/avatar/AvatarLoader.java` | Add in-memory `Bitmap` LRU cache keyed by `(deviceId, width)` |
| `app/src/main/java/com/jdroidx/jfindme/engine/map/MultiMarkerEngine.kt` | Keep `deviceDeletedCache` but add longer TTL or invalidate only on explicit device removal |
| `app/src/main/java/com/jdroidx/jfindme/ui/base/BaseActivityJava.java` | Defer `SpatialProcessor.init` to `onIdle` via `Choreographer.postFrameCallback` to avoid CPU contention during map load |
