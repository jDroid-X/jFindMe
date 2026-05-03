/**
 * View: ModalUI
 * Purpose: Handles the display and interactions of the editor modal.
 * Version: 2.0 (High-Fidelity JSON Editor)
 */
class ModalUI {
    constructor() {
        this.overlay = document.getElementById('editor-modal');
        this.formContainer = document.getElementById('modal-form-container');
        this.closeButtons = document.querySelectorAll('.close-modal');
        this.saveBtn = document.getElementById('save-record-btn');
        this.terminal = document.getElementById('terminal-output');
        
        this._initEvents();
    }

    _initEvents() {
        this.closeButtons.forEach(btn => {
            btn.addEventListener('click', () => this.hide());
        });
        
        this.overlay.addEventListener('click', (e) => {
            if (e.target === this.overlay) this.hide();
        });
    }

    /**
     * Show the editor with a JSON view
     */
    show(title, data, onSave) {
        this.overlay.classList.remove('hidden');
        this.formContainer.innerHTML = '';
        
        const titleEl = this.overlay.querySelector('h3');
        titleEl.innerHTML = `<i data-lucide="database"></i> ${title.toUpperCase()}`;
        if (window.lucide) window.lucide.createIcons();

        const container = document.createElement('div');
        container.className = 'json-editor-wrapper';
        
        const textarea = document.createElement('textarea');
        textarea.id = 'raw-json-editor';
        textarea.className = 'json-editor';
        textarea.value = JSON.stringify(data, null, 2);
        textarea.placeholder = "Enter JSON content here...";
        
        const helpText = document.createElement('p');
        helpText.className = 'editor-help';
        helpText.textContent = "Ensure valid JSON format. All fields will be synced to cloud.";

        container.appendChild(textarea);
        container.appendChild(helpText);
        this.formContainer.appendChild(container);
        
        // Handle Save
        const handleSave = () => {
            try {
                const updatedData = JSON.parse(textarea.value);
                onSave(updatedData);
                this.hide();
            } catch (err) {
                textarea.style.borderColor = 'var(--danger)';
                alert(`MALFORMED_JSON_DETECTED: ${err.message}`);
            }
        };

        const newSaveBtn = this.saveBtn.cloneNode(true);
        this.saveBtn.parentNode.replaceChild(newSaveBtn, this.saveBtn);
        this.saveBtn = newSaveBtn;
        this.saveBtn.addEventListener('click', handleSave);
    }

    hide() {
        this.overlay.classList.add('hidden');
    }
}

export const modalUI = new ModalUI();
