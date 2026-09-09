"use client";

import React, { useState, useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { QrCode, AlertCircle, Loader2, CheckCircle2, Copy } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { InputOTP, InputOTPGroup, InputOTPSlot } from "@/components/ui/input-otp";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { api } from "@/lib/api-client";
import { MfaEnrollResponse } from "@/lib/types";

function MfaEnrollContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const enrollmentToken = searchParams.get("enrollmentToken");

  const [enrollData, setEnrollData] = useState<MfaEnrollResponse | null>(null);
  const [code, setCode] = useState("");
  const [loadingEnroll, setLoadingEnroll] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [backupCodes, setBackupCodes] = useState<string[] | null>(null);
  const [copiedSecret, setCopiedSecret] = useState(false);

  useEffect(() => {
    if (!enrollmentToken) {
      router.push("/login");
      return;
    }

    let isMounted = true;
    const fetchEnrollment = async () => {
      try {
        setLoadingEnroll(true);
        setError(null);
        const data = await api.enrollMfa(enrollmentToken);
        if (isMounted) {
          setEnrollData(data);
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err.message || "Erro ao iniciar configuração MFA.");
        }
      } finally {
        if (isMounted) {
          setLoadingEnroll(false);
        }
      }
    };

    fetchEnrollment();

    return () => {
      isMounted = false;
    };
  }, [enrollmentToken, router]);

  const handleConfirm = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!enrollmentToken || !code.trim()) return;

    try {
      setIsSubmitting(true);
      setError(null);
      const res = await api.confirmMfa(code.trim(), enrollmentToken);

      if (res.accessToken || res.token) {
        api.setToken(res.accessToken || res.token || "");
        localStorage.setItem(
          "fincomp_user",
          JSON.stringify({
            email: res.email,
            role: res.role,
            fullName: res.fullName,
          })
        );
      }

      if (res.backupCodes && res.backupCodes.length > 0) {
        setBackupCodes(res.backupCodes);
      } else {
        router.push("/");
      }
    } catch (err: any) {
      setError(err.message || "Código incorreto. Tente novamente.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCopySecret = () => {
    const secret = enrollData?.secretKey || enrollData?.secret;
    if (secret) {
      navigator.clipboard.writeText(secret);
      setCopiedSecret(true);
      setTimeout(() => setCopiedSecret(false), 2000);
    }
  };

  if (backupCodes) {
    return (
      <Card className="w-full max-w-md shadow-sm border-border bg-card">
        <CardHeader className="space-y-2 text-center">
          <div className="mx-auto w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
            <CheckCircle2 className="h-6 w-6" />
          </div>
          <CardTitle className="text-xl font-bold tracking-tight">MFA Ativado</CardTitle>
          <CardDescription className="text-xs text-muted-foreground">
            Guarde os códigos de recuperação:
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-2 p-3 bg-muted rounded-md font-mono text-xs text-center">
            {backupCodes.map((c, i) => (
              <div key={i} className="p-1 border border-border rounded bg-background">
                {c}
              </div>
            ))}
          </div>
        </CardContent>
        <CardFooter>
          <Button className="w-full text-xs font-semibold" onClick={() => router.push("/")}>
            Aceder à Plataforma
          </Button>
        </CardFooter>
      </Card>
    );
  }

  return (
    <Card className="w-full max-w-md shadow-sm border-border bg-card">
      <CardHeader className="space-y-1 text-center">
        <div className="mx-auto w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary mb-1">
          <QrCode className="h-6 w-6" />
        </div>
        <CardTitle className="text-xl font-bold tracking-tight">
          Configurar autenticação de dois fatores
        </CardTitle>
        <CardDescription className="text-xs text-muted-foreground">
          Digitalize este código com a sua aplicação autenticadora
        </CardDescription>
      </CardHeader>

      <form onSubmit={handleConfirm}>
        <CardContent className="space-y-4">
          {error && (
            <div className="p-3 bg-destructive/10 border border-destructive/20 rounded-md flex items-center gap-2 text-xs text-destructive">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {loadingEnroll ? (
            <div className="flex flex-col items-center justify-center py-8 gap-2">
              <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
              <span className="text-xs text-muted-foreground">A gerar credenciais...</span>
            </div>
          ) : enrollData ? (
            <div className="space-y-4">
              <div className="flex justify-center p-4 bg-background rounded-lg border border-border">
                {enrollData.qrCodeBase64 ? (
                  <img
                    src={`data:image/png;base64,${enrollData.qrCodeBase64}`}
                    alt="QR Code MFA"
                    className="w-48 h-48 bg-white p-2 rounded"
                  />
                ) : enrollData.qrCodeUri ? (
                  <img
                    src={enrollData.qrCodeUri}
                    alt="QR Code MFA"
                    className="w-48 h-48 bg-white p-2 rounded"
                  />
                ) : null}
              </div>

              {(enrollData.secretKey || enrollData.secret) && (
                <div className="flex items-center justify-between p-2.5 bg-muted rounded-md border border-border text-xs">
                  <span className="font-mono truncate mr-2 select-all">
                    {enrollData.secretKey || enrollData.secret}
                  </span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={handleCopySecret}
                    className="h-7 px-2 text-xs shrink-0"
                  >
                    <Copy className="h-3.5 w-3.5 mr-1" />
                    {copiedSecret ? "Copiado" : "Copiar"}
                  </Button>
                </div>
              )}

              <div className="space-y-2 text-center pt-2">
                <Label htmlFor="enroll-code" className="text-xs font-semibold block">
                  Código de confirmação (6 dígitos)
                </Label>
                <div className="flex justify-center py-1">
                  <InputOTP
                    id="enroll-code"
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
              </div>
            </div>
          ) : null}
        </CardContent>

        <CardFooter className="flex flex-col gap-2">
          <Button
            type="submit"
            className="w-full text-xs font-semibold"
            disabled={isSubmitting || code.length !== 6 || loadingEnroll}
          >
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                A confirmar...
              </>
            ) : (
              "Confirmar e Ativar MFA"
            )}
          </Button>

          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => router.push("/login")}
            className="w-full text-xs text-muted-foreground hover:text-foreground"
          >
            Voltar ao login
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}

export default function MfaEnrollPage() {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center bg-muted p-6 md:p-10">
      <div className="w-full max-w-md">
        <Suspense fallback={<div className="text-center text-sm text-muted-foreground">A carregar...</div>}>
          <MfaEnrollContent />
        </Suspense>
      </div>
    </div>
  );
}
