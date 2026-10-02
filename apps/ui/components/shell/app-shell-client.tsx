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
        label: "Navegar",
        items: [
          {
            id: "overview",
            label: "Visão geral",
            icon: <LayoutDashboard />,
            keywords: ["dashboard", "inicio"],
            onSelect: () => router.push("/"),
          },
          {
            id: "insights",
            label: "Indicadores",
            icon: <BarChart3 />,
            keywords: ["metricas", "indicadores", "cobertura"],
            onSelect: () => router.push("/insights"),
          },
          {
            id: "clients",
            label: "Clientes",
            icon: <Users />,
            keywords: ["titulares", "contas"],
            onSelect: () => router.push("/clients"),
          },
          {
            id: "flags",
            label: "Alertas",
            icon: <AlertTriangle />,
            keywords: ["sinalizacoes", "triagem"],
            onSelect: () => router.push("/flags"),
          },
          {
            id: "reports",
            label: "Relatórios",
            icon: <FileText />,
            onSelect: () => router.push("/reports"),
          },
          {
            id: "settings",
            label: "Definições",
            icon: <Settings />,
            onSelect: () => router.push("/settings"),
          },
        ],
      },
      {
        id: "actions",
        label: "Acções",
        items: [
          {
            id: "import",
            label: "Importar transacções",
            icon: <UploadCloud />,
            shortcut: "⌘I",
            keywords: ["carregar", "ficheiro", "csv"],
            onSelect: () => router.push("/import"),
          },
          {
            id: "sanctions",
            label: "Rever correspondências de sanções",
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
              className="bg-muted-foreground/75 h-4 w-[1px]"
            />
            <OrganizationSelector
              organizations={[{ id: "default", name: "Instituição", short: "IN" }]}
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
                            <span className="border-border bg-muted text-muted-foreground rounded-full border px-1.5 py-0.5 font-mono text-[10px] font-semibold">
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
                Definições
              </AppShellNavItem>
            </AppShellNavFooter>
          </AppShellSidebar>

          <AppShellContent>{children}</AppShellContent>
        </AppShellMain>
      </AppShellBody>
    </AppShell>
  )
}
