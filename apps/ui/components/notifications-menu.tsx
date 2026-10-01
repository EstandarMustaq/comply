"use client"

import * as React from "react"
import Link from "next/link"
import { Bell } from "lucide-react"
import { cn } from "@/lib/utils"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { ScrollArea } from "@/components/ui/scroll-area"
import { api } from "@/lib/api-client"

interface NotificationEntry {
  id: string
  title: string
  description?: string
  href: string
  urgent?: boolean
}

export function NotificationsMenu() {
  const [entries, setEntries] = React.useState<NotificationEntry[]>([])
  const [loaded, setLoaded] = React.useState(false)

  React.useEffect(() => {
    let mounted = true
    api
      .getInsights()
      .then((data) => {
        if (!mounted) return
        const next: NotificationEntry[] = []

        if (data.sanctionsMatchCount) {
          next.push({
            id: "sanctions",
            title: `${data.sanctionsMatchCount} correspondências de sanções`,
            description: "Exigem decisão dentro de 24 horas",
            href: "/flags?type=sanctions",
            urgent: true,
          })
        }
        if (data.pendingFlagsCount) {
          next.push({
            id: "pending",
            title: `${data.pendingFlagsCount} alertas por rever`,
            description: "Aguardam triagem",
            href: "/flags",
          })
        }
        if (data.overdueSlaCount) {
          next.push({
            id: "sla",
            title: `${data.overdueSlaCount} casos fora de prazo`,
            description: "Decisão em atraso",
            href: "/flags",
            urgent: true,
          })
        }
        if (data.pepReviewDueCount) {
          next.push({
            id: "pep",
            title: `${data.pepReviewDueCount} revisões de cliente pendentes`,
            description: "Revisão periódica vencida",
            href: "/clients",
          })
        }

        setEntries(next)
        setLoaded(true)
      })
      .catch(() => setLoaded(true))
    return () => {
      mounted = false
    }
  }, [])

  const hasUrgent = entries.some((entry) => entry.urgent)

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          aria-label="Notificações"
          className="relative inline-flex size-8 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-background/60 hover:text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
        >
          <Bell className="size-4" />
          {entries.length > 0 && (
            <span
              className={cn(
                "absolute right-1 top-1 size-1.5 rounded-full",
                hasUrgent ? "bg-destructive" : "bg-primary"
              )}
              aria-hidden="true"
            />
          )}
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" sideOffset={8} className="w-80 p-0">
        <div className="flex items-center justify-between border-b border-border px-3 py-2.5">
          <span className="text-sm font-medium text-foreground">Notificações</span>
          {entries.length > 0 && (
            <span className="text-xs text-muted-foreground">{entries.length}</span>
          )}
        </div>

        <ScrollArea className="max-h-80">
          <div className="p-1.5">
            {!loaded && (
              <p className="px-3 py-6 text-center text-xs text-muted-foreground">
                A carregar...
              </p>
            )}

            {loaded && entries.length === 0 && (
              <p className="px-3 py-6 text-center text-xs text-muted-foreground">
                Nada por rever.
              </p>
            )}

            {entries.map((entry) => (
              <Link
                key={entry.id}
                href={entry.href}
                className="flex items-start gap-2.5 rounded-lg px-3 py-2.5 transition-colors hover:bg-muted/60"
              >
                <span
                  className={cn(
                    "mt-1.5 size-1.5 shrink-0 rounded-full",
                    entry.urgent ? "bg-destructive" : "bg-primary"
                  )}
                  aria-hidden="true"
                />
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-xs font-medium text-foreground">
                    {entry.title}
                  </span>
                  {entry.description && (
                    <span className="block truncate text-[11px] text-muted-foreground">
                      {entry.description}
                    </span>
                  )}
                </span>
              </Link>
            ))}
          </div>
        </ScrollArea>

        <div className="border-t border-border p-1.5">
          <Link
            href="/flags"
            className="flex w-full items-center justify-center rounded-lg px-3 py-2 text-xs text-muted-foreground transition-colors hover:bg-muted/60 hover:text-foreground"
          >
            Ver todos os alertas
          </Link>
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
