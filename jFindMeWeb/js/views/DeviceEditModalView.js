import { AvatarService } from '../services/AvatarService.js';
import { FeedbackDialog } from '../components/FeedbackDialog.js';

/**
 * jFindMe Matrix - Device Profile & Avatar Edit Modal View (Dynamic OOPS Architecture)
 * Closed-Loop profile editing dialog allowing admins to update avatars, names, contact details,
 * and sync atomically to Firebase RTDB.
 * 100% Theme-Adaptive (Dark / Light) with live validations and feedback toasts.
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
        modal.className = 'fixed inset-0 z-[1100] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-fade-in font-sans';

        // Available Avatars 1 to 37
        const avatarOptions = Array.from({ length: 37 }, (_, i) => i + 1);
        const currentAvatarId = device.avatarId || "avatar_custom_1";
        let selectedAvatarId = currentAvatarId;

        modal.innerHTML = `
            <div class="card-theme border rounded-2xl w-full max-w-2xl shadow-2xl overflow-hidden text-primary-theme flex flex-col max-h-[92vh]">
                <!-- Header -->
                <div class="p-4 border-b border-subtle-theme flex items-center justify-between surface-theme">
                    <div class="flex items-center gap-3">
                        <div class="w-9 h-9 rounded-xl bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-sky-400 text-base font-bold">
                            ✏️
                        </div>
                        <div>
                            <h3 class="text-sm font-extrabold tracking-tight text-primary-theme uppercase">Edit Device Dossier</h3>
                            <p class="text-[11px] text-secondary-theme font-mono">Agent ID: <span class="text-sky-400 font-bold">${device.agentId}</span> • Hardware: ${device.deviceId}</p>
                        </div>
                    </div>
                    <button id="btn-close-edit" class="text-muted-theme hover:text-primary-theme p-1.5 rounded-lg text-lg font-mono">
                        ✕
                    </button>
                </div>

                <!-- Body -->
                <div class="p-5 overflow-y-auto space-y-4 flex-1 custom-scrollbar">
                    <!-- Error / Notice Box -->
                    <div id="edit-error-box" class="hidden p-2.5 rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-500 text-xs font-mono font-medium"></div>

                    <!-- AVATAR PICKER GRID -->
                    <div class="space-y-1.5">
                        <label class="text-xs font-bold uppercase tracking-wider text-sky-400 block">Select Avatar Icon (1 - 37)</label>
                        <div class="grid grid-cols-6 sm:grid-cols-9 gap-2 p-2.5 surface-theme rounded-xl border max-h-44 overflow-y-auto custom-scrollbar" id="avatar-picker-grid">
                            ${avatarOptions.map(num => {
                                const optId = `avatar_custom_${num}`;
                                const isCurrent = currentAvatarId === optId || currentAvatarId === String(num);
                                return `
                                    <div class="avatar-option-card relative cursor-pointer rounded-full p-0.5 border-2 transition-all ${isCurrent ? 'border-sky-500 bg-sky-500/20 scale-105 shadow-md shadow-sky-500/30' : 'border-transparent hover:border-sky-400/40 hover:scale-105'}"
                                         data-avatar-id="${optId}">
                                        <img src="/Avatar/${num}.png" class="w-9 h-9 rounded-full object-cover bg-transparent"
                                             onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><circle cx=%2250%22 cy=%2250%22 r=%2248%22 fill=%22%230f172a%22/><text x=%2250%%22 y=%2255%%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2235%22>${num}</text></svg>'">
                                        ${isCurrent ? '<span class="absolute -top-1 -right-1 w-3.5 h-3.5 bg-sky-500 rounded-full flex items-center justify-center text-[8px] font-bold text-white">✓</span>' : ''}
                                    </div>
                                `;
                            }).join('')}
                        </div>
                    </div>

                    <!-- FORM FIELDS -->
                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        <div>
                            <label class="text-[11px] font-bold uppercase tracking-wider text-secondary-theme block mb-1">Device / User Name *</label>
                            <input type="text" id="edit-name" value="${device.name || ''}" placeholder="e.g. John Citizen"
                                   class="input-theme w-full p-2 text-xs font-medium">
                        </div>

                        <div>
                            <label class="text-[11px] font-bold uppercase tracking-wider text-secondary-theme block mb-1">Network Role / Title</label>
                            <select id="edit-role" class="input-theme w-full p-2 text-xs font-medium cursor-pointer">
                                ${["Admin", "Guardian", "Father", "Mother", "Spouse", "Child", "Friend", "Member"].map(r => {
                                    const currentRole = device.roleFamily && device.roleFamily !== 'None' ? device.roleFamily : (device.roleFriend && device.roleFriend !== 'None' ? device.roleFriend : (device.role || 'Member'));
                                    const isSel = currentRole.toLowerCase().includes(r.toLowerCase());
                                    return `<option value="${r}" ${isSel ? 'selected' : ''}>${r}</option>`;
                                }).join('')}
                            </select>
                        </div>

                        <div>
                            <label class="text-[11px] font-bold uppercase tracking-wider text-secondary-theme block mb-1">Department / Organization</label>
                            <input type="text" id="edit-dept" value="${device.department || ''}" placeholder="e.g. Field Operations"
                                   class="input-theme w-full p-2 text-xs">
                        </div>

                        <div>
                            <label class="text-[11px] font-bold uppercase tracking-wider text-secondary-theme block mb-1">Phone Number</label>
                            <input type="tel" id="edit-phone" value="${device.phone || ''}" placeholder="e.g. +1 555-0192"
                                   class="input-theme w-full p-2 text-xs font-mono">
                        </div>

                        <div class="sm:col-span-2">
                            <label class="text-[11px] font-bold uppercase tracking-wider text-secondary-theme block mb-1">Email Address</label>
                            <input type="email" id="edit-email" value="${device.email || ''}" placeholder="e.g. user@domain.com"
                                   class="input-theme w-full p-2 text-xs font-mono">
                        </div>
                    </div>

                    <div>
                        <label class="text-[11px] font-bold uppercase tracking-wider text-secondary-theme block mb-1">Emergency Medical Note</label>
                        <input type="text" id="edit-note" value="${device.emergencyNote || ''}" placeholder="e.g. Blood Group O+, Allergies: None"
                               class="input-theme w-full p-2 text-xs">
                    </div>

                    <!-- ACL ACCESS GRANTS TACTILE SWITCHES -->
                    <div class="p-3 surface-theme rounded-xl border space-y-2.5">
                        <div class="flex items-center justify-between border-b border-subtle-theme pb-1">
                            <label class="text-[11px] font-bold uppercase tracking-wider text-sky-400 block">Security Access Grants (ACL)</label>
                            <span class="badge-status badge-accent text-[8px]">CLOSED LOOP</span>
                        </div>
                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                            <div class="flex items-center justify-between p-2 rounded-xl bg-card-subtle border border-subtle-theme">
                                <span class="text-secondary-theme font-medium text-[11px]">Live GPS Tracking</span>
                                <label class="switch-toggle switch-sm">
                                    <input type="checkbox" id="acl-live-tracking" ${device.raw?.acl?.acl_live_tracking === 'DENIED' ? '' : 'checked'}>
                                    <span class="switch-slider"></span>
                                </label>
                            </div>
                            <div class="flex items-center justify-between p-2 rounded-xl bg-card-subtle border border-subtle-theme">
                                <span class="text-secondary-theme font-medium text-[11px]">History Trace Audit</span>
                                <label class="switch-toggle switch-sm">
                                    <input type="checkbox" id="acl-history-audit" ${device.raw?.acl?.acl_history_audit === 'DENIED' ? '' : 'checked'}>
                                    <span class="switch-slider"></span>
                                </label>
                            </div>
                            <div class="flex items-center justify-between p-2 rounded-xl bg-card-subtle border border-subtle-theme">
                                <span class="text-secondary-theme font-medium text-[11px]">Safe Zone Breach Alerts</span>
                                <label class="switch-toggle switch-sm">
                                    <input type="checkbox" id="acl-geofence" ${device.raw?.acl?.acl_geofence === 'DENIED' ? '' : 'checked'}>
                                    <span class="switch-slider"></span>
                                </label>
                            </div>
                            <div class="flex items-center justify-between p-2 rounded-xl bg-card-subtle border border-subtle-theme">
                                <span class="text-secondary-theme font-medium text-[11px]">Ghost SOS Override</span>
                                <label class="switch-toggle switch-sm">
                                    <input type="checkbox" id="acl-ghost-override" ${device.raw?.acl?.acl_ghost_override === 'DENIED' ? '' : 'checked'}>
                                    <span class="switch-slider"></span>
                                </label>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Footer -->
                <div class="p-3.5 border-t border-subtle-theme surface-theme flex items-center justify-end gap-2">
                    <button id="btn-cancel-edit" class="btn-action-secondary text-xs">
                        Cancel
                    </button>
                    <button id="btn-save-edit" class="btn-action-primary text-xs">
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
                    c.classList.remove('border-sky-500', 'bg-sky-500/20', 'scale-105', 'shadow-md', 'shadow-sky-500/30');
                    c.classList.add('border-transparent');
                    const badge = c.querySelector('span');
                    if (badge) badge.remove();
                });
                card.classList.remove('border-transparent');
                card.classList.add('border-sky-500', 'bg-sky-500/20', 'scale-105', 'shadow-md', 'shadow-sky-500/30');
                const badge = document.createElement('span');
                badge.className = 'absolute -top-1 -right-1 w-3.5 h-3.5 bg-sky-500 rounded-full flex items-center justify-center text-[8px] font-bold text-white';
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
                    FeedbackDialog.showToast(`Profile for [${name}] synchronized to Firebase!`, "success");
                    FeedbackDialog.showSlideNotification({
                        title: "Closed-Loop Sync Complete",
                        message: `Profile [${name}] (Avatar: ${selectedAvatarId}, Role: ${role}) synchronized atomically across all database nodes.`,
                        type: "success",
                        duration: 4500
                    });
                } catch (err) {
                    errorBox.textContent = `Sync Fault: ${err.message}`;
                    errorBox.classList.remove('hidden');
                    saveBtn.disabled = false;
                    saveBtn.innerHTML = `<span>Save to Firebase</span>`;
                    FeedbackDialog.showToast(`Profile update failed: ${err.message}`, "error");
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
