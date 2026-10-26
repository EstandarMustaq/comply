function getRequiredPublicEnv(key: string, value: string | undefined): string {
  if (!value) {
    if (typeof window !== "undefined") {
      console.error(
        `[Comply config error] Required environment variable ${key} is missing. Set it in .env.local.`
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
  NEXT_PUBLIC_AUTH_URL: process.env.NEXT_PUBLIC_AUTH_URL || "",
  NEXT_PUBLIC_APP_NAME: process.env.NEXT_PUBLIC_APP_NAME || "Comply",
} as const
