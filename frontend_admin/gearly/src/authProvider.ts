import type { AuthProvider } from "@refinedev/core";

// Backend API root. The admin surface lives under `${API_URL}/admin/**` and is
// locked behind ROLE_ADMIN; login goes through the shared `${API_URL}/users`.
export const API_URL =
  (import.meta.env.VITE_API_URL as string | undefined) ??
  "http://localhost:8080/api";

export const TOKEN_KEY = "gearly-admin-token";
export const IDENTITY_KEY = "gearly-admin-identity";

export const authProvider: AuthProvider = {
  login: async ({ email, password }) => {
    try {
      const res = await fetch(`${API_URL}/users/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        return {
          success: false,
          error: {
            name: "LoginError",
            message: body.error ?? "Invalid email or password",
          },
        };
      }

      const { token, user } = await res.json();

      // The admin API is gated by ROLE_ADMIN. Probe an admin endpoint to make
      // sure this account can actually use the dashboard before letting it in.
      const probe = await fetch(`${API_URL}/admin/users`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (probe.status === 401 || probe.status === 403) {
        return {
          success: false,
          error: {
            name: "NotAuthorized",
            message: "This account is not an administrator.",
          },
        };
      }

      localStorage.setItem(TOKEN_KEY, token);
      localStorage.setItem(
        IDENTITY_KEY,
        JSON.stringify({
          id: user?.id,
          name: user?.fullName || user?.email,
          avatar: user?.profileAvatar,
        }),
      );
      return { success: true, redirectTo: "/" };
    } catch {
      return {
        success: false,
        error: {
          name: "LoginError",
          message: "Unable to reach the server. Please try again.",
        },
      };
    }
  },

  logout: async () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(IDENTITY_KEY);
    return { success: true, redirectTo: "/login" };
  },

  onError: async (error) => {
    const status = error?.response?.status ?? error?.statusCode;
    if (status === 401 || status === 403) {
      return { logout: true, redirectTo: "/login", error };
    }
    return { error };
  },

  check: async () => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) {
      return { authenticated: true };
    }
    return {
      authenticated: false,
      redirectTo: "/login",
      error: { name: "NotAuthenticated", message: "Token not found" },
    };
  },

  getPermissions: async () => "admin",

  getIdentity: async () => {
    const raw = localStorage.getItem(IDENTITY_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  },

  // Password reset is handled on the storefront; keep stubs so the Refine
  // AuthPage renders its links without runtime errors.
  forgotPassword: async () => ({ success: true }),
  updatePassword: async () => ({ success: true }),
};
