"use client";

import React, { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import {
  FileText,
  Send,
  Download,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Building2,
  FileCheck,
  RefreshCw,
  Loader2,
} from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { api } from "@/lib/api-client";
import { SuspiciousActivityReport, CurrencyTransactionReport } from "@/lib/types";

export default function ReportsPage() {
  const searchParams = useSearchParams();
  const highlightStrId = searchParams.get("strId");

  const [activeTab, setActiveTab] = useState<string>("STR");
  const [strs, setStrs] = useState<SuspiciousActivityReport[]>([]);
  const [ctrs, setCtrs] = useState<CurrencyTransactionReport[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // Submit Modal States
  const [selectedStr, setSelectedStr] = useState<SuspiciousActivityReport | null>(null);
  const [isSubmitModalOpen, setIsSubmitModalOpen] = useState(false);
  const [suspicionGrounds, setSuspicionGrounds] = useState("");
  const [operationDetails, setOperationDetails] = useState("");
  const [evidenceSummary, setEvidenceSummary] = useState("");
  const [actionTaken, setActionTaken] = useState("");
  const [gifimProtocol, setGifimProtocol] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const loadData = async () => {
    try {
      setIsLoading(true);
      const [strsData, ctrsData] = await Promise.all([
        api.getStrs(),
        api.getCtrs(),
      ]);
      setStrs(strsData);
      setCtrs(ctrsData);

      if (highlightStrId) {
        const found = strsData.find((s) => s.id === highlightStrId);
        if (found) {
          openSubmitModal(found);
        }
      }
    } catch (err) {
      console.error("Erro ao carregar relatórios:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [highlightStrId]);

  const openSubmitModal = (report: SuspiciousActivityReport) => {
    setSelectedStr(report);
    setSuspicionGrounds(report.suspicionGrounds || "");
    setOperationDetails(report.operationDetails || "");
    setEvidenceSummary(report.evidenceSummary || "");
    setActionTaken(report.actionTaken || "Transaction held pending formal review.");
    setGifimProtocol(report.gifimProtocolNumber || `REF-${new Date().getFullYear()}-${Math.floor(1000 + Math.random() * 9000)}`);
    setIsSubmitModalOpen(true);
  };

  const handleSubmitStr = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedStr) return;

    if (!suspicionGrounds || !operationDetails || !gifimProtocol) {
      setSubmitError("Grounds, operation details, and reference number are required.");
      return;
    }

    try {
      setIsSubmitting(true);
      setSubmitError(null);
      await api.submitStr(selectedStr.id, {
        suspicionGrounds,
        operationDetails,
        evidenceSummary,
        actionTaken,
        gifimProtocolNumber: gifimProtocol,
      });

      setIsSubmitModalOpen(false);
      setSelectedStr(null);
      loadData();
    } catch (err: any) {
      setSubmitError(err.message || "Failed to submit report.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatMt = (val?: number) => {
    if (val === undefined || val === null) return "0 MT";
    return new Intl.NumberFormat("pt-MZ", {
      style: "currency",
      currency: "MZN",
      maximumFractionDigits: 0,
    }).format(val);
  };

  return (
    <div className="flex-1 flex flex-col">
      <SiteHeader
        title="Reports"
        action={
          <Button
            variant="outline"
            size="sm"
            onClick={loadData}
            disabled={isLoading}
            className="text-xs gap-1.5 h-8"
          >
            <RefreshCw className={`h-3 w-3 ${isLoading ? "animate-spin" : ""}`} />
            <span>Refresh</span>
          </Button>
        }
      />

      <main className="flex-1 p-6 space-y-6 overflow-y-auto">
        {/* Retention Notice */}
        <div className="p-3.5 bg-muted/40 border border-border rounded-lg flex items-start gap-3 text-xs text-foreground">
          <Clock className="h-4 w-4 text-primary shrink-0 mt-0.5" />
          <div>
            <span className="font-semibold">Report retention</span>
            <p className="mt-0.5 text-muted-foreground text-[11px]">
              All submitted reports, supporting evidence, and due diligence files are retained with deletion locks for <strong>10 years</strong>.
            </p>
          </div>
        </div>

        {/* Tab Selection */}
        <div className="flex items-center justify-between">
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full max-w-md">
            <TabsList className="grid grid-cols-2 w-full">
              <TabsTrigger value="STR" className="text-xs">
                Suspicious Activity (STR)
              </TabsTrigger>
              <TabsTrigger value="CTR" className="text-xs">
                Currency Transactions (CTR)
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </div>

        {activeTab === "STR" ? (
          /* STR Table Card */
          <Card className="shadow-sm border-border/70">
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-semibold flex items-center gap-2">
                <FileText className="h-4 w-4 text-primary" />
                Suspicious activity reports ({strs.length})
              </CardTitle>
              <CardDescription className="text-xs">
                Formal reports submitted to the regulatory body for suspicious transaction activity
              </CardDescription>
            </CardHeader>
            <CardContent>
              {strs.length === 0 ? (
                <div className="text-center py-12 text-xs text-muted-foreground">
                  No STR reports on record.
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Reference</TableHead>
                      <TableHead>Client</TableHead>
                      <TableHead>Protocol #</TableHead>
                      <TableHead>Retained until</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="text-right">Action</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {strs.map((report) => {
                      const isSubmitted = report.status === "SUBMITTED";
                      return (
                        <TableRow key={report.id}>
                          <TableCell className="font-mono text-xs font-semibold">
                            {report.strReference}
                          </TableCell>
                          <TableCell className="text-xs font-medium">
                            {report.client?.fullName || "—"}
                            <div className="text-[10px] text-muted-foreground font-mono">
                              {report.client?.documentNumber}
                            </div>
                          </TableCell>
                          <TableCell className="font-mono text-xs">
                            {report.gifimProtocolNumber ? (
                              <span className="text-emerald-700 font-semibold">
                                {report.gifimProtocolNumber}
                              </span>
                            ) : (
                              <span className="text-muted-foreground italic">Pending</span>
                            )}
                          </TableCell>
                          <TableCell className="text-xs text-muted-foreground">
                            {report.retentionDeadline ? (
                              <span className="font-medium text-foreground">
                                {new Date(report.retentionDeadline).toLocaleDateString()}
                              </span>
                            ) : (
                              "After submission"
                            )}
                          </TableCell>
                          <TableCell>
                            <Badge
                              variant={isSubmitted ? "success" : "warning"}
                              className="text-[10px]"
                            >
                              {isSubmitted ? "SUBMITTED" : "DRAFT"}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-right">
                            {isSubmitted ? (
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => openSubmitModal(report)}
                                className="text-xs h-7 px-2.5 gap-1"
                              >
                                <FileCheck className="h-3 w-3 text-emerald-600" />
                                <span>View</span>
                              </Button>
                            ) : (
                              <Button
                                size="sm"
                                onClick={() => openSubmitModal(report)}
                                className="text-xs h-7 px-2.5 gap-1"
                              >
                                <Send className="h-3 w-3" />
                                <span>Complete & submit</span>
                              </Button>
                            )}
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        ) : (
          /* CTR Table Card */
          <Card className="shadow-sm border-border/70">
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-semibold flex items-center gap-2">
                <FileText className="h-4 w-4 text-primary" />
                Currency transaction reports ({ctrs.length})
              </CardTitle>
              <CardDescription className="text-xs">
                Monthly reports for currency transactions that met the reporting threshold
              </CardDescription>
            </CardHeader>
            <CardContent>
              {ctrs.length === 0 ? (
                <div className="text-center py-12 text-xs text-muted-foreground">
                  No CTR reports pending.
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Reference</TableHead>
                      <TableHead>Client</TableHead>
                      <TableHead>Period</TableHead>
                      <TableHead>Total amount</TableHead>
                      <TableHead className="text-center">Transactions</TableHead>
                      <TableHead>Status</TableHead>
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
      </main>

      {/* Submit / View STR Dialog */}
      <Dialog open={isSubmitModalOpen} onOpenChange={setIsSubmitModalOpen}>
        <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Building2 className="h-5 w-5 text-primary" />
              {selectedStr?.status === "SUBMITTED"
                ? "Report record"
                : "Submit suspicious activity report"}
            </DialogTitle>
            <DialogDescription className="text-xs">
              Ref: <strong>{selectedStr?.strReference}</strong> · Client:{" "}
              <strong>{selectedStr?.client?.fullName || "—"}</strong>
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmitStr} className="space-y-4 py-2 text-xs">
            {submitError && (
              <div className="p-2.5 bg-red-500/10 border border-red-500/30 rounded text-red-600 flex items-center gap-1.5">
                <AlertTriangle className="h-4 w-4 shrink-0" />
                <span>{submitError}</span>
              </div>
            )}

            <div className="space-y-1.5">
              <Label htmlFor="gifimProtocol" className="font-semibold">
                Reference number <span className="text-red-500">*</span>
              </Label>
              <Input
                id="gifimProtocol"
                value={gifimProtocol}
                onChange={(e) => setGifimProtocol(e.target.value)}
                placeholder="e.g. REF-2026-00891"
                className="text-xs font-mono"
                disabled={selectedStr?.status === "SUBMITTED"}
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="suspicionGrounds" className="font-semibold">
                Grounds for suspicion <span className="text-red-500">*</span>
              </Label>
              <textarea
                id="suspicionGrounds"
                rows={3}
                value={suspicionGrounds}
                onChange={(e) => setSuspicionGrounds(e.target.value)}
                placeholder="Describe the objective indicators of suspicious activity, inconsistencies with the client's financial profile, or matches against sanctions lists…"
                className="w-full rounded-md border border-input bg-background p-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                disabled={selectedStr?.status === "SUBMITTED"}
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="operationDetails" className="font-semibold">
                Transaction details <span className="text-red-500">*</span>
              </Label>
              <textarea
                id="operationDetails"
                rows={3}
                value={operationDetails}
                onChange={(e) => setOperationDetails(e.target.value)}
                placeholder="Accounts, amounts, dates, channels, and counterparties involved…"
                className="w-full rounded-md border border-input bg-background p-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                disabled={selectedStr?.status === "SUBMITTED"}
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="evidenceSummary" className="font-semibold">
                Evidence summary
              </Label>
              <textarea
                id="evidenceSummary"
                rows={2}
                value={evidenceSummary}
                onChange={(e) => setEvidenceSummary(e.target.value)}
                placeholder="Account statements, deposit receipts, screening reports…"
                className="w-full rounded-md border border-input bg-background p-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                disabled={selectedStr?.status === "SUBMITTED"}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="actionTaken" className="font-semibold">
                Action taken
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
              <div className="p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-lg text-emerald-900 space-y-1">
                <div className="flex items-center gap-1.5 font-bold">
                  <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                  Report submitted — retention active
                </div>
                <p className="text-[11px]">
                  Submitted: {new Date(selectedStr.submissionDate || "").toLocaleString()}
                </p>
                <p className="text-[11px]">
                  Retained until: <strong>{selectedStr.retentionDeadline}</strong>
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
                Close
              </Button>
              {selectedStr?.status !== "SUBMITTED" && (
                <Button
                  type="submit"
                  size="sm"
                  disabled={isSubmitting}
                  className="gap-1.5"
                >
                  {isSubmitting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                  Submit report
                </Button>
              )}
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
