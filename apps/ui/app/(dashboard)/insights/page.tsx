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
      label: "Identificação completa",
      value: `${kycPercent}%`,
      detail: `${data?.kycCompleteCount ?? 0} de ${totalClients} clientes`,
      percent: kycPercent,
    },
    {
      label: "Regime reforçado",
      value: (data?.normalRegimeCount ?? 0).toLocaleString("pt-MZ"),
      detail: `${percent(data?.normalRegimeCount ?? 0, totalClients)}% da carteira`,
      percent: percent(data?.normalRegimeCount ?? 0, totalClients),
    },
    {
      label: "Regime simplificado",
      value: (data?.simplifiedRegimeCount ?? 0).toLocaleString("pt-MZ"),
      detail: `${percent(data?.simplifiedRegimeCount ?? 0, totalClients)}% da carteira`,
      percent: percent(data?.simplifiedRegimeCount ?? 0, totalClients),
    },
  ]

  const attention = [
    {
      label: "Correspondências de sanções",
      value: data?.sanctionsMatchCount ?? 0,
      description: "Decisão dentro de 24 horas",
      icon: ShieldAlert,
      urgent: true,
      href: "/flags?type=sanctions",
    },
    {
      label: "Casos fora de prazo",
      value: data?.overdueSlaCount ?? 0,
      description: "Decisão em atraso",
      icon: Clock,
      urgent: true,
      href: "/flags?sla=overdue",
    },
    {
      label: "Revisões periódicas vencidas",
      value: data?.pepReviewDueCount ?? 0,
      description: "Clientes por reavaliar",
      icon: Users,
      urgent: false,
      href: "/clients",
    },
    {
      label: "Registos a expirar",
      value: data?.retentionExpiringCount ?? 0,
      description: "Prazo de conservação a terminar",
      icon: Clock,
      urgent: false,
      href: "/reports",
    },
  ]

  const nothingPending = attention.every((item) => item.value === 0)

  return (
    <div>
      <SiteHeader
        title="Indicadores"
        description="Onde está a carteira e o que precisa de decisão agora"
        action={
          <Button
            size="sm"
            variant="outline"
            className="gap-1.5 text-xs"
            onClick={() => router.push("/reports")}
          >
            Ver relatórios
            <ArrowUpRight className="size-3.5" />
          </Button>
        }
      />

      <div className="space-y-6">
        {/* Precisa de decisão */}
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
                  "cursor-pointer shadow-sm transition-colors hover:border-primary/50",
                  isActive && item.urgent && "border-destructive/40"
                )}
              >
                <CardHeader className="flex flex-row items-start justify-between gap-2 pb-2">
                  <CardTitle className="text-xs font-medium text-muted-foreground">
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
                  <p className="text-[11px] text-muted-foreground">{item.description}</p>
                </CardContent>
              </Card>
            )
          })}
        </section>

        {nothingPending && !isLoading && (
          <div className="flex items-center gap-2 rounded-lg border border-border bg-muted/40 px-4 py-3 text-sm text-muted-foreground">
            <CheckCircle2 className="size-4 text-primary" />
            Nada por decidir neste momento.
          </div>
        )}

        {/* Cobertura da carteira */}
        <Card className="shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-semibold">Cobertura da carteira</CardTitle>
            <CardDescription className="text-xs">
              Como estão distribuídos os {totalClients.toLocaleString("pt-MZ")} clientes
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {coverage.map((row) => (
              <div key={row.label} className="space-y-1.5">
                <div className="flex items-baseline justify-between gap-3">
                  <span className="text-xs text-muted-foreground">{row.label}</span>
                  <span className="text-sm font-semibold tabular-nums text-foreground">
                    {isLoading ? "—" : row.value}
                  </span>
                </div>
                <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
                  <div
                    className="h-full rounded-full bg-primary transition-all duration-500"
                    style={{ width: `${isLoading ? 0 : row.percent}%` }}
                  />
                </div>
                <p className="text-[11px] text-muted-foreground">{row.detail}</p>
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Volume ao longo do tempo */}
        <ChartAreaInteractive />

        {/* Actividade acumulada */}
        <Card className="shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-semibold">Acumulado</CardTitle>
            <CardDescription className="text-xs">
              Desde o início do acompanhamento
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-3">
            {[
              {
                label: "Transacções analisadas",
                value: (data?.totalTransactionsCount ?? 0).toLocaleString("pt-MZ"),
              },
              {
                label: "Alertas por rever",
                value: (data?.pendingFlagsCount ?? 0).toLocaleString("pt-MZ"),
              },
              {
                label: "Comunicações submetidas",
                value: (data?.reportedStrsCount ?? 0).toLocaleString("pt-MZ"),
              },
            ].map((stat) => (
              <div key={stat.label} className="space-y-0.5">
                <p className="text-xs text-muted-foreground">{stat.label}</p>
                {isLoading ? (
                  <Skeleton className="h-7 w-20" />
                ) : (
                  <p className="text-2xl font-semibold tabular-nums text-foreground">
                    {stat.value}
                  </p>
                )}
              </div>
            ))}
          </CardContent>
        </Card>

        {data?.complianceScorePercent !== undefined && (
          <div className="flex items-center justify-between gap-3 rounded-lg border border-border px-4 py-3">
            <div>
              <p className="text-xs text-muted-foreground">Estado geral</p>
              <p className="text-sm font-medium text-foreground">
                {data.complianceScorePercent}% dos controlos em dia
              </p>
            </div>
            <Badge variant={data.complianceScorePercent >= 80 ? "default" : "urgent"}>
              {data.complianceScorePercent >= 80 ? "Estável" : "Requer atenção"}
            </Badge>
          </div>
        )}
      </div>
    </div>
  )
}
