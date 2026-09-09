"use client"

import React, { useEffect, useState } from "react"
import Link from "next/link"
import {
  LayoutDashboard,
  Users,
  AlertTriangle,
  UploadCloud,
  FileText,
  Settings,
} from "lucide-react"

import { FincompLogo } from "@/components/fincomp-logo"
import { NavMain, type NavGroup } from "@/components/nav-main"
import { NavUser } from "@/components/nav-user"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarSeparator,
} from "@/components/ui/sidebar"
import { api } from "@/lib/api-client"

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const [pendingCount, setPendingCount] = useState<number | undefined>(undefined)

  useEffect(() => {
    let mounted = true
    api
      .getInsights()
      .then((data) => {
        if (mounted && typeof data.pendingFlagsCount === "number" && data.pendingFlagsCount > 0) {
          setPendingCount(data.pendingFlagsCount)
        }
      })
      .catch(() => {})
    return () => { mounted = false }
  }, [])

  const navigationGroups: NavGroup[] = [
    {
      label: "Core",
      items: [
        { title: "Overview",   url: "/",        icon: LayoutDashboard },
        { title: "Clients",    url: "/clients",  icon: Users },
        {
          title: "Alerts",
          url: "/flags",
          icon: AlertTriangle,
          badge: pendingCount,
          badgeVariant: "urgent",
        },
        { title: "Import",     url: "/import",   icon: UploadCloud },
      ],
    },
    {
      label: "Reporting",
      items: [
        { title: "Reports",    url: "/reports",  icon: FileText },
        { title: "Settings",   url: "/settings", icon: Settings },
      ],
    },
  ]

  return (
    <Sidebar collapsible="icon" className="border-r border-border/70" {...props}>
      <SidebarHeader className="border-b border-border/60 p-4">
        <Link
          href="/"
          className="inline-flex items-center transition-opacity hover:opacity-85 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          aria-label="FinComp home"
        >
          <FincompLogo size="md" priority />
        </Link>
      </SidebarHeader>

      <SidebarContent className="px-1 py-2">
        <NavMain groups={navigationGroups} />
      </SidebarContent>

      <SidebarSeparator className="mx-2 border-border/60" />

      <SidebarFooter className="p-2">
        <NavUser />
      </SidebarFooter>
    </Sidebar>
  )
}
