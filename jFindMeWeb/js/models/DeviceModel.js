import { GeoUtils } from '../core/GeoUtils.js';
import { AvatarService } from '../services/AvatarService.js';
import { SanitizerService } from '../services/SanitizerService.js';

/**
 * jFindMe Matrix - Device Entity Model (OOPS Architecture)
 * Encapsulates device identity, location, hardware telemetry, and UI presentation states
 */
export class DeviceModel {
    constructor(data = {}) {
        this.raw = data.raw || data || {};
        const raw = this.raw;

        // ==========================================
        // TIER 1: MASTER DATA (Immutable Identity & Silicon Anchor)
        // ==========================================
        this.agentId = SanitizerService.sanitizeHardwareId(data.agentId || raw.agentId || raw.agent_id || "");
        this.deviceId = data.deviceId || raw.deviceId || raw.device_id || `DEV_${this.agentId}`;
        this.authUid = raw.Identity?.auth_uid || raw.auth_uid || raw.device?.live?.auth_uid || "";
        this.ownerUid = raw.Identity?.owner_uid || raw.owner_uid || raw.profile?.owner_uid || "";
        this.activeGroup = raw.Identity?.active_group || raw.active_group || raw.profile?.group || "Default Circle";
        this.connectedId = SanitizerService.sanitizeConnectedId(raw.Identity?.connected_id || raw.connected_id, this.activeGroup);

        // Hardware Silicon Specifications (Brand + Model + Board + Hardware + Device + IMEI 1/2)
        const hwProps = raw.Hardware || raw.Identity?.Hardware || raw.hardwareProperties || (typeof raw.device?.hardware === 'object' ? raw.device?.hardware : {}) || {};
        this.brand = hwProps.brand || raw.brand || raw.device?.brand || "Android";
        this.model = hwProps.model || raw.model || raw.device?.model || data.model || data.deviceName || "Device";
        this.board = hwProps.board || raw.board || raw.device?.board || "universal";
        this.hardware = hwProps.hardware || raw.hardware || (typeof raw.device?.hardware === 'string' ? raw.device.hardware : "qcom");
        const rawDevNode = hwProps.device || (typeof raw.device === 'string' ? raw.device : (raw.device?.device_name || raw.device?.name || raw.device?.model || raw.product_device || ""));
        this.device = (typeof rawDevNode === 'string' && rawDevNode && rawDevNode !== '[object Object]') ? rawDevNode : "generic_node";

        // Deep Dual-SIM & Cellular Telemetry Resolution (Extracts from raw.device, raw.profile, raw.Hardware, raw.telemetry)
        const devObj = (raw.device && typeof raw.device === 'object') ? raw.device : {};
        const rawProf = raw.profile || raw.Profile || {};
        const simDossier = rawProf.sim_dossier || raw.telemetry?.sim_dossier || null;
        this.simDossier = simDossier;
        const rawImei1 = hwProps.imei1 || raw.imei1 || devObj.imei_1 || devObj.imei1 || devObj.imei || hwProps.imei || raw.imei || "";
        const rawImei2 = hwProps.imei2 || raw.imei2 || devObj.imei_2 || devObj.imei2 || "";
        const isDualSim = Boolean(devObj.is_dual_sim || raw.is_dual_sim || hwProps.is_dual_sim || hwProps.isDualSim);
        let userPhone = rawProf.phone || rawProf.userPhone || data.phone || devObj.phone || devObj.sim_number || "";
        if (!userPhone && simDossier?.slot_0?.phone && simDossier.slot_0.phone !== "Not Detected") {
            userPhone = simDossier.slot_0.phone;
        }
        let carrier = devObj.carrier || raw.carrier || rawProf.carrier || raw.telemetry?.carrier || data.carrier || "";
        if (!carrier && simDossier?.slot_0?.carrier) {
            carrier = simDossier.slot_0.carrier;
        }
        const networkPreference = devObj.network_preference || raw.network_preference || rawProf.network_preference || data.networkPreference || "AUTO_ANY_ACTIVE";

        // Format IMEI 1 with smart detection & privacy protection awareness (Android 10+ privacy policy)
        if (rawImei1 && rawImei1 !== "PERMISSION_REQUIRED" && rawImei1 !== "RESTRICTED_API29+" && rawImei1 !== "UNAVAILABLE" && rawImei1 !== "Not Detected") {
            this.imei1 = rawImei1;
        } else if (rawImei1 === "PERMISSION_REQUIRED" || rawImei1 === "RESTRICTED_API29+") {
            this.imei1 = "Protected (Android 10+)";
        } else if (userPhone || isDualSim) {
            this.imei1 = "Active (SIM Detected)";
        } else {
            this.imei1 = "Not Detected";
        }

        // Format IMEI 2 with dual SIM awareness
        if (rawImei2 && rawImei2 !== "PERMISSION_REQUIRED" && rawImei2 !== "RESTRICTED_API29+" && rawImei2 !== "UNAVAILABLE" && !rawImei2.includes("Not Detected")) {
            this.imei2 = rawImei2;
        } else if (isDualSim) {
            if (rawImei2 === "PERMISSION_REQUIRED" || rawImei2 === "RESTRICTED_API29+") {
                this.imei2 = "Slot 2 (OS Protected)";
            } else {
                this.imei2 = "Slot 2 Active";
            }
        } else {
            this.imei2 = "Not Detected (Single SIM)";
        }

        this.carrier = carrier;
        this.networkPreference = networkPreference;
        this.isDualSim = isDualSim;
        this.hasSim = Boolean(userPhone || isDualSim || rawImei1 === "PERMISSION_REQUIRED" || rawImei1 === "RESTRICTED_API29+");
        const carrierTag = carrier ? ` (${carrier})` : '';
        this.simStatus = this.hasSim
            ? (isDualSim ? `Dual SIM Active${carrierTag}${userPhone ? ` • ${userPhone}` : ''}` : `SIM Active${carrierTag}${userPhone ? ` • ${userPhone}` : ''}`)
            : "No SIM Detected";
        this.simPhone = userPhone;
        this.siliconHash = hwProps.siliconHash || raw.siliconHash || data.siliconHash || this._generateSiliconSignature();

        // System App Package & Build Versioning (Clean SSOT Extraction strictly per-device)
        const parsedVersion = DeviceModel.parseVersion(raw, data);
        this.versionName = parsedVersion.versionName;
        this.versionCode = parsedVersion.versionCode;
        this.appVersion = `V${this.versionName} (Build ${this.versionCode})`;
        this.packageName = raw.device?.package_name || data.packageName || "com.jdroidx.jfindme";
        this.packageSizeFormatted = data.packageSizeFormatted || raw.Version_Update?.latest?.package_size_formatted || raw.package_size_formatted || "80.6 MB";
        this.databaseSchema = data.databaseSchema || raw.Version_Update?.latest?.database_schema || raw.database_schema || "V75";
        this.otaStatus = data.otaStatus || raw.Version_Update?.latest?.status || raw.status || "INSTALLED";
        this.otaPendingPush = data.otaPendingPush || raw.Version_Update?.pending_push || null;

        // ==========================================
        // TIER 2: SUB-MASTER DATA (Profile, Persona, Circles & ACL Access)
        // ==========================================
        const prof = raw.profile || raw.Profile || {};
        const rawName = data.name || prof.name || prof.fullName || prof.userName || prof.display_name || prof.owner_name || raw.name || data.agentId || "Unknown Agent";
        this.name = SanitizerService.sanitizeDeviceName(rawName, this.model, this.agentId);
        const rawDevName = data.deviceName || prof.deviceName || prof.device_name || devObj.device_name || devObj.deviceName || raw.device_name || this.model || "Android Device";
        this.deviceName = SanitizerService.sanitizeDeviceName(rawDevName, this.model, this.agentId);
        this.hostName = data.hostName || prof.hostName || prof.fullName || prof.name || this.name;
        const rawIdentity = raw.identity || raw.Identity || {};
        this.avatarId = data.avatarId || data.avatar_id || prof.avatarId || prof.avatar_id || prof.equipped_avatar_id || rawIdentity.avatar_id || rawIdentity.avatarId || "butterfly";
        this.avatarData = data.avatarData || prof.avatar_data || prof.avatarData || raw.avatar_data || (raw.avatar_sync ? raw.avatar_sync.avatar_data : "") || "";
        this.avatarUrl = data.avatarUrl || prof.avatar_url || prof.avatarUrl || (this.avatarData ? (this.avatarData.startsWith("data:image/") ? this.avatarData : `data:image/jpeg;base64,${this.avatarData}`) : AvatarService.getAvatarUrl(this.avatarId, this.name, this.avatarData));
        this.email = prof.email || prof.email_id || data.email || "";
        this.phone = prof.phone || prof.userPhone || data.phone || devObj.phone || devObj.sim_number || userPhone || "";
        this.dob = prof.dob || raw.dob || "";
        this.gender = prof.gender || raw.gender || "";
        this.role = SanitizerService.sanitizeRole(prof.role || data.role, "MEMBER");
        this.roleFamily = prof.role_family || prof.roleFamily || data.role_family || "None";
        this.roleFriend = prof.role_friend || prof.roleFriend || data.role_friend || "None";
        this.roleTitle = prof.role_title || prof.roleTitle || data.roleTitle || this.role;
        this.department = prof.department || prof.dept_name || prof.org_name || data.department || "General";
        this.emergencyNote = prof.emergencyNote || prof.emergency_note || prof.note || data.emergencyNote || "";
        // [Variables]: relationships | [Constants]: None | [Dependencies]: None | [Integration Points]: Relational SSOT & Circles
        this.safeZonesCount = parseInt(data.safeZonesCount || Object.keys(raw.safe_zones || raw.SafeZones || {}).length || 0);
        this.relationships = raw.relationships || data.relationships || {};
        this.relationshipsCount = parseInt(data.relationshipsCount || Object.keys(this.relationships).length || 0);

        // Access Controls & Permissions (ACL)
        const acl = raw.acl || raw.Access || {};
        this.acl = {
            liveTracking: acl.acl_live_tracking !== 'DENIED',
            geofence: acl.acl_geofence !== 'DENIED',
            historyAudit: acl.acl_history_audit !== 'DENIED',
            ghostOverride: acl.acl_ghost_override === 'GRANTED',
            camera: acl.camera !== 'DENIED',
            gps: acl.gps !== 'DENIED',
            bluetooth: acl.bluetooth !== 'DENIED',
            foregroundService: acl.foreground_service !== 'DENIED'
        };

        // ==========================================
        // TIER 3: SUB-SUB-MASTER DATA (Live Telemetry & Diagnostics - SSOT Binding)
        // ==========================================
        const live = raw.device?.live || raw.live || {};
        const telem = raw.telemetry || raw.device?.telemetry || {};

        // Parse Coordinates (Supporting standard latitude/longitude, lat/lng, and compact loc array/string)
        let lat = parseFloat(live.latitude || live.lat || data.latitude || data.lat || raw.latitude || 0);
        let lng = parseFloat(live.longitude || live.lng || data.longitude || data.lng || raw.longitude || 0);
        if ((lat === 0 || isNaN(lat)) && live.loc) {
            if (Array.isArray(live.loc) && live.loc.length >= 2) {
                lat = parseFloat(live.loc[0]);
                lng = parseFloat(live.loc[1]);
            } else if (typeof live.loc === 'string' && live.loc.includes(',')) {
                const parts = live.loc.split(',');
                lat = parseFloat(parts[0]);
                lng = parseFloat(parts[1]);
            }
        }
        this.latitude = isNaN(lat) ? 0 : lat;
        this.longitude = isNaN(lng) ? 0 : lng;
        this.hasValidLocation = Boolean(!isNaN(this.latitude) && !isNaN(this.longitude) && this.latitude !== 0 && this.longitude !== 0);

        this.speed = parseFloat(live.speed || live.spd || data.speed || raw.speed || 0);
        this.bearing = parseFloat(live.bearing || live.brg || data.bearing || raw.bearing || 0);
        this.accuracy = parseFloat(live.accuracy || data.accuracy || raw.accuracy || 0);
        this.altitude = parseFloat(live.altitude || data.altitude || raw.altitude || 0);
        this.address = live.address || data.address || raw.address || "";
        this.geohash = live.geohash || raw.geohash || "";

        // Decode Battery: Enterprise SSOT Priority (Valid Live -> Telemetry -> vMask -> Root Data)
        let bVal = -1;
        const liveBat = (live.battery != null && !isNaN(parseInt(live.battery, 10))) ? parseInt(live.battery, 10) : -1;
        const telemBat = (telem.battery_pct != null && !isNaN(parseInt(telem.battery_pct, 10))) ? parseInt(telem.battery_pct, 10) :
                         (telem.battery != null && !isNaN(parseInt(telem.battery, 10))) ? parseInt(telem.battery, 10) : -1;
        const dataBat = (data.battery != null && !isNaN(parseInt(data.battery, 10))) ? parseInt(data.battery, 10) : -1;
        const vmaskRaw = (live.vmask != null && !isNaN(parseInt(live.vmask, 10))) ? (parseInt(live.vmask, 10) & 0x7F) : -1;

        if (dataBat >= 0) {
            bVal = dataBat;
        } else if (liveBat > 0) {
            bVal = liveBat;
        } else if (telemBat >= 0) {
            bVal = telemBat;
        } else if (vmaskRaw > 0 && vmaskRaw <= 100) {
            bVal = vmaskRaw;
        } else if (liveBat >= 0) {
            bVal = liveBat;
        }
        this.battery = bVal;

        // Decode SOS Active: Live flag, vmask bit 7, telem, or data
        const vmaskSos = live.vmask != null ? Boolean((parseInt(live.vmask, 10) >> 7) & 1) : false;
        this.isSos = Boolean(live.isSosActive || live.sos || telem.is_sos || data.isSos || data.isSosActive || vmaskSos);

        this.isCharging = Boolean(telem.isCharging != null ? telem.isCharging : (telem.is_charging != null ? telem.is_charging : data.isCharging));
        this.isMockGps = Boolean(telem.isMockGps != null ? telem.isMockGps : (telem.is_mock_gps != null ? telem.is_mock_gps : data.isMockGps));
        this.isAirplaneMode = Boolean(telem.isAirplaneMode != null ? telem.isAirplaneMode : (telem.is_airplane_mode != null ? telem.is_airplane_mode : data.isAirplaneMode));
        this.networkType = live.networkType || telem.networkType || telem.network_type || data.networkType || "UNKNOWN";

        // Cellular Baseband & Failover Route Telemetry
        this.activeRoute = data.activeRoute || live.active_route || telem.active_route || raw.active_route || "N1_CELLULAR_FAST";
        this.cellularOperator = data.cellularOperator || live.cellular_operator || telem.cellular_operator || this.carrier || "Cellular Provider";
        this.cellularMccMnc = data.cellularMccMnc || live.cellular_mcc_mnc || telem.cellular_mcc_mnc || "000-00";
        this.cellularLacCid = data.cellularLacCid || live.cellular_lac_cid || telem.cellular_lac_cid || "0000-00000";
        this.cellularSignalDbm = data.cellularSignalDbm != null ? data.cellularSignalDbm : (live.cellular_signal_dbm != null ? live.cellular_signal_dbm : (telem.cellular_signal_dbm != null ? telem.cellular_signal_dbm : -75));

        // Last Seen Resolution: Live updated_at, live t (epoch ms), live timestamp, telem, or data
        this.lastSeen = live.updated_at || live.t || live.timestamp || telem.timestamp || telem.last_seen || data.lastSeen || data.lastUpdated || data.timestamp || 0;
        const epochInit = GeoUtils.parseTimestampToEpoch(this.lastSeen);
        this.timestamp = epochInit > 0 ? epochInit : (typeof data.timestamp === 'number' ? data.timestamp : (typeof live.t === 'number' ? live.t : 0));
        const elapsedInit = epochInit > 0 ? (Date.now() - epochInit) : Infinity;

        // [Variables]: recencyLimitMs, dormantLimitMs | [Constants]: None | [Dependencies]: DeviceModel | [Integration Points]: SettingsModel SSOT
        const recencyLimitMs = DeviceModel.getRecencyThresholdMs();
        const dormantLimitMs = DeviceModel.getDormantThresholdMs();
        const dormantHours = Math.max(1, Math.round(dormantLimitMs / 3600000));

        // Online State Resolution: Strictly governed by dynamic recency window and active session
        const isRecentInit = elapsedInit <= recencyLimitMs;
        const isExplicitlyOfflineInit = (raw.st_session_state === "OFFLINE") || (rawIdentity.st_session_state === "OFFLINE") || (telem.session_state === "OFFLINE") || (data.sessionState === "OFFLINE");
        this.isOnline = Boolean(isRecentInit && !isExplicitlyOfflineInit && (data.isOnline !== false));
        this.sessionState = this.isOnline ? "ONLINE" : (data.sessionState && data.sessionState !== "ONLINE" ? data.sessionState : "OFFLINE");

        // RAG Status Mapping from Centralized Rules (Respect Server-Calculated RAG if available)
        if (data.ragStatus) {
            this.ragStatus = data.ragStatus;
            this.ragColor = data.ragColor || (this.ragStatus === "ACTIVE" ? "#22C55E" : (this.ragStatus.includes("WARNING") ? "#F59E0B" : "#EF4444"));
            this.ragLabel = data.ragLabel || (this.ragStatus === "ACTIVE" ? "ACTIVE (LIVE)" : (this.ragStatus.includes("WARNING") ? `INACTIVE (< ${dormantHours}H)` : `INACTIVE (> ${dormantHours}H)`));
        } else if (this.isSos) {
            this.ragStatus = "EMERGENCY_SOS";
            this.ragColor = "#EF4444";
            this.ragLabel = "EMERGENCY SOS";
        } else if (raw.isGhost || raw.Identity?.status === "Ghost" || (elapsedInit === Infinity && !raw.profile)) {
            this.ragStatus = "GHOST";
            this.ragColor = "#64748B";
            this.ragLabel = "UNLINKED / GHOST";
        } else if (this.isOnline) {
            this.ragStatus = "ACTIVE";
            this.ragColor = "#22C55E";
            this.ragLabel = "ACTIVE (LIVE)";
        } else if (elapsedInit <= dormantLimitMs) {
            this.ragStatus = "INACTIVE_WARNING";
            this.ragColor = "#F59E0B";
            this.ragLabel = `INACTIVE (< ${dormantHours}H)`;
        } else {
            this.ragStatus = "INACTIVE_CRITICAL";
            this.ragColor = "#EF4444";
            this.ragLabel = `INACTIVE (> ${dormantHours}H)`;
        }

        // Avatar Resolution (Including Live Avatar propagation)
        if (live.avatar_id && live.avatar_id !== "1") {
            this.avatarId = live.avatar_id;
        }

        this.oldDeviceIdArchive = raw.Old_Device_ID || {};

        // Dynamic Distance computation against reference station
        this.distanceFromRefKm = Infinity;
        this.isWithinSearchRadius = false;
    }

