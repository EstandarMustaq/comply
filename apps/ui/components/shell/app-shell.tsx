"use client"

import * as React from "react"
import { ChevronDown, PanelLeftClose, PanelLeftOpen } from "lucide-react"
import { cn } from "@/lib/utils"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { ScrollArea } from "@/components/ui/scroll-area"

type AppShellContextProps = {
  sidebarOpen: boolean
  setSidebarOpen: (open: boolean) => void
  toggleSidebar: () => void
  mobileDrawerOpen: boolean
  setMobileDrawerOpen: (open: boolean) => void
  toggleMobileDrawer: () => void
  railContent: React.ReactNode
  setRailContent: (content: React.ReactNode) => void
  sidebarContent: React.ReactNode
  setSidebarContent: (content: React.ReactNode) => void
}

const AppShellContext = React.createContext<AppShellContextProps | null>(null)

export function useAppShell() {
  const context = React.useContext(AppShellContext)
  if (!context) {
    throw new Error("useAppShell must be used within an AppShell.")
  }
  return context
}

/**
 * Provide shared desktop sidebar and mobile drawer state for shell children.
 * defaultSidebarOpen seeds desktop state only. Cmd/Ctrl+Backslash toggles the
 * mobile drawer below 768px and the desktop sidebar at wider viewport widths.
 */
export function AppShell({
  defaultSidebarOpen = true,
  children,
  ...props
}: Omit<React.ComponentProps<"div">, "className"> & {
  defaultSidebarOpen?: boolean
}) {
  const [sidebarOpen, setSidebarOpen] = React.useState(defaultSidebarOpen)
  const [mobileDrawerOpen, setMobileDrawerOpen] = React.useState(false)
  const [railContent, setRailContent] = React.useState<React.ReactNode>(null)
  const [sidebarContent, setSidebarContent] = React.useState<React.ReactNode>(null)

  const toggleSidebar = React.useCallback(() => setSidebarOpen((prev) => !prev), [])
  const toggleMobileDrawer = React.useCallback(
    () => setMobileDrawerOpen((prev) => !prev),
    []
  )

  // Toggle the sidebar with Cmd/Ctrl + \.
  React.useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "\\" && (e.metaKey || e.ctrlKey)) {
        e.preventDefault()
        const isMobile = window.matchMedia("(max-width: 767px)").matches
        if (isMobile) {
          setMobileDrawerOpen((prev) => !prev)
        } else {
          setSidebarOpen((prev) => !prev)
        }
      }
    }
    document.addEventListener("keydown", handleKeyDown)
    return () => document.removeEventListener("keydown", handleKeyDown)
  }, [])

  const value = React.useMemo<AppShellContextProps>(
    () => ({
      sidebarOpen,
      setSidebarOpen,
      toggleSidebar,
      mobileDrawerOpen,
      setMobileDrawerOpen,
      toggleMobileDrawer,
      railContent,
      setRailContent,
      sidebarContent,
      setSidebarContent,
    }),
    [
      sidebarOpen,
      toggleSidebar,
      mobileDrawerOpen,
      toggleMobileDrawer,
      railContent,
      sidebarContent,
    ]
  )

  return (
    <AppShellContext.Provider value={value}>
      <TooltipProvider delayDuration={200}>
        <div
          data-slot="app-shell"
          data-sidebar-open={sidebarOpen}
          className="flex h-svh w-full flex-col overflow-hidden bg-muted"
          {...props}
        >
          {children}
        </div>
      </TooltipProvider>
    </AppShellContext.Provider>
  )
}

/**
 * Render header slots with a mobile menu toggle; centerContent is hidden below 768px.
 * @throws {Error} When rendered outside AppShell.
 */
export function AppShellNavbar({
  startContent,
  centerContent,
  endContent,
  children,
  ...props
}: Omit<React.ComponentProps<"header">, "className"> & {
  startContent?: React.ReactNode
  centerContent?: React.ReactNode
  endContent?: React.ReactNode
}) {
  const { toggleMobileDrawer } = useAppShell()

  return (
    <header
      data-slot="app-shell-navbar"
      className="relative flex h-14 shrink-0 items-center gap-2 bg-transparent px-4"
      {...props}
    >
      <div className="z-10 flex items-center gap-2">
        <button
          type="button"
          onClick={toggleMobileDrawer}
          className="inline-flex size-8 items-center justify-center rounded-md text-muted-foreground hover:bg-background/60 hover:text-foreground md:hidden"
          aria-label="Open menu"
        >
          <PanelLeftOpen className="size-4" />
        </button>
        {startContent}
      </div>

      {centerContent && (
        <div className="pointer-events-none absolute inset-0 hidden items-center justify-center md:flex">
          <div className="pointer-events-auto">{centerContent}</div>
        </div>
      )}

      <div className="z-10 ml-auto flex items-center gap-1.5">{endContent}</div>
      {children}
    </header>
  )
}

