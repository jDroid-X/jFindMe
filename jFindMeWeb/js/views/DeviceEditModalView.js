import { AvatarService } from '../services/AvatarService.js';

/**
 * jFindMe Matrix - Device Profile & Avatar Edit Modal View (OOPS Architecture)
 * Closed-Loop profile editing dialog allowing admins to update avatars, names, contact details,
 * and sync atomically to Firebase RTDB.
 */
export class DeviceEditModalView {
    constructor(options = {}) {
        this.onSave = options.onSave || null;
        this.modalElement = null;
    }

    /**
     * Shows the Edit Profile Modal
     * @param {import('../models/DeviceModel.js').DeviceModel} device 
     */
    show(device) {
        this.close();

        const modal = document.createElement('div');
        modal.id = 'device-edit-modal';
        modal.className = 'fixed inset-0 z-[1100] flex items-center justify-center bg-black/85 backdrop-blur-md p-4 animate-fade-in font-sans';

        // Available Avatars 1 to 37
        const avatarOptions = Array.from({ length: 37 }, (_, i) => i + 1);
        const currentAvatarId = device.avatarId || "avatar_custom_1";
        let selectedAvatarId = currentAvatarId;

        modal.innerHTML = `
            <div class="bg-slate-900 border border-white/10 rounded-2xl w-full max-w-2xl shadow-2xl overflow-hidden text-white flex flex-col max-h-[92vh]">
                <!-- Header -->
                <div class="p-5 border-b border-white/10 flex items-center justify-between bg-slate-950/60">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-xl bg-sky-500/20 border border-sky-500/30 flex items-center justify-center text-sky-400 text-lg font-bold">
                            ✏️
                        </div>
                        <div>
                            <h3 class="text-base font-black tracking-tight text-white uppercase">Edit Device Dossier</h3>
                            <p class="text-xs text-slate-400 font-mono">Agent ID: ${device.agentId} • Hardware: ${device.deviceId}</p>
                        </div>
                    </div>
                    <button id="btn-close-edit" class="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-white/5 transition-all text-xl font-mono">
                        ✕
                    </button>
                </div>

                <!-- Body -->
                <div class="p-6 overflow-y-auto space-y-5 flex-1 custom-scrollbar">
                    <!-- Error / Notice Box -->
                    <div id="edit-error-box" class="hidden p-3 rounded-lg bg-red-950/80 border border-red-500/50 text-red-200 text-xs font-mono"></div>

                    <!-- AVATAR PICKER GRID -->
                    <div class="space-y-2">
                        <label class="text-xs font-bold uppercase tracking-wider text-sky-400 block">Select Avatar Icon (1 - 37)</label>
                        <div class="grid grid-cols-6 sm:grid-cols-9 gap-2 p-3 bg-black/40 rounded-xl border border-white/5 max-h-44 overflow-y-auto custom-scrollbar" id="avatar-picker-grid">
                            ${avatarOptions.map(num => {
                                const optId = `avatar_custom_${num}`;
                                const isCurrent = currentAvatarId === optId || currentAvatarId === String(num);
                                return `
                                    <div class="avatar-option-card relative cursor-pointer rounded-full p-1 border-2 transition-all ${isCurrent ? 'border-sky-400 bg-sky-500/20 scale-105 shadow-lg shadow-sky-500/30' : 'border-transparent hover:border-white/30 hover:scale-105'}"
                                         data-avatar-id="${optId}">
                                        <img src="/Avatar/${num}.png" class="w-10 h-10 rounded-full object-cover bg-slate-950"
                                             onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><circle cx=%2250%22 cy=%2250%22 r=%2248%22 fill=%22%230f172a%22/><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2235%22>${num}</text></svg>'">
                                        ${isCurrent ? '<span class="absolute -top-1 -right-1 w-3.5 h-3.5 bg-sky-400 rounded-full flex items-center justify-center text-[8px] font-bold text-black">✓</span>' : ''}
                                    </div>
                                `;
                            }).join('')}
                        </div>
                    </div>

                    <!-- FORM FIELDS -->
                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <div>
                            <label class="text-[11px] font-bold uppercase tracking-wider text-slate-300 block mb-1">Device / User Name *</label>
                            <input type="text" id="edit-name" value="${device.name || ''}" placeholder="e.g. John Citizen"
                                   class="w-full bg-slate-950 border border-white/10 rounded-lg p-2.5 text-xs text-white focus:border-sky-500 focus:outline-none font-medium">
                        </div>

                        <div>
                            <label class="text-[11px] font-bold uppercase tracking-wider text-slate-300 block mb-1">Network Role / Title</label>
                            <select id="edit-role" class="w-full bg-slate-950 border border-white/10 rounded-lg p-2.5 text-xs text-white focus:border-sky-500 focus:outline-none font-medium cursor-pointer">
                                ${["Admin", "Guardian", "Father", "Mother", "Spouse", "Child", "Friend", "Member"].map(r => {
                                    const currentRole = device.roleFamily && device.roleFamily !== 'None' ? device.roleFamily : (device.roleFriend && device.roleFriend !== 'None' ? device.roleFriend : (device.role || 'Member'));
                                    const isSel = currentRole.toLowerCase().includes(r.toLowerCase());
                                    return `<option value="${r}" ${isSel ? 'selected' : ''}>${r}</option>`;
                                }).join('')}
                            </select>
                        </div>

                        <div>
                            <label class="text-[11px] font-bold uppercase tracking-wider text-slate-300 block mb-1">Department / Organization</label>
                            <input type="text" id="edit-dept" value="${device.department || ''}" placeholder="e.g. Field Operations"
                                   class="w-full bg-slate-950 border border-white/10 rounded-lg p-2.5 text-xs text-white focus:border-sky-500 focus:outline-none">
                        </div>

                        <div>
                            <label class="text-[11px] font-bold uppercase tracking-wider text-slate-300 block mb-1">Phone Number</label>
                            <input type="tel" id="edit-phone" value="${device.phone || ''}" placeholder="e.g. +1 555-0192"
                                   class="w-full bg-slate-950 border border-white/10 rounded-lg p-2.5 text-xs text-white font-mono focus:border-sky-500 focus:outline-none">
                        </div>

                        <div class="sm:col-span-2">
                            <label class="text-[11px] font-bold uppercase tracking-wider text-slate-300 block mb-1">Email Address</label>
                            <input type="email" id="edit-email" value="${device.email || ''}" placeholder="e.g. user@domain.com"
                                   class="w-full bg-slate-950 border border-white/10 rounded-lg p-2.5 text-xs text-white font-mono focus:border-sky-500 focus:outline-none">
                        </div>
                    </div>

                    <div>
                        <label class="text-[11px] font-bold uppercase tracking-wider text-slate-300 block mb-1">Emergency Medical Note</label>
                        <input type="text" id="edit-note" value="${device.emergencyNote || ''}" placeholder="e.g. Blood Group O+, Allergies: None"
                               class="w-full bg-slate-950 border border-white/10 rounded-lg p-2.5 text-xs text-white focus:border-sky-500 focus:outline-none">
                    </div>

                    <!-- ACL ACCESS GRANTS CHECKBOXES -->
                    <div class="p-3 bg-black/40 rounded-xl border border-white/5 space-y-2">
                        <label class="text-[11px] font-bold uppercase tracking-wider text-sky-400 block">Security Access Grants (ACL)</label>
                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                            <label class="flex items-center gap-2 text-slate-300 cursor-pointer hover:text-white">
                                <input type="checkbox" id="acl-live-tracking" checked class="w-4 h-4 rounded bg-slate-900 border-white/20 text-sky-600 focus:ring-sky-500">
                                <span>Live GPS Tracking Protocol</span>
                            </label>
                            <label class="flex items-center gap-2 text-slate-300 cursor-pointer hover:text-white">
                                <input type="checkbox" id="acl-history-audit" checked class="w-4 h-4 rounded bg-slate-900 border-white/20 text-sky-600 focus:ring-sky-500">
                                <span>History Trace Audit Logs</span>
                            </label>
                            <label class="flex items-center gap-2 text-slate-300 cursor-pointer hover:text-white">
                                <input type="checkbox" id="acl-geofence" checked class="w-4 h-4 rounded bg-slate-900 border-white/20 text-sky-600 focus:ring-sky-500">
                                <span>Safe Zone Breach Alerts</span>
                            </label>
                            <label class="flex items-center gap-2 text-slate-300 cursor-pointer hover:text-white">
                                <input type="checkbox" id="acl-ghost-override" checked class="w-4 h-4 rounded bg-slate-900 border-white/20 text-sky-600 focus:ring-sky-500">
                                <span>Stealth Ghost Mode SOS Override</span>
                            </label>
                        </div>
                    </div>
                </div>

                <!-- Footer -->
                <div class="p-4 border-t border-white/10 bg-slate-950 flex items-center justify-end gap-3">
                    <button id="btn-cancel-edit" class="px-4 py-2 rounded-xl text-xs font-bold text-slate-400 hover:text-white hover:bg-white/5 transition-all">
                        Cancel
                    </button>
                    <button id="btn-save-edit" class="px-6 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-black uppercase tracking-wider transition-all shadow-lg shadow-sky-600/30 flex items-center gap-1.5">
                        <span>Save to Firebase</span>
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
        this.modalElement = modal;

        this.bindEvents(modal, device, selectedAvatarId);
    }

    bindEvents(modal, device, selectedAvatarId) {
        const closeBtn = modal.querySelector('#btn-close-edit');
        const cancelBtn = modal.querySelector('#btn-cancel-edit');
        const saveBtn = modal.querySelector('#btn-save-edit');
        const errorBox = modal.querySelector('#edit-error-box');

        const handleClose = () => this.close();
        closeBtn.addEventListener('click', handleClose);
        cancelBtn.addEventListener('click', handleClose);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) handleClose();
        });

        // Avatar Selection
        modal.querySelectorAll('.avatar-option-card').forEach(card => {
            card.addEventListener('click', () => {
                selectedAvatarId = card.dataset.avatarId;
                modal.querySelectorAll('.avatar-option-card').forEach(c => {
                    c.classList.remove('border-sky-400', 'bg-sky-500/20', 'scale-105', 'shadow-lg', 'shadow-sky-500/30');
                    c.classList.add('border-transparent');
                    const badge = c.querySelector('span');
                    if (badge) badge.remove();
                });
                card.classList.remove('border-transparent');
                card.classList.add('border-sky-400', 'bg-sky-500/20', 'scale-105', 'shadow-lg', 'shadow-sky-500/30');
                const badge = document.createElement('span');
                badge.className = 'absolute -top-1 -right-1 w-3.5 h-3.5 bg-sky-400 rounded-full flex items-center justify-center text-[8px] font-bold text-black';
                badge.textContent = '✓';
                card.appendChild(badge);
            });
        });

        // Save Click with Validation
        saveBtn.addEventListener('click', async () => {
            const name = modal.querySelector('#edit-name').value.trim();
            const dept = modal.querySelector('#edit-dept').value.trim();
            const phone = modal.querySelector('#edit-phone').value.trim();
            const email = modal.querySelector('#edit-email').value.trim();
            const note = modal.querySelector('#edit-note').value.trim();

            if (!name) {
                errorBox.textContent = "Validation Error: Device / User Name cannot be empty.";
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

            const role = modal.querySelector('#edit-role') ? modal.querySelector('#edit-role').value : "Member";
            const aclLive = modal.querySelector('#acl-live-tracking') ? modal.querySelector('#acl-live-tracking').checked : true;
            const aclHistory = modal.querySelector('#acl-history-audit') ? modal.querySelector('#acl-history-audit').checked : true;
            const aclGeofence = modal.querySelector('#acl-geofence') ? modal.querySelector('#acl-geofence').checked : true;
            const aclGhost = modal.querySelector('#acl-ghost-override') ? modal.querySelector('#acl-ghost-override').checked : true;

            const payload = {
                name,
                avatar_id: selectedAvatarId,
                department: dept,
                phone,
                email,
                emergency_note: note,
                role,
                role_family: role.toLowerCase().includes("friend") ? "None" : role,
                role_friend: role.toLowerCase().includes("friend") ? "Friends" : "None",
                acl_live_tracking: aclLive ? "GRANTED" : "DENIED",
                acl_history_audit: aclHistory ? "GRANTED" : "DENIED",
                acl_geofence: aclGeofence ? "GRANTED" : "DENIED",
                acl_ghost_override: aclGhost ? "GRANTED" : "DENIED"
            };

            if (this.onSave) {
                try {
                    await this.onSave(device.agentId, payload);
                    this.close();
                } catch (err) {
                    errorBox.textContent = `Sync Fault: ${err.message}`;
                    errorBox.classList.remove('hidden');
                    saveBtn.disabled = false;
                    saveBtn.innerHTML = `<span>Save to Firebase</span>`;
                }
            }
        });
    }

    close() {
        if (this.modalElement) {
            this.modalElement.remove();
            this.modalElement = null;
        }
    }
}
