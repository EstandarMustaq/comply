import type * as React from "react"
import { cn } from "@/lib/utils"

export function Kbd({ className, children, ...props }: React.ComponentProps<"kbd">) {
  return (
    <kbd
      className={cn(
        "border-border bg-muted text-muted-foreground pointer-events-none inline-flex h-5 select-none items-center gap-0.5 rounded border px-1.5 font-mono text-[10px] font-medium",
        className
      )}
      {...props}
    >
      {children}
    </kbd>
  )
}
