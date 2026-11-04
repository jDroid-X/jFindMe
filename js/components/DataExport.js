/**
 * DataExport.js - Enterprise Data Export Module
 * Supports CSV, JSON, and PDF export for reports and audits
 */
import { GeoUtils } from '../core/GeoUtils.js';

export class DataExport {
    /**
     * Export devices to CSV
     */
    static exportDevicesToCSV(devices, filename = 'devices_export.csv') {
        if (!devices || devices.length === 0) {
            console.warn('[EXPORT] No devices to export');
            return;
        }
        
        const headers = [
            'Agent ID', 'Name', 'Role', 'Department', 'Status', 
            'Latitude', 'Longitude', 'Speed (km/h)', 'Battery (%)',
            'Last Seen', 'Address'
        ];
        
        const rows = devices.map(d => [
            d.agentId || '',
            `"${(d.name || '').replace(/"/g, '""')}"`,
            d.role || '',
            d.department || '',
            d.isOnline ? 'ONLINE' : 'OFFLINE',
            d.latitude?.toFixed(6) || '',
            d.longitude?.toFixed(6) || '',
            d.speed?.toFixed(1) || '0',
            d.battery >= 0 ? d.battery : 'N/A',
            new Date(d.timestamp || Date.now()).toLocaleString(),
            `"${(d.address || '').replace(/"/g, '""')}"`
        ]);
        
        const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
        this._downloadFile(csv, filename, 'text/csv;charset=utf-8;');
    }

    /**
     * Export devices to JSON
     */
    static exportDevicesToJSON(devices, filename = 'devices_export.json') {
        if (!devices || devices.length === 0) {
            console.warn('[EXPORT] No devices to export');
            return;
        }
        
        const data = devices.map(d => ({
            agentId: d.agentId,
            name: d.name,
            role: d.role,
            department: d.department,
            status: d.isOnline ? 'ONLINE' : 'OFFLINE',
            location: {
                latitude: d.latitude,
                longitude: d.longitude,
                address: d.address
            },
            telemetry: {
                speed: d.speed,
                battery: d.battery,
                timestamp: d.timestamp,
                lastSeen: d.lastSeen
            }
        }));
        
        const json = JSON.stringify(data, null, 2);
        this._downloadFile(json, filename, 'application/json;charset=utf-8;');
    }

    /**
     * Export safe zones to CSV
     */
    static exportSafeZonesToCSV(zones, filename = 'safe_zones_export.csv') {
        if (!zones || zones.length === 0) {
            console.warn('[EXPORT] No zones to export');
            return;
        }
        
        const headers = ['Zone ID', 'Zone Name', 'Type', 'Center Lat', 'Center Lng', 'Radius (m)', 'Active', 'Assigned Devices'];
        
        const rows = zones.map(z => [
            z.zoneId || '',
            `"${(z.zoneName || '').replace(/"/g, '""')}"`,
            z.type || 'circular',
            z.latitude?.toFixed(6) || '',
            z.longitude?.toFixed(6) || '',
            z.radius || 0,
            z.isActive ? 'Yes' : 'No',
            z.connectedDevices?.length || 0
        ]);
        
        const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
        this._downloadFile(csv, filename, 'text/csv;charset=utf-8;');
    }

    /**
     * Export alerts/history to CSV
     */
    static exportAlertsToCSV(alerts, filename = 'alerts_export.csv') {
        if (!alerts || alerts.length === 0) {
            console.warn('[EXPORT] No alerts to export');
            return;
        }
        
        const headers = ['Timestamp', 'Device ID', 'Zone ID', 'Event Type', 'Status', 'Coordinates'];
        
        const rows = alerts.map(a => [
            new Date(a.timestamp || Date.now()).toLocaleString(),
            a.deviceId || '',
            a.zoneId || '',
            a.eventType || '',
            a.status || '',
            `${a.latitude || ''}, ${a.longitude || ''}`
        ]);
        
        const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
        this._downloadFile(csv, filename, 'text/csv;charset=utf-8;');
    }

    /**
     * Generate printable report HTML
     */
    static generateReportHTML(devices, zones, title = 'jButterfly Fleet Report') {
        const now = new Date().toLocaleString();
        
        const html = `
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${title}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        h1 { color: #1e3a5f; }
        table { border-collapse: collapse; width: 100%; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; font-size: 12px; }
        th { background: #1e3a5f; color: white; }
        tr:nth-child(even) { background: #f9f9f9; }
        .online { color: green; font-weight: bold; }
        .offline { color: red; }
        .footer { margin-top: 40px; font-size: 10px; color: #666; text-align: center; }
    </style>
</head>
<body>
    <h1>🦋 ${title}</h1>
    <p>Generated: ${now}</p>
    
    <h2>Fleet Summary</h2>
    <table>
        <tr><th>Agent ID</th><th>Name</th><th>Status</th><th>Speed</th><th>Battery</th><th>Last Seen</th></tr>
        ${devices.map(d => `
            <tr>
                <td>${d.agentId}</td>
                <td>${d.name}</td>
                <td class="${d.isOnline ? 'online' : 'offline'}">${d.isOnline ? 'ONLINE' : 'OFFLINE'}</td>
                <td>${d.speed?.toFixed(1) || 0} km/h</td>
                <td>${d.battery}%</td>
                <td>${new Date(d.timestamp).toLocaleString()}</td>
            </tr>
        `).join('')}
    </table>
    
    <div class="footer">
        jButterfly GPS Telemetry System | ${now}
    </div>
</body>
</html>
        `;
        
        this._downloadFile(html, `${title.replace(/\s+/g, '_')}.html`, 'text/html;charset=utf-8;');
    }

    /**
     * Helper to trigger file download
     */
    static _downloadFile(content, filename, mimeType) {
        const blob = new Blob([content], { type: mimeType });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    }
}

export default DataExport;
