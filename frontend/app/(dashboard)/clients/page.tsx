"use client";

import React, { useEffect, useState } from "react";
import {
  Users,
  UserPlus,
  Search,
  Filter,
  ShieldCheck,
  AlertTriangle,
  FileText,
  DollarSign,
  RefreshCw,
  Loader2,
} from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { ClientDetailSheet } from "@/components/client-detail-sheet";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { api } from "@/lib/api-client";
import { Client, ClientRegime, ClientRiskRating } from "@/lib/types";

export default function ClientsPage() {
  const [clients, setClients] = useState<Client[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [regimeFilter, setRegimeFilter] = useState<string>("ALL");
  const [isLoading, setIsLoading] = useState(false);
  const [selectedClient, setSelectedClient] = useState<Client | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);

  // New Client Modal States
  const [isNewClientOpen, setIsNewClientOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [newClientName, setNewClientName] = useState("");
  const [newClientDoc, setNewClientDoc] = useState("");
  const [newClientNuib, setNewClientNuib] = useState("");
  const [newClientType, setNewClientType] = useState<"INDIVIDUAL" | "CORPORATE">("INDIVIDUAL");
  const [newClientRegime, setNewClientRegime] = useState<ClientRegime>("SIMPLIFICADO");
  const [newClientRisk, setNewClientRisk] = useState<ClientRiskRating>("BAIXO");
  const [newClientIsPep, setNewClientIsPep] = useState(false);
  const [newClientPepCategory, setNewClientPepCategory] = useState("");
  const [modalError, setModalError] = useState<string | null>(null);

  const loadClients = async () => {
    try {
      setIsLoading(true);
      const data = await api.getClients(0, 100, searchQuery);
      setClients(data);
    } catch (err) {
      console.error("Erro ao carregar clientes:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadClients();
  }, [searchQuery]);

  const handleCreateClient = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newClientName || !newClientDoc) {
      setModalError("Nome e Documento são de preenchimento obrigatório.");
      return;
    }

    try {
      setIsCreating(true);
      setModalError(null);
      await api.createClient({
        fullName: newClientName,
        documentNumber: newClientDoc,
        nuib: newClientNuib || undefined,
        clientType: newClientType,
        regime: newClientRegime,
        riskRating: newClientRisk,
        isPep: newClientIsPep,
        pepCategory: newClientIsPep ? newClientPepCategory : undefined,
      });

      // Reset form
      setNewClientName("");
      setNewClientDoc("");
      setNewClientNuib("");
      setIsNewClientOpen(false);
      loadClients();
    } catch (err: any) {
      setModalError(err.message || "Erro ao cadastrar cliente.");
    } finally {
      setIsCreating(false);
    }
  };

  const openClientDetail = (client: Client) => {
    setSelectedClient(client);
    setIsDetailOpen(true);
  };

  const filteredClients = clients.filter((c) => {
    if (regimeFilter !== "ALL" && c.regime !== regimeFilter) return false;
    return true;
  });

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
        title="Clients"
        action={
          <Button
            size="sm"
            onClick={() => setIsNewClientOpen(true)}
            className="text-xs gap-1.5 shadow-sm"
          >
            <UserPlus className="h-3.5 w-3.5" />
            <span>Add client</span>
          </Button>
        }
      />

      <main className="flex-1 p-6 space-y-6 overflow-y-auto">
        {/* Search & Filters */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="relative w-full sm:w-80">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Pesquisar por nome, documento ou NUIB..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 text-xs"
            />
          </div>

          <div className="flex items-center gap-3 w-full sm:w-auto">
            <Select value={regimeFilter} onValueChange={setRegimeFilter}>
              <SelectTrigger className="w-[180px] h-9 text-xs">
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
              className="text-xs gap-1.5 h-9"
            >
              <RefreshCw className={`h-3 w-3 ${isLoading ? "animate-spin" : ""}`} />
              <span>Atualizar</span>
            </Button>
          </div>
        </div>

        {/* Clients Table Card */}
        <Card className="shadow-sm border-border/70">
          <CardHeader className="pb-3">
            <CardTitle className="text-base font-semibold flex items-center gap-2">
              <Users className="h-4 w-4 text-primary" />
              Clients ({filteredClients.length})
            </CardTitle>
            <CardDescription className="text-xs">
              Click any client to view their profile, risk rating, and account details
            </CardDescription>
          </CardHeader>
          <CardContent>
            {filteredClients.length === 0 ? (
              <div className="text-center py-12 text-xs text-muted-foreground">
                No clients match the selected filters.
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Client</TableHead>
                    <TableHead>Document / ID</TableHead>
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
                      <TableCell className="font-semibold text-xs">
                        {client.fullName}
                        <div className="text-[10px] text-muted-foreground font-normal">
                          {client.clientType === "INDIVIDUAL" ? "Individual" : "Legal entity"}
                        </div>
                      </TableCell>
                      <TableCell className="text-xs font-mono">
                        {client.documentNumber}
                        {client.nuib && (
                          <div className="text-[10px] text-muted-foreground">
                            NUIB: {client.nuib}
                          </div>
                        )}
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={client.regime === "SIMPLIFICADO" ? "outline" : "secondary"}
                          className="text-[10px]"
                        >
                          {client.regime}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={client.riskRating === "ALTO" ? "destructive" : "secondary"}
                          className="text-[10px]"
                        >
                          {client.riskRating}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {client.isPep ? (
                          <Badge variant="warning" className="text-[10px]">
                            PEP ({client.pepCategory || "National"})
                          </Badge>
                        ) : (
                          <span className="text-[11px] text-muted-foreground">—</span>
                        )}
                      </TableCell>
                      <TableCell className="text-xs font-semibold">
                        {formatMt(client.monthlyVolumeMt)}
                      </TableCell>
                      <TableCell className="text-right" onClick={(e) => e.stopPropagation()}>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openClientDetail(client)}
                          className="text-xs h-7 px-2.5"
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
      </main>

      {/* Slide-out Client Detail Sheet */}
      <ClientDetailSheet
        client={selectedClient}
        isOpen={isDetailOpen}
        onClose={() => {
          setIsDetailOpen(false);
          setSelectedClient(null);
        }}
      />

      {/* New Client KYC Dialog */}
      <Dialog open={isNewClientOpen} onOpenChange={setIsNewClientOpen}>
        <DialogContent className="max-w-lg max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <UserPlus className="h-5 w-5 text-primary" />
              Add client
            </DialogTitle>
            <DialogDescription className="text-xs">
              Create a client profile and run an immediate screening check.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleCreateClient} className="space-y-4 py-2 text-xs">
            {modalError && (
              <div className="p-2.5 bg-red-500/10 border border-red-500/30 rounded text-red-600 flex items-center gap-1.5">
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
                  className="text-xs font-mono"
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
                  className="text-xs font-mono"
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
                    <SelectItem value="SIMPLIFICADO">Simplificado (Cap. XIII)</SelectItem>
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

            {/* PEP Checkbox */}
            <div className="p-3 bg-muted/40 rounded-lg border space-y-2">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={newClientIsPep}
                  onChange={(e) => setNewClientIsPep(e.target.checked)}
                  className="rounded border-input text-primary focus:ring-primary h-4 w-4"
                />
                <span className="font-semibold text-foreground">
                  Identificado como Pessoa Exposta Politicamente (PEP)?
                </span>
              </label>

              {newClientIsPep && (
                <div className="pt-2 space-y-1.5">
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
              <Button
                type="submit"
                size="sm"
                disabled={isCreating}
                className="gap-1.5"
              >
                {isCreating && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                Cadastrar e Efetuar Triagem
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
