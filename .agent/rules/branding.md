# jFindMe & jButterFly Brand Identity & Design System (V3.0 Enterprise Standard)

> **Mandatory Reference Pipeline**: `Workflow (.agent/rules/workflow.md)` ➔ `Rules (.agent/rules/rules.md)` ➔ `Agents (.agent/rules/agents.md)` ➔ `Skills (.agent/rules/skills.md)` ➔ `Branding (.agent/rules/branding.md)`

---

## 1. Product Identity & Dual-Module Architecture

- **Primary Product Name**: `jFindMe`
- **Twin Module / Stealth Engine**: `jButterFly` (Butterfly Point / Stealth Family Tracking / Mesh / Device Gateway)
- **Architectural Parity Mandate**:
  - The application exists as a dual-module project: `app` (Standard edition) and `jButterFly` (Stealth/mesh-hardened twin).
  - All branding, layouts, drawables, color tokens, and activity structures must maintain **100% exact parity** across both modules. Zero drift is permitted.

---

## 2. Visual Aesthetic Foundation: L7 Enterprise UI Standard (`UiArchitect`)

All visual presentation across `jFindMe` and `jButterFly` follows the **L7 2D Hierarchy Standard**:

### Layer 1: The Matrix (Background Dynamics)
- Never use plain flat single-color backgrounds.
- Backgrounds utilize dynamic HSL-tailored gradient canvases (Left-to-Right 2-point linear gradient) or subtle layered surface depth.

### Layer 2: The Glass Stack (Glassmorphism)
- Secondary containers, notification banners, floating radar hud panels, and dialog backgrounds utilize Glassmorphism:
  - 15% white translucent overlay (`#26FFFFFF` or `#1A000000` depending on theme dark/light mode).
  - Background blur and subtle surface stroke (`1dp` translucent border).

### Layer 3: Enterprise Integrity Cards
- Core interactive sections, settings tiles, and device cards are housed in **Enterprise Integrity Cards**:
  - Corner Radii: `16f` to `30f` (Zero 90-degree corners anywhere in the application).
  - Universal Elevation: **Every Card, Tile, and Container MUST have visible shadow depth (Elevation 8f to 25f)**. Flat, shadowless cards are strictly forbidden.

### Layer 4: Interactive Handlers & Capsule Geometry
- Primary CTA Buttons: **Capsule Geometry (`50f` radius)** with high shadow depth (`25f` elevation).
- Two-point linear gradient fill: `getGradientStart()` ➔ `getGradientEnd()`.
- Interactive ripples and micro-haptic feedback on all tap targets.

---

## 3. Dynamic 12-Theme Color Palette (`ThemeManager.THEMES`)

The system provides 12 distinct, WCAG AA/AAA-compliant theme palettes accessible via `ThemeManager`. All UI components must read colors dynamically from the active theme and never hardcode static color hexes.

| ID | Theme Name | Palette Type | Surface Background | Primary Text | Action Gradient (Start ➔ End) |
|:---|:---|:---|:---|:---|:---|
| **0** | **Coral Sea Teal** | Light | `#FDF6F4` | `#1E293B` | `#F43F5E` (Rose) ➔ `#0D9488` (Teal) |
| **1** | **Nordic Obsidian** | Dark | `#080C14` | `#F0F6FC` | `#1E40AF` (Deep Blue) ➔ `#3B82F6` (Electric Blue) |
| **2** | **Amber Pine** | Light | `#FBF8F2` | `#1F2937` | `#D97706` (Amber) ➔ `#059669` (Pine Green) |
| **3** | **Sunrise Terracotta** | Light | `#F8EDE5` | `#431407` | `#EA580C` (Terracotta) ➔ `#F59E0B` (Amber) |
| **4** | **Teal & Umber** | Light | `#F7F3EE` | `#27170E` | `#78350F` (Umber) ➔ `#0F766E` (Dark Teal) |
| **5** | **Mint & Mocha** | Light | `#F3F7F4` | `#1C1917` | `#44403C` (Mocha) ➔ `#10B981` (Mint Green) |
| **6** | **Celadon Sage** | Light | `#EBF2EB` | `#0D2818` | `#15803D` (Sage) ➔ `#166534` (Deep Forest) |
| **7** | **Golden Hour Dark** | Dark | `#0C0A07` | `#FEF3C7` | `#B45309` (Warm Bronze) ➔ `#F59E0B` (Amber Glow) |
| **8** | **Midnight Teal Dark** | Dark | `#061017` | `#E0F2FE` | `#0369A1` (Ocean) ➔ `#0D9488` (Teal) |
| **9** | **Crimson Coral Dark** | Dark | `#12080A` | `#FFE4E6` | `#BE123C` (Crimson) ➔ `#FB7185` (Coral Rose) |
| **10**| **Earthen Clay** | Light | `#F3EBE3` | `#3E1A0A` | `#9A3412` (Clay) ➔ `#D97706` (Warm Ochre) |
| **11**| **Silver Slate** | Light | `#E2E8F0` | `#0F172A` | `#475569` (Slate) ➔ `#2563EB` (Cobalt) |

---

## 4. Standardized Centralized RAG Status Engine

Telemetry freshness, network connectivity, and device health are communicated using the **Unified Centralized RAG Status Engine**:

