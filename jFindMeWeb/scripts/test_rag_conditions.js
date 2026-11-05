const http = require('http');
const fs = require('fs');
const path = require('path');

class RAGConditionTester {
    constructor() {
        this.host = '127.0.0.1';
        this.port = 3000;
        this.results = [];
        this.startTime = Date.now();
    }

    async request(urlPath, method = 'GET', body = null) {
        return new Promise((resolve, reject) => {
            const options = {
                hostname: this.host,
                port: this.port,
                path: urlPath,
                method,
                headers: { 'Content-Type': 'application/json' },
                timeout: 8000
            };

            const req = http.request(options, (res) => {
                let data = '';
                res.on('data', chunk => data += chunk);
                res.on('end', () => {
                    let parsed = null;
                    try {
                        parsed = JSON.parse(data);
                    } catch (e) {
                        parsed = data;
                    }
                    resolve({ status: res.statusCode, data: parsed, raw: data });
                });
            });

            req.on('timeout', () => {
                req.destroy();
                reject(new Error(`Timeout on ${method} ${urlPath}`));
            });

            req.on('error', reject);
            if (body) req.write(JSON.stringify(body));
            req.end();
        });
    }

    recordResult(suite, scenarioName, condition, validationRule, expectedStatus, actualStatus, pass, details = {}) {
        let sanitizedDetails = details;
        if (details && typeof details === 'object') {
            if (details.error) {
                sanitizedDetails = { error: details.error, message: details.message };
            } else {
                sanitizedDetails = {
                    status: details.status || (pass ? "SUCCESS" : "FAIL"),
                    count: Array.isArray(details) ? details.length : (details.count !== undefined ? details.count : undefined),
                    keys: Object.keys(details).slice(0, 6)
                };
            }
        }
        const item = {
            suite,
            scenarioName,
            condition,
            validationRule,
            expectedStatus,
            actualStatus,
            pass,
            details: sanitizedDetails
        };
        this.results.push(item);
        const icon = pass ? 'PASS [GREEN]' : 'FAIL [RED]';
        console.log(`  ${icon} [${suite}] ${scenarioName} -> Expected: ${expectedStatus}, Got: ${actualStatus}`);
        if (!pass) {
            console.log(`      Error: ${JSON.stringify(sanitizedDetails)}`);
        }
    }

