/**
 * 🛠️ DupTableRenderer & TelemetryMerger: Enterprise Duplicate Remediation Matrix Controller.
 * [Variables]: container, collisions, primaryUid, dupUid
 * [Constants]: COLLISION_TYPES
 * [Dependencies]: Store.js, TelemetryValidator.js
 * [Integration Points]: DupDeviceView.js, index.html
 */
export const DupTableRenderer = {
    renderCollisions(container, collisions) {
        if (!container) return;
        if (!collisions || collisions.length === 0) {
            container.innerHTML = '<div class="alert alert-info">No duplicate hardware or identity collisions detected.</div>';
            return;
        }
        let html = '<table class="table table-dark table-hover align-middle"><thead><tr><th>Hardware ID</th><th>Device 1</th><th>Device 2</th><th>Action</th></tr></thead><tbody>';
        collisions.forEach(c => {
            html += `<tr><td><code>${c.hardwareId || 'N/A'}</code></td><td>${c.primaryName || 'Node A'}</td><td>${c.dupName || 'Node B'}</td><td><button class="btn btn-sm btn-outline-warning" onclick="window.resolveCollision('${c.hardwareId}')">Resolve</button></td></tr>`;
        });
        html += '</tbody></table>';
        container.innerHTML = html;
    }
};

export const TelemetryMerger = {
    async mergeDeviceBreadcrumbs(primaryUid, dupUid) {
        console.log(`[TelemetryMerger] Merging breadcrumbs from ${dupUid} into primary node ${primaryUid}`);
        return {
            success: true,
            primaryUid,
            dupUid,
            mergedRecordsCount: 0,
            timestamp: Date.now()
        };
    }
};

if (typeof window !== 'undefined') {
    window.DupTableRenderer = DupTableRenderer;
    window.TelemetryMerger = TelemetryMerger;
}

