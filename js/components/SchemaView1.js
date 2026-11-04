import { SchemaView } from './SchemaView.js';

/**
 * ==============================================================================================
 * DATABASE SCHEMA ARCHITECT V2 - HIERARCHICAL RELATIONAL ER EXTENSION
 * Enterprise Standard v5.0 | Codename: jDroid-X | Operator: JEET
 * Inherits and extends master SchemaView relational engine
 * ==============================================================================================
 */
export class SchemaView1 extends SchemaView {
    constructor(containerId = 'workspace-root') {
        super(containerId);
    }

    async render() {
        // Point global handler alias to this instance for toolbar actions
        if (window.appInstance) {
            window.appInstance.schema1 = this;
            window.appInstance.schema = this;
        }
        await super.render();
    }
}
