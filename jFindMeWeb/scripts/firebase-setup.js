/**
 * Firebase Database Setup Script
 * Creates necessary paths and default configurations
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

async function setupDatabase() {
    console.log('\n🔧 jButterfly Firebase Database Setup\n');
    console.log('='.repeat(50));

    // Load service account
    let SA_PATH = path.join(__dirname, "../service-account.json");
    const GS_PATH = path.join(__dirname, "../google-services.json");

    if (!fs.existsSync(SA_PATH)) {
        const root = path.join(__dirname, "../");
        const files = fs.readdirSync(root);
        const saFile = files.find(f => f.includes('firebase-adminsdk') && f.endsWith('.json'));
        if (saFile) {
            SA_PATH = path.join(root, saFile);
            console.log(`[INFO] Using: ${saFile}`);
        }
    }

    if (!fs.existsSync(SA_PATH) || !fs.existsSync(GS_PATH)) {
        console.error('[ERROR] Missing service-account.json or google-services.json');
        process.exit(1);
    }

    const G_SERVICE_ACCOUNT = JSON.parse(fs.readFileSync(SA_PATH, 'utf8'));
    const G_SERVICES = JSON.parse(fs.readFileSync(GS_PATH, 'utf8'));

    try {
        admin.initializeApp({
            credential: admin.credential.cert(G_SERVICE_ACCOUNT),
            databaseURL: G_SERVICES.project_info.firebase_url
        });
        console.log('[OK] Firebase Admin SDK initialized\n');
    } catch (e) {
        console.error('[ERROR] Failed to initialize Firebase:', e.message);
        process.exit(1);
    }

    const db = admin.database();

    // Default circles/groups
    const defaultCircles = {
        family: { id: 'family', name: 'Family Circle', description: 'Family members and close contacts' },
        operations: { id: 'operations', name: 'Operations', description: 'Operations team members' },
        field: { id: 'field', name: 'Field Service', description: 'Field service personnel' },
        default: { id: 'default', name: 'Default Circle', description: 'General circle' }
    };

    // Settings template
    const defaultSettings = {
        referenceLat: 18.5788,
        referenceLng: 73.8887,
        referenceAddress: 'Operator Station',
        searchRadiusKm: 10.0,
        mapEngine: 'leaflet',
        mapTileTheme: 'google_clean',
        refreshIntervalSec: 5,
        showRadiusCircle: true,
        showAllDevicesIfOutsideRadius: true,
        autoFitBounds: false,
        lastSyncTime: Date.now()
    };

    const operations = [
        {
            path: 'config/circles',
            data: defaultCircles,
            label: 'Default Circles/Groups'
        },
        {
            path: 'config/operator/settings',
            data: defaultSettings,
            label: 'Default Operator Settings'
        },
        {
            path: 'config/system',
            data: {
                version: '3.1.0',
                lastUpdated: Date.now(),
                features: {
                    mobileSupport: true,
                    offlineMode: true,
                    keyboardShortcuts: true,
                    dataExport: true
                }
            },
            label: 'System Configuration'
        }
    ];

    console.log('Creating database paths...\n');

    for (const op of operations) {
        try {
            const snapshot = await db.ref(op.path).once('value');
            
            if (snapshot.exists()) {
                console.log(`[SKIP] ${op.label} - Already exists at ${op.path}`);
                
                // Update with defaults if not exists
                const current = snapshot.val();
                const updates = {};
                
                for (const [key, value] of Object.entries(op.data)) {
                    if (current[key] === undefined) {
                        updates[key] = value;
                    }
                }
                
                if (Object.keys(updates).length > 0) {
                    await db.ref(op.path).update(updates);
                    console.log(`[UPDATE] Added missing fields to ${op.label}`);
                }
            } else {
                await db.ref(op.path).set(op.data);
                console.log(`[CREATE] ${op.label} - Created at ${op.path}`);
            }
        } catch (err) {
            console.error(`[ERROR] Failed to setup ${op.label}:`, err.message);
        }
    }

    console.log('\n' + '='.repeat(50));
    console.log('\n✅ Database setup completed!\n');
    
    console.log('Created paths:');
    console.log('  • config/circles - Default group options');
    console.log('  • config/operator/settings - Default operator settings');
    console.log('  • config/system - System configuration');
    console.log('\nYou can now use the Device Registration Wizard and');
    console.log('settings will persist across sessions.\n');

    process.exit(0);
}

// Run setup
setupDatabase().catch(err => {
    console.error('[FATAL] Setup failed:', err);
    process.exit(1);
});
