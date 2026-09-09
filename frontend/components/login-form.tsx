"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { api } from "@/lib/api-client"
import { FincompLogo } from "@/components/fincomp-logo"
import { LoginDepth } from "@/components/login-depth"
import { Loader2, ArrowRight } from "lucide-react"

export function LoginForm({
  className,
  ...props
}: React.ComponentProps<"div">) {
  const router = useRouter()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const res = await api.login(email, password)

      if (res.status === "MFA_ENROLLMENT_REQUIRED" && res.enrollmentToken) {
        router.push(`/mfa/enroll?enrollmentToken=${encodeURIComponent(res.enrollmentToken)}`)
        return
      }

      if (res.status === "MFA_REQUIRED" && res.challengeToken) {
        router.push(`/mfa/verify?challengeToken=${encodeURIComponent(res.challengeToken)}`)
        return
      }

      if (res.accessToken || res.token) {
        router.push("/")
        return
      }
    } catch (err: any) {
      setError(err.message || "Invalid credentials. Please check your details and try again.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={cn("flex flex-col gap-6", className)} {...props}>
      <Card className="overflow-hidden border-border/50 rounded-xl shadow-lg">
        <CardContent className="grid p-0 md:grid-cols-2">

          {/* Left: Sign-in form */}
          <div className="flex flex-col justify-between p-7 md:p-8">
            <div>
              {/* Logo */}
              <div className="mb-7">
                <FincompLogo size="md" priority />
              </div>

              <form className="space-y-3" onSubmit={handleSubmit}>
                {error && (
                  <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2.5 text-xs text-destructive font-medium">
                    {error}
                  </div>
                )}

                <div className="space-y-1">
                  <Label htmlFor="email" className="text-xs font-medium">
                    Email
                  </Label>
                  <Input
                    id="email"
                    type="email"
                    placeholder="you@organization.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    autoComplete="email"
                    className="h-9 text-sm"
                  />
                </div>

                <div className="space-y-1">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="password" className="text-xs font-medium">
                      Password
                    </Label>
                    <button
                      type="button"
                      tabIndex={-1}
                      className="text-[11px] text-muted-foreground hover:text-foreground transition-colors"
                    >
                      Forgot your password?
                    </button>
                  </div>
                  <Input
                    id="password"
                    type="password"
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    autoComplete="current-password"
                    className="h-9 text-sm"
                  />
                </div>

                <Button
                  type="submit"
                  className="w-full h-9 font-medium text-sm"
                  disabled={loading}
                >
                  {loading ? (
                    <>
                      <Loader2 className="mr-2 h-3.5 w-3.5 animate-spin" />
                      Signing in…
                    </>
                  ) : (
                    <>
                      Continue
                      <ArrowRight className="ml-2 h-3.5 w-3.5" />
                    </>
                  )}
                </Button>
              </form>
            </div>

            {/* Footer */}
            <div className="mt-6 space-y-3">
              <p className="text-center text-[10px] leading-relaxed text-muted-foreground/60">
                By clicking continue, you agree to our{" "}
                <button
                  type="button"
                  className="underline underline-offset-2 hover:text-muted-foreground transition-colors"
                >
                  Terms of Service
                </button>{" "}
                and{" "}
                <button
                  type="button"
                  className="underline underline-offset-2 hover:text-muted-foreground transition-colors"
                >
                  Privacy Policy
                </button>
                .
              </p>
              <p className="text-center text-[10px] text-muted-foreground/40">
                FinComp · v1.0
              </p>
            </div>
          </div>

          {/* Right: Depth background panel */}
          <LoginDepth />

        </CardContent>
      </Card>
    </div>
  )
}
