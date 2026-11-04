import { AvatarService } from '../services/AvatarService.js';
import { FeedbackDialog } from '../components/FeedbackDialog.js';

/**
 * jFindMe Matrix - Device Profile & Avatar Edit Modal View (Dynamic OOPS Architecture)
 * Closed-Loop profile editing dialog allowing admins to update avatars, names, contact details,
 * and sync atomically to Firebase RTDB Cloud SSOT.
 * 100% Theme-Adaptive (Dark / Light) with live validations and feedback toasts.
 */
export class DeviceEditModalView {
    constructor(options = {}) {
        this.onSave = options.onSave || null;
        this.modalElement = null;
        this.availableAvatars = [];
    }

    /**
     * Fetches dynamic avatars from server
     */
    async loadAvailableAvatars() {
        try {
            const res = await (window.fetchApi ? window.fetchApi('/api/avatars') : fetch('/api/avatars').then(r => r.json()));
            if (res && Array.isArray(res.avatars) && res.avatars.length > 0) {
                this.availableAvatars = res.avatars;
            }
        } catch (e) {
            console.warn('[DeviceEditModalView] Could not load dynamic avatars, using default presets:', e.message);
        }
    }

    /**
     * Shows the Edit Profile Modal
     * @param {import('../models/DeviceModel.js').DeviceModel} device 
     */
    async show(device) {
        this.close();

        // Pre-fetch avatars if not cached
        if (!this.availableAvatars || this.availableAvatars.length === 0) {
            await this.loadAvailableAvatars();
        }

        const modal = document.createElement('div');
        modal.id = 'device-edit-modal';
        // Agent 3 Mandate: Right-Side Slide-Out Drawer with backdrop dismissal and full viewport height
        modal.className = 'fixed inset-0 z-[1100] flex justify-end bg-black/80 backdrop-blur-sm animate-fade-in font-sans overflow-hidden';

        const currentAvatarId = device.avatarId || "avatar_custom_1";
        let selectedAvatarId = currentAvatarId;

        // Build avatar list: numeric presets 1 to 37 + any server-scanned avatars
        const avatarItems = [];
        const seenIds = new Set();

        if (this.availableAvatars && this.availableAvatars.length > 0) {
            this.availableAvatars.forEach(av => {
                seenIds.add(av.id);
                avatarItems.push({
                    id: av.id,
                    url: av.url,
                    label: av.filename
                });
            });
        }

        // Ensure 1-37 are always present
        for (let i = 1; i <= 37; i++) {
            const numId = `avatar_custom_${i}`;
            const shortId = String(i);
            if (!seenIds.has(numId) && !seenIds.has(shortId)) {
                avatarItems.push({
                    id: numId,
                    url: `/assets/avatars/${i}.png`,
                    label: `Avatar ${i}`
                });
            }
        }

        const rawNode = device.raw || {};
        const profile = rawNode.profile || {};
        const rawDevice = rawNode.device || {};

        const initialDeviceName = device.deviceName || profile.deviceName || rawDevice.device_name || device.model || 'Android Device';
        const initialName = device.name || profile.name || device.agentId;
        const initialEmail = device.email || profile.email || '';
        const initialPhone = device.phone || profile.phone || '';
        const initialDob = device.dob || profile.dob || '';
        const initialGender = device.gender || profile.gender || '';
        const initialDept = device.department || profile.department || profile.dept_name || 'General';
        const initialOrg = profile.org_name || profile.orgName || 'jFindMe Fleet';
        const initialAddress = device.address || profile.address || '';
        const initialEmergencyNote = device.emergencyNote || profile.emergency_note || profile.note || '';
        const initialFamilyRole = device.roleFamily && device.roleFamily !== 'None' ? device.roleFamily : 'Child';
        const initialFriendRole = device.roleFriend && device.roleFriend !== 'None' ? device.roleFriend : 'None';
        const initialGeneralRole = device.role || profile.role || 'Member';
        const initialCarrier = device.carrier || rawDevice.carrier || profile.carrier || rawNode.telemetry?.carrier || '';
        const initialNetworkPref = device.networkPreference || rawDevice.network_preference || profile.network_preference || 'AUTO_ANY_ACTIVE';
        const initialIsDualSim = Boolean(device.isDualSim || rawDevice.is_dual_sim || profile.is_dual_sim);

        modal.innerHTML = `
            <div class="card-theme border-l border-subtle-theme w-full max-w-2xl h-full shadow-2xl overflow-hidden text-primary-theme flex flex-col transform transition-transform duration-300 translate-x-0">
                <!-- Header -->
                <div class="p-3.5 border-b border-subtle-theme flex items-center justify-between surface-theme shrink-0">
                    <div class="flex items-center gap-2.5">
                        <div class="w-8 h-8 rounded-xl bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-sky-400 text-base font-bold shadow-inner">
                            ✏️
                        </div>
                        <div>
                            <h3 class="text-sm font-extrabold tracking-tight text-primary-theme uppercase flex items-center gap-1.5">
                                <span>Edit Device Dossier</span>
                                <span class="badge-status badge-accent text-[8px] px-1 py-0.2">DYNAMIC SSOT</span>
                            </h3>
                            <p class="text-[10px] text-secondary-theme font-mono">
                                Agent ID: <span class="text-sky-400 font-bold">${device.agentId}</span> • Silicon Hardware: <span class="text-primary-theme">${device.deviceId}</span>
                            </p>
                        </div>
                    </div>
                    <button id="btn-close-edit" class="text-muted-theme hover:text-primary-theme p-1.5 rounded-lg text-base font-mono cursor-pointer transition-colors" title="Close dialog">
                        ✕
                    </button>
                </div>

                <!-- Body (Scrollable Smart-Fit) -->
                <div class="p-4 overflow-y-auto space-y-3.5 flex-1 custom-scrollbar text-xs">
                    <!-- Error / Notice Box -->
                    <div id="edit-error-box" class="hidden p-2.5 rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-500 text-xs font-mono font-medium"></div>

                    <!-- AVATAR SELECTION & PREVIEW SECTION -->
                    <div class="p-3 surface-theme rounded-xl border space-y-2">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-1">
                            <label class="text-[11px] font-bold uppercase tracking-wider text-sky-400 flex items-center gap-1.5">
                                <span>🎨 Online Avatar Asset</span>
                                <span class="badge-status badge-accent text-[7.5px] px-1 py-0.2 font-mono">${avatarItems.length} Available</span>
                            </label>
                            <span class="text-[8.5px] text-secondary-theme font-mono">Synced to all circle devices</span>
                        </div>

                        <div class="flex items-center gap-3">
                            <!-- Live Avatar Preview Circle -->
                            <div class="relative shrink-0 flex flex-col items-center">
                                <img id="edit-avatar-preview" src="${typeof device.getAvatarUrl === 'function' ? device.getAvatarUrl() : AvatarService.getAvatarUrl(selectedAvatarId, initialName, device.avatarData)}" 
                                     alt="Preview" class="w-12 h-12 rounded-full object-cover border-2 shadow-md"
                                     style="border-color: ${device.getThemeColor ? device.getThemeColor() : '#38bdf8'};">
                                <span class="text-[8px] font-mono text-muted-theme mt-1 truncate max-w-[64px]" id="edit-avatar-id-label">${selectedAvatarId}</span>
                            </div>

                            <!-- Avatar Grid Picker (Smart Responsive Scroll) -->
                            <div class="flex-1 grid grid-cols-6 sm:grid-cols-10 gap-1.5 p-2 bg-card-subtle rounded-lg border max-h-32 overflow-y-auto custom-scrollbar" id="avatar-picker-grid">
                                ${avatarItems.map(av => {
                                    const isCurrent = (selectedAvatarId === av.id || selectedAvatarId === `avatar_custom_${av.id}` || selectedAvatarId === av.filename);
                                    return `
                                        <div class="avatar-option-card relative cursor-pointer rounded-full p-0.5 border-2 transition-all shrink-0 ${isCurrent ? 'border-sky-500 bg-sky-500/20 scale-105 shadow-md shadow-sky-500/30' : 'border-transparent hover:border-sky-400/50 hover:scale-105'}"
                                             data-avatar-id="${av.id}" data-avatar-url="${av.url}" title="${av.label}">
                                            <img src="${av.url}" class="w-7 h-7 rounded-full object-cover bg-transparent"
                                                 onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><circle cx=%2250%22 cy=%2250%22 r=%2248%22 fill=%22%230f172a%22/><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2235%22>👤</text></svg>'">
                                            ${isCurrent ? '<span class="absolute -top-1 -right-1 w-3 h-3 bg-sky-500 rounded-full flex items-center justify-center text-[7px] font-bold text-white shadow">✓</span>' : ''}
                                        </div>
                                    `;
                                }).join('')}
                            </div>
                        </div>

                        <!-- Custom Avatar Identifier / URL Write-In -->
                        <div class="flex items-center gap-2 pt-1 border-t border-subtle-theme">
                            <span class="text-[9px] font-mono text-secondary-theme shrink-0">Custom URL / Identifier:</span>
                            <input type="text" id="edit-custom-avatar-url" placeholder="e.g. /assets/avatars/ButterFly.png or https://..." 
                                   value="${selectedAvatarId.startsWith('http') || selectedAvatarId.endsWith('.png') ? selectedAvatarId : ''}"
                                   class="input-theme flex-1 py-0.5 px-2 text-[10px] font-mono">
                            <button type="button" id="btn-apply-custom-avatar" class="btn-action-secondary text-[9px] py-0.5 px-2 shrink-0">
                                Apply
                            </button>
                        </div>
                    </div>

                    <!-- FORM FIELDS: 2-COLUMN ENTERPRISE DOSSIER -->
                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                        
                        <!-- Persona User Name -->
                        <div>
                            <label class="text-[10px] font-bold uppercase tracking-wider text-secondary-theme block mb-0.5">User / Persona Name *</label>
                            <input type="text" id="edit-name" value="${initialName}" placeholder="e.g. Pushpa Citizen"
                                   class="input-theme w-full py-1 px-2 text-xs font-semibold">
                        </div>

                        <!-- Device Model / Friendly Hardware Name -->
                        <div>
                            <label class="text-[10px] font-bold uppercase tracking-wider text-secondary-theme block mb-0.5">Device Hardware Name / Model *</label>
                            <input type="text" id="edit-device-name" value="${initialDeviceName}" placeholder="e.g. Redmi Note 5 Pro"
                                   class="input-theme w-full py-1 px-2 text-xs font-semibold">
                        </div>

                        <!-- Family Circle Role -->
                        <div>
                            <label class="text-[10px] font-bold uppercase tracking-wider text-amber-500 block mb-0.5">👨‍👩‍👧‍👦 Family Circle Role</label>
                            <select id="edit-role-family" class="input-theme w-full py-1 px-2 text-xs font-medium cursor-pointer">
                                ${["Child", "Father", "Mother", "Guardian", "Spouse", "Son", "Daughter", "Grandparent", "Sibling", "Admin", "Member", "None"].map(r => {
                                    const isSel = initialFamilyRole.toLowerCase() === r.toLowerCase();
                                    return `<option value="${r}" ${isSel ? 'selected' : ''}>${r}</option>`;
                                }).join('')}
                            </select>
                        </div>

                        <!-- Friends Circle Role -->
                        <div>
                            <label class="text-[10px] font-bold uppercase tracking-wider text-emerald-500 block mb-0.5">🤝 Friends Circle Role</label>
                            <select id="edit-role-friend" class="input-theme w-full py-1 px-2 text-xs font-medium cursor-pointer">
                                ${["Friend", "Colleague", "Peer", "Emergency Contact", "Buddy", "None"].map(r => {
                                    const isSel = initialFriendRole.toLowerCase() === r.toLowerCase();
                                    return `<option value="${r}" ${isSel ? 'selected' : ''}>${r}</option>`;
                                }).join('')}
                            </select>
                        </div>

                        <!-- Phone Number -->
                        <div>
                            <label class="text-[10px] font-bold uppercase tracking-wider text-secondary-theme block mb-0.5">Phone Number</label>
                            <input type="tel" id="edit-phone" value="${initialPhone}" placeholder="e.g. +91 9876543210"
                                   class="input-theme w-full py-1 px-2 text-xs font-mono">
                        </div>

                        <!-- Email Address -->
                        <div>
                            <label class="text-[10px] font-bold uppercase tracking-wider text-secondary-theme block mb-0.5">Email Address</label>
                            <input type="email" id="edit-email" value="${initialEmail}" placeholder="e.g. user@domain.com"
                                   class="input-theme w-full py-1 px-2 text-xs font-mono">
                        </div>

                        <!-- Date of Birth (DOB) -->
                        <div>
                            <label class="text-[10px] font-bold uppercase tracking-wider text-secondary-theme block mb-0.5">Date of Birth (DOB)</label>
                            <input type="date" id="edit-dob" value="${initialDob}"
                                   class="input-theme w-full py-1 px-2 text-xs font-mono">
                        </div>

                        <!-- Gender -->
                        <div>
                            <label class="text-[10px] font-bold uppercase tracking-wider text-secondary-theme block mb-0.5">Gender</label>
                            <select id="edit-gender" class="input-theme w-full py-1 px-2 text-xs font-medium cursor-pointer">
                                ${["Not Specified", "Male", "Female", "Other"].map(g => {
                                    const isSel = initialGender.toLowerCase() === g.toLowerCase();
                                    return `<option value="${g}" ${isSel ? 'selected' : ''}>${g}</option>`;
                                }).join('')}
                            </select>
                        </div>

                        <!-- Organization Name -->
                        <div>
                            <label class="text-[10px] font-bold uppercase tracking-wider text-secondary-theme block mb-0.5">Organization / Circle Hierarchy</label>
                            <input type="text" id="edit-org" value="${initialOrg}" placeholder="e.g. jFindMe Fleet"
                                   class="input-theme w-full py-1 px-2 text-xs">
                        </div>

                        <!-- Department / Unit -->
                        <div>
                            <label class="text-[10px] font-bold uppercase tracking-wider text-secondary-theme block mb-0.5">Department / Operational Unit</label>
                            <input type="text" id="edit-dept" value="${initialDept}" placeholder="e.g. Field Operations"
                                   class="input-theme w-full py-1 px-2 text-xs">
                        </div>

                        <!-- Postal / Physical Address (Span 2) -->
                        <div class="sm:col-span-2">
                            <label class="text-[10px] font-bold uppercase tracking-wider text-secondary-theme block mb-0.5">Base Address / Primary Location</label>
                            <input type="text" id="edit-address" value="${initialAddress}" placeholder="e.g. 101 Marine Lines, Pune"
                                   class="input-theme w-full py-1 px-2 text-xs">
                        </div>

                        <!-- Emergency Medical Note (Span 2) -->
                        <div class="sm:col-span-2">
                            <label class="text-[10px] font-bold uppercase tracking-wider text-amber-500 block mb-0.5">🚨 Emergency Medical / Critical Note</label>
                            <input type="text" id="edit-note" value="${initialEmergencyNote}" placeholder="e.g. Blood Group O+, Allergy: Penicillin, ICE: +91 9999999999"
                                   class="input-theme w-full py-1 px-2 text-xs">
                        </div>
                    </div>

                    <!-- CELLULAR & NETWORK TELEMETRY ROUTING CONFIGURATION (HUMAN-IN-THE-LOOP CONTROL) -->
                    <div class="p-3 surface-theme rounded-xl border space-y-2">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-1">
                            <label class="text-[10.5px] font-bold uppercase tracking-wider text-emerald-400 block flex items-center gap-1.5">
                                <span>📶 Cellular & Network Telemetry Routing</span>
                                <span class="badge-status badge-success text-[7.5px]">LIVE NETWORK</span>
                            </label>
                            <span class="text-[8.5px] text-secondary-theme font-mono">Dynamic Routing & SIM Configuration</span>
                        </div>
                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-2.5 text-xs">
                            <!-- Cellular Carrier / Operator -->
                            <div>
                                <label class="text-[10px] font-bold uppercase tracking-wider text-secondary-theme block mb-0.5">Cellular Carrier / Network Operator</label>
                                <input type="text" id="edit-carrier" value="${initialCarrier}" placeholder="e.g. Jio 4G, Airtel, VI, BSNL"
                                       class="input-theme w-full py-1 px-2 text-xs font-semibold">
                            </div>

                            <!-- Network Routing Preference -->
                            <div>
                                <label class="text-[10px] font-bold uppercase tracking-wider text-secondary-theme block mb-0.5">Network Routing Preference (OTA & Sync)</label>
                                <select id="edit-network-preference" class="input-theme w-full py-1 px-2 text-xs font-medium cursor-pointer">
                                    <option value="AUTO_ANY_ACTIVE" ${initialNetworkPref === 'AUTO_ANY_ACTIVE' ? 'selected' : ''}>⚡ Auto (Active Mobile Data or Wi-Fi)</option>
                                    <option value="MOBILE_DATA_ONLY" ${initialNetworkPref === 'MOBILE_DATA_ONLY' ? 'selected' : ''}>📶 Mobile Cellular Data Only</option>
                                    <option value="WIFI_ONLY" ${initialNetworkPref === 'WIFI_ONLY' ? 'selected' : ''}>📡 Wi-Fi Only (Conserve Cellular Data)</option>
                                </select>
                            </div>

                            <!-- Dual SIM Active Switch -->
                            <div class="flex items-center justify-between p-1.5 px-2 rounded-lg bg-card-subtle border border-subtle-theme sm:col-span-2">
                                <div class="flex flex-col">
                                    <span class="text-primary-theme font-bold text-[10.5px]">Dual SIM Mode Enabled</span>
                                    <span class="text-[9px] text-secondary-theme font-mono">Device has secondary SIM slot active (Slot 2)</span>
                                </div>
                                <label class="switch-toggle switch-sm">
                                    <input type="checkbox" id="edit-is-dual-sim" ${initialIsDualSim ? 'checked' : ''}>
                                    <span class="switch-slider"></span>
                                </label>
                            </div>
                        </div>
                    </div>

                    <!-- ACL ACCESS GRANTS TACTILE SWITCHES -->
                    <div class="p-3 surface-theme rounded-xl border space-y-2">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-1">
                            <label class="text-[10.5px] font-bold uppercase tracking-wider text-sky-400 block">Security Access Grants (ACL)</label>
                            <span class="badge-status badge-accent text-[7.5px]">CLOSED LOOP POLICY</span>
                        </div>
                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                            <div class="flex items-center justify-between p-1.5 px-2 rounded-lg bg-card-subtle border border-subtle-theme">
                                <span class="text-secondary-theme font-medium text-[10.5px]">Live GPS Tracking</span>
                                <label class="switch-toggle switch-sm">
                                    <input type="checkbox" id="acl-live-tracking" ${device.raw?.acl?.acl_live_tracking === 'DENIED' ? '' : 'checked'}>
                                    <span class="switch-slider"></span>
                                </label>
                            </div>
                            <div class="flex items-center justify-between p-1.5 px-2 rounded-lg bg-card-subtle border border-subtle-theme">
                                <span class="text-secondary-theme font-medium text-[10.5px]">History Trace Audit</span>
                                <label class="switch-toggle switch-sm">
                                    <input type="checkbox" id="acl-history-audit" ${device.raw?.acl?.acl_history_audit === 'DENIED' ? '' : 'checked'}>
                                    <span class="switch-slider"></span>
                                </label>
                            </div>
                            <div class="flex items-center justify-between p-1.5 px-2 rounded-lg bg-card-subtle border border-subtle-theme">
                                <span class="text-secondary-theme font-medium text-[10.5px]">Safe Zone Breach Alerts</span>
                                <label class="switch-toggle switch-sm">
                                    <input type="checkbox" id="acl-geofence" ${device.raw?.acl?.acl_geofence === 'DENIED' ? '' : 'checked'}>
                                    <span class="switch-slider"></span>
                                </label>
                            </div>
                            <div class="flex items-center justify-between p-1.5 px-2 rounded-lg bg-card-subtle border border-subtle-theme">
                                <span class="text-secondary-theme font-medium text-[10.5px]">Ghost SOS Override</span>
                                <label class="switch-toggle switch-sm">
                                    <input type="checkbox" id="acl-ghost-override" ${device.raw?.acl?.acl_ghost_override === 'DENIED' ? '' : 'checked'}>
                                    <span class="switch-slider"></span>
                                </label>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Footer Action Buttons -->
                <div class="p-3 border-t border-subtle-theme surface-theme flex items-center justify-end gap-2 shrink-0">
                    <button id="btn-cancel-edit" class="btn-action-secondary text-xs py-1 px-3">
                        Cancel
                    </button>
                    <button id="btn-save-edit" class="btn-action-primary text-xs py-1 px-4 flex items-center gap-1.5 font-bold shadow-md">
                        <span>💾 Synchronize Dossier</span>
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
        this.modalElement = modal;

        // --- BIND INTERACTION EVENTS ---
        const previewEl = modal.querySelector('#edit-avatar-preview');
        const previewLabel = modal.querySelector('#edit-avatar-id-label');
        const customUrlInput = modal.querySelector('#edit-custom-avatar-url');

        // Avatar Option Card Clicks
        modal.querySelectorAll('.avatar-option-card').forEach(card => {
            card.addEventListener('click', () => {
                modal.querySelectorAll('.avatar-option-card').forEach(c => {
                    c.classList.remove('border-sky-500', 'bg-sky-500/20', 'scale-105', 'shadow-md', 'shadow-sky-500/30');
                    c.classList.add('border-transparent');
                    const badge = c.querySelector('span');
                    if (badge) badge.remove();
                });
                card.classList.remove('border-transparent');
                card.classList.add('border-sky-500', 'bg-sky-500/20', 'scale-105', 'shadow-md', 'shadow-sky-500/30');

                const checkBadge = document.createElement('span');
                checkBadge.className = 'absolute -top-1 -right-1 w-3 h-3 bg-sky-500 rounded-full flex items-center justify-center text-[7px] font-bold text-white shadow';
                checkBadge.textContent = '✓';
                card.appendChild(checkBadge);

                selectedAvatarId = card.getAttribute('data-avatar-id');
                const avatarUrl = card.getAttribute('data-avatar-url') || AvatarService.getAvatarUrl(selectedAvatarId, initialName);
                if (previewEl) previewEl.src = avatarUrl;
                if (previewLabel) previewLabel.textContent = selectedAvatarId;
                if (customUrlInput) customUrlInput.value = '';
            });
        });

        // Apply Custom Avatar URL
        modal.querySelector('#btn-apply-custom-avatar')?.addEventListener('click', () => {
            const customVal = customUrlInput ? customUrlInput.value.trim() : '';
            if (customVal) {
                selectedAvatarId = customVal;
                if (previewEl) previewEl.src = customVal;
                if (previewLabel) previewLabel.textContent = customVal.length > 15 ? customVal.slice(0, 12) + '...' : customVal;
                modal.querySelectorAll('.avatar-option-card').forEach(c => {
                    c.classList.remove('border-sky-500', 'bg-sky-500/20', 'scale-105', 'shadow-md', 'shadow-sky-500/30');
                    c.classList.add('border-transparent');
                    const badge = c.querySelector('span');
                    if (badge) badge.remove();
                });
            }
        });

        // Close handlers
        this._escapeHandler = (e) => {
            if (e.key === 'Escape') this.close();
        };
        window.addEventListener('keydown', this._escapeHandler);

        modal.querySelector('#btn-close-edit')?.addEventListener('click', () => this.close());
        modal.querySelector('#btn-cancel-edit')?.addEventListener('click', () => this.close());
        modal.addEventListener('click', (e) => {
            if (e.target === modal) this.close();
        });

        // Save handler
        modal.querySelector('#btn-save-edit')?.addEventListener('click', async () => {
            const name = modal.querySelector('#edit-name').value.trim();
            const deviceName = modal.querySelector('#edit-device-name').value.trim();
            const phone = modal.querySelector('#edit-phone').value.trim();
            const email = modal.querySelector('#edit-email').value.trim();
            const dob = modal.querySelector('#edit-dob').value.trim();
            const gender = modal.querySelector('#edit-gender').value;
            const roleFamily = modal.querySelector('#edit-role-family').value;
            const roleFriend = modal.querySelector('#edit-role-friend').value;
            const orgName = modal.querySelector('#edit-org').value.trim();
            const dept = modal.querySelector('#edit-dept').value.trim();
            const address = modal.querySelector('#edit-address').value.trim();
            const note = modal.querySelector('#edit-note').value.trim();
            const carrier = modal.querySelector('#edit-carrier') ? modal.querySelector('#edit-carrier').value.trim() : '';
            const networkPreference = modal.querySelector('#edit-network-preference') ? modal.querySelector('#edit-network-preference').value : 'AUTO_ANY_ACTIVE';
            const isDualSim = modal.querySelector('#edit-is-dual-sim') ? modal.querySelector('#edit-is-dual-sim').checked : false;

            const errorBox = modal.querySelector('#edit-error-box');
            const saveBtn = modal.querySelector('#btn-save-edit');

            if (!name) {
                errorBox.textContent = "Validation Error: User / Persona Name cannot be blank.";
                errorBox.classList.remove('hidden');
                return;
            }

            if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                errorBox.textContent = "Validation Error: Please enter a valid email address.";
                errorBox.classList.remove('hidden');
                return;
            }

            errorBox.classList.add('hidden');
            saveBtn.disabled = true;
            saveBtn.innerHTML = `<span>⏳ Synchronizing...</span>`;

            const aclLive = modal.querySelector('#acl-live-tracking') ? modal.querySelector('#acl-live-tracking').checked : true;
            const aclHistory = modal.querySelector('#acl-history-audit') ? modal.querySelector('#acl-history-audit').checked : true;
            const aclGeofence = modal.querySelector('#acl-geofence') ? modal.querySelector('#acl-geofence').checked : true;
            const aclGhost = modal.querySelector('#acl-ghost-override') ? modal.querySelector('#acl-ghost-override').checked : true;

            const primaryRole = roleFamily !== 'None' ? roleFamily : (roleFriend !== 'None' ? roleFriend : initialGeneralRole);

            const payload = {
                name,
                deviceName: deviceName || initialDeviceName,
                device_name: deviceName || initialDeviceName,
                avatar_id: selectedAvatarId,
                avatarId: selectedAvatarId,
                phone,
                carrier,
                network_preference: networkPreference,
                networkPreference,
                is_dual_sim: isDualSim,
                isDualSim,
                sim_status: (phone || carrier) ? (isDualSim ? `Dual SIM Active${carrier ? ` (${carrier})` : ''} • ${phone}` : `SIM Active${carrier ? ` (${carrier})` : ''} • ${phone}`) : (isDualSim ? "Dual SIM Active" : "No SIM Detected"),
                email,
                dob,
                gender,
                department: dept,
                dept_name: dept,
                org_name: orgName,
                address,
                emergency_note: note,
                note,
                role: primaryRole,
                role_family: roleFamily,
                roleFamily,
                role_friend: roleFriend,
                roleFriend,
                acl: {
                    acl_live_tracking: aclLive ? "GRANTED" : "DENIED",
                    acl_history_audit: aclHistory ? "GRANTED" : "DENIED",
                    acl_geofence: aclGeofence ? "GRANTED" : "DENIED",
                    acl_ghost_override: aclGhost ? "GRANTED" : "DENIED"
                }
            };

            if (this.onSave) {
                try {
                    await this.onSave(device.agentId, payload);
                    this.close();
                    FeedbackDialog.showToast(`Dossier for [${name}] synchronized to online RTDB SSOT!`, "success");
                    FeedbackDialog.showSlideNotification({
                        title: "Closed-Loop Sync Complete",
                        message: `Profile [${name}] (${deviceName}, Avatar: ${selectedAvatarId}) synchronized atomically to online RTDB SSOT.`,
                        type: "success",
                        duration: 4500
                    });
                } catch (err) {
                    errorBox.textContent = `Sync Fault: ${err.message}`;
                    errorBox.classList.remove('hidden');
                    saveBtn.disabled = false;
                    saveBtn.innerHTML = `<span>💾 Synchronize Dossier</span>`;
                    FeedbackDialog.showToast(`Profile update failed: ${err.message}`, "error");
                }
            }
        });
    }

    close() {
        if (this._escapeHandler) {
            window.removeEventListener('keydown', this._escapeHandler);
            this._escapeHandler = null;
        }
        if (this.modalElement) {
            this.modalElement.remove();
            this.modalElement = null;
        }
    }
}
