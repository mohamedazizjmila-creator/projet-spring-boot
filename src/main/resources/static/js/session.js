// Gestionnaire de session
class SessionManager {
    
    // Vérifier si l'utilisateur est connecté
    static async checkAuth() {
        try {
            const response = await fetch('/api/auth/check-session');
            const data = await response.json();
            return data.authenticated;
        } catch (error) {
            console.error('Erreur de vérification de session:', error);
            return false;
        }
    }
    
    // Récupérer l'utilisateur courant
    static async getCurrentUser() {
        try {
            const response = await fetch('/api/auth/current-user');
            if (response.ok) {
                return await response.json();
            }
            return null;
        } catch (error) {
            console.error('Erreur de récupération utilisateur:', error);
            return null;
        }
    }
    
    // Déconnexion
    static async logout() {
        try {
            await fetch('/api/auth/logout', { method: 'POST' });
            sessionStorage.removeItem('currentUser');
            window.location.href = '/login';
        } catch (error) {
            console.error('Erreur de déconnexion:', error);
        }
    }
    
    // Initialiser la session au chargement de la page
    static async init() {
        const isAuthenticated = await this.checkAuth();
        if (!isAuthenticated && !window.location.pathname.includes('/login') 
            && !window.location.pathname.includes('/register')) {
            window.location.href = '/login';
            return false;
        }
        return true;
    }
}

// Exposer au scope global
window.SessionManager = SessionManager;

// Initialiser au chargement de la page
document.addEventListener('DOMContentLoaded', async () => {
    // Pour les pages protégées, vérifier l'authentification
    if (!window.location.pathname.includes('/login') && 
        !window.location.pathname.includes('/register')) {
        await SessionManager.init();
    }
});