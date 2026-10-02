"use client"

import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import {
  ShieldAlert,
  ArrowRight,
  Clock,
  CheckCircle2,
  FileSpreadsheet,
} from "lucide-react"
import { SiteHeader } from "@/components/site-header"
import { SectionCards } from "@/components/section-cards"
import { ChartAreaInteractive } from "@/components/chart-area-interactive"
import { FlagReviewSheet } from "@/components/flag-review-sheet"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { api } from "@/lib/api-client"
import { type Flag, type InsightsData } from "@/lib/types"
import { getFlagTypeLabel } from "@/lib/labels"

export default function DashboardOverviewPage() {
  const router = useRouter()
  const [insights, setInsights] = useState<InsightsData | null>(null)
  const [pendingFlags, setPendingFlags] = useState<Flag[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [selectedFlag, setSelectedFlag] = useState<Flag | null>(null)
  const [isReviewOpen, setIsReviewOpen] = useState(false)

  const loadDashboardData = useCallback(async () => {
    try {
      setIsLoading(true)
      const [insightsRes, flagsRes] = await Promise.all([
        api.getInsights(),
        api.getFlags("PENDING"),
      ])
      setInsights(insightsRes)
      setPendingFlags(flagsRes.slice(0, 10)) // Top 10 priority
    } catch (err) {
      console.error("Erro ao carregar dashboard:", err)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    queueMicrotask(() => {
      loadDashboardData()
    })
  }, [loadDashboardData])

  const openFlagReview = (flag: Flag) => {
    setSelectedFlag(flag)
    setIsReviewOpen(true)
  }

  const getSlaBadge = (deadline?: string) => {
    if (!deadline) return null
    const diff = new Date(deadline).getTime() - new Date().getTime()
    if (diff <= 0) {
      return (
        <Badge variant="destructive" className="text-[10px]">
          SLA Expirado
        </Badge>
      )
    }
    const hours = Math.floor(diff / (1000 * 60 * 60))
    return (
      <Badge variant="urgent" className="flex items-center gap-1 text-[10px]">
        <Clock className="h-2.5 w-2.5" />
        {hours}h restantes
      </Badge>
    )
  }

  return (
    <div>
      <SiteHeader
        title="Visão geral"
        action={
          <Button
            size="sm"
            onClick={() => router.push("/import")}
            className="gap-1.5 text-xs shadow-sm"
          >
            <FileSpreadsheet className="h-3.5 w-3.5" />
            <span>Importar dados</span>
          </Button>
        }
      />

      <div className="space-y-6">
        {/* Operational summary */}
        <SectionCards data={insights} isLoading={isLoading} />

        {/* Activity trends */}
        <div className="grid gap-6">
          <ChartAreaInteractive />
        </div>

        {/* Priority alerts */}
        <Card className="border-border shadow-sm">
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="flex items-center gap-2 text-base font-semibold">
                <ShieldAlert className="h-4 w-4 text-amber-500" />
                Alertas em aberto
              </CardTitle>
              <CardDescription className="text-xs">
                Priorizados por gravidade — clique para analisar e resolver
              </CardDescription>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => router.push("/flags")}
              className="gap-1 text-xs"
            >
              <span>Ver todos</span>
              <ArrowRight className="h-3 w-3" />
            </Button>
          </CardHeader>
          <CardContent>
            {pendingFlags.length === 0 ? (
              <div className="text-muted-foreground flex flex-col items-center gap-2 py-10 text-center text-xs">
                <CheckCircle2 className="h-7 w-7 text-emerald-500" />
                <span>Nenhum alerta pendente — tudo em conformidade.</span>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[160px]">Tipo</TableHead>
                    <TableHead>Cliente</TableHead>
                    <TableHead>Regra activada</TableHead>
                    <TableHead>Prazo</TableHead>
                    <TableHead className="text-right">Acção</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {pendingFlags.map((flag) => {
                    const isSanction = flag.flagType === "SANCTIONS_MATCH"
                    return (
                      <TableRow
                        key={flag.id}
                        className="hover:bg-muted/50 cursor-pointer"
                      >
                        <TableCell>
                          <Badge
                            variant={isSanction ? "urgent" : "secondary"}
                            className="text-[11px]"
                          >
                            {getFlagTypeLabel(flag.flagType)}
                          </Badge>
                        </TableCell>
                        <TableCell className="font-medium">
                          {flag.client?.fullName || "—"}
                          <div className="text-muted-foreground text-[11px]">
                            {flag.client?.documentNumber}
                          </div>
                        </TableCell>
                        <TableCell className="max-w-xs truncate text-xs">
                          {flag.ruleTriggered}
                        </TableCell>
                        <TableCell>
                          {isSanction ? (
                            getSlaBadge(flag.slaDeadline)
                          ) : (
                            <span className="text-muted-foreground text-xs">Normal</span>
                          )}
                        </TableCell>
                        <TableCell className="text-right">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => openFlagReview(flag)}
                            className="h-7 px-2.5 text-xs"
                          >
                            Analisar
                          </Button>
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Alert review panel */}
      <FlagReviewSheet
        flag={selectedFlag}
        isOpen={isReviewOpen}
        onClose={() => {
          setIsReviewOpen(false)
          setSelectedFlag(null)
        }}
        onFlagUpdated={loadDashboardData}
      />
    </div>
  )
}
