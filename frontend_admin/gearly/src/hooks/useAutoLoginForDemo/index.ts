/**
 * The admin app previously auto-logged-in with hard-coded demo credentials
 * ("demo@refine.dev"). It now uses real authentication against the backend
 * (see `authProvider`), so this hook is intentionally a no-op: it never blocks
 * rendering, and Refine's <Authenticated> gate redirects to /login when needed.
 *
 * Kept (rather than deleted) only to avoid churning App.tsx's bootstrap.
 */
export const useAutoLoginForDemo = () => ({ loading: false });
