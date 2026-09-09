"use client"

import React from "react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { type LucideIcon } from "lucide-react"
import { cn } from "@/lib/utils"
import { Badge } from "@/components/ui/badge"
import {
  SidebarGroup,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar"

export interface NavItem {
  title: string
  url: string
  icon: LucideIcon
  badge?: string | number
  badgeVariant?: "default" | "secondary" | "destructive" | "outline" | "urgent"
}

export interface NavGroup {
  label?: string
  items: NavItem[]
}

export function NavMain({
  groups,
}: {
  groups: NavGroup[]
}) {
  const pathname = usePathname()

  return (
    <div className="flex flex-col gap-4 py-2">
      {groups.map((group, groupIdx) => (
        <SidebarGroup key={group.label || groupIdx} className="px-2 py-0">
          {group.label && (
            <SidebarGroupLabel className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground/70 px-3 mb-1.5">
              {group.label}
            </SidebarGroupLabel>
          )}
          <SidebarMenu className="gap-1">
            {group.items.map((item) => {
              const isActive =
                item.url === "/"
                  ? pathname === "/"
                  : pathname === item.url || pathname.startsWith(item.url + "/")

              return (
                <SidebarMenuItem key={item.title}>
                  <SidebarMenuButton
                    asChild
                    tooltip={item.title}
                    className={cn(
                      "group/btn relative flex h-9 items-center gap-2.5 rounded-md px-3 text-xs font-medium transition-all duration-150",
                      isActive
                        ? "bg-nav-teal-bg text-nav-teal-text font-semibold shadow-xs"
                        : "text-muted-foreground hover:bg-muted/70 hover:text-foreground"
                    )}
                  >
                    <Link href={item.url} className="flex items-center w-full gap-2.5">
                      {/* Active indicator bar */}
                      {isActive && (
                        <span
                          className="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-r-full bg-nav-teal-border"
                          aria-hidden="true"
                        />
                      )}
                      
                      <item.icon
                        className={cn(
                          "h-4 w-4 shrink-0 transition-colors",
                          isActive
                            ? "text-nav-teal-text"
                            : "text-muted-foreground/80 group-hover/btn:text-foreground"
                        )}
                      />
                      
                      <span className="truncate flex-1">{item.title}</span>

                      {item.badge !== undefined && (
                        <span
                          className={cn(
                            "ml-auto text-[10px] font-mono px-1.5 py-0.5 rounded-full font-semibold",
                            item.badgeVariant === "destructive" || item.badgeVariant === "urgent"
                              ? "bg-destructive/15 text-destructive border border-destructive/20"
                              : "bg-muted text-muted-foreground border border-border"
                          )}
                        >
                          {item.badge}
                        </span>
                      )}
                    </Link>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              )
            })}
          </SidebarMenu>
        </SidebarGroup>
      ))}
    </div>
  )
}
