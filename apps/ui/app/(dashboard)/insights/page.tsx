"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { ArrowUpRight, CheckCircle2, Clock, ShieldAlert, Users } from "lucide-react"

import { SiteHeader } from "@/components/site-header"
import { ChartAreaInteractive } from "@/components/chart-area-interactive"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { api } from "@/lib/api-client"
import type { InsightsData } from "@/lib/types"
import { cn } from "@/lib/utils"

function percent(part: number, total: number) {
  if (!total) return 0
  return Math.round((part / total) * 100)
}

export default function InsightsPage() {
  const router = useRouter()
  const [data, setData] = useState<InsightsData | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    let mounted = true
    api
      .getInsights()
      .then((result) => {
        if (mounted) setData(result)
      })
      .catch(() => {})
      .finally(() => {
        if (mounted) setIsLoading(false)
      })
    return () => {
      mounted = false
    }
  }, [])

  const totalClients = data?.totalClients ?? 0
  const kycPercent =
    data?.kycCompletenessPercentage ?? percent(data?.kycCompleteCount ?? 0, totalClients)

  const coverage = [
    {
      label: "Complete identity",
      value: `${kycPercent}%`,
      detail: `${data?.kycCompleteCount ?? 0} of ${totalClients} customers`,
      percent: kycPercent,
    },
    {
      label: "Enhanced due diligence",
      value: (data?.normalRegimeCount ?? 0).toLocaleString("en-MZ"),
      detail: `${percent(data?.normalRegimeCount ?? 0, totalClients)}% of customers`,
      percent: percent(data?.normalRegimeCount ?? 0, totalClients),
    },
    {
      label: "Simplified due diligence",
      value: (data?.simplifiedRegimeCount ?? 0).toLocaleString("en-MZ"),
      detail: `${percent(data?.simplifiedRegimeCount ?? 0, totalClients)}% of customers`,
      percent: percent(data?.simplifiedRegimeCount ?? 0, totalClients),
    },
  ]

  const attention = [
    {
      label: "Sanctions matches",
      value: data?.sanctionsMatchCount ?? 0,
      description: "Decision due within 24 hours",
      icon: ShieldAlert,
      urgent: true,
      href: "/flags?type=sanctions",
    },
    {
      label: "Overdue cases",
      value: data?.overdueSlaCount ?? 0,
      description: "Decision is overdue",
      icon: Clock,
      urgent: true,
      href: "/flags?sla=overdue",
    },
    {
      label: "Customer reviews due",
      value: data?.pepReviewDueCount ?? 0,
      description: "Customers to reassess",
      icon: Users,
      urgent: false,
      href: "/clients",
    },
    {
      label: "Records expiring",
      value: data?.retentionExpiringCount ?? 0,
      description: "Retention period ending",
      icon: Clock,
      urgent: false,
      href: "/reports",
    },
  ]

  const nothingPending = attention.every((item) => item.value === 0)

  return (
    <div>
      <SiteHeader
        title="Insights"
        description="See your workload and what needs a decision now."
        action={
          <Button
            size="sm"
            variant="outline"
            className="gap-1.5 text-xs"
            onClick={() => router.push("/reports")}
          >
            View reports
            <ArrowUpRight className="size-3.5" />
          </Button>
        }
      />

      <div className="space-y-6">
        {/* Items requiring a decision */}
        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {attention.map((item) => {
            const isActive = item.value > 0
            return (
              <Card
                key={item.label}
                role="button"
                tabIndex={0}
                onClick={() => router.push(item.href)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") router.push(item.href)
                }}
                className={cn(
                  "hover:border-primary/50 cursor-pointer shadow-sm transition-colors",
                  isActive && item.urgent && "border-destructive/40"
                )}
              >
                <CardHeader className="flex flex-row items-start justify-between gap-2 pb-2">
                  <CardTitle className="text-muted-foreground text-xs font-medium">
                    {item.label}
                  </CardTitle>
                  <item.icon
                    className={cn(
                      "size-4 shrink-0",
                      isActive && item.urgent
                        ? "text-destructive"
                        : "text-muted-foreground"
                    )}
                  />
                </CardHeader>
                <CardContent className="space-y-1">
                  {isLoading ? (
                    <Skeleton className="h-7 w-12" />
                  ) : (
                    <p
                      className={cn(
                        "text-2xl font-semibold tabular-nums",
                        isActive && item.urgent ? "text-destructive" : "text-foreground"
                      )}
                    >
                      {item.value}
                    </p>
                  )}
                  <p className="text-muted-foreground text-[11px]">{item.description}</p>
                </CardContent>
              </Card>
            )
          })}
        </section>

        {nothingPending && !isLoading && (
          <div className="border-border bg-muted/40 text-muted-foreground flex items-center gap-2 rounded-lg border px-4 py-3 text-sm">
            <CheckCircle2 className="text-primary size-4" />
            Nothing needs a decision right now.
          </div>
        )}

        {/* Portfolio coverage */}
        <Card className="shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-semibold">Customer coverage</CardTitle>
            <CardDescription className="text-xs">
              Breakdown of {totalClients.toLocaleString("en-MZ")} customers
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {coverage.map((row) => (
              <div key={row.label} className="space-y-1.5">
                <div className="flex items-baseline justify-between gap-3">
                  <span className="text-muted-foreground text-xs">{row.label}</span>
                  <span className="text-foreground text-sm font-semibold tabular-nums">
                    {isLoading ? "—" : row.value}
                  </span>
                </div>
                <div className="bg-muted h-1.5 w-full overflow-hidden rounded-full">
                  <div
                    className="bg-primary h-full rounded-full transition-all duration-500"
                    style={{ width: `${isLoading ? 0 : row.percent}%` }}
                  />
                </div>
                <p className="text-muted-foreground text-[11px]">{row.detail}</p>
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Volume over time */}
        <ChartAreaInteractive />

        {/* Cumulative activity */}
        <Card className="shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-semibold">All-time activity</CardTitle>
            <CardDescription className="text-xs">Since monitoring began</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-3">
            {[
              {
                label: "Transactions reviewed",
                value: (data?.totalTransactionsCount ?? 0).toLocaleString("en-MZ"),
              },
              {
                label: "Alerts to review",
                value: (data?.pendingFlagsCount ?? 0).toLocaleString("en-MZ"),
              },
              {
                label: "Reports submitted",
                value: (data?.reportedStrsCount ?? 0).toLocaleString("en-MZ"),
              },
            ].map((stat) => (
              <div key={stat.label} className="space-y-0.5">
                <p className="text-muted-foreground text-xs">{stat.label}</p>
                {isLoading ? (
                  <Skeleton className="h-7 w-20" />
                ) : (
                  <p className="text-foreground text-2xl font-semibold tabular-nums">
                    {stat.value}
                  </p>
                )}
              </div>
            ))}
          </CardContent>
        </Card>

        {data?.complianceScorePercent !== undefined && (
          <div className="border-border flex items-center justify-between gap-3 rounded-lg border px-4 py-3">
            <div>
              <p className="text-muted-foreground text-xs">Overall status</p>
              <p className="text-foreground text-sm font-medium">
                {data.complianceScorePercent}% of controls are current
              </p>
            </div>
            <Badge variant={data.complianceScorePercent >= 80 ? "default" : "urgent"}>
              {data.complianceScorePercent >= 80 ? "On track" : "Needs attention"}
            </Badge>
          </div>
        )}
      </div>
    </div>
  )
}
