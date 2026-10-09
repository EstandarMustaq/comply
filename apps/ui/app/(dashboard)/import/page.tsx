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

/**
 * Show spreadsheet upload and import history, refreshing history on mount,
 * on request, and after mapping confirmation. Failed loads retain prior history.
 */
export default function ImportPage() {
  const [batches, setBatches] = useState<ImportBatch[]>([])
  const [isLoading, setIsLoading] = useState(false)

  const loadBatches = useCallback(async () => {
    try {
      setIsLoading(true)
      const data = await api.getBatches()
      setBatches(data)
    } catch (err) {
      console.error("Failed to load import batches:", err)
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
            Complete
          </Badge>
        )
      case "PROCESSING":
        return (
          <Badge variant="warning" className="text-[10px]">
            Processing
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
      <SiteHeader title="Import transactions" />

      <div className="space-y-6">
        <UploadDropzone onSuccess={loadBatches} />

        <Card className="shadow-sm">
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="flex items-center gap-2 text-base font-semibold">
                <FileSpreadsheet className="h-4 w-4 text-primary" />
                Import history
              </CardTitle>
              <CardDescription className="text-xs">
                Track uploaded files and their processing status.
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
              <span>Refresh history</span>
            </Button>
          </CardHeader>
          <CardContent>
            {batches.length === 0 ? (
              <div className="py-8 text-center text-xs text-muted-foreground">
                No imports yet. Drop a spreadsheet above to get started.
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>File name</TableHead>
                    <TableHead>Format</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-center">Total rows</TableHead>
                    <TableHead className="text-center">Processed</TableHead>
                    <TableHead className="text-center">Errors</TableHead>
                    <TableHead className="text-right">Uploaded</TableHead>
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
                      <TableCell className="text-right text-xs text-muted-foreground">
                        {new Date(b.createdAt).toLocaleString("en-MZ")}
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
