"use client";

import React, { useEffect, useState } from "react";
import { FileSpreadsheet, RefreshCw, CheckCircle2, AlertCircle, Clock } from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { UploadDropzone } from "@/components/upload-dropzone";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api-client";
import { ImportBatch } from "@/lib/types";

export default function ImportPage() {
  const [batches, setBatches] = useState<ImportBatch[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const loadBatches = async () => {
    try {
      setIsLoading(true);
      const data = await api.getBatches();
      setBatches(data);
    } catch (err) {
      console.error("Erro ao carregar lotes de importação:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadBatches();
  }, []);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "COMPLETED":
        return <Badge variant="success" className="text-[10px]">Concluído</Badge>;
      case "PROCESSING":
        return <Badge variant="warning" className="text-[10px]">A Processar</Badge>;
      case "FAILED":
        return <Badge variant="destructive" className="text-[10px]">Falhou</Badge>;
      default:
        return <Badge variant="secondary" className="text-[10px]">{status}</Badge>;
    }
  };

  return (
    <div className="flex-1 flex flex-col">
      <SiteHeader
        title="Ingestão de Extratos e Planilhas"
        subtitle="Processamento automático de transações com triagem imediata de sanções e regras de fracionamento"
      />

      <main className="flex-1 p-6 space-y-6 overflow-y-auto max-w-5xl mx-auto w-full">
        {/* Dropzone Container */}
        <UploadDropzone onSuccess={loadBatches} />

        {/* History of Import Batches */}
        <Card className="shadow-sm">
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="text-base font-semibold flex items-center gap-2">
                <FileSpreadsheet className="h-4 w-4 text-primary" />
                Histórico de Lotes Importados
              </CardTitle>
              <CardDescription className="text-xs">
                Auditoria de ficheiros processados pelo OpenCSV e Apache POI
              </CardDescription>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={loadBatches}
              disabled={isLoading}
              className="text-xs gap-1.5 h-8"
            >
              <RefreshCw className={`h-3 w-3 ${isLoading ? "animate-spin" : ""}`} />
              <span>Atualizar Histórico</span>
            </Button>
          </CardHeader>
          <CardContent>
            {batches.length === 0 ? (
              <div className="text-center py-8 text-xs text-muted-foreground">
                Nenhum lote importado ainda. Arraste uma folha de cálculo acima para iniciar.
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Nome do Ficheiro</TableHead>
                    <TableHead>Formato</TableHead>
                    <TableHead>Estado</TableHead>
                    <TableHead className="text-center">Total Linhas</TableHead>
                    <TableHead className="text-center">Processadas</TableHead>
                    <TableHead className="text-center">Erros</TableHead>
                    <TableHead className="text-right">Data de Envio</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {batches.map((b) => (
                    <TableRow key={b.id}>
                      <TableCell className="font-medium text-xs">
                        {b.fileName}
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline" className="text-[10px]">
                          {b.fileType}
                        </Badge>
                      </TableCell>
                      <TableCell>{getStatusBadge(b.status)}</TableCell>
                      <TableCell className="text-center text-xs">{b.totalRows}</TableCell>
                      <TableCell className="text-center text-xs font-semibold text-emerald-600">
                        {b.processedRows}
                      </TableCell>
                      <TableCell className="text-center text-xs font-semibold text-red-600">
                        {b.errorRows}
                      </TableCell>
                      <TableCell className="text-right text-xs text-muted-foreground">
                        {new Date(b.createdAt).toLocaleString("pt-MZ")}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  );
}
