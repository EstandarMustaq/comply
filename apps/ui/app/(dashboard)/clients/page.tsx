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
      console.error("Erro ao carregar clientes:", err)
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
      setModalError("Nome e Documento são de preenchimento obrigatório.")
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
      setModalError(err instanceof Error ? err.message : "Erro ao cadastrar cliente.")
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
    if (val === undefined || val === null) return "0 MT"
    return new Intl.NumberFormat("pt-MZ", {
      style: "currency",
      currency: "MZN",
      maximumFractionDigits: 0,
    }).format(val)
  }

  return (
    <div>
      <SiteHeader
        title="Clientes"
        action={
          <Button
            size="sm"
            onClick={() => setIsNewClientOpen(true)}
            className="gap-1.5 text-xs shadow-sm"
          >
            <UserPlus className="h-3.5 w-3.5" />
            <span>Adicionar cliente</span>
          </Button>
        }
      />

      <div className="space-y-6">
        {/* Customer search and filters */}
        <div className="flex flex-col items-center justify-between gap-4 sm:flex-row">
          <div className="relative w-full sm:w-80">
            <Search className="text-muted-foreground absolute left-3 top-2.5 h-4 w-4" />
            <Input
              type="text"
              placeholder="Pesquisar por nome, documento ou NUIB..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 text-xs"
            />
          </div>

          <div className="flex w-full items-center gap-3 sm:w-auto">
            <Select value={regimeFilter} onValueChange={setRegimeFilter}>
              <SelectTrigger className="h-9 w-[180px] text-xs">
                <SelectValue placeholder="Filtrar por Regime" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Todos os Regimes</SelectItem>
                <SelectItem value="SIMPLIFICADO">Regime Simplificado</SelectItem>
                <SelectItem value="NORMAL">Regime Normal</SelectItem>
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
              <span>Atualizar</span>
            </Button>
          </div>
        </div>

        {/* Customer records */}
        <Card className="border-border shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base font-semibold">
              <Users className="text-primary h-4 w-4" />
              Clientes ({filteredClients.length})
            </CardTitle>
            <CardDescription className="text-xs">
              Clique em qualquer cliente para ver o perfil, nível de risco e detalhes de
              conta
            </CardDescription>
          </CardHeader>
          <CardContent>
            {filteredClients.length === 0 ? (
              <div className="text-muted-foreground py-12 text-center text-xs">
                Nenhum cliente corresponde aos filtros seleccionados.
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Cliente</TableHead>
                    <TableHead>Documento</TableHead>
                    <TableHead>Diligência</TableHead>
                    <TableHead>Nível de risco</TableHead>
                    <TableHead>PEP</TableHead>
                    <TableHead>Volume mensal</TableHead>
                    <TableHead className="text-right">Acção</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredClients.map((client) => (
                    <TableRow
                      key={client.id}
                      className="hover:bg-muted/50 cursor-pointer"
                      onClick={() => openClientDetail(client)}
                    >
                      <TableCell className="text-xs font-semibold">
                        {client.fullName}
                        <div className="text-muted-foreground text-[10px] font-normal">
                          {client.clientType === "INDIVIDUAL"
                            ? "Particular"
                            : "Pessoa colectiva"}
                        </div>
                      </TableCell>
                      <TableCell className="font-mono text-xs">
                        {client.documentNumber}
                        {client.nuib && (
                          <div className="text-muted-foreground text-[10px]">
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
                          <span className="text-muted-foreground text-[11px]">—</span>
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
                          Ver
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
              <UserPlus className="text-primary h-5 w-5" />
              Adicionar cliente
            </DialogTitle>
            <DialogDescription className="text-xs">
              Criar perfil de cliente e executar verificação imediata de triagem.
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
                Nome Completo / Denominação Social <span className="text-red-500">*</span>
              </Label>
              <Input
                id="clientName"
                value={newClientName}
                onChange={(e) => setNewClientName(e.target.value)}
                placeholder="Ex: Bernardo Silva ou Cooperativa Agrícola"
                className="text-xs"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="clientDoc" className="font-semibold">
                  Documento (BI / DIRE / NUIT) <span className="text-red-500">*</span>
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
                  NUIB (Identificador Bancário)
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
                <Label className="font-semibold">Regime de Diligência</Label>
                <Select
                  value={newClientRegime}
                  onValueChange={(val: ClientRegime) => setNewClientRegime(val)}
                >
                  <SelectTrigger className="text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="SIMPLIFICADO">Simplificado</SelectItem>
                    <SelectItem value="NORMAL">Normal / Reforçado</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label className="font-semibold">Classificação de Risco Inicial</Label>
                <Select
                  value={newClientRisk}
                  onValueChange={(val: ClientRiskRating) => setNewClientRisk(val)}
                >
                  <SelectTrigger className="text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="BAIXO">Baixo Risco</SelectItem>
                    <SelectItem value="MEDIO">Médio Risco</SelectItem>
                    <SelectItem value="ALTO">Alto Risco</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Politically exposed person indicator */}
            <div className="bg-muted/40 space-y-2 rounded-lg border p-3">
              <label className="flex cursor-pointer items-center gap-2">
                <input
                  type="checkbox"
                  checked={newClientIsPep}
                  onChange={(e) => setNewClientIsPep(e.target.checked)}
                  className="border-input text-primary focus:ring-primary h-4 w-4 rounded"
                />
                <span className="text-foreground font-semibold">
                  Pessoa com exposição política
                </span>
              </label>

              {newClientIsPep && (
                <div className="space-y-1.5 pt-2">
                  <Label htmlFor="pepCategory" className="text-muted-foreground">
                    Cargo / Função Pública / Categoria
                  </Label>
                  <Input
                    id="pepCategory"
                    value={newClientPepCategory}
                    onChange={(e) => setNewClientPepCategory(e.target.value)}
                    placeholder="Ex: Membro do Governo, Deputado, Dirigente de Empresa Pública"
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
                Cancelar
              </Button>
              <Button type="submit" size="sm" disabled={isCreating} className="gap-1.5">
                {isCreating && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                Registar e verificar
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
