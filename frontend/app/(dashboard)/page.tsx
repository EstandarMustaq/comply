"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  ShieldAlert,
  ArrowRight,
  Clock,
  CheckCircle2,
  FileSpreadsheet,
  AlertTriangle,
} from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { SectionCards } from "@/components/section-cards";
import { ChartAreaInteractive } from "@/components/chart-area-interactive";
import { FlagReviewSheet } from "@/components/flag-review-sheet";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api-client";
import { Flag, InsightsData } from "@/lib/types";

export default function DashboardOverviewPage() {
  const router = useRouter();
  const [insights, setInsights] = useState<InsightsData | null>(null);
  const [pendingFlags, setPendingFlags] = useState<Flag[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedFlag, setSelectedFlag] = useState<Flag | null>(null);
  const [isReviewOpen, setIsReviewOpen] = useState(false);

  const loadDashboardData = async () => {
    try {
      setIsLoading(true);
      const [insightsRes, flagsRes] = await Promise.all([
        api.getInsights(),
        api.getFlags("PENDING"),
      ]);
      setInsights(insightsRes);
      setPendingFlags(flagsRes.slice(0, 10)); // Top 10 priority
    } catch (err) {
      console.error("Erro ao carregar dashboard:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  const openFlagReview = (flag: Flag) => {
    setSelectedFlag(flag);
    setIsReviewOpen(true);
  };

  const getSlaBadge = (deadline?: string) => {
    if (!deadline) return null;
    const diff = new Date(deadline).getTime() - new Date().getTime();
    if (diff <= 0) {
      return <Badge variant="destructive" className="text-[10px]">SLA Expirado</Badge>;
    }
    const hours = Math.floor(diff / (1000 * 60 * 60));
    return (
      <Badge variant="urgent" className="text-[10px] flex items-center gap-1">
        <Clock className="h-2.5 w-2.5" />
        {hours}h restantes
      </Badge>
    );
  };

  return (
    <div className="flex-1 flex flex-col">
      <SiteHeader
        title="Overview"
        action={
          <Button
            size="sm"
            onClick={() => router.push("/import")}
            className="text-xs gap-1.5 shadow-sm"
          >
            <FileSpreadsheet className="h-3.5 w-3.5" />
            <span>Import data</span>
          </Button>
        }
      />

      <main className="flex-1 p-6 space-y-6 overflow-y-auto">
        {/* KPI Section Cards */}
        <SectionCards data={insights} isLoading={isLoading} />

        {/* Charts & Analytical Trends */}
        <div className="grid gap-6">
          <ChartAreaInteractive />
        </div>

        {/* Priority Alerts Table */}
        <Card className="shadow-sm border-border/70">
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="text-base font-semibold flex items-center gap-2">
                <ShieldAlert className="h-4 w-4 text-amber-500" />
                Open Alerts
              </CardTitle>
              <CardDescription className="text-xs">
                Prioritised by severity — click to review and resolve
              </CardDescription>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => router.push("/flags")}
              className="text-xs gap-1"
            >
              <span>View all</span>
              <ArrowRight className="h-3 w-3" />
            </Button>
          </CardHeader>
          <CardContent>
            {pendingFlags.length === 0 ? (
              <div className="flex flex-col items-center gap-2 py-10 text-center text-xs text-muted-foreground">
                <CheckCircle2 className="h-7 w-7 text-emerald-500" />
                <span>No open alerts — everything looks clean.</span>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[160px]">Type</TableHead>
                    <TableHead>Client</TableHead>
                    <TableHead>Rule triggered</TableHead>
                    <TableHead>SLA</TableHead>
                    <TableHead className="text-right">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {pendingFlags.map((flag) => {
                    const isSanction = flag.flagType === "SANCTIONS_MATCH";
                    return (
                      <TableRow key={flag.id} className="cursor-pointer hover:bg-muted/50">
                        <TableCell>
                          <Badge
                            variant={isSanction ? "urgent" : "secondary"}
                            className="text-[11px]"
                          >
                            {flag.flagType.replace(/_/g, " ")}
                          </Badge>
                        </TableCell>
                        <TableCell className="font-medium">
                          {flag.client?.fullName || "—"}
                          <div className="text-[11px] text-muted-foreground">
                            {flag.client?.documentNumber}
                          </div>
                        </TableCell>
                        <TableCell className="text-xs max-w-xs truncate">
                          {flag.ruleTriggered}
                        </TableCell>
                        <TableCell>
                          {isSanction ? (
                            getSlaBadge(flag.slaDeadline)
                          ) : (
                            <span className="text-xs text-muted-foreground">Standard</span>
                          )}
                        </TableCell>
                        <TableCell className="text-right">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => openFlagReview(flag)}
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

      {/* Lateral Flag Review Sheet */}
      <FlagReviewSheet
        flag={selectedFlag}
        isOpen={isReviewOpen}
        onClose={() => {
          setIsReviewOpen(false);
          setSelectedFlag(null);
        }}
        onFlagUpdated={loadDashboardData}
      />
    </div>
  );
}
