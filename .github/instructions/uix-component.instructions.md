---
applyTo: "**/js/views/*.js", "**/js/components/*.js", "**/css/*.css"
description: "UI/UX guidelines for telemetry dashboard components and styling in jButterfly"
---

# UI/UX Component Instructions

Apply to all view and component files, plus CSS styling.

## Component Structure

### View Pattern
```javascript
class DeviceCardView {
  constructor(container) {
    this.container = container;
    this.element = null;
  }
  
  render(device) {
    this.element = document.createElement('div');
    this.element.className = 'device-card glass';
    this.element.innerHTML = `
      <div class="device-avatar">${device.avatar}</div>
      <div class="device-info">
        <h3>${device.name}</h3>
        <span class="status-badge ${device.status}">${device.status}</span>
      </div>
      <div class="device-metrics">
        <div class="metric">
          <span class="label">Speed</span>
          <span class="value">${device.speed} km/h</span>
        </div>
      </div>
    `;
    return this.element;
  }
  
  update(device) {
    if (!this.element) return;
    // Update DOM efficiently
    this.element.querySelector('.device-metrics .value').textContent = 
      `${device.speed} km/h`;
  }
  
  destroy() {
    if (this.element && this.element.parentNode) {
      this.element.parentNode.removeChild(this.element);
    }
    this.element = null;
  }
}
```

## CSS Conventions

### Glassmorphism Cards
```css
/* Use existing variables from variables.css */
.glass {
  background: var(--glass-bg);
  backdrop-filter: blur(10px);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

/* Dark mode override */
.dark .glass {
  background: rgba(30, 41, 59, 0.7);
}
```

### Responsive Grid
```css
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-md);
}

@media (max-width: 768px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
```

## Accessibility

### Color Contrast
- Minimum 4.5:1 contrast ratio for text
- Use CSS variables for theme colors
- Test with WCAG contrast checker

### Keyboard Navigation
```html
<!-- Always include focus states -->
<button class="nav-item" tabindex="0" aria-label="Dashboard">
  Dashboard
</button>

/* Add focus outline */
.nav-item:focus {
  outline: 2px solid var(--focus-color);
  outline-offset: 2px;
}
```

### ARIA Labels
```html
<div role="status" aria-live="polite">
  Device updated: Alpha Unit at 28.61°N, 77.21°E
</div>
```

## Animation Guidelines

### Micro-interactions
```css
/* Subtle hover effects */
.device-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.device-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

/* Loading skeleton */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.skeleton {
  background: linear-gradient(90deg, 
    var(--surface-1) 25%, 
    var(--surface-2) 50%, 
    var(--surface-1) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
```

## Testing Checklist
- [ ] Test on mobile (320px width)
- [ ] Test on tablet (768px width)
- [ ] Test on desktop (1920px width)
- [ ] Verify keyboard navigation
- [ ] Check screen reader compatibility
- [ ] Test dark/light theme toggle
- [ ] Verify touch targets (min 44x44px)
