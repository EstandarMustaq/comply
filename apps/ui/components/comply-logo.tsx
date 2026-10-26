import type React from "react"
import Image from "next/image"
import { cn } from "@/lib/utils"

export interface ComplyLogoProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: "auto" | "light-bg" | "dark-bg"
  size?: "sm" | "md" | "lg" | "xl"
  priority?: boolean
}

const sizeMap = {
  sm: { height: 24, width: 86, className: "h-6 w-auto" },
  md: { height: 32, width: 114, className: "h-8 w-auto" },
  lg: { height: 40, width: 143, className: "h-10 w-auto" },
  xl: { height: 48, width: 172, className: "h-12 w-auto" },
}

export function ComplyLogo({
  variant = "auto",
  size = "md",
  priority = false,
  className,
  ...props
}: ComplyLogoProps) {
  const config = sizeMap[size]

  return (
    <div className={cn("inline-flex select-none items-center", className)} {...props}>
      {(variant === "auto" || variant === "light-bg") && (
        <Image
          src="/comply-logo-dark-text.svg"
          alt="Comply"
          width={config.width * 2}
          height={config.height * 2}
          priority={priority}
          className={cn(
            "object-contain transition-opacity duration-200",
            config.className,
            variant === "auto" ? "block dark:hidden" : "block"
          )}
          style={{ imageRendering: "-webkit-optimize-contrast" }}
        />
      )}

      {(variant === "auto" || variant === "dark-bg") && (
        <Image
          src="/comply-logo-light-text.svg"
          alt="Comply"
          width={config.width * 2}
          height={config.height * 2}
          priority={priority}
          className={cn(
            "object-contain transition-opacity duration-200",
            config.className,
            variant === "auto" ? "hidden dark:block" : "block"
          )}
          style={{ imageRendering: "-webkit-optimize-contrast" }}
        />
      )}
    </div>
  )
}
