import { notFound } from "next/navigation";
import { PortalPage } from "@/components/PortalPage";
const sections = new Set(["dashboard", "workouts", "exercises", "attendance", "consistency", "membership", "profile", "trainer", "admin-users", "admin-plans", "admin-schedule"]);
export const dynamicParams = false;
export function generateStaticParams() {
    return [...sections].map((section) => ({ section }));
}
export default async function SectionPage({ params }: {
    params: Promise<{
        section: string;
    }>;
}) {
    const { section } = await params;
    if (!sections.has(section))
        notFound();
    return <PortalPage section={section}/>;
}
