"use client"

import type React from "react"
import { useState, useRef } from "react"
import {
  UploadCloud,
  FileSpreadsheet,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  Loader2,
} from "lucide-react"
import { Button } from "./ui/button"
import { Card, CardContent } from "./ui/card"
import { Badge } from "./ui/badge"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "./ui/dialog"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "./ui/select"
import { api } from "@/lib/api-client"
import { type ImportBatch } from "@/lib/types"

interface UploadDropzoneProps {
  onSuccess?: () => void
}

const REQUIRED_TARGET_FIELDS = [
  { key: "clientDocument", label: "Document / NUIB / NUIT", required: true },
  { key: "clientName", label: "Customer name", required: true },
  { key: "accountNumber", label: "Account number", required: true },
  { key: "transactionType", label: "Type (DEPOSIT, WITHDRAWAL...)", required: true },
  { key: "amount", label: "Amount (MZN)", required: true },
  { key: "transactionDate", label: "Transaction date", required: true },
  { key: "channel", label: "Channel (BRANCH, ATM, POS...)", required: false },
  { key: "counterpartyName", label: "Counterparty name", required: false },
  { key: "counterpartyDocument", label: "Counterparty document", required: false },
]

/**
 * Upload the first selected or dropped CSV/XLSX file and ask for column mapping.
 * Invoke onSuccess after the mapping confirmation request succeeds, without
 * waiting for any promise it returns. Upload/confirmation failures and synchronous
 * onSuccess errors appear inline; other filename extensions are rejected locally.
 */
export function UploadDropzone({ onSuccess }: UploadDropzoneProps) {
  const [isDragging, setIsDragging] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [isProcessing, setIsProcessing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [currentBatch, setCurrentBatch] = useState<ImportBatch | null>(null)
  const [isMappingModalOpen, setIsMappingModalOpen] = useState(false)
  const [columnMapping, setColumnMapping] = useState<Record<string, string>>({})
  const [successResult, setSuccessResult] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }

  const handleDragLeave = () => {
    setIsDragging(false)
  }

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
    const dropped = e.dataTransfer.files?.[0]
    if (dropped) {
      await uploadFile(dropped)
    }
  }

  const handleFileInput = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0]
    if (selected) {
      await uploadFile(selected)
    }
  }

  const uploadFile = async (file: File) => {
    setError(null)
    setSuccessResult(null)

    const ext = file.name.split(".").pop()?.toLowerCase()
    if (ext !== "csv" && ext !== "xlsx") {
      setError("Upload a CSV or XLSX file.")
      return
    }

    try {
      setIsUploading(true)
      const batch = await api.uploadBatch(file)
      setCurrentBatch(batch)

      if (batch.columnMapping) {
        setColumnMapping(batch.columnMapping)
      }
      setIsMappingModalOpen(true)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Could not upload the file.")
    } finally {
      setIsUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ""
    }
  }

  const handleConfirmMapping = async () => {
    if (!currentBatch) return
    try {
      setIsProcessing(true)
      setError(null)
      const result = await api.confirmMapping(currentBatch.id, columnMapping)
      setIsMappingModalOpen(false)
      setSuccessResult(
        `Import complete — ${result.processedRows || result.totalRows || 0} transactions processed and screened.`
      )
      if (onSuccess) onSuccess()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Could not process this import.")
    } finally {
      setIsProcessing(false)
    }
  }

  return (
    <div className="space-y-4">
      <Card
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={`cursor-pointer border-2 border-dashed transition-all ${
          isDragging
            ? "border-primary bg-primary/5"
            : "border-muted-foreground/25 hover:border-primary/50"
        }`}
        onClick={() => fileInputRef.current?.click()}
      >
        <CardContent className="flex flex-col items-center justify-center space-y-4 p-8 text-center">
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv,.xlsx"
            className="hidden"
            onChange={handleFileInput}
          />
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-primary/10 text-primary">
            {isUploading ? (
              <Loader2 className="h-7 w-7 animate-spin" />
            ) : (
              <UploadCloud className="h-7 w-7" />
            )}
          </div>

          <div className="space-y-1">
            <h3 className="text-base font-semibold">
              {isUploading
                ? "Uploading and reading headers..."
                : "Choose or drop a spreadsheet here"}
            </h3>
            <p className="max-w-md text-xs text-muted-foreground">
              Upload operational statements as CSV or XLSX. Comply detects delimiters,
              encoding and header rows.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <Badge variant="secondary" className="text-xs">
              <FileSpreadsheet className="mr-1 h-3 w-3" /> CSV files (.csv)
            </Badge>
            <Badge variant="secondary" className="text-xs">
              <FileSpreadsheet className="mr-1 h-3 w-3" /> Spreadsheets (.xlsx)
            </Badge>
          </div>
        </CardContent>
      </Card>

      {error && (
        <div className="flex items-center gap-2 rounded-md border border-destructive/40 p-3 text-xs text-destructive">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {successResult && (
        <div className="flex items-center gap-2 rounded-md border border-emerald-500/30 bg-emerald-500/10 p-3 text-xs text-emerald-700">
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>{successResult}</span>
        </div>
      )}

      <Dialog open={isMappingModalOpen} onOpenChange={setIsMappingModalOpen}>
        <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <FileSpreadsheet className="h-5 w-5 text-primary" />
              Confirm column mapping
            </DialogTitle>
            <DialogDescription className="text-xs">
              File: <strong>{currentBatch?.fileName}</strong>. Match each statement header
              to a Comply field.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-3">
            <div className="grid grid-cols-2 gap-3 border-b pb-2 text-xs font-semibold uppercase text-muted-foreground">
              <div>Comply field</div>
              <div>Detected column</div>
            </div>

            {REQUIRED_TARGET_FIELDS.map((target) => {
              const currentVal = columnMapping[target.key] || ""
              return (
                <div
                  key={target.key}
                  className="grid grid-cols-2 items-center gap-3 py-1 text-sm"
                >
                  <div>
                    <span className="font-medium text-foreground">{target.label}</span>
                    {target.required && <span className="ml-1 text-destructive">*</span>}
                  </div>
                  <div>
                    <Select
                      value={currentVal}
                      onValueChange={(val) =>
                        setColumnMapping((prev) => ({ ...prev, [target.key]: val }))
                      }
                    >
                      <SelectTrigger className="h-8 text-xs">
                        <SelectValue placeholder="Select a matching column" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="NOT_MAPPED">-- Do not map --</SelectItem>
                        {currentBatch?.detectedHeaders?.map((header) => (
                          <SelectItem key={header} value={header}>
                            {header}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
              )
            })}
          </div>

          <DialogFooter className="gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsMappingModalOpen(false)}
              disabled={isProcessing}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={handleConfirmMapping}
              disabled={isProcessing}
              className="gap-1.5"
            >
              {isProcessing ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Screening transactions...</span>
                </>
              ) : (
                <>
                  <span>Confirm and process import</span>
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
