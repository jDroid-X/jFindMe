# 🏛️ Global UI Architect: Universal Screen Workflow (L7 Enterprise)

This document serves as the **Single Source of Truth** for managing UI screens within the jFindMe ecosystem. It mandates a deterministic approach to programmatic layout, responsive scaling, and high-fidelity aesthetics.

---

## 🧭 The Universal 7-Step Workflow

Follow this protocol for every new screen or major refactor:

### 1. Requirement Matrix (Nomenclature & Identity)

- **Identify Role**: Determine if the screen is for `SELF` (Read/Write) or a `TARGET` (Read-Only).
- **Map Tokens**: Identify all mandatory inputs requiring a Red `(*)` asterisk in both the Label and the Hint.
- **Enterprise Language**: Use standard nomenclature:
  - *Profile* -> **Dossier**
  - *Device* -> **Node**
  - *Country/State* -> **Territory / Zone**
  - *History* -> **Audit Logs**

### 2. Primary Foundation (BaseActivity)

- **Class Signature**: The Activity **MUST** extend `BaseActivityJava`.
- **Structural Integrity**: Initialize a `LinearLayout` or `FrameLayout` root.
- **Scroll Buffer**: Wrap contents in a `ScrollView` to handle **Stage 1-5** scaling without clipping.
- **Thematic Injection**: Set `root.setBackgroundColor(getPrimaryColor())`.

### 3. Responsive Quota Injection (Zero DIP Policy)

- **Rule**: Never use hardcoded `dp`. Replace with dynamic quotas:
  - **Padding/Margins**: `int quota = getResponsivePaddingQuota();`
  - **Hero Spacing**: `int topGap = getResponsiveHeaderTopQuota();`
  - **Icon Geometry**: `int iSize = getResponsiveIconQuota();`
  - **Corner Geometry**: `float radius = getResponsiveRadiusQuota();`

### 4. Component Construction (Waterfall MVVM)

- **Hero Header**: Call `applyGlobalHeaderGeometry(header)` for the two-point linear gradient.
- **Label-and-Box**: Every input field must have a bold descriptive label above a themed `wrapEnterpriseField` box.
- **Primary CTA**: Position the master action button at the absolute bottom with **Stage-Scaled vertical breathing room** (100dp baseline).

### 5. Aesthetic Hardening (Elevation & Glass)

- **Layer 3 Cards**: Use `UiArchitect.applyCard(this, view, "CARD_STANDARD")` for elevation >= 8f.
- **Glassmorphism**: Apply a 15% white overlay (`CARD_GLASS`) to secondary content containers.
- **Capsule Geometry**: Action buttons must have a **50f radius** and **25f elevation**.

### 6. Accessibility & Interactivity (D-Pad/Enter)

- **Tab Sequence**: Order fields logically from Top-Left to Bottom-Right.
- **IME Action**: Bind the `Enter` key (`IME_ACTION_DONE`) to trigger the primary screen action. Call `vibrate(50)` on success.
- **Installation Guide**: If critical fields are empty on `onOpen`, intercept the user with a Non-Cancelable Guide Popup.

### 7. Stage 5 Validation (QA Final Audit)

- **5-Stage Warp**: Test the screen in the `UiConfigActivity` diagnostic hub.
- **Hash Verification**: Ensure a stage change triggers a global `safeRecreate()` across the activity backstack.

---

## 📐 The Global Scaling Matrix (L7 Architecture)

### I. Screen Padding Matrix (UI Parameters)

| Design Stage | Target Device Profile | Screen Padding (dp) | Header Top Gap (dp) | Text Scale Factor |
| :--- | :--- | :--- | :--- | :--- |
| **LIBERAL** | Tablets / Large Displays | 25dp | 30dp | 1.6x |
| **STANDARD** | Standard Sized Smartphones | 20dp | 20dp | 1.3x |
| **COMPACT** | High-Density Phablets | 15dp | 15dp | 1.0x |
| **SLIM** | Small/Legacy Smartphones | 10dp | 10dp | 0.8x |
| **ULTRA_SLIM** | Micro Screens / HW | 5dp | 5dp | 0.65x |

### II. Iconography Scaling (Vitals)

