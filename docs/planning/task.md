# Task Assignments and Progress

## Agent 2 (Logic & Database Engineer)

### Phase 1 Execution
- [x] LocationRepository.kt: Fix Zombie Listeners using a HashMap<DatabaseReference, ValueEventListener> to track and safely unregister anonymous listeners in `stopListening()`.
- [x] LocationRepository.kt: Add debounce logic to `Geocoder.getFromLocation` so it only fires if the device moved significantly or a set time elapsed, preventing API bans.
- [x] LocationRepository.kt: Fix FATAL Firebase Egress. Do not download the entire `devices` snapshot. Instead, query specifically for the IDs in `allowedList` and `friendList` using individual listeners, or implement a secure query structure.
- [x] SIMTrackingService.kt: Strip out the permanent Wakelock (`cpuWakeLock`) and `Timer` loop. Convert the forensic logging into a `PeriodicWorkRequest` using Android WorkManager (`SimForensicWorker`).

### Phase 2 Execution
- [x] Create/verify `LiveDeviceModel.kt` and `SafeZoneModel.kt` in the data layer.
- [x] Create `MapV2ViewModel.kt` (in ui/map/) to handle Firebase reads, SafeZone haversine distance calculations, and offline caching validations (Scenario A and B).

### Phase 3 Execution
- [x] Update `FirebaseComm.kt` to wrap snapshot/JSON parsing in `try-catch` blocks specifically targeting `JSONException`. Implement an exponential backoff mechanism for retries during connection timeouts.
- [x] Create a `database.rules.json` file in the project root implementing strict Firebase rules so only validated friends can read a user's `live` node location.
