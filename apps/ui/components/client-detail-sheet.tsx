"use client"

import { User, ShieldCheck, AlertTriangle, DollarSign, Layers } from "lucide-react"
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "./ui/sheet"
import { Badge } from "./ui/badge"
import { type Client } from "@/lib/types"

interface ClientDetailSheetProps {
  client: Client | null
  isOpen: boolean
  onClose: () => void
}

export function ClientDetailSheet({ client, isOpen, onClose }: ClientDetailSheetProps) {
  if (!client) return null

  const isSimplified = client.regime === "SIMPLIFICADO"
  const monthlyLimit = 30000
  const annualLimit = 240000

  const monthlyVolume = client.monthlyVolumeMt || 0
  const annualVolume = client.annualVolumeMt || 0

  const monthlyPercent = Math.min(100, Math.round((monthlyVolume / monthlyLimit) * 100))
  const annualPercent = Math.min(100, Math.round((annualVolume / annualLimit) * 100))

  const formatMt = (val: number) => {
    return new Intl.NumberFormat("pt-MZ", {
      style: "currency",
      currency: "MZN",
      maximumFractionDigits: 0,
    }).format(val)
  }

  return (
    <Sheet open={isOpen} onOpenChange={onClose}>
      <SheetContent className="max-h-screen overflow-y-auto sm:max-w-xl">
        <SheetHeader className="border-b pb-4">
          <div className="flex items-center justify-between">
            <Badge
              variant={client.riskRating === "ALTO" ? "destructive" : "secondary"}
              className="text-xs"
            >
              Risco: {client.riskRating}
            </Badge>
            <Badge variant="outline" className="font-mono text-xs">
              ID: {client.id.substring(0, 8)}
            </Badge>
          </div>
          <SheetTitle className="text-foreground mt-2 flex items-center gap-2 text-base font-bold">
            <User className="text-primary h-5 w-5" />
            {client.fullName}
          </SheetTitle>
          <SheetDescription className="text-xs">
            {client.clientType === "INDIVIDUAL" ? "Pessoa Singular" : "Pessoa Colectiva"}{" "}
            • Cadastrado em {new Date(client.createdAt).toLocaleDateString("pt-MZ")}
          </SheetDescription>
        </SheetHeader>

        <div className="space-y-6 py-4 text-xs">
          {/* Identity and documents */}
          <div className="space-y-2">
            <h4 className="text-muted-foreground font-semibold uppercase tracking-wider">
              Identificação e Documentação
            </h4>
            <div className="bg-muted/40 space-y-2 rounded-lg border p-3">
              <div className="flex justify-between">
                <span className="text-muted-foreground">
                  Documento (BI/DIRE/Passaporte/NUIT):
                </span>
                <span className="font-semibold">{client.documentNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">
                  NUIB (Identificação Bancária):
                </span>
                <span className="font-mono">{client.nuib || "Pendente de emissão"}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">
                  Regime de Diligência Aplicável:
                </span>
                <Badge variant={isSimplified ? "default" : "secondary"}>
                  {isSimplified ? "Simplificado" : "Normal / Reforçado"}
                </Badge>
              </div>
            </div>
          </div>

          {/* Politically exposed person status */}
          {client.isPep ? (
            <div className="space-y-1 rounded-lg border border-amber-500/30 bg-amber-500/10 p-3 text-amber-900">
              <div className="flex items-center gap-1.5 font-bold">
                <AlertTriangle className="h-4 w-4 text-amber-600" />
                Pessoa com exposição política
              </div>
              <p className="text-[11px] text-amber-800">
                Categoria:{" "}
                <strong>
                  {client.pepCategory || "Nacional / Funções públicas relevantes"}
                </strong>
              </p>
              {client.lastPepReviewDate && (
                <p className="text-[10px] text-amber-700">
                  Última revisão anual de conformidade:{" "}
                  {new Date(client.lastPepReviewDate).toLocaleDateString("pt-MZ")}
                </p>
              )}
            </div>
          ) : (
            <div className="flex items-center gap-2 rounded-lg border border-emerald-500/20 bg-emerald-500/10 p-2.5 text-emerald-800">
              <ShieldCheck className="h-4 w-4 text-emerald-600" />
              <span>
                Cliente não identificado como pessoa com exposição política. Triagem
                limpa.
              </span>
            </div>
          )}

          {/* Simplified regime limits */}
          {isSimplified && (
            <div className="border-border bg-muted/40 space-y-3 rounded-lg border p-3">
              <div className="flex items-center justify-between">
                <h4 className="text-foreground flex items-center gap-1.5 font-semibold">
                  <DollarSign className="text-primary h-4 w-4" />
                  Monitorização de limiares
                </h4>
                <Badge
                  variant="outline"
                  className="border-border text-foreground text-[10px]"
                >
                  Regime simplificado
                </Badge>
              </div>

              {/* Monthly limit */}
              <div className="space-y-1">
                <div className="flex justify-between text-[11px]">
                  <span>
                    Limite Mensal: {formatMt(monthlyVolume)} / {formatMt(monthlyLimit)}
                  </span>
                  <span
                    className={`font-bold ${monthlyPercent > 90 ? "text-destructive" : "text-primary"}`}
                  >
                    {monthlyPercent}%
                  </span>
                </div>
                <div className="bg-muted h-2 w-full overflow-hidden rounded-full">
                  <div
                    className={`h-full transition-all ${
                      monthlyPercent > 90 ? "bg-destructive" : "bg-primary"
                    }`}
                    style={{ width: `${monthlyPercent}%` }}
                  />
                </div>
              </div>

              {/* Annual limit */}
              <div className="space-y-1">
                <div className="flex justify-between text-[11px]">
                  <span>
                    Limite Anual: {formatMt(annualVolume)} / {formatMt(annualLimit)}
                  </span>
                  <span
                    className={`font-bold ${annualPercent > 90 ? "text-destructive" : "text-primary"}`}
                  >
                    {annualPercent}%
                  </span>
                </div>
                <div className="bg-muted h-2 w-full overflow-hidden rounded-full">
                  <div
                    className={`h-full transition-all ${
                      annualPercent > 90 ? "bg-destructive" : "bg-primary"
                    }`}
                    style={{ width: `${annualPercent}%` }}
                  />
                </div>
              </div>
              <p className="text-muted-foreground text-[10px]">
                Ultrapassar o limite mensal (30.000 MT) ou anual (240.000 MT) exige
                transição para diligência reforçada.
              </p>
            </div>
          )}

          {/* Beneficial owners */}
          <div className="space-y-2">
            <h4 className="text-muted-foreground flex items-center gap-1.5 font-semibold uppercase tracking-wider">
              <Layers className="text-primary h-3.5 w-3.5" />
              Beneficiários efectivos
            </h4>
            {client.beneficialOwners && client.beneficialOwners.length > 0 ? (
              <div className="space-y-2">
                {client.beneficialOwners.map((ubo) => (
                  <div
                    key={ubo.id}
                    className="bg-muted/40 space-y-1 rounded-lg border p-2.5"
                  >
                    <div className="flex justify-between font-semibold">
                      <span>{ubo.fullName}</span>
                      <span className="text-primary">
                        {ubo.ownershipPercentage}% Participação
                      </span>
                    </div>
                    <div className="text-muted-foreground flex justify-between text-[11px]">
                      <span>
                        Doc: {ubo.documentNumber} ({ubo.nationality})
                      </span>
                      {ubo.isPep && (
                        <Badge variant="warning" className="text-[10px]">
                          PEP
                        </Badge>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="bg-muted/20 text-muted-foreground rounded-lg border p-3 text-center">
                Sem beneficiários efectivos indiretos declarados (titular direto único).
              </div>
            )}
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}
