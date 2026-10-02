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
    return <Skeleton className="bg-muted-foreground/25 h-7 w-7 rounded-full" />
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          className="focus-visible:ring-ring rounded-full transition-opacity hover:opacity-80 focus-visible:outline-none focus-visible:ring-1"
          aria-label="Conta"
        >
          <Avatar className="border-border h-7 w-7 rounded-full border">
            <AvatarFallback className="bg-nav-teal-bg text-nav-teal-text rounded-full text-[11px] font-semibold">
              {getInitials(user.fullName)}
            </AvatarFallback>
          </Avatar>
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent
        className="border-border w-56 rounded-lg p-1.5 shadow-md"
        align="end"
        sideOffset={8}
      >
        <DropdownMenuLabel className="px-2 py-1.5 font-normal">
          <div className="flex flex-col gap-0.5">
            <span className="text-foreground text-xs font-semibold">{user.fullName}</span>
            <span className="text-muted-foreground text-[11px]">{user.email}</span>
          </div>
        </DropdownMenuLabel>

        <DropdownMenuSeparator className="my-1" />

        <DropdownMenuGroup>
          <DropdownMenuItem
            className="cursor-pointer gap-2 py-1.5 text-xs"
            onClick={() => router.push("/settings")}
          >
            <Settings className="text-muted-foreground h-3.5 w-3.5" />
            Definições
          </DropdownMenuItem>
        </DropdownMenuGroup>

        <DropdownMenuSeparator className="my-1" />

        <DropdownMenuItem
          className="text-destructive focus:text-destructive cursor-pointer gap-2 py-1.5 text-xs"
          onClick={handleLogout}
        >
          <LogOut className="h-3.5 w-3.5" />
          Sair
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
