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
  /** Chave de contagem vinda dos insights, mostrada como distintivo */
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
  /** Primeira rota do módulo, usada ao clicar no rail */
  href: string
  /** Prefixos de rota que activam este módulo */
  matches: string[]
  title: string
  description?: string
  groups: NavGroup[]
}

export const navModules: NavModule[] = [
  {
    id: "compliance",
    label: "Conformidade",
    icon: ShieldCheck,
    href: "/",
    matches: ["/", "/clients", "/flags", "/insights"],
    title: "Conformidade",
    description: "Monitorização e triagem",
    groups: [
      {
        items: [
          { title: "Visão geral", url: "/", icon: LayoutDashboard },
          { title: "Indicadores", url: "/insights", icon: BarChart3 },
        ],
      },
      {
        label: "Monitorização",
        items: [
          {
            title: "Clientes",
            url: "/clients",
            icon: Users,
            items: [
              { title: "Todos os clientes", url: "/clients" },
              { title: "Risco elevado", url: "/clients?risk=ALTO" },
              { title: "Regime reforçado", url: "/clients?regime=NORMAL" },
            ],
          },
          {
            title: "Alertas",
            url: "/flags",
            icon: AlertTriangle,
            badgeKey: "pendingFlagsCount",
            items: [
              { title: "Por rever", url: "/flags" },
              { title: "Sanções", url: "/flags?type=sanctions" },
              { title: "Fora de prazo", url: "/flags?sla=overdue" },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "data",
    label: "Dados",
    icon: Database,
    href: "/import",
    matches: ["/import"],
    title: "Dados",
    description: "Entrada de transacções",
    groups: [
      {
        items: [
          {
            title: "Importar",
            url: "/import",
            icon: UploadCloud,
            items: [
              { title: "Novo ficheiro", url: "/import" },
              { title: "Histórico", url: "/import?view=history" },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "reports",
    label: "Relatórios",
    icon: FileText,
    href: "/reports",
    matches: ["/reports"],
    title: "Relatórios",
    description: "Comunicações e arquivo",
    groups: [
      {
        items: [
          {
            title: "Relatórios",
            url: "/reports",
            icon: FileText,
            items: [
              { title: "Todos", url: "/reports" },
              { title: "Operações suspeitas", url: "/reports?type=STR" },
              { title: "Transacções em numerário", url: "/reports?type=CTR" },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "settings",
    label: "Definições",
    icon: Settings,
    href: "/settings",
    matches: ["/settings"],
    title: "Definições",
    description: "Parâmetros e registo",
    groups: [
      {
        items: [
          {
            title: "Definições",
            url: "/settings",
            icon: Settings,
            items: [
              { title: "Limiares", url: "/settings" },
              { title: "Registo de actividade", url: "/settings?tab=audit" },
              { title: "Equipa", url: "/settings?tab=team" },
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
    throw new Error("navModules não pode estar vazio")
  }
  return fallback
}
