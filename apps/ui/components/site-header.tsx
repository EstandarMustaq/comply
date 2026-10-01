"use client"

import type * as React from "react"

/**
 * Cabeçalho de página, dentro da área de conteúdo.
 * A navegação global (logótipo, pesquisa, notificações, conta) vive na navbar
 * do app shell, não aqui.
 */
export function SiteHeader({
  title,
  description,
  action,
}: {
  title?: string
  description?: string
  action?: React.ReactNode
}) {
  if (!title && !description && !action) return null

  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div className="min-w-0">
        {title && (
          <h1 className="truncate text-2xl font-semibold tracking-tight text-foreground">
            {title}
          </h1>
        )}
        {description && (
          <p className="mt-0.5 text-sm text-muted-foreground">{description}</p>
        )}
      </div>
      {action && <div className="flex shrink-0 items-center gap-2">{action}</div>}
    </div>
  )
}
