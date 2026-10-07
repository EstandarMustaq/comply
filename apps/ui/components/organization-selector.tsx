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
          className="flex h-8 items-center gap-2 rounded-md px-2 text-sm text-foreground transition-colors hover:bg-background/60 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:opacity-50"
        >
          <span className="shadow-2xs flex size-5 shrink-0 items-center justify-center rounded border border-border/80 bg-background text-[10px] font-semibold text-foreground dark:border-muted-foreground/30 dark:bg-muted-foreground/10">
            {selected?.icon ??
              selected?.short ??
              (selected ? initialsOf(selected.name) : "—")}
          </span>
          <span className="truncate" style={{ maxWidth }}>
            {selected?.name ?? placeholder}
          </span>
          <ChevronDown className="size-3.5 shrink-0 text-muted-foreground" />
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent
        align="start"
        sideOffset={8}
        className="w-64 overflow-hidden p-0"
      >
        <div className="p-1.5 pb-1">
          <div className="flex items-center gap-2 rounded-md border border-border/80 bg-muted/60 px-2.5 transition-colors">
            <Search className="size-3.5 shrink-0 text-muted-foreground" />
            <input
              autoFocus
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => e.stopPropagation()}
              placeholder={searchPlaceholder}
              className="h-8 flex-1 bg-transparent text-xs text-foreground outline-none placeholder:text-muted-foreground"
            />
          </div>
        </div>

        <ScrollArea className="max-h-64">
          <div className="p-1.5">
            {filtered.length === 0 && (
              <p className="px-2 py-6 text-center text-xs text-muted-foreground">
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
                  <span className="shadow-2xs flex size-5 shrink-0 items-center justify-center rounded border border-border/80 bg-background text-[10px] font-semibold text-foreground dark:border-muted-foreground/25 dark:bg-muted-foreground/10">
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
