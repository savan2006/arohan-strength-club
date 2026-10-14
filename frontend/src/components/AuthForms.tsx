"use client";
import { FormEvent, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, ArrowRight, Eye, EyeOff, LockKeyhole, ShieldCheck } from "lucide-react";
import { apiRequest, AuthUser, saveSession } from "@/lib/api";
import { BrandMark } from "./BrandMark";
type AuthResponse = {
    token: string;
    user: AuthUser;
};
type AuthMode = "login" | "register";
export function AuthForms({ mode }: {
    mode: AuthMode;
}) {
    const router = useRouter();
    const isRegister = mode === "register";
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [form, setForm] = useState({
        fullName: "", email: "", phone: "", password: "", age: "25", heightCm: "175", weightKg: "72",
        fitnessGoal: "GENERAL_FITNESS", experienceLevel: "BEGINNER", plannedDaysPerWeek: "4", preferredSchedule: "EVENING",
    });
    const update = (key: keyof typeof form, value: string) => setForm((old) => ({ ...old, [key]: value }));
    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError("");
        setBusy(true);
        try {
            const payload = isRegister ? {
                fullName: form.fullName.trim(), email: form.email.trim(), phone: form.phone.trim(), password: form.password,
                age: Number(form.age), heightCm: Number(form.heightCm), weightKg: Number(form.weightKg),
                fitnessGoal: form.fitnessGoal, experienceLevel: form.experienceLevel,
                plannedDaysPerWeek: Number(form.plannedDaysPerWeek), preferredSchedule: form.preferredSchedule,
            } : { email: form.email.trim(), password: form.password };
            const result = await apiRequest<AuthResponse>(`/api/auth/${mode}`, { method: "POST", body: JSON.stringify(payload) });
            saveSession(result.token, result.user);
            router.push(result.user.role === "ADMIN" ? "/admin-users" : result.user.role === "TRAINER" ? "/trainer" : "/dashboard");
        }
        catch (cause) {
            setError(cause instanceof Error ? cause.message : "Unable to sign you in right now.");
        }
        finally {
            setBusy(false);
        }
    }
    return (<main className="auth-layout">
      <section className="auth-visual">
        <div className="auth-visual__top"><BrandMark /><Link href="/" className="back-link"><ArrowLeft size={16}/> Home</Link></div>
        <div className="auth-visual__copy">
          <p className="eyebrow eyebrow--light"><span className="eyebrow-dot"/> Built for the work</p>
          <h1>{isRegister ? <>Your next<br /><em>chapter</em> starts here.</> : <>Discipline<br />looks good<br /><em>on you.</em></>}</h1>
          <p>{isRegister ? "Set your baseline. Choose your goal. We'll help you build the consistency that makes the difference." : "Your training, your progress, your place to get stronger. Pick up where your last session left off."}</p>
        </div>
        <div className="auth-visual__bottom"><span>01 — SHOW UP</span><span>02 — PUT IN THE WORK</span><span>03 — RISE BEYOND LIMITS</span></div>
        <div className="auth-red-rule"/>
      </section>

      <section className="auth-form-side">
        <div className="auth-form-inner">
          <div className="auth-form-heading">
            <span className="micro-label">AROHAN / MEMBER ACCESS</span>
            <h2>{isRegister ? "Create your account" : "Welcome back"}</h2>
            <p>{isRegister ? "A few details to get your training profile ready." : "Sign in to your training space."}</p>
          </div>
          {error && <div className="alert alert--error" role="alert">{error}</div>}
          <form onSubmit={submit} className="form-stack">
            {isRegister && <div className="field"><label htmlFor="fullName">Full name</label><input id="fullName" autoComplete="name" value={form.fullName} onChange={(e) => update("fullName", e.target.value)} placeholder="Enter your name" required maxLength={120}/></div>}
            <div className="field"><label htmlFor="email">Email address</label><input id="email" type="email" autoComplete="email" value={form.email} onChange={(e) => update("email", e.target.value)} placeholder="you@example.com" required/></div>
            {isRegister && <div className="field"><label htmlFor="phone">Phone number</label><input id="phone" type="tel" autoComplete="tel" value={form.phone} onChange={(e) => update("phone", e.target.value)} placeholder="10-digit mobile number" required minLength={8} maxLength={20}/></div>}
            <div className="field"><label htmlFor="password">Password</label><div className="password-wrap"><input id="password" type={showPassword ? "text" : "password"} autoComplete={isRegister ? "new-password" : "current-password"} value={form.password} onChange={(e) => update("password", e.target.value)} placeholder="At least 8 characters" required minLength={8} maxLength={72}/><button type="button" className="icon-button password-toggle" aria-label={showPassword ? "Hide password" : "Show password"} onClick={() => setShowPassword((show) => !show)}>{showPassword ? <EyeOff size={17}/> : <Eye size={17}/>}</button></div></div>
            {isRegister && <>
              <div className="form-grid form-grid--3">
                <div className="field"><label htmlFor="age">Age</label><input id="age" type="number" min="14" max="100" value={form.age} onChange={(e) => update("age", e.target.value)} required/></div>
                <div className="field"><label htmlFor="heightCm">Height (cm)</label><input id="heightCm" type="number" min="50" max="260" value={form.heightCm} onChange={(e) => update("heightCm", e.target.value)} required/></div>
                <div className="field"><label htmlFor="weightKg">Weight (kg)</label><input id="weightKg" type="number" step="0.1" min="30" max="300" value={form.weightKg} onChange={(e) => update("weightKg", e.target.value)} required/></div>
              </div>
              <div className="field"><label htmlFor="fitnessGoal">Primary goal</label><select id="fitnessGoal" value={form.fitnessGoal} onChange={(e) => update("fitnessGoal", e.target.value)}><option value="GENERAL_FITNESS">General fitness</option><option value="MUSCLE_GAIN">Build muscle</option><option value="FAT_LOSS">Fat loss</option><option value="STRENGTH">Build strength</option><option value="ENDURANCE">Improve endurance</option></select></div>
              <div className="form-grid">
                <div className="field"><label htmlFor="experienceLevel">Experience</label><select id="experienceLevel" value={form.experienceLevel} onChange={(e) => update("experienceLevel", e.target.value)}><option value="BEGINNER">Beginner</option><option value="INTERMEDIATE">Intermediate</option><option value="ADVANCED">Advanced</option><option value="PRO">Pro</option></select></div>
                <div className="field"><label htmlFor="plannedDaysPerWeek">Training days / week</label><select id="plannedDaysPerWeek" value={form.plannedDaysPerWeek} onChange={(e) => update("plannedDaysPerWeek", e.target.value)}>{[3, 4, 5, 6].map((n) => <option key={n} value={n}>{n} days</option>)}</select></div>
              </div>
              <div className="field"><label htmlFor="preferredSchedule">Preferred training time</label><select id="preferredSchedule" value={form.preferredSchedule} onChange={(e) => update("preferredSchedule", e.target.value)}><option value="MORNING">Morning</option><option value="AFTERNOON">Afternoon</option><option value="EVENING">Evening</option><option value="FLEXIBLE">Flexible</option></select></div>
            </>}
            <button className="button button--primary button--wide auth-submit" type="submit" disabled={busy}>{busy ? "Please wait…" : isRegister ? "Build my profile" : "Sign in"}<ArrowRight size={17}/></button>
          </form>
          <div className="auth-legal"><ShieldCheck size={15}/><span>Your account is protected with encrypted passwords and secure sessions.</span></div>
          <p className="auth-switch">{isRegister ? "Already training with us?" : "New to AROHAN?"} <Link href={isRegister ? "/login" : "/register"}>{isRegister ? "Sign in" : "Create an account"}</Link></p>
          <div className="auth-lockup"><LockKeyhole size={13}/> PRIVATE MEMBER AREA <span>•</span> AROHAN STRENGTH CLUB</div>
        </div>
      </section>
    </main>);
}
