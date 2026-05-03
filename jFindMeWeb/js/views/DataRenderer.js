/**
 * View: DataRenderer
 * Purpose: Handles rendering of data structures into HTML tables and lists.
 */
class DataRenderer {
    constructor() {
        this.nodeList = document.getElementById('node-list');
        this.dataContainer = document.getElementById('data-container');
        this.currentNodeName = document.getElementById('current-node-name');
    }

    /**
     * Render the sidebar navigation items
     * @param {Array} nodes - List of root node names
     * @param {function} onSelect - Click handler callback
     */
    renderNodeList(nodes, onSelect) {
        this.nodeList.innerHTML = '';
        nodes.forEach(node => {
            const li = document.createElement('li');
            li.className = 'nav-item';
            li.innerHTML = `
                <i data-lucide="layers" class="nav-icon"></i>
                <span>${node}</span>
            `;
            li.addEventListener('click', () => {
                // Update active state
                document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
                li.classList.add('active');
                this.currentNodeName.textContent = node.toUpperCase();
                onSelect(node);
            });
            this.nodeList.appendChild(li);
        });
        // Re-initialize icons
        if (window.lucide) window.lucide.createIcons();
    }

    /**
     * Render a table based on data object/array
     * @param {object} data - Firebase snapshot value
     * @param {string} nodePath - The current node path
     * @param {object} handlers - { onEdit: func, onDelete: func }
     */
    renderTable(data, nodePath, handlers) {
        if (!data || Object.keys(data).length === 0) {
            this.dataContainer.innerHTML = `
                <div class="welcome-screen">
                    <i data-lucide="inbox" class="welcome-icon"></i>
                    <h2>NO_DATA_FOUND</h2>
                    <p>This node is currently empty or does not exist.</p>
                </div>
            `;
            if (window.lucide) window.lucide.createIcons();
            return;
        }

        // Identify columns (keys of the first object)
        const keys = Object.keys(data);
        const firstValue = data[keys[0]];
        const columns = (typeof firstValue === 'object' && firstValue !== null) 
            ? ['ID', ...Object.keys(firstValue)] 
            : ['ID', 'VALUE'];

        let html = `
            <table class="data-grid">
                <thead>
                    <tr>
                        ${columns.map(col => `<th>${col}</th>`).join('')}
                        <th>ACTIONS</th>
                    </tr>
                </thead>
                <tbody>
        `;

        Object.entries(data).forEach(([id, value]) => {
            html += `<tr class="data-row">`;
            
            // Render columns
            if (typeof value === 'object' && value !== null) {
                html += `<td class="clickable-id" onclick="window.app.handleEdit('${nodePath}/${id}', \`${this._escape(JSON.stringify(value))}\`)">${id}</td>`;
                columns.slice(1).forEach(col => {
                    const cellVal = value[col];
                    html += `<td>${this._formatCell(cellVal)}</td>`;
                });
            } else {
                html += `<td class="clickable-id" onclick="window.app.handleEdit('${nodePath}', \`${this._escape(JSON.stringify(data))}\`)">${id}</td>`;
                html += `<td>${this._formatCell(value)}</td>`;
            }

            // Render Actions
            html += `
                <td class="data-actions">
                    <i data-lucide="edit-3" class="action-icon" onclick="window.app.handleEdit('${nodePath}/${id}', \`${this._escape(JSON.stringify(value))}\`)"></i>
                    <i data-lucide="trash-2" class="action-icon delete" onclick="window.app.handleDelete('${nodePath}/${id}')"></i>
                </td>
            `;

            html += `</tr>`;
        });

        html += `</tbody></table>`;
        this.dataContainer.innerHTML = html;
        if (window.lucide) window.lucide.createIcons();
    }

    _formatCell(val) {
        if (val === null || val === undefined) return '<span class="text-muted">null</span>';
        if (typeof val === 'object') return '<span class="badge badge-pending">OBJECT</span>';
        if (typeof val === 'boolean') return `<span class="badge ${val ? 'badge-active' : 'badge-pending'}">${val}</span>`;
        if (typeof val === 'number' && val > 1000000000000) return new Date(val).toLocaleString(); // Approximate TS check
        return String(val);
    }

    _escape(str) {
        return str.replace(/'/g, "\\'").replace(/"/g, '&quot;');
    }

    /**
     * Render the Specialized Error Log & Diagnostics Screen
     * @param {Array} logs - List of captured errors
     */
    renderErrorLogs(logs) {
        this.currentNodeName.textContent = "SYSTEM_DIAGNOSTICS";
        
        let html = `
            <div class="error-log-container">
                <header class="error-header">
                    <h2><i data-lucide="shield-alert"></i> SYSTEM_HEALTH_STATUS</h2>
                    <p>System records for connectivity and login errors.</p>
                </header>

                <div class="diagnostic-grid">
                    <div class="diagnostic-card glass">
                        <span class="diag-label">ACTIVE_LOGS</span>
                        <span class="diag-value">${logs.length}</span>
                    </div>
                    <div class="diagnostic-card glass">
                        <span class="diag-label">CONNECTION</span>
                        <span class="diag-value success">ESTABLISHED</span>
                    </div>
                </div>

                <div class="error-history">
                    ${logs.length === 0 ? `
                        <div class="no-errors">
                            <i data-lucide="check-circle" class="success-icon"></i>
                            <h3>ZERO_ERRORS_DETECTED</h3>
                            <p>System is running within forensic parameters.</p>
                        </div>
                    ` : `
                        <table class="data-grid">
                            <thead>
                                <tr>
                                    <th>TIMESTAMP</th>
                                    <th>ERROR_SIGNATURE</th>
                                    <th>SUGGESTED_FIX</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${logs.map(err => `
                                    <tr class="data-row">
                                        <td class="text-muted">${err.ts}</td>
                                        <td class="text-danger">${err.msg}</td>
                                        <td><span class="badge badge-pending">${this._getFix(err.msg)}</span></td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    `}
                </div>

                <div class="common-fixes glass">
                    <h3><i data-lucide="wrench"></i> COMMON_RECOVERY_STEPS</h3>
                    <ul>
                        <li><strong>Web Page Not Open:</strong> Run <code>jFireBase.bat</code>, select Option 1. If it fails, check if Port 8080 is blocked by a firewall.</li>
                        <li><strong>Auth Error:</strong> Ensure you are logged into Firebase or check <code>firebase-config.js</code> for API Key validity.</li>
                        <li><strong>CORS Blocked:</strong> Start the server with the <code>--cors</code> flag (included in bat file).</li>
                    </ul>
                </div>
            </div>
        `;

        this.dataContainer.innerHTML = html;
        if (window.lucide) window.lucide.createIcons();
    }

    _getFix(msg) {
        if (msg.includes('connection')) return "Check Network / Bat File Status";
        if (msg.includes('auth')) return "Verify Firebase Config Credentials";
        if (msg.includes('PERMISSION_DENIED')) return "Check Firebase Security Rules";
        if (msg.includes('JSON')) return "Validate JSON Syntax in Editor";
        return "Contact System Architect / Run Diagnostics";
    }
}

export const renderer = new DataRenderer();
