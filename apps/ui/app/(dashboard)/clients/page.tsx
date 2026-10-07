"use client"

import type React from "react"
import { useCallback, useEffect, useState } from "react"
import { Users, UserPlus, Search, AlertTriangle, RefreshCw, Loader2 } from "lucide-react"
import { SiteHeader } from "@/components/site-header"
import { ClientDetailSheet } from "@/components/client-detail-sheet"
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
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { api } from "@/lib/api-client"
import { type Client, type ClientRegime, type ClientRiskRating } from "@/lib/types"

/**
 * Load and search the first 100 matching customers, filter them by regime,
 * and provide profile creation and detail views. Load failures retain the
 * previous list; creation failures appear in the form.
 */
export default function ClientsPage() {
  const [clients, setClients] = useState<Client[]>([])
  const [searchQuery, setSearchQuery] = useState("")
  const [regimeFilter, setRegimeFilter] = useState<string>("ALL")
  const [isLoading, setIsLoading] = useState(false)
  const [selectedClient, setSelectedClient] = useState<Client | null>(null)
  const [isDetailOpen, setIsDetailOpen] = useState(false)

  // New customer dialog state.
  const [isNewClientOpen, setIsNewClientOpen] = useState(false)
  const [isCreating, setIsCreating] = useState(false)
  const [newClientName, setNewClientName] = useState("")
  const [newClientDoc, setNewClientDoc] = useState("")
  const [newClientNuib, setNewClientNuib] = useState("")
  const [newClientType] = useState<"INDIVIDUAL" | "CORPORATE">("INDIVIDUAL")
  const [newClientRegime, setNewClientRegime] = useState<ClientRegime>("SIMPLIFICADO")
  const [newClientRisk, setNewClientRisk] = useState<ClientRiskRating>("BAIXO")
  const [newClientIsPep, setNewClientIsPep] = useState(false)
  const [newClientPepCategory, setNewClientPepCategory] = useState("")
  const [modalError, setModalError] = useState<string | null>(null)

  const loadClients = useCallback(async () => {
    try {
      setIsLoading(true)
      const data = await api.getClients(0, 100, searchQuery)
      setClients(data)
    } catch (err) {
      console.error("Failed to load customers:", err)
    } finally {
      setIsLoading(false)
    }
  }, [searchQuery])

  useEffect(() => {
    queueMicrotask(() => {
      loadClients()
    })
  }, [loadClients])

  const handleCreateClient = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newClientName || !newClientDoc) {
      setModalError("Enter a name and document number.")
      return
    }

    try {
      setIsCreating(true)
      setModalError(null)
      await api.createClient({
        fullName: newClientName,
        documentNumber: newClientDoc,
        nuib: newClientNuib || undefined,
        clientType: newClientType,
        regime: newClientRegime,
        riskRating: newClientRisk,
        isPep: newClientIsPep,
        pepCategory: newClientIsPep ? newClientPepCategory : undefined,
      })

      // Reset the customer form.
      setNewClientName("")
      setNewClientDoc("")
      setNewClientNuib("")
      setIsNewClientOpen(false)
      loadClients()
    } catch (err: unknown) {
      setModalError(
        err instanceof Error ? err.message : "Could not create the customer profile."
      )
    } finally {
      setIsCreating(false)
    }
  }

  const openClientDetail = (client: Client) => {
    setSelectedClient(client)
    setIsDetailOpen(true)
  }

  const filteredClients = clients.filter((c) => {
    if (regimeFilter !== "ALL" && c.regime !== regimeFilter) return false
    return true
  })

  const formatMt = (val?: number) => {
    if (val === undefined || val === null) return "MZN 0"
    return new Intl.NumberFormat("en-MZ", {
      style: "currency",
      currency: "MZN",
      maximumFractionDigits: 0,
    }).format(val)
  }

  return (
    <div>
      <SiteHeader
        title="Customers"
        action={
          <Button
            size="sm"
            onClick={() => setIsNewClientOpen(true)}
            className="gap-1.5 text-xs shadow-sm"
          >
            <UserPlus className="h-3.5 w-3.5" />
            <span>Add customer</span>
          </Button>
        }
      />

      <div className="space-y-6">
        {/* Customer search and filters */}
        <div className="flex flex-col items-center justify-between gap-4 sm:flex-row">
          <div className="relative w-full sm:w-80">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Search by name, document or NUIB..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 text-xs"
            />
          </div>

          <div className="flex w-full items-center gap-3 sm:w-auto">
            <Select value={regimeFilter} onValueChange={setRegimeFilter}>
              <SelectTrigger className="h-9 w-[180px] text-xs">
                <SelectValue placeholder="Filter by due diligence" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All regimes</SelectItem>
                <SelectItem value="SIMPLIFICADO">Simplified</SelectItem>
                <SelectItem value="NORMAL">Standard</SelectItem>
              </SelectContent>
            </Select>

            <Button
              variant="outline"
              size="sm"
              onClick={loadClients}
              disabled={isLoading}
              className="h-9 gap-1.5 text-xs"
            >
              <RefreshCw className={`h-3 w-3 ${isLoading ? "animate-spin" : ""}`} />
              <span>Refresh</span>
            </Button>
          </div>
        </div>

        {/* Customer records */}
        <Card className="border-border shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base font-semibold">
              <Users className="h-4 w-4 text-primary" />
              Customers ({filteredClients.length})
            </CardTitle>
            <CardDescription className="text-xs">
              Select a customer to review their profile, risk rating and account details.
            </CardDescription>
          </CardHeader>
          <CardContent>
            {filteredClients.length === 0 ? (
              <div className="py-12 text-center text-xs text-muted-foreground">
                No customers match these filters.
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Customer</TableHead>
                    <TableHead>Document</TableHead>
                    <TableHead>Due diligence</TableHead>
                    <TableHead>Risk rating</TableHead>
                    <TableHead>PEP</TableHead>
                    <TableHead>Monthly volume</TableHead>
                    <TableHead className="text-right">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredClients.map((client) => (
                    <TableRow
                      key={client.id}
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => openClientDetail(client)}
                    >
                      <TableCell className="text-xs font-semibold">
                        {client.fullName}
                        <div className="text-[10px] font-normal text-muted-foreground">
                          {client.clientType === "INDIVIDUAL" ? "Individual" : "Business"}
                        </div>
                      </TableCell>
                      <TableCell className="font-mono text-xs">
                        {client.documentNumber}
                        {client.nuib && (
                          <div className="text-[10px] text-muted-foreground">
                            NUIB: {client.nuib}
                          </div>
                        )}
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            client.regime === "SIMPLIFICADO" ? "outline" : "secondary"
                          }
                          className="text-[10px]"
                        >
                          {client.regime}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            client.riskRating === "ALTO" ? "destructive" : "secondary"
                          }
                          className="text-[10px]"
                        >
                          {client.riskRating}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {client.isPep ? (
                          <Badge variant="warning" className="text-[10px]">
                            PEP ({client.pepCategory || "Nacional"})
                          </Badge>
                        ) : (
                          <span className="text-[11px] text-muted-foreground">—</span>
                        )}
                      </TableCell>
                      <TableCell className="text-xs font-semibold">
                        {formatMt(client.monthlyVolumeMt)}
                      </TableCell>
                      <TableCell
                        className="text-right"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openClientDetail(client)}
                          className="h-7 px-2.5 text-xs"
                        >
                          View
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Customer details panel */}
      <ClientDetailSheet
        client={selectedClient}
        isOpen={isDetailOpen}
        onClose={() => {
          setIsDetailOpen(false)
          setSelectedClient(null)
        }}
      />

      {/* New customer profile dialog */}
      <Dialog open={isNewClientOpen} onOpenChange={setIsNewClientOpen}>
        <DialogContent className="max-h-[85vh] max-w-lg overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <UserPlus className="h-5 w-5 text-primary" />
              Add customer
            </DialogTitle>
            <DialogDescription className="text-xs">
              Create a customer profile and run screening now.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleCreateClient} className="space-y-4 py-2 text-xs">
            {modalError && (
              <div className="flex items-center gap-1.5 rounded border border-red-500/30 bg-red-500/10 p-2.5 text-red-600">
                <AlertTriangle className="h-4 w-4 shrink-0" />
                <span>{modalError}</span>
              </div>
            )}

            <div className="space-y-1.5">
              <Label htmlFor="clientName" className="font-semibold">
                Full name / business name <span className="text-red-500">*</span>
              </Label>
              <Input
                id="clientName"
                value={newClientName}
                onChange={(e) => setNewClientName(e.target.value)}
                placeholder="e.g. Jordan Smith or Acme Cooperative"
                className="text-xs"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="clientDoc" className="font-semibold">
                  Document (ID / residence permit / NUIT){" "}
                  <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="clientDoc"
                  value={newClientDoc}
                  onChange={(e) => setNewClientDoc(e.target.value)}
                  placeholder="Ex: 110100234567M"
                  className="font-mono text-xs"
                  required
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="clientNuib" className="font-semibold">
                  NUIB (bank identifier)
                </Label>
                <Input
                  id="clientNuib"
                  value={newClientNuib}
                  onChange={(e) => setNewClientNuib(e.target.value)}
                  placeholder="Ex: 000100000000000000001"
                  className="font-mono text-xs"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label className="font-semibold">Due diligence</Label>
                <Select
                  value={newClientRegime}
                  onValueChange={(val: ClientRegime) => setNewClientRegime(val)}
                >
                  <SelectTrigger className="text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="SIMPLIFICADO">Simplificado</SelectItem>
                    <SelectItem value="NORMAL">Standard / enhanced</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label className="font-semibold">Initial risk rating</Label>
                <Select
                  value={newClientRisk}
                  onValueChange={(val: ClientRiskRating) => setNewClientRisk(val)}
                >
                  <SelectTrigger className="text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="BAIXO">Low</SelectItem>
                    <SelectItem value="MEDIO">Moderate</SelectItem>
                    <SelectItem value="ALTO">High</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Politically exposed person indicator */}
            <div className="space-y-2 rounded-lg border bg-muted/40 p-3">
              <label className="flex cursor-pointer items-center gap-2">
                <input
                  type="checkbox"
                  checked={newClientIsPep}
                  onChange={(e) => setNewClientIsPep(e.target.checked)}
                  className="h-4 w-4 rounded border-input text-primary focus-visible:ring-primary"
                />
                <span className="font-semibold text-foreground">
                  Politically exposed person
                </span>
              </label>

              {newClientIsPep && (
                <div className="space-y-1.5 pt-2">
                  <Label htmlFor="pepCategory" className="text-muted-foreground">
                    Public office / category
                  </Label>
                  <Input
                    id="pepCategory"
                    value={newClientPepCategory}
                    onChange={(e) => setNewClientPepCategory(e.target.value)}
                    placeholder="e.g. Minister, legislator, public company director"
                    className="text-xs"
                  />
                </div>
              )}
            </div>

            <DialogFooter className="gap-2 pt-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setIsNewClientOpen(false)}
                disabled={isCreating}
              >
                Cancel
              </Button>
              <Button type="submit" size="sm" disabled={isCreating} className="gap-1.5">
                {isCreating && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                Create and screen
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