/**
 * Render shell children alongside a mobile drawer using registered rail and sidebar
 * content. Clicking the backdrop closes the drawer.
 * @throws {Error} When rendered outside AppShell.
 */
export function AppShellBody({
  children,
  ...props
}: Omit<React.ComponentProps<"div">, "className">) {
  const { mobileDrawerOpen, setMobileDrawerOpen, railContent, sidebarContent } =
    useAppShell()

  return (
    <div
      data-slot="app-shell-body"
      className="flex min-h-0 flex-1 overflow-hidden bg-transparent"
      {...props}
    >
      {/* Render the rail and sidebar together in the mobile drawer. */}
      <div className="md:hidden">
        {mobileDrawerOpen && (
          <div
            className="fixed inset-0 z-40 bg-black/50"
            onClick={() => setMobileDrawerOpen(false)}
            aria-hidden="true"
          />
        )}
        <div
          className={cn(
            "fixed inset-y-0 left-0 z-50 flex transform transition-transform duration-200 ease-in-out",
            mobileDrawerOpen ? "translate-x-0" : "-translate-x-full"
          )}
        >
          {railContent && (
            <div className="flex w-14 shrink-0 flex-col items-center gap-1 border-r border-border bg-muted py-3">
              {railContent}
            </div>
          )}
          <div className="flex w-64 flex-col bg-background p-2">{sidebarContent}</div>
        </div>
      </div>

      {children}
    </div>
  )
}

export function AppShellMain({
  children,
  ...props
}: Omit<React.ComponentProps<"div">, "className">) {
  return (
    <div
      data-slot="app-shell-main"
      className="mb-2 ml-2 mr-2 flex min-h-0 flex-1 overflow-hidden rounded-xl border border-border bg-background"
      {...props}
    >
      {children}
    </div>
  )
}

type RailIndicatorContextProps = {
  registerItem: (id: string, element: HTMLElement | null) => void
  activeId: string | null
  setActiveId: (id: string | null) => void
}

const RailIndicatorContext = React.createContext<RailIndicatorContextProps | null>(null)

/**
 * Render the desktop module rail and register its children for the mobile drawer.
 * showSidebarToggle controls the desktop collapse button; unmounting clears the
 * registered mobile rail content.
 * @throws {Error} When rendered outside AppShell.
 */
export function AppShellRail({
  showSidebarToggle = true,
  children,
  ...props
}: Omit<React.ComponentProps<"div">, "className"> & {
  showSidebarToggle?: boolean
}) {
  const { sidebarOpen, toggleSidebar, setRailContent } = useAppShell()
  const itemsContainerRef = React.useRef<HTMLDivElement>(null)
  const indicatorRef = React.useRef<HTMLSpanElement>(null)
  const itemsRef = React.useRef<Map<string, HTMLElement>>(new Map())
  const [activeId, setActiveId] = React.useState<string | null>(null)

  React.useEffect(() => {
    setRailContent(children)
    return () => setRailContent(null)
  }, [children, setRailContent])

  const registerItem = React.useCallback((id: string, element: HTMLElement | null) => {
    if (element) itemsRef.current.set(id, element)
    else itemsRef.current.delete(id)
  }, [])

  const updateIndicator = React.useCallback(() => {
    const indicator = indicatorRef.current
    const container = itemsContainerRef.current
    if (!indicator || !container) return

    const activeElement = activeId ? itemsRef.current.get(activeId) : null
    if (!activeElement) {
      indicator.style.opacity = "0"
      return
    }

    const containerRect = container.getBoundingClientRect()
    const itemRect = activeElement.getBoundingClientRect()
    indicator.style.top = `${itemRect.top - containerRect.top + itemRect.height / 2 - 12}px`
    indicator.style.opacity = "1"
  }, [activeId])

  React.useEffect(() => {
    updateIndicator()
  }, [updateIndicator])

  React.useEffect(() => {
    window.addEventListener("resize", updateIndicator)
    return () => window.removeEventListener("resize", updateIndicator)
  }, [updateIndicator])

  const value = React.useMemo<RailIndicatorContextProps>(
    () => ({ registerItem, activeId, setActiveId }),
    [registerItem, activeId]
  )

  return (
    <RailIndicatorContext.Provider value={value}>
      <div
        data-slot="app-shell-rail"
        className="hidden w-14 shrink-0 flex-col items-center gap-1 py-2 md:flex"
        {...props}
      >
        <div
          ref={itemsContainerRef}
          className="relative flex flex-1 flex-col items-center gap-1"
        >
          <span
            ref={indicatorRef}
            className="pointer-events-none absolute right-0 -mr-2 h-6 w-1 rounded-full bg-primary transition-all duration-300 ease-out"
            style={{ opacity: 0, top: 0 }}
            aria-hidden="true"
          />
          {children}
        </div>

        {showSidebarToggle && (
          <button
            type="button"
            onClick={toggleSidebar}
            className="flex size-10 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-background/60 hover:text-foreground"
            aria-label={sidebarOpen ? "Collapse sidebar" : "Expand sidebar"}
          >
            {sidebarOpen ? (
              <PanelLeftClose className="size-5" />
            ) : (
              <PanelLeftOpen className="size-5" />
            )}
          </button>
        )}
      </div>
    </RailIndicatorContext.Provider>
  )
}

