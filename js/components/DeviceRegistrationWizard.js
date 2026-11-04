/**
 * DeviceRegistrationWizard.js - Enterprise Device Onboarding Flow
 * Multi-step wizard for adding new devices to the telemetry system
 */
import { FBBridge } from '../services/FirebaseService.js';
import { UIControls } from './UIControls.js';
import { FeedbackDialog } from './FeedbackDialog.js';
import { AvatarService } from '../services/AvatarService.js';

export class DeviceRegistrationWizard {
    constructor(options = {}) {
        this.onComplete = options.onComplete || null;
        this.steps = [];
        this.currentStep = 0;
        this.formData = {};
    }

    /**
     * Show the registration wizard modal
     */
    async show() {
        this.currentStep = 0;
        this.formData = {};
        this.steps = [
            { id: 'identity', title: 'Device Identity', icon: '📱' },
            { id: 'profile', title: 'Profile Setup', icon: '👤' },
            { id: 'circle', title: 'Circle Assignment', icon: '👥' },
            { id: 'geofence', title: 'Safe Zones', icon: '🛡️' },
            { id: 'review', title: 'Review & Confirm', icon: '✅' }
        ];
        
        this._renderModal();
        await this._loadAvailableData();
    }

    /**
     * Load available data for dropdowns
     */
    async _loadAvailableData() {
        try {
            // Load existing circles/groups for selection
            const circlesRes = await FBBridge.fetchRaw('/api/circles');
            if (circlesRes?.circles) {
                this.availableCircles = circlesRes.circles;
            }
            
            // Load existing safe zones
            const zonesRes = await FBBridge.fetchRaw('/api/safe-zones');
            if (zonesRes?.safeZones) {
                this.availableZones = zonesRes.safeZones;
            }
        } catch (err) {
            console.warn('[WIZARD] Failed to load available data:', err);
            this.availableCircles = [{ id: 'default', name: 'Default Circle' }];
            this.availableZones = [];
        }
    }

