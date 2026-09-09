"use client";

import React from "react";
import {
  User,
  ShieldCheck,
  AlertTriangle,
  FileText,
  DollarSign,
  Briefcase,
  Layers,
  Calendar,
} from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "./ui/sheet";
import { Badge } from "./ui/badge";
import { Client } from "@/lib/types";

interface ClientDetailSheetProps {
  client: Client | null;
  isOpen: boolean;
  onClose: () => void;
}

export function ClientDetailSheet({
  client,
  isOpen,
  onClose,
}: ClientDetailSheetProps) {
  if (!client) return null;

  const isSimplified = client.regime === "SIMPLIFICADO";
  const monthlyLimit = 30000;
  const annualLimit = 240000;

  const monthlyVolume = client.monthlyVolumeMt || 0;
  const annualVolume = client.annualVolumeMt || 0;

  const monthlyPercent = Math.min(100, Math.round((monthlyVolume / monthlyLimit) * 100));
  const annualPercent = Math.min(100, Math.round((annualVolume / annualLimit) * 100));

  const formatMt = (val: number) => {
    return new Intl.NumberFormat("pt-MZ", {
      style: "currency",
      currency: "MZN",
      maximumFractionDigits: 0,
    }).format(val);
  };

  return (
    <Sheet open={isOpen} onOpenChange={onClose}>
      <SheetContent className="overflow-y-auto max-h-screen sm:max-w-xl">
        <SheetHeader className="pb-4 border-b">
          <div className="flex items-center justify-between">
            <Badge
              variant={client.riskRating === "ALTO" ? "destructive" : "secondary"}
              className="text-xs"
            >
              Risco: {client.riskRating}
            </Badge>
            <Badge variant="outline" className="text-xs font-mono">
              ID: {client.id.substring(0, 8)}
            </Badge>
          </div>
          <SheetTitle className="text-base font-bold text-foreground mt-2 flex items-center gap-2">
            <User className="h-5 w-5 text-primary" />
            {client.fullName}
          </SheetTitle>
          <SheetDescription className="text-xs">
            {client.clientType === "INDIVIDUAL" ? "Pessoa Singular" : "Pessoa Colectiva"} • Cadastrado em{" "}
            {new Date(client.createdAt).toLocaleDateString("pt-MZ")}
          </SheetDescription>
        </SheetHeader>

        <div className="space-y-6 py-4 text-xs">
          {/* Identificação e Documentos */}
          <div className="space-y-2">
            <h4 className="font-semibold text-muted-foreground uppercase tracking-wider">
              Identificação & Documentação KYC
            </h4>
            <div className="p-3 bg-muted/40 rounded-lg space-y-2 border">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Documento (BI/DIRE/Passaporte/NUIT):</span>
                <span className="font-semibold">{client.documentNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">NUIB (Identificação Bancária):</span>
                <span className="font-mono">{client.nuib || "Pendente de emissão"}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-muted-foreground">Regime de Diligência Aplicável:</span>
                <Badge variant={isSimplified ? "default" : "secondary"}>
                  {isSimplified ? "Simplificado (Cap. XIII)" : "Normal / Reforçado"}
                </Badge>
              </div>
            </div>
          </div>

          {/* PEP Status */}
          {client.isPep ? (
            <div className="p-3 bg-amber-500/10 border border-amber-500/30 rounded-lg space-y-1 text-amber-900">
              <div className="flex items-center gap-1.5 font-bold">
                <AlertTriangle className="h-4 w-4 text-amber-600" />
                Pessoa Exposta Politicamente (PEP)
              </div>
              <p className="text-[11px] text-amber-800">
                Categoria: <strong>{client.pepCategory || "Nacional / Funções Públicas Relevantes"}</strong>
              </p>
              {client.lastPepReviewDate && (
                <p className="text-[10px] text-amber-700">
                  Última revisão anual de conformidade: {new Date(client.lastPepReviewDate).toLocaleDateString("pt-MZ")}
                </p>
              )}
            </div>
          ) : (
            <div className="p-2.5 bg-emerald-500/10 border border-emerald-500/20 rounded-lg flex items-center gap-2 text-emerald-800">
              <ShieldCheck className="h-4 w-4 text-emerald-600" />
              <span>Cliente não identificado como PEP. Triagem de listas negativas negativa.</span>
            </div>
          )}

          {/* Limites do Regime Simplificado */}
          {isSimplified && (
            <div className="space-y-3 p-3 bg-muted/40 border border-border rounded-lg">
              <div className="flex items-center justify-between">
                <h4 className="font-semibold text-foreground flex items-center gap-1.5">
                  <DollarSign className="h-4 w-4 text-primary" />
                  Monitoramento de Limiares
                </h4>
                <Badge variant="outline" className="text-[10px] border-border text-foreground">
                  Cap. XIII
                </Badge>
              </div>

              {/* Mensal */}
              <div className="space-y-1">
                <div className="flex justify-between text-[11px]">
                  <span>Limite Mensal: {formatMt(monthlyVolume)} / {formatMt(monthlyLimit)}</span>
                  <span className={`font-bold ${monthlyPercent > 90 ? "text-destructive" : "text-primary"}`}>
                    {monthlyPercent}%
                  </span>
                </div>
                <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                  <div
                    className={`h-full transition-all ${
                      monthlyPercent > 90 ? "bg-destructive" : "bg-primary"
                    }`}
                    style={{ width: `${monthlyPercent}%` }}
                  />
                </div>
              </div>

              {/* Anual */}
              <div className="space-y-1">
                <div className="flex justify-between text-[11px]">
                  <span>Limite Anual: {formatMt(annualVolume)} / {formatMt(annualLimit)}</span>
                  <span className={`font-bold ${annualPercent > 90 ? "text-destructive" : "text-primary"}`}>
                    {annualPercent}%
                  </span>
                </div>
                <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                  <div
                    className={`h-full transition-all ${
                      annualPercent > 90 ? "bg-destructive" : "bg-primary"
                    }`}
                    style={{ width: `${annualPercent}%` }}
                  />
                </div>
              </div>
              <p className="text-[10px] text-muted-foreground">
                Se excedido o teto mensal (30.000 MT) ou anual (240.000 MT), a conta é automaticamente migrada para o Regime Normal com exigência de diligência completa.
              </p>
            </div>
          )}

          {/* Beneficiários Efectivos (UBO) */}
          <div className="space-y-2">
            <h4 className="font-semibold text-muted-foreground uppercase tracking-wider flex items-center gap-1.5">
              <Layers className="h-3.5 w-3.5 text-primary" />
              Beneficiários Efectivos (UBO) - Decreto 53/2023
            </h4>
            {client.beneficialOwners && client.beneficialOwners.length > 0 ? (
              <div className="space-y-2">
                {client.beneficialOwners.map((ubo) => (
                  <div key={ubo.id} className="p-2.5 bg-muted/40 rounded-lg border space-y-1">
                    <div className="flex justify-between font-semibold">
                      <span>{ubo.fullName}</span>
                      <span className="text-primary">{ubo.ownershipPercentage}% Participação</span>
                    </div>
                    <div className="flex justify-between text-muted-foreground text-[11px]">
                      <span>Doc: {ubo.documentNumber} ({ubo.nationality})</span>
                      {ubo.isPep && <Badge variant="warning" className="text-[10px]">PEP</Badge>}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-3 bg-muted/20 rounded-lg border text-center text-muted-foreground">
                Sem beneficiários efectivos indiretos declarados (titular direto único).
              </div>
            )}
          </div>
        </div>
      </SheetContent>
    </Sheet>
  );
}
