"use client"

import * as React from "react"
import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import {
  AlertTriangle,
  BarChart3,
  FileText,
  LayoutDashboard,
  Settings,
  UploadCloud,
  Users,
} from "lucide-react"

import {
  AppShell,
  AppShellBody,
  AppShellContent,
  AppShellMain,
  AppShellNav,
  AppShellNavFooter,
  AppShellNavGroup,
  AppShellNavItem,
  AppShellNavItemCollapsible,
  AppShellNavSubItem,
  AppShellNavbar,
  AppShellRail,
  AppShellRailItem,
  AppShellSidebar,
  AppShellSidebarHeader,
} from "@/components/shell/app-shell"
import { navModules, resolveModule } from "@/components/shell/nav-config"
import { CommandSearch, type CommandSearchGroup } from "@/components/command-search"
import { NotificationsMenu } from "@/components/notifications-menu"
import { HeaderUserMenu } from "@/components/header-user-menu"
import { OrganizationSelector } from "@/components/organization-selector"
import { ComplyzLogo } from "@/components/complyz-logo"
import { Separator } from "@/components/ui/separator"
import { api } from "@/lib/api-client"
import type { InsightsData } from "@/lib/types"

/**
 * Wrap route content with module navigation, command search, and account controls.
 * Fetch insights once for navigation badges; a failed request leaves badges absent.
 */
export function AppShellClient({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const router = useRouter()
  const activeModule = resolveModule(pathname)
  const [insights, setInsights] = React.useState<InsightsData | null>(null)

  const currentUrl = React.useMemo(
    () =>
      typeof window !== "undefined"
        ? window.location.pathname + window.location.search
        : "",
    // eslint-disable-next-line react-hooks/exhaustive-deps -- recalculate when Next.js pathname changes
    [pathname]
  )

  React.useEffect(() => {
    let mounted = true
    api
      .getInsights()
      .then((data) => {
        if (mounted) setInsights(data)
      })
      .catch(() => {})
    return () => {
      mounted = false
    }
  }, [])

  const commandGroups: CommandSearchGroup[] = React.useMemo(
    () => [
      {
        id: "navigate",
        label: "Navigate",
        items: [
          {
            id: "overview",
            label: "Overview",
            icon: <LayoutDashboard />,
            keywords: ["dashboard", "home"],
            onSelect: () => router.push("/"),
          },
          {
            id: "insights",
            label: "Insights",
            icon: <BarChart3 />,
            keywords: ["metrics", "insights", "coverage"],
            onSelect: () => router.push("/insights"),
          },
          {
            id: "clients",
            label: "Customers",
            icon: <Users />,
            keywords: ["customers", "accounts"],
            onSelect: () => router.push("/clients"),
          },
          {
            id: "flags",
            label: "Alertas",
            icon: <AlertTriangle />,
            keywords: ["alerts", "screening"],
            onSelect: () => router.push("/flags"),
          },
          {
            id: "reports",
            label: "Reports",
            icon: <FileText />,
            onSelect: () => router.push("/reports"),
          },
          {
            id: "settings",
            label: "Settings",
            icon: <Settings />,
            onSelect: () => router.push("/settings"),
          },
        ],
      },
      {
        id: "actions",
        label: "Actions",
        items: [
          {
            id: "import",
            label: "Import transactions",
            icon: <UploadCloud />,
            shortcut: "⌘I",
            keywords: ["upload", "file", "csv"],
            onSelect: () => router.push("/import"),
          },
          {
            id: "sanctions",
            label: "Review sanctions matches",
            icon: <AlertTriangle />,
            onSelect: () => router.push("/flags?type=sanctions"),
          },
        ],
      },
    ],
    [router]
  )

  return (
    <AppShell>
      <AppShellNavbar
        startContent={
          <div className="flex items-center gap-2">
            <Link
              href="/"
              aria-label="Complyz"
              className="transition-opacity hover:opacity-85"
            >
              <ComplyzLogo size="sm" priority />
            </Link>
            <Separator
              orientation="vertical"
              className="h-4 w-[1px] bg-muted-foreground/75"
            />
            <OrganizationSelector
              organizations={[{ id: "default", name: "Organization", short: "ORG" }]}
            />
          </div>
        }
        centerContent={<CommandSearch groups={commandGroups} />}
        endContent={
          <>
            <NotificationsMenu />
            <HeaderUserMenu />
          </>
        }
      />

      <AppShellBody>
        <AppShellRail>
          {navModules.map((module) => (
            <AppShellRailItem
              key={module.id}
              icon={<module.icon />}
              label={module.label}
              isActive={module.id === activeModule.id}
              onClick={() => router.push(module.href)}
            />
          ))}
        </AppShellRail>

        <AppShellMain>
          <AppShellSidebar>
            <AppShellSidebarHeader
              icon={<activeModule.icon />}
              title={activeModule.title}
              description={activeModule.description}
            />

            <AppShellNav>
              {activeModule.groups.map((group, groupIndex) => (
                <AppShellNavGroup key={group.label ?? groupIndex} label={group.label}>
                  {group.items.map((item) => {
                    const isActive =
                      item.url === "/"
                        ? pathname === "/"
                        : pathname === item.url || pathname.startsWith(item.url + "/")
                    const badge = item.badgeKey ? insights?.[item.badgeKey] : undefined

                    if (item.items?.length) {
                      return (
                        <AppShellNavItemCollapsible
                          key={item.title}
                          label={item.title}
                          icon={<item.icon />}
                          isActive={isActive}
                        >
                          {item.items.map((sub) => (
                            <AppShellNavSubItem
                              key={sub.url}
                              href={sub.url}
                              onClick={(e) => {
                                e.preventDefault()
                                router.push(sub.url)
                              }}
                              isActive={currentUrl === sub.url}
                            >
                              {sub.title}
                            </AppShellNavSubItem>
                          ))}
                        </AppShellNavItemCollapsible>
                      )
                    }

                    return (
                      <AppShellNavItem
                        key={item.title}
                        href={item.url}
                        onClick={(e) => {
                          e.preventDefault()
                          router.push(item.url)
                        }}
                        icon={<item.icon />}
                        isActive={isActive}
                      >
                        <span className="flex w-full items-center justify-between gap-2">
                          <span className="truncate">{item.title}</span>
                          {typeof badge === "number" && badge > 0 && (
                            <span className="rounded-full border border-border bg-muted px-1.5 py-0.5 font-mono text-[10px] font-semibold text-muted-foreground">
                              {badge}
                            </span>
                          )}
                        </span>
                      </AppShellNavItem>
                    )
                  })}
                </AppShellNavGroup>
              ))}
            </AppShellNav>

            <AppShellNavFooter>
              <AppShellNavItem
                href="/settings"
                onClick={(e) => {
                  e.preventDefault()
                  router.push("/settings")
                }}
                icon={<Settings />}
                isActive={pathname.startsWith("/settings")}
              >
                Settings
              </AppShellNavItem>
            </AppShellNavFooter>
          </AppShellSidebar>

          <AppShellContent>{children}</AppShellContent>
        </AppShellMain>
      </AppShellBody>
    </AppShell>
  )
}