    /**
     * Render the wizard modal
     */
    _renderModal() {
        const modal = document.createElement('div');
        modal.id = 'device-registration-wizard';
        modal.className = 'fixed inset-0 z-[1200] flex items-center justify-center bg-black/90 backdrop-blur-xl p-4 animate-fade-in';
        
        modal.innerHTML = `
            <div class="bg-card border border-accent/20 rounded-2xl w-full max-w-3xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
                <!-- Header -->
                <div class="px-6 py-4 border-b border-subtle-theme flex items-center justify-between bg-surface-theme">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-sky-500 to-indigo-600 flex items-center justify-center text-lg">
                            📱
                        </div>
                        <div>
                            <h2 class="text-sm font-extrabold text-primary-theme uppercase tracking-wider">Device Registration Wizard</h2>
                            <p class="text-[11px] text-secondary-theme font-mono">Step ${this.currentStep + 1} of ${this.steps.length}</p>
                        </div>
                    </div>
                    <button id="wizard-close" class="text-muted-theme hover:text-primary-theme p-2 rounded-lg transition-colors">
                        ✕
                    </button>
                </div>

                <!-- Progress Bar -->
                <div class="px-6 py-3 border-b border-subtle-theme bg-surface-theme">
                    <div class="flex items-center gap-2">
                        ${this.steps.map((step, idx) => `
                            <div class="flex items-center gap-2 flex-1 ${idx <= this.currentStep ? 'opacity-100' : 'opacity-40'}">
                                <div class="flex items-center gap-1.5">
                                    <span class="text-xs">${step.icon}</span>
                                    <span class="text-[10px] font-mono hidden sm:inline">${step.title}</span>
                                </div>
                                ${idx < this.steps.length - 1 ? '<span class="text-muted-theme">→</span>' : ''}
                            </div>
                        `).join('')}
                    </div>
                    <div class="mt-2 h-1 bg-slate-800 rounded-full overflow-hidden">
                        <div class="h-full bg-gradient-to-r from-sky-500 to-indigo-500 transition-all duration-300" 
                             style="width: ${((this.currentStep + 1) / this.steps.length) * 100}%"></div>
                    </div>
                </div>

                <!-- Body -->
                <div class="flex-1 overflow-y-auto p-6" id="wizard-body">
                    <!-- Step content injected here -->
                </div>

                <!-- Footer -->
                <div class="px-6 py-4 border-t border-subtle-theme flex items-center justify-between bg-surface-theme">
                    <button id="wizard-prev" class="btn-secondary text-xs px-4 py-2 rounded-lg hidden">
                        ← Previous
                    </button>
                    <div class="flex-1"></div>
                    <button id="wizard-next" class="btn-primary text-xs px-6 py-2 rounded-lg">
                        Next →
                    </button>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        this.modalElement = modal;
        this._bindEvents();
        this._renderStep();
    }

    /**
     * Bind modal events
     */
    _bindEvents() {
        document.getElementById('wizard-close').onclick = () => this.close();
        document.getElementById('wizard-prev').onclick = () => this.previousStep();
        document.getElementById('wizard-next').onclick = () => this.nextStep();
        
        // Close on Escape
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') this.close();
        });
    }

    /**
     * Render current step
     */
    _renderStep() {
        const body = document.getElementById('wizard-body');
        if (!body) return;
        
        const step = this.steps[this.currentStep];
        
        switch(step.id) {
            case 'identity':
                body.innerHTML = this._renderIdentityStep();
                break;
            case 'profile':
                body.innerHTML = this._renderProfileStep();
                break;
            case 'circle':
                body.innerHTML = this._renderCircleStep();
                break;
            case 'geofence':
                body.innerHTML = this._renderGeofenceStep();
                break;
            case 'review':
                body.innerHTML = this._renderReviewStep();
                break;
        }
        
        // Update buttons
        const prevBtn = document.getElementById('wizard-prev');
        const nextBtn = document.getElementById('wizard-next');
        
        if (prevBtn) {
            prevBtn.classList.toggle('hidden', this.currentStep === 0);
        }
        
        if (nextBtn) {
            nextBtn.textContent = this.currentStep === this.steps.length - 1 ? '✓ Complete Registration' : 'Next →';
        }
    }

    /**
     * Render identity step
     */
    _renderIdentityStep() {
        return `
            <div class="space-y-4">
                <div class="p-4 rounded-xl bg-surface-theme border border-subtle-theme">
                    <h3 class="text-xs font-bold uppercase tracking-wider text-sky-400 mb-3">Device Identity Information</h3>
                    
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                        <div>
                            <label class="text-[11px] font-bold uppercase text-secondary-theme block mb-1">
                                Agent ID <span class="text-danger">*</span>
                            </label>
                            <input type="text" id="reg-agent-id" placeholder="e.g. DEVICE_001"
                                   class="input-theme w-full p-2.5 text-xs font-mono"
                                   value="${this.formData.agentId || ''}">
                            <p class="text-[9px] text-muted-theme mt-1">Unique identifier for this device</p>
                        </div>
                        
                        <div>
                            <label class="text-[11px] font-bold uppercase text-secondary-theme block mb-1">
                                Hardware Model
                            </label>
                            <select id="reg-model" class="input-theme w-full p-2.5 text-xs cursor-pointer">
                                <option value="">Select model...</option>
                                ${['Samsung Galaxy S24', 'iPhone 15 Pro', 'OnePlus 12', 'Google Pixel 8', 'Xiaomi 14', 'Custom Android'].map(m => 
                                    `<option value="${m}" ${this.formData.model === m ? 'selected' : ''}>${m}</option>`
                                ).join('')}
                            </select>
                        </div>
                        
                        <div>
                            <label class="text-[11px] font-bold uppercase text-secondary-theme block mb-1">
                                IMEI (Optional)
                            </label>
                            <input type="text" id="reg-imei" placeholder="33XXXXXXXXXXXX"
                                   class="input-theme w-full p-2.5 text-xs font-mono"
                                   value="${this.formData.imei || ''}">
                        </div>
                        
                        <div>
                            <label class="text-[11px] font-bold uppercase text-secondary-theme block mb-1">
                                Status
                            </label>
                            <select id="reg-status" class="input-theme w-full p-2.5 text-xs cursor-pointer">
                                <option value="online" ${this.formData.status === 'online' ? 'selected' : ''}>🟢 Online</option>
                                <option value="offline" ${this.formData.status === 'offline' ? 'selected' : ''}>⚪ Offline</option>
                                <option value="testing" ${this.formData.status === 'testing' ? 'selected' : ''}>🟡 Testing</option>
                            </select>
                        </div>
                    </div>
                </div>
                
                <div class="p-3 rounded-lg bg-warning/10 border border-warning/30 text-warning-text text-[11px]">
                    ⚠️ Agent ID must be unique. Duplicate IDs will overwrite existing devices.
                </div>
            </div>
        `;
    }

    /**
     * Render profile step
     */
    _renderProfileStep() {
        return `
            <div class="space-y-4">
                <div class="p-4 rounded-xl bg-surface-theme border border-subtle-theme">
                    <h3 class="text-xs font-bold uppercase tracking-wider text-sky-400 mb-3">Person Profile</h3>
                    
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                        <div>
                            <label class="text-[11px] font-bold uppercase text-secondary-theme block mb-1">
                                Display Name <span class="text-danger">*</span>
                            </label>
                            <input type="text" id="reg-name" placeholder="e.g. John Citizen"
                                   class="input-theme w-full p-2.5 text-xs"
                                   value="${this.formData.name || ''}">
                        </div>
                        
                        <div>
                            <label class="text-[11px] font-bold uppercase text-secondary-theme block mb-1">
                                Role / Relationship
                            </label>
                            <select id="reg-role" class="input-theme w-full p-2.5 text-xs cursor-pointer">
                                ${['Self', 'Child', 'Spouse', 'Parent', 'Guardian', 'Friend', 'Colleague', 'Member'].map(r =>
                                    `<option value="${r}" ${this.formData.role === r ? 'selected' : ''}>${r}</option>`
                                ).join('')}
                            </select>
                        </div>
                        
                        <div>
                            <label class="text-[11px] font-bold uppercase text-secondary-theme block mb-1">
                                Email
                            </label>
                            <input type="email" id="reg-email" placeholder="user@example.com"
                                   class="input-theme w-full p-2.5 text-xs"
                                   value="${this.formData.email || ''}">
                        </div>
                        
                        <div>
                            <label class="text-[11px] font-bold uppercase text-secondary-theme block mb-1">
                                Phone
                            </label>
                            <input type="tel" id="reg-phone" placeholder="+91 XXXXX XXXXX"
                                   class="input-theme w-full p-2.5 text-xs"
                                   value="${this.formData.phone || ''}">
                        </div>
                        
                        <div>
                            <label class="text-[11px] font-bold uppercase text-secondary-theme block mb-1">
                                Department
                            </label>
                            <input type="text" id="reg-dept" placeholder="e.g. Operations"
                                   class="input-theme w-full p-2.5 text-xs"
                                   value="${this.formData.department || ''}">
                        </div>
                        
                        <div>
                            <label class="text-[11px] font-bold uppercase text-secondary-theme block mb-1">
                                Avatar
                            </label>
                            <div class="flex items-center gap-2">
                                <div class="w-10 h-10 rounded-full bg-sky-500/20 border border-sky-500/40 flex items-center justify-center text-sky-400 text-sm font-bold">
                                    ${this.formData.name ? this.formData.name.charAt(0).toUpperCase() : '👤'}
                                </div>
                                <span class="text-xs text-muted-theme">Auto-generated from name</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Render circle assignment step
     */
    _renderCircleStep() {
        return `
            <div class="space-y-4">
                <div class="p-4 rounded-xl bg-surface-theme border border-subtle-theme">
                    <h3 class="text-xs font-bold uppercase tracking-wider text-sky-400 mb-3">Circle & Group Assignment</h3>
                    
                    <div class="space-y-3">
                        <div>
                            <label class="text-[11px] font-bold uppercase text-secondary-theme block mb-1">
                                Assign to Circle <span class="text-danger">*</span>
                            </label>
                            <select id="reg-circle" class="input-theme w-full p-2.5 text-xs cursor-pointer">
                                <option value="">Select circle...</option>
                                ${this.availableCircles?.map(c => 
                                    `<option value="${c.id}" ${this.formData.circle === c.id ? 'selected' : ''}>${c.name}</option>`
                                ).join('') || 
                                '<option value="family">Family Circle</option>' +
                                '<option value="operations">Operations</option>' +
                                '<option value="field">Field Service</option>'
                                }
                            </select>
                        </div>
                        
                        <div>
                            <label class="text-[11px] font-bold uppercase text-secondary-theme block mb-1">
                                Emergency Contact Note
                            </label>
                            <textarea id="reg-emergency-note" rows="2" 
                                      placeholder="Any emergency contact information..."
                                      class="input-theme w-full p-2.5 text-xs resize-none">${this.formData.emergencyNote || ''}</textarea>
                        </div>
                    </div>
                </div>
                
                <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
                    ${['Family Circle', 'Operations', 'Field Service'].map(circle => `
                        <button class="circle-option-btn p-3 rounded-xl border border-subtle-theme hover:border-sky-500/50 hover:bg-sky-500/10 transition-all text-left"
                                data-circle="${circle}">
                            <div class="text-xs font-bold text-primary-theme">${circle}</div>
                            <div class="text-[10px] text-muted-theme mt-0.5">Standard access level</div>
                        </button>
                    `).join('')}
                </div>
            </div>
        `;
    }

    /**
     * Render geofence assignment step
     */
    _renderGeofenceStep() {
        return `
            <div class="space-y-4">
                <div class="p-4 rounded-xl bg-surface-theme border border-subtle-theme">
                    <h3 class="text-xs font-bold uppercase tracking-wider text-sky-400 mb-3">Safe Zone Assignment</h3>
                    
                    <div class="space-y-2">
                        <label class="text-[11px] font-bold uppercase text-secondary-theme block mb-1">
                            Assign to Safe Zones
                        </label>
                        
                        ${this.availableZones?.length > 0 ? 
                            this.availableZones.map(z => `
                                <label class="flex items-center gap-3 p-3 rounded-lg bg-slate-900/50 border border-subtle-theme cursor-pointer hover:border-sky-500/30 transition-all">
                                    <input type="checkbox" class="zone-checkbox w-4 h-4 rounded" 
                                           value="${z.zoneId}" 
                                           ${this.formData.zones?.includes(z.zoneId) ? 'checked' : ''}>
                                    <div class="flex-1">
                                        <div class="text-xs font-medium text-primary-theme">${z.zoneName}</div>
                                        <div class="text-[10px] text-muted-theme">${z.type || 'Circular'} • ${z.radius}m radius</div>
                                    </div>
                                    <span class="badge-status badge-accent text-[9px]">${z.isActive ? 'Active' : 'Inactive'}</span>
                                </label>
                            `).join('') : 
                            '<p class="text-xs text-muted-theme italic">No safe zones configured yet. Create zones in Geofencing view first.</p>'
                        }
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Render review step
     */
    _renderReviewStep() {
        const avatarUrl = AvatarService.generateInitialAvatarSvg(this.formData.name || 'Device', '#38BDF8');
        
        return `
            <div class="space-y-4">
                <div class="p-4 rounded-xl bg-surface-theme border border-subtle-theme">
                    <h3 class="text-xs font-bold uppercase tracking-wider text-sky-400 mb-4">Review & Confirm</h3>
                    
                    <div class="flex gap-4 mb-4">
                        <img src="${avatarUrl}" class="w-16 h-16 rounded-full border-2 border-sky-500/40 bg-slate-800" alt="Avatar">
                        <div>
                            <div class="text-sm font-bold text-primary-theme">${this.formData.name || 'Unnamed Device'}</div>
                            <div class="text-[11px] text-muted-theme font-mono">${this.formData.agentId || 'No ID'}</div>
                            <div class="text-[11px] text-sky-400 mt-1">${this.formData.role || 'Member'}</div>
                        </div>
                    </div>
                    
                    <div class="grid grid-cols-2 gap-3 text-xs">
                        <div class="p-2 rounded-lg bg-slate-900/50">
                            <span class="text-muted-theme">Email:</span>
                            <span class="ml-1 text-primary-theme">${this.formData.email || 'Not provided'}</span>
                        </div>
                        <div class="p-2 rounded-lg bg-slate-900/50">
                            <span class="text-muted-theme">Phone:</span>
                            <span class="ml-1 text-primary-theme">${this.formData.phone || 'Not provided'}</span>
                        </div>
                        <div class="p-2 rounded-lg bg-slate-900/50">
                            <span class="text-muted-theme">Circle:</span>
                            <span class="ml-1 text-primary-theme">${this.formData.circle || 'Default Circle'}</span>
                        </div>
                        <div class="p-2 rounded-lg bg-slate-900/50">
                            <span class="text-muted-theme">Model:</span>
                            <span class="ml-1 text-primary-theme">${this.formData.model || 'Android Device'}</span>
                        </div>
                    </div>
                    
                    ${this.formData.zones?.length > 0 ? `
                        <div class="mt-3 p-2 rounded-lg bg-success/10 border border-success/30">
                            <span class="text-success-text text-[11px]">✓ Assigned to ${this.formData.zones.length} safe zone(s)</span>
                        </div>
                    ` : ''}
                </div>
                
                <div class="p-3 rounded-lg bg-warning/10 border border-warning/30 text-warning-text text-[11px]">
                    ⚠️ This action cannot be undone. The device will be immediately visible to circle members.
                </div>
            </div>
        `;
    }

    /**
     * Collect form data from current step
     */
    _collectFormData() {
        const step = this.steps[this.currentStep];
        
        switch(step.id) {
            case 'identity':
                this.formData.agentId = document.getElementById('reg-agent-id')?.value?.trim();
                this.formData.model = document.getElementById('reg-model')?.value;
                this.formData.imei = document.getElementById('reg-imei')?.value?.trim();
                this.formData.status = document.getElementById('reg-status')?.value;
                break;
            case 'profile':
                this.formData.name = document.getElementById('reg-name')?.value?.trim();
                this.formData.role = document.getElementById('reg-role')?.value;
                this.formData.email = document.getElementById('reg-email')?.value?.trim();
                this.formData.phone = document.getElementById('reg-phone')?.value?.trim();
                this.formData.department = document.getElementById('reg-dept')?.value?.trim();
                break;
            case 'circle':
                this.formData.circle = document.getElementById('reg-circle')?.value;
                this.formData.emergencyNote = document.getElementById('reg-emergency-note')?.value?.trim();
                break;
            case 'geofence':
                const checkboxes = document.querySelectorAll('.zone-checkbox:checked');
                this.formData.zones = Array.from(checkboxes).map(cb => cb.value);
                break;
        }
    }

    /**
     * Validate current step
     */
    validateStep() {
        const step = this.steps[this.currentStep];
        
        switch(step.id) {
            case 'identity': {
                const agentId = document.getElementById('reg-agent-id')?.value?.trim();
                if (!agentId) {
                    FeedbackDialog.showToast('Agent ID is required', 'error');
                    return false;
                }
                if (!/^[A-Z0-9_]{3,20}$/.test(agentId)) {
                    FeedbackDialog.showToast('Agent ID must be 3-20 alphanumeric characters', 'error');
                    return false;
                }
                break;
            }
            case 'profile': {
                const name = document.getElementById('reg-name')?.value?.trim();
                if (!name) {
                    FeedbackDialog.showToast('Display name is required', 'error');
                    return false;
                }
                if (name.length < 2) {
                    FeedbackDialog.showToast('Name must be at least 2 characters', 'error');
                    return false;
                }
                break;
            }
            case 'circle': {
                // Optional field
                break;
            }
            case 'geofence':
                // Optional field
                break;
        }
        
        return true;
    }

    /**
     * Go to next step
     */
    nextStep() {
        if (!this.validateStep()) return;
        
        this._collectFormData();
        
        if (this.currentStep < this.steps.length - 1) {
            this.currentStep++;
            this._renderStep();
        } else {
            this._submit();
        }
    }

    /**
     * Go to previous step
     */
    previousStep() {
        if (this.currentStep > 0) {
            this.currentStep--;
            this._renderStep();
        }
    }

    /**
     * Submit registration
     */
    async _submit() {
        try {
            // Final validation
            if (!this.formData.agentId || !this.formData.name) {
                throw new Error('Required fields missing');
            }
            
            // Build payload
            const payload = {
                agentId: this.formData.agentId,
                profile: {
                    name: this.formData.name,
                    role: this.formData.role || 'Member',
                    email: this.formData.email,
                    phone: this.formData.phone,
                    department: this.formData.department || 'General',
                    emergency_note: this.formData.emergencyNote || ''
                },
                Identity: {
                    auth_uid: `AUTO_${Date.now()}`,
                    owner_uid: Auth.getSession()?.userId || 'admin',
                    active_group: this.formData.circle || 'Default Circle'
                },
                Hardware: {
                    model: this.formData.model || 'Android',
                    imei: this.formData.imei || 'Not Set'
                },
                telemetry: {
                    battery: 100,
                    isMockGps: false,
                    session_state: this.formData.status || 'online'
                },
                safe_zones: {}
            };
            
            // Add zone assignments
            if (this.formData.zones?.length > 0) {
                this.formData.zones.forEach(zoneId => {
                    payload.safe_zones[zoneId] = true;
                });
            }
            
            // Save to Firebase
            const response = await FBBridge.fetchRaw('/api/devices', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            
            if (response?.status === 'SUCCESS' || response?.agentId) {
                FeedbackDialog.showToast(`Device "${this.formData.name}" registered successfully!`, 'success');
                this.close();
                if (this.onComplete) this.onComplete(response);
            } else {
                throw new Error(response?.error || 'Registration failed');
            }
            
        } catch (err) {
            console.error('[WIZARD] Registration error:', err);
            FeedbackDialog.showToast(`Registration failed: ${err.message}`, 'error');
        }
    }

    /**
     * Close wizard
     */
    close() {
        if (this.modalElement) {
            this.modalElement.remove();
            this.modalElement = null;
        }
    }
}

// Export singleton instance
export const DeviceRegistrationWizardInstance = new DeviceRegistrationWizard();

// Also export for backwards compatibility
export default DeviceRegistrationWizardInstance;
