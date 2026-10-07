/**
 * Type-safe environment variable access for Complyz.
 * Prevents hardcoded service endpoints in application code.
 */

function getRequiredPublicEnv(key: string, value: string | undefined): string {
  if (!value) {
    if (typeof window !== "undefined") {
      console.error(
        `[Complyz config error] Required environment variable ${key} is missing. Set it in .env.local.`
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
  // Optional authentication entry point on the API domain.
  // Example: https://api.trycomplyz.com
  NEXT_PUBLIC_AUTH_URL: process.env.NEXT_PUBLIC_AUTH_URL || "",
  NEXT_PUBLIC_APP_NAME: process.env.NEXT_PUBLIC_APP_NAME || "Complyz",
} as const
