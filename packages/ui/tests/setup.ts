import { beforeEach, vi } from "vitest"

process.env.NEXT_PUBLIC_API_URL = "http://api:8080/api"

beforeEach(() => {
  localStorage.clear()
  vi.unstubAllGlobals()
})
