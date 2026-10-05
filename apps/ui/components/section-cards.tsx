"use client"

import { Users, AlertTriangle, TrendingUp, ShieldCheck } from "lucide-react"
import { Card } from "@/components/ui/card"
import { type InsightsData } from "@/lib/types"
import { cn } from "@/lib/utils"

interface SectionCardsProps {
  data?: InsightsData | null
  isLoading?: boolean
}

export function SectionCards({ data, isLoading }: SectionCardsProps = {}) {
  const formatMt = (val?: number) =>
    (val || 0).toLocaleString("en-MZ", {
      style: "currency",
      currency: "MZN",
      maximumFractionDigits: 0,
    })

  const cards = [
    {
      title: "Customers",
      subtitle: `${data?.simplifiedRegimeClients ?? 0} simplified · ${data?.normalRegimeClients ?? 0} enhanced`,
      value: (data?.totalClients ?? 0).toLocaleString("en-MZ"),
      icon: Users,
      badge: "Active",
      badgeVariant: "default",
      accentColor: "text-blue-500",
    },
    {
      title: "Alertas em aberto",
      subtitle: data?.urgentSanctionsFlagsCount
        ? `${data.urgentSanctionsFlagsCount} need a decision now`
        : "All clear",
      value: (data?.pendingFlagsCount ?? 0).toString(),
      icon: AlertTriangle,
      badge: data?.urgentSanctionsFlagsCount
        ? `${data.urgentSanctionsFlagsCount} urgent`
        : "Normal",
      badgeVariant: data?.urgentSanctionsFlagsCount ? "destructive" : "secondary",
      accentColor: data?.urgentSanctionsFlagsCount
        ? "text-destructive"
        : "text-amber-500",
    },
    {
      title: "Volume monitorizado",
      subtitle: `${data?.totalTransactions ?? 0} transactions reviewed`,
      value: formatMt(data?.totalVolumeMt),
      icon: TrendingUp,
      badge: "All time",
      badgeVariant: "secondary",
      accentColor: "text-emerald-500",
    },
    {
      title: "Compliance score",
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
      {cards.map((card) => {
        const Icon = card.icon
        return (
          <Card
            key={card.title}
            className="backdrop-blur-xs shadow-xs border-border bg-card/50 hover:border-border group relative overflow-hidden rounded-lg border p-5 transition-all duration-200 hover:shadow-sm"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="border-border bg-muted/50 text-foreground group-hover:bg-muted flex h-8 w-8 items-center justify-center rounded-lg border transition-colors">
                  <Icon className={cn("h-4 w-4", card.accentColor)} />
                </div>
                <span className="text-muted-foreground text-xs font-medium">
                  {card.title}
                </span>
              </div>
              <span
                className={cn(
                  "rounded-full border px-2 py-0.5 text-[10px] font-medium",
                  card.badgeVariant === "destructive"
                    ? "border-destructive/40 text-destructive bg-transparent font-semibold"
                    : "border-border bg-muted/70 text-muted-foreground"
                )}
              >
                {card.badge}
              </span>
            </div>

            <div className="mt-4">
              <div className="text-foreground text-2xl font-bold tabular-nums tracking-tight">
                {isLoading ? (
                  <span className="bg-muted inline-block h-8 w-24 animate-pulse rounded" />
                ) : (
                  card.value
                )}
              </div>
              <p className="text-muted-foreground/80 mt-1.5 truncate text-xs">
                {isLoading ? (
                  <span className="bg-muted inline-block h-3.5 w-32 animate-pulse rounded" />
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
