# jButterfly - Visual Architecture Map

## System Data Flow

```mermaid
graph TD
    A[🖥️ Browser: index.html] --> B{Auth Check}
    B -->|Auto-Bypass| C[App Bootstrap]
    B -->|Manual Login| D[Master Key Entry]
    
    C --> E[Firebase Bridge Service]
    D --> E
    
    E --> F[Backend Proxy: server.js]
    F --> G[Firebase RTDB]
    F --> H[Firebase Firestore]
    
    G --> I[Devices Collection]
    G --> J[SafeZones Collection]
    G --> K[Alerts Collection]
    
    E --> L[MapController]
    L --> M[MapView Facade]
    M --> N[Leaflet.js]
    M --> O[Google Maps]
    
    L --> P[DeviceController]
    P --> Q[DeviceModel Array]
    
    L --> R[SafeZoneController]
    R --> S[SafeZoneModel Array]
    
    P --> T[DashboardView]
    P --> U[DeviceUserView]
    P --> V[GeofencingView]
    P --> W[EagleAnalyticsView]
```

---

## Component Interaction Map

```mermaid
sequenceDiagram
    participant User
    participant App
    participant MapCtrl
    participant DevCtrl
    participant Firebase
    
    User->>App: Open http://127.0.0.1:3000
    App->>App: Auth.autoLogin()
    App->>Firebase: GET /api/discovery
    Firebase-->>App: {projectId, collections}
    App->>MapCtrl: init()
    MapCtrl->>Firebase: GET /api/devices-live
    Firebase-->>MapCtrl: [devices[]]
    MapCtrl->>DevCtrl: subscribe(callback)
    DevCtrl->>MapCtrl: notify(devices)
    MapCtrl->>User: Render map markers
    MapCtrl->>User: Update sidebar list
```

---

## Navigation Flow

```mermaid
graph LR
    A[Login Screen] --> B[Live Map Radar]
    B --> C[Dashboard]
    B --> D[Device User]
    B --> E[Geofencing]
    B --> F[Database Architect]
    
    D --> G[Edit Modal]
    E --> H[Create Zone Modal]
    F --> I[Data Grid]
```

---

## Data Entities

```mermaid
erDiagram
    DEVICE ||--o{ SAFE_ZONE : belongs_to
    DEVICE {
        string agentId PK
        string name
        string role
        float latitude
        float longitude
        int battery
        bool isOnline
        timestamp lastSeen
    }
    SAFE_ZONE {
        string zoneId PK
        string zoneName
        float latitude
        float longitude
        float radius
        array polygonPoints
    }
    ALERT {
        string alertId PK
        string deviceId FK
        string zoneId FK
        string eventType
        timestamp timestamp
    }
```

---

## Technology Stack

```mermaid
graph TB
    subgraph Frontend
        HTML[HTML5/CSS3]
        JS[Vanilla JavaScript ES6+]
        Tailwind[Tailwind CSS CDN]
        Leaflet[Leaflet.js 1.9.4]
    end
    
    subgraph Backend
        Node[Node.js Express]
        CORS[CORS Middleware]
    end
    
    subgraph Cloud
        Firebase[Firebase Platform]
        RTDB[(Realtime DB)]
        Auth[Auth Service]
    end
    
    Frontend -->|Fetch API| Backend
    Backend -->|Admin SDK| Firebase
    Firebase --> RTDB
```

---

## View Responsibilities

| View | Controller | Data Source | Updates Via |
|------|-----------|-------------|-------------|
| LiveMap | MapController | DeviceController | Polling (5s) |
| Dashboard | App | DeviceController | Subscribe |
| DeviceUser | App | DeviceController + SafeZoneController | Subscribe |
| Geofencing | App | SafeZoneController + DeviceController | Subscribe |
| EagleAnalytics | App | DeviceController | Subscribe |

---

## Authentication Flow (Current vs Recommended)

```mermaid
graph TD
    subgraph CURRENT_IMPLEMENTATION
        A1[Page Load] --> B1[Auto-login Bypass]
        B1 --> C1[Hardcoded Key 'Jeet@123Meet']
        C1 --> D1[Session Stored in localStorage]
    end
    
    subgraph RECOMMENDED_IMPLEMENTATION
        A2[Page Load] --> B2[Firebase Auth Check]
        B2 --> C2{User Logged In?}
        C2 -->|Yes| D2[Show Dashboard]
        C2 -->|No| E2[Email/Password Form]
        E2 --> F2[Firebase signInWithEmailAndPassword]
        F2 --> G2[User Token in Session]
    end
```

---

## Deployment Architecture

```mermaid
graph LR
    Client[Browsers] --> LB[Load Balancer]
    LB --> Server1[Node.js Server 1]
    LB --> Server2[Node.js Server 2]
    
    Server1 --> Firebase[(Firebase Cloud)]
    Server2 --> Firebase
    
    style Client fill:#e1f5fe
    style Firebase fill:#fff3e0
```

---

## Error Handling Cascade

```mermaid
flowchart TD
    A[API Request] --> B{Success?}
    B -->|Yes| C[Update UI]
    B -->|No| D{Error Type}
    
    D -->|Network| E[Retry with Backoff]
    D -->|Auth| F[Clear Session → Re-auth]
    D -->|404| G[Log Warning]
    D -->|500| H[Show Toast + Alert Admin]
    
    E --> I{Attempts < 3?}
    I -->|Yes| A
    I -->|No| J[Show Offline Mode]
```
