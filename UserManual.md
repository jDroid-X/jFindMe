# jFindMe - Enterprise User Manual

## Version 1.0 | Enterprise Mobile System Architecture

Welcome to **jFindMe**, a professional-grade real-time location tracking system designed for multi-user synchronization, security, and high-performance spatial awareness.

---

### 1. System Overview (Holistic)

jFindMe utilizes a cutting-edge hybrid architecture:

- **UI Engine**: 100% Java Programmatic Layouts for maximum performance.
- **Tracking Engine**: High-precision GPS with adaptive 3-second refresh rates.
- **Data Engine**: Python-powered spatial analysis and database sanitization.
- **Visuals**: 7 High-contrast enterprise themes and dual-pane landscape support.

---

### 2. Getting Started (Onboarding)

#### 2.1 Authentication Screen (AuthActivity)

- **Login/Register**: Enter your enterprise credentials.
- **Icons as Buttons**: Use the "Arrow" icon to proceed or the "Lock" icon for password recovery.
- **Theme Selection**: Access the theme picker via the "Palette" icon to select one of the 7 high-contrast color schemes.

---

### 3. Main Tracking Interface (MapActivity)

#### 3.1 The Map Hub

- **Real-time Markers**: View up to 50 active members with smooth avatar interpolation (≤1s latency).
- **Marker Clustering**: Grouped icons appear when zoomed out to maintain map clarity.
- **Layer Control**: Toggle between Standard, Satellite, and Terrain views using the "Layer" icon.

#### 3.2 Landscape Mode (Dual-Pane)

- **Split Screen**: In landscape, the screen automatically divides into **Map (Left)** and **Control Panel (Right)**.
- **Screen Toggles**:
  - Click the **"Single Square"** icon to expand the map to full screen.
  - Click the **"Split Square"** icon to restore the dual-pane view.

---

### 4. Safety & Emergency Features

#### 4.1 SOS Alerts

- **Immediate Action**: Press and hold the **"Red Alert"** icon in the control panel to broadcast an SOS to all group members.
- **Visual Feedback**: Your marker will pulse red on all connected devices.

#### 4.2 Geofencing

- **Spatial Bounds**: Define "Safe Zones" in settings. You will receive a high-priority notification if a member enters or exits these bounds.

---

### 5. Group Management & Pairing

#### 5.1 Device Pairing

- **Unique ID**: Every device is assigned a pairing code.
- **Joining Groups**: Enter a group code provided by an administrator via the **"Add Member"** icon.
- **Multi-User Tracking**: View all members of your active group in the "Group List" pane.

---

### 6. Privacy & Dossier Management (ACL)

#### 6.1 Granular Permissions Matrix

The System uses the **L7 Access Control List (ACL)** to manage sensitive data. Checkboxes in the Profile Details screen allow you to toggle specific data flows.

| Permission State | Pros of Selection (Granted) | Cons of De-Selection (Denied) |
| :--- | :--- | :--- |
| **ENABLED (✅)** | Full enterprise recovery, real-time safety tracking, remote administration, and automated cloud sync. | Slightly higher data usage; sensitive records (Biometrics/Medical) are encrypted on the cloud hub. |
| **DISABLED (❌)** | **Pure Local Privacy (GDPR)**. Sensitive dossiers stay strictly on your physical device. | No remote recovery if device is lost; emergency responders cannot access medical protocols remotely. |

#### 6.2 Key Permissions Definitions

- **Biometric Identity Lock**: Fingerprint/Face signatures for secure node handshaking.
- **Administrative Overrides**: Grant global administrative permissions to modify profile settings remotely.
- **Stealth Oversite Logic**: Allow the system to override 'Ghost Mode' when an emergency protocol is triggered.

---

### 7. Personalization & Settings

#### 7.1 Enterprise Themes

Choose from 7 professional combinations:

1. **Enterprise Blue** (Default)
2. **Forest Green**
3. **Deep Orange**
4. **Midnight Purple**
5. **Charcoal Grey**
6. **Ocean Teal**
7. **Burgundy Red**

*All themes feature AAA-standard high-contrast text for visibility in all lighting conditions.*

---

### 8. Technical Support

For data-related queries or synchronization issues, the system utilizes an automated **Python Data Sanitizer** to resolve conflicts. Check the "System Status" in Settings for real-time health checks.
