"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { LogOut, Settings } from "lucide-react"
import { env } from "@/lib/env"

import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Skeleton } from "@/components/ui/skeleton"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { api, type CurrentUser } from "@/lib/api-client"

function getInitials(name: string): string {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase()
}

/**
 * Load the current user and show account actions; failed user lookup leaves
 * the loading placeholder visible. Sign-out clears local tokens and redirects
 * to the configured auth URL or reloads the page. Logout request failures are
 * ignored by the API client; local storage errors can still reject sign-out.
 */
export function HeaderUserMenu() {
  const router = useRouter()
  const [user, setUser] = useState<CurrentUser | null>(null)

  useEffect(() => {
    let cancelled = false
    api
      .getMe()
      .then((data) => {
        if (!cancelled) setUser(data)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  const handleLogout = async () => {
    await api.logout()
    if (env.NEXT_PUBLIC_AUTH_URL) {
      window.location.href = env.NEXT_PUBLIC_AUTH_URL
    } else {
      window.location.reload()
    }
  }

  if (!user) {
    return <Skeleton className="h-7 w-7 rounded-full bg-muted-foreground/25" />
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          className="rounded-full transition-opacity hover:opacity-80 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          aria-label="Account menu"
        >
          <Avatar className="h-7 w-7 rounded-full border border-border">
            <AvatarFallback className="rounded-full bg-nav-teal-bg text-[11px] font-semibold text-nav-teal-text">
              {getInitials(user.fullName)}
            </AvatarFallback>
          </Avatar>
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent
        className="w-56 rounded-lg border-border p-1.5 shadow-md"
        align="end"
        sideOffset={8}
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
            className="cursor-pointer gap-2 py-1.5 text-xs"
            onClick={() => router.push("/settings")}
          >
            <Settings className="h-3.5 w-3.5 text-muted-foreground" />
            Settings
          </DropdownMenuItem>
        </DropdownMenuGroup>

        <DropdownMenuSeparator className="my-1" />

        <DropdownMenuItem
          className="cursor-pointer gap-2 py-1.5 text-xs text-destructive focus:text-destructive"
          onClick={handleLogout}
        >
          <LogOut className="h-3.5 w-3.5" />
          Sign out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