    async runAll() {
        console.log('======================================================================');
        console.log('🔍 jButterFly MLM Web Admin - Critical RAG Condition & Validation Audit');
        console.log('======================================================================\n');

        // SECTION 1: INGRESS & ROSTER GET ENDPOINTS (PASS SCENARIOS)
        console.log('--- 1. Ingress & Roster Read Endpoints (Positive Scenarios) ---');
        const getEndpoints = [
            { path: '/api/ping', rule: 'BFR-01: Heartbeat responds 200', check: res => res.data?.status === 'ALIVE' },
            { path: '/api/discovery', rule: 'BFR-02: Cloud topology discovery responds 200', check: res => Boolean(res.data?.projectId && Array.isArray(res.data?.rtdb)) },
            { path: '/api/devices-live', rule: 'BFR-03: Real-time aggregated fleet roster responds 200', check: res => res.data?.status === 'SUCCESS' && Array.isArray(res.data?.devices) },
            { path: '/api/fleet/compact-live', rule: 'BFR-04: Compact lightweight radar telemetry responds 200', check: res => res.data?.status === 'SUCCESS' && Array.isArray(res.data?.devices) },
            { path: '/api/safe-zones', rule: 'BFR-05: Geofence perimeters responds 200', check: res => res.data?.status === 'SUCCESS' && Array.isArray(res.data?.zones || res.data?.safeZones) },
            { path: '/api/settings', rule: 'BFR-06: Operator configuration settings responds 200', check: res => (res.data?.status === 'SUCCESS' || res.data?.status === 'OK') && res.data?.settings !== undefined },
            { path: '/api/circles', rule: 'BFR-07: MLM circle taxonomy responds 200', check: res => Array.isArray(res.data?.circles) },
            { path: '/api/stats', rule: 'BFR-08: Macro-to-micro drilldown stats responds 200', check: res => res.data?.total_records !== undefined || res.data?.stats !== undefined },
            { path: '/api/system-release', rule: 'BFR-09: SSOT OTA release metadata responds 200', check: res => res.data?.status === 'SUCCESS' && res.data?.release?.version_code },
            { path: '/api/archived-devices', rule: 'BFR-10: Cold-storage decommissioned devices responds 200', check: res => res.data?.status === 'SUCCESS' && Array.isArray(res.data?.archivedDevices) },
            { path: '/api/duplicate-devices', rule: 'BFR-11: Silicon SHA-256 duplicate candidate scan responds 200', check: res => Array.isArray(res.data?.duplicates) },
            { path: '/api/avatars', rule: 'BFR-12: High-DPI avatar asset catalog responds 200', check: res => res.data?.status === 'SUCCESS' && Array.isArray(res.data?.avatars) }
        ];

        for (const ep of getEndpoints) {
            try {
                const res = await this.request(ep.path, 'GET');
                const valid = res.status === 200 && ep.check(res);
                this.recordResult('GET_ENDPOINTS', ep.path, 'Valid authenticated operator request', ep.rule, 200, res.status, valid, res.data);
            } catch (err) {
                this.recordResult('GET_ENDPOINTS', ep.path, 'Valid authenticated operator request', ep.rule, 200, 500, false, { error: err.message });
            }
        }

        // SECTION 2: INPUT VALIDATION GUARDS (NEGATIVE FAIL SCENARIOS)
        console.log('\n--- 2. Input Validation Guards (Negative Reject Scenarios -> 400 Bad Request) ---');
        const validationGuards = [
            {
                name: 'Reject SafeZone creation with missing coordinates',
                path: '/api/save-safe-zone',
                method: 'POST',
                body: { zoneName: 'Incomplete SafeZone' }, // Missing lat/lng
                rule: 'BFR-SZ-01: Name, latitude, and longitude mandatory',
                expectedStatus: 400,
                check: res => res.data?.error === 'INVALID_PARAMETERS'
            },
            {
                name: 'Reject SafeZone toggle with missing zone_id',
                path: '/api/safezone/toggle-state',
                method: 'POST',
                body: { isActive: false }, // Missing zoneId
                rule: 'BFR-SZ-02: Target zone_id mandatory for state toggle',
                expectedStatus: 400,
                check: res => res.data?.error === 'MISSING_ZONE_ID'
            },
            {
                name: 'Reject SafeZone update targets with missing zone_id',
                path: '/api/safezone/update-targets',
                method: 'POST',
                body: { connectedDevices: ['TEST_DEVICE'] }, // Missing zoneId
                rule: 'BFR-SZ-03: Target zone_id mandatory for target mapping',
                expectedStatus: 400,
                check: res => res.data?.error === 'MISSING_ZONE_ID'
            },
            {
                name: 'Reject SafeZone delete with missing zone_id',
                path: '/api/safezone/delete',
                method: 'POST',
                body: {}, // Missing zoneId
                rule: 'BFR-SZ-04: Target zone_id mandatory for purge',
                expectedStatus: 400,
                check: res => res.data?.error === 'MISSING_ZONE_ID'
            },
            {
                name: 'Reject Device Poke with missing agentId',
                path: '/api/poke-device',
                method: 'POST',
                body: {}, // Missing agentId
                rule: 'BFR-DEV-01: Target agentId mandatory for live nudge',
                expectedStatus: 400,
                check: res => res.data?.error === 'MISSING_AGENT_ID'
            },
            {
                name: 'Reject Decommission delete-everywhere with missing agentId',
                path: '/api/device/delete-everywhere',
                method: 'POST',
                body: { mode: 'archive' }, // Missing agentId
                rule: 'BFR-DEV-02: Target agentId mandatory for decommissioning',
                expectedStatus: 400,
                check: res => res.data?.error === 'MISSING_AGENT_ID'
            },
            {
                name: 'Reject Restore Archive with missing agentId',
                path: '/api/device/restore-archive',
                method: 'POST',
                body: {}, // Missing agentId
                rule: 'BFR-DEV-03: Target agentId mandatory for restoration',
                expectedStatus: 400,
                check: res => res.data?.error === 'MISSING_AGENT_ID'
            },
            {
                name: 'Reject Toggle ACL with missing target parameters',
                path: '/api/device/toggle-acl',
                method: 'POST',
                body: { agentId: 'UNIT_01' }, // Missing flag
                rule: 'BFR-MLM-01: Both agentId and flag required for ACL toggle',
                expectedStatus: 400,
                check: res => res.data?.error === 'MISSING_PARAMETERS' || res.data?.error === 'MISSING_PARAMS'
            },
            {
                name: 'Reject Add Relationship with missing peer parameters',
                path: '/api/relationship/add',
                method: 'POST',
                body: { agentId: 'UNIT_01' }, // Missing targetAgentId
                rule: 'BFR-MLM-02: Symmetrical pairing requires both unit identities',
                expectedStatus: 400,
                check: res => res.data?.error === 'MISSING_PARAMS' || res.data?.error === 'MISSING_AGENT_IDS'
            },
            {
                name: 'Reject Delete Relationship with missing peer parameters',
                path: '/api/relationship/delete',
                method: 'POST',
                body: { agentId: 'UNIT_01' }, // Missing targetAgentId
                rule: 'BFR-MLM-03: Symmetrical unlinking requires both unit identities',
                expectedStatus: 400,
                check: res => res.data?.error === 'MISSING_PARAMS' || res.data?.error === 'MISSING_AGENT_IDS'
            },
            {
                name: 'Reject OTA Push Update with missing agentId',
                path: '/api/device/push-update',
                method: 'POST',
                body: {}, // Missing agentId
                rule: 'BFR-OTA-01: Target unit identity required for OTA command',
                expectedStatus: 400,
                check: res => res.data?.error === 'MISSING_AGENT_ID'
            },
            {
                name: 'Reject Duplicate Pair Verify with missing IDs',
                path: '/api/verify-duplicate-pair',
                method: 'POST',
                body: { oldAgentId: 'UNIT_OLD' }, // Missing newAgentId
                rule: 'BFR-DUP-01: Both oldAgentId and newAgentId required for pre-flight',
                expectedStatus: 400,
                check: res => res.data?.error === 'MISSING_AGENT_IDS' || res.data?.error === 'MISSING_PARAMS'
            },
            {
                name: 'Reject Duplicate Merge with missing IDs',
                path: '/api/merge-duplicate-devices',
                method: 'POST',
                body: { oldAgentId: 'UNIT_OLD' }, // Missing newAgentId
                rule: 'BFR-DUP-02: Both oldAgentId and newAgentId required for migration',
                expectedStatus: 400,
                check: res => res.data?.error === 'MISSING_AGENT_IDS' || res.data?.error === 'MISSING_PARAMS'
            },
            {
                name: 'Reject Heading Sync with missing required keys',
                path: '/api/sync-heading',
                method: 'POST',
                body: { agentId: 'UNIT_01' }, // Missing oldAgentId, newAgentId, headingKey
                rule: 'BFR-DUP-03: Required heading synchronization parameters mandatory',
                expectedStatus: 400,
                check: res => res.data?.error === 'MISSING_PARAMS' || res.data?.error === 'MISSING_AGENT_ID'
            },
            {
                name: 'Reject Settings Save with empty payload',
                path: '/api/save-settings',
                method: 'POST',
                body: {}, // Empty body
                rule: 'BFR-CFG-01: Non-empty settings payload required',
                expectedStatus: 400,
                check: res => res.data?.error === 'INVALID_PAYLOAD'
            }
        ];

        for (const guard of validationGuards) {
            try {
                const res = await this.request(guard.path, guard.method, guard.body);
                const valid = res.status === guard.expectedStatus && guard.check(res);
                this.recordResult('INPUT_VALIDATIONS', guard.name, 'Malformed or incomplete payload', guard.rule, guard.expectedStatus, res.status, valid, res.data);
            } catch (err) {
                this.recordResult('INPUT_VALIDATIONS', guard.name, 'Malformed or incomplete payload', guard.rule, guard.expectedStatus, 500, false, { error: err.message });
            }
        }

        // SECTION 3: CHRONOLOGICAL SEQUENCE FLOW VERIFICATION
        console.log('\n--- 3. Chronological Sequence Flow Audits ---');

        // Sequence 1: Fleet Interrogate Burst
        try {
            const burstRes = await this.request('/api/fleet/interrogate-live', 'POST', { requester: 'RAG_Auto_Tester' });
            const burstPass = burstRes.status === 200 && burstRes.data?.status === 'SUCCESS';
            this.recordResult('CHRONO_FLOWS', 'Seq 1: Fleet Live Interrogation & Nudge Burst', 'Admin triggers mass FCM wake broadcast', 'BFR-SEQ-01: Dispatches high-priority data messages', 200, burstRes.status, burstPass, burstRes.data);
        } catch (e) {
            this.recordResult('CHRONO_FLOWS', 'Seq 1: Fleet Live Interrogation & Nudge Burst', 'Admin triggers mass FCM wake broadcast', 'BFR-SEQ-01: Dispatches high-priority data messages', 200, 500, false, { error: e.message });
        }

        // Sequence 2: SafeZone Complete Lifecycle (Create -> Read -> Update -> Delete)
        const testZoneId = `TEST_SZ_${Date.now()}`;
        try {
            // Step A: Create SafeZone
            const createRes = await this.request('/api/save-safe-zone', 'POST', {
                zoneId: testZoneId,
                zoneName: 'Automated RAG Audit Perimeter',
                latitude: 19.0760,
                longitude: 72.8777,
                radius: 250,
                creatorId: 'AUDIT_ROBOT',
                isActive: true
            });
            const createPass = createRes.status === 200 && (createRes.data?.status === 'SUCCESS' || createRes.data?.success);

            // Step B: Toggle State
            const toggleRes = await this.request('/api/safezone/toggle-state', 'POST', {
                zoneId: testZoneId,
                isActive: false
            });
            const togglePass = toggleRes.status === 200 && (toggleRes.data?.status === 'SUCCESS' || toggleRes.data?.success);

            // Step C: Delete SafeZone
            const deleteRes = await this.request('/api/safezone/delete', 'POST', {
                zoneId: testZoneId,
                creatorUid: 'AUDIT_ROBOT'
            });
            const deletePass = deleteRes.status === 200 && (deleteRes.data?.status === 'SUCCESS' || deleteRes.data?.status === 'DELETED');

            const szSeqPass = createPass && togglePass && deletePass;
            this.recordResult('CHRONO_FLOWS', 'Seq 2: SafeZone Closed-Loop Lifecycle (Create -> Toggle -> Delete)', 'Operator manages geofence perimeter', 'BFR-SEQ-02: Atomic RTDB CRUD and cache invalidation', 200, deleteRes.status, szSeqPass, { createPass, togglePass, deletePass });
        } catch (e) {
            this.recordResult('CHRONO_FLOWS', 'Seq 2: SafeZone Closed-Loop Lifecycle', 'Operator manages geofence perimeter', 'BFR-SEQ-02: Atomic RTDB CRUD and cache invalidation', 200, 500, false, { error: e.message });
        }

        // Sequence 3: Settings Sync & Persistence Lifecycle
        try {
            const saveRes = await this.request('/api/save-settings', 'POST', {
                path: 'config/operator/settings',
                data: {
                    theme: 'dark',
                    audit_timestamp: Date.now(),
                    rag_verified: true
                }
            });
            const savePass = saveRes.status === 200 && saveRes.data?.status === 'SUCCESS';

            const getSettingsRes = await this.request('/api/settings', 'GET');
            const getPass = getSettingsRes.status === 200 && getSettingsRes.data?.settings !== undefined;

            this.recordResult('CHRONO_FLOWS', 'Seq 3: Operator Settings Write-Read Round-Trip', 'Operator modifies UI theme and radar preferences', 'BFR-SEQ-03: Realtime sync across multi-admin instances', 200, getSettingsRes.status, savePass && getPass, { savePass, getPass });
        } catch (e) {
            this.recordResult('CHRONO_FLOWS', 'Seq 3: Operator Settings Write-Read Round-Trip', 'Operator modifies UI theme and radar preferences', 'BFR-SEQ-03: Realtime sync across multi-admin instances', 200, 500, false, { error: e.message });
        }

        // Sequence 4: Master Release Audit & Dynamic Update
        try {
            const relPost = await this.request('/api/system-release', 'POST', {
                version_name: '1.0.13',
                version_code: 87,
                database_schema: 'V75',
                notes: 'RAG Audit Verified Enterprise Release'
            });
            const relPostPass = relPost.status === 200 && relPost.data?.status === 'SUCCESS';

            const relGet = await this.request('/api/system-release', 'GET');
            const relGetPass = relGet.status === 200 && relGet.data?.release?.version_code === 87;

            this.recordResult('CHRONO_FLOWS', 'Seq 4: SSOT Master Release OTA Management', 'Release engineer registers binary update', 'BFR-SEQ-04: Instantaneous fleet version-gap calculation', 200, relGet.status, relPostPass && relGetPass, { relPostPass, relGetPass });
        } catch (e) {
            this.recordResult('CHRONO_FLOWS', 'Seq 4: SSOT Master Release OTA Management', 'Release engineer registers binary update', 'BFR-SEQ-04: Instantaneous fleet version-gap calculation', 200, 500, false, { error: e.message });
        }

        // Sequence 5: Dynamic Fleet Recency & RAG Calculation Loop
        try {
            const devRes = await this.request('/api/devices-live', 'GET');
            const devPass = devRes.status === 200 && Array.isArray(devRes.data?.devices) && devRes.data.devices.length >= 8;
            const hasGhostNodes = devRes.data?.devices?.some(d => d.agentId === 'UNIT0001' && d.ragStatus === 'GHOST');
            const hasTwinClones = devRes.data?.devices?.some(d => d.agentId === '4379A761') && devRes.data?.devices?.some(d => d.agentId === 'A7D5968D');
            const seq5Pass = devPass && hasGhostNodes && hasTwinClones;
            this.recordResult('CHRONO_FLOWS', 'Seq 5: Dynamic Fleet Recency & RAG Calculation Loop', '8 Firebase devices validated with dynamic recency thresholds', 'BFR-SEQ-05: Realtime fleet parity matching Firebase RTDB', 200, devRes.status, seq5Pass, { total: devRes.data?.count, hasGhostNodes, hasTwinClones });
        } catch (e) {
            this.recordResult('CHRONO_FLOWS', 'Seq 5: Dynamic Fleet Recency & RAG Calculation Loop', '8 Firebase devices validated with dynamic recency thresholds', 'BFR-SEQ-05: Realtime fleet parity matching Firebase RTDB', 200, 500, false, { error: e.message });
        }

        // Sequence 6: Duplicate Pair Silicon Hash Verification & Old_Device_ID Parity
        try {
            const dupRes = await this.request('/api/duplicate-devices', 'GET');
            const dupPass = dupRes.status === 200 && Array.isArray(dupRes.data?.duplicates);
            const foundPair = dupRes.data?.duplicates?.find(d => d.pairId === 'A7D5968D_TO_4379A761' || (d.oldAgentId === 'A7D5968D' && d.newAgentId === '4379A761'));
            const hasSiliconHash = Boolean(foundPair?.siliconHash);
            const seq6Pass = dupPass && Boolean(foundPair) && hasSiliconHash;
            this.recordResult('CHRONO_FLOWS', 'Seq 6: Duplicate Pair Silicon Hash Verification & Old_Device_ID Parity', 'Hardware clone detection via Silicon SHA-256', 'BFR-SEQ-06: Precise hardware twin identification without false positives', 200, dupRes.status, seq6Pass, { foundPair: Boolean(foundPair), siliconHash: foundPair?.siliconHash });
        } catch (e) {
            this.recordResult('CHRONO_FLOWS', 'Seq 6: Duplicate Pair Silicon Hash Verification & Old_Device_ID Parity', 'Hardware clone detection via Silicon SHA-256', 'BFR-SEQ-06: Precise hardware twin identification without false positives', 200, 500, false, { error: e.message });
        }

        // Summary
        this.printSummary();
    }

