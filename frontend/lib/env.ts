/**
 * Type-safe environment variable access for FinComp Frontend.
 * Ensures zero hardcoded endpoints in the codebase.
 */

function getRequiredPublicEnv(key: string, value: string | undefined): string {
  if (!value) {
    if (typeof window !== "undefined") {
      console.error(`[FinComp Config Error] Variável de ambiente obrigatória ausente: ${key}. Configure em .env.local`);
    }
    return "";
  }
  return value;
}

export const env = {
  NEXT_PUBLIC_API_URL: getRequiredPublicEnv(
    "NEXT_PUBLIC_API_URL",
    process.env.NEXT_PUBLIC_API_URL
  ),
  NEXT_PUBLIC_APP_NAME: process.env.NEXT_PUBLIC_APP_NAME || "FinComp",
} as const;
