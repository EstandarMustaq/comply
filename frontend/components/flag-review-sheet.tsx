"use client";

import React, { useState } from "react";
import {
  ShieldAlert,
  Clock,
  User,
  CreditCard,
  FileSpreadsheet,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  FileWarning,
  Loader2,
} from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "./ui/sheet";
import { Badge } from "./ui/badge";
import { Button } from "./ui/button";
import { Label } from "./ui/label";
import { Flag } from "@/lib/types";
import { api } from "@/lib/api-client";
import { useRouter } from "next/navigation";

interface FlagReviewSheetProps {
  flag: Flag | null;
  isOpen: boolean;
  onClose: () => void;
  onFlagUpdated: () => void;
}

export function FlagReviewSheet({
  flag,
  isOpen,
  onClose,
  onFlagUpdated,
}: FlagReviewSheetProps) {
  const router = useRouter();
  const [notes, setNotes] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  if (!flag) return null;

  const isSanctions = flag.flagType === "SANCTIONS_MATCH";

  const calculateSlaRemaining = (deadline?: string) => {
    if (!deadline) return null;
    const diff = new Date(deadline).getTime() - new Date().getTime();
    if (diff <= 0) return "SLA Expirado (Atraso crítico!)";
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    return `${hours}h ${minutes}m restantes`;
  };

  const slaText = calculateSlaRemaining(flag.slaDeadline);

  const handleReviewAction = async (action: "DISMISS" | "RESOLVE") => {
    if (!notes.trim()) {
      setActionError("Review notes are required before archiving or resolving an alert.");
      return;
    }
    try {
      setIsSubmitting(true);
      setActionError(null);
      await api.reviewFlag(flag.id, action, notes);
      setNotes("");
      onFlagUpdated();
      onClose();
    } catch (err: any) {
      setActionError(err.message || "Failed to update alert.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEscalateStr = async () => {
    try {
      setIsSubmitting(true);
      setActionError(null);
      const str = await api.escalateFlagToStr(
        flag.id,
        notes || "Escalated for formal reporting."
      );
      setNotes("");
      onFlagUpdated();
      onClose();
      router.push(`/reports?strId=${str.id}`);
    } catch (err: any) {
      setActionError(err.message || "Failed to escalate alert.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatMt = (val?: number) => {
    if (val === undefined || val === null) return "N/D";
    return new Intl.NumberFormat("pt-MZ", {
      style: "currency",
      currency: "MZN",
    }).format(val);
  };

  return (
    <Sheet open={isOpen} onOpenChange={onClose}>
      <SheetContent className="overflow-y-auto max-h-screen sm:max-w-xl">
        <SheetHeader className="pb-4 border-b">
          <div className="flex items-center justify-between gap-2">
            <Badge
              variant={isSanctions ? "urgent" : "secondary"}
              className="text-xs px-2.5 py-0.5 uppercase tracking-wider"
            >
              {flag.flagType.replace(/_/g, " ")}
            </Badge>
            <Badge variant="outline" className="text-[11px]">
              ID: {flag.id.substring(0, 8)}...
            </Badge>
          </div>
          <SheetTitle className="text-base font-bold text-foreground mt-2">
            Alert Review
          </SheetTitle>
          <SheetDescription className="text-xs">
            Rule triggered: <strong>{flag.ruleTriggered}</strong>
          </SheetDescription>
        </SheetHeader>

        {/* SLA Banner for Sanctions */}
        {isSanctions && (
          <div className="my-4 p-3 bg-red-500/10 border border-red-500/30 rounded-lg flex items-start gap-2.5 text-xs text-red-700">
            <Clock className="h-4 w-4 shrink-0 text-red-600 mt-0.5 animate-pulse" />
            <div>
              <p className="font-bold">Sanctions match — action required</p>
              <p className="mt-0.5">
                {slaText} — freeze, review, and escalate within the required window.
              </p>
            </div>
          </div>
        )}

        <div className="space-y-5 py-4 text-xs">
          {/* Client Details Section */}
          <div className="space-y-2">
            <h4 className="font-semibold text-muted-foreground uppercase tracking-wider flex items-center gap-1.5">
              <User className="h-3.5 w-3.5 text-primary" /> Client
            </h4>
            <div className="p-3 bg-muted/40 rounded-lg space-y-1.5 border">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Name</span>
                <span className="font-semibold text-foreground">{flag.client?.fullName || "—"}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Document ID</span>
                <span className="font-medium">{flag.client?.documentNumber} {flag.client?.nuib ? `· NUIB: ${flag.client.nuib}` : ""}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-muted-foreground">Due diligence</span>
                <Badge variant={flag.client?.regime === "SIMPLIFICADO" ? "outline" : "secondary"}>
                  {flag.client?.regime || "STANDARD"}
                </Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-muted-foreground">Risk rating</span>
                <Badge variant={flag.client?.riskRating === "ALTO" ? "destructive" : "secondary"}>
                  {flag.client?.riskRating || "LOW"}
                </Badge>
              </div>
              {flag.client?.isPep && (
                <div className="flex justify-between items-center bg-amber-500/10 p-1.5 rounded border border-amber-500/20">
                  <span className="font-bold text-amber-800">Politically Exposed Person (PEP)</span>
                  <Badge variant="warning">{flag.client.pepCategory || "National"}</Badge>
                </div>
              )}
            </div>
          </div>

          {/* Transaction Section */}
          {flag.transaction && (
            <div className="space-y-2">
              <h4 className="font-semibold text-muted-foreground uppercase tracking-wider flex items-center gap-1.5">
                <CreditCard className="h-3.5 w-3.5 text-primary" /> Transaction
              </h4>
              <div className="p-3 bg-muted/40 rounded-lg space-y-1.5 border">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Amount</span>
                  <span className="font-bold text-base text-foreground">
                    {formatMt(flag.transaction.amount)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Type</span>
                  <span className="font-medium">{flag.transaction.transactionType}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Account</span>
                  <span className="font-mono">{flag.transaction.accountNumber}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Date</span>
                  <span>{new Date(flag.transaction.transactionDate).toLocaleString()}</span>
                </div>
                {flag.transaction.counterpartyName && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Counterparty</span>
                    <span>{flag.transaction.counterpartyName} ({flag.transaction.counterpartyDocument || "—"})</span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Technical details */}
          <div className="space-y-2">
            <h4 className="font-semibold text-muted-foreground uppercase tracking-wider flex items-center gap-1.5">
              <FileSpreadsheet className="h-3.5 w-3.5 text-primary" /> Alert details
            </h4>
            <div className="p-3 bg-muted/20 rounded-lg border font-mono text-[11px] leading-relaxed break-all">
              {flag.details ? (
                typeof flag.details === "string" ? (
                  flag.details
                ) : (
                  JSON.stringify(flag.details, null, 2)
                )
              ) : (
                "No additional details."
              )}
            </div>
          </div>

          {/* Review notes */}
          <div className="space-y-2 pt-2">
            <Label htmlFor="reviewNotes" className="font-semibold text-foreground">
              Review notes <span className="text-red-500">*</span>
            </Label>
            <textarea
              id="reviewNotes"
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Describe your analysis and the basis for your decision…"
              className="w-full rounded-md border border-input bg-background p-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            />
            {actionError && (
              <p className="text-xs text-red-600 font-medium flex items-center gap-1">
                <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
                {actionError}
              </p>
            )}
          </div>
        </div>

        <SheetFooter className="border-t pt-4 flex-col sm:flex-row gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => handleReviewAction("DISMISS")}
            disabled={isSubmitting}
            className="text-xs text-muted-foreground hover:text-foreground"
          >
            <XCircle className="h-3.5 w-3.5 mr-1" />
            Dismiss
          </Button>

          <Button
            variant="secondary"
            size="sm"
            onClick={() => handleReviewAction("RESOLVE")}
            disabled={isSubmitting}
            className="text-xs"
          >
            <CheckCircle2 className="h-3.5 w-3.5 mr-1 text-emerald-600" />
            Resolve
          </Button>

          <Button
            variant="destructive"
            size="sm"
            onClick={handleEscalateStr}
            disabled={isSubmitting}
            className="text-xs gap-1 shadow-sm"
          >
            {isSubmitting ? (
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
            ) : (
              <FileWarning className="h-3.5 w-3.5" />
            )}
            Escalate to report
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