| Icon Tier | Base Size (Compact) | Liberal (1.6x) | Slim (0.8x) | Ultra (0.65x) |
| :--- | :--- | :--- | :--- | :--- |
| **ICON_TINY** | 90px | 144px | 72px | 58px |
| **ICON_SMALL** | 110px | 176px | 88px | 71px |
| **ICON_STANDARD** | 140px | 224px | 112px | 91px |
| **ICON_LARGE** | 180px | 288px | 144px | 117px |
| **ICON_HERO** | 250px | 400px | 200px | 162px |

### III. Shadow Elevation (Depth Physics)

| Depth Tier | Base (Ultra 1.0x) | Compact (1.4x) | Standard (1.6x) | Liberal (2.0x) |
| :--- | :--- | :--- | :--- | :--- |
| **SHADOW_LOW** | 4f | 6f | 7f | 8f |
| **SHADOW_MEDIUM** | 12f | 17f | 19f | 24f |
| **SHADOW_HIGH** | 25f | 35f | 40f | 50f |

### IV. Corner Radius Hierarchy (Curvature)

| Radius Tier | Base (Compact 1.0x) | Liberal (2.0x) | Slim (0.6x) | Ultra (0.4x) |
| :--- | :--- | :--- | :--- | :--- |
| **RADIUS_TIER_1** | 25f | 50f | 15f | 10f |
| **RADIUS_TIER_2** | 50f | 100f | 30f | 20f |
| **RADIUS_TIER_3** | 100f | 200f | 60f | 40f |

---

## 🎨 Visual Identity Rules

1. **Zero Flat Surfaces**: Every card, tile, and button must have visible shadow depth (Elevation 8f-50f).
2. **Gradient Palette**: Use two-point linear gradients (Left-to-Right) for all primary headers and section headers.
3. **AngelLoader Sequence**: All state transitions (Loading, Syncing, Saving) must use the **AngelLoader** sequence with real-time status text updates.
4. **Haptic Integrity**: Success handshakes require haptic feedback (50ms vibration).
5. **No 90-Degree Corners**: Minimum corner radius for any container is **16f** (Standard) or **25f** (Enterprise Card).
6. **Label Precision**: Always use bolded labels above fields. If specified by the L7 design, labels may also be mirrored inside the hint for absolute clarity.

---

## 🛠️ Global UI Architect Diagnostic Suite

The `UiConfigActivity` acts as the command-and-control center for UI scaling. It monitors and validates the following **12 Core Elements**:

### I. The 12-Element Registry (Diagnostic Monitors)

1. **Icons (IconTable)**: 10-property scaling matrix (Alpha, Glow, Tint, Bounds).
2. **Tiles (DashboardTileTable)**: Grid-based dashboard elements with 5-stage sizing.
3. **Cards (EnterpriseCardTable)**: Universal elevation, radius, and depth containers.
4. **Input Fields (FormInputTable)**: Standardized labeled inputs and themed text boxes.
5. **Headers (HeaderTable)**: Multi-point linear gradient hero zones.
6. **Bottom Nav (BottomNavTable)**: Dynamic tabbed navigation with active-state physics.
7. **Action Buttons (ActionButtonTable)**: High-fidelity capsule buttons with haptic hooks.
8. **List Items (ListItemTable)**: Responsive diagnostic rows and history log cards.
9. **Marquees (MarqueeTable)**: Real-time scrolling system status bars.
10. **Status Pills (StatusPillTable)**: Color-coded health badges and status levels.
11. **System Geometry (SystemGeometryTable)**: Centralized shadow, radius, and padding quotas.
12. **Themes (ThemeManager)**: 7-theme multi-palette color coordination engine.

### II. Real-Time QA Monitors

- **Geometric Depth Monitor**: Visualizes elevation (4f to 50f) recalculated by active stage.
- **Scaling Awareness Monitor**: Reports the active structural multiplier (x0.65 to x1.6).
- **Component Density Monitor**: Validates padding/header quotas (5dp to 30dp).
- **Vector Icon Density Monitor**: Validates icon bounds (50px to 250px).
- **Widget Tint Monitor**: Tracks theme-integrated luminance and alpha states.

---
*Verified by the Global UI Architect - Stage 5 Validation Complete.*
