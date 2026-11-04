#!/usr/bin/env node
/**
 * jButterfly Comprehensive Validation Suite
 * Tests all components and integrations
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

// [Variables]: WEB_ROOT, resolveWeb | [Constants]: WEB_ROOT | [Dependencies]: fs, path | [Integration Points]: System Validation Harness
const WEB_ROOT = path.resolve(__dirname, '..');
const resolveWeb = (rel) => path.resolve(WEB_ROOT, rel);

class ValidationSuite {
    constructor() {
        this.results = [];
        this.baseURL = 'http://127.0.0.1:3000';
        this.startTime = Date.now();
    }

    async run() {
        console.log('\n🦋 jButterfly GPS Telemetry - Comprehensive Validation\n');
        console.log('='.repeat(70));
        console.log(`Started: ${new Date().toLocaleString()}\n`);

        await this.testFileStructure();
        await this.testCodeQuality();
        await this.testAPIEndpoints();
        await this.testMobileIntegration();
        await this.testSecurity();
        await this.testDocumentation();

        this.printSummary();
    }

    async testFileStructure() {
        console.log('📁 TEST 1: File Structure Verification\n');
        
        const requiredFiles = [
            // Core files
            { path: 'index.html', desc: 'Main HTML entry point' },
            { path: 'js/app.js', desc: 'Main application orchestrator' },
            { path: 'js/server.js', desc: 'Backend server' },
            { path: 'package.json', desc: 'Project configuration' },
            
            // Security fixes
            { path: 'js/core/Auth.js', desc: 'Authentication service (fixed)' },
            { path: 'js/models/SettingsModel.js', desc: 'Settings with server sync' },
            
            // New components
            { path: 'js/components/KeyboardShortcuts.js', desc: 'Keyboard navigation' },
            { path: 'js/components/DeviceRegistrationWizard.js', desc: 'Device registration' },
            { path: 'js/components/DataExport.js', desc: 'Data export functionality' },
            { path: 'js/core/OfflineQueue.js', desc: 'Offline IndexedDB queue' },
            { path: 'js/core/ErrorRecovery.js', desc: 'Error handling system' },
            
            // Styling
            { path: 'css/mobile-responsive.css', desc: 'Mobile responsive styles' },
            { path: 'css/variables.css', desc: 'Design tokens' },
            
            // Views with fixes
            { path: 'js/views/GeofencingView.js', desc: 'Geofencing (debounce fix)' },
            
            // Documentation (check docs/ directory)
            { path: fs.existsSync(resolveWeb('docs/DEPLOYMENT_READY.md')) ? 'docs/DEPLOYMENT_READY.md' : '.github/DEPLOYMENT_READY.md', desc: 'Deployment guide' },
            { path: fs.existsSync(resolveWeb('docs/FINAL_VALIDATION_REPORT.md')) ? 'docs/FINAL_VALIDATION_REPORT.md' : '.github/FINAL_VALIDATION_REPORT.md', desc: 'Validation report' },
            { path: fs.existsSync(resolveWeb('docs/QUICK_START.md')) ? 'docs/QUICK_START.md' : '.github/QUICK_START.md', desc: 'Quick start guide' }
        ];

        let passed = 0;
        let failed = 0;

        for (const file of requiredFiles) {
            const absPath = resolveWeb(file.path);
            const exists = fs.existsSync(absPath);
            if (exists) {
                passed++;
                console.log(`  ✅ ${file.desc}`);
            } else {
                failed++;
                console.log(`  ❌ ${file.desc} - MISSING: ${file.path}`);
            }
            this.results.push({ test: file.desc, pass: exists, file: file.path });
        }

        console.log(`\n  Result: ${passed}/${requiredFiles.length} files present\n`);
    }

    async testCodeQuality() {
        console.log('🔍 TEST 2: Code Quality Checks\n');
        
        // Check Auth.js for hardcoded credentials
        const authContent = fs.readFileSync(resolveWeb('js/core/Auth.js'), 'utf8');
        const hasHardcodedKey = authContent.includes("'Jeet@123Meet'");
        const hasConfigurableKey = authContent.includes('window.ENV?.MASTER_KEY_ID');
        
        this.results.push({ 
            test: 'Auth.js: No hardcoded credentials', 
            pass: !hasHardcodedKey 
        });
        this.results.push({ 
            test: 'Auth.js: Configurable master key', 
            pass: hasConfigurableKey 
        });
        
        console.log(`  ${!hasHardcodedKey ? '✅' : '❌'} Hardcoded password removed`);
        console.log(`  ${hasConfigurableKey ? '✅' : '❌'} Environment variable support added`);
        
        // Check SettingsModel for server sync
        const settingsContent = fs.readFileSync(resolveWeb('js/models/SettingsModel.js'), 'utf8');
        const hasServerSync = settingsContent.includes('loadFromServer') && 
                             settingsContent.includes('_syncToServer');
        
        this.results.push({ 
            test: 'SettingsModel: Server sync capability', 
            pass: hasServerSync 
        });
        console.log(`  ${hasServerSync ? '✅' : '❌'} Server-side persistence implemented`);
        
        // Check GeofencingView for debounce
        const geoContent = fs.readFileSync(resolveWeb('js/views/GeofencingView.js'), 'utf8');
        const hasDebounce = geoContent.includes('_debounce') && 
                           geoContent.includes('150');
        
        this.results.push({ 
            test: 'GeofencingView: Race condition fix', 
            pass: hasDebounce 
        });
        console.log(`  ${hasDebounce ? '✅' : '❌'} Debounce implementation for race conditions`);
        
        console.log('');
    }

    // [Variables]: endpoints | [Constants]: expectedStatus | [Dependencies]: http, fs, path | [Integration Points]: Express Proxy & Test Harness
    async testAPIEndpoints() {
        console.log('🔌 TEST 3: API Endpoints (Reconciled Suite)\n');
        
        const endpoints = [
            { path: '/api/ping', method: 'GET', expectedStatus: 200 },
            { path: '/api/discovery', method: 'GET', expectedStatus: 200 },
            { path: '/api/devices-live', method: 'GET', expectedStatus: 200 },
            { path: '/api/safe-zones', method: 'GET', expectedStatus: 200 },
            { path: '/api/settings', method: 'GET', expectedStatus: 200 },
            { path: '/api/circles', method: 'GET', expectedStatus: 200 },
            { path: '/api/stats', method: 'GET', expectedStatus: 200 },
            { path: '/api/system-release', method: 'GET', expectedStatus: 200 },
            { path: '/api/archived-devices', method: 'GET', expectedStatus: 200 },
            { path: '/api/duplicate-devices', method: 'GET', expectedStatus: 200 },
            { path: '/api/save-settings', method: 'POST', body: { path: 'config/operator/settings', data: { lastValidated: Date.now() } }, expectedStatus: 200 }
        ];

        for (const endpoint of endpoints) {
            try {
                const response = await this.fetchEndpoint(endpoint.path, endpoint.method, endpoint.body);
                const pass = response.status === endpoint.expectedStatus;
                
                this.results.push({ 
                    test: `${endpoint.method} ${endpoint.path}`, 
                    pass,
                    status: response.status
                });
                console.log(`  ${pass ? '✅' : '❌'} ${endpoint.method} ${endpoint.path} → ${response.status}`);
                
                if (endpoint.method === 'GET' && pass) {
                    try {
                        const data = JSON.parse(response.body);
                        if (data.status === 'SUCCESS') {
                            console.log(`      └─ Status: OK`);
                        }
                    } catch (e) {
                        // Ignore parse errors
                    }
                }
            } catch (error) {
                this.results.push({ 
                    test: `${endpoint.method} ${endpoint.path}`, 
                    pass: false,
                    error: error.message
                });
                console.log(`  ❌ ${endpoint.method} ${endpoint.path} → ERROR: ${error.message}`);
            }
        }
        console.log('');
    }

    async testMobileIntegration() {
        console.log('📱 TEST 4: Mobile Integration\n');
        
        // Check index.html for mobile elements
        const htmlContent = fs.readFileSync(resolveWeb('index.html'), 'utf8');
        const hasMobileCSS = htmlContent.includes('mobile-responsive.css');
        const hasMobileButton = htmlContent.includes('mobile-menu-toggle');
        const hasMobileOverlay = htmlContent.includes('sidebar-overlay');
        
        this.results.push({ test: 'HTML: Mobile CSS linked', pass: hasMobileCSS });
        this.results.push({ test: 'HTML: Mobile menu button', pass: hasMobileButton });
        this.results.push({ test: 'HTML: Sidebar overlay', pass: hasMobileOverlay });
        
        console.log(`  ${hasMobileCSS ? '✅' : '❌'} Mobile CSS included in index.html`);
        console.log(`  ${hasMobileButton ? '✅' : '❌'} Mobile menu toggle button present`);
        console.log(`  ${hasMobileOverlay ? '✅' : '❌'} Sidebar overlay element present`);
        
        // Check CSS content
        const mobileCssPath = resolveWeb('css/mobile-responsive.css');
        if (fs.existsSync(mobileCssPath)) {
            const cssContent = fs.readFileSync(mobileCssPath, 'utf8');
            const hasBreakpoint = cssContent.includes('@media (max-width: 575.98px)');
            const hasTouchTarget = cssContent.includes('44px');
            
            this.results.push({ test: 'CSS: Mobile breakpoint', pass: hasBreakpoint });
            this.results.push({ test: 'CSS: Touch target size', pass: hasTouchTarget });
            
            console.log(`  ${hasBreakpoint ? '✅' : '❌'} Mobile breakpoint (< 576px) defined`);
            console.log(`  ${hasTouchTarget ? '✅' : '❌'} Touch target minimum size (44px)`);
        }
        
        console.log('');
    }

    async testSecurity() {
        console.log('🔐 TEST 5: Security Checks\n');
        
        // Check for hardcoded credentials in Auth.js
        const authContent = fs.readFileSync(resolveWeb('js/core/Auth.js'), 'utf8');
        const noHardcodedPassword = !authContent.includes("'Jeet@123Meet'");
        const hasEnvSupport = authContent.includes('window.ENV');
        const hasValidationError = authContent.includes('INVALID_CREDENTIALS');
        
        this.results.push({ test: 'Auth: No hardcoded password', pass: noHardcodedPassword });
        this.results.push({ test: 'Auth: Environment variable support', pass: hasEnvSupport });
        this.results.push({ test: 'Auth: Input validation', pass: hasValidationError });
        
        console.log(`  ${noHardcodedPassword ? '✅' : '❌'} No hardcoded password in source`);
        console.log(`  ${hasEnvSupport ? '✅' : '❌'} Environment variable support`);
        console.log(`  ${hasValidationError ? '✅' : '❌'} Input validation present`);
        
        // Check CORS configuration
        const serverContent = fs.readFileSync(resolveWeb('js/server.js'), 'utf8');
        const hasCORS = serverContent.includes('cors(') || serverContent.includes('cors()');
        
        this.results.push({ test: 'Server: CORS enabled', pass: hasCORS });
        console.log(`  ${hasCORS ? '✅' : '❌'} CORS middleware configured`);
        
        console.log('');
    }

    async testDocumentation() {
        console.log('📚 TEST 6: Documentation completeness\n');
        
        const docNames = [
            { name: 'DEPLOYMENT_READY.md', desc: 'Deployment guide' },
            { name: 'FINAL_VALIDATION_REPORT.md', desc: 'Validation report' },
            { name: 'WORKFLOW_GUIDE.md', desc: 'Workflow guide' },
            { name: 'QUICK_START.md', desc: 'Quick start' },
            { name: 'ERROR_RECOVERY_GUIDE.md', desc: 'Error recovery' },
            { name: 'KNOWN_ISSUES.md', desc: 'Known issues' }
        ];

        const docs = docNames.map(d => ({
            path: fs.existsSync(resolveWeb(`docs/${d.name}`)) ? `docs/${d.name}` : `.github/${d.name}`,
            desc: d.desc
        }));

        let passed = 0;
        for (const doc of docs) {
            const exists = fs.existsSync(resolveWeb(doc.path));
            if (exists) {
                passed++;
                console.log(`  ✅ ${doc.desc}`);
            } else {
                console.log(`  ❌ ${doc.desc} - MISSING`);
            }
            this.results.push({ test: doc.desc, pass: exists });
        }

        console.log(`\n  Result: ${passed}/${docs.length} documentation files\n`);
    }

    async fetchEndpoint(path, method = 'GET', body = null) {
        return new Promise((resolve, reject) => {
            const options = {
                hostname: '127.0.0.1',
                port: 3000,
                path,
                method,
                headers: { 'Content-Type': 'application/json' }
            };

            options.timeout = 30000;
            const req = http.request(options, (res) => {
                let data = '';
                res.on('data', chunk => data += chunk);
                res.on('end', () => {
                    resolve({ status: res.statusCode, body: data });
                });
            });

            req.on('timeout', () => {
                req.destroy(new Error('Request timed out after 10000ms'));
            });
            req.on('error', reject);
            if (body) req.write(JSON.stringify(body));
            req.end();
        });
    }

    printSummary() {
        const duration = ((Date.now() - this.startTime) / 1000).toFixed(2);
        const passed = this.results.filter(r => r.pass).length;
        const failed = this.results.filter(r => !r.pass).length;
        const total = this.results.length;
        const percentage = Math.round((passed / total) * 100);

        console.log('='.repeat(70));
        console.log('\n📊 VALIDATION SUMMARY\n');
        console.log('-'.repeat(50));
        console.log(`Total Tests:   ${total}`);
        console.log(`Passed:        ${passed} ✅`);
        console.log(`Failed:        ${failed} ❌`);
        console.log(`Success Rate:  ${percentage}%`);
        console.log(`Duration:      ${duration}s`);
        console.log('='.repeat(70));

        if (failed > 0) {
            console.log('\n❌ FAILED TESTS:\n');
            this.results.filter(r => !r.pass).forEach(r => {
                console.log(`  • ${r.test}`);
                if (r.file) console.log(`    File: ${r.file}`);
                if (r.error) console.log(`    Error: ${r.error}`);
            });
            console.log('');
        }

        if (passed === total) {
            console.log('\n🎉 ALL VALIDATIONS PASSED!');
            console.log('   System is fully operational and ready for deployment.\n');
        } else {
            console.log('\n⚠️  Some validations failed. Please review and fix before production.');
            console.log('   See failed tests above for details.\n');
        }

        console.log('Next steps:');
        console.log('  1. Change MASTER_KEY_ID in js/core/Auth.js line ~15');
        console.log('  2. Configure CORS origins for production');
        console.log('  3. Enable HTTPS/SSL certificates');
        console.log('  4. Deploy to production environment\n');

        process.exit(failed > 0 ? 1 : 0);
    }
}

// Run validation
const suite = new ValidationSuite();
suite.run().catch(err => {
    console.error('[FATAL] Validation failed:', err);
    process.exit(1);
});
