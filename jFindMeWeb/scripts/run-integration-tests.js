#!/usr/bin/env node
/**
 * jButterfly Integration Test Suite
 * Comprehensive validation of all components
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

class IntegrationTestSuite {
    constructor() {
        this.results = [];
        this.baseURL = 'http://127.0.0.1:3000';
        this.startTime = Date.now();
    }

    async run() {
        console.log('\n🚀 Starting jButterfly Integration Test Suite\n');
        console.log('=' .repeat(60));

        await this.testFileStructure();
        await this.testComponentImports();
        await this.testAPIEndpoints();
        await this.testBackendHealth();
        await this.testDatabaseConnections();

        this.printSummary();
    }

    async testFileStructure() {
        console.log('\n📁 Testing File Structure...');
        
        const requiredFiles = [
            'index.html',
            'js/app.js',
            'js/core/Auth.js',
            'js/core/SettingsModel.js',
            'js/controllers/MapController.js',
            'js/views/GeofencingView.js',
            'css/mobile-responsive.css',
            'js/components/KeyboardShortcuts.js',
            'js/components/DeviceRegistrationWizard.js',
            'js/components/DataExport.js',
            'js/core/OfflineQueue.js',
            'js/core/ErrorRecovery.js'
        ];

        let allPassed = true;
        for (const file of requiredFiles) {
            const exists = fs.existsSync(file);
            if (!exists) allPassed = false;
            this.results.push({ test: `File exists: ${file}`, pass: exists });
            console.log(`  ${exists ? '✅' : '❌'} ${file}`);
        }

        console.log(`\n  Result: ${allPassed ? 'PASS' : 'FAIL'}\n`);
    }

    async testComponentImports() {
        console.log('📦 Testing Component Imports...');
        
        // Check Auth.js for hardcoded credentials
        const authContent = fs.readFileSync('js/core/Auth.js', 'utf8');
        const hasHardcodedKey = authContent.includes("'Jeet@123Meet'");
        const hasConfigurableKey = authContent.includes("window.ENV?.MASTER_KEY_ID");
        
        this.results.push({ test: 'Auth.js: No hardcoded credentials', pass: !hasHardcodedKey });
        this.results.push({ test: 'Auth.js: Configurable master key', pass: hasConfigurableKey });
        
        console.log(`  ${!hasHardcodedKey ? '✅' : '❌'} Hardcoded credentials removed`);
        console.log(`  ${hasConfigurableKey ? '✅' : '❌'} Configurable master key added`);
        console.log('');
    }

    async testAPIEndpoints() {
        console.log('🔌 Testing API Endpoints...');
        
        const endpoints = [
            { path: '/api/discovery', method: 'GET' },
            { path: '/api/devices-live', method: 'GET' },
            { path: '/api/safe-zones', method: 'GET' },
            { path: '/api/save-settings', method: 'POST' }
        ];

        for (const endpoint of endpoints) {
            try {
                const response = await this.fetchEndpoint(endpoint.path, endpoint.method);
                const status = response.status;
                const pass = status >= 200 && status < 300;
                
                this.results.push({ test: `${endpoint.method} ${endpoint.path}`, pass });
                console.log(`  ${pass ? '✅' : '❌'} ${endpoint.method} ${endpoint.path} (${status})`);
            } catch (error) {
                this.results.push({ test: `${endpoint.method} ${endpoint.path}`, pass: false });
                console.log(`  ❌ ${endpoint.method} ${endpoint.path} (${error.message})`);
            }
        }
        console.log('');
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

            const req = http.request(options, (res) => {
                let data = '';
                res.on('data', chunk => data += chunk);
                res.on('end', () => {
                    resolve({ status: res.statusCode, body: data });
                });
            });

            req.on('error', reject);
            if (body) req.write(JSON.stringify(body));
            req.end();
        });
    }

    async testBackendHealth() {
        console.log('💻 Testing Backend Health...');
        
        // Check if server is running
        try {
            const response = await this.fetchEndpoint('/api/discovery');
            const data = JSON.parse(response.body);
            
            const pass = response.status === 200 && data.projectId;
            this.results.push({ test: 'Backend is responsive', pass });
            console.log(`  ${pass ? '✅' : '❌'} Server responding: ${data.projectId || 'unknown'}`);
            
            if (data.rtdb) {
                console.log(`  ℹ️  RTDB collections: ${data.rtdb.length}`);
            }
        } catch (error) {
            this.results.push({ test: 'Backend is responsive', pass: false });
            console.log(`  ❌ Backend not accessible: ${error.message}`);
        }
        console.log('');
    }

    async testDatabaseConnections() {
        console.log('🗄️  Testing Database Connections...');
        
        // Test Firebase connectivity via API
        try {
            const response = await this.fetchEndpoint('/api/devices-live');
            const data = JSON.parse(response.body);
            
            const pass = response.status === 200 && data.status === 'SUCCESS';
            this.results.push({ test: 'Firebase devices endpoint', pass });
            console.log(`  ${pass ? '✅' : '❌'} Devices API: ${data.count || 0} devices found`);
            
            // Test safe zones
            const zonesResponse = await this.fetchEndpoint('/api/safe-zones');
            const zonesData = JSON.parse(zonesResponse.body);
            const zonesPass = zonesResponse.status === 200 && zonesData.status === 'SUCCESS';
            this.results.push({ test: 'Firebase zones endpoint', pass: zonesPass });
            console.log(`  ${zonesPass ? '✅' : '❌'} Zones API: ${zonesData.count || 0} zones found`);
            
        } catch (error) {
            this.results.push({ test: 'Firebase connectivity', pass: false });
            console.log(`  ❌ Firebase connection error: ${error.message}`);
        }
        console.log('');
    }

    printSummary() {
        const duration = ((Date.now() - this.startTime) / 1000).toFixed(2);
        const passed = this.results.filter(r => r.pass).length;
        const failed = this.results.filter(r => !r.pass).length;
        const total = this.results.length;
        const percentage = Math.round((passed / total) * 100);

        console.log('=' .repeat(60));
        console.log('\n📊 TEST SUMMARY');
        console.log('-'.repeat(40));
        console.log(`Total Tests:  ${total}`);
        console.log(`Passed:       ${passed} ✅`);
        console.log(`Failed:       ${failed} ❌`);
        console.log(`Success Rate: ${percentage}%`);
        console.log(`Duration:     ${duration}s`);
        console.log('='.repeat(60));

        if (failed > 0) {
            console.log('\n❌ FAILED TESTS:');
            this.results.filter(r => !r.pass).forEach(r => {
                console.log(`  • ${r.test}`);
            });
        }

        if (passed === total) {
            console.log('\n🎉 ALL TESTS PASSED! System is ready for production.');
        } else {
            console.log('\n⚠️  Some tests failed. Please review and fix before deployment.');
        }

        console.log('\n');
        process.exit(failed > 0 ? 1 : 0);
    }
}

// Run tests
const suite = new IntegrationTestSuite();
suite.run().catch(err => {
    console.error('Test suite error:', err);
    process.exit(1);
});
