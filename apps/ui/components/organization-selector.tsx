"use client"

import * as React from "react"
import { Check, ChevronDown, Search } from "lucide-react"
import { cn } from "@/lib/utils"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { ScrollArea } from "@/components/ui/scroll-area"

export interface Organization {
  id: string
  name: string
  /** Initials or a compact icon displayed in the leading slot. */
  short?: string
  icon?: React.ReactNode
}

interface OrganizationSelectorProps {
  organizations: Organization[]
  value?: string
  defaultValue?: string
  onValueChange?: (organizationId: string) => void
  placeholder?: string
  searchPlaceholder?: string
  emptySearchText?: string
  loading?: boolean
  maxWidth?: string
}

function initialsOf(name: string) {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase()
}

export function OrganizationSelector({
  organizations,
  value,
  defaultValue,
  onValueChange,
  placeholder = "Select Organization",
  searchPlaceholder = "Search Organizations...",
  emptySearchText = "No Organizations found.",
  loading = false,
  maxWidth = "160px",
}: OrganizationSelectorProps) {
  const [open, setOpen] = React.useState(false)
  const [query, setQuery] = React.useState("")
  const [internalValue, setInternalValue] = React.useState(
    defaultValue ?? organizations[0]?.id
  )

  const selectedId = value ?? internalValue
  const selected = organizations.find((org) => org.id === selectedId)

  const handleOpenChange = React.useCallback((next: boolean) => {
    setOpen(next)
    if (!next) setQuery("")
  }, [])

  const filtered = React.useMemo(
    () =>
      organizations.filter((org) =>
        `${org.name} ${org.id}`.toLowerCase().includes(query.toLowerCase())
      ),
    [organizations, query]
  )

  const handleSelect = (id: string) => {
    if (value === undefined) setInternalValue(id)
    onValueChange?.(id)
    setOpen(false)
  }

  return (
    <DropdownMenu open={open} onOpenChange={handleOpenChange}>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          disabled={loading}
          className="text-foreground hover:bg-background/60 focus-visible:ring-ring flex h-8 items-center gap-2 rounded-md px-2 text-sm transition-colors focus-visible:outline-none focus-visible:ring-1 disabled:opacity-50"
        >
          <span className="shadow-2xs border-border/80 bg-background text-foreground dark:border-muted-foreground/30 dark:bg-muted-foreground/10 flex size-5 shrink-0 items-center justify-center rounded border text-[10px] font-semibold">
            {selected?.icon ??
              selected?.short ??
              (selected ? initialsOf(selected.name) : "—")}
          </span>
          <span className="truncate" style={{ maxWidth }}>
            {selected?.name ?? placeholder}
          </span>
          <ChevronDown className="text-muted-foreground size-3.5 shrink-0" />
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent
        align="start"
        sideOffset={8}
        className="w-64 overflow-hidden p-0"
      >
        <div className="p-1.5 pb-1">
          <div className="border-border/80 bg-muted/60 flex items-center gap-2 rounded-md border px-2.5 transition-colors">
            <Search className="text-muted-foreground size-3.5 shrink-0" />
            <input
              autoFocus
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => e.stopPropagation()}
              placeholder={searchPlaceholder}
              className="text-foreground placeholder:text-muted-foreground h-8 flex-1 bg-transparent text-xs outline-none"
            />
          </div>
        </div>

        <ScrollArea className="max-h-64">
          <div className="p-1.5">
            {filtered.length === 0 && (
              <p className="text-muted-foreground px-2 py-6 text-center text-xs">
                {emptySearchText}
              </p>
            )}

            {filtered.map((org) => {
              const isSelected = org.id === selectedId
              return (
                <button
                  key={org.id}
                  type="button"
                  onClick={() => handleSelect(org.id)}
                  className={cn(
                    "flex w-full cursor-pointer items-center gap-2.5 rounded-lg px-2 py-2 text-xs transition-colors",
                    isSelected
                      ? "bg-muted text-foreground"
                      : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
                  )}
                >
                  <span className="shadow-2xs border-border/80 bg-background text-foreground dark:border-muted-foreground/25 dark:bg-muted-foreground/10 flex size-5 shrink-0 items-center justify-center rounded border text-[10px] font-semibold">
                    {org.icon ?? org.short ?? initialsOf(org.name)}
                  </span>
                  <span className="flex-1 truncate text-left">{org.name}</span>
                  {isSelected && <Check className="size-3.5 shrink-0" />}
                </button>
              )
            })}
          </div>
        </ScrollArea>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
