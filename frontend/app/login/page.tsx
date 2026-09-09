import { LoginForm } from "@/components/login-form"

export default function LoginPage() {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center bg-muted/40 p-4 sm:p-6 md:p-10">
      <div className="w-full max-w-sm md:max-w-3xl lg:max-w-4xl">
        <LoginForm />
      </div>
    </div>
  )
}
