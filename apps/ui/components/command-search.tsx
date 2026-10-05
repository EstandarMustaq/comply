"use client"

import * as React from "react"
import { Search } from "lucide-react"
import { cn } from "@/lib/utils"
import { Kbd } from "@/components/ui/kbd"
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog"
import { ScrollArea } from "@/components/ui/scroll-area"

export interface CommandSearchItem {
  id: string
  label: string
  icon?: React.ReactNode
  shortcut?: string
  keywords?: string[]
  onSelect?: () => void
}

export interface CommandSearchGroup {
  id: string
  label: string
  items: CommandSearchItem[]
}

interface CommandSearchProps {
  placeholder?: string
  emptyText?: string
  groups?: CommandSearchGroup[]
  onSelect?: (item: CommandSearchItem) => void
  triggerWidth?: "sm" | "md" | "lg" | "full"
  showTrigger?: boolean
}

const triggerWidths = {
  sm: "w-48 md:w-64",
  md: "w-64 md:w-80",
  lg: "w-80 md:w-96",
  full: "w-full max-w-md",
} as const

function matches(item: CommandSearchItem, query: string) {
  if (!query) return true
  const haystack = [item.label, ...(item.keywords ?? [])].join(" ").toLowerCase()
  return haystack.includes(query.toLowerCase())
}

export function CommandSearch({
  placeholder = "Search...",
  emptyText = "No results.",
  groups = [],
  onSelect,
  triggerWidth = "md",
  showTrigger = true,
}: CommandSearchProps) {
  const [open, setOpen] = React.useState(false)
  const [query, setQuery] = React.useState("")
  const [activeIndex, setActiveIndex] = React.useState(0)

  // Open the command palette with Cmd/Ctrl + K.
  React.useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key.toLowerCase() === "k" && (e.metaKey || e.ctrlKey)) {
        e.preventDefault()
        setOpen((prev) => {
          const next = !prev
          if (!next) {
            setQuery("")
            setActiveIndex(0)
          }
          return next
        })
      }
    }
    document.addEventListener("keydown", handleKeyDown)
    return () => document.removeEventListener("keydown", handleKeyDown)
  }, [])

  const handleOpenChange = React.useCallback((next: boolean) => {
    setOpen(next)
    if (!next) {
      setQuery("")
      setActiveIndex(0)
    }
  }, [])

  const filteredGroups = React.useMemo(
    () =>
      groups
        .map((group) => ({
          ...group,
          items: group.items.filter((item) => matches(item, query)),
        }))
        .filter((group) => group.items.length > 0),
    [groups, query]
  )

  const flatItems = React.useMemo(
    () => filteredGroups.flatMap((group) => group.items),
    [filteredGroups]
  )

  // Cache each item's global index to avoid mutating state during render.
  const indexById = React.useMemo(() => {
    const map = new Map<string, number>()
    flatItems.forEach((item, index) => map.set(item.id, index))
    return map
  }, [flatItems])

  const runItem = React.useCallback(
    (item: CommandSearchItem) => {
      setOpen(false)
      item.onSelect?.()
      onSelect?.(item)
    },
    [onSelect]
  )

  const handleListKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "ArrowDown") {
      e.preventDefault()
      setActiveIndex((prev) => (prev + 1) % Math.max(flatItems.length, 1))
    } else if (e.key === "ArrowUp") {
      e.preventDefault()
      setActiveIndex((prev) =>
        prev === 0 ? Math.max(flatItems.length - 1, 0) : prev - 1
      )
    } else if (e.key === "Enter") {
      e.preventDefault()
      const item = flatItems[activeIndex]
      if (item) runItem(item)
    }
  }

  return (
    <>
      {showTrigger && (
        <button
          type="button"
          onClick={() => setOpen(true)}
          className={cn(
            "border-border bg-background/60 text-muted-foreground hover:bg-background focus-visible:ring-ring flex h-8 items-center gap-2 rounded-md border px-2.5 text-left text-xs transition-colors focus-visible:outline-none focus-visible:ring-1",
            triggerWidths[triggerWidth]
          )}
        >
          <Search className="size-3.5 shrink-0" />
          <span className="flex-1 truncate">{placeholder}</span>
          <Kbd>⌘K</Kbd>
        </button>
      )}

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent className="top-[20%] max-w-xl translate-y-0 gap-0 overflow-hidden p-0 [&>button]:hidden">
          <DialogTitle className="sr-only">Search</DialogTitle>
          <DialogDescription className="sr-only">
            Search Complyz and jump to a workspace
          </DialogDescription>

          <div className="p-3 pb-2">
            <div className="border-border/80 bg-muted/60 flex items-center gap-2.5 rounded-lg border px-3 transition-colors">
              <Search className="text-muted-foreground size-4 shrink-0" />
              <input
                autoFocus
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value)
                  setActiveIndex(0)
                }}
                onKeyDown={handleListKeyDown}
                placeholder={placeholder}
                className="text-foreground placeholder:text-muted-foreground h-8 flex-1 bg-transparent text-sm outline-none"
              />
              <Kbd>Esc</Kbd>
            </div>
          </div>

          <ScrollArea className="max-h-80">
            <div className="p-2 pt-0">
              {flatItems.length === 0 && (
                <p className="text-muted-foreground px-3 py-6 text-center text-sm">
                  {emptyText}
                </p>
              )}

              {filteredGroups.map((group) => (
                <div key={group.id} className="mb-1 last:mb-0">
                  <div className="text-muted-foreground px-2 py-1.5 text-xs font-medium uppercase tracking-wider">
                    {group.label}
                  </div>
                  <div className="space-y-0.5">
                    {group.items.map((item) => {
                      const index = indexById.get(item.id) ?? -1
                      const isActive = index === activeIndex
                      return (
                        <button
                          key={item.id}
                          type="button"
                          onMouseEnter={() => setActiveIndex(index)}
                          onClick={() => runItem(item)}
                          className={cn(
                            "flex w-full cursor-pointer items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors",
                            isActive
                              ? "bg-muted text-foreground"
                              : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
                          )}
                        >
                          {item.icon && (
                            <span className="size-4 shrink-0 [&>svg]:size-4">
                              {item.icon}
                            </span>
                          )}
                          <span className="flex-1 truncate text-left">{item.label}</span>
                          {item.shortcut && <Kbd>{item.shortcut}</Kbd>}
                        </button>
                      )
                    })}
                  </div>
                </div>
              ))}
            </div>
          </ScrollArea>
        </DialogContent>
      </Dialog>
    </>
  )
}
