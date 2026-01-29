// Common JavaScript functions

// Session management
async function checkSession() {
    try {
        const response = await fetch('/api/auth/check-session');
        const data = await response.json();
        
        if (!data.authenticated && !window.location.pathname.includes('/login') 
            && !window.location.pathname.includes('/register')) {
            window.location.href = '/login';
            return false;
        }
        return true;
    } catch (error) {
        console.error('Session check error:', error);
        if (!window.location.pathname.includes('/login') 
            && !window.location.pathname.includes('/register')) {
            window.location.href = '/login';
        }
        return false;
    }
}

// Initialize on page load for protected pages
if (!window.location.pathname.includes('/login') 
    && !window.location.pathname.includes('/register')) {
    document.addEventListener('DOMContentLoaded', checkSession);
}

// Confirmation dialog for delete actions
function confirmDelete(message = 'Êtes-vous sûr de vouloir supprimer ?') {
    return confirm(message);
}

// Format price
function formatPrice(price) {
    return new Intl.NumberFormat('fr-FR', {
        style: 'currency',
        currency: 'EUR'
    }).format(price);
}

// Show notification
function showNotification(message, type = 'info') {
    const alert = document.createElement('div');
    alert.className = `alert alert-${type} alert-dismissible fade show`;
    alert.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    const container = document.querySelector('.main-content');
    container.insertBefore(alert, container.firstChild);
    
    setTimeout(() => {
        if (alert.parentNode) {
            alert.remove();
        }
    }, 5000);
}

// Form validation helpers
function validateRequired(inputs) {
    let isValid = true;
    inputs.forEach(input => {
        if (!input.value.trim()) {
            input.classList.add('is-invalid');
            isValid = false;
        } else {
            input.classList.remove('is-invalid');
        }
    });
    return isValid;
}

// Export to window object
window.Common = {
    checkSession,
    confirmDelete,
    formatPrice,
    showNotification,
    validateRequired
};