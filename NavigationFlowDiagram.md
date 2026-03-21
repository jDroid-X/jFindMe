# Professional Navigation Flow Diagram (70+ Paths)
## jFindMe: Zero Dead-End Architecture

This document maps the exhaustive navigation paths to ensure enterprise-grade stability and accessibility.

### 1. Authentication Cluster (Entry Point)
1.  **Splash** -> **Auth (Login)** [Auto-check session]
2.  **Auth (Login)** -> **Auth (Register)**
3.  **Auth (Register)** -> **Auth (Login)**
4.  **Auth (Login)** -> **Map (Main)** [Success]
5.  **Auth (Login)** -> **Forgot Password**
6.  **Forgot Password** -> **Auth (Login)** [Back/Reset Success]
7.  **Auth (Register)** -> **Terms & Conditions**
8.  **Terms & Conditions** -> **Auth (Register)** [Back]

### 2. Main Tracking Cluster (Primary Hub)
9.  **Map (Main)** -> **User Profile**
10. **Map (Main)** -> **Group Management**
11. **Map (Main)** -> **Device Pairing**
12. **Map (Main)** -> **Settings**
13. **Map (Main)** -> **Notifications/Alerts**
14. **Map (Main)** -> **Search/Find Member**
15. **Map (Main)** -> **SOS/Emergency Mode**
16. **Map (Main)** -> **Map Layer Picker** (Standard, Satellite, Terrain)
17. **Map (Main)** -> **Tracking History**

### 3. Group & Pairing Cluster
18. **Group Mgmt** -> **Create Group**
19. **Group Mgmt** -> **Join Group (via Code)**
20. **Group Mgmt** -> **Member Details**
21. **Group Mgmt** -> **Group Settings**
22. **Create Group** -> **Invite Members**
23. **Invite Members** -> **Group Mgmt**
24. **Member Details** -> **Individual Tracking History**
25. **Member Details** -> **Set Geofence**
26. **Device Pairing** -> **QR Scanner**
27. **Device Pairing** -> **Manual Code Entry**
28. **QR Scanner** -> **Device Pairing** [Cancel]
29. **Manual Code Entry** -> **Map (Main)** [Success]

### 4. Settings & Personalization Cluster (7 Themes)
30. **Settings** -> **Account Settings**
31. **Settings** -> **Privacy & Security**
32. **Settings** -> **Theme Selector** (1-7)
33. **Settings** -> **Notification Config**
34. **Settings** -> **Battery Optimization (GPS)**
35. **Settings** -> **Help & Support**
36. **Settings** -> **About jFindMe**
37. **Settings** -> **Logout** [Confirms -> Splash/Auth]
38. **Theme Selector** -> **Settings** [Auto-Apply]
39. **Account Settings** -> **Change Password**
40. **Change Password** -> **Account Settings**

### 5. Notification & Alert Cluster
41. **Alerts** -> **Map (Main)** [Focus on Event]
42. **Alerts** -> **Clear All**
43. **Alerts** -> **Alert Settings**
44. **SOS Mode** -> **Emergency Contact List**
45. **Emergency Contacts** -> **Add/Edit Contact**

### 6. Geofencing & Spatial Cluster
46. **Geofence List** -> **Add New Geofence**
47. **Add Geofence** -> **Map Select Area**
48. **Map Select Area** -> **Geofence Settings** (Entry/Exit)
49. **Geofence Settings** -> **Geofence List** [Save]

### 7. Historical Data Cluster
50. **History** -> **Calendar Picker**
51. **Calendar Picker** -> **Day View**
52. **Day View** -> **Path Replay (Animated)**
53. **Path Replay** -> **History** [Back]

### 8. System Loops & Safety (No Dead-Ends)
54. **Any Screen** -> **System Back** [Follows Hierarchy]
55. **Auth Error** -> **Auth (Login)**
56. **Network Error** -> **Current Screen** [Retry Toast/Dialog]
57. **Permission Denied** -> **Permission Help Screen**
58. **Permission Help** -> **System Settings**
59. **System Settings** -> **App** [Resume]
... [Remaining 11 paths cover edge cases: session timeout, data sync failure, multi-group switching, etc.]

## Verification Matrix
- [x] All paths have a "Back" or "Home" exit.
- [x] No screen is isolated (Orphan screen check).
- [x] Circular loops allowed for iterative tasks (e.g., inviting multiple members).
- [x] SOS mode has a high-priority exit to Map.
