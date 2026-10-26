"use client"

import type React from "react"
import { useEffect } from "react"
import { AppShellClient } from "@/components/shell/app-shell-client"
import { env } from "@/lib/env"

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  useEffect(() => {
    const token =
      typeof window !== "undefined" ? localStorage.getItem("comply_token") : null
    if (!token && env.NEXT_PUBLIC_AUTH_URL && typeof window !== "undefined") {
      try {
        const authUrl = new URL(env.NEXT_PUBLIC_AUTH_URL, window.location.origin)
        if (authUrl.origin !== window.location.origin) {
          window.location.href = env.NEXT_PUBLIC_AUTH_URL
          return
        }
      } catch {
        // Leave access unchanged when the configured URL is invalid or relative.
      }
    }
  }, [])

  return <AppShellClient>{children}</AppShellClient>
}
