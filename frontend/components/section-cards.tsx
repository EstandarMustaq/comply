"use client"

import React from "react"
import { Users, AlertTriangle, TrendingUp, ShieldCheck, CheckCircle2 } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { InsightsData } from "@/lib/types"
import { cn } from "@/lib/utils"

interface SectionCardsProps {
  data?: InsightsData | null
  isLoading?: boolean
}

export function SectionCards({ data, isLoading }: SectionCardsProps = {}) {
  const formatMt = (val?: number) =>
    (val || 0).toLocaleString("pt-MZ", { style: "currency", currency: "MZN", maximumFractionDigits: 0 })

  const cards = [
    {
      title: "Clients",
      subtitle: `${data?.simplifiedRegimeClients ?? 0} standard · ${data?.normalRegimeClients ?? 0} enhanced`,
      value: (data?.totalClients ?? 0).toLocaleString(),
      icon: Users,
      badge: "Active",
      badgeVariant: "default",
      accentColor: "text-blue-500",
    },
    {
      title: "Open Alerts",
      subtitle: data?.urgentSanctionsFlagsCount
        ? `${data.urgentSanctionsFlagsCount} require immediate review`
        : "All clear",
      value: (data?.pendingFlagsCount ?? 0).toString(),
      icon: AlertTriangle,
      badge: data?.urgentSanctionsFlagsCount ? `${data.urgentSanctionsFlagsCount} urgent` : "Clear",
      badgeVariant: data?.urgentSanctionsFlagsCount ? "destructive" : "secondary",
      accentColor: data?.urgentSanctionsFlagsCount ? "text-destructive" : "text-amber-500",
    },
    {
      title: "Monitored Volume",
      subtitle: `${data?.totalTransactions ?? 0} transactions screened`,
      value: formatMt(data?.totalVolumeMt),
      icon: TrendingUp,
      badge: "Cumulative",
      badgeVariant: "secondary",
      accentColor: "text-emerald-500",
    },
    {
      title: "Risk Score",
      subtitle: `${data?.reportedStrsCount ?? 0} reports submitted`,
      value: `${data?.complianceScorePercent ?? 100}%`,
      icon: ShieldCheck,
      badge: "Overall",
      badgeVariant: "default",
      accentColor: "text-teal-500",
    },
  ]

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {cards.map((card, idx) => {
        const Icon = card.icon
        return (
          <Card
            key={card.title}
            className="group relative overflow-hidden rounded-xl border border-border/70 bg-card/50 backdrop-blur-xs p-5 shadow-xs transition-all duration-200 hover:border-border hover:shadow-sm"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-border/60 bg-muted/50 text-foreground transition-colors group-hover:bg-muted">
                  <Icon className={cn("h-4 w-4", card.accentColor)} />
                </div>
                <span className="text-xs font-medium text-muted-foreground">
                  {card.title}
                </span>
              </div>
              <span
                className={cn(
                  "rounded-full px-2 py-0.5 text-[10px] font-medium border",
                  card.badgeVariant === "destructive"
                    ? "bg-destructive/10 text-destructive border-destructive/20 font-semibold"
                    : "bg-muted/70 text-muted-foreground border-border/60"
                )}
              >
                {card.badge}
              </span>
            </div>

            <div className="mt-4">
              <div className="text-2xl font-bold tracking-tight text-foreground tabular-nums">
                {isLoading ? (
                  <span className="inline-block h-8 w-24 animate-pulse rounded bg-muted" />
                ) : (
                  card.value
                )}
              </div>
              <p className="mt-1.5 text-xs text-muted-foreground/80 truncate">
                {isLoading ? (
                  <span className="inline-block h-3.5 w-32 animate-pulse rounded bg-muted" />
                ) : (
                  card.subtitle
                )}
              </p>
            </div>
          </Card>
        )
      })}
    </div>
  )
}
