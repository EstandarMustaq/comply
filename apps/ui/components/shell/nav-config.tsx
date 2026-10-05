import {
  LayoutDashboard,
  BarChart3,
  Users,
  AlertTriangle,
  UploadCloud,
  FileText,
  Settings,
  ShieldCheck,
  Database,
  type LucideIcon,
} from "lucide-react"

export interface NavSubItem {
  title: string
  url: string
}

export interface NavItem {
  title: string
  url: string
  icon: LucideIcon
  /** Insight count key displayed as a navigation badge. */
  badgeKey?: "pendingFlagsCount" | "sanctionsMatchCount" | "overdueSlaCount"
  items?: NavSubItem[]
}

export interface NavGroup {
  label?: string
  items: NavItem[]
}

export interface NavModule {
  id: string
  label: string
  icon: LucideIcon
  /** Destination opened when the module rail item is selected. */
  href: string
  /** Route prefixes that activate this module. */
  matches: string[]
  title: string
  description?: string
  groups: NavGroup[]
}

export const navModules: NavModule[] = [
  {
    id: "compliance",
    label: "Compliance",
    icon: ShieldCheck,
    href: "/",
    matches: ["/", "/clients", "/flags", "/insights"],
    title: "Compliance",
    description: "Monitoring and screening",
    groups: [
      {
        items: [
          { title: "Overview", url: "/", icon: LayoutDashboard },
          { title: "Insights", url: "/insights", icon: BarChart3 },
        ],
      },
      {
        label: "Monitoring",
        items: [
          {
            title: "Customers",
            url: "/clients",
            icon: Users,
            items: [
              { title: "All customers", url: "/clients" },
              { title: "High risk", url: "/clients?risk=ALTO" },
              { title: "Enhanced due diligence", url: "/clients?regime=NORMAL" },
            ],
          },
          {
            title: "Alerts",
            url: "/flags",
            icon: AlertTriangle,
            badgeKey: "pendingFlagsCount",
            items: [
              { title: "Needs review", url: "/flags" },
              { title: "Sanctions", url: "/flags?type=sanctions" },
              { title: "Overdue", url: "/flags?sla=overdue" },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "data",
    label: "Data",
    icon: Database,
    href: "/import",
    matches: ["/import"],
    title: "Data",
    description: "Transaction imports",
    groups: [
      {
        items: [
          {
            title: "Import",
            url: "/import",
            icon: UploadCloud,
            items: [
              { title: "New file", url: "/import" },
              { title: "History", url: "/import?view=history" },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "reports",
    label: "Reports",
    icon: FileText,
    href: "/reports",
    matches: ["/reports"],
    title: "Reports",
    description: "Reports and records",
    groups: [
      {
        items: [
          {
            title: "Reports",
            url: "/reports",
            icon: FileText,
            items: [
              { title: "All reports", url: "/reports" },
              { title: "Suspicious activity", url: "/reports?type=STR" },
              { title: "Cash transactions", url: "/reports?type=CTR" },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "settings",
    label: "Settings",
    icon: Settings,
    href: "/settings",
    matches: ["/settings"],
    title: "Settings",
    description: "Rules and activity",
    groups: [
      {
        items: [
          {
            title: "Settings",
            url: "/settings",
            icon: Settings,
            items: [
              { title: "Thresholds", url: "/settings" },
              { title: "Activity history", url: "/settings?tab=audit" },
              { title: "Team", url: "/settings?tab=team" },
            ],
          },
        ],
      },
    ],
  },
]

export function resolveModule(pathname: string): NavModule {
  const exact = navModules.find((module) =>
    module.matches.some(
      (match) => match !== "/" && (pathname === match || pathname.startsWith(match + "/"))
    )
  )
  if (exact) return exact

  const fallback = navModules[0]
  if (!fallback) {
    throw new Error("navModules cannot be empty")
  }
  return fallback
}
