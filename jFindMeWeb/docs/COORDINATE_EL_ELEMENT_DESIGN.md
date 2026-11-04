# 🎨 Coordinate EL: Element Layer & UI Design System (Functional & Technical)
## jFindMe / jButterfly Enterprise Ecosystem & jFindMeWeb Subsystem
> **Enterprise Standard v5.0** | **Codename:** `jDroid-X` | **Operator:** `JEET`  
> **Repository Target:** `C:\Users\jiten\jAnitGravity\jFindMe\jFindMeWeb`  
> **Coordinate:** `EL` in `ROOT ⊃ BR ⊃ EL ⊃ FE ⊃ BE ⊃ DB`  
> **Methodology:** UiArchitect L7 Universal Design System & Fluid Glassmorphic Matrix

---

## 1. UiArchitect L7 Architecture & Atomic UI Component Suite

The UI design system across `jFindMeWeb` is implemented via the **UiArchitect L7 standard**, codified in `js/components/UIControls.js`, `css/variables.css`, and `css/enterprise.css`. Every element is engineered as an accessible, reusable, zero-leak atomic component.

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                            UIARCHITECT L7 ATOMIC COMPONENT SUITE                                 │
├──────────────────────┬───────────────────────────────┬───────────────────────────────────────────┤
│ Component Name       │ Implementation Method         │ Functional & Technical Specifications     │
├──────────────────────┼───────────────────────────────┼───────────────────────────────────────────┤
│ 1. Toggle Switch     │ `UIControls.createSwitch`     │ Accessible `role="switch"`, ARIA-checked, │
│                      │                               │ 24px slider with smooth cubic-bezier ease │
│ 2. Segmented Toggle  │ `createSegmentedToggle`       │ Multi-state pill selector, keyboard nav,  │
│                      │                               │ active sliding background highlight pill  │
│ 3. 3D Card Flipper   │ `create3DFlipper`             │ CSS 3D perspective (1000px), rotateY(180),│
│                      │                               │ dual-face flip with preserved height      │
│ 4. ListBox Dropdown  │ `createListBoxDropdown`       │ Search-filterable popup listbox, virtual  │
│                      │                               │ scrolling, ESC key dismiss, ARIA listbox  │
│ 5. Multi-Choice Box  │ `createMultiChoiceDialog`     │ Accessible multi-select modal dialog with  │
│                      │                               │ count badges, select all / clear actions  │
│ 6. Slide-In Toast    │ `createSlideNotification`     │ Top-right slide notification, auto-expire │
│                      │                               │ (3000ms), error/warning/success states    │
│ 7. Validated Textbox │ `createValidatedInput`        │ Micro-badge validation indicators, regex   │
│                      │                               │ format checkers (RFC 5322, E.164 phone)   │
│ 8. Virtual Data Grid │ `DataGrid.js`                 │ Virtualized scrolling table, sorting,     │
│                      │                               │ dynamic column filters, pagination bar    │
│ 9. Time Scrubber     │ `HistoryTrailView.js`         │ Range slider with animated timestamp pill │
│ 10. Radar Pulse Ring │ `MapView.js`                  │ SVG pulsing concentric ring marker badge  │
└──────────────────────┴───────────────────────────────┴───────────────────────────────────────────┘
```

---

## 2. 5-Stage Universal Sizing Matrix & Viewport Scaling

Elements dynamically scale across 5 standardized size tiers utilizing CSS `clamp()`, `rem` units, and container queries:

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                             5-STAGE UNIVERSAL SIZING MATRIX (CSS / UI)                           │
├───────┬────────────┬─────────────┬─────────────┬─────────────┬───────────────────────────────────┤
│ Tier  │ Height     │ Font Size   │ Padding     │ Icon Size   │ Application Context               │
├───────┼────────────┼─────────────┼─────────────┼─────────────┼───────────────────────────────────┤
│ XS    │ 24px       │ 10px        │ 4px 8px     │ 12px        │ Table Micro-Badges, Log Tags      │
│ S     │ 32px       │ 12px        │ 6px 12px    │ 16px        │ Filter Chips, Secondary Buttons   │
│ M     │ 40px       │ 14px        │ 8px 16px    │ 20px        │ Primary Form Inputs, Nav Items    │
│ L     │ 48px       │ 16px        │ 12px 20px   │ 24px        │ Tactical Action Buttons, Headers  │
│ XL    │ 56px       │ 18px        │ 16px 24px   │ 28px        │ Emergency SOS Dispatcher, Modals  │
└───────┴────────────┴─────────────┴─────────────┴─────────────┴───────────────────────────────────┘
```

