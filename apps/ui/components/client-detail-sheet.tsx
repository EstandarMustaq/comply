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
    return new Intl.NumberFormat("en-MZ", {
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
              Risk: {client.riskRating}
            </Badge>
            <Badge variant="outline" className="font-mono text-xs">
              ID: {client.id.substring(0, 8)}
            </Badge>
          </div>
          <SheetTitle className="mt-2 flex items-center gap-2 text-base font-bold text-foreground">
            <User className="h-5 w-5 text-primary" />
            {client.fullName}
          </SheetTitle>
          <SheetDescription className="text-xs">
            {client.clientType === "INDIVIDUAL" ? "Individual" : "Business"} • Created{" "}
            {new Date(client.createdAt).toLocaleDateString("en-MZ")}
          </SheetDescription>
        </SheetHeader>

        <div className="space-y-6 py-4 text-xs">
          {/* Identity and documents */}
          <div className="space-y-2">
            <h4 className="font-semibold uppercase tracking-wider text-muted-foreground">
              Identity and documents
            </h4>
            <div className="space-y-2 rounded-lg border bg-muted/40 p-3">
              <div className="flex justify-between">
                <span className="text-muted-foreground">
                  Document (ID / residence permit / passport / NUIT):
                </span>
                <span className="font-semibold">{client.documentNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">NUIB (bank identifier):</span>
                <span className="font-mono">{client.nuib || "Not issued"}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Due diligence:</span>
                <Badge variant={isSimplified ? "default" : "secondary"}>
                  {isSimplified ? "Simplified" : "Standard / enhanced"}
                </Badge>
              </div>
            </div>
          </div>

          {/* Politically exposed person status */}
          {client.isPep ? (
            <div className="space-y-1 rounded-lg border border-amber-500/30 bg-amber-500/10 p-3 text-amber-900">
              <div className="flex items-center gap-1.5 font-bold">
                <AlertTriangle className="h-4 w-4 text-amber-600" />
                Politically exposed person
              </div>
              <p className="text-[11px] text-amber-800">
                Category:{" "}
                <strong>
                  {client.pepCategory || "National / relevant public office"}
                </strong>
              </p>
              {client.lastPepReviewDate && (
                <p className="text-[10px] text-amber-700">
                  Last annual review:{" "}
                  {new Date(client.lastPepReviewDate).toLocaleDateString("en-MZ")}
                </p>
              )}
            </div>
          ) : (
            <div className="flex items-center gap-2 rounded-lg border border-emerald-500/20 bg-emerald-500/10 p-2.5 text-emerald-800">
              <ShieldCheck className="h-4 w-4 text-emerald-600" />
              <span>No politically exposed person match. Screening is clear.</span>
            </div>
          )}

          {/* Simplified regime limits */}
          {isSimplified && (
            <div className="space-y-3 rounded-lg border border-border bg-muted/40 p-3">
              <div className="flex items-center justify-between">
                <h4 className="flex items-center gap-1.5 font-semibold text-foreground">
                  <DollarSign className="h-4 w-4 text-primary" />
                  Threshold monitoring
                </h4>
                <Badge
                  variant="outline"
                  className="border-border text-[10px] text-foreground"
                >
                  Simplified due diligence
                </Badge>
              </div>

              {/* Monthly limit */}
              <div className="space-y-1">
                <div className="flex justify-between text-[11px]">
                  <span>
                    Monthly limit: {formatMt(monthlyVolume)} / {formatMt(monthlyLimit)}
                  </span>
                  <span
                    className={`font-bold ${monthlyPercent > 90 ? "text-destructive" : "text-primary"}`}
                  >
                    {monthlyPercent}%
                  </span>
                </div>
                <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
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
                    Annual limit: {formatMt(annualVolume)} / {formatMt(annualLimit)}
                  </span>
                  <span
                    className={`font-bold ${annualPercent > 90 ? "text-destructive" : "text-primary"}`}
                  >
                    {annualPercent}%
                  </span>
                </div>
                <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                  <div
                    className={`h-full transition-all ${
                      annualPercent > 90 ? "bg-destructive" : "bg-primary"
                    }`}
                    style={{ width: `${annualPercent}%` }}
                  />
                </div>
              </div>
              <p className="text-[10px] text-muted-foreground">
                Exceeding the monthly (30,000 MZN) or annual (240,000 MZN) limit requires
                enhanced due diligence.
              </p>
            </div>
          )}

          {/* Beneficial owners */}
          <div className="space-y-2">
            <h4 className="flex items-center gap-1.5 font-semibold uppercase tracking-wider text-muted-foreground">
              <Layers className="h-3.5 w-3.5 text-primary" />
              Beneficial owners
            </h4>
            {client.beneficialOwners && client.beneficialOwners.length > 0 ? (
              <div className="space-y-2">
                {client.beneficialOwners.map((ubo) => (
                  <div
                    key={ubo.id}
                    className="space-y-1 rounded-lg border bg-muted/40 p-2.5"
                  >
                    <div className="flex justify-between font-semibold">
                      <span>{ubo.fullName}</span>
                      <span className="text-primary">
                        {ubo.ownershipPercentage}% ownership
                      </span>
                    </div>
                    <div className="flex justify-between text-[11px] text-muted-foreground">
                      <span>
                        ID: {ubo.documentNumber} ({ubo.nationality})
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
              <div className="rounded-lg border bg-muted/20 p-3 text-center text-muted-foreground">
                No indirect beneficial owners declared (single direct owner).
              </div>
            )}
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}