export function AppShellRailItem({
  isActive,
  icon,
  label,
  ...props
}: Omit<React.ComponentProps<"button">, "className"> & {
  isActive?: boolean
  icon: React.ReactNode
  label?: string
}) {
  const buttonRef = React.useRef<HTMLButtonElement>(null)
  const context = React.useContext(RailIndicatorContext)
  const itemId = React.useId()

  React.useEffect(() => {
    context?.registerItem(itemId, buttonRef.current)
    return () => context?.registerItem(itemId, null)
  }, [context, itemId])

  React.useEffect(() => {
    if (isActive) context?.setActiveId(itemId)
  }, [isActive, context, itemId])

  const button = (
    <button
      ref={buttonRef}
      type="button"
      data-slot="app-shell-rail-item"
      data-active={isActive}
      className={cn(
        "flex size-10 cursor-pointer items-center justify-center rounded-lg transition-all duration-200",
        isActive
          ? "bg-primary text-primary-foreground shadow-md"
          : "text-muted-foreground hover:bg-accent hover:text-foreground hover:shadow active:scale-95 dark:hover:bg-muted"
      )}
      aria-label={label}
      {...props}
    >
      <span className="[&>svg]:size-5">{icon}</span>
    </button>
  )

  if (!label) return button

  return (
    <Tooltip>
      <TooltipTrigger asChild>{button}</TooltipTrigger>
      <TooltipContent side="right" sideOffset={8}>
        {label}
      </TooltipContent>
    </Tooltip>
  )
}

/**
 * Render the collapsible desktop sidebar and register its children for the mobile
 * drawer. Unmounting clears the registered mobile sidebar content.
 * @throws {Error} When rendered outside AppShell.
 */
export function AppShellSidebar({
  children,
  ...props
}: Omit<React.ComponentProps<"aside">, "className">) {
  const { sidebarOpen, setSidebarContent } = useAppShell()
  const isCollapsed = !sidebarOpen

  React.useEffect(() => {
    setSidebarContent(children)
    return () => setSidebarContent(null)
  }, [children, setSidebarContent])

  return (
    <aside
      data-slot="app-shell-sidebar"
      data-collapsed={isCollapsed}
      className={cn(
        "hidden shrink-0 overflow-hidden bg-background transition-[width,padding] duration-200 ease-in-out md:flex md:flex-col",
        isCollapsed ? "w-0 border-0 p-0" : "w-64 p-2"
      )}
      {...props}
    >
      {/* Keep the inner width fixed to prevent text compression during transitions. */}
      <div
        className={cn(
          "flex h-full w-60 flex-col transition-opacity duration-100",
          isCollapsed ? "opacity-0" : "opacity-100 delay-75"
        )}
      >
        {children}
      </div>
    </aside>
  )
}

export function AppShellSidebarHeader({
  icon,
  title,
  description,
  action,
  children,
  ...props
}: Omit<React.ComponentProps<"div">, "className"> & {
  icon?: React.ReactNode
  title: string
  description?: string
  action?: React.ReactNode
}) {
  const isSimple = !icon && !action && !children
  return (
    <div
      data-slot="app-shell-sidebar-header"
      className={cn(
        "mb-2 flex items-center border-b border-border px-2",
        isSimple ? "py-3" : "gap-3 py-2"
      )}
      {...props}
    >
      {icon && (
        <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted text-foreground [&>svg]:size-4">
          {icon}
        </span>
      )}
      <div className="min-w-0 flex-1">
        <div
          className={cn(
            "truncate text-foreground",
            isSimple ? "text-base" : "text-sm font-medium"
          )}
        >
          {title}
        </div>
        {description && (
          <div className="truncate text-xs text-muted-foreground">{description}</div>
        )}
      </div>
      {action && <div className="shrink-0">{action}</div>}
      {children}
    </div>
  )
}