    /**
     * Generates a deterministic SHA-like silicon signature fallback from physical hardware fields
     */
    _generateSiliconSignature() {
        const rawKey = `${this.brand}|${this.model}|${this.board}|${this.hardware}|${this.device}`.toLowerCase();
        let hash = 0;
        for (let i = 0; i < rawKey.length; i++) {
            const char = rawKey.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash |= 0;
        }
        return 'SILICON_' + Math.abs(hash).toString(16).toUpperCase().padStart(8, '0');
    }

    /**
     * TIER 1: Master Data Representation
     * @returns {Object}
     */
    getMasterData() {
        return {
            tier: 1,
            tierName: "Master Data (Identity & Silicon Anchor)",
            agentId: this.agentId,
            deviceId: this.deviceId,
            authUid: this.authUid,
            ownerUid: this.ownerUid,
            connectedId: this.connectedId,
            activeGroup: this.activeGroup,
            hardware: {
                brand: this.brand,
                model: this.model,
                board: this.board,
                hardware: this.hardware,
                device: this.device,
                imei1: this.imei1,
                imei2: this.imei2,
                siliconHash: this.siliconHash
            }
        };
    }

    /**
     * TIER 2: Sub-Master Data Representation
     * @returns {Object}
     */
    getSubMasterData() {
        return {
            tier: 2,
            tierName: "Sub-Master Data (Profile, Persona & ACL)",
            profile: {
                name: this.name,
                deviceName: this.deviceName,
                hostName: this.hostName,
                avatarId: this.avatarId,
                avatarUrl: this.getAvatarUrl(),
                phone: this.phone,
                email: this.email,
                dob: this.dob,
                gender: this.gender,
                role: this.role,
                roleFamily: this.roleFamily,
                roleFriend: this.roleFriend,
                roleTitle: this.roleTitle,
                department: this.department,
                emergencyNote: this.emergencyNote,
                simDossier: this.simDossier
            },
            relationshipsCount: this.relationshipsCount,
            safeZonesCount: this.safeZonesCount,
            acl: this.acl,
            rawRelationships: this.raw.relationships || {},
            rawSafeZones: this.raw.safe_zones || this.raw.SafeZones || {}
        };
    }

