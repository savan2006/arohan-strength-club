import { BrandMark } from "@/components/BrandMark";

/** Lightweight route fallback shown while a portal section is being resolved. */
export default function SectionLoading() {
    return (
        <main className="portal-loading" aria-busy="true" aria-live="polite">
            <BrandMark />
            <div className="loading-bar" role="progressbar" aria-label="Loading portal section" />
            <p>Preparing your training space…</p>
        </main>
    );
}
