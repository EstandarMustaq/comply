"use client"

import type * as React from "react"

/** Render the page heading and its optional description and action. */
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
          <h1 className="text-foreground truncate text-2xl font-semibold tracking-tight">
            {title}
          </h1>
        )}
        {description && (
          <p className="text-muted-foreground mt-0.5 text-sm">{description}</p>
        )}
      </div>
      {action && <div className="flex shrink-0 items-center gap-2">{action}</div>}
    </div>
  )
}
