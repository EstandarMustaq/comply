"use client";

import React, { useState, useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ShieldCheck, Key, AlertCircle, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { InputOTP, InputOTPGroup, InputOTPSlot } from "@/components/ui/input-otp";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { api } from "@/lib/api-client";

function MfaVerifyContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const challengeToken = searchParams.get("challengeToken") || searchParams.get("tempToken");

  const [code, setCode] = useState("");
  const [useBackupCode, setUseBackupCode] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!challengeToken) {
      router.push("/login");
    }
  }, [challengeToken, router]);

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!challengeToken) return;

    if (!code.trim()) {
      setError(useBackupCode ? "Insira o código de recuperação." : "Insira o código TOTP.");
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const res = await api.verifyMfa(challengeToken, code.trim());

      const token = res.accessToken || res.token;
      if (token) {
        api.setToken(token);
        localStorage.setItem(
          "fincomp_user",
          JSON.stringify({
            email: res.email,
            role: res.role,
            fullName: res.fullName,
          })
        );
        router.push("/");
      } else {
        setError(res.message || "Código inválido.");
      }
    } catch (err: any) {
      setError(err.message || "Código inválido ou expirado.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card className="w-full max-w-md shadow-sm border-border bg-card">
      <CardHeader className="space-y-2 text-center">
        <div className="mx-auto w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
          <ShieldCheck className="h-6 w-6" />
        </div>
        <CardTitle className="text-xl font-bold tracking-tight">Verificação MFA</CardTitle>
        <CardDescription className="text-xs text-muted-foreground">
          {useBackupCode
            ? "Introduza o código de recuperação"
            : "Introduza o código de autenticação"}
        </CardDescription>
      </CardHeader>

      <form onSubmit={handleVerify}>
        <CardContent className="space-y-4">
          {error && (
            <div className="p-3 bg-destructive/10 border border-destructive/20 rounded-md flex items-center gap-2 text-xs text-destructive">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div className="space-y-3">
            <Label htmlFor="code" className="text-xs font-semibold block text-center">
              {useBackupCode ? "Código de recuperação" : "Código de 6 dígitos"}
            </Label>
            {useBackupCode ? (
              <Input
                id="code"
                type="text"
                placeholder="Ex: a1b2c3d4"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                className="text-center font-mono text-xl tracking-widest h-12"
                autoFocus
                required
              />
            ) : (
              <div className="flex justify-center py-1">
                <InputOTP
                  id="code"
                  maxLength={6}
                  value={code}
                  onChange={(val) => setCode(val)}
                  autoFocus
                >
                  <InputOTPGroup>
                    <InputOTPSlot index={0} />
                    <InputOTPSlot index={1} />
                    <InputOTPSlot index={2} />
                    <InputOTPSlot index={3} />
                    <InputOTPSlot index={4} />
                    <InputOTPSlot index={5} />
                  </InputOTPGroup>
                </InputOTP>
              </div>
            )}
          </div>

          <div className="flex justify-between items-center text-xs pt-1">
            <button
              type="button"
              onClick={() => {
                setUseBackupCode(!useBackupCode);
                setCode("");
                setError(null);
              }}
              className="text-primary hover:underline flex items-center gap-1 font-medium"
            >
              <Key className="h-3 w-3" />
              {useBackupCode ? "Usar código TOTP" : "Usar código de recuperação"}
            </button>
          </div>
        </CardContent>

        <CardFooter className="flex flex-col gap-2">
          <Button
            type="submit"
            className="w-full text-xs font-semibold"
            disabled={isLoading || (!useBackupCode && code.length !== 6)}
          >
            {isLoading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                A validar...
              </>
            ) : (
              "Confirmar"
            )}
          </Button>

          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => router.push("/login")}
            className="w-full text-xs text-muted-foreground hover:text-foreground"
          >
            Voltar
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}

export default function MfaVerifyPage() {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center bg-muted p-6 md:p-10">
      <div className="w-full max-w-md">
        <Suspense fallback={<div className="text-center text-sm text-muted-foreground">A carregar...</div>}>
          <MfaVerifyContent />
        </Suspense>
      </div>
    </div>
  );
}
