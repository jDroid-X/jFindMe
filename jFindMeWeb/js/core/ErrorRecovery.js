/**
 * ErrorRecovery.js - Centralized Error Handling & Recovery System
 * Provides graceful degradation, retry logic, and user feedback
 */

export class ErrorRecovery {
    static _retryCounters = new Map();
    static _errorHandlers = new Map();
    static _unrecoverableErrors = new Set();

    /**
     * Register a custom error handler
     */
    static registerHandler(errorType, handler) {
        this._errorHandlers.set(errorType, handler);
    }

    /**
     * Handle an error with appropriate recovery strategy
     */
    static async handleError(error, context = 'unknown') {
        const errorType = this._classifyError(error);
        
        console.error(`[${context}] ${errorType}:`, error.message);
        
        // Show user-friendly notification
        this._showUserNotification(errorType, error.message);
        
        // Try registered handlers
        const handler = this._errorHandlers.get(errorType);
        if (handler) {
            try {
                await handler(error, context);
                return;
            } catch (handlerErr) {
                console.warn('[ERROR_RECOVERY] Handler failed:', handlerErr);
            }
        }
        
        // Apply default recovery strategies
        await this._applyDefaultRecovery(errorType, context);
    }

    /**
     * Classify error type
     */
    static _classifyError(error) {
        if (error.message?.includes('NETWORK')) return 'network_error';
        if (error.message?.includes('AUTH') || error.message?.includes('PERMISSION')) return 'auth_error';
        if (error.message?.includes('404') || error.message?.includes('NOT_FOUND')) return 'not_found';
        if (error.message?.includes('500') || error.message?.includes('SERVER')) return 'server_error';
        if (error.message?.includes('TIMEOUT')) return 'timeout_error';
        if (error instanceof TypeError && error.message?.includes('Failed to fetch')) return 'network_error';
        
        return 'unknown_error';
    }

    /**
     * Show user notification based on error type
     */
    static _showUserNotification(errorType, message) {
        const notifications = {
            network_error: {
                title: 'Network Connection Lost',
                message: 'Checking connection... Your data will sync when restored.',
                type: 'warning',
                action: 'Retry'
            },
            auth_error: {
                title: 'Authentication Expired',
                message: 'Please log in again to continue.',
                type: 'error',
                action: 'Login'
            },
            not_found: {
                title: 'Resource Not Found',
                message: 'The requested data is unavailable.',
                type: 'warning',
                action: null
            },
            server_error: {
                title: 'Server Error',
                message: 'Temporary issue on our end. Please try again.',
                type: 'error',
                action: 'Retry'
            },
            timeout_error: {
                title: 'Request Timeout',
                message: 'Operation took too long. Check your connection.',
                type: 'warning',
                action: 'Retry'
            }
        };

        const config = notifications[errorType] || notifications.unknown_error;
        
        // Show toast notification
        if (window.notify) {
            window.notify(`${config.title}: ${message}`, config.type, {
                duration: 5000,
                action: config.action ? () => this._handleAction(errorType) : null
            });
        }
    }

    /**
     * Handle action button click
     */
    static _handleAction(errorType) {
        if (errorType === 'auth_error') {
            window.showLoginOverlay?.();
        } else if (['network_error', 'server_error', 'timeout_error'].includes(errorType)) {
            location.reload();
        }
    }

    /**
     * Apply default recovery strategies
     */
    static async _applyDefaultRecovery(errorType, context) {
        switch (errorType) {
            case 'network_error':
                // Attempt reconnection
                this._scheduleReconnect(context);
                break;
                
            case 'auth_error':
                // Clear invalid session
                localStorage.removeItem('jFindMe_Session');
                break;
                
            case 'server_error':
                // Log for monitoring
                this._logForMonitoring(errorType, context);
                break;
        }
    }

    /**
     * Schedule automatic reconnect attempt
     */
    static _scheduleReconnect(context) {
        const key = `${context}_${Date.now()}`;
        this._retryCounters.set(key, 0);
        
        const attempt = async () => {
            const count = this._retryCounters.get(key) || 0;
            
            if (count >= 3) {
                console.warn('[ERROR_RECOVERY] Max retries exceeded for', context);
                this._retryCounters.delete(key);
                return;
            }
            
            this._retryCounters.set(key, count + 1);
            
            // Check network status
            if (navigator.onLine) {
                console.log(`[ERROR_RECOVERY] Reconnecting (${count + 1}/3)...`);
                // Trigger reload or retry specific operation
            }
            
            setTimeout(attempt, Math.pow(2, count) * 1000); // Exponential backoff
        };
        
        attempt();
    }

    /**
     * Log errors for monitoring
     */
    static _logForMonitoring(errorType, context) {
        const logEntry = {
            timestamp: Date.now(),
            type: errorType,
            context,
            userAgent: navigator.userAgent,
            url: window.location.href
        };
        
        // Store in localStorage for later analysis
        try {
            const logs = JSON.parse(localStorage.getItem('error_logs') || '[]');
            logs.push(logEntry);
            // Keep last 50 errors
            if (logs.length > 50) logs.splice(0, logs.length - 50);
            localStorage.setItem('error_logs', JSON.stringify(logs));
        } catch (e) {
            console.warn('[ERROR_RECOVERY] Failed to log error:', e);
        }
    }

    /**
     * Get error statistics
     */
    static getStats() {
        try {
            const logs = JSON.parse(localStorage.getItem('error_logs') || '[]');
            const stats = {};
            
            logs.forEach(log => {
                stats[log.type] = (stats[log.type] || 0) + 1;
            });
            
            return {
                total: logs.length,
                byType: stats,
                recent: logs.slice(-10)
            };
        } catch (e) {
            return { total: 0, byType: {}, recent: [] };
        }
    }

    /**
     * Clear error logs
     */
    static clearLogs() {
        localStorage.removeItem('error_logs');
    }
}

// Register default handlers
ErrorRecovery.registerHandler('auth_error', async (error) => {
    // Auto-redirect to login after 3 seconds
    setTimeout(() => {
        if (!Auth.isAuthenticated()) {
            window.location.hash = '#login';
        }
    }, 3000);
});

export default ErrorRecovery;
