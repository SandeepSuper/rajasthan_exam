/**
 * admin-auth.js — Shared admin authentication helper.
 * Include this script on every admin page BEFORE any page logic.
 *
 * What it does:
 *  1. Reads the JWT from localStorage.
 *  2. If missing or expired → redirects immediately to /admin/login.
 *  3. Exposes `getAdminToken()` so page scripts can use it in fetch headers.
 *  4. Exposes `adminLogout()` for the logout button.
 */
(function () {
    const TOKEN_KEY = 'admin_jwt';
    const LOGIN_URL = '/admin/login';

    function parseJwtExpiry(token) {
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.exp ? payload.exp * 1000 : null; // convert to ms
        } catch (e) {
            return null;
        }
    }

    function isTokenExpired(token) {
        const expiry = parseJwtExpiry(token);
        if (!expiry) return true;
        return Date.now() >= expiry - 30000; // 30-second buffer before actual expiry
    }

    // ── Redirect to login if no valid token ──────────────────────────────────
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token || isTokenExpired(token)) {
        // Don't redirect if we're already on the login page
        if (!window.location.pathname.endsWith('/login')) {
            window.location.href = LOGIN_URL;
        }
    }

    // ── Expose helpers globally ───────────────────────────────────────────────
    window.getAdminToken = function () {
        return localStorage.getItem(TOKEN_KEY) || '';
    };

    window.adminLogout = function () {
        localStorage.removeItem(TOKEN_KEY);
        window.location.href = LOGIN_URL;
    };
})();
