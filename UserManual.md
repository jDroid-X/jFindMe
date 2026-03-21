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

### 6. Personalization & Settings
#### 6.1 Enterprise Themes
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

### 7. Technical Support
For data-related queries or synchronization issues, the system utilizes an automated **Python Data Sanitizer** to resolve conflicts. Check the "System Status" in Settings for real-time health checks.