    /**
     * TIER 3: Sub-Sub-Master Data Representation
     * @returns {Object}
     */
    getSubSubMasterData() {
        return {
            tier: 3,
            tierName: "Sub-Sub-Master Data (Telemetry & Diagnostics)",
            telemetry: {
                battery: this.battery,
                isCharging: this.isCharging,
                isMockGps: this.isMockGps,
                isAirplaneMode: this.isAirplaneMode,
                networkType: this.networkType,
                activeRoute: this.activeRoute,
                cellularOperator: this.cellularOperator,
                cellularMccMnc: this.cellularMccMnc,
                cellularLacCid: this.cellularLacCid,
                cellularSignalDbm: this.cellularSignalDbm,
                isOnline: this.isOnline,
                sessionState: this.sessionState,
                lastSeen: this.lastSeen,
                formattedLastSeen: this.getFormattedLastSeen(),
                formattedDateTime: this.getFormattedDateTime()
            },
            liveFix: {
                latitude: this.latitude,
                longitude: this.longitude,
                hasValidLocation: this.hasValidLocation,
                speed: this.speed,
                bearing: this.bearing,
                altitude: this.altitude,
                accuracy: this.accuracy,
                address: this.address,
                geohash: this.geohash,
                formattedCoords: this.getFormattedCoords(),
                formattedSpeed: this.getFormattedSpeed()
            },
            oldDeviceIdArchive: this.oldDeviceIdArchive
        };
    }

