# Task 04: Advanced Telemetry, GIS & Geofencing Specification

## 1. Feature Overview
This specification details the enterprise requirements for advanced spatial computation, trajectory tracking, polygon geofences, and dynamic telemetry throttling for `jFindMe`.

---

## 2. Multi-Point Polygon SafeZones
* **Definition**: In addition to standard circular radius geofences, nodes and circles support arbitrary convex and non-convex polygon boundaries (e.g. school campuses, residential communities, corporate complexes).
* **Data Representation**: Array of coordinate vertices `[{lat: Double, lng: Double}, ...]` serialized as JSON in `safe_zones.polygon_points`.
* **Containment Algorithm**: Point-in-Polygon (PIP) ray-casting algorithm executing in $< 2\text{ ms}$ on mobile CPU.
* **Scheduled Time Windows**:
  - `active_days_mask`: Bitmask for active days of week (Bit 0 = Sun ... Bit 6 = Sat).
  - `active_start_time`: Time of day in seconds from midnight (e.g. 28800 for 08:00 AM).
  - `active_end_time`: Time of day in seconds from midnight (e.g. 54000 for 03:00 PM).
* **Dwell & Loiter Alerts**: Triggers `ZONE_DWELL_ALERT` if a node remains inside a designated zone exceeding `dwell_threshold_minutes`.

---

## 3. 24-Hour Historical Trajectory & Breadcrumbs
* **Trajectory Replay**:
  - Time-indexed retrieval from local SQLite `tracking_history` indexed on `(target_uid, timestamp DESC)`.
  - Motion Heading Chevrons: Directional markers rendered along the trajectory based on instantaneous bearing (`0°–360°`).
  - Speed Heatmap Coloring:
    - 🟢 $< 10\text{ km/h}$ (Stationary / Walking)
    - 🟡 $10–40\text{ km/h}$ (Biking / City Traffic)
    - 🔴 $> 40\text{ km/h}$ (Transit / Highway Driving)
* **Stationary Stop Detection**: Clusters adjacent fixes with displacement $< 20\text{ m}$ for duration $> 5\text{ min}$ into a single "Stop Point" with arrival/departure timestamps.

---

## 4. Adaptive Telemetry Polling Engine
Dynamic battery-aware location polling intervals:

| State | Condition | Location Polling Interval | Min Displacement |
|---|---|---|---|
| **EMERGENCY SOS** | `isSos == true` | 1 second | 0 meters |
| **ACTIVE MOTION** | Speed $> 1.5\text{ m/s}$ or Activity != STILL | 5 seconds | 10 meters |
| **STATIONARY** | Displacement $< 10\text{ m}$ for 5 min | 300 seconds (5 min) | 25 meters |
| **BATTERY SAVER** | Battery $\le 15\%$ & Not Charging | 600 seconds (10 min) | 50 meters |
