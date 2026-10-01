"use client"

import type React from "react"
import { Suspense, useCallback, useEffect, useState } from "react"
import { useSearchParams } from "next/navigation"
import {
  FileText,
  Send,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Building2,
  FileCheck,
  RefreshCw,
  Loader2,
} from "lucide-react"
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
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { api } from "@/lib/api-client"
import {
  type SuspiciousActivityReport,
  type CurrencyTransactionReport,
} from "@/lib/types"

function ReportsContent() {
  const searchParams = useSearchParams()
  const highlightStrId = searchParams.get("strId")

  const [activeTab, setActiveTab] = useState<string>("STR")
  const [strs, setStrs] = useState<SuspiciousActivityReport[]>([])
  const [ctrs, setCtrs] = useState<CurrencyTransactionReport[]>([])
  const [isLoading, setIsLoading] = useState(false)

  // Submit Modal States
  const [selectedStr, setSelectedStr] = useState<SuspiciousActivityReport | null>(null)
  const [isSubmitModalOpen, setIsSubmitModalOpen] = useState(false)
  const [suspicionGrounds, setSuspicionGrounds] = useState("")
  const [operationDetails, setOperationDetails] = useState("")
  const [evidenceSummary, setEvidenceSummary] = useState("")
  const [actionTaken, setActionTaken] = useState("")
  const [gifimProtocol, setGifimProtocol] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  const openSubmitModal = (report: SuspiciousActivityReport) => {
    setSelectedStr(report)
    setSuspicionGrounds(report.suspicionGrounds || "")
    setOperationDetails(report.operationDetails || "")
    setEvidenceSummary(report.evidenceSummary || "")
    setActionTaken(report.actionTaken || "Transacção retida para análise formal.")
    setGifimProtocol(
      report.gifimProtocolNumber ||
        `REF-${new Date().getFullYear()}-${Math.floor(1000 + Math.random() * 9000)}`
    )
    setIsSubmitModalOpen(true)
  }

  const loadData = useCallback(async () => {
    try {
      setIsLoading(true)
      const [strsData, ctrsData] = await Promise.all([api.getStrs(), api.getCtrs()])
      setStrs(strsData)
      setCtrs(ctrsData)

      if (highlightStrId) {
        const found = strsData.find((s) => s.id === highlightStrId)
        if (found) {
          openSubmitModal(found)
        }
      }
    } catch (err) {
      console.error("Erro ao carregar relatórios:", err)
    } finally {
      setIsLoading(false)
    }
  }, [highlightStrId])

  useEffect(() => {
    queueMicrotask(() => {
      loadData()
    })
  }, [loadData])

  const handleSubmitStr = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedStr) return

    if (!suspicionGrounds || !operationDetails || !gifimProtocol) {
      setSubmitError(
        "Fundamentos, detalhes da transacção e número de protocolo são obrigatórios."
      )
      return
    }

    try {
      setIsSubmitting(true)
      setSubmitError(null)
      await api.submitStr(selectedStr.id, {
        suspicionGrounds,
        operationDetails,
        evidenceSummary,
        actionTaken,
        gifimProtocolNumber: gifimProtocol,
      })

      setIsSubmitModalOpen(false)
      setSelectedStr(null)
      loadData()
    } catch (err: unknown) {
      setSubmitError(err instanceof Error ? err.message : "Failed to submit report.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const formatMt = (val?: number) => {
    if (val === undefined || val === null) return "0 MT"
    return new Intl.NumberFormat("pt-MZ", {
      style: "currency",
      currency: "MZN",
      maximumFractionDigits: 0,
    }).format(val)
  }

  return (
    <div>
      <SiteHeader
        title="Relatórios"
        action={
          <Button
            variant="outline"
            size="sm"
            onClick={loadData}
            disabled={isLoading}
            className="h-8 gap-1.5 text-xs"
          >
            <RefreshCw className={`h-3 w-3 ${isLoading ? "animate-spin" : ""}`} />
            <span>Actualizar</span>
          </Button>
        }
      />

      <div className="space-y-6">
        {/* Retention Notice */}
        <div className="flex items-start gap-3 rounded-lg border border-border bg-muted/40 p-3.5 text-xs text-foreground">
          <Clock className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
          <div>
            <span className="font-semibold">Conservação de documentos</span>
            <p className="mt-0.5 text-[11px] text-muted-foreground">
              Todas as comunicações submetidas, evidências e registos de diligência são
              conservados com bloqueio de eliminação por <strong>10 anos</strong>.
            </p>
          </div>
        </div>

        {/* Tab Selection */}
        <div className="flex items-center justify-between">
          <Tabs
            value={activeTab}
            onValueChange={setActiveTab}
            className="w-full max-w-md"
          >
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="STR" className="text-xs">
                Operações suspeitas
              </TabsTrigger>
              <TabsTrigger value="CTR" className="text-xs">
                Transacções em numerário
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </div>

        {activeTab === "STR" ? (
          /* STR Table Card */
          <Card className="border-border shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base font-semibold">
                <FileText className="h-4 w-4 text-primary" />
                Comunicações de operações suspeitas ({strs.length})
              </CardTitle>
              <CardDescription className="text-xs">
                Comunicações formais submetidas às autoridades sobre operações com
                indícios de suspeita.
              </CardDescription>
            </CardHeader>
            <CardContent>
              {strs.length === 0 ? (
                <div className="py-12 text-center text-xs text-muted-foreground">
                  Nenhuma operação suspeita registada.
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Referência</TableHead>
                      <TableHead>Cliente</TableHead>
                      <TableHead>N.º de protocolo</TableHead>
                      <TableHead>Conservação até</TableHead>
                      <TableHead>Estado</TableHead>
                      <TableHead className="text-right">Acção</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {strs.map((report) => {
                      const isSubmitted = report.status === "SUBMITTED"
                      return (
                        <TableRow key={report.id}>
                          <TableCell className="font-mono text-xs font-semibold">
                            {report.strReference}
                          </TableCell>
                          <TableCell className="text-xs font-medium">
                            {report.client?.fullName || "—"}
                            <div className="font-mono text-[10px] text-muted-foreground">
                              {report.client?.documentNumber}
                            </div>
                          </TableCell>
                          <TableCell className="font-mono text-xs">
                            {report.gifimProtocolNumber ? (
                              <span className="font-semibold text-emerald-700">
                                {report.gifimProtocolNumber}
                              </span>
                            ) : (
                              <span className="italic text-muted-foreground">
                                Pendente
                              </span>
                            )}
                          </TableCell>
                          <TableCell className="text-xs text-muted-foreground">
                            {report.retentionDeadline ? (
                              <span className="font-medium text-foreground">
                                {new Date(report.retentionDeadline).toLocaleDateString(
                                  "pt-MZ"
                                )}
                              </span>
                            ) : (
                              "Após submissão"
                            )}
                          </TableCell>
                          <TableCell>
                            <Badge
                              variant={isSubmitted ? "success" : "warning"}
                              className="text-[10px]"
                            >
                              {isSubmitted ? "SUBMETIDO" : "RASCUNHO"}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-right">
                            {isSubmitted ? (
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => openSubmitModal(report)}
                                className="h-7 gap-1 px-2.5 text-xs"
                              >
                                <FileCheck className="h-3 w-3 text-emerald-600" />
                                <span>Ver</span>
                              </Button>
                            ) : (
                              <Button
                                size="sm"
                                onClick={() => openSubmitModal(report)}
                                className="h-7 gap-1 px-2.5 text-xs"
                              >
                                <Send className="h-3 w-3" />
                                <span>Completar e submeter</span>
                              </Button>
                            )}
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        ) : (
          /* CTR Table Card */
          <Card className="border-border shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base font-semibold">
                <FileText className="h-4 w-4 text-primary" />
                Comunicações de transacções em numerário ({ctrs.length})
              </CardTitle>
              <CardDescription className="text-xs">
                Comunicações periódicas de transacções em numerário que atingiram o limiar
                de declaração.
              </CardDescription>
            </CardHeader>
            <CardContent>
              {ctrs.length === 0 ? (
                <div className="py-12 text-center text-xs text-muted-foreground">
                  Nenhuma transacção em numerário pendente.
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Referência</TableHead>
                      <TableHead>Cliente</TableHead>
                      <TableHead>Período</TableHead>
                      <TableHead>Montante total</TableHead>
                      <TableHead className="text-center">Transacções</TableHead>
                      <TableHead>Estado</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {ctrs.map((ctr) => (
                      <TableRow key={ctr.id}>
                        <TableCell className="font-mono text-xs font-semibold">
                          {ctr.ctrReference}
                        </TableCell>
                        <TableCell className="text-xs font-medium">
                          {ctr.client?.fullName}
                          <div className="text-[10px] text-muted-foreground">
                            {ctr.client?.documentNumber}
                          </div>
                        </TableCell>
                        <TableCell className="text-xs">
                          {ctr.reportingMonth}/{ctr.reportingYear}
                        </TableCell>
                        <TableCell className="text-xs font-bold text-foreground">
                          {formatMt(ctr.aggregateAmount)}
                        </TableCell>
                        <TableCell className="text-center text-xs">
                          {ctr.transactionCount}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-[10px]">
                            {ctr.status}
                          </Badge>
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

      {/* Submit / View STR Dialog */}
      <Dialog open={isSubmitModalOpen} onOpenChange={setIsSubmitModalOpen}>
        <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Building2 className="h-5 w-5 text-primary" />
              {selectedStr?.status === "SUBMITTED"
                ? "Registo da comunicação"
                : "Submeter comunicação de operação suspeita"}
            </DialogTitle>
            <DialogDescription className="text-xs">
              Ref: <strong>{selectedStr?.strReference}</strong> · Cliente:{" "}
              <strong>{selectedStr?.client?.fullName || "—"}</strong>
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmitStr} className="space-y-4 py-2 text-xs">
            {submitError && (
              <div className="flex items-center gap-1.5 rounded border border-red-500/30 bg-red-500/10 p-2.5 text-red-600">
                <AlertTriangle className="h-4 w-4 shrink-0" />
                <span>{submitError}</span>
              </div>
            )}

            <div className="space-y-1.5">
              <Label htmlFor="gifimProtocol" className="font-semibold">
                Número de protocolo / referência <span className="text-red-500">*</span>
              </Label>
              <Input
                id="gifimProtocol"
                value={gifimProtocol}
                onChange={(e) => setGifimProtocol(e.target.value)}
                placeholder="ex.: REF-2026-00891"
                className="font-mono text-xs"
                disabled={selectedStr?.status === "SUBMITTED"}
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="suspicionGrounds" className="font-semibold">
                Fundamentos da suspeita <span className="text-red-500">*</span>
              </Label>
              <textarea
                id="suspicionGrounds"
                rows={3}
                value={suspicionGrounds}
                onChange={(e) => setSuspicionGrounds(e.target.value)}
                placeholder="Descreva os indicadores objectivos de suspeita, incongruências com o perfil financeiro do cliente ou correspondências em listas de sanções…"
                className="w-full rounded-md border border-input bg-background p-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                disabled={selectedStr?.status === "SUBMITTED"}
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="operationDetails" className="font-semibold">
                Detalhes da transacção <span className="text-red-500">*</span>
              </Label>
              <textarea
                id="operationDetails"
                rows={3}
                value={operationDetails}
                onChange={(e) => setOperationDetails(e.target.value)}
                placeholder="Contas, montantes, datas, canais e contrapartes envolvidas…"
                className="w-full rounded-md border border-input bg-background p-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                disabled={selectedStr?.status === "SUBMITTED"}
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="evidenceSummary" className="font-semibold">
                Resumo de evidências
              </Label>
              <textarea
                id="evidenceSummary"
                rows={2}
                value={evidenceSummary}
                onChange={(e) => setEvidenceSummary(e.target.value)}
                placeholder="Extractos de conta, comprovativos de depósito, relatórios de triagem…"
                className="w-full rounded-md border border-input bg-background p-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                disabled={selectedStr?.status === "SUBMITTED"}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="actionTaken" className="font-semibold">
                Acção tomada
              </Label>
              <Input
                id="actionTaken"
                value={actionTaken}
                onChange={(e) => setActionTaken(e.target.value)}
                className="text-xs"
                disabled={selectedStr?.status === "SUBMITTED"}
              />
            </div>

            {selectedStr?.status === "SUBMITTED" && (
              <div className="space-y-1 rounded-lg border border-emerald-500/30 bg-emerald-500/10 p-3 text-emerald-900">
                <div className="flex items-center gap-1.5 font-bold">
                  <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                  Comunicação submetida — período de conservação activo
                </div>
                <p className="text-[11px]">
                  Submetido em:{" "}
                  {new Date(selectedStr.submissionDate || "").toLocaleString("pt-MZ")}
                </p>
                <p className="text-[11px]">
                  Conservação até: <strong>{selectedStr.retentionDeadline}</strong>
                </p>
              </div>
            )}

            <DialogFooter className="gap-2 pt-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setIsSubmitModalOpen(false)}
              >
                Fechar
              </Button>
              {selectedStr?.status !== "SUBMITTED" && (
                <Button
                  type="submit"
                  size="sm"
                  disabled={isSubmitting}
                  className="gap-1.5"
                >
                  {isSubmitting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                  Submeter comunicação
                </Button>
              )}
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}

export default function ReportsPage() {
  return (
    <Suspense
      fallback={
        <div className="flex flex-1 items-center justify-center p-6">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      }
    >
      <ReportsContent />
    </Suspense>
  )
}