    /**
     * TIER 4: Transactional Data Representation
     * @returns {Object}
     */
    getTransactionalData() {
        const historyEntries = Object.entries(this.raw.history || {}).map(([k, v]) => ({ id: k, ...v }));
        const changeLogs = this.raw.Change_Log || this.raw.change_log || {};
        const alerts = this.raw.alerts || {};

        return {
            tier: 4,
            tierName: "Transactional Data (Time-Series & Forensic Logs)",
            historyPointsCount: historyEntries.length,
            history: historyEntries.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0)),
            changeLog: changeLogs,
            alerts: alerts,
            systemAudit: this.raw.system_audit || {}
        };
    }

    /**
     * Updates relative distance and proximity state based on operator's coordinates
     * @param {number} refLat 
     * @param {number} refLng 
     * @param {number} radiusKm 
     */
    updateProximity(refLat, refLng, radiusKm = 10) {
        if (this.hasValidLocation && refLat && refLng) {
            this.distanceFromRefKm = GeoUtils.calculateDistanceKm(refLat, refLng, this.latitude, this.longitude);
            this.isWithinSearchRadius = this.distanceFromRefKm <= radiusKm;
        } else {
            this.distanceFromRefKm = Infinity;
            this.isWithinSearchRadius = false;
        }
    }

    /**
     * Resolves Avatar URL
     * @returns {string}
     */
    getAvatarUrl() {
        if (this.avatarUrl && typeof this.avatarUrl === 'string' && !this.avatarUrl.includes('/data/user/')) {
            return this.avatarUrl;
        }
        return AvatarService.getAvatarUrl(this.avatarId, this.name, this.avatarData);
    }

    /**
     * Standardized Centralized RAG Status Classifier (Protocol Specification):
     * - 🟢 ACTIVE (LIVE): Heartbeat / location update < 15 minutes (#22C55E Emerald Green)
     * - 🟡 INACTIVE (< 6H): Last seen between 15m – 6 hours OR Battery ≤ 20% (#F59E0B Amber Orange)
     * - 🔴 INACTIVE (> 6H): Last seen > 6 hours (#EF4444 Rose Red)
     * - 🚨 EMERGENCY SOS: isSos == true (#EF4444 Pulsing Crimson)
     * - ⚪ UNLINKED / GHOST: Deleted, unlinked, or Ghost Mode (#64748B Slate Gray)
     */
    getRagStatus() {
        if (this.isSos) {
            return "EMERGENCY_SOS";
        }
        if (this.sessionState === "UNINSTALLED" || this.sessionState === "PARKED" || this.sessionState === "DELETED" || this.isGhostMode) {
            return "UNINSTALLED";
        }
        const epochMs = GeoUtils.parseTimestampToEpoch(this.lastSeen);
        const now = Date.now();
        const elapsedMs = epochMs > 0 ? (now - epochMs) : Infinity;

        const isLowBattery = (typeof this.battery === 'number' && this.battery >= 0 && this.battery <= 20) ||
                             (typeof this.batteryLevel === 'number' && this.batteryLevel >= 0 && this.batteryLevel <= 20);

        const recencyLimitMs = DeviceModel.getRecencyThresholdMs();
        const dormantLimitMs = DeviceModel.getDormantThresholdMs();

        if (this.isOnline || elapsedMs <= recencyLimitMs) {
            if (isLowBattery) {
                return "INACTIVE_RECENT"; // Battery <= 20% demotes active to Amber warning
            }
            return "ACTIVE"; // Green
        }
        if (elapsedMs <= dormantLimitMs) {
            return "INACTIVE_RECENT"; // Orange (< dormantLimitMs)
        }
        return "INACTIVE_OLD"; // Red (> dormantLimitMs)
    }

    /**
     * Resolves Unified Status Descriptor for Enterprise Dashboard, Roster & Header
     * @returns {Object} { key, status, label, shortLabel, color, badgeClass, dotClass, borderClass }
     */
    getUnifiedStatus() {
        const rag = this.getRagStatus();
        switch (rag) {
            case "EMERGENCY_SOS":
                return {
                    key: "sos",
                    status: "EMERGENCY_SOS",
                    label: "EMERGENCY SOS",
                    shortLabel: "SOS",
                    color: "#EF4444",
                    badgeClass: "badge-status badge-danger animate-pulse font-bold",
                    dotClass: "bg-rose-500 animate-ping",
                    borderClass: "border-rose-500 shadow-rose-500/30"
                };
            case "ACTIVE":
                return {
                    key: "live",
                    status: "ACTIVE",
                    label: "ACTIVE (LIVE)",
                    shortLabel: "Live",
                    color: "#22C55E",
                    badgeClass: "badge-status badge-success",
                    dotClass: "bg-emerald-500 animate-pulse",
                    borderClass: "border-emerald-500 shadow-emerald-500/20"
                };
            case "INACTIVE_RECENT":
                return {
                    key: "inactive",
                    status: "INACTIVE_RECENT",
                    label: "INACTIVE (< 6H)",
                    shortLabel: "Inact (<6h)",
                    color: "#F59E0B",
                    badgeClass: "badge-status badge-warning",
                    dotClass: "bg-amber-500",
                    borderClass: "border-amber-500 shadow-amber-500/20"
                };
            case "INACTIVE_OLD":
                return {
                    key: "stale",
                    status: "INACTIVE_OLD",
                    label: "INACTIVE (> 6H)",
                    shortLabel: "Off (>6h)",
                    color: "#EF4444",
                    badgeClass: "badge-status badge-danger",
                    dotClass: "bg-rose-500",
                    borderClass: "border-rose-500 shadow-rose-500/20"
                };
            case "UNINSTALLED":
            default:
                return {
                    key: "unlinked",
                    status: "UNLINKED",
                    label: "UNLINKED / GHOST",
                    shortLabel: "Ghost",
                    color: "#64748B",
                    badgeClass: "badge-status badge-neutral",
                    dotClass: "bg-slate-400",
                    borderClass: "border-slate-500"
                };
        }
    }

    getRagColor() {
        const status = this.getRagStatus();
        switch (status) {
            case "EMERGENCY_SOS": return "#EF4444"; // Pulsing Crimson
            case "ACTIVE": return "#22C55E"; // Green
            case "INACTIVE_RECENT": return "#F59E0B"; // Orange
            case "INACTIVE_OLD": return "#EF4444"; // Red
            case "UNINSTALLED": return "#64748B"; // Gray
            default: return "#64748B";
        }
    }

    getRagBorderClass() {
        const status = this.getRagStatus();
        switch (status) {
            case "EMERGENCY_SOS": return "border-rose-500 shadow-rose-500/30 animate-pulse";
            case "ACTIVE": return "border-emerald-500 shadow-emerald-500/20";
            case "INACTIVE_RECENT": return "border-amber-500 shadow-amber-500/20";
            case "INACTIVE_OLD": return "border-rose-500 shadow-rose-500/20";
            case "UNINSTALLED": return "border-slate-500 shadow-slate-500/20";
            default: return "border-slate-500";
        }
    }

    getRagBadgeClass() {
        const status = this.getRagStatus();
        switch (status) {
            case "EMERGENCY_SOS": return "bg-rose-500/25 text-rose-400 border-rose-500/50 animate-pulse font-bold";
            case "ACTIVE": return "bg-emerald-500/20 text-emerald-400 border-emerald-500/30";
            case "INACTIVE_RECENT": return "bg-amber-500/20 text-amber-400 border-amber-500/30";
            case "INACTIVE_OLD": return "bg-rose-500/20 text-rose-400 border-rose-500/30";
            case "UNINSTALLED": return "bg-slate-800 text-slate-400 border-white/10";
            default: return "bg-slate-800 text-slate-400";
        }
    }

    getRagLabel() {
        const status = this.getRagStatus();
        switch (status) {
            case "EMERGENCY_SOS": return "🚨 EMERGENCY SOS";
            case "ACTIVE": return "ACTIVE (LIVE)";
            case "INACTIVE_RECENT": return "INACTIVE (< 6H)";
            case "INACTIVE_OLD": return "INACTIVE (> 6H)";
            case "UNINSTALLED": return "UNLINKED / GHOST";
            default: return "OFFLINE";
        }
    }

    /**
     * Resolves Theme Color for this device based on RAG status
     * @returns {string} Hex color
     */
    getThemeColor() {
        return this.getRagColor();
    }

    /**
     * Formatted string of distance
     * @returns {string}
     */
    getFormattedDistance() {
        return GeoUtils.formatDistance(this.distanceFromRefKm);
    }

    /**
     * Formatted relative time delta (e.g., "Live (Just now)", "2m ago")
     * @returns {string}
     */
    getFormattedLastSeen() {
        return GeoUtils.formatRelativeTime(this.lastSeen);
    }

    /**
     * Formatted relative time delta
     * @returns {string}
     */
    getFormattedTimeDelta() {
        return GeoUtils.formatRelativeTime(this.lastSeen);
    }

    /**
     * Formatted absolute Date & Time (e.g., "02 Sep 2026, 11:41:42 AM")
     * @returns {string}
     */
    getFormattedDateTime() {
        return GeoUtils.formatDateTime(this.lastSeen);
    }

    /**
     * Formatted speed
     * @returns {string}
     */
    getFormattedSpeed() {
        return GeoUtils.formatSpeed(this.speed);
    }

    /**
     * Formatted Coordinates
     * @returns {string}
     */
    getFormattedCoords() {
        return GeoUtils.formatCoords(this.latitude, this.longitude);
    }

    /**
     * Battery visual status class
     * @returns {{ text: string, color: string, icon: string }}
     */
    getBatteryMeta() {
        if (this.battery < 0) return { text: "N/A", color: "#64748B", icon: "🔋" };
        if (this.isCharging) return { text: `${this.battery}% ⚡`, color: "#10B981", icon: "⚡" };
        if (this.battery <= 15) return { text: `${this.battery}%`, color: "#EF4444", icon: "🪫" };
        if (this.battery <= 40) return { text: `${this.battery}%`, color: "#F59E0B", icon: "🔋" };
        return { text: `${this.battery}%`, color: "#10B981", icon: "🔋" };
    }

    /**
     * Returns standard epoch milliseconds from this device's timestamp or lastSeen
     * @returns {number}
     */
    getEpochTimestamp() {
        if (typeof this.timestamp === 'number' && this.timestamp > 0) return this.timestamp;
        const t = this.lastSeen || this.raw?.updated_at;
        const epoch = GeoUtils.parseTimestampToEpoch(t);
        if (epoch > 0) this.timestamp = epoch;
        return epoch || 0;
    }

    /**
     * Temporal Recency Tier:
     * - LIVE: < 1 minute (or isOnline)
     * - IDLE: 1m to 15m
     * - STALE: > 15m
     * @returns {{ tier: 'LIVE'|'IDLE'|'STALE', label: string, color: string, badgeClass: string, elapsedMs: number }}
     */
    getRecencyTier() {
        const epoch = this.getEpochTimestamp();
        const now = Date.now();
        const elapsedMs = epoch > 0 ? Math.max(0, now - epoch) : Infinity;

        if (this.isOnline && elapsedMs < 60 * 1000) {
            return {
                tier: 'LIVE',
                label: '🟢 LIVE (Now)',
                color: '#10b981',
                badgeClass: 'badge-status badge-success',
                elapsedMs
            };
        }
        if (elapsedMs <= DeviceModel.getRecencyThresholdMs()) {
            const mins = Math.max(1, Math.floor(elapsedMs / 60000));
            return {
                tier: 'IDLE',
                label: `🟡 IDLE (${mins}m ago)`,
                color: '#f59e0b',
                badgeClass: 'badge-status badge-warning',
                elapsedMs
            };
        }
        return {
            tier: 'STALE',
            label: '🔴 STALE (>15m)',
            color: '#ef4444',
            badgeClass: 'badge-status badge-danger',
            elapsedMs
        };
    }

    /**
     * Master Centralized Enterprise RAG Status Engine (Single Source of Truth across all screens):
     * - 🚨 EMERGENCY SOS: isSos == true (Pulsing Crimson #EF4444)
     * - 🟢 ACTIVE (LIVE): Telemetry < 15 minutes & Battery > 20% (Emerald Green #22C55E)
     * - 🟡 INACTIVE (< 6H): Telemetry 15m–6h OR Battery <= 20% (Amber Orange #F59E0B)
     * - 🔴 INACTIVE (> 6H): Telemetry > 6 hours (Rose Red #EF4444)
     * - ⚪ UNLINKED / GHOST: Uninstalled, ghost, parked, or deleted (Slate Gray #64748B)
     * @returns {{ status: string, label: string, isOnline: boolean, isMoving: boolean, color: string, badgeClass: string, dotClass: string, elapsedMs: number }}
     */
    getUnifiedStatus() {
        const epoch = this.getEpochTimestamp();
        const now = Date.now();
        const elapsedMs = epoch > 0 ? Math.max(0, now - epoch) : Infinity;
        const speed = parseFloat(this.speed || 0);

        // 🚨 1. EMERGENCY SOS (Pulsing Crimson)
        if (this.isSos || this.sessionState === "SOS") {
            return {
                key: 'sos',
                status: 'EMERGENCY',
                label: 'EMERGENCY SOS',
                shortLabel: 'SOS',
                isOnline: true,
                isMoving: speed > 2,
                color: '#EF4444',
                badgeClass: 'badge-status badge-danger animate-pulse font-bold',
                dotClass: 'bg-rose-600 animate-ping',
                borderClass: 'border-rose-500 shadow-rose-500/30',
                elapsedMs
            };
        }

        // ⚪ 2. UNLINKED / GHOST / UNINSTALLED (Slate Gray)
        if (this.sessionState === "UNINSTALLED" || this.sessionState === "PARKED" || this.sessionState === "DELETED" || this.sessionState === "GHOST" || this.isGhostMode) {
            return {
                key: 'unlinked',
                status: 'UNLINKED',
                label: this.sessionState || 'UNLINKED / GHOST',
                shortLabel: 'Ghost',
                isOnline: false,
                isMoving: false,
                color: '#64748B',
                badgeClass: 'badge-status badge-secondary',
                dotClass: 'bg-slate-500',
                borderClass: 'border-slate-500',
                elapsedMs
            };
        }

        const recencyLimitMs = DeviceModel.getRecencyThresholdMs();
        const dormantLimitMs = DeviceModel.getDormantThresholdMs();
        const dormantHours = Math.max(1, Math.round(dormantLimitMs / 3600000));

        // 🟢 3. ACTIVE (LIVE) < recencyLimitMs & Battery > 20%
        if (elapsedMs <= recencyLimitMs && (this.battery < 0 || this.battery > 20)) {
            const isMoving = speed > 2;
            return {
                key: 'live',
                status: 'LIVE',
                label: isMoving ? 'LIVE (MOVING)' : 'LIVE (ACTIVE)',
                shortLabel: 'Live',
                isOnline: true,
                isMoving,
                color: '#22C55E',
                badgeClass: 'badge-status badge-success',
                dotClass: 'bg-emerald-500 animate-pulse',
                borderClass: 'border-emerald-500 shadow-emerald-500/20',
                elapsedMs
            };
        }

        // 🟡 4. INACTIVE (< dormantLimitMs) or Low Battery <= 20%
        if (elapsedMs <= dormantLimitMs || (this.battery >= 0 && this.battery <= 20)) {
            let label = `INACTIVE (<${dormantHours}H)`;
            if (this.battery >= 0 && this.battery <= 20 && elapsedMs <= recencyLimitMs) {
                label = `LOW BATT (${this.battery}%)`;
            } else if (elapsedMs <= 60 * 60 * 1000) {
                const mins = Math.max(1, Math.floor(elapsedMs / 60000));
                label = `IDLE (${mins}m)`;
            } else {
                const hrs = Math.floor(elapsedMs / 3600000);
                label = `INACTIVE (${hrs}h)`;
            }
            return {
                key: 'inactive',
                status: 'INACTIVE_RECENT',
                label,
                shortLabel: `Inact (<${dormantHours}h)`,
                isOnline: elapsedMs <= recencyLimitMs,
                isMoving: false,
                color: '#F59E0B',
                badgeClass: 'badge-status badge-warning',
                dotClass: 'bg-amber-400',
                borderClass: 'border-amber-500 shadow-amber-500/20',
                elapsedMs
            };
        }

        // 🔴 5. INACTIVE (> 6H)
        return {
            key: 'stale',
            status: 'INACTIVE_STALE',
            label: 'INACTIVE (>6H)',
            shortLabel: 'Off (>6h)',
            isOnline: false,
            isMoving: false,
            color: '#EF4444',
            badgeClass: 'badge-status badge-danger',
            dotClass: 'bg-rose-500',
            borderClass: 'border-rose-500 shadow-rose-500/20',
            elapsedMs
        };
    }

    /**
     * Dynamic threshold for live active device status (resolved from SettingsModel SSOT)
     * @returns {number} Threshold in milliseconds
     */
    static getRecencyThresholdMs() {
        const mins = (typeof window !== 'undefined' && window.app?.settingsModel?.recencyThresholdMinutes) || 15;
        return Math.max(1, parseInt(mins, 10)) * 60 * 1000;
    }

    /**
     * Dynamic threshold for dormant / inactive ceiling (resolved from SettingsModel SSOT)
     * @returns {number} Threshold in milliseconds
     */
    static getDormantThresholdMs() {
        const hrs = (typeof window !== 'undefined' && window.app?.settingsModel?.dormantThresholdHours) || 6;
        return Math.max(1, parseInt(hrs, 10)) * 60 * 60 * 1000;
    }

    /**
     * Robust version parser to extract clean semantic version and build code
     * without duplicating "V" prefixes or misattributing obsolete fallbacks.
     * @param {Object} raw Raw device snapshot from Firebase
     * @param {Object} data Secondary payload or fallback object
     * @returns {{ versionName: string, versionCode: number }}
     */
    static parseVersion(raw = {}, data = {}) {
        const rawObj = (raw && typeof raw === 'object') ? raw : {};
        const dataObj = (data && typeof data === 'object') ? data : {};
        const rawDevice = rawObj.device || {};
        const rawTelem = rawObj.telemetry || {};
        const rawVerUpdate = rawObj.Version_Update || {};

        // If raw is a string directly (e.g. "V1.0.13 (Build 86)")
        const rawString = typeof raw === 'string' ? raw : null;

        // Ensure Version_Update candidate only comes from device's own reported node
        const isDeviceVerUpdate = rawVerUpdate.latest && (rawVerUpdate.latest.device_id || !rawVerUpdate.master_release);
        const devLatest = isDeviceVerUpdate ? rawVerUpdate.latest : {};

        const candidates = [
            rawString,
            rawDevice.version_name,
            rawDevice.app_version,
            rawTelem.app_version,
            rawTelem.version_name,
            devLatest.version_name,
            dataObj.versionName,
            dataObj.appVersion
        ].filter(Boolean);

        let vName = "";
        let vCode = 0;

        for (const cand of candidates) {
            const str = String(cand).trim();
            // Match e.g. "V1.0.13 (Build 86)" or "1.0.13 (Build 87)" or "V1.0.13" or "1.0.13"
            const m = str.match(/^(?:V)?(\d+\.\d+\.\d+)(?:\s*\((?:Build\s*)?(\d+)\))?/i);
            if (m) {
                if (!vName) vName = m[1];
                if (!vCode && m[2]) vCode = parseInt(m[2], 10);
            }
        }

        const directCodes = [
            rawDevice.version_code,
            rawTelem.version_code,
            devLatest.version_code,
            dataObj.versionCode
        ].filter(v => v != null && !isNaN(parseInt(v, 10))).map(v => parseInt(v, 10));

        if (directCodes.length > 0) {
            const maxDirect = Math.max(...directCodes);
            if (maxDirect > vCode) vCode = maxDirect;
        }

        // Project SSOT baseline: 1.0.13 / Build 86 (installed)
        if (!vName) vName = "1.0.13";
        if (!vCode) vCode = 86;

        return { versionName: vName, versionCode: vCode };
    }
}

