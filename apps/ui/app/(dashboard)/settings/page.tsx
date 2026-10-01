"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { Scale, Lock, RefreshCw, History } from "lucide-react"
import { SiteHeader } from "@/components/site-header"
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
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { api } from "@/lib/api-client"
import { type ThresholdRule, type AuditLog } from "@/lib/types"

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<string>("RULES")
  const [rules, setRules] = useState<ThresholdRule[]>([])
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const _userRole = useMemo(() => {
    try {
      const stored = localStorage.getItem("fincomp_user")
      if (stored) {
        const u = JSON.parse(stored) as { role?: string }
        return u.role || ""
      }
    } catch {
      // Sem permissão para ler o registo de actividade; a secção fica vazia.
    }
    return ""
  }, [])

  const loadData = useCallback(async () => {
    try {
      setIsLoading(true)
      if (activeTab === "RULES") {
        const rulesData = await api.getThresholdRules()
        setRules(rulesData)
      } else if (activeTab === "AUDIT") {
        const logsData = await api.getAuditLogs()
        setLogs(logsData)
      }
    } catch (err) {
      console.error("Erro ao carregar configurações:", err)
    } finally {
      setIsLoading(false)
    }
  }, [activeTab])

  useEffect(() => {
    queueMicrotask(() => {
      loadData()
    })
  }, [loadData])

  const formatMt = (val?: number) => {
    if (val === undefined || val === null) return "N/D"
    return new Intl.NumberFormat("pt-MZ", {
      style: "currency",
      currency: "MZN",
      maximumFractionDigits: 0,
    }).format(val)
  }

  return (
    <div>
      <SiteHeader
        title="Configurações Regulatórias & Auditoria Imutável"
        action={
          <Button
            variant="outline"
            size="sm"
            onClick={loadData}
            disabled={isLoading}
            className="h-8 gap-1.5 text-xs"
          >
            <RefreshCw className={`h-3 w-3 ${isLoading ? "animate-spin" : ""}`} />
            <span>Atualizar</span>
          </Button>
        }
      />

      <div className="space-y-6">
        {/* Navigation Tabs */}
        <div className="flex items-center justify-between">
          <Tabs
            value={activeTab}
            onValueChange={setActiveTab}
            className="w-full max-w-md"
          >
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="RULES" className="text-xs">
                Limiares de monitorização
              </TabsTrigger>
              <TabsTrigger value="AUDIT" className="text-xs">
                Registo de actividade
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </div>

        {activeTab === "RULES" ? (
          /* Rules Card */
          <div className="space-y-6">
            <Card className="border-border shadow-sm">
              <CardHeader className="pb-3">
                <CardTitle className="flex items-center gap-2 text-base font-semibold">
                  <Scale className="h-4 w-4 text-primary" />
                  Regras de triagem
                </CardTitle>
                <CardDescription className="text-xs">
                  Regras automáticas aplicadas na importação para sinalização imediata.
                </CardDescription>
              </CardHeader>
              <CardContent>
                {rules.length === 0 ? (
                  <div className="py-8 text-center text-xs text-muted-foreground">
                    A carregar regras…
                  </div>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Código</TableHead>
                        <TableHead>Regra</TableHead>
                        <TableHead>Limiar</TableHead>
                        <TableHead>Janela</TableHead>
                        <TableHead>Regime</TableHead>
                        <TableHead>Estado</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {rules.map((rule) => (
                        <TableRow key={rule.id}>
                          <TableCell className="font-mono text-xs font-semibold">
                            {rule.ruleCode}
                          </TableCell>
                          <TableCell className="text-xs">
                            <span className="font-medium text-foreground">
                              {rule.name}
                            </span>
                            <div className="text-[10px] text-muted-foreground">
                              {rule.description}
                            </div>
                          </TableCell>
                          <TableCell className="text-xs font-bold text-foreground">
                            {formatMt(rule.thresholdAmount)}
                          </TableCell>
                          <TableCell className="text-xs">
                            {rule.timeWindowDays > 0
                              ? `${rule.timeWindowDays}d`
                              : "Por transacção"}
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline" className="text-[10px]">
                              {rule.regime || "TODOS"}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Badge
                              variant={rule.isActive ? "success" : "secondary"}
                              className="text-[10px]"
                            >
                              {rule.isActive ? "ACTIVO" : "INACTIVO"}
                            </Badge>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </CardContent>
            </Card>

            {/* Regime thresholds summary */}
            <div className="grid gap-4 md:grid-cols-2">
              <Card className="border-border">
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-bold">Regime simplificado</CardTitle>
                </CardHeader>
                <CardContent className="space-y-2 text-xs">
                  <div className="flex justify-between border-b pb-1">
                    <span>Limite mensal por cliente</span>
                    <span className="font-bold">30.000 MT</span>
                  </div>
                  <div className="flex justify-between border-b pb-1">
                    <span>Limite anual</span>
                    <span className="font-bold">240.000 MT</span>
                  </div>
                  <p className="pt-1 text-[11px] text-muted-foreground">
                    Aplica-se a clientes de baixo risco com diligência simplificada.
                    Ultrapassar o limite exige transição para diligência reforçada.
                  </p>
                </CardContent>
              </Card>

              <Card className="border-destructive/40">
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-bold text-destructive">
                    Rastreio de sanções
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-2 text-xs text-muted-foreground">
                  <div className="flex justify-between border-b pb-1">
                    <span>Janela de decisão</span>
                    <span className="font-bold text-red-700">24 horas</span>
                  </div>
                  <div className="flex justify-between border-b pb-1">
                    <span>Acção necessária</span>
                    <span className="font-bold">Bloquear e escalar</span>
                  </div>
                  <p className="pt-1 text-[11px] text-muted-foreground">
                    Verificação automática contra listas oficiais e consolidadas de
                    sanções.
                  </p>
                </CardContent>
              </Card>
            </div>
          </div>
        ) : (
          /* Audit Logs Card */
          <Card className="shadow-sm">
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="flex items-center gap-2 text-base font-semibold">
                    <History className="h-4 w-4 text-primary" />
                    Registo de auditoria permanente
                  </CardTitle>
                  <CardDescription className="text-xs">
                    Histórico permanente e protegido de todas as acções executadas no
                    sistema.
                  </CardDescription>
                </div>
                <Badge
                  variant="outline"
                  className="border-emerald-300 bg-emerald-50 text-[11px] text-emerald-700"
                >
                  <Lock className="mr-1 h-3 w-3" /> Registo Permanente
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              {logs.length === 0 ? (
                <div className="py-8 text-center text-xs text-muted-foreground">
                  Nenhum evento de auditoria registado ou sem permissão de acesso
                  (exclusivo para ADMIN).
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Data / Hora</TableHead>
                      <TableHead>Utilizador</TableHead>
                      <TableHead>Perfil</TableHead>
                      <TableHead>Acção realizada</TableHead>
                      <TableHead>Recurso afectado</TableHead>
                      <TableHead>Endereço IP</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {logs.map((log) => (
                      <TableRow key={log.id}>
                        <TableCell className="whitespace-nowrap text-xs text-muted-foreground">
                          {new Date(log.createdAt).toLocaleString("pt-MZ")}
                        </TableCell>
                        <TableCell className="text-xs font-semibold">
                          {log.userEmail}
                        </TableCell>
                        <TableCell>
                          <Badge variant="secondary" className="text-[10px]">
                            {log.userRole}
                          </Badge>
                        </TableCell>
                        <TableCell className="font-mono text-xs font-medium text-foreground">
                          {log.action}
                        </TableCell>
                        <TableCell className="text-xs text-muted-foreground">
                          {log.resourceType}{" "}
                          {log.resourceId ? `(${log.resourceId.substring(0, 8)}...)` : ""}
                        </TableCell>
                        <TableCell className="font-mono text-xs text-muted-foreground">
                          {log.ipAddress || "127.0.0.1"}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  )
}
