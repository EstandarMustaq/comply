"use client"

import React, { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { LogOut, Settings, ChevronUpIcon, Loader2 } from "lucide-react"

import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar"
import { api, type CurrentUser } from "@/lib/api-client"

const ROLE_LABEL: Record<string, string> = {
  ADMIN: "Administrator",
  COMPLIANCE_OFFICER: "Compliance Officer",
  OCOS: "OCOS",
  AUDITOR_READONLY: "Auditor",
}

function getInitials(name: string): string {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase()
}

export function NavUser() {
  const router = useRouter()
  const { isMobile } = useSidebar()
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    api
      .getMe()
      .then((data) => {
        if (!cancelled) setUser(data)
      })
      .catch(() => {
        // Token likely invalid; dashboard layout will redirect to /login
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const handleLogout = async () => {
    await api.logout()
    router.push("/login")
  }

  if (loading) {
    return (
      <SidebarMenu>
        <SidebarMenuItem>
          <div className="flex h-9 items-center gap-2 px-3 text-xs text-muted-foreground">
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
            <span>Loading…</span>
          </div>
        </SidebarMenuItem>
      </SidebarMenu>
    )
  }

  if (!user) return null

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <SidebarMenuButton
              size="lg"
              className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground group rounded-lg transition-colors"
            >
              <Avatar className="h-7 w-7 rounded-md border border-border/60">
                <AvatarFallback className="rounded-md bg-nav-teal-bg text-nav-teal-text text-[11px] font-semibold">
                  {getInitials(user.fullName)}
                </AvatarFallback>
              </Avatar>
              <div className="grid flex-1 text-left text-xs leading-tight min-w-0">
                <span className="truncate font-semibold text-foreground">
                  {user.fullName}
                </span>
                <span className="truncate text-[11px] text-muted-foreground">
                  {ROLE_LABEL[user.role] ?? user.role}
                </span>
              </div>
              <ChevronUpIcon className="ml-auto size-3.5 text-muted-foreground/70 group-data-[state=open]:rotate-180 transition-transform duration-150" />
            </SidebarMenuButton>
          </DropdownMenuTrigger>

          <DropdownMenuContent
            className="w-[--radix-dropdown-menu-trigger-width] min-w-52 rounded-lg p-1.5 shadow-md border-border/70"
            side={isMobile ? "bottom" : "right"}
            align="end"
            sideOffset={6}
          >
            <DropdownMenuLabel className="px-2 py-1.5 font-normal">
              <div className="flex flex-col gap-0.5">
                <span className="text-xs font-semibold text-foreground">{user.fullName}</span>
                <span className="text-[11px] text-muted-foreground">{user.email}</span>
              </div>
            </DropdownMenuLabel>

            <DropdownMenuSeparator className="my-1" />

            <DropdownMenuGroup>
              <DropdownMenuItem
                className="text-xs gap-2 py-1.5 cursor-pointer"
                onClick={() => router.push("/settings")}
              >
                <Settings className="h-3.5 w-3.5 text-muted-foreground" />
                Settings
              </DropdownMenuItem>
            </DropdownMenuGroup>

            <DropdownMenuSeparator className="my-1" />

            <DropdownMenuItem
              className="text-xs text-destructive focus:text-destructive gap-2 py-1.5 cursor-pointer"
              onClick={handleLogout}
            >
              <LogOut className="h-3.5 w-3.5" />
              Sign out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </SidebarMenuItem>
    </SidebarMenu>
  )
}
