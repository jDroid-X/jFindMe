# Location Performance & Accurate Data Display Rules
Source Framework: `C:\Users\jiten\jAnitGravity\.kit`

## 1. High Performance Telemetry
- **Adaptive Polling:** High-frequency location updates (1-3s) when speed > 5 km/h; throttle to low-frequency (30s) when stationary.
- **Marker Movement:** Update marker `setLatLng` directly via transform matrices; NEVER re-initialize or reload the map instance.
- **Data Batching:** Aggregate location history points before committing to database to minimize network overhead.

## 2. Accurate Data Display
- **Accuracy Radius:** Always render a translucent circle representing horizontal accuracy radius (`accuracy` in meters).
- **Sub-Pixel Interpolation:** Smoothly interpolate coordinates between GPS ticks to prevent marker jumping or stuttering.
- **Single Source of Truth:** Changes to Avatar, Nickname, or Safe Zone must cascade across all UI views and tables simultaneously.

## 3. Circuit Breakers & Fallback Handling
- If Firebase RTDB connection drops, seamlessly switch to IndexedDB/local cache and display an offline badge with timestamp.
- On GPS loss, indicate "Last Known Location" with timestamp rather than clearing the display.
