"use client"

import { useCallback, useEffect, useState } from "react"
import { FileSpreadsheet, RefreshCw } from "lucide-react"
import { SiteHeader } from "@/components/site-header"
import { UploadDropzone } from "@/components/upload-dropzone"
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
import { api } from "@/lib/api-client"
import { type ImportBatch } from "@/lib/types"
import { getImportStatusLabel } from "@/lib/labels"

export default function ImportPage() {
  const [batches, setBatches] = useState<ImportBatch[]>([])
  const [isLoading, setIsLoading] = useState(false)

  const loadBatches = useCallback(async () => {
    try {
      setIsLoading(true)
      const data = await api.getBatches()
      setBatches(data)
    } catch (err) {
      console.error("Erro ao carregar lotes de importação:", err)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    queueMicrotask(() => {
      loadBatches()
    })
  }, [loadBatches])

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "COMPLETED":
        return (
          <Badge variant="success" className="text-[10px]">
            Concluído
          </Badge>
        )
      case "PROCESSING":
        return (
          <Badge variant="warning" className="text-[10px]">
            A Processar
          </Badge>
        )
      case "FAILED":
        return (
          <Badge variant="destructive" className="text-[10px]">
            Falhou
          </Badge>
        )
      default:
        return (
          <Badge variant="secondary" className="text-[10px]">
            {getImportStatusLabel(status)}
          </Badge>
        )
    }
  }

  return (
    <div>
      <SiteHeader title="Importar transacções" />

      <div className="space-y-6">
        {/* File upload area */}
        <UploadDropzone onSuccess={loadBatches} />

        {/* Recent imports */}
        <Card className="shadow-sm">
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="flex items-center gap-2 text-base font-semibold">
                <FileSpreadsheet className="text-primary h-4 w-4" />
                Histórico de Lotes Importados
              </CardTitle>
              <CardDescription className="text-xs">
                Histórico e estado de processamento dos ficheiros carregados.
              </CardDescription>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={loadBatches}
              disabled={isLoading}
              className="h-8 gap-1.5 text-xs"
            >
              <RefreshCw className={`h-3 w-3 ${isLoading ? "animate-spin" : ""}`} />
              <span>Atualizar Histórico</span>
            </Button>
          </CardHeader>
          <CardContent>
            {batches.length === 0 ? (
              <div className="text-muted-foreground py-8 text-center text-xs">
                Nenhum lote importado ainda. Arraste uma folha de cálculo acima para
                iniciar.
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
                      <TableCell className="text-xs font-medium">{b.fileName}</TableCell>
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
                      <TableCell className="text-muted-foreground text-right text-xs">
                        {new Date(b.createdAt).toLocaleString("pt-MZ")}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
