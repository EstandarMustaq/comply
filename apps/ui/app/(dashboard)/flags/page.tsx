"use client"

import { useCallback, useEffect, useState } from "react"
import { ShieldAlert, Clock, CheckCircle2, RefreshCw } from "lucide-react"
import { SiteHeader } from "@/components/site-header"
import { FlagReviewSheet } from "@/components/flag-review-sheet"
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
import { type Flag } from "@/lib/types"
import { getFlagStatusLabel, getFlagTypeLabel } from "@/lib/labels"

/**
 * Load alerts for the selected status or pending alert category, placing
 * sanctions matches first, and refresh them after review. Failed loads retain
 * the previous list.
 */
export default function FlagsPage() {
  const [flags, setFlags] = useState<Flag[]>([])
  const [activeTab, setActiveTab] = useState<string>("PENDING")
  const [isLoading, setIsLoading] = useState(false)
  const [selectedFlag, setSelectedFlag] = useState<Flag | null>(null)
  const [isReviewOpen, setIsReviewOpen] = useState(false)

  const loadFlags = useCallback(async () => {
    try {
      setIsLoading(true)
      let statusParam: string | undefined = activeTab
      let typeParam: string | undefined = undefined

      if (activeTab === "SANCTIONS") {
        statusParam = "PENDING"
        typeParam = "SANCTIONS_MATCH"
      } else if (activeTab === "STRUCTURING") {
        statusParam = "PENDING"
        typeParam = "STRUCTURING_SUSPECTED"
      }

      const data = await api.getFlags(statusParam, typeParam)

      const sorted = [...data].sort((a, b) => {
        if (a.flagType === "SANCTIONS_MATCH" && b.flagType !== "SANCTIONS_MATCH")
          return -1
        if (b.flagType === "SANCTIONS_MATCH" && a.flagType !== "SANCTIONS_MATCH") return 1
        if (a.slaDeadline && b.slaDeadline) {
          return new Date(a.slaDeadline).getTime() - new Date(b.slaDeadline).getTime()
        }
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      })

      setFlags(sorted)
    } catch (err) {
      console.error("Failed to load alerts:", err)
    } finally {
      setIsLoading(false)
    }
  }, [activeTab])

  useEffect(() => {
    queueMicrotask(() => {
      loadFlags()
    })
  }, [loadFlags])

  const openReview = (flag: Flag) => {
    setSelectedFlag(flag)
    setIsReviewOpen(true)
  }

  const getSlaBadge = (deadline?: string) => {
    if (!deadline) return null
    const diff = new Date(deadline).getTime() - new Date().getTime()
    if (diff <= 0) {
      return (
        <Badge variant="destructive" className="text-[10px]">
          SLA Expirado!
        </Badge>
      )
    }
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    return (
      <Badge variant="urgent" className="flex items-center gap-1 text-[10px]">
        <Clock className="h-2.5 w-2.5" />
        {hours}h {minutes}m restantes
      </Badge>
    )
  }

  const formatMt = (val?: number) => {
    if (!val) return "—"
    return new Intl.NumberFormat("en-MZ", {
      style: "currency",
      currency: "MZN",
    }).format(val)
  }

  return (
    <div>
      <SiteHeader
        title="Alertas"
        action={
          <Button
            variant="outline"
            size="sm"
            onClick={loadFlags}
            disabled={isLoading}
            className="h-8 gap-1.5 text-xs"
          >
            <RefreshCw className={`h-3 w-3 ${isLoading ? "animate-spin" : ""}`} />
            <span>Refresh</span>
          </Button>
        }
      />

      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <Tabs
            value={activeTab}
            onValueChange={setActiveTab}
            className="w-full max-w-xl"
          >
            <TabsList className="grid w-full grid-cols-4">
              <TabsTrigger value="PENDING" className="text-xs">
                Pending
              </TabsTrigger>
              <TabsTrigger
                value="SANCTIONS"
                className="text-xs font-semibold text-red-600"
              >
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

        <Card className="border-border shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base font-semibold">
              <ShieldAlert className="h-4 w-4 text-amber-500" />
              Open alerts ({flags.length})
            </CardTitle>
            <CardDescription className="text-xs">
              Select an alert to review it and record a decision.
            </CardDescription>
          </CardHeader>
          <CardContent>
            {flags.length === 0 ? (
              <div className="flex flex-col items-center gap-2 py-12 text-center text-xs text-muted-foreground">
                <CheckCircle2 className="h-7 w-7 text-emerald-500" />
                <span>No alerts in this category.</span>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[160px]">Tipo</TableHead>
                    <TableHead>Customer</TableHead>
                    <TableHead>Amount</TableHead>
                    <TableHead>Triggered rule</TableHead>
                    <TableHead>Due</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {flags.map((flag) => {
                    const isSanction = flag.flagType === "SANCTIONS_MATCH"
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
                            {getFlagTypeLabel(flag.flagType)}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-xs font-medium">
                          {flag.client?.fullName || "—"}
                          <div className="font-mono text-[11px] text-muted-foreground">
                            {flag.client?.documentNumber}
                          </div>
                        </TableCell>
                        <TableCell className="text-xs font-semibold">
                          {formatMt(flag.transaction?.amount)}
                        </TableCell>
                        <TableCell className="max-w-sm truncate text-xs text-muted-foreground">
                          {flag.ruleTriggered}
                        </TableCell>
                        <TableCell>
                          {isSanction ? (
                            getSlaBadge(flag.slaDeadline)
                          ) : (
                            <span className="text-xs text-muted-foreground">Normal</span>
                          )}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-[10px]">
                            {getFlagStatusLabel(flag.status)}
                          </Badge>
                        </TableCell>
                        <TableCell
                          className="text-right"
                          onClick={(e) => e.stopPropagation()}
                        >
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => openReview(flag)}
                            className="h-7 px-2.5 text-xs"
                          >
                            Review
                          </Button>
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </div>

      <FlagReviewSheet
        flag={selectedFlag}
        isOpen={isReviewOpen}
        onClose={() => {
          setIsReviewOpen(false)
          setSelectedFlag(null)
        }}
        onFlagUpdated={loadFlags}
      />
    </div>
  )
}
