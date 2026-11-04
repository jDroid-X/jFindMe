/**
 * [Variables]: start, data, duration, j
 * [Constants]: http, urls
 * [Dependencies]: node http module
 * [Integration Points]: Express Proxy (3000), Static Server (8080), Firebase RTDB endpoints
 */
const http = require('http');

function test(name, url) {
    return new Promise((resolve) => {
        const start = Date.now();
        http.get(url, (res) => {
            let data = '';
            res.on('data', c => data += c);
            res.on('end', () => {
                const duration = Date.now() - start;
                console.log(`[${name}] STATUS: ${res.statusCode} | BYTES: ${data.length} | TIME: ${duration}ms`);
                if (url.includes('compact-live')) {
                    try {
                        const j = JSON.parse(data);
                        console.log(`  -> Devices Count: ${j.count}, withGps: ${j.withGps}`);
                        console.log(`  -> Fleet: ${j.devices.map(d => d.agentId + ' (' + d.name + '): ' + d.ragStatus).join(', ')}`);
                    } catch(e) {}
                }
                resolve(res.statusCode);
            });
        }).on('error', (err) => {
            console.error(`[${name}] FAILED: ${err.message}`);
            resolve(500);
        });
    });
}

(async () => {
    console.log('====================================================');
    console.log('   jFindMeWeb UNIFIED SINGLE-PORT (3000) VERIFICATION');
    console.log('====================================================');
    await test('API: compact-live on 3000', 'http://127.0.0.1:3000/api/fleet/compact-live');
    await test('STATIC: Web UI on 3000', 'http://127.0.0.1:3000/index.html');
    await test('JS: App Module on 3000', 'http://127.0.0.1:3000/js/app.js');
    await test('AVATAR: Asset on 3000', 'http://127.0.0.1:3000/assets/avatars/1.png');
    await test('GEO: Reverse Geocode on 3000', 'http://127.0.0.1:3000/api/reverse-geocode?lat=18.5788&lng=73.8887');
    console.log('====================================================');
    console.log('   UNIFIED PORT 3000 VERIFICATION COMPLETED');
    console.log('====================================================');
    process.exit(0);
})();