| RAG Status | Color Name | Hex Code | Telemetry Condition | Visual Treatment |
|:---|:---|:---|:---|:---|
| 🟢 **ACTIVE (LIVE)** | Emerald Green | `#22C55E` | Telemetry `< 15 min` | Solid glowing emerald dot, green role border |
| 🟡 **INACTIVE (< 6H)** | Amber Orange | `#F59E0B` | Telemetry `15m – 6h` OR Battery $\le 20\%$ | Solid amber dot, warning badge |
| 🔴 **INACTIVE (> 6H)** | Rose Red | `#EF4444` | Telemetry `> 6 hours` | Solid rose red dot, offline indicator |
| 🚨 **EMERGENCY SOS** | Pulsing Crimson | `#EF4444` | `isSos == true` | 1.5Hz pulsing crimson aura, urgent banner |
| ⚪ **UNLINKED / GHOST** | Slate Gray | `#64748B` | Ghost Mode, unlinked, or deleted | Neutral muted dot, opacity 50% |

---

## 5. Tactical Live Map Marker Branding Standard

To ensure 60fps GPU-accelerated rendering across thousands of spatial points, map markers follow the **Lightweight Marker Standard**:

1. **Top Name Callout Pill**:
   - Dynamic Role Border Color (e.g. Ward = `#3B82F6`, Parent = `#10B981`, Friend = `#F59E0B`).
   - Text color matches the device's real-time RAG status (Emerald Green for live, Amber for idle, Rose for offline).
2. **Direct Avatar Bubble**:
   - Circular bitmap avatar with crisp 2dp theme border.
   - Isolated disk cache keying (`REMOTE_HQ_{devId}_{avId}`).
3. **Live Status Dot**:
   - Small circular RAG dot pinned to bottom-right of avatar (`#22C55E`, `#F59E0B`, `#EF4444`).
4. **Motion Heading Chevron**:
   - Directional chevron rotating dynamically with GPS bearing.
5. **Strict Prohibitions**:
   - **NO heavy canvas blur/glow filters directly on the map viewport**.
   - **NO oversized battery pin badges on the map surface**; battery, GPS accuracy, and MCC/MNC cellular vitals belong in the sliding bottom sheet and radar dialogs.
6. **Floating Distant Device Panel**:
   - Devices strictly $> 50\text{ m}$ away appear in the top-left floating panel (`@+id/floating_device_panel`) as clean circular avatars (zero clutter text). Tapping an avatar smoothly pans camera to target.

---

## 6. Form Fields & Input Branding Standard

1. **Format**: Every input field must use the **Label-and-Box format**:
   - Bold descriptive label positioned above the field.
   - Rounded wrapEnterpriseField input box with subtle theme outline and focus glow.
2. **Mandatory Identity Handshake**:
   - Every required input MUST display a **Red Asterisk `(*)`** in both the **Label** and the **Hint** using `Html.fromHtml("<font color='#EF4444'>*</font>")`.
3. **Nomenclature SSOT**:
   - Use enterprise standard terminology: e.g., `"Zip Code"` (never `"Pincode"`), `"Safe Zone"` (never `"Fence"`), `"Hardware Agent ID"` (never `"User ID"`).
4. **Validation Popups**:
   - Missing mandatory fields on page load trigger a non-cancelable installation guide banner/dialog interceptor.

---

## 7. Universal 5-Stage Scaling Matrix

Layouts dynamically adapt to screen density and form factor via the **Universal 5-Stage Scaling Engine**:

| Design Stage | Target Device Profile | Screen Padding | Header Gap | Tile Gap | Text Scale | Shadow Elev. |
|:---|:---|:---|:---|:---|:---|:---|
| **LIBERAL** | Tablets / Large Foldables ($\ge 600\text{dp}$) | `25dp` | `30dp` | `25dp` | 1.4x | High (`25f – 50f`) |
| **STANDARD** | Standard Sized Smartphones ($400 - 599\text{dp}$) | `20dp` | `20dp` | `20dp` | 1.2x | High (`20f – 40f`) |
| **COMPACT** | High-Density Phablets ($360 - 399\text{dp}$) | `15dp` | `15dp` | `15dp` | 1.0x | Medium (`15f – 30f`) |
| **SLIM** | Small / Legacy Phones ($320 - 359\text{dp}$) | `10dp` | `10dp` | `10dp` | 0.9x | Medium (`10f – 20f`) |
| **ULTRA_SLIM** | Micro Screens / Specialized Wearables ($< 320\text{dp}$) | `5dp` | `5dp` | `5dp` | 0.8x | Low (`4f – 8f`) |

---

## 8. Operator & Assistant Identity Branding

- **Assistant Codename**: `jDroid-X`
- **Lead Operator**: `JEET`
- **Closed Circuit Mandate**: Direct, two-party line between `JEET` and `jDroid-X`.
- **Mandatory Transmission Header**:
  ```
  -- made by jDroid-X--
  ```
  Every response from the agent must start with `-- made by jDroid-X--` on its own first line.
- **Internal Anti-Drift Breaker**:
  `"beep beep beep... hehe... Let me fix!"`
- **Production Standard**:
  - 20-Year Senior Architect / Principal Developer quality.
  - Full runnable builds, real production APIs, zero hedges, zero partial stubs, zero speculative crawling.
  - Prime Directives strictly enforced across all operations.