export function AppShellContent({
  children,
  ...props
}: Omit<React.ComponentProps<"main">, "className">) {
  return (
    <main
      data-slot="app-shell-content"
      className="flex min-h-0 flex-1 flex-col overflow-hidden border-l border-border bg-background"
      {...props}
    >
      <ScrollArea className="h-full w-full flex-1">
        <div className="p-4 md:p-6">{children}</div>
      </ScrollArea>
    </main>
  )
}

export function AppShellNav({
  children,
  ...props
}: Omit<React.ComponentProps<"nav">, "className">) {
  return (
    <ScrollArea className="flex-1">
      <nav data-slot="app-shell-nav" className="space-y-4 py-2 pr-2" {...props}>
        {children}
      </nav>
    </ScrollArea>
  )
}

export function AppShellNavGroup({
  label,
  children,
  ...props
}: Omit<React.ComponentProps<"div">, "className"> & { label?: string }) {
  return (
    <div data-slot="app-shell-nav-group" {...props}>
      {label && (
        <div className="px-2 pb-1 text-xs font-medium uppercase tracking-wider text-muted-foreground">
          {label}
        </div>
      )}
      <div className="space-y-0.5">{children}</div>
    </div>
  )
}

export function AppShellNavItem({
  isActive,
  icon,
  children,
  ...props
}: Omit<React.ComponentProps<"a">, "className"> & {
  isActive?: boolean
  icon?: React.ReactNode
}) {
  return (
    <a
      data-slot="app-shell-nav-item"
      data-active={isActive}
      className={cn(
        "flex w-full cursor-pointer items-center gap-3 rounded-lg px-3 py-2 text-sm",
        "transition-all duration-150 ease-out",
        isActive
          ? "bg-muted font-medium text-foreground"
          : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
      )}
      {...props}
    >
      {icon && <span className="size-4 shrink-0 [&>svg]:size-4">{icon}</span>}
      <span className="flex-1 truncate text-left">{children}</span>
    </a>
  )
}

/**
 * Render a navigation group whose initial expansion uses defaultOpen, then isActive.
 * An active group always reopens if collapsed; becoming inactive does not close it.
 */
export function AppShellNavItemCollapsible({
  isActive,
  defaultOpen,
  icon,
  label,
  children,
  ...props
}: Omit<React.ComponentProps<"div">, "className"> & {
  isActive?: boolean
  defaultOpen?: boolean
  icon?: React.ReactNode
  label: string
}) {
  const [isOpen, setIsOpen] = React.useState(defaultOpen ?? isActive ?? false)

  // Expand an active item before commit so its current route is immediately visible.
  if (isActive && !isOpen) {
    setIsOpen(true)
  }

  return (
    <div data-slot="app-shell-nav-item-collapsible" {...props}>
      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        aria-expanded={isOpen}
        className={cn(
          "flex w-full cursor-pointer items-center gap-3 rounded-lg px-3 py-2 text-sm",
          "transition-all duration-150 ease-out",
          isActive || isOpen
            ? "font-semibold text-foreground"
            : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
        )}
      >
        {icon && <span className="size-4 shrink-0 [&>svg]:size-4">{icon}</span>}
        <span className="flex-1 text-left">{label}</span>
        <ChevronDown
          className={cn(
            "size-4 text-muted-foreground transition-transform duration-200",
            isOpen && "rotate-180"
          )}
        />
      </button>

      <div
        className={cn(
          "overflow-hidden transition-all duration-200 ease-out",
          isOpen ? "max-h-96 opacity-100" : "max-h-0 opacity-0"
        )}
      >
        <div className="ml-2 mt-1 space-y-0.5 rounded-xl bg-muted/40 p-1.5 dark:bg-muted/20">
          {children}
        </div>
      </div>
    </div>
  )
}

export function AppShellNavSubItem({
  isActive,
  children,
  ...props
}: Omit<React.ComponentProps<"a">, "className"> & { isActive?: boolean }) {
  return (
    <a
      data-slot="app-shell-nav-sub-item"
      data-active={isActive}
      className={cn(
        "flex w-full cursor-pointer items-center rounded-lg px-3 py-2 text-sm",
        "transition-all duration-150 ease-out",
        isActive
          ? "bg-background font-medium text-foreground shadow-sm"
          : "text-muted-foreground hover:bg-background/50 hover:text-foreground"
      )}
      {...props}
    >
      {children}
    </a>
  )
}

export function AppShellNavFooter({
  children,
  ...props
}: Omit<React.ComponentProps<"div">, "className">) {
  return (
    <div
      data-slot="app-shell-nav-footer"
      className="mt-auto space-y-1 border-t border-border pt-2"
      {...props}
    >
      {children}
    </div>
  )
}
