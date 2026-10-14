"use client";
import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { ArrowDownRight, ArrowRight, ArrowUpRight, Clock3, Dumbbell, ShieldCheck, Sparkles } from "lucide-react";
import { apiRequest, rupees } from "@/lib/api";
import { BrandMark } from "./BrandMark";
type GymStatus = {
    open: boolean;
    message: string;
    openingTime?: string;
    closingTime?: string;
    date?: string;
};
type MembershipPlan = {
    id: string;
    name: string;
    description: string;
    durationMonths: number;
    price: number;
    active: boolean;
    features: string[];
};
export function LandingPage() {
    const [gym, setGym] = useState<GymStatus | null>(null);
    const [plans, setPlans] = useState<MembershipPlan[]>([]);
    useEffect(() => {
        apiRequest<GymStatus>("/api/gym/status").then(setGym).catch(() => setGym(null));
        apiRequest<MembershipPlan[]>("/api/membership-plans").then(setPlans).catch(() => setPlans([]));
    }, []);
    return (<main className="site-landing">
      <header className="site-header">
        <Link href="/" className="header-brand"><BrandMark compact priority/></Link>
        <nav className="site-nav" aria-label="Main navigation"><a href="#approach">Our approach</a><a href="#membership">Membership</a><a href="#club">The club</a></nav>
        <div className="header-actions"><Link href="/login" className="text-link">Member sign in</Link><Link href="/register" className="button button--small button--dark">Start your climb <ArrowUpRight size={15}/></Link></div>
      </header>

      <section className="hero-section">
        <div className="hero-copy">
          <div className="eyebrow"><span className="eyebrow-dot"/> Strength is built, not found</div>
          <h1>RISE<br /><span>BEYOND</span><br /><em>LIMITS.</em></h1>
          <p className="hero-lede">A training space for people who take their progress seriously. Built around better movement, steady habits and strength that carries beyond the gym.</p>
          <div className="hero-actions"><Link href="/register" className="button button--primary">Begin your climb <ArrowRight size={17}/></Link><a href="#approach" className="hero-scroll">Explore the method <ArrowDownRight size={17}/></a></div>
          <div className="hero-proof"><div><strong>01</strong><span>Train with intent</span></div><div><strong>02</strong><span>Track real progress</span></div><div><strong>03</strong><span>Own the consistency</span></div></div>
        </div>
        <div className="hero-art" aria-label="AROHAN training floor">
          <Image src="/gym-hero.jpg" alt="Strength training floor with gym equipment" fill priority sizes="(max-width: 900px) 100vw, 54vw" className="hero-photo"/>
          <div className="hero-art-shade"/>
          <div className="hero-art-stamp"><span>ASC</span><small>EST. FOR THE WORK</small></div>
          <div className="hero-art-caption"><div><span className="micro-label micro-label--light">AROHAN STRENGTH CLUB</span><h2>WORK<br />WITH PURPOSE.</h2></div><span className="caption-mark">↗</span></div>
          <div className="hero-red-cut"/>
          <div className="hero-status"><span className={`status-pip ${gym?.open ? "status-pip--open" : ""}`}/><div><small>CLUB STATUS</small><strong>{gym ? gym.open ? "OPEN NOW" : "CURRENTLY CLOSED" : "STATUS UPDATING"}</strong><span>{gym?.message || "Check in for today's facility hours"}</span></div><Clock3 size={19}/></div>
        </div>
        <div className="hero-index">ASC / 001 <span>AHMEDABAD · INDIA</span></div>
      </section>

      <section id="approach" className="approach-section section-pad">
        <div className="section-side-label"><span>01 / THE METHOD</span><span>BUILT FOR THE LONG GAME</span></div>
        <div className="approach-grid">
          <div><p className="eyebrow"><span className="eyebrow-dot"/> A system, not a shortcut</p><h2>Progress is a<br /><em>practice.</em></h2></div>
          <div className="approach-intro"><p>Good training needs more than motivation. AROHAN brings your routines, sessions, membership and progress into one considered place—so the next right step is always clear.</p><Link href="/register" className="underlined-link">Find your starting point <ArrowUpRight size={16}/></Link></div>
        </div>
        <div className="feature-grid">
          <article className="feature-cell"><span className="feature-number">01</span><div className="feature-icon"><Dumbbell size={22}/></div><h3>Training with structure</h3><p>Recommended splits, trainer-assigned programmes and custom routines that keep every session pointed somewhere.</p><span className="feature-bottom">THE WORKOUT LOGIC <ArrowUpRight size={15}/></span></article>
          <article className="feature-cell"><span className="feature-number">02</span><div className="feature-icon"><Clock3 size={22}/></div><h3>Every session counts</h3><p>Check in, track time and mark exercise progress. Your history builds a more useful picture than a vague memory.</p><span className="feature-bottom">THE SESSION RECORD <ArrowUpRight size={15}/></span></article>
          <article className="feature-cell"><span className="feature-number">03</span><div className="feature-icon"><ShieldCheck size={22}/></div><h3>Accountability, made clear</h3><p>Membership validity, weekly consistency and progress views bring the small commitments into focus.</p><span className="feature-bottom">THE LONG GAME <ArrowUpRight size={15}/></span></article>
        </div>
      </section>

      <section id="club" className="club-section">
        <div className="club-image"><Image src="/gym-hero.jpg" alt="Weights and strength equipment at the club" fill sizes="(max-width: 900px) 100vw, 50vw"/><div className="club-image-tag">STRENGTH / MOVEMENT / DISCIPLINE</div></div>
        <div className="club-copy"><p className="eyebrow eyebrow--light"><span className="eyebrow-dot"/> The AROHAN standard</p><h2>No noise.<br /><em>Just progress.</em></h2><p>Keep the distractions out of your training. Follow a plan, work inside your own rhythm and show up with a clearer sense of what you&apos;re building.</p><div className="club-callout"><Sparkles size={18}/><span>Every strong habit starts with one honest session.</span></div><Link href="/register" className="button button--primary">Make your first move <ArrowRight size={17}/></Link></div>
      </section>

      <section id="membership" className="membership-section section-pad">
        <div className="section-side-label"><span>02 / MEMBERSHIP</span><span>MAKE ROOM FOR PROGRESS</span></div>
        <div className="membership-heading"><div><p className="eyebrow"><span className="eyebrow-dot"/> Choose your commitment</p><h2>Invest in the<br /><em>work.</em></h2></div><p>Start where you are. Build the routine. The details can change—your commitment is what makes the plan work.</p></div>
        <div className="public-plans">
          {(plans.length ? plans : [
            { id: "starter", name: "Iron Start", description: "A focused first step into consistent training.", durationMonths: 1, price: 1499, active: true, features: ["Full gym access", "Workout tracker", "Exercise library"] },
            { id: "quarter", name: "Forge Quarterly", description: "Build momentum with a 90-day training block.", durationMonths: 3, price: 3999, active: true, features: ["Full gym access", "Workout plans", "Monthly progress review"] },
            { id: "annual", name: "Summit Annual", description: "A full year built around strength and consistency.", durationMonths: 12, price: 11999, active: true, features: ["Full gym access", "Workout plans", "Quarterly progress review"] },
        ]).slice(0, 3).map((plan, index) => <article className={`public-plan ${index === 1 ? "public-plan--featured" : ""}`} key={plan.id}><div className="plan-topline"><span>0{index + 1} / {plan.durationMonths} {plan.durationMonths === 1 ? "MONTH" : "MONTHS"}</span>{index === 1 && <span className="plan-recommended">MOST CHOSEN</span>}</div><h3>{plan.name}</h3><p>{plan.description}</p><div className="plan-price">{rupees(plan.price)}<small> / {plan.durationMonths} {plan.durationMonths === 1 ? "month" : "months"}</small></div><ul>{plan.features?.slice(0, 4).map((feature) => <li key={feature}><span>+</span>{feature}</li>)}</ul><Link href="/register" className={index === 1 ? "button button--primary button--wide" : "button button--outline button--wide"}>Choose this plan <ArrowRight size={16}/></Link></article>)}
        </div>
        <p className="membership-note">Membership checkouts inside the app are currently simulated. Add a verified payment gateway before accepting live payments.</p>
      </section>

      <footer className="site-footer"><BrandMark compact/><div className="footer-phrase">RISE BEYOND LIMITS <span>—</span> EVERY REP HAS A REASON.</div><div className="footer-links"><Link href="/login">Member sign in</Link><Link href="/register">Join the club</Link></div><span className="footer-copyright">© {new Date().getFullYear()} AROHAN STRENGTH CLUB</span></footer>
    </main>);
}
