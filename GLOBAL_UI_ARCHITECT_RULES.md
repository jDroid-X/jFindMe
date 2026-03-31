# Global UI Architect Design Rules (L7 Enterprise Standard)

"Build/Update the [Screen Name] following the Global UI Architect L7 Design Standard.

1. Structural Hierarchy:
Use a Waterfall MVVM architecture.
The root must be a FrameLayout containing a ScrollView to ensure Stage 1-5 responsiveness.
Organize content into Enterprise Integrity Cards with 30f corner radii and 12f elevation.

2. Component Format:
Every input must use the Label-and-Box format: A bold descriptive label above a themed wrapEnterpriseField box.
Mandatory fields must have a Red (*) in both the Label and the Hint using Html.fromHtml.
Display lables inside the text box if not minetiond outside.

3. Visual Aesthetics:
- Use Universal Elevation: EVERY Card, Tile, and Button MUST have visible shadow depth (Elevation 8f-25f). NEVER leave backgrounds flat or shadowless.
 
- Apply Glassmorphism (15% white overlay) to secondary containers.
- Use a Two-Point Linear Gradient (left-to-right) for all primary Action Buttons and Section Headers.
- All buttons must be Capsule Geometry (50f radius) with high shadow depth (25f elevation).



4. Interaction Hooks:

Standardize the Tab Sequence for all fields from top to bottom.
Enable D-Pad/Enter key support for the primary CTA (IME_ACTION_DONE).
Trigger a Non-Cancelable Installation Guide Popup if mandatory fields are detected as blank on page open.
5. Navigation:

Group secondary settings into a Horizontal Tile Grid and place the Primary Master Action at the very bottom with massive vertical breathing room (100dp)."

## 1. Architectural Foundation: L7 2D Hierarchy
- **Layer 1: The Matrix**: The fundamental background layer, typically utilizing a dynamic gradient axis (Left-Right) with HSL-tailored colors.
- **Layer 2: The Glass Stack**: Semi-transparent, blur-enabled containers (Glassmorphism) for transient information and secondary actions.
- **Layer 3: The Enterprise Cards**: Solid geometric containers with `16f` to `30f` corner radii and `elevation >= 8f`.
- **Layer 4: Interactive Handlers**: Buttons and inputs with `50f` radii (Capsule Geometry) and strict focus state indicators.

## 2. Universal Scaling Engine (5-Stage Model)
- **Stage 1 (Mobile)**: Compact paddings (`15dp`), single-column layouts, and `14sp` primary text.
- **Stage 2 (Large Mobile/Foldable)**: Increased horizontal margins (`25dp`), standard `16sp` text.
- **Stage 3 (Tablet)**: Dual-pane potential, increased elevation (`15f`), and `20sp` headers.
- **Stage 4 (Desktop/Web Broadcast)**: Fixed-width center alignment, large geometric tiles.
- **Stage 5 (High Fidelity/TV)**: D-Pad prioritized, high contrast, and `32sp+` display headings.

- **Unified Screen Padding Matrix (Device Targets):**
    | Design Stage | Target Device Profile | Screen Padding (dp) |
    | :--- | :--- | :--- |
    | **LIBERAL** | Tablets / Large Displays | 25dp |
    | **STANDARD** | Standard Sized Smartphones | 20dp |
    | **COMPACT** | High-Density Phablets | 15dp |
    | **SLIM** | Small/Legacy Smartphones | 10dp |
    | **ULTRA_SLIM** | Micro Screens / Specialized HW | 5dp |

- **Unified Screen Padding Matrix (UI Parameters):**
    | Design Stage | Screen Padding (dp) | Header Top Gap (dp) | Tile Gap (dp) | Text Scale Factor |
    | :--- | :--- | :--- | :--- | :--- |
    | **LIBERAL** | 25dp | 30dp | 25dp | 1.6x |
    | **STANDARD** | 20dp | 20dp | 20dp | 1.3x |
    | **COMPACT** | 15dp | 15dp | 15dp | 1.0x |
    | **SLIM** | 10dp | 10dp | 10dp | 0.8x |
    | **ULTRA_SLIM** | 5dp | 5dp | 5dp | 0.65x |

## 3. Thematic Visual Rules
- **Gradient Palette**: Avoid flat colors. Use a two-point linear gradient (Start -> End) for all CTAs and active headers.
- **Micro-Animations**: All state transitions (Loading, Success, Error) must use the `AngelLoader` sequence with status text updates.
- **Shadow Physics**: Use consistent elevation levels to imply depth. Floating action buttons must have `15f+` elevation.
- **Content Geometry**: No 90-degree corners. Minimum corner radius is `16f`.

## 4. Mandatory Identity Handshake (Security UI)
- **Mandatory Fields**: Every required input must have a red asterisk `(*)` in BOTH the label and the hint.
- **Label Sequence**: Titles must be positioned above the field, bolded, and match the enterprise nomenclature (e.g., "Zip Code" instead of "Pincode").
- **Verification Popups**: If critical data (Zip Code, Name) is missing, a non-cancelable installation guide must intercept the user.

## 5. Global Accessibility & Interaction
- **Tab Sequence**: Every page must maintain a deterministic focus order from top-left to bottom-right.
- **Enter Action**: The `IME_ACTION_DONE` or `Enter` key must trigger the primary page action (LOGIN, SAVE, SEARCH).
- **Haptic Integrity**: All touch interactions must provide feedback (vibration or visual ripple).

## 6. Iconography & Symbolism
- **Thematic Consistency**: Use standard Android/Google Material icons but tinted to match the `getGradientStart()` theme color.
- **Scale-Independent Graphics**: All icons must use a minimum `40x40` touch target.

### **7. Structural Hierarchy Matrices (Dynamic Scaling)**

- **Iconography Scaling Matrix (Vitals):**
    | Icon Tier | Base Size (Compact) | Liberal (1.6x) | Slim (0.8x) | Ultra (0.65x) |
    | :--- | :--- | :--- | :--- | :--- |
    | **ICON_TINY** | 90px | 144px | 72px | 58px |
    | **ICON_SMALL** | 110px | 176px | 88px | 71px |
    | **ICON_STANDARD** | 140px | 224px | 112px | 91px |
    | **ICON_LARGE** | 180px | 288px | 144px | 117px |
    | **ICON_HERO** | 250px | 400px | 200px | 162px |

- **Shadow Elevation Matrix (Depth):**
    | Depth Tier | Base (Ultra 1.0x) | Compact (1.4x) | Standard (1.6x) | Liberal (2.0x) |
    | :--- | :--- | :--- | :--- | :--- |
    | **SHADOW_LOW** | 4f | 6f | 7f | 8f |
    | **SHADOW_MEDIUM** | 12f | 17f | 19f | 24f |
    | **SHADOW_HIGH** | 25f | 35f | 40f | 50f |

- **Corner Radius Hierarchy (Curvature):**
    | Radius Tier | Base (Compact 1.0x) | Liberal (2.0x) | Slim (0.6x) | Ultra (0.4x) |
    | :--- | :--- | :--- | :--- | :--- |
    | **RADIUS_TIER_1** | 25f | 50f | 15f | 10f |
    | **RADIUS_TIER_2** | 50f | 100f | 30f | 20f |
    | **RADIUS_TIER_3** | 100f | 200f | 60f | 40f |

---
*Verified by the Global UI Architect - Stage 5 Validation Complete.*
