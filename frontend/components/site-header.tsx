import React from "react"
import Link from "next/link"
import { ChevronRight } from "lucide-react"
import { Separator } from "@/components/ui/separator"
import { SidebarTrigger } from "@/components/ui/sidebar"

interface SiteHeaderProps {
  title?: string
  subtitle?: string
  action?: React.ReactNode
}

export function SiteHeader({ title, subtitle, action }: SiteHeaderProps = {}) {
  return (
    <header className="sticky top-0 z-30 flex h-14 shrink-0 items-center justify-between border-b border-border/70 bg-background/95 px-4 backdrop-blur supports-[backdrop-filter]:bg-background/80 lg:px-6">
      {/* Left: Sidebar trigger + breadcrumb */}
      <div className="flex items-center gap-2 min-w-0">
        <SidebarTrigger className="-ml-1 h-8 w-8 shrink-0 text-muted-foreground hover:text-foreground" />
        <Separator orientation="vertical" className="h-4 bg-border/80 shrink-0" />

        <nav aria-label="Breadcrumb" className="flex items-center gap-1.5 text-xs min-w-0">
          <Link
            href="/"
            className="shrink-0 font-medium text-muted-foreground/70 hover:text-foreground transition-colors"
          >
            FinComp
          </Link>
          <ChevronRight className="h-3.5 w-3.5 shrink-0 text-muted-foreground/40" />
          <span className="truncate font-semibold text-foreground">
            {title || "Overview"}
          </span>
        </nav>
      </div>

      {/* Right: page-level actions */}
      <div className="flex items-center gap-3">
        {/* System live indicator — product status, no regulatory text */}
        <div className="hidden xl:flex items-center gap-2 rounded-full border border-border/70 bg-muted/40 px-2.5 py-1 text-[11px] font-medium text-muted-foreground">
          <span className="relative flex h-1.5 w-1.5">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-500 opacity-70" />
            <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-emerald-500" />
          </span>
          <span>Live</span>
        </div>

        {action && <div className="flex items-center gap-2">{action}</div>}
      </div>
    </header>
  )
}
