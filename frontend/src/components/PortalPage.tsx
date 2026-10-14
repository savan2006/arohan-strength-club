/* eslint-disable @typescript-eslint/no-explicit-any, react-hooks/set-state-in-effect, react-hooks/refs */
"use client";
import Image from "next/image";
import Link from "next/link";
import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Activity, ArrowRight, ArrowUpRight, Bell, CalendarDays, Check, CheckCircle2, ChevronDown, ChevronRight, CircleHelp, Clock3, Dumbbell, Flame, Gauge, Home, LogOut, Menu, Plus, Search, Settings2, ShieldCheck, Sparkles, UserRound, Users, Wallet, X } from "lucide-react";
import { apiRequest, AuthUser, clearSession, friendlyDate, readStoredUser, Role, rupees } from "@/lib/api";
import { BrandMark } from "./BrandMark";
type AnyRecord = Record<string, any>;
const EMPTY_DAYS: AnyRecord[] = [];

const MEMBER_ONLY_SECTIONS = new Set([
    "workouts",
    "exercises",
    "attendance",
    "consistency",
    "membership",
    "profile",
]);
const ADMIN_ONLY_SECTIONS = new Set(["admin-users", "admin-plans", "admin-schedule"]);

/** Return the correct portal route when a verified role cannot access a section. */
function redirectForRole(role: Role, section: string): string | null {
    if (role === "TRAINER" && section !== "trainer") {
        return "/trainer";
    }
    if (role === "MEMBER" && (section === "trainer" || ADMIN_ONLY_SECTIONS.has(section))) {
        return "/dashboard";
    }
    if (role === "ADMIN" && MEMBER_ONLY_SECTIONS.has(section)) {
        return "/dashboard";
    }
    return null;
}
type SectionMeta = {
    title: string;
    subtitle: string;
    code: string;
};
const SECTION_META: Record<string, SectionMeta> = {
    dashboard: { title: "Your training floor.", subtitle: "A clear read on where you are—and what comes next.", code: "01 / OVERVIEW" },
    workouts: { title: "Make every rep count.", subtitle: "Your plan, one day at a time. Good form before heavy numbers.", code: "02 / TRAINING PLAN" },
    exercises: { title: "Know the movement.", subtitle: "A practical reference for the movements inside your routine.", code: "03 / EXERCISE INDEX" },
    attendance: { title: "Show up. Log it.", subtitle: "Session records make your consistency visible.", code: "04 / CLUB SESSIONS" },
    consistency: { title: "The work adds up.", subtitle: "Review your training rhythm, weekly adherence and logged time.", code: "05 / CONSISTENCY" },
    membership: { title: "Your commitment, formalised.", subtitle: "Choose a plan and keep your access status up to date.", code: "06 / MEMBERSHIP" },
    profile: { title: "Your baseline.", subtitle: "Keep the details behind your training current.", code: "07 / TRAINING PROFILE" },
    trainer: { title: "Coach the process.", subtitle: "Know your roster, build routines and assign work with intent.", code: "08 / TRAINER WORKSPACE" },
    "admin-users": { title: "People, at a glance.", subtitle: "Keep roles, access and membership status accurate.", code: "09 / MEMBER DIRECTORY" },
    "admin-plans": { title: "Set the terms.", subtitle: "Manage the membership options that support the club.", code: "10 / PLAN MANAGEMENT" },
    "admin-schedule": { title: "Set the operating rhythm.", subtitle: "Publish hours and planned closures before members arrive.", code: "11 / FACILITY SETTINGS" },
};
const NAV_MEMBER = [
    { label: "Overview", href: "/dashboard", key: "dashboard", icon: Home, group: "YOUR SPACE" },
    { label: "Workout plan", href: "/workouts", key: "workouts", icon: Dumbbell },
    { label: "Exercise index", href: "/exercises", key: "exercises", icon: Activity },
    { label: "Attendance", href: "/attendance", key: "attendance", icon: CalendarDays },
    { label: "Consistency", href: "/consistency", key: "consistency", icon: Gauge },
    { label: "Membership", href: "/membership", key: "membership", icon: Wallet, group: "YOUR ACCOUNT" },
    { label: "My profile", href: "/profile", key: "profile", icon: UserRound },
];
const NAV_TRAINER = [
    { label: "Roster & plans", href: "/trainer", key: "trainer", icon: Users, group: "COACHING" },
];
const NAV_ADMIN = [
    { label: "Overview", href: "/dashboard", key: "dashboard", icon: Home, group: "OPERATIONS" },
    { label: "Member directory", href: "/admin-users", key: "admin-users", icon: Users },
    { label: "Membership plans", href: "/admin-plans", key: "admin-plans", icon: Wallet },
    { label: "Hours & closures", href: "/admin-schedule", key: "admin-schedule", icon: CalendarDays },
    { label: "Trainer workspace", href: "/trainer", key: "trainer", icon: Dumbbell, group: "COACHING" },
];
const EXERCISES = [
    { name: "Bench Press", muscle: "Chest", equipment: "Barbell", difficulty: "Intermediate", file: "bench-press.svg", instructions: "Retract the shoulder blades, keep feet planted and lower the bar under control." },
    { name: "Incline Press", muscle: "Chest", equipment: "Dumbbells", difficulty: "Intermediate", file: "incline-press.svg", instructions: "Set the bench to a modest incline and press without bouncing at the bottom." },
    { name: "Push-up", muscle: "Chest", equipment: "Bodyweight", difficulty: "Beginner", file: "pushup.svg", instructions: "Keep the trunk braced and the body aligned from head to heel." },
    { name: "Lat Pulldown", muscle: "Back", equipment: "Cable machine", difficulty: "Beginner", file: "lat-pulldown.svg", instructions: "Drive elbows down and avoid swinging your torso to move the weight." },
    { name: "Cable Row", muscle: "Back", equipment: "Cable machine", difficulty: "Beginner", file: "cable-row.svg", instructions: "Stay tall, reach forward without rounding, then pull to the lower ribs." },
    { name: "Deadlift", muscle: "Back + Legs", equipment: "Barbell", difficulty: "Advanced", file: "deadlift.svg", instructions: "Brace before lifting, hinge through the hips and keep the bar close to your body." },
    { name: "Squat", muscle: "Legs", equipment: "Barbell", difficulty: "Intermediate", file: "squats.svg", instructions: "Brace your core, keep knees tracking with toes and use a pain-free range." },
    { name: "Leg Press", muscle: "Legs", equipment: "Machine", difficulty: "Beginner", file: "leg-press.svg", instructions: "Control the lowering phase and avoid locking the knees at the top." },
    { name: "Lunges", muscle: "Legs", equipment: "Dumbbells / bodyweight", difficulty: "Beginner", file: "lunges.svg", instructions: "Step into a balanced stance, keep your torso tall and push through the whole front foot." },
    { name: "Shoulder Press", muscle: "Shoulders", equipment: "Dumbbells", difficulty: "Intermediate", file: "shoulder-press.svg", instructions: "Press overhead while keeping ribs stacked over the pelvis." },
    { name: "Lateral Raise", muscle: "Shoulders", equipment: "Dumbbells", difficulty: "Beginner", file: "lateral-raise.svg", instructions: "Use a light load, lead with the elbows and resist the weight on the way down." },
    { name: "Bicep Curl", muscle: "Arms", equipment: "Dumbbells", difficulty: "Beginner", file: "bicep-curl.svg", instructions: "Keep upper arms still and avoid swinging through the hips." },
    { name: "Tricep Pushdown", muscle: "Arms", equipment: "Cable machine", difficulty: "Beginner", file: "tricep-pushdown.svg", instructions: "Keep elbows pinned near your sides and finish by extending without leaning forward." },
    { name: "Plank", muscle: "Core", equipment: "Bodyweight", difficulty: "Beginner", file: "plank.svg", instructions: "Squeeze glutes, keep hips level and breathe throughout the hold." },
];
/** Today's date (YYYY-MM-DD) in the facility time zone. toISOString() is UTC and is wrong for 00:00–05:30 IST. */
function todayInFacility(): string {
    return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Kolkata" }).format(new Date());
}
function humanize(value?: string | null) {
    if (!value)
        return "—";
    return value.toLowerCase().split("_").map((s) => s.charAt(0).toUpperCase() + s.slice(1)).join(" ");
}
function makePlanRequest(name: string, goal: string) {
    const days = [
        { dayNumber: 1, dayName: "Monday", focus: "Chest + Triceps", exercises: ["Bench Press", "Incline Press", "Tricep Pushdown"] },
        { dayNumber: 2, dayName: "Tuesday", focus: "Back + Biceps", exercises: ["Lat Pulldown", "Cable Row", "Bicep Curl"] },
        { dayNumber: 3, dayName: "Wednesday", focus: "Legs + Core", exercises: ["Squat", "Leg Press", "Plank"] },
        { dayNumber: 4, dayName: "Thursday", focus: "Shoulders + Conditioning", exercises: ["Shoulder Press", "Lateral Raise", "Push-up"] },
    ];
    return {
        name: name.trim(), description: "A coach-led routine built for consistent, measurable progress.", fitnessGoal: goal,
        days: days.map((day) => ({ dayNumber: day.dayNumber, dayName: day.dayName, focus: day.focus, dayType: "WORKOUT", exercises: day.exercises.map((exerciseName) => {
                const source = EXERCISES.find((item) => item.name.toLowerCase() === exerciseName.toLowerCase());
                return { exerciseName, muscleGroup: source?.muscle || "Full Body", sets: 3, reps: goal === "STRENGTH" ? "4-6" : "8-12", restSeconds: 60, instructions: source?.instructions || "Use controlled movement and a comfortable range.", imageUrl: source ? `/exercises/${source.file}` : "" };
            }) })),
    };
}
function getTabSessionId() {
    const key = "arohan.tab-session-id";
    let value = window.sessionStorage.getItem(key);
    if (!value) {
        value = typeof window.crypto?.randomUUID === "function" ? window.crypto.randomUUID() : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
        window.sessionStorage.setItem(key, value);
    }
    return value;
}
export function PortalPage({ section }: {
    section: string;
}) {
    const router = useRouter();
    const [user, setUser] = useState<AuthUser | null>(null);
    const [data, setData] = useState<AnyRecord>({});
    const currentSectionRef = useRef(section);
    currentSectionRef.current = section;
    const updateSectionData = useCallback((key: string, next: AnyRecord) => {
        // Prevent slower requests from a previous route from overwriting the current section.
        if (currentSectionRef.current === key) {
            setData(next);
        }
    }, []);
    const [loading, setLoading] = useState(true);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");
    const [dataChangedInAnotherSession, setDataChangedInAnotherSession] = useState(false);
    const syncVersionRef = useRef(0);
    const [search, setSearch] = useState("");
    const [exerciseGroup, setExerciseGroup] = useState("All movements");
    const [routineName, setRoutineName] = useState("My strength split");
    const [trainerPlanName, setTrainerPlanName] = useState("Foundations — 4 day split");
    const [trainerPlanGoal, setTrainerPlanGoal] = useState("GENERAL_FITNESS");
    const [selectedAssignPlan, setSelectedAssignPlan] = useState<Record<string, string>>({});
    const [profileForm, setProfileForm] = useState<AnyRecord>({});
    const [planForm, setPlanForm] = useState({ name: "", durationMonths: "1", price: "1499", description: "", features: "Full gym access\nWorkout tracker\nExercise library" });
    const [closureForm, setClosureForm] = useState({ date: "", reason: "", announcement: "" });
    const loadData = useCallback(async (current: AuthUser, key = section) => {
        if (key === "dashboard") {
            if (current.role === "ADMIN")
                updateSectionData(key, { overview: await apiRequest("/api/admin/system-overview") });
            else if (current.role === "TRAINER") {
                const [members, plans] = await Promise.all([
                    apiRequest<AnyRecord[]>("/api/trainer/members"),
                    apiRequest<AnyRecord[]>("/api/trainer/plans"),
                ]);
                updateSectionData(key, { members, plans });
            }
            else
                updateSectionData(key, { dashboard: await apiRequest("/api/member/dashboard") });
        }
        else if (["workouts", "exercises", "attendance", "consistency", "profile", "membership"].includes(key)) {
            if (key === "workouts")
                updateSectionData(key, { plan: await apiRequest("/api/member/workout-plan") });
            if (key === "exercises")
                updateSectionData(key, {});
            if (key === "attendance") {
                const [currentSession, history] = await Promise.all([apiRequest<AnyRecord>("/api/member/attendance/current-session"), apiRequest<AnyRecord[]>("/api/member/attendance")]);
                updateSectionData(key, { currentSession, history });
            }
            if (key === "consistency") {
                const [consistency, fitnessLevel] = await Promise.all([apiRequest<AnyRecord>("/api/member/consistency"), apiRequest<AnyRecord>("/api/member/fitness-level")]);
                updateSectionData(key, { consistency, fitnessLevel });
            }
            if (key === "profile") {
                const [profile, trainers] = await Promise.all([apiRequest<AnyRecord>("/api/member/profile"), apiRequest<AnyRecord[]>("/api/member/trainers")]);
                updateSectionData(key, { profile, trainers });
                if (currentSectionRef.current === key) {
                    setProfileForm({
                        fullName: profile.fullName,
                        phone: profile.phone,
                        age: String(profile.age),
                        heightCm: String(profile.heightCm),
                        weightKg: String(profile.weightKg),
                        fitnessGoal: profile.fitnessGoal,
                        experienceLevel: profile.experienceLevel,
                        plannedDaysPerWeek: String(profile.plannedDaysPerWeek),
                        preferredSchedule: profile.preferredSchedule,
                        wantsTrainer: profile.wantsTrainer,
                        trainerId: profile.trainerId || "",
                    });
                }
            }
            if (key === "membership") {
                const [plans, profile] = await Promise.all([apiRequest<AnyRecord[]>("/api/membership-plans"), apiRequest<AnyRecord>("/api/member/profile")]);
                updateSectionData(key, { plans, profile });
            }
        }
        else if (key === "trainer") {
            const [members, plans] = await Promise.all([apiRequest<AnyRecord[]>("/api/trainer/members"), apiRequest<AnyRecord[]>("/api/trainer/plans")]);
            updateSectionData(key, { members, plans });
        }
        else if (key === "admin-users")
            updateSectionData(key, { users: await apiRequest<AnyRecord[]>("/api/admin/users") });
        else if (key === "admin-plans")
            updateSectionData(key, { plans: await apiRequest<AnyRecord[]>("/api/admin/plans") });
        else if (key === "admin-schedule") {
            const [schedule, closures] = await Promise.all([apiRequest<AnyRecord[]>("/api/admin/schedule"), apiRequest<AnyRecord[]>("/api/admin/closures")]);
            updateSectionData(key, { schedule: schedule.map((d) => ({ ...d, openingTime: String(d.openingTime || "06:00").slice(0, 5), closingTime: String(d.closingTime || "22:00").slice(0, 5) })), closures });
        }
    }, [section, updateSectionData]);
    useEffect(() => {
        if (!user) {
            syncVersionRef.current = 0;
            setDataChangedInAnotherSession(false);
            return;
        }
        let mounted = true;
        const checkForChanges = async () => {
            try {
                const version = syncVersionRef.current;
                const result = await apiRequest<AnyRecord>(`/api/sync/status?clientVersion=${version}`, { headers: { "X-Tab-Session-Id": getTabSessionId() } });
                if (!mounted)
                    return;
                if (version === 0)
                    syncVersionRef.current = Number(result.latestVersion || 0);
                else if (result.hasChanges)
                    setDataChangedInAnotherSession(true);
                else
                    syncVersionRef.current = Number(result.latestVersion || version);
            }
            catch {
                // Sync is advisory; transient polling failures must not interrupt the user workflow.
            }
        };
        void checkForChanges();
        const interval = window.setInterval(() => { if (!document.hidden)
            void checkForChanges(); }, 20000);
        return () => { mounted = false; window.clearInterval(interval); };
    }, [user]);
    useEffect(() => {
        let active = true;

        async function bootstrap() {
            setLoading(true);
            setError("");

            const storedUser = readStoredUser();
            if (!storedUser) {
                router.replace("/login");
                setLoading(false);
                return;
            }

            try {
                // Validate identity and load the requested data concurrently to reduce page latency.
                // Avoid fetching protected section data when the cached role already requires a redirect.
                const cachedRedirect = redirectForRole(storedUser.role, section);
                const dataRequest = cachedRedirect
                    ? Promise.resolve()
                    : loadData(storedUser);
                const [identityResult, dataResult] = await Promise.allSettled([
                    apiRequest<AuthUser>("/api/auth/me"),
                    dataRequest,
                ]);
                if (!active) return;

                if (identityResult.status === "rejected") {
                    throw identityResult.reason;
                }

                const current = identityResult.value;
                setUser(current);

                const requiredRoute = redirectForRole(current.role, section);
                if (requiredRoute) {
                    router.replace(requiredRoute);
                    return;
                }

                // The stored role may be stale after an administrator changes account permissions.
                // Reload once using the server-verified identity only when that role has changed.
                if (current.role !== storedUser.role) {
                    await loadData(current);
                } else if (dataResult.status === "rejected") {
                    throw dataResult.reason;
                }
            }
            catch (cause) {
                if (!active) return;
                if ((cause as { status?: number })?.status === 401) {
                    clearSession();
                    router.replace("/login");
                }
                else {
                    setError(cause instanceof Error ? cause.message : "Unable to load this section. Please try again.");
                }
            }
            finally {
                if (active) setLoading(false);
            }
        }

        void bootstrap();
        return () => {
            active = false;
        };
    }, [section, router, loadData]);

    async function act<T = unknown>(path: string, method = "POST", body?: unknown, success = "Changes saved.", reload = true) {
        setBusy(true);
        setError("");
        setNotice("");
        try {
            const result = await apiRequest<T>(path, { method, ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
            setNotice(success);
            if (reload && user) {
                const sectionAtRefresh = section;
                void loadData(user).catch((cause) => {
                    if (currentSectionRef.current === sectionAtRefresh) {
                        setError(cause instanceof Error ? cause.message : "Changes were saved, but updated data could not be loaded.");
                    }
                });
            }
            return result;
        }
        catch (cause) {
            setError(cause instanceof Error ? cause.message : "That action could not be completed.");
            return undefined;
        }
        finally {
            setBusy(false);
        }
    }
    async function dismissDataChangeNotice() {
        setDataChangedInAnotherSession(false);
        try {
            const result = await apiRequest<AnyRecord>(`/api/sync/status?clientVersion=0`, { headers: { "X-Tab-Session-Id": getTabSessionId() } });
            syncVersionRef.current = Number(result.latestVersion || 0);
        }
        catch { /* The next poll establishes a fresh baseline if this request fails. */ }
    }
    function logout() { void apiRequest("/api/auth/logout", { method: "POST" }).catch(() => undefined); clearSession(); router.replace("/"); }
    function updateProfileField(key: string, value: string) { setProfileForm((old) => ({ ...old, [key]: value })); }
    function updateSchedule(day: string, key: string, value: string | boolean) {
        setData((old: AnyRecord) => ({ ...old, schedule: (old.schedule || []).map((row: AnyRecord) => row.dayOfWeek === day ? { ...row, [key]: value } : row) }));
    }
    const meta = SECTION_META[section] || SECTION_META.dashboard;
    const nav = user?.role === "ADMIN" ? NAV_ADMIN : user?.role === "TRAINER" ? NAV_TRAINER : NAV_MEMBER;
    const initials = user?.fullName?.split(/\s+/).map((name) => name[0]).slice(0, 2).join("").toUpperCase() || "A";
    if (!user)
        return <main className="portal-loading"><BrandMark /><div className="loading-bar"/><p>Preparing your training space…</p></main>;
    return (<div className="portal-shell">
      <aside className="sidebar">
        <div className="sidebar-brand"><Link href={user.role === "MEMBER" ? "/dashboard" : user.role === "TRAINER" ? "/trainer" : "/dashboard"}><BrandMark /></Link><button className="icon-button sidebar-close" aria-label="Close navigation" onClick={() => document.body.classList.remove("nav-open")}><X size={18}/></button></div>
        <div className="sidebar-club-state"><span className="sidebar-state-mark"/><div><span>MEMBER SYSTEM</span><strong>AROHAN / ACTIVE</strong></div></div>
        <nav className="side-nav" aria-label="Member navigation">
          {nav.map((item) => <div key={item.key}>
            {item.group && <div className="side-nav-label">{item.group}</div>}
            <Link href={item.href} className={`side-nav-item ${section === item.key ? "side-nav-item--active" : ""}`}><item.icon size={17} strokeWidth={1.8}/><span>{item.label}</span>{section === item.key && <span className="nav-active-mark"/>}</Link>
          </div>)}
        </nav>
        <div className="sidebar-bottom"><div className="sidebar-slogan"><span>SHOW UP.</span><span>PUT IN THE WORK.</span><span>RISE BEYOND LIMITS.</span><i /></div><div className="sidebar-user"><div className="avatar avatar--dark">{initials}</div><div className="sidebar-user-info"><strong>{user.fullName}</strong><span>{humanize(user.role)}</span></div><button className="icon-button logout-button" aria-label="Sign out" onClick={logout}><LogOut size={16}/></button></div></div>
      </aside>

      <main className="portal-main">
        <header className="portal-topbar">
          <button className="icon-button mobile-menu" aria-label="Open navigation" onClick={() => document.body.classList.add("nav-open")}><Menu size={20}/></button>
          <div className="topbar-breadcrumb"><span>AROHAN STRENGTH CLUB</span><ChevronRight size={13}/><span>{meta.code.split(" / ")[1]}</span></div>
          <div className="topbar-right"><span className="topbar-date">{new Intl.DateTimeFormat("en-IN", { weekday: "short", day: "2-digit", month: "short" }).format(new Date())}</span><span className="topbar-divider"/><div className="topbar-user"><span className="avatar">{initials}</span><div><strong>{user.fullName}</strong><small>{humanize(user.role)}</small></div><ChevronDown size={14}/></div></div>
        </header>

        <div className="portal-content">
          {dataChangedInAnotherSession && <div className="sync-change-banner" role="status"><div><span className="sync-change-mark"><Bell size={16}/></span><span><strong>Updates are available.</strong><small>Another tab or device changed data relevant to your account. Reload to see the latest state.</small></span></div><div><button className="button button--primary button--small" onClick={() => window.location.reload()}>Reload now <ArrowRight size={14}/></button><button className="sync-later-button" onClick={dismissDataChangeNotice}>Later</button></div></div>}
          <div className="page-intro"><div><div className="eyebrow"><span className="eyebrow-dot"/> {meta.code}</div><h1>{meta.title}</h1><p>{meta.subtitle}</p></div><div className="page-intro-side"><div className="date-block"><small>TODAY / FACILITY TIME</small><strong>{new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date())}</strong><span>Asia / Kolkata</span></div><div className="page-mark">A<span>/</span>01</div></div></div>
          {error && <div className="alert alert--error" role="alert"><CircleHelp size={16}/>{error}<button className="alert-dismiss" onClick={() => setError("")} aria-label="Dismiss error"><X size={15}/></button></div>}
          {notice && <div className="alert alert--success" role="status"><CheckCircle2 size={16}/>{notice}<button className="alert-dismiss" onClick={() => setNotice("")} aria-label="Dismiss notice"><X size={15}/></button></div>}
          {loading ? <div className="section-loading" aria-busy="true" style={{ padding: "48px 0" }}><div className="loading-bar" role="progressbar" aria-label="Loading section"/></div> : <>
          {section === "dashboard" && user.role === "MEMBER" && <MemberDashboard data={data.dashboard || {}} user={user}/>}
          {section === "dashboard" && user.role === "ADMIN" && <AdminDashboard data={data.overview || {}}/>}
          {section === "workouts" && <WorkoutSection plan={data.plan} busy={busy} routineName={routineName} setRoutineName={setRoutineName} act={act}/>}
          {section === "exercises" && <ExerciseSection search={search} setSearch={setSearch} group={exerciseGroup} setGroup={setExerciseGroup}/>}
          {section === "attendance" && <AttendanceSection data={data} busy={busy} act={act}/>}
          {section === "consistency" && <ConsistencySection data={data.consistency || {}} fitnessLevel={data.fitnessLevel || {}} busy={busy} act={act}/>}
          {section === "membership" && <MembershipSection data={data} busy={busy} act={act}/>}
          {section === "profile" && <ProfileSection profileForm={profileForm} update={updateProfileField} trainers={data.trainers || []} busy={busy} act={act}/>}
          {section === "trainer" && <TrainerSection data={data} busy={busy} act={act} name={trainerPlanName} setName={setTrainerPlanName} goal={trainerPlanGoal} setGoal={setTrainerPlanGoal} selectedAssignPlan={selectedAssignPlan} setSelectedAssignPlan={setSelectedAssignPlan}/>}
          {section === "admin-users" && <AdminUsersSection users={data.users || []} busy={busy} act={act}/>}
          {section === "admin-plans" && <AdminPlansSection plans={data.plans || []} busy={busy} act={act} form={planForm} setForm={setPlanForm}/>}
          {section === "admin-schedule" && <AdminScheduleSection data={data} busy={busy} act={act} closureForm={closureForm} setClosureForm={setClosureForm} updateSchedule={updateSchedule}/>}
          </>}
          <footer className="portal-footer"><span>AROHAN STRENGTH CLUB</span><span>CONSISTENCY OVER INTENSITY. INTENTION OVER EGO.</span><span>ASC / 01</span></footer>
        </div>
      </main>
      <div className="nav-backdrop" onClick={() => document.body.classList.remove("nav-open")}/>
    </div>);
}
function MetricCard({ label, value, note, icon: Icon, red = false }: {
    label: string;
    value: string | number;
    note: string;
    icon: any;
    red?: boolean;
}) {
    return <article className={`metric-card ${red ? "metric-card--red" : ""}`}><div className="metric-top"><span>{label}</span><Icon size={17}/></div><div className="metric-value">{value}</div><div className="metric-note">{note}</div><div className="metric-baseline"/></article>;
}
function MemberDashboard({ data, user }: {
    data: AnyRecord;
    user: AuthUser;
}) {
    const membershipActive = data.membershipStatus === "ACTIVE";
    return <>
    <section className="welcome-banner"><div className="welcome-ghost">ASC</div><div className="welcome-copy"><p className="micro-label micro-label--light">YOUR TRAINING LOG / {new Date().getFullYear()}</p><h2>Good to see you,<br /><em>{data.greeting || user.fullName.split(" ")[0]}.</em></h2><p>Your effort is the constant. Let your plan take care of the direction.</p><div className="welcome-actions"><Link href="/workouts" className="button button--paper">Open workout plan <ArrowRight size={16}/></Link><Link href="/attendance" className="welcome-link">View training log <ArrowUpRight size={16}/></Link></div></div><div className="welcome-stats"><div><small>MEMBERSHIP</small><strong className={membershipActive ? "text-green" : "text-red"}>{humanize(data.membershipStatus)}</strong><span>{data.membershipExpiry ? `Valid through ${friendlyDate(data.membershipExpiry)}` : "Activate a plan to unlock check-ins"}</span></div><div><small>ACTIVE ROUTINE</small><strong>{data.activeWorkoutName || "Recommended split"}</strong><span>Structured around your goal</span></div></div><div className="welcome-stripe"/></section>
    <div className="metric-grid"><MetricCard label="CLUB VISITS / MONTH" value={data.monthlyVisits ?? 0} note="All recorded visits this month" icon={CalendarDays}/><MetricCard label="TRAINING TIME" value={`${Math.floor((data.workoutMinutesThisMonth || 0) / 60)}h ${(data.workoutMinutesThisMonth || 0) % 60}m`} note="Time logged at the club" icon={Clock3}/><MetricCard label="WEEKLY CONSISTENCY" value={`${data.consistencyPercent ?? 0}%`} note={`${data.streak || 0} day current streak`} icon={Flame} red/><MetricCard label="PLANNED TRAINING" value={`${data.plannedDaysPerWeek || 4} days`} note="Your weekly training target" icon={Dumbbell}/></div>
    <div className="dashboard-lower-grid"><section className="panel panel--large"><div className="panel-heading"><div><span className="micro-label">THE NEXT SESSION</span><h3>Keep the chain moving.</h3></div><span className="panel-number">01 /</span></div><div className="next-session"><div className="next-session-mark"><Dumbbell size={25}/></div><div><strong>{data.activeWorkoutName || "Arohan Recommended Split"}</strong><p>A structured session is ready whenever you are. Start with the warm-up, move with control and log the exercises you complete.</p><div className="session-tags"><span>FORM FIRST</span><span>PROGRESSIVE</span><span>TRACKED</span></div></div></div><div className="panel-bottom-link"><Link href="/workouts">Review your routine <ArrowRight size={16}/></Link><Link href="/attendance">Session history <ArrowUpRight size={15}/></Link></div></section><section className="panel"><div className="panel-heading"><div><span className="micro-label">CLUB ACCESS</span><h3>Before you arrive.</h3></div><Clock3 size={18}/></div><div className="checklist-list"><div><span className="checklist-index">01</span><p><strong>Active membership</strong><small>{membershipActive ? "Your access is active." : "Choose a membership plan to check in."}</small></p><span className={`mini-status ${membershipActive ? "mini-status--good" : "mini-status--alert"}`}>{membershipActive ? "READY" : "ACTION"}</span></div><div><span className="checklist-index">02</span><p><strong>One workout per day</strong><small>Completed sessions are capped by facility date.</small></p><CheckCircle2 size={16}/></div><div><span className="checklist-index">03</span><p><strong>Track exercise progress</strong><small>Check off movements inside an active session.</small></p><CheckCircle2 size={16}/></div></div><Link href="/attendance" className="text-action">Manage today&apos;s session <ArrowRight size={15}/></Link></section></div>
  </>;
}
function AdminDashboard({ data }: {
    data: AnyRecord;
}) {
    return <>
    <section className="welcome-banner welcome-banner--admin"><div className="welcome-ghost">OPS</div><div className="welcome-copy"><p className="micro-label micro-label--light">CLUB OPERATIONS / LIVE OVERVIEW</p><h2>The floor is yours.<br /><em>Run it clean.</em></h2><p>People, access and facility readiness in one operational view.</p><div className="welcome-actions"><Link href="/admin-users" className="button button--paper">Manage members <ArrowRight size={16}/></Link><Link href="/admin-schedule" className="welcome-link">Facility settings <ArrowUpRight size={16}/></Link></div></div><div className="welcome-stats"><div><small>OPERATING DATE</small><strong>{friendlyDate(data.date)}</strong><span>Facility timezone / Asia Kolkata</span></div><div><small>TRAINER REQUESTS</small><strong>{data.pendingTrainerRequests ?? 0}</strong><span>Members waiting for a trainer</span></div></div><div className="welcome-stripe"/></section>
    <div className="metric-grid"><MetricCard label="REGISTERED MEMBERS" value={data.totalMembers ?? 0} note="Member accounts" icon={Users}/><MetricCard label="ACTIVE TRAINERS" value={data.totalTrainers ?? 0} note="Coaching accounts" icon={Dumbbell}/><MetricCard label="ACTIVE MEMBERSHIPS" value={data.activeMemberships ?? 0} note="Access status currently active" icon={ShieldCheck} red/><MetricCard label="TODAY'S VISITS" value={data.visitsToday ?? 0} note="Facility entries recorded today" icon={Activity}/></div>
    <div className="admin-shortcuts"><Link href="/admin-users" className="shortcut-tile"><span>01 / DIRECTORY</span><Users size={22}/><strong>Manage people</strong><small>Roles, memberships and account access</small><ArrowUpRight size={16}/></Link><Link href="/admin-plans" className="shortcut-tile"><span>02 / COMMERCE</span><Wallet size={22}/><strong>Membership plans</strong><small>Pricing, duration and included benefits</small><ArrowUpRight size={16}/></Link><Link href="/admin-schedule" className="shortcut-tile"><span>03 / FACILITY</span><CalendarDays size={22}/><strong>Hours & closures</strong><small>Set weekly hours and announce closures</small><ArrowUpRight size={16}/></Link></div>
  </>;
}
function WorkoutSection({ plan, busy, routineName, setRoutineName, act }: {
    plan: AnyRecord;
    busy: boolean;
    routineName: string;
    setRoutineName: (value: string) => void;
    act: (path: string, method?: string, body?: unknown, success?: string, reload?: boolean) => Promise<any>;
}) {
    const [showCreate, setShowCreate] = useState(false);
    const [editingSchedule, setEditingSchedule] = useState(false);
    const days: AnyRecord[] = Array.isArray(plan?.days) ? plan.days : EMPTY_DAYS;
    const [expanded, setExpanded] = useState<number | null>(1);
    const [weeklySchedule, setWeeklySchedule] = useState<AnyRecord[]>([]);
    useEffect(() => {
        const existing = new Map<number, AnyRecord>(days.map((day: AnyRecord) => [Number(day.dayNumber), day] as [
            number,
            AnyRecord
        ]));
        setWeeklySchedule(Array.from({ length: 7 }, (_, index) => {
            const dayNumber = index + 1;
            const day = existing.get(dayNumber) as AnyRecord | undefined;
            return { dayNumber, dayName: day?.dayName || ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"][index], focus: day?.focus || "Full Body", dayType: day?.dayType || (index < 4 ? "WORKOUT" : "REST") };
        }));
    }, [plan?.id, plan?.updatedAt, days]);
    async function saveWeeklySchedule(event: FormEvent) {
        event.preventDefault();
        const workoutDays = weeklySchedule.filter((day) => day.dayType === "WORKOUT").length;
        if (workoutDays < 3 || workoutDays > 6)
            return;
        const saved = await act("/api/member/weekly-schedule", "PUT", { weekSchedule: weeklySchedule }, "Your seven-day training schedule has been saved.");
        if (saved)
            setEditingSchedule(false);
    }
    async function createCustom(event: FormEvent) {
        event.preventDefault();
        const request = makePlanRequest(routineName, plan?.fitnessGoal || "GENERAL_FITNESS");
        const created = await act("/api/member/workout-plan", "POST", request, "Custom workout plan created and activated.");
        if (created)
            setShowCreate(false);
    }
    return <>
    <div className="section-action-row"><div className="inline-note"><Sparkles size={16}/><span><strong>{plan?.source === "TRAINER" ? "Trainer assigned" : plan?.source === "CUSTOM" ? "Your custom plan" : "Recommended for your goal"}</strong><small>{plan?.description || "A routine you can follow and refine over time."}</small></span></div><div className="action-row-buttons"><button className="button button--outline" type="button" disabled={busy} onClick={() => { if (plan?.source === "TRAINER" && !window.confirm("This replaces the plan your trainer assigned. Continue?")) return; void act("/api/member/workout-plan/generate", "POST", undefined, "A recommended split has been generated."); }}>Regenerate plan <Sparkles size={16}/></button><button className="button button--primary" type="button" onClick={() => setShowCreate((visible) => !visible)}><Plus size={16}/> Custom plan</button></div></div>
    {showCreate && <form onSubmit={createCustom} className="inline-create-form"><div><span className="micro-label">MEMBER BUILDER</span><h3>Name the routine.</h3><p>Creates a four-day editable foundation that you can refine later.</p></div><div className="inline-create-controls"><input aria-label="Custom plan name" value={routineName} onChange={(e) => setRoutineName(e.target.value)} required maxLength={120}/><button className="button button--primary" disabled={busy}>Create plan <ArrowRight size={15}/></button></div></form>}
    <div className="workout-summary-strip"><div><small>PROGRAMME</small><strong>{plan?.name || "Arohan Recommended Split"}</strong></div><div><small>FOCUS GOAL</small><strong>{humanize(plan?.fitnessGoal)}</strong></div><div><small>TRAINING DAYS</small><strong>{days.filter((d: AnyRecord) => d.dayType === "WORKOUT").length} sessions / week</strong></div><div><small>PLAN SOURCE</small><strong>{humanize(plan?.source)}</strong></div></div>
    <section className="panel weekly-schedule-panel"><div className="panel-heading"><div><span className="micro-label">WEEKLY RHYTHM</span><h3>Make the week fit your life.</h3></div><CalendarDays size={19}/></div><p>Choose training and recovery days. The active plan keeps its existing exercises; a newly activated training day borrows a safe baseline from your current routine.</p>
      {!editingSchedule ? <div className="weekly-schedule-strip">{weeklySchedule.map((day: AnyRecord) => <div className={`weekly-schedule-chip ${day.dayType === "WORKOUT" ? "weekly-schedule-chip--workout" : ""}`} key={day.dayNumber}><span>{day.dayName.slice(0, 3).toUpperCase()}</span><strong>{day.dayType === "WORKOUT" ? "TRAIN" : "REST"}</strong></div>)}<button type="button" className="button button--outline button--small" onClick={() => setEditingSchedule(true)}><Settings2 size={14}/> Edit week</button></div> : <form onSubmit={saveWeeklySchedule} className="weekly-schedule-editor"><div className="weekly-schedule-editor-grid">{weeklySchedule.map((day: AnyRecord) => <div className="weekly-schedule-edit-day" key={day.dayNumber}><div><span>{String(day.dayNumber).padStart(2, "0")}</span><strong>{day.dayName}</strong></div><label>Plan for the day<select value={day.dayType} onChange={(event) => setWeeklySchedule((old) => old.map((item) => item.dayNumber === day.dayNumber ? { ...item, dayType: event.target.value, focus: event.target.value === "REST" ? "Recovery" : (item.focus === "Recovery" ? "Full Body" : item.focus) } : item))}><option value="WORKOUT">Training day</option><option value="REST">Recovery day</option></select></label><label>Focus<input value={day.focus} maxLength={100} onChange={(event) => setWeeklySchedule((old) => old.map((item) => item.dayNumber === day.dayNumber ? { ...item, focus: event.target.value } : item))} required/></label></div>)}</div><div className="weekly-schedule-editor-footer"><span className="small-print">Choose 3–6 training days each week. Rest days are included in the plan.</span><div><button type="button" className="button button--outline button--small" onClick={() => { const current = new Map<number, AnyRecord>(days.map((day: AnyRecord) => [Number(day.dayNumber), day] as [
        number,
        AnyRecord
    ])); setWeeklySchedule(Array.from({ length: 7 }, (_, index) => { const day = current.get(index + 1) as AnyRecord | undefined; return { dayNumber: index + 1, dayName: day?.dayName || ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"][index], focus: day?.focus || "Full Body", dayType: day?.dayType || (index < 4 ? "WORKOUT" : "REST") }; })); setEditingSchedule(false); }}>Cancel</button><button type="submit" className="button button--primary button--small" disabled={busy || weeklySchedule.filter((day) => day.dayType === "WORKOUT").length < 3 || weeklySchedule.filter((day) => day.dayType === "WORKOUT").length > 6}>Save weekly rhythm <ArrowRight size={14}/></button></div></div></form>}
    </section>
    <div className="workout-day-list">{days.map((day: AnyRecord) => <article key={day.id || day.dayNumber} className={`workout-day ${expanded === day.dayNumber ? "workout-day--expanded" : ""}`}><button className="workout-day-head" onClick={() => setExpanded(expanded === day.dayNumber ? null : day.dayNumber)} type="button"><div className="workout-day-index">{String(day.dayNumber).padStart(2, "0")}</div><div className="workout-day-name"><span>{day.dayName}</span><strong>{day.focus}</strong><small>{day.dayType === "REST" ? "Recovery day" : `${day.exercises?.length || 0} movements · ${humanize(day.dayType)}`}</small></div><div className="workout-day-head-status">{day.dayType === "REST" ? <span className="chip">RECOVERY</span> : <span className="chip chip--red">TRAIN</span>}<ChevronDown size={17}/></div></button>{expanded === day.dayNumber && <div className="workout-day-body">{day.dayType === "REST" ? <div className="rest-day-note"><Flame size={20}/><div><strong>Recovery is part of the programme.</strong><p>Use this day for a light walk, hydration and sleep. Consistency includes respecting recovery.</p></div></div> : <><div className="exercise-table-head"><span>MOVEMENT</span><span>SETS × REPS</span><span>REST</span><span>GUIDANCE</span></div>{day.exercises?.map((exercise: AnyRecord, index: number) => <div className="exercise-row" key={exercise.id || `${exercise.exerciseName}-${index}`}><div className="exercise-row-name"><span className="exercise-row-index">{String(index + 1).padStart(2, "0")}</span><div><strong>{exercise.exerciseName}</strong><small>{exercise.muscleGroup}</small></div></div><span className="exercise-prescription">{exercise.sets} × {exercise.reps}</span><span className="exercise-rest">{exercise.restSeconds}s</span><p>{exercise.instructions || "Control the movement and use a comfortable range."}</p></div>)}<div className="complete-day-row"><span><CheckCircle2 size={16}/> Mark this workout as completed after training.</span><button className="button button--outline button--small" onClick={() => act(`/api/member/workout-plan/day/${day.dayNumber}/toggle-complete`, "PATCH", undefined, "Workout completion recorded.")} disabled={busy}>Mark complete <Check size={15}/></button></div></>}</div>}</article>)}</div>
    <div className="subtle-warning"><ShieldCheck size={16}/><p><strong>Train within your limits.</strong> Exercise guidance is informational, not medical advice. Use a spotter for heavy lifts and stop if you feel pain.</p></div>
  </>;
}
function ExerciseSection({ search, setSearch, group, setGroup }: {
    search: string;
    setSearch: (value: string) => void;
    group: string;
    setGroup: (value: string) => void;
}) {
    const groups = ["All movements", ...Array.from(new Set(EXERCISES.map((e) => e.muscle)))];
    const shown = EXERCISES.filter((exercise) => (group === "All movements" || exercise.muscle === group) && `${exercise.name} ${exercise.muscle} ${exercise.equipment}`.toLowerCase().includes(search.toLowerCase()));
    return <>
    <div className="exercise-tools"><div className="search-field"><Search size={17}/><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search movement, muscle or equipment" aria-label="Search exercises"/><kbd>/</kbd></div><div className="exercise-count">{shown.length} MOVEMENTS</div></div>
    <div className="filter-pills">{groups.map((item) => <button type="button" key={item} className={`filter-pill ${group === item ? "filter-pill--active" : ""}`} onClick={() => setGroup(item)}>{item}</button>)}</div>
    <div className="exercise-library-grid">{shown.map((exercise, index) => <article key={exercise.name} className="exercise-library-card"><div className="exercise-art"><Image src={`/exercises/${exercise.file}`} alt={`${exercise.name} illustration`} width={132} height={132}/><span className="exercise-art-number">{String(index + 1).padStart(2, "0")}</span><span className="exercise-art-muscle">{exercise.muscle.toUpperCase()}</span></div><div className="exercise-library-info"><div className="exercise-library-meta"><span>{exercise.equipment}</span><span>{exercise.difficulty}</span></div><h3>{exercise.name}</h3><p>{exercise.instructions}</p><div className="exercise-card-foot"><span>FORM / FIRST</span><ArrowUpRight size={15}/></div></div></article>)}</div>
    {!shown.length && <div className="empty-state"><Search size={24}/><h3>No movement found.</h3><p>Try a different search term or muscle group.</p><button className="button button--outline" onClick={() => { setSearch(""); setGroup("All movements"); }}>Reset filters</button></div>}
  </>;
}
function AttendanceSection({ data, busy, act }: {
    data: AnyRecord;
    busy: boolean;
    act: (path: string, method?: string, body?: unknown, success?: string, reload?: boolean) => Promise<any>;
}) {
    const active = data.currentSession?.active ? data.currentSession.session : null;
    const history: AnyRecord[] = data.history || [];
    return <>
    <section className={`attendance-live ${active ? "attendance-live--active" : ""}`}><div className="attendance-live-top"><span className="micro-label">LIVE SESSION / {active ? "IN PROGRESS" : "NO ACTIVE SESSION"}</span><span className={`status-chip ${active ? "status-chip--live" : ""}`}><span className="status-pip"/>{active ? "CHECKED IN" : "READY WHEN YOU ARE"}</span></div><div className="attendance-live-main"><div><h2>{active ? "Make today count." : "Start with one session."}</h2><p>{active ? `Checked in at ${new Date(active.checkInTime).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}. Mark each movement as you go, then check out to finish the record.` : "Check in during club operating hours to start a timed session. Your membership must be active."}</p></div><div className="attendance-live-actions">{active ? <button className="button button--primary" disabled={busy} onClick={() => act("/api/member/attendance/check-out", "POST", undefined, "Session checked out and saved.")}>Check out <ArrowRight size={16}/></button> : <button className="button button--primary" disabled={busy} onClick={() => act("/api/member/attendance/check-in", "POST", undefined, "You're checked in. Have a focused session.")}>Check in <ArrowRight size={16}/></button>}</div></div>
      {active && <div className="active-session-exercises"><div className="active-session-heading"><strong>SESSION CHECKLIST</strong><span>{active.exercises?.filter((e: AnyRecord) => e.completed).length || 0} / {active.exercises?.length || 0} DONE</span></div>{active.exercises?.map((exercise: AnyRecord) => <button key={exercise.id} className={`active-exercise-item ${exercise.completed ? "active-exercise-item--done" : ""}`} onClick={() => act(`/api/member/attendance/active/exercise/${exercise.id}/toggle`, "PATCH", undefined, exercise.completed ? "Exercise marked incomplete." : "Exercise marked complete.")} disabled={busy}><span className="exercise-check">{exercise.completed && <Check size={14}/>}</span><span><strong>{exercise.exerciseName}</strong><small>{exercise.sets} × {exercise.reps} · {exercise.restSeconds}s rest</small></span><span className="exercise-done-label">{exercise.completed ? "DONE" : "MARK DONE"}</span></button>)}</div>}
    </section>
    <div className="section-divider-title"><h3>Session history</h3><span>THE LAST ENTRIES</span></div>
    <div className="data-table-wrap"><table className="data-table"><thead><tr><th>FACILITY DATE</th><th>CHECK IN</th><th>CHECK OUT</th><th>WORKOUT</th><th>DURATION</th><th>STATUS</th></tr></thead><tbody>{history.map((entry) => <tr key={entry.id}><td><strong>{friendlyDate(entry.facilityDate)}</strong></td><td>{entry.checkInTime ? new Date(entry.checkInTime).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" }) : "—"}</td><td>{entry.checkOutTime ? new Date(entry.checkOutTime).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" }) : "—"}</td><td>{entry.workoutDayName || "Open session"}</td><td>{entry.durationMinutes ? `${entry.durationMinutes} min` : "—"}</td><td><span className={`table-status ${entry.status === "COMPLETED" ? "table-status--good" : "table-status--live"}`}>{humanize(entry.status)}</span></td></tr>)}</tbody></table>{history.length === 0 && <div className="table-empty">No visits have been recorded yet. Your first check-in starts the log.</div>}</div>
  </>;
}
function ConsistencySection({ data, fitnessLevel, busy, act }: {
    data: AnyRecord;
    fitnessLevel: AnyRecord;
    busy: boolean;
    act: (path: string, method?: string, body?: unknown, success?: string, reload?: boolean) => Promise<any>;
}) {
    const percent = Math.max(0, Math.min(100, Number(data.percentage || 0)));
    const dashes = Array.from({ length: 7 }, (_, index) => index < Number(data.completedWorkouts || 0));
    return <>
    <div className="consistency-hero"><div><span className="micro-label micro-label--light">WEEKLY ADHERENCE / CURRENT WEEK</span><h2>Your goal is<br /><em>in reach.</em></h2><p>Consistency measures showing up for the plan over time. Rest and recovery days matter too.</p></div><div className="consistency-gauge"><div className="gauge-number">{percent}<span>%</span></div><span>WEEKLY COMPLETION</span><svg viewBox="0 0 220 18" aria-hidden="true"><rect width="220" height="4" y="7" fill="rgba(255,255,255,.18)"/><rect width={220 * percent / 100} height="4" y="7" fill="#ff2633"/></svg></div><div className="consistency-ghost">{String(data.currentStreak || 0).padStart(2, "0")}</div></div>
    <div className="metric-grid metric-grid--three"><MetricCard label="WORKOUTS LOGGED" value={data.completedWorkouts ?? 0} note={`Against ${data.expectedWorkouts || 0} expected so far`} icon={CheckCircle2}/><MetricCard label="CURRENT STREAK" value={`${data.currentStreak ?? 0} days`} note="Consecutive completed calendar days" icon={Flame} red/><MetricCard label="TIME THIS MONTH" value={`${Math.floor((data.workoutMinutesThisMonth || 0) / 60)}h ${(data.workoutMinutesThisMonth || 0) % 60}m`} note={`${data.visitsThisMonth ?? 0} facility entries`} icon={Clock3}/></div>
    <section className="panel consistency-panel"><div className="panel-heading"><div><span className="micro-label">YOUR WEEK / AT A GLANCE</span><h3>Small actions, repeated.</h3></div><span className="panel-number">M—S</span></div><div className="week-progress">{["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"].map((day, index) => <div className={`week-progress-day ${dashes[index] ? "week-progress-day--done" : ""}`} key={day}><span>{day}</span><div>{dashes[index] ? <Check size={16}/> : <span>{String(index + 1).padStart(2, "0")}</span>}</div><small>{dashes[index] ? "LOGGED" : "UP NEXT"}</small></div>)}</div><p className="small-print">This view summarises the current week. It does not penalise rest days or replace professional coaching advice.</p></section>
    <section className="panel level-progress-panel"><div className="level-progress-heading"><div><span className="micro-label">FITNESS LEVEL / EARNED OVER TIME</span><h3>Progress is built, not selected.</h3><p>Your starting level reflects your onboarding baseline. Future promotions use complete calendar months and consistency calculated from recorded sessions; profile edits cannot grant a promotion.</p></div><div className="level-current"><small>CURRENT LEVEL</small><strong>{humanize(fitnessLevel.currentLevel || "BEGINNER")}</strong><span>Since {friendlyDate(fitnessLevel.levelSince)}</span></div></div><div className="level-rules-grid"><div><span>90%+ CONSISTENCY</span><strong>{fitnessLevel.consecutive90 || 0} <small>consecutive months</small></strong></div><div><span>80%+ CONSISTENCY</span><strong>{fitnessLevel.consecutive80 || 0} <small>consecutive months</small></strong></div><button className="button button--primary" disabled={busy || fitnessLevel.currentLevel === "PRO"} onClick={() => act("/api/member/fitness-level/check-promotion", "POST", undefined, "Fitness level checked against your recorded training history.")}>{fitnessLevel.currentLevel === "PRO" ? "Highest level reached" : "Evaluate my level"} <ArrowRight size={15}/></button></div>{fitnessLevel.reason && <p className="level-feedback">{fitnessLevel.reason}</p>}<div className="monthly-consistency"><div className="panel-heading"><div><span className="micro-label">COMPLETED MONTHS</span><h3>Consistency trend</h3></div><span className="small-print">Last 18 months</span></div><div className="monthly-consistency-grid">{(fitnessLevel.monthlyHistory || []).slice(-18).map((month: AnyRecord) => <div className="monthly-consistency-item" key={month.month} title={`${month.month}: ${month.percentage}% (${month.completedWorkouts}/${month.expectedWorkouts} expected sessions)`}><div className="monthly-consistency-bar"><span style={{ height: `${Math.max(2, Math.min(100, Number(month.percentage || 0)))}%` }}/></div><strong>{Math.round(month.percentage)}%</strong><small>{new Date(`${month.month}-01T12:00:00`).toLocaleDateString("en-IN", { month: "short" })}</small></div>)}</div><p className="small-print">A completed session counts once per facility day. The monthly target is rounded up from your planned weekly frequency and the actual number of days in each month.</p></div>{(fitnessLevel.promotionHistory || []).length > 0 && <div className="promotion-history"><span className="micro-label">PROMOTION RECORD</span>{fitnessLevel.promotionHistory.map((entry: AnyRecord) => <div className="promotion-history-row" key={entry.id}><span className="promotion-stamp"><Check size={14}/></span><div><strong>{humanize(entry.fromLevel)} → {humanize(entry.toLevel)}</strong><small>{friendlyDate(entry.promotedAt)} · {entry.reason}</small></div></div>)}</div>}</section>
  </>;
}
function MembershipSection({ data, busy, act }: {
    data: AnyRecord;
    busy: boolean;
    act: (path: string, method?: string, body?: unknown, success?: string, reload?: boolean) => Promise<any>;
}) {
    const profile = data.profile || {};
    const plans: AnyRecord[] = data.plans || [];
    const active = profile.membershipStatus === "ACTIVE" && (!profile.membershipExpiry || profile.membershipExpiry >= todayInFacility());
    return <>
    <div className="membership-status-banner"><div className="membership-status-icon"><Wallet size={22}/></div><div><span className="micro-label">CURRENT ACCESS</span><h3>{active ? "Membership active." : "Your membership is not active."}</h3><p>{active ? `Your current access is scheduled through ${friendlyDate(profile.membershipExpiry)}.` : "Choose a plan below to unlock attendance and member workout actions."}</p></div><span className={`status-chip ${active ? "status-chip--good" : "status-chip--needs"}`}>{humanize(profile.membershipStatus)}</span></div>
    <div className="membership-history-strip"><div><span>LAST SELECTED PLAN</span><strong>{profile.currentMembershipPlan || "No plan selected yet"}</strong></div><div><span>MEMBERSHIP START</span><strong>{friendlyDate(profile.membershipStart)}</strong></div><div><span>CURRENT TERM ENDS</span><strong>{friendlyDate(profile.membershipExpiry)}</strong></div><div><span>ACCUMULATED DURATION</span><strong>{profile.accumulatedDurationMonths || 0} {Number(profile.accumulatedDurationMonths) === 1 ? "month" : "months"}</strong></div></div>
    <div className="public-plans public-plans--portal">{plans.map((plan, index) => <article className={`public-plan ${index === 1 ? "public-plan--featured" : ""}`} key={plan.id}><div className="plan-topline"><span>0{index + 1} / {plan.durationMonths} {plan.durationMonths === 1 ? "MONTH" : "MONTHS"}</span>{index === 1 && <span className="plan-recommended">POPULAR</span>}</div><h3>{plan.name}</h3><p>{plan.description}</p><div className="plan-price">{rupees(plan.price)}<small> / {plan.durationMonths} {plan.durationMonths === 1 ? "month" : "months"}</small></div><ul>{(plan.features || []).map((feature: string) => <li key={feature}><span>+</span>{feature}</li>)}</ul><button className={index === 1 ? "button button--primary button--wide" : "button button--outline button--wide"} disabled={busy || !plan.active} onClick={() => act("/api/member/membership/pay", "POST", { planId: plan.id }, "Membership purchase recorded. Payment is simulated in this build.")}>Select {plan.name} <ArrowRight size={16}/></button></article>)}</div>
    <div className="subtle-warning"><ShieldCheck size={16}/><p><strong>Payment note.</strong> This starter records a simulated membership purchase. Connect and verify a payment gateway webhook before using it to accept real money.</p></div>
  </>;
}
function ProfileSection({ profileForm, update, trainers, busy, act }: {
    profileForm: AnyRecord;
    update: (key: string, value: string) => void;
    trainers: AnyRecord[];
    busy: boolean;
    act: (path: string, method?: string, body?: unknown, success?: string, reload?: boolean) => Promise<any>;
}) {
    async function save(event: FormEvent) {
        event.preventDefault();
        const body = { fullName: profileForm.fullName, phone: profileForm.phone, age: Number(profileForm.age), heightCm: Number(profileForm.heightCm), weightKg: Number(profileForm.weightKg), fitnessGoal: profileForm.fitnessGoal, experienceLevel: profileForm.experienceLevel, plannedDaysPerWeek: Number(profileForm.plannedDaysPerWeek), preferredSchedule: profileForm.preferredSchedule };
        await act("/api/member/profile", "PUT", body, "Training profile updated.");
    }
    async function saveTrainerPreference(event: FormEvent) {
        event.preventDefault();
        await act("/api/member/trainer-preference", "PATCH", { wantsTrainer: String(profileForm.wantsTrainer) === "true", trainerId: profileForm.trainerId || null }, "Trainer preference updated.");
    }
    return <div className="profile-layout"><form onSubmit={save} className="panel profile-form"><div className="panel-heading"><div><span className="micro-label">PERSONAL DETAILS</span><h3>Keep your baseline current.</h3></div><UserRound size={20}/></div><div className="form-grid"><div className="field"><label>Full name</label><input value={profileForm.fullName || ""} onChange={(e) => update("fullName", e.target.value)} required maxLength={120}/></div><div className="field"><label>Phone</label><input value={profileForm.phone || ""} onChange={(e) => update("phone", e.target.value)} required maxLength={20}/></div></div><div className="form-grid form-grid--3"><div className="field"><label>Age</label><input type="number" min="14" max="100" value={profileForm.age || ""} onChange={(e) => update("age", e.target.value)} required/></div><div className="field"><label>Height (cm)</label><input type="number" min="50" max="260" value={profileForm.heightCm || ""} onChange={(e) => update("heightCm", e.target.value)} required/></div><div className="field"><label>Weight (kg)</label><input type="number" min="30" max="300" step="0.1" value={profileForm.weightKg || ""} onChange={(e) => update("weightKg", e.target.value)} required/></div></div><div className="form-grid"><div className="field"><label>Fitness goal</label><select value={profileForm.fitnessGoal || "GENERAL_FITNESS"} onChange={(e) => update("fitnessGoal", e.target.value)}><option value="GENERAL_FITNESS">General fitness</option><option value="MUSCLE_GAIN">Build muscle</option><option value="FAT_LOSS">Fat loss</option><option value="STRENGTH">Strength</option><option value="ENDURANCE">Endurance</option></select></div><div className="field"><label>Current training level</label><select value={profileForm.experienceLevel || "BEGINNER"} disabled><option value="BEGINNER">Beginner</option><option value="INTERMEDIATE">Intermediate</option><option value="ADVANCED">Advanced</option><option value="PRO">Pro</option></select><small className="field-hint">Future levels are earned through completed monthly consistency.</small></div></div><div className="form-grid"><div className="field"><label>Planned days per week</label><select value={profileForm.plannedDaysPerWeek || "4"} onChange={(e) => update("plannedDaysPerWeek", e.target.value)}>{[3, 4, 5, 6].map((n) => <option key={n} value={n}>{n} days</option>)}</select></div><div className="field"><label>Preferred training time</label><select value={profileForm.preferredSchedule || "EVENING"} onChange={(e) => update("preferredSchedule", e.target.value)}><option value="MORNING">Morning</option><option value="AFTERNOON">Afternoon</option><option value="EVENING">Evening</option><option value="FLEXIBLE">Flexible</option></select></div></div><button className="button button--primary" disabled={busy}>Save profile <Check size={16}/></button></form>
    <div className="profile-side"><section className="profile-identity"><div className="profile-avatar"><span>{String(profileForm.fullName || "A").split(/\s+/).map((v: string) => v[0]).slice(0, 2).join("").toUpperCase()}</span></div><span className="micro-label micro-label--light">TRAINING MEMBER</span><h3>{profileForm.fullName || "Your profile"}</h3><p>Fitness goal / {humanize(profileForm.fitnessGoal)}</p><div className="identity-metrics"><div><small>HEIGHT</small><strong>{profileForm.heightCm || "—"} cm</strong></div><div><small>WEIGHT</small><strong>{profileForm.weightKg || "—"} kg</strong></div><div><small>AGE</small><strong>{profileForm.age || "—"}</strong></div></div></section><form className="panel trainer-preference" onSubmit={saveTrainerPreference}><span className="micro-label">COACHING PREFERENCE</span><h3>Train with a coach?</h3><p>Request a trainer or choose one who&apos;s available in the club directory.</p><div className="field"><label>Preference</label><select value={profileForm.wantsTrainer === true ? "true" : profileForm.wantsTrainer || "false"} onChange={(e) => update("wantsTrainer", e.target.value)}><option value="false">Self-guided training</option><option value="true">I&apos;d like a trainer</option></select></div><div className="field"><label>Choose trainer</label><select value={profileForm.trainerId || ""} onChange={(e) => update("trainerId", e.target.value)}><option value="">Request / no preference</option>{trainers.map((trainer) => <option key={trainer.id} value={trainer.id}>{trainer.fullName}{trainer.specialization ? ` · ${trainer.specialization}` : ""}</option>)}</select></div><button className="button button--outline button--wide" disabled={busy}>Update preference <ArrowRight size={15}/></button></form></div></div>;
}
function TrainerSection({ data, busy, act, name, setName, goal, setGoal, selectedAssignPlan, setSelectedAssignPlan }: {
    data: AnyRecord;
    busy: boolean;
    act: (path: string, method?: string, body?: unknown, success?: string, reload?: boolean) => Promise<any>;
    name: string;
    setName: (value: string) => void;
    goal: string;
    setGoal: (value: string) => void;
    selectedAssignPlan: Record<string, string>;
    setSelectedAssignPlan: (value: Record<string, string>) => void;
}) {
    const members: AnyRecord[] = data.members || [];
    const plans: AnyRecord[] = data.plans || [];
    async function createPlan(event: FormEvent) { event.preventDefault(); await act("/api/trainer/plans", "POST", makePlanRequest(name, goal), "Trainer template created."); }
    return <>
    <div className="trainer-stats"><div><span>ASSIGNED MEMBERS</span><strong>{members.length}</strong><small>Current coaching roster</small></div><div><span>PLAN TEMPLATES</span><strong>{plans.length}</strong><small>Reusable routines you own</small></div><div><span>ACCESS MODEL</span><strong>RBAC</strong><small>Only assigned trainees are visible</small></div></div>
    <section className="panel trainer-plan-builder"><div className="panel-heading"><div><span className="micro-label">PLAN LIBRARY / NEW TEMPLATE</span><h3>Build a foundation.</h3></div><Plus size={20}/></div><form onSubmit={createPlan} className="trainer-create-grid"><div className="field"><label>Template name</label><input value={name} onChange={(e) => setName(e.target.value)} maxLength={120} required/></div><div className="field"><label>Primary goal</label><select value={goal} onChange={(e) => setGoal(e.target.value)}><option value="GENERAL_FITNESS">General fitness</option><option value="MUSCLE_GAIN">Muscle gain</option><option value="FAT_LOSS">Fat loss</option><option value="STRENGTH">Strength</option><option value="ENDURANCE">Endurance</option></select></div><button className="button button--primary" disabled={busy}>Create template <ArrowRight size={16}/></button></form><p className="small-print">Creates a standard four-day template with exercise instructions. You can expand and edit the template as your coaching workflow grows.</p></section>
    <div className="section-divider-title"><h3>Member roster</h3><span>{members.length} MEMBERS</span></div>
    <div className="roster-list">{members.map((member) => <article className="roster-card" key={member.id}><div className="roster-avatar">{member.fullName.split(/\s+/).map((v: string) => v[0]).slice(0, 2).join("").toUpperCase()}</div><div className="roster-member"><strong>{member.fullName}</strong><span>{member.email}</span><div className="roster-chips"><span>{humanize(member.fitnessGoal)}</span><span>{humanize(member.experienceLevel)}</span><span className={`table-status ${member.membershipStatus === "ACTIVE" ? "table-status--good" : "table-status--alert"}`}>{humanize(member.membershipStatus)}</span></div></div><div className="roster-consistency"><small>CONSISTENCY</small><strong>{member.consistencyPercent}%</strong><div className="mini-progress"><span style={{ width: `${Math.max(0, Math.min(100, member.consistencyPercent))}%` }}/></div></div><div className="roster-assign"><select aria-label={`Plan for ${member.fullName}`} value={selectedAssignPlan[member.id] || ""} onChange={(e) => setSelectedAssignPlan({ ...selectedAssignPlan, [member.id]: e.target.value })}><option value="">Choose template</option>{plans.map((plan) => <option key={plan.id} value={plan.id}>{plan.name}</option>)}</select><button className="button button--outline button--small" disabled={busy || !selectedAssignPlan[member.id]} onClick={() => act(`/api/trainer/members/${member.id}/assign-plan`, "POST", { planId: selectedAssignPlan[member.id] }, `Workout assigned to ${member.fullName}.`)}>Assign <ArrowRight size={14}/></button></div></article>)}</div>
    {!members.length && <div className="empty-state"><Users size={25}/><h3>Your roster is quiet.</h3><p>Members appear here once an administrator assigns them to your trainer account.</p></div>}
    <div className="section-divider-title"><h3>Your workout templates</h3><span>{plans.length} TEMPLATES</span></div>
    <div className="template-grid">{plans.map((plan) => <article className="template-card" key={plan.id}><span className="micro-label">{humanize(plan.fitnessGoal)} / {plan.days?.length || 0} DAYS</span><h3>{plan.name}</h3><p>{plan.description}</p><div className="template-day-tags">{plan.days?.filter((day: AnyRecord) => day.dayType === "WORKOUT").slice(0, 4).map((day: AnyRecord) => <span key={day.id || day.dayNumber}>{day.focus}</span>)}</div><button className="text-action" onClick={() => act(`/api/trainer/plans/${plan.id}`, "DELETE", undefined, "Template deleted.")} disabled={busy}>Delete template <X size={14}/></button></article>)}</div>
  </>;
}
function AdminUsersSection({ users, busy, act }: {
    users: AnyRecord[];
    busy: boolean;
    act: (path: string, method?: string, body?: unknown, success?: string, reload?: boolean) => Promise<any>;
}) {
    const [roleFilter, setRoleFilter] = useState("ALL");
    const [query, setQuery] = useState("");
    const filtered = users.filter((user) => (roleFilter === "ALL" || user.role === roleFilter) && `${user.fullName} ${user.email} ${user.phone}`.toLowerCase().includes(query.toLowerCase()));
    return <>
    <div className="directory-toolbar"><div className="search-field"><Search size={17}/><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search by name, email or phone"/></div><div className="filter-select"><span>ROLE</span><select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}><option value="ALL">All roles</option><option value="MEMBER">Members</option><option value="TRAINER">Trainers</option><option value="ADMIN">Admins</option></select></div><span className="directory-count">{filtered.length} RESULTS</span></div>
    <div className="data-table-wrap"><table className="data-table data-table--users"><thead><tr><th>PERSON</th><th>ROLE</th><th>MEMBERSHIP</th><th>TRAINER</th><th>MEMBERSHIP START</th><th>VALID THROUGH</th><th>ACCOUNT CREATED</th><th>ACCESS CONTROL</th></tr></thead><tbody>{filtered.map((person) => <tr key={person.id}><td><div className="table-person"><span className="table-person-avatar">{person.fullName.split(/\s+/).map((v: string) => v[0]).slice(0, 2).join("").toUpperCase()}</span><span><strong>{person.fullName}</strong><small>{person.email}</small></span></div></td><td><span className={`role-tag role-tag--${person.role.toLowerCase()}`}>{humanize(person.role)}</span></td><td>{person.membershipStatus ? <span className={`table-status ${person.membershipStatus === "ACTIVE" ? "table-status--good" : "table-status--alert"}`}>{humanize(person.membershipStatus)}</span> : "—"}</td><td>{person.role === "MEMBER" ? <div className="admin-row-actions admin-trainer-cell"><select key={`${person.id}-${person.trainerId || ""}`} aria-label={`Trainer for ${person.fullName}`} defaultValue={person.trainerId || ""} onChange={(e) => act(`/api/admin/members/${person.id}/trainer`, "PUT", { trainerId: e.target.value || null }, e.target.value ? `Trainer assigned to ${person.fullName}.` : `Trainer removed for ${person.fullName}.`)} disabled={busy}><option value="">{person.trainerRequested ? "Requested — assign…" : "No trainer"}</option>{users.filter((candidate) => candidate.role === "TRAINER").map((trainer) => <option key={trainer.id} value={trainer.id}>{trainer.fullName}</option>)}</select></div> : "—"}</td><td>{friendlyDate(person.membershipStart)}</td><td>{friendlyDate(person.membershipExpiry)}</td><td>{friendlyDate(person.createdAt)}</td><td><div className="admin-row-actions"><select aria-label={`Role for ${person.fullName}`} defaultValue={person.role} onChange={(e) => { const next = e.target.value; if (!window.confirm(`Change ${person.fullName} to ${humanize(next)}?`)) { e.target.value = person.role; return; } void act(`/api/admin/users/${person.id}`, "PATCH", { role: next }, `Role updated for ${person.fullName}.`); }} disabled={busy}><option value="MEMBER">Member</option><option value="TRAINER">Trainer</option><option value="ADMIN">Admin</option></select>{person.role === "MEMBER" && <select aria-label={`Membership status for ${person.fullName}`} defaultValue={person.membershipStatus || "EXPIRED"} onChange={(e) => act(`/api/admin/users/${person.id}/status`, "PATCH", { membershipStatus: e.target.value }, `Membership status updated for ${person.fullName}.`)} disabled={busy}><option value="ACTIVE">Active</option><option value="FROZEN">Frozen</option><option value="EXPIRED">Expired</option></select>}<button className="table-delete" type="button" title={`Delete ${person.fullName}`} onClick={() => { if (window.confirm(`Permanently delete ${person.fullName}? This cannot be undone.`))
        act(`/api/admin/users/${person.id}`, "DELETE", undefined, "User account deleted."); }} disabled={busy}>Delete</button></div></td></tr>)}</tbody></table>{filtered.length === 0 && <div className="table-empty">No users match this filter.</div>}</div>
    <div className="subtle-warning"><ShieldCheck size={16}/><p><strong>Administrative authority.</strong> Role updates take effect on the next authenticated request. Deleting an account is permanent; the current administrator cannot delete their own account.</p></div>
  </>;
}
function AdminPlansSection({ plans, busy, act, form, setForm }: {
    plans: AnyRecord[];
    busy: boolean;
    act: (path: string, method?: string, body?: unknown, success?: string, reload?: boolean) => Promise<any>;
    form: {
        name: string;
        durationMonths: string;
        price: string;
        description: string;
        features: string;
    };
    setForm: (value: {
        name: string;
        durationMonths: string;
        price: string;
        description: string;
        features: string;
    }) => void;
}) {
    async function create(event: FormEvent) {
        event.preventDefault();
        const body = { name: form.name, durationMonths: Number(form.durationMonths), price: Number(form.price), description: form.description, active: true, features: form.features.split("\n").map((s) => s.trim()).filter(Boolean) };
        const result = await act("/api/admin/plans", "POST", body, "Membership plan created.");
        if (result)
            setForm({ name: "", durationMonths: "1", price: "1499", description: "", features: "Full gym access\nWorkout tracker\nExercise library" });
    }
    async function toggle(plan: AnyRecord) {
        await act(`/api/admin/plans/${plan.id}`, "PUT", { name: plan.name, durationMonths: plan.durationMonths, price: plan.price, description: plan.description, active: !plan.active, features: plan.features }, `${plan.name} ${plan.active ? "deactivated" : "activated"}.`);
    }
    return <div className="admin-plans-layout"><div className="admin-plan-catalog"><div className="section-divider-title"><h3>Published tiers</h3><span>{plans.length} PLANS</span></div>{plans.map((plan, index) => <article className={`admin-plan-row ${!plan.active ? "admin-plan-row--off" : ""}`} key={plan.id}><div className="admin-plan-index">0{index + 1}</div><div className="admin-plan-main"><div className="admin-plan-heading"><h3>{plan.name}</h3><span className={`table-status ${plan.active ? "table-status--good" : "table-status--alert"}`}>{plan.active ? "Active" : "Inactive"}</span></div><p>{plan.description}</p><div className="admin-plan-features">{(plan.features || []).map((feature: string) => <span key={feature}>{feature}</span>)}</div></div><div className="admin-plan-price"><strong>{rupees(plan.price)}</strong><span>{plan.durationMonths} {plan.durationMonths === 1 ? "month" : "months"}</span><button className="button button--outline button--small" onClick={() => toggle(plan)} disabled={busy}>{plan.active ? "Deactivate" : "Activate"}</button></div></article>)}</div><form onSubmit={create} className="panel plan-create-panel"><div className="panel-heading"><div><span className="micro-label">NEW MEMBERSHIP</span><h3>Add a tier.</h3></div><Plus size={20}/></div><div className="field"><label>Plan name</label><input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required maxLength={80} placeholder="e.g. Forge Quarterly"/></div><div className="form-grid"><div className="field"><label>Duration (months)</label><input type="number" min="1" max="60" value={form.durationMonths} onChange={(e) => setForm({ ...form, durationMonths: e.target.value })} required/></div><div className="field"><label>Price (INR)</label><input type="number" min="0" step="1" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} required/></div></div><div className="field"><label>Description</label><textarea rows={3} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} maxLength={500} placeholder="What this plan is designed for"/></div><div className="field"><label>Included features <small>One line per feature</small></label><textarea rows={4} value={form.features} onChange={(e) => setForm({ ...form, features: e.target.value })} placeholder="Full gym access\nWorkout tracker"/></div><button className="button button--primary button--wide" disabled={busy}>Create membership tier <ArrowRight size={16}/></button><p className="small-print">Prices are shown in INR. Configure payment processing separately before accepting actual payments.</p></form></div>;
}
function AdminScheduleSection({ data, busy, act, closureForm, setClosureForm, updateSchedule }: {
    data: AnyRecord;
    busy: boolean;
    act: (path: string, method?: string, body?: unknown, success?: string, reload?: boolean) => Promise<any>;
    closureForm: {
        date: string;
        reason: string;
        announcement: string;
    };
    setClosureForm: (value: {
        date: string;
        reason: string;
        announcement: string;
    }) => void;
    updateSchedule: (day: string, key: string, value: string | boolean) => void;
}) {
    const schedule: AnyRecord[] = data.schedule || [];
    const closures: AnyRecord[] = data.closures || [];
    async function saveSchedule(event: FormEvent) { event.preventDefault(); await act("/api/admin/schedule", "PUT", { days: schedule }, "Weekly operating hours updated."); }
    async function addClosure(event: FormEvent) { event.preventDefault(); const result = await act("/api/admin/closures", "POST", closureForm, "Facility closure published."); if (result)
        setClosureForm({ date: "", reason: "", announcement: "" }); }
    return <div className="schedule-layout"><form onSubmit={saveSchedule} className="panel schedule-panel"><div className="panel-heading"><div><span className="micro-label">WEEKLY OPERATING HOURS</span><h3>Opening the doors.</h3><p>These hours govern check-in access.</p></div><Clock3 size={20}/></div><div className="schedule-table"><div className="schedule-table-head"><span>DAY</span><span>OPEN</span><span>OPENS</span><span>CLOSES</span></div>{schedule.map((day) => <div className="schedule-row" key={day.dayOfWeek}><strong>{day.dayOfWeek}</strong><label className="toggle-control"><input type="checkbox" checked={day.open} onChange={(e) => updateSchedule(day.dayOfWeek, "open", e.target.checked)}/><span /></label><input type="time" aria-label={`${day.dayOfWeek} opening time`} value={day.openingTime} onChange={(e) => updateSchedule(day.dayOfWeek, "openingTime", e.target.value)} disabled={!day.open} required/><input type="time" aria-label={`${day.dayOfWeek} closing time`} value={day.closingTime} onChange={(e) => updateSchedule(day.dayOfWeek, "closingTime", e.target.value)} disabled={!day.open} required/></div>)}</div><div className="field schedule-notes"><label>Facility note <small>Optional status note</small></label><textarea rows={2} value={schedule[0]?.notes || ""} onChange={(e) => schedule.forEach((day) => updateSchedule(day.dayOfWeek, "notes", e.target.value))} placeholder="Standard operating hours"/></div><button className="button button--primary" disabled={busy}>Save operating hours <Check size={16}/></button></form>
    <div className="closure-side"><form onSubmit={addClosure} className="panel closure-create"><div className="panel-heading"><div><span className="micro-label">SPECIAL DATES</span><h3>Schedule a closure.</h3></div><CalendarDays size={20}/></div><div className="field"><label>Date</label><input type="date" value={closureForm.date} onChange={(e) => setClosureForm({ ...closureForm, date: e.target.value })} min={todayInFacility()} required/></div><div className="field"><label>Reason</label><input value={closureForm.reason} onChange={(e) => setClosureForm({ ...closureForm, reason: e.target.value })} required maxLength={200} placeholder="e.g. Annual maintenance"/></div><div className="field"><label>Member announcement</label><textarea value={closureForm.announcement} onChange={(e) => setClosureForm({ ...closureForm, announcement: e.target.value })} maxLength={600} rows={3} placeholder="Optional note for members"/></div><button className="button button--primary button--wide" disabled={busy}>Publish closure <ArrowRight size={16}/></button></form><section className="panel upcoming-closures"><div className="panel-heading"><div><span className="micro-label">UPCOMING / PUBLISHED</span><h3>Dates to know.</h3></div><span className="panel-number">{closures.length}</span></div>{closures.filter((closure) => closure.date >= todayInFacility()).map((closure) => <div className="closure-item" key={closure.id}><div className="closure-date"><strong>{new Date(`${closure.date}T12:00:00`).toLocaleDateString("en-IN", { day: "2-digit" })}</strong><span>{new Date(`${closure.date}T12:00:00`).toLocaleDateString("en-IN", { month: "short" }).toUpperCase()}</span></div><div className="closure-item-copy"><strong>{closure.reason}</strong><small>{closure.announcement || "No announcement provided."}</small></div><button className="table-delete" onClick={() => act(`/api/admin/closures/${closure.id}`, "DELETE", undefined, "Closure removed.")} disabled={busy}>Remove</button></div>)}{!closures.some((closure) => closure.date >= todayInFacility()) && <div className="empty-inline">No upcoming closures are published.</div>}</section></div></div>;
}