### Responsive Viewport Breakpoints:
- **Mobile Compact (XS)**: $< 640\text{px}$ — Single-column layout, drawer takes 95vw, bottom mobile tab-bar.
- **Tablet / Phablet (S)**: $640\text{px} - 768\text{px}$ — Dual-column cards, condensed sidebar.
- **Desktop Standard (M)**: $768\text{px} - 1024\text{px}$ — Split-view radar and list, docked sidebar.
- **Desktop High-Res (L)**: $1024\text{px} - 1280\text{px}$ — 3-column tactical console, full forensic terminal.
- **Ultra-Wide Tactical (XL)**: $> 1280\text{px}$ — Multi-monitor mission control, dual GIS panes.

---

## 3. Geometric Tokens, Shadows, Elevation & Glassmorphism

The visual identity relies on calibrated geometric radii and multi-layer backdrop filters:

### 3.1 Border Radii Tokens:
```css
:root {
    --radius-sm: 6px;    /* Chips, badges, tooltip popups */
    --radius-md: 10px;   /* Standard buttons, input textboxes */
    --radius-lg: 16px;   /* Tactical cards, settings containers */
    --radius-xl: 20px;   /* Modal dialogs, slide-out drawer shell */
}
```

### 3.2 Glassmorphism Elevation Stack:
- **Level 0 (Base Canvas)**: `--bg-base: #070b14` (Deep Space Navy).
- **Level 1 (Surface Panel)**: `--bg-surface: #0e1626` with `border: 1px solid rgba(255, 255, 255, 0.08)`.
- **Level 2 (Elevated Card)**: `--bg-card: rgba(23, 33, 53, 0.65)` with `backdrop-filter: blur(18px)` and `box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3)`.
- **Level 3 (Modal / Drawer)**: `--bg-surface-elevated: #131d31` with `box-shadow: 0 20px 50px rgba(0, 0, 0, 0.6)`.

---

## 4. ThemeManager & Dynamic Color Palettes

The platform features instant theme switching governed by `ThemeManager.js`, persisted in `localStorage` without layout thrashing.

### 4.1 Master Google-Style Brand Palette:
- **j (Sky Blue)**: `#4285F4`
- **B (Coral Red)**: `#EA4335`
- **u (Amber Gold)**: `#FBBC05`
- **t (Emerald Green)**: `#34A853`

### 4.2 Semantic Status Palettes:
```css
/* Dark Mode Core (Default) */
--color-emerald: #10b981;  /* RAG Active, GPS Locked, Success */
--color-amber:   #f59e0b;  /* RAG Stale, Battery Low, Warning */
--color-rose:    #ef4444;  /* RAG Inactive, SOS Alert, Fault */
--color-sky:     #38bdf8;  /* Primary Action, Selected Node */
--color-purple:  #a855f7;  /* Geofence Boundary, Cloud Sync */
```

---

## 5. Slide-Out Right Drawer Mandate (2-Column Smart-Fit)

As demonstrated in `database-sync-settings.html` (lines 49-65, 800-950), configuration inspection utilizes a dedicated slide-out right drawer:

```
┌────────────────────────────────────────────────────────┐
│ SLIDE-OUT RIGHT DRAWER MANDATE                         │
│ Width: min(640px, 92vw) | Backdrop: blur(12px)         │
├────────────────────────────────────────────────────────┤
│ [Header]: Parameter ID • Module Title • Close [X]       │
├───────────────────────────┬────────────────────────────┤
│ Left Column (Specs)       │ Right Column (Live Bridge) │
│ • Parameter Key Monospace │ • Edge SQLite Value        │
│ • Data Type & Default Val │ • Firebase Cloud Value     │
│ • RAG Sync Status Badge   │ • Bi-directional Sync Pill │
│ • Architectural Notes     │ • Immediate Overwrite Call │
├───────────────────────────┴────────────────────────────┤
│ [Footer Actions]: Force Push • Force Pull • Test ACK   │
└────────────────────────────────────────────────────────┘
```

- **Smart-Fit Folding**: When the viewport falls below 768px, the drawer dynamically stacks into a single vertical column with accordion disclosure headers.

---

## 6. Custom Visual Components & Visualizers

1. **Radar Sweep Canvas (`MapView.js`)**: Real-time rotating radial gradient sweep simulating tactical military radar sweeps at 30 RPM.
2. **D3 Cluster MindMap (`CoordinateTreeMindMapView.js`, `EagleAnalyticsTreeView.js`)**: SVG-based dynamic collapsible tree visualizing nodes, telemetry edges, and cluster densities with spring physics.
3. **Battery & Speed Circular Gauges (`DeviceCardView.js`)**: SVG stroke-dasharray circular progress rings color-coded to battery percentage (Emerald > 50%, Amber 20-50%, Rose < 20%).
4. **Pulsing SOS Alarm Pill (`AlertBannerView.js`)**: Top-anchored crimson notification bar featuring high-frequency CSS keyframe box-shadow pulses for unacknowledged emergencies.

