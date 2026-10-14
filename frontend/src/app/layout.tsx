import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
    title: "AROHAN Strength Club — Rise Beyond Limits",
    description: "A strength-first gym platform for memberships, personal training, workouts and consistency.",
    applicationName: "AROHAN Strength Club",
};

export const viewport: Viewport = {
    themeColor: "#111111",
    width: "device-width",
    initialScale: 1,
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return (
        <html lang="en">
            <head>
                <link rel="preconnect" href="https://fonts.googleapis.com" />
                <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
                <link
                    rel="stylesheet"
                    href="https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@500;600;700;800;900&family=DM+Sans:wght@400;500;600;700&family=Space+Mono:wght@400;700&display=swap"
                />
            </head>
            <body>{children}</body>
        </html>
    );
}
