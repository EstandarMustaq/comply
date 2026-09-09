"use client";

import React, { useState, useRef } from "react";
import { UploadCloud, FileSpreadsheet, CheckCircle2, AlertCircle, ArrowRight, Loader2 } from "lucide-react";
import { Button } from "./ui/button";
import { Card, CardContent } from "./ui/card";
import { Badge } from "./ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "./ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "./ui/select";
import { api } from "@/lib/api-client";
import { ImportBatch } from "@/lib/types";

interface UploadDropzoneProps {
  onSuccess?: () => void;
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
];

export function UploadDropzone({ onSuccess }: UploadDropzoneProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentBatch, setCurrentBatch] = useState<ImportBatch | null>(null);
  const [isMappingModalOpen, setIsMappingModalOpen] = useState(false);
  const [columnMapping, setColumnMapping] = useState<Record<string, string>>({});
  const [successResult, setSuccessResult] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      await uploadFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileInput = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      await uploadFile(e.target.files[0]);
    }
  };

  const uploadFile = async (file: File) => {
    setError(null);
    setSuccessResult(null);

    const ext = file.name.split(".").pop()?.toLowerCase();
    if (ext !== "csv" && ext !== "xlsx") {
      setError("Apenas ficheiros nos formatos CSV e XLSX são suportados.");
      return;
    }

    try {
      setIsUploading(true);
      const batch = await api.uploadBatch(file);
      setCurrentBatch(batch);

      // Pre-fill heuristic mapping
      if (batch.columnMapping) {
        setColumnMapping(batch.columnMapping);
      }
      setIsMappingModalOpen(true);
    } catch (err: any) {
      setError(err.message || "Falha ao enviar o arquivo de extrato.");
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleConfirmMapping = async () => {
    if (!currentBatch) return;
    try {
      setIsProcessing(true);
      setError(null);
      const result = await api.confirmMapping(currentBatch.id, columnMapping);
      setIsMappingModalOpen(false);
      setSuccessResult(
        `Import complete — ${result.processedRows || result.totalRows || 0} transactions processed and screened.`
      );
      if (onSuccess) onSuccess();
    } catch (err: any) {
      setError(err.message || "Erro ao processar as transações do lote.");
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="space-y-4">
      <Card
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={`border-2 border-dashed transition-all cursor-pointer ${
          isDragging
            ? "border-primary bg-primary/5"
            : "border-muted-foreground/25 hover:border-primary/50"
        }`}
        onClick={() => fileInputRef.current?.click()}
      >
        <CardContent className="flex flex-col items-center justify-center p-8 text-center space-y-4">
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv,.xlsx"
            className="hidden"
            onChange={handleFileInput}
          />
          <div className="h-14 w-14 rounded-full bg-primary/10 flex items-center justify-center text-primary">
            {isUploading ? (
              <Loader2 className="h-7 w-7 animate-spin" />
            ) : (
              <UploadCloud className="h-7 w-7" />
            )}
          </div>

          <div className="space-y-1">
            <h3 className="font-semibold text-base">
              {isUploading
                ? "A carregar e a analisar cabeçalhos..."
                : "Clique ou arraste a folha de cálculo aqui"}
            </h3>
            <p className="text-xs text-muted-foreground max-w-md">
              Compatível com extratos operacionais de instituições de microfinança e microcrédito (CSV ou XLSX). Detetação automática de delimitadores, encodings e linhas de cabeçalho.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <Badge variant="secondary" className="text-xs">
              <FileSpreadsheet className="h-3 w-3 mr-1" /> OpenCSV (UTF-8, ISO-8859-1)
            </Badge>
            <Badge variant="secondary" className="text-xs">
              <FileSpreadsheet className="h-3 w-3 mr-1" /> Apache POI (Excel .xlsx)
            </Badge>
          </div>
        </CardContent>
      </Card>

      {/* Error alert */}
      {error && (
        <div className="p-3 bg-red-500/10 border border-red-500/30 rounded-md flex items-center gap-2 text-xs text-red-600">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Success alert */}
      {successResult && (
        <div className="p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-md flex items-center gap-2 text-xs text-emerald-700">
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>{successResult}</span>
        </div>
      )}

      {/* Mapping Confirmation Dialog */}
      <Dialog open={isMappingModalOpen} onOpenChange={setIsMappingModalOpen}>
        <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <FileSpreadsheet className="h-5 w-5 text-primary" />
              Confirmação de Mapeamento de Colunas
            </DialogTitle>
            <DialogDescription className="text-xs">
              Arquivo: <strong>{currentBatch?.fileName}</strong>. Confirme a correspondência dos cabeçalhos do extrato com os campos de conformidade do FinComp.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-3">
            <div className="grid grid-cols-2 gap-3 text-xs font-semibold text-muted-foreground uppercase border-b pb-2">
              <div>FinComp field</div>
              <div>Detected column</div>
            </div>

            {REQUIRED_TARGET_FIELDS.map((target) => {
              const currentVal = columnMapping[target.key] || "";
              return (
                <div key={target.key} className="grid grid-cols-2 gap-3 items-center text-sm py-1">
                  <div>
                    <span className="font-medium text-foreground">{target.label}</span>
                    {target.required && <span className="text-red-500 ml-1">*</span>}
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
              );
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
  );
}
