"use client"

import { useState } from "react"
import {
  Clock,
  User,
  CreditCard,
  FileSpreadsheet,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  FileWarning,
  Loader2,
} from "lucide-react"
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "./ui/sheet"
import { Badge } from "./ui/badge"
import { Button } from "./ui/button"
import { Label } from "./ui/label"
import { type Flag } from "@/lib/types"
import { api } from "@/lib/api-client"
import { useRouter } from "next/navigation"

interface FlagReviewSheetProps {
  flag: Flag | null
  isOpen: boolean
  onClose: () => void
  onFlagUpdated: () => void
}

export function FlagReviewSheet({
  flag,
  isOpen,
  onClose,
  onFlagUpdated,
}: FlagReviewSheetProps) {
  const router = useRouter()
  const [notes, setNotes] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  if (!flag) return null

  const isSanctions = flag.flagType === "SANCTIONS_MATCH"

  const calculateSlaRemaining = (deadline?: string) => {
    if (!deadline) return null
    const diff = new Date(deadline).getTime() - new Date().getTime()
    if (diff <= 0) return "SLA Expirado (Atraso crítico!)"
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    return `${hours}h ${minutes}m restantes`
  }

  const slaText = calculateSlaRemaining(flag.slaDeadline)

  const handleReviewAction = async (action: "DISMISS" | "RESOLVE") => {
    if (!notes.trim()) {
      setActionError("Review notes are required before archiving or resolving an alert.")
      return
    }
    try {
      setIsSubmitting(true)
      setActionError(null)
      await api.reviewFlag(flag.id, action, notes)
      setNotes("")
      onFlagUpdated()
      onClose()
    } catch (err: unknown) {
      setActionError(err instanceof Error ? err.message : "Failed to update alert.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleEscalateStr = async () => {
    try {
      setIsSubmitting(true)
      setActionError(null)
      const str = await api.escalateFlagToStr(
        flag.id,
        notes || "Escalated for formal reporting."
      )
      setNotes("")
      onFlagUpdated()
      onClose()
      router.push(`/reports?strId=${str.id}`)
    } catch (err: unknown) {
      setActionError(err instanceof Error ? err.message : "Failed to escalate alert.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const formatMt = (val?: number) => {
    if (val === undefined || val === null) return "N/D"
    return new Intl.NumberFormat("pt-MZ", {
      style: "currency",
      currency: "MZN",
    }).format(val)
  }

  return (
    <Sheet open={isOpen} onOpenChange={onClose}>
      <SheetContent className="max-h-screen overflow-y-auto sm:max-w-xl">
        <SheetHeader className="border-b pb-4">
          <div className="flex items-center justify-between gap-2">
            <Badge
              variant={isSanctions ? "urgent" : "secondary"}
              className="px-2.5 py-0.5 text-xs uppercase tracking-wider"
            >
              {flag.flagType.replace(/_/g, " ")}
            </Badge>
            <Badge variant="outline" className="text-[11px]">
              ID: {flag.id.substring(0, 8)}...
            </Badge>
          </div>
          <SheetTitle className="mt-2 text-base font-bold text-foreground">
            Análise do alerta
          </SheetTitle>
          <SheetDescription className="text-xs">
            Regra activada: <strong>{flag.ruleTriggered}</strong>
          </SheetDescription>
        </SheetHeader>

        {/* SLA Banner for Sanctions */}
        {isSanctions && (
          <div className="my-4 flex items-start gap-2.5 rounded-lg border border-destructive/40 p-3 text-xs text-destructive">
            <Clock className="mt-0.5 h-4 w-4 shrink-0 text-destructive" />
            <div>
              <p className="font-bold">Correspondência de sanções — acção necessária</p>
              <p className="mt-0.5">
                {slaText} — bloquear, rever e escalar dentro da janela de decisão.
              </p>
            </div>
          </div>
        )}

        <div className="space-y-5 py-4 text-xs">
          {/* Client Details Section */}
          <div className="space-y-2">
            <h4 className="flex items-center gap-1.5 font-semibold uppercase tracking-wider text-muted-foreground">
              <User className="h-3.5 w-3.5 text-primary" /> Cliente
            </h4>
            <div className="space-y-1.5 rounded-lg border bg-muted/40 p-3">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Nome</span>
                <span className="font-semibold text-foreground">
                  {flag.client?.fullName || "—"}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Documento</span>
                <span className="font-medium">
                  {flag.client?.documentNumber}{" "}
                  {flag.client?.nuib ? `· NUIB: ${flag.client.nuib}` : ""}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Diligência</span>
                <Badge
                  variant={
                    flag.client?.regime === "SIMPLIFICADO" ? "outline" : "secondary"
                  }
                >
                  {flag.client?.regime || "STANDARD"}
                </Badge>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Nível de risco</span>
                <Badge
                  variant={
                    flag.client?.riskRating === "ALTO" ? "destructive" : "secondary"
                  }
                >
                  {flag.client?.riskRating || "BAIXO"}
                </Badge>
              </div>
              {flag.client?.isPep && (
                <div className="flex items-center justify-between rounded border border-amber-500/20 bg-amber-500/10 p-1.5">
                  <span className="font-bold text-amber-800">
                    Pessoa com exposição política
                  </span>
                  <Badge variant="warning">{flag.client.pepCategory || "Nacional"}</Badge>
                </div>
              )}
            </div>
          </div>

          {/* Transaction Section */}
          {flag.transaction && (
            <div className="space-y-2">
              <h4 className="flex items-center gap-1.5 font-semibold uppercase tracking-wider text-muted-foreground">
                <CreditCard className="h-3.5 w-3.5 text-primary" /> Transacção
              </h4>
              <div className="space-y-1.5 rounded-lg border bg-muted/40 p-3">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Montante</span>
                  <span className="text-base font-bold text-foreground">
                    {formatMt(flag.transaction.amount)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Tipo</span>
                  <span className="font-medium">{flag.transaction.transactionType}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Conta</span>
                  <span className="font-mono">{flag.transaction.accountNumber}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Data</span>
                  <span>
                    {new Date(flag.transaction.transactionDate).toLocaleString("pt-MZ")}
                  </span>
                </div>
                {flag.transaction.counterpartyName && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Contraparte</span>
                    <span>
                      {flag.transaction.counterpartyName} (
                      {flag.transaction.counterpartyDocument || "—"})
                    </span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Technical details */}
          <div className="space-y-2">
            <h4 className="flex items-center gap-1.5 font-semibold uppercase tracking-wider text-muted-foreground">
              <FileSpreadsheet className="h-3.5 w-3.5 text-primary" /> Detalhes do alerta
            </h4>
            <div className="break-all rounded-lg border bg-muted/20 p-3 font-mono text-[11px] leading-relaxed">
              {flag.details
                ? typeof flag.details === "string"
                  ? flag.details
                  : JSON.stringify(flag.details, null, 2)
                : "Sem detalhes adicionais."}
            </div>
          </div>

          {/* Review notes */}
          <div className="space-y-2 pt-2">
            <Label htmlFor="reviewNotes" className="font-semibold text-foreground">
              Notas de análise <span className="text-destructive">*</span>
            </Label>
            <textarea
              id="reviewNotes"
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Descreva a sua análise e a fundamentação da decisão…"
              className="w-full rounded-md border border-input bg-background p-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            />
            {actionError && (
              <p className="flex items-center gap-1 text-xs font-medium text-destructive">
                <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
                {actionError}
              </p>
            )}
          </div>
        </div>

        <SheetFooter className="flex-col gap-2 border-t pt-4 sm:flex-row">
          <Button
            variant="outline"
            size="sm"
            onClick={() => handleReviewAction("DISMISS")}
            disabled={isSubmitting}
            className="text-xs text-muted-foreground hover:text-foreground"
          >
            <XCircle className="mr-1 h-3.5 w-3.5" />
            Descartar
          </Button>

          <Button
            variant="secondary"
            size="sm"
            onClick={() => handleReviewAction("RESOLVE")}
            disabled={isSubmitting}
            className="text-xs"
          >
            <CheckCircle2 className="mr-1 h-3.5 w-3.5 text-emerald-600" />
            Resolver
          </Button>

          <Button
            variant="destructive"
            size="sm"
            onClick={handleEscalateStr}
            disabled={isSubmitting}
            className="gap-1 text-xs shadow-sm"
          >
            {isSubmitting ? (
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
            ) : (
              <FileWarning className="h-3.5 w-3.5" />
            )}
            Escalar para comunicação
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  )
}
