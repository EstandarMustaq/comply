"use client";

import React, { useEffect, useState } from "react";
import {
  ShieldAlert,
  Clock,
  Filter,
  CheckCircle2,
  AlertTriangle,
  FileSpreadsheet,
  RefreshCw,
} from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { FlagReviewSheet } from "@/components/flag-review-sheet";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { api } from "@/lib/api-client";
import { Flag } from "@/lib/types";

export default function FlagsPage() {
  const [flags, setFlags] = useState<Flag[]>([]);
  const [activeTab, setActiveTab] = useState<string>("PENDING");
  const [isLoading, setIsLoading] = useState(false);
  const [selectedFlag, setSelectedFlag] = useState<Flag | null>(null);
  const [isReviewOpen, setIsReviewOpen] = useState(false);

  const loadFlags = async () => {
    try {
      setIsLoading(true);
      let statusParam: string | undefined = activeTab;
      let typeParam: string | undefined = undefined;

      if (activeTab === "SANCTIONS") {
        statusParam = "PENDING";
        typeParam = "SANCTIONS_MATCH";
      } else if (activeTab === "STRUCTURING") {
        statusParam = "PENDING";
        typeParam = "STRUCTURING_SUSPECTED";
      }

      const data = await api.getFlags(statusParam, typeParam);

      // Sort: SANCTIONS_MATCH first, then by SLA deadline ascending
      const sorted = [...data].sort((a, b) => {
        if (a.flagType === "SANCTIONS_MATCH" && b.flagType !== "SANCTIONS_MATCH") return -1;
        if (b.flagType === "SANCTIONS_MATCH" && a.flagType !== "SANCTIONS_MATCH") return 1;
        if (a.slaDeadline && b.slaDeadline) {
          return new Date(a.slaDeadline).getTime() - new Date(b.slaDeadline).getTime();
        }
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      });

      setFlags(sorted);
    } catch (err) {
      console.error("Erro ao carregar flags:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadFlags();
  }, [activeTab]);

  const openReview = (flag: Flag) => {
    setSelectedFlag(flag);
    setIsReviewOpen(true);
  };

  const getSlaBadge = (deadline?: string) => {
    if (!deadline) return null;
    const diff = new Date(deadline).getTime() - new Date().getTime();
    if (diff <= 0) {
      return (
        <Badge variant="destructive" className="text-[10px]">
          SLA Expirado!
        </Badge>
      );
    }
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    return (
      <Badge variant="urgent" className="text-[10px] flex items-center gap-1">
        <Clock className="h-2.5 w-2.5" />
        {hours}h {minutes}m restantes
      </Badge>
    );
  };

  const formatMt = (val?: number) => {
    if (!val) return "N/D";
    return new Intl.NumberFormat("pt-MZ", {
      style: "currency",
      currency: "MZN",
    }).format(val);
  };

  return (
    <div className="flex-1 flex flex-col">
      <SiteHeader
        title="Alerts"
        action={
          <Button
            variant="outline"
            size="sm"
            onClick={loadFlags}
            disabled={isLoading}
            className="text-xs gap-1.5 h-8"
          >
            <RefreshCw className={`h-3 w-3 ${isLoading ? "animate-spin" : ""}`} />
            <span>Refresh</span>
          </Button>
        }
      />

      <main className="flex-1 p-6 space-y-6 overflow-y-auto">
        <div className="flex items-center justify-between">
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full max-w-xl">
            <TabsList className="grid grid-cols-4 w-full">
              <TabsTrigger value="PENDING" className="text-xs">
                Pending
              </TabsTrigger>
              <TabsTrigger value="SANCTIONS" className="text-xs text-red-600 font-semibold">
                Sanctions
              </TabsTrigger>
              <TabsTrigger value="STRUCTURING" className="text-xs">
                Structuring
              </TabsTrigger>
              <TabsTrigger value="RESOLVED" className="text-xs">
                Resolved
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </div>

        {/* Flags List Card */}
        <Card className="shadow-sm border-border/70">
          <CardHeader className="pb-3">
            <CardTitle className="text-base font-semibold flex items-center gap-2">
              <ShieldAlert className="h-4 w-4 text-amber-500" />
              Open alerts ({flags.length})
            </CardTitle>
            <CardDescription className="text-xs">
              Click any row to review and record a decision
            </CardDescription>
          </CardHeader>
          <CardContent>
            {flags.length === 0 ? (
              <div className="text-center py-12 text-xs text-muted-foreground flex flex-col items-center gap-2">
                <CheckCircle2 className="h-7 w-7 text-emerald-500" />
                <span>No alerts in this category.</span>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[160px]">Type</TableHead>
                    <TableHead>Client</TableHead>
                    <TableHead>Amount</TableHead>
                    <TableHead>Rule triggered</TableHead>
                    <TableHead>SLA</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {flags.map((flag) => {
                    const isSanction = flag.flagType === "SANCTIONS_MATCH";
                    return (
                      <TableRow
                        key={flag.id}
                        className={`cursor-pointer hover:bg-muted/50 ${
                          isSanction ? "bg-red-50/20" : ""
                        }`}
                        onClick={() => openReview(flag)}
                      >
                        <TableCell>
                          <Badge
                            variant={isSanction ? "urgent" : "secondary"}
                            className="text-[10px]"
                          >
                            {flag.flagType.replace(/_/g, " ")}
                          </Badge>
                        </TableCell>
                        <TableCell className="font-medium text-xs">
                          {flag.client?.fullName || "—"}
                          <div className="text-[11px] text-muted-foreground font-mono">
                            {flag.client?.documentNumber}
                          </div>
                        </TableCell>
                        <TableCell className="text-xs font-semibold">
                          {formatMt(flag.transaction?.amount)}
                        </TableCell>
                        <TableCell className="text-xs max-w-sm truncate text-muted-foreground">
                          {flag.ruleTriggered}
                        </TableCell>
                        <TableCell>
                          {isSanction ? (
                            getSlaBadge(flag.slaDeadline)
                          ) : (
                            <span className="text-xs text-muted-foreground">Standard</span>
                          )}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-[10px]">
                            {flag.status}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-right" onClick={(e) => e.stopPropagation()}>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => openReview(flag)}
                            className="text-xs h-7 px-2.5"
                          >
                            Review
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </main>

      {/* Slide-out Review Sheet */}
      <FlagReviewSheet
        flag={selectedFlag}
        isOpen={isReviewOpen}
        onClose={() => {
          setIsReviewOpen(false);
          setSelectedFlag(null);
        }}
        onFlagUpdated={loadFlags}
      />
    </div>
  );
}
