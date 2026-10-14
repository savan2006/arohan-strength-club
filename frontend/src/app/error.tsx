"use client";

import Link from "next/link";

type RouteErrorProps = {
    error: Error & { digest?: string };
    reset: () => void;
};

/** Keeps unexpected route errors recoverable instead of leaving a generic framework screen. */
export default function RouteError({ error, reset }: RouteErrorProps) {
    return (
        <main className="portal-loading error-state" role="alert">
            <span className="error-state__mark" aria-hidden="true">!</span>
            <p className="micro-label micro-label--light">AROHAN / RECOVERY</p>
            <h1>That didn’t go to plan.</h1>
            <p className="error-state__message">The page couldn’t finish loading. Please try again, or return to the home page.</p>
            <div className="error-state__actions">
                <button className="button button--primary" onClick={reset}>Try again</button>
                <Link className="error-state__home" href="/">Back to home</Link>
            </div>
            {error.digest ? <small className="error-state__reference">Reference: {error.digest}</small> : null}
        </main>
    );
}
