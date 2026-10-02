"use client"

import { useState } from "react"
import {
  Clock,
  User,
  CreditCard,
  FileSpreadsheet,
  AlertTriangle,
  CheckCircle2,
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
import { getFlagTypeLabel, getRegimeLabel, getTransactionTypeLabel } from "@/lib/labels"
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
    if (diff <= 0) return "Prazo de revisão ultrapassado"
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    return `${hours}h ${minutes}m restantes`
  }

  const slaText = calculateSlaRemaining(flag.slaDeadline)

  const handleReviewAction = async () => {
    if (!notes.trim()) {
      setActionError("Registe as notas de análise antes de resolver o alerta.")
      return
    }
    try {
      setIsSubmitting(true)
      setActionError(null)
      await api.reviewFlag(flag.id, notes)
      setNotes("")
      onFlagUpdated()
      onClose()
    } catch (err: unknown) {
      setActionError(
        err instanceof Error ? err.message : "Não foi possível atualizar o alerta."
      )
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
        notes || "Escalado para comunicação formal."
      )
      setNotes("")
      onFlagUpdated()
      onClose()
      router.push(`/reports?strId=${str.id}`)
    } catch (err: unknown) {
      setActionError(
        err instanceof Error ? err.message : "Não foi possível escalar o alerta."
      )
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
              {getFlagTypeLabel(flag.flagType)}
            </Badge>
            <Badge variant="outline" className="text-[11px]">
              ID: {flag.id.substring(0, 8)}...
            </Badge>
          </div>
          <SheetTitle className="text-foreground mt-2 text-base font-bold">
            Análise do alerta
          </SheetTitle>
          <SheetDescription className="text-xs">
            Regra activada: <strong>{flag.ruleTriggered}</strong>
          </SheetDescription>
        </SheetHeader>

        {/* Deadline status for restricted-party matches */}
        {isSanctions && (
          <div className="border-destructive/40 text-destructive my-4 flex items-start gap-2.5 rounded-lg border p-3 text-xs">
            <Clock className="text-destructive mt-0.5 h-4 w-4 shrink-0" />
            <div>
              <p className="font-bold">Correspondência de sanções — acção necessária</p>
              <p className="mt-0.5">
                {slaText} — bloquear, rever e escalar dentro da janela de decisão.
              </p>
            </div>
          </div>
        )}

        <div className="space-y-5 py-4 text-xs">
          {/* Customer details */}
          <div className="space-y-2">
            <h4 className="text-muted-foreground flex items-center gap-1.5 font-semibold uppercase tracking-wider">
              <User className="text-primary h-3.5 w-3.5" /> Cliente
            </h4>
            <div className="bg-muted/40 space-y-1.5 rounded-lg border p-3">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Nome</span>
                <span className="text-foreground font-semibold">
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
                  {getRegimeLabel(flag.client?.regime || "NORMAL")}
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

          {/* Transaction details */}
          {flag.transaction && (
            <div className="space-y-2">
              <h4 className="text-muted-foreground flex items-center gap-1.5 font-semibold uppercase tracking-wider">
                <CreditCard className="text-primary h-3.5 w-3.5" /> Transacção
              </h4>
              <div className="bg-muted/40 space-y-1.5 rounded-lg border p-3">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Montante</span>
                  <span className="text-foreground text-base font-bold">
                    {formatMt(flag.transaction.amount)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Tipo</span>
                  <span className="font-medium">
                    {getTransactionTypeLabel(flag.transaction.transactionType)}
                  </span>
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

          {/* Match details */}
          <div className="space-y-2">
            <h4 className="text-muted-foreground flex items-center gap-1.5 font-semibold uppercase tracking-wider">
              <FileSpreadsheet className="text-primary h-3.5 w-3.5" /> Detalhes do alerta
            </h4>
            <div className="bg-muted/20 break-all rounded-lg border p-3 font-mono text-[11px] leading-relaxed">
              {flag.details
                ? typeof flag.details === "string"
                  ? flag.details
                  : JSON.stringify(flag.details, null, 2)
                : "Sem detalhes adicionais."}
            </div>
          </div>

          {/* Review notes */}
          <div className="space-y-2 pt-2">
            <Label htmlFor="reviewNotes" className="text-foreground font-semibold">
              Notas de análise <span className="text-destructive">*</span>
            </Label>
            <textarea
              id="reviewNotes"
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Descreva a sua análise e a fundamentação da decisão…"
              className="border-input bg-background focus-visible:ring-ring w-full rounded-md border p-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1"
            />
            {actionError && (
              <p className="text-destructive flex items-center gap-1 text-xs font-medium">
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
            onClick={handleReviewAction}
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
