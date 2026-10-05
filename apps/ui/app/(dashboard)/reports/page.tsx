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
import { getReportStatusLabel } from "@/lib/labels"

function ReportsContent() {
  const searchParams = useSearchParams()
  const highlightStrId = searchParams.get("strId")

  const [activeTab, setActiveTab] = useState<string>("STR")
  const [strs, setStrs] = useState<SuspiciousActivityReport[]>([])
  const [ctrs, setCtrs] = useState<CurrencyTransactionReport[]>([])
  const [isLoading, setIsLoading] = useState(false)

  // Report submission dialog state.
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
    setActionTaken(report.actionTaken || "Transaction held for formal review.")
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
      console.error("Failed to load reports:", err)
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
      setSubmitError("Enter the reason, transaction details and protocol number.")
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
      setSubmitError(err instanceof Error ? err.message : "Could not submit the report.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const formatMt = (val?: number) => {
    if (val === undefined || val === null) return "0 MT"
    return new Intl.NumberFormat("en-MZ", {
      style: "currency",
      currency: "MZN",
      maximumFractionDigits: 0,
    }).format(val)
  }

  return (
    <div>
      <SiteHeader
        title="Reports"
        action={
          <Button
            variant="outline"
            size="sm"
            onClick={loadData}
            disabled={isLoading}
            className="h-8 gap-1.5 text-xs"
          >
            <RefreshCw className={`h-3 w-3 ${isLoading ? "animate-spin" : ""}`} />
            <span>Refresh</span>
          </Button>
        }
      />

      <div className="space-y-6">
        {/* Report retention details */}
        <div className="border-border bg-muted/40 text-foreground flex items-start gap-3 rounded-lg border p-3.5 text-xs">
          <Clock className="text-primary mt-0.5 h-4 w-4 shrink-0" />
          <div>
            <span className="font-semibold">Record retention</span>
            <p className="text-muted-foreground mt-0.5 text-[11px]">
              Submitted reports, evidence and due diligence records are locked against
              deletion for <strong>10 years</strong>.
            </p>
          </div>
        </div>

        {/* Report category tabs */}
        <div className="flex items-center justify-between">
          <Tabs
            value={activeTab}
            onValueChange={setActiveTab}
            className="w-full max-w-md"
          >
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="STR" className="text-xs">
                Suspicious activity
              </TabsTrigger>
              <TabsTrigger value="CTR" className="text-xs">
                Cash transactions
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </div>

        {activeTab === "STR" ? (
          /* Investigation reports */
          <Card className="border-border shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base font-semibold">
                <FileText className="text-primary h-4 w-4" />
                Suspicious activity reports ({strs.length})
              </CardTitle>
              <CardDescription className="text-xs">
                Formal reports for transactions with indicators of suspicious activity.
              </CardDescription>
            </CardHeader>
            <CardContent>
              {strs.length === 0 ? (
                <div className="text-muted-foreground py-12 text-center text-xs">
                  No suspicious activity reports.
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Reference</TableHead>
                      <TableHead>Customer</TableHead>
                      <TableHead>Filing reference</TableHead>
                      <TableHead>Retain until</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="text-right">Action</TableHead>
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
                            <div className="text-muted-foreground font-mono text-[10px]">
                              {report.client?.documentNumber}
                            </div>
                          </TableCell>
                          <TableCell className="font-mono text-xs">
                            {report.gifimProtocolNumber ? (
                              <span className="font-semibold text-emerald-700">
                                {report.gifimProtocolNumber}
                              </span>
                            ) : (
                              <span className="text-muted-foreground italic">
                                Pending
                              </span>
                            )}
                          </TableCell>
                          <TableCell className="text-muted-foreground text-xs">
                            {report.retentionDeadline ? (
                              <span className="text-foreground font-medium">
                                {new Date(report.retentionDeadline).toLocaleDateString(
                                  "en-MZ"
                                )}
                              </span>
                            ) : (
                              "Set after filing"
                            )}
                          </TableCell>
                          <TableCell>
                            <Badge
                              variant={isSubmitted ? "success" : "warning"}
                              className="text-[10px]"
                            >
                              {isSubmitted ? "FILED" : "DRAFT"}
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
                                <span>Complete and file</span>
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
          /* Cash transaction reports */
          <Card className="border-border shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base font-semibold">
                <FileText className="text-primary h-4 w-4" />
                Cash transaction reports ({ctrs.length})
              </CardTitle>
              <CardDescription className="text-xs">
                Periodic reports for cash transactions that meet the reporting threshold.
              </CardDescription>
            </CardHeader>
            <CardContent>
              {ctrs.length === 0 ? (
                <div className="text-muted-foreground py-12 text-center text-xs">
                  No pending cash transaction reports.
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Reference</TableHead>
                      <TableHead>Customer</TableHead>
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
                          <div className="text-muted-foreground text-[10px]">
                            {ctr.client?.documentNumber}
                          </div>
                        </TableCell>
                        <TableCell className="text-xs">
                          {ctr.reportingMonth}/{ctr.reportingYear}
                        </TableCell>
                        <TableCell className="text-foreground text-xs font-bold">
                          {formatMt(ctr.aggregateAmount)}
                        </TableCell>
                        <TableCell className="text-center text-xs">
                          {ctr.transactionCount}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-[10px]">
                            {getReportStatusLabel(ctr.status)}
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

      {/* Report submission and details dialog */}
      <Dialog open={isSubmitModalOpen} onOpenChange={setIsSubmitModalOpen}>
        <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Building2 className="text-primary h-5 w-5" />
              {selectedStr?.status === "SUBMITTED"
                ? "Filing record"
                : "File suspicious activity report"}
            </DialogTitle>
            <DialogDescription className="text-xs">
              Reference: <strong>{selectedStr?.strReference}</strong> · Customer:{" "}
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
                Filing reference <span className="text-red-500">*</span>
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
                Reason for suspicion <span className="text-red-500">*</span>
              </Label>
              <textarea
                id="suspicionGrounds"
                rows={3}
                value={suspicionGrounds}
                onChange={(e) => setSuspicionGrounds(e.target.value)}
                placeholder="Describe the indicators, how they differ from the customer's profile, or any sanctions matches…"
                className="border-input bg-background focus-visible:ring-ring w-full rounded-md border p-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1"
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
                placeholder="Include accounts, amounts, dates, channels and counterparties…"
                className="border-input bg-background focus-visible:ring-ring w-full rounded-md border p-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1"
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
                placeholder="Account statements, deposit receipts, screening results…"
                className="border-input bg-background focus-visible:ring-ring w-full rounded-md border p-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1"
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
              <div className="space-y-1 rounded-lg border border-emerald-500/30 bg-emerald-500/10 p-3 text-emerald-900">
                <div className="flex items-center gap-1.5 font-bold">
                  <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                  Report filed — retention period is active
                </div>
                <p className="text-[11px]">
                  Filed on:{" "}
                  {new Date(selectedStr.submissionDate || "").toLocaleString("en-MZ")}
                </p>
                <p className="text-[11px]">
                  Retain until: <strong>{selectedStr.retentionDeadline}</strong>
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
                  File report
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
          <Loader2 className="text-muted-foreground h-6 w-6 animate-spin" />
        </div>
      }
    >
      <ReportsContent />
    </Suspense>
  )
}
