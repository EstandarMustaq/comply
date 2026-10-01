/**
 * Type-safe environment variable access for Fincomp System Design.
 * Ensures zero hardcoded endpoints in the codebase.
 */

function getRequiredPublicEnv(key: string, value: string | undefined): string {
  if (!value) {
    if (typeof window !== "undefined") {
      console.error(
        `[Fincomp Config Error] Variável de ambiente obrigatória ausente: ${key}. Configure em .env.local`
      )
    }
    return ""
  }
  return value
}

export const env = {
  NEXT_PUBLIC_API_URL: getRequiredPublicEnv(
    "NEXT_PUBLIC_API_URL",
    process.env.NEXT_PUBLIC_API_URL
  ),
  // Subdomínio próprio de autenticação (login + MFA), separado desta app.
  // Opcional em ambiente local / preview de design system. Ex.: https://auth.fincomp.pro
  NEXT_PUBLIC_AUTH_URL: process.env.NEXT_PUBLIC_AUTH_URL || "",
  NEXT_PUBLIC_APP_NAME: process.env.NEXT_PUBLIC_APP_NAME || "Fincomp",
} as const
