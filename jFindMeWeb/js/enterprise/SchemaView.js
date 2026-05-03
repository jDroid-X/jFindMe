/**
 * Matrix Schema Architect - Visual ERD Engine
 * Features: Infinite Canvas, Regional Zoom, Relation Mapping
 */
export class SchemaView {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.zoom = 1.0;
    }

    render() {
        const schema = [
            { id: 'users', x: 450, y: 100, fields: ['id (PK)', 'name', 'email', 'avatar_id'] },
            { id: 'groups', x: 100, y: 100, fields: ['id (PK)', 'name', 'host (FK)', 'members (FK)'] },
            { id: 'locations', x: 800, y: 100, fields: ['id (UID)', 'lat', 'lng', 'timestamp'] },
            { id: 'history', x: 800, y: 350, fields: ['id (UID)', 'timestamp', 'metadata'] },
            { id: 'pairing_requests', x: 100, y: 350, fields: ['id (PK)', 'fromUID (FK)', 'toEmail', 'status'] }
        ];

        let html = `
            <div class="schema-viewport" id="schema-viewport">
                <div class="zoom-controls">
                    <button class="zoom-btn" onclick="window.adjustZoom(0.1)">+</button>
                    <button class="zoom-btn" onclick="window.adjustZoom(-0.1)">-</button>
                    <button class="zoom-btn" onclick="window.adjustZoom(0, true)">⟲</button>
                </div>
                <div class="schema-canvas" id="schema-canvas" style="transform: scale(${this.zoom})">
                    <!-- Tables -->
                    ${schema.map(node => `
                        <div class="schema-node" style="left: ${node.x}px; top: ${node.y}px">
                            <div class="schema-node-title">${node.id}</div>
                            ${node.fields.map(f => {
                                const [name, type] = f.split(' ');
                                return `<div class="schema-field">
                                    <span>${name}</span>
                                    <span class="field-type">${type || ''}</span>
                                </div>`;
                            }).join('')}
                        </div>
                    `).join('')}

                    <!-- Relationship Lines (SVG Layer) -->
                    <svg style="position: absolute; top: 0; left: 0; width: 1200px; height: 1000px; overflow: visible; pointer-events: none;">
                        <defs>
                            <marker id="arrow" markerWidth="10" markerHeight="10" refX="0" refY="3" orient="auto" markerUnits="strokeWidth">
                                <path d="M0,0 L0,6 L9,3 z" fill="var(--border)" />
                            </marker>
                        </defs>
                        <!-- Relations -->
                        <line x1="320" y1="150" x2="450" y2="150" stroke="var(--border)" stroke-width="2" marker-end="url(#arrow)" />
                        <line x1="670" y1="150" x2="800" y2="150" stroke="var(--border)" stroke-width="2" marker-end="url(#arrow)" />
                        <line x1="670" y1="180" x2="800" y2="350" stroke="var(--border)" stroke-width="2" marker-end="url(#arrow)" />
                        <line x1="320" y1="350" x2="450" y2="210" stroke="var(--border)" stroke-width="2" marker-end="url(#arrow)" />
                    </svg>
                </div>
            </div>
        `;

        this.container.innerHTML = html;
        window.log("SCHEMA: Visual Matrix projected to viewport.", "success");
    }

    setZoom(delta, reset = false) {
        if (reset) this.zoom = 1.0;
        else this.zoom = Math.min(Math.max(0.5, this.zoom + delta), 2.0);
        
        const canvas = document.getElementById('schema-canvas');
        if (canvas) canvas.style.transform = `scale(${this.zoom})`;
    }
}
