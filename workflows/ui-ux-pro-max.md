# UI/UX Pro Max Rules (jFindMe Location Tracking Edition)
Source: `.kit/.agent/workflows/ui-ux-pro-max.md`

## 1. Visual Hierarchy & Aesthetic Standards
- **Style:** Modern dark mode & sleek glassmorphism with high visual contrast.
- **Typography:** Google Fonts (`Outfit`, `Inter`, `JetBrains Mono` for telemetry numbers).
- **HUD Elements:** Floating telemetry cards (Speed, Battery %, Accuracy radius, Online badge) with subtle backdrop-filter blur.

## 2. Dynamic Map Display
- Marker animations MUST use CSS hardware acceleration / sub-pixel transforms instead of full map canvas reloads.
- Polyline history playback with gradient speed trails (e.g. green for cruising, yellow for idle, red for rapid deceleration).
- SafeZone geofences rendered with translucent animated pulsing boundaries.

## 3. Vertically Compact Layout
- Ultra-compact controls to maximize usable map viewport.
- Collapsible sidebar / bottom sheet for device list and family circles.
- Complete Material Design menu drawer without dead navigation paths.