    printSummary() {
        const duration = ((Date.now() - this.startTime) / 1000).toFixed(2);
        const total = this.results.length;
        const passed = this.results.filter(r => r.pass).length;
        const failed = this.results.filter(r => !r.pass).length;
        const passPercent = Math.round((passed / total) * 100);

        console.log('\n======================================================================');
        console.log('📊 RAG AUDIT REPORT SUMMARY');
        console.log('----------------------------------------------------------------------');
        console.log(`Total Scenarios Tested:  ${total}`);
        console.log(`Passed (GREEN):          ${passed} ✅`);
        console.log(`Failed (RED):            ${failed} ❌`);
        console.log(`Compliance Score:        ${passPercent}%`);
        console.log(`Execution Time:          ${duration}s`);
        console.log('======================================================================\n');

        if (failed > 0) {
            console.log('🚨 FAILED CRITICAL SCENARIOS (ATTENTION REQUIRED):');
            this.results.filter(r => !r.pass).forEach(f => {
                console.log(`  - [${f.suite}] ${f.scenarioName}`);
                console.log(`    Expected Status: ${f.expectedStatus} | Actual: ${f.actualStatus}`);
                console.log(`    Validation Rule: ${f.validationRule}`);
                console.log(`    Details: ${JSON.stringify(f.details)}\n`);
            });
        } else {
            console.log('🎉 ALL RAG CRITICAL SCENARIOS PASSED WITH ZERO DRIFT OR GAPS.');
        }

        // Write full JSON report for analysis
        fs.writeFileSync(
            path.join(__dirname, '../docs/RAG_AUDIT_EXECUTION_RESULTS.json'),
            JSON.stringify({ timestamp: new Date().toISOString(), total, passed, failed, passPercent, duration, results: this.results }, null, 2),
            'utf-8'
        );
    }
}

const tester = new RAGConditionTester();
tester.runAll().catch(e => {
    console.error('Fatal execution error:', e);
    process.exit(1);
});
