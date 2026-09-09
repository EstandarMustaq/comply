"use client";

import React, { useEffect, useState } from "react";
import {
  Settings,
  ShieldCheck,
  Scale,
  Lock,
  FileCheck,
  RefreshCw,
  AlertTriangle,
  History,
  CheckCircle2,
} from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { api } from "@/lib/api-client";
import { ThresholdRule, AuditLog } from "@/lib/types";

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<string>("RULES");
  const [rules, setRules] = useState<ThresholdRule[]>([]);
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [userRole, setUserRole] = useState<string>("");

  useEffect(() => {
    try {
      const stored = localStorage.getItem("fincomp_user");
      if (stored) {
        const u = JSON.parse(stored);
        setUserRole(u.role || "");
      }
    } catch {}
  }, []);

  const loadData = async () => {
    try {
      setIsLoading(true);
      if (activeTab === "RULES") {
        const rulesData = await api.getThresholdRules();
        setRules(rulesData);
      } else if (activeTab === "AUDIT") {
        const logsData = await api.getAuditLogs();
        setLogs(logsData);
      }
    } catch (err) {
      console.error("Erro ao carregar configurações:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [activeTab]);

  const formatMt = (val?: number) => {
    if (val === undefined || val === null) return "N/D";
    return new Intl.NumberFormat("pt-MZ", {
      style: "currency",
      currency: "MZN",
      maximumFractionDigits: 0,
    }).format(val);
  };

  return (
    <div className="flex-1 flex flex-col">
      <SiteHeader
        title="Configurações Regulatórias & Auditoria Imutável"
        subtitle="Parametrização de limiares de triagem e visualização da trilha forense imutável"
        action={
          <Button
            variant="outline"
            size="sm"
            onClick={loadData}
            disabled={isLoading}
            className="text-xs gap-1.5 h-8"
          >
            <RefreshCw className={`h-3 w-3 ${isLoading ? "animate-spin" : ""}`} />
            <span>Atualizar</span>
          </Button>
        }
      />

      <main className="flex-1 p-6 space-y-6 overflow-y-auto">
        {/* Navigation Tabs */}
        <div className="flex items-center justify-between">
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full max-w-md">
            <TabsList className="grid grid-cols-2 w-full">
              <TabsTrigger value="RULES" className="text-xs">
                Regras de Limiares Legais
              </TabsTrigger>
              <TabsTrigger value="AUDIT" className="text-xs">
                Trilha Forense de Auditoria
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </div>

        {activeTab === "RULES" ? (
          /* Rules Card */
          <div className="space-y-6">
            <Card className="shadow-sm border-border/70">
              <CardHeader className="pb-3">
                <CardTitle className="text-base font-semibold flex items-center gap-2">
                  <Scale className="h-4 w-4 text-primary" />
                  Screening rules
                </CardTitle>
                <CardDescription className="text-xs">
                  Automated rules applied at ingestion to flag transactions in real time
                </CardDescription>
              </CardHeader>
              <CardContent>
                {rules.length === 0 ? (
                  <div className="text-center py-8 text-xs text-muted-foreground">
                    Loading rules…
                  </div>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Rule code</TableHead>
                        <TableHead>Name</TableHead>
                        <TableHead>Threshold</TableHead>
                        <TableHead>Window</TableHead>
                        <TableHead>Regime</TableHead>
                        <TableHead>Status</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {rules.map((rule) => (
                        <TableRow key={rule.id}>
                          <TableCell className="font-mono text-xs font-semibold">
                            {rule.ruleCode}
                          </TableCell>
                          <TableCell className="text-xs">
                            <span className="font-medium text-foreground">{rule.name}</span>
                            <div className="text-[10px] text-muted-foreground">
                              {rule.description}
                            </div>
                          </TableCell>
                          <TableCell className="text-xs font-bold text-foreground">
                            {formatMt(rule.thresholdAmount)}
                          </TableCell>
                          <TableCell className="text-xs">
                            {rule.timeWindowDays > 0 ? `${rule.timeWindowDays}d` : "Per transaction"}
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline" className="text-[10px]">
                              {rule.regime || "ALL"}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Badge
                              variant={rule.isActive ? "success" : "secondary"}
                              className="text-[10px]"
                            >
                              {rule.isActive ? "ACTIVE" : "INACTIVE"}
                            </Badge>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </CardContent>
            </Card>

            {/* Regime thresholds summary */}
            <div className="grid gap-4 md:grid-cols-2">
              <Card className="border-border">
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-bold">
                    Standard regime
                  </CardTitle>
                </CardHeader>
                <CardContent className="text-xs space-y-2">
                  <div className="flex justify-between border-b pb-1">
                    <span>Monthly cap per client</span>
                    <span className="font-bold">30,000 MT</span>
                  </div>
                  <div className="flex justify-between border-b pb-1">
                    <span>Annual cap</span>
                    <span className="font-bold">240,000 MT</span>
                  </div>
                  <p className="text-[11px] text-muted-foreground pt-1">
                    Applies to low-risk clients with simplified onboarding. Exceeding the cap triggers a mandatory upgrade to enhanced due diligence.
                  </p>
                </CardContent>
              </Card>

              <Card className="border-red-200 bg-red-50/20">
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-bold text-red-900">
                    Sanctions screening
                  </CardTitle>
                </CardHeader>
                <CardContent className="text-xs space-y-2 text-red-950">
                  <div className="flex justify-between border-b pb-1">
                    <span>Action window</span>
                    <span className="font-bold text-red-700">24 hours</span>
                  </div>
                  <div className="flex justify-between border-b pb-1">
                    <span>Required action</span>
                    <span className="font-bold">Freeze & escalate</span>
                  </div>
                  <p className="text-[11px] text-muted-foreground pt-1">
                    Mandatory screening against national and UN consolidated sanctions lists.
                  </p>
                </CardContent>
              </Card>
            </div>
          </div>
        ) : (
          /* Audit Logs Card */
          <Card className="shadow-sm">
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-base font-semibold flex items-center gap-2">
                    <History className="h-4 w-4 text-primary" />
                    Trilha de auditoria imutável
                  </CardTitle>
                  <CardDescription className="text-xs">
                    Registo inviolável protegido por trigger a nível de base de dados PostgreSQL (rejeição estrita de UPDATE e DELETE)
                  </CardDescription>
                </div>
                <Badge variant="outline" className="text-[11px] bg-emerald-50 text-emerald-700 border-emerald-300">
                  <Lock className="h-3 w-3 mr-1" /> Imutabilidade Garantida
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              {logs.length === 0 ? (
                <div className="text-center py-8 text-xs text-muted-foreground">
                  Nenhum evento de auditoria registado ou sem permissão de acesso (exclusivo para ADMIN).
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Data / Hora</TableHead>
                      <TableHead>Utilizador</TableHead>
                      <TableHead>Perfil</TableHead>
                      <TableHead>Ação Realizada</TableHead>
                      <TableHead>Recurso Afetado</TableHead>
                      <TableHead>Endereço IP</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {logs.map((log) => (
                      <TableRow key={log.id}>
                        <TableCell className="text-xs text-muted-foreground whitespace-nowrap">
                          {new Date(log.createdAt).toLocaleString("pt-MZ")}
                        </TableCell>
                        <TableCell className="text-xs font-semibold">
                          {log.userEmail}
                        </TableCell>
                        <TableCell>
                          <Badge variant="secondary" className="text-[10px]">
                            {log.userRole}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-xs font-mono font-medium text-foreground">
                          {log.action}
                        </TableCell>
                        <TableCell className="text-xs text-muted-foreground">
                          {log.resourceType} {log.resourceId ? `(${log.resourceId.substring(0, 8)}...)` : ""}
                        </TableCell>
                        <TableCell className="text-xs font-mono text-muted-foreground">
                          {log.ipAddress || "127.0.0.1"}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        )}
      </main>
    </div>
  );
}
