"use client"

import type React from "react"
import { useEffect } from "react"
import { AppShellClient } from "@/components/shell/app-shell-client"
import { env } from "@/lib/env"

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  useEffect(() => {
    const token =
      typeof window !== "undefined" ? localStorage.getItem("fincomp_token") : null
    if (!token && env.NEXT_PUBLIC_AUTH_URL && typeof window !== "undefined") {
      try {
        const authUrl = new URL(env.NEXT_PUBLIC_AUTH_URL, window.location.origin)
        if (authUrl.origin !== window.location.origin) {
          window.location.href = env.NEXT_PUBLIC_AUTH_URL
          return
        }
      } catch {
        // URL inválida ou relativa: mantém acesso liberado
      }
    }
  }, [])

  return <AppShellClient>{children}</AppShellClient>
}
