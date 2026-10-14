import Image from "next/image";
export function BrandMark({ compact = false, priority = false }: {
    compact?: boolean;
    priority?: boolean;
}) {
    return (<div className={`brand-mark ${compact ? "brand-mark--compact" : ""}`}>
      <Image src="/arohan-logo.png" alt="AROHAN Strength Club logo" width={220} height={165} priority={priority}/>
    </div>);
}
