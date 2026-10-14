/** Shared contracts and authenticated HTTP helpers for the browser application. */

export type Role = "MEMBER" | "TRAINER" | "ADMIN";

export type AuthUser = {
    id: string;
    fullName: string;
    email: string;
    phone: string;
    role: Role;
    specialization?: string;
};

export type ApiError = Error & {
    status?: number;
    code?: string;
};

const TOKEN_KEY = "arohan.access-token";
const USER_KEY = "arohan.current-user";
const TAB_KEY = "arohan.tab-session-id";
const API_TIMEOUT_MS = 20_000;

const configuredApiBase = process.env.NEXT_PUBLIC_API_URL?.trim();
export const API_BASE = (configuredApiBase || "http://localhost:8080").replace(/\/+$/, "");

const dateFormatter = new Intl.DateTimeFormat("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
});
const rupeeFormatter = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
});

/** Returns a stable per-tab identifier used to distinguish cross-tab change events. */
function tabSessionId(): string | null {
    if (typeof window === "undefined") {
        return null;
    }

    let value = window.sessionStorage.getItem(TAB_KEY);
    if (!value) {
        value = typeof window.crypto?.randomUUID === "function"
            ? window.crypto.randomUUID()
            : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
        window.sessionStorage.setItem(TAB_KEY, value);
    }
    return value;
}

/** Stores a short-lived browser-tab session; the API remains authoritative for identity and roles. */
export function saveSession(token: string, user: AuthUser): void {
    if (typeof window === "undefined") {
        return;
    }

    window.sessionStorage.setItem(TOKEN_KEY, token);
    window.sessionStorage.setItem(USER_KEY, JSON.stringify(user));
}

/** Removes authentication material after sign-out or an expired/invalid token. */
export function clearSession(): void {
    if (typeof window === "undefined") {
        return;
    }

    window.sessionStorage.removeItem(TOKEN_KEY);
    window.sessionStorage.removeItem(USER_KEY);
}

/** Reads only a structurally valid cached user; server-side authorization never trusts this value. */
export function readStoredUser(): AuthUser | null {
    if (typeof window === "undefined") {
        return null;
    }

    try {
        const raw = window.sessionStorage.getItem(USER_KEY);
        if (!raw) {
            return null;
        }

        const candidate: unknown = JSON.parse(raw);
        if (candidate === null || typeof candidate !== "object") {
            return null;
        }

        const user = candidate as Partial<AuthUser>;
        const validRole = user.role === "MEMBER" || user.role === "TRAINER" || user.role === "ADMIN";
        if (typeof user.id !== "string"
            || typeof user.fullName !== "string"
            || typeof user.email !== "string"
            || !validRole) {
            return null;
        }

        return user as AuthUser;
    }
    catch {
        return null;
    }
}

function apiError(message: string, code: string, status?: number): ApiError {
    const error = new Error(message) as ApiError;
    error.code = code;
    if (status !== undefined) {
        error.status = status;
    }
    return error;
}

/** Makes a JSON API request and normalizes network, timeout, and server errors for the UI. */
export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers(init.headers);
    const method = (init.method || "GET").toUpperCase();
    const isPublicRead = method === "GET"
        && (path === "/api/gym/status" || path === "/api/membership-plans");
    if (init.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    if (typeof window !== "undefined") {
        const token = window.sessionStorage.getItem(TOKEN_KEY);
        if (token && !isPublicRead) {
            headers.set("Authorization", `Bearer ${token}`);
        }

        if (method !== "GET" && method !== "HEAD" && !headers.has("X-Tab-Session-Id")) {
            const tabId = tabSessionId();
            if (tabId) {
                headers.set("X-Tab-Session-Id", tabId);
            }
        }
    }

    let response: Response;
    try {
        // A stalled API must not leave a route in its loading state forever.
        const signal = init.signal ?? (typeof AbortSignal.timeout === "function"
            ? AbortSignal.timeout(API_TIMEOUT_MS)
            : undefined);
        response = await fetch(`${API_BASE}${path}`, {
            ...init,
            headers,
            cache: "no-store",
            ...(signal ? { signal } : {}),
        });
    }
    catch (cause) {
        const errorName = cause instanceof Error ? cause.name : "";
        if (errorName === "TimeoutError") {
            throw apiError("The API took too long to respond. Check the backend and try again.", "API_TIMEOUT");
        }
        if (errorName === "AbortError") {
            throw apiError("The request was cancelled. Please try again.", "REQUEST_ABORTED");
        }
        throw apiError(
            "Could not reach the API. Check that Spring Boot is running and the API URL is correct.",
            "NETWORK_ERROR",
        );
    }

    const raw = await response.text();
    let result: unknown = null;
    if (raw) {
        try {
            result = JSON.parse(raw);
        }
        catch {
            result = raw;
        }
    }

    if (!response.ok) {
        const payload = result !== null && typeof result === "object"
            ? result as Record<string, unknown>
            : {};
        const details = payload.details !== null && typeof payload.details === "object"
            ? Object.entries(payload.details as Record<string, unknown>)
                .map(([field, message]) => `${field}: ${String(message)}`)
                .join(" ")
            : "";
        const message = [payload.message, details]
            .filter((part): part is string => typeof part === "string" && part.length > 0)
            .join(" ") || String(payload.error || `Request failed (${response.status})`);
        const error = apiError(
            message,
            typeof payload.error === "string" ? payload.error : "HTTP_ERROR",
            response.status,
        );

        if (response.status === 401) {
            clearSession();
        }
        throw error;
    }

    return result as T;
}

/** Formats server dates without shifting date-only values across time zones. */
export function friendlyDate(value?: string | null): string {
    if (!value) {
        return "—";
    }

    const date = new Date(value.length === 10 ? `${value}T12:00:00` : value);
    return Number.isNaN(date.getTime()) ? value : dateFormatter.format(date);
}

/** Formats a numeric amount using the Indian rupee locale. */
export function rupees(value: number | string): string {
    const amount = Number(value);
    return rupeeFormatter.format(Number.isFinite(amount) ? amount : 0);
}
