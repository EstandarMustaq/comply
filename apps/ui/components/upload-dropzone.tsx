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
  { key: "clientDocument", label: "Documento / NUIB / NUIT", required: true },
  { key: "clientName", label: "Nome do Cliente", required: true },
  { key: "accountNumber", label: "Número de Conta", required: true },
  { key: "transactionType", label: "Tipo (DEPÓSITO, LEVANTAMENTO...)", required: true },
  { key: "amount", label: "Montante / Valor (MT)", required: true },
  { key: "transactionDate", label: "Data da Transação", required: true },
  { key: "channel", label: "Canal (BALCÃO, ATM, POS...)", required: false },
  { key: "counterpartyName", label: "Contraparte (Nome)", required: false },
  { key: "counterpartyDocument", label: "Contraparte (Documento)", required: false },
]

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
      setError("Apenas ficheiros nos formatos CSV e XLSX são suportados.")
      return
    }

    try {
      setIsUploading(true)
      const batch = await api.uploadBatch(file)
      setCurrentBatch(batch)

      // Pre-fill heuristic mapping
      if (batch.columnMapping) {
        setColumnMapping(batch.columnMapping)
      }
      setIsMappingModalOpen(true)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Não foi possível enviar o ficheiro.")
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
        `Importação concluída — ${result.processedRows || result.totalRows || 0} transacções processadas e verificadas.`
      )
      if (onSuccess) onSuccess()
    } catch (err: unknown) {
      setError(
        err instanceof Error
          ? err.message
          : "Não foi possível processar as transacções do lote."
      )
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
          <div className="bg-primary/10 text-primary flex h-14 w-14 items-center justify-center rounded-full">
            {isUploading ? (
              <Loader2 className="h-7 w-7 animate-spin" />
            ) : (
              <UploadCloud className="h-7 w-7" />
            )}
          </div>

          <div className="space-y-1">
            <h3 className="text-base font-semibold">
              {isUploading
                ? "A carregar e a analisar cabeçalhos..."
                : "Clique ou arraste a folha de cálculo aqui"}
            </h3>
            <p className="text-muted-foreground max-w-md text-xs">
              Compatível com extratos operacionais de instituições de microfinança e
              microcrédito (CSV ou XLSX). Detetação automática de delimitadores, encodings
              e linhas de cabeçalho.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <Badge variant="secondary" className="text-xs">
              <FileSpreadsheet className="mr-1 h-3 w-3" /> Ficheiros CSV (.csv)
            </Badge>
            <Badge variant="secondary" className="text-xs">
              <FileSpreadsheet className="mr-1 h-3 w-3" /> Folhas de cálculo (.xlsx)
            </Badge>
          </div>
        </CardContent>
      </Card>

      {/* Error alert */}
      {error && (
        <div className="border-destructive/40 text-destructive flex items-center gap-2 rounded-md border p-3 text-xs">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Success alert */}
      {successResult && (
        <div className="flex items-center gap-2 rounded-md border border-emerald-500/30 bg-emerald-500/10 p-3 text-xs text-emerald-700">
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>{successResult}</span>
        </div>
      )}

      {/* Confirm the selected column mapping before import. */}
      <Dialog open={isMappingModalOpen} onOpenChange={setIsMappingModalOpen}>
        <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <FileSpreadsheet className="text-primary h-5 w-5" />
              Confirmação de Mapeamento de Colunas
            </DialogTitle>
            <DialogDescription className="text-xs">
              Arquivo: <strong>{currentBatch?.fileName}</strong>. Confirme a
              correspondência dos cabeçalhos do extrato com os campos de conformidade do
              Complyz.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-3">
            <div className="text-muted-foreground grid grid-cols-2 gap-3 border-b pb-2 text-xs font-semibold uppercase">
              <div>Campo do Complyz</div>
              <div>Coluna identificada</div>
            </div>

            {REQUIRED_TARGET_FIELDS.map((target) => {
              const currentVal = columnMapping[target.key] || ""
              return (
                <div
                  key={target.key}
                  className="grid grid-cols-2 items-center gap-3 py-1 text-sm"
                >
                  <div>
                    <span className="text-foreground font-medium">{target.label}</span>
                    {target.required && <span className="text-destructive ml-1">*</span>}
                  </div>
                  <div>
                    <Select
                      value={currentVal}
                      onValueChange={(val) =>
                        setColumnMapping((prev) => ({ ...prev, [target.key]: val }))
                      }
                    >
                      <SelectTrigger className="h-8 text-xs">
                        <SelectValue placeholder="Selecione a coluna correspondente" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="NOT_MAPPED">-- Não mapear --</SelectItem>
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
              Cancelar
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
                  <span>A processar regras e sanções...</span>
                </>
              ) : (
                <>
                  <span>Confirmar e Processar Lote</span>
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
