"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { Scale, Lock, RefreshCw, History } from "lucide-react"
import { SiteHeader } from "@/components/site-header"
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
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { api } from "@/lib/api-client"
import { type ThresholdRule, type AuditLog } from "@/lib/types"
import {
  getAuditActionLabel,
  getResourceTypeLabel,
  getRuleTypeLabel,
  getUserRoleLabel,
} from "@/lib/labels"

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<string>("RULES")
  const [rules, setRules] = useState<ThresholdRule[]>([])
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const _userRole = useMemo(() => {
    try {
      const stored = localStorage.getItem("complyz_user")
      if (stored) {
        const u = JSON.parse(stored) as { role?: string }
        return u.role || ""
      }
    } catch {
      // Keep the activity section empty when the user cannot access its records.
    }
    return ""
  }, [])

  const loadData = useCallback(async () => {
    try {
      setIsLoading(true)
      if (activeTab === "RULES") {
        const rulesData = await api.getThresholdRules()
        setRules(rulesData)
      } else if (activeTab === "AUDIT") {
        const logsData = await api.getAuditLogs()
        setLogs(logsData)
      }
    } catch (err) {
      console.error("Failed to load settings:", err)
    } finally {
      setIsLoading(false)
    }
  }, [activeTab])

  useEffect(() => {
    queueMicrotask(() => {
      loadData()
    })
  }, [loadData])

  const formatMt = (val?: number) => {
    if (val === undefined || val === null) return "N/D"
    return new Intl.NumberFormat("en-MZ", {
      style: "currency",
      currency: "MZN",
      maximumFractionDigits: 0,
    }).format(val)
  }

  return (
    <div>
      <SiteHeader
        title="Rules and activity"
        action={
          <Button
            variant="outline"
            size="sm"
            onClick={loadData}
            disabled={isLoading}
            className="h-8 gap-1.5 text-xs"
          >
            <RefreshCw className={`h-3 w-3 ${isLoading ? "animate-spin" : ""}`} />
            <span>Refresh</span>
          </Button>
        }
      />

      <div className="space-y-6">
        {/* Settings tabs */}
        <div className="flex items-center justify-between">
          <Tabs
            value={activeTab}
            onValueChange={setActiveTab}
            className="w-full max-w-md"
          >
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="RULES" className="text-xs">
                Monitoring thresholds
              </TabsTrigger>
              <TabsTrigger value="AUDIT" className="text-xs">
                Activity history
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </div>

        {activeTab === "RULES" ? (
          /* Monitoring rules */
          <div className="space-y-6">
            <Card className="border-border shadow-sm">
              <CardHeader className="pb-3">
                <CardTitle className="flex items-center gap-2 text-base font-semibold">
                  <Scale className="h-4 w-4 text-primary" />
                  Screening rules
                </CardTitle>
                <CardDescription className="text-xs">
                  Rules that screen transactions during import.
                </CardDescription>
              </CardHeader>
              <CardContent>
                {rules.length === 0 ? (
                  <div className="py-8 text-center text-xs text-muted-foreground">
                    {isLoading ? "Loading rules…" : "No rules configured."}
                  </div>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Type</TableHead>
                        <TableHead>Rule</TableHead>
                        <TableHead>Threshold</TableHead>
                        <TableHead>Aggregation window</TableHead>
                        <TableHead>Status</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {rules.map((rule) => (
                        <TableRow key={rule.id}>
                          <TableCell className="font-mono text-xs font-semibold">
                            {getRuleTypeLabel(rule.type)}
                          </TableCell>
                          <TableCell className="text-xs">
                            <span className="font-medium text-foreground">
                              {rule.name}
                            </span>
                          </TableCell>
                          <TableCell className="text-xs font-bold text-foreground">
                            {rule.valueThreshold === null
                              ? "Not applicable"
                              : formatMt(rule.valueThreshold)}
                          </TableCell>
                          <TableCell className="text-xs">
                            {rule.aggregationWindowDays && rule.aggregationWindowDays > 0
                              ? `${rule.aggregationWindowDays} days`
                              : "Per transaction"}
                          </TableCell>
                          <TableCell>
                            <Badge
                              variant={rule.active ? "success" : "secondary"}
                              className="text-[10px]"
                            >
                              {rule.active ? "ACTIVE" : "INACTIVE"}
                            </Badge>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </CardContent>
            </Card>

            {/* Monitoring threshold summary */}
            <div className="grid gap-4 md:grid-cols-2">
              <Card className="border-border">
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-bold">
                    Simplified due diligence
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-2 text-xs">
                  <div className="flex justify-between border-b pb-1">
                    <span>Monthly limit per customer</span>
                    <span className="font-bold">30.000 MT</span>
                  </div>
                  <div className="flex justify-between border-b pb-1">
                    <span>Annual limit</span>
                    <span className="font-bold">240.000 MT</span>
                  </div>
                  <p className="pt-1 text-[11px] text-muted-foreground">
                    Applies to low-risk customers under simplified due diligence.
                    Exceeding the limit requires enhanced due diligence.
                  </p>
                </CardContent>
              </Card>

              <Card className="border-destructive/40">
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-bold text-destructive">
                    Sanctions screening
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-2 text-xs text-muted-foreground">
                  <div className="flex justify-between border-b pb-1">
                    <span>Decision window</span>
                    <span className="font-bold text-red-700">24 hours</span>
                  </div>
                  <div className="flex justify-between border-b pb-1">
                    <span>Action required</span>
                    <span className="font-bold">Block and escalate</span>
                  </div>
                  <p className="pt-1 text-[11px] text-muted-foreground">
                    Automatically screen against official and consolidated sanctions
                    lists.
                  </p>
                </CardContent>
              </Card>
            </div>
          </div>
        ) : (
          <Card className="shadow-sm">
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="flex items-center gap-2 text-base font-semibold">
                    <History className="h-4 w-4 text-primary" />
                    Audit history
                  </CardTitle>
                  <CardDescription className="text-xs">
                    A protected record of actions taken in the system.
                  </CardDescription>
                </div>
                <Badge
                  variant="outline"
                  className="border-emerald-300 bg-emerald-50 text-[11px] text-emerald-700"
                >
                  <Lock className="mr-1 h-3 w-3" /> Immutable history
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              {logs.length === 0 ? (
                <div className="py-8 text-center text-xs text-muted-foreground">
                  No activity to show, or you do not have permission to view it.
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Date / time</TableHead>
                      <TableHead>User</TableHead>
                      <TableHead>Role</TableHead>
                      <TableHead>Action</TableHead>
                      <TableHead>Resource</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {logs.map((log) => (
                      <TableRow key={log.id}>
                        <TableCell className="whitespace-nowrap text-xs text-muted-foreground">
                          {new Date(log.createdAt).toLocaleString("en-MZ")}
                        </TableCell>
                        <TableCell className="text-xs font-semibold">
                          {log.actor?.email || "Unknown user"}
                        </TableCell>
                        <TableCell>
                          <Badge variant="secondary" className="text-[10px]">
                            {getUserRoleLabel(log.actor?.role || "")}
                          </Badge>
                        </TableCell>
                        <TableCell className="font-mono text-xs font-medium text-foreground">
                          {getAuditActionLabel(log.action)}
                        </TableCell>
                        <TableCell className="text-xs text-muted-foreground">
                          {getResourceTypeLabel(log.entity)}{" "}
                          {log.entityId ? `(${log.entityId.substring(0, 8)}...)` : ""}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  )
}
