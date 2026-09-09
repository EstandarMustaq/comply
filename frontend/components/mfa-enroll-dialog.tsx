"use client";

import React, { useState } from "react";
import { QrCode, Shield, Copy, Check, Key, Loader2, AlertCircle } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "./ui/dialog";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { api } from "@/lib/api-client";
import { MfaEnrollResponse } from "@/lib/types";

interface MfaEnrollDialogProps {
  isOpen: boolean;
  onClose: () => void;
  tempToken: string;
  enrollData: MfaEnrollResponse | null;
  onSuccess: () => void;
}

export function MfaEnrollDialog({
  isOpen,
  onClose,
  tempToken,
  enrollData,
  onSuccess,
}: MfaEnrollDialogProps) {
  const [totpCode, setTotpCode] = useState("");
  const [copiedSecret, setCopiedSecret] = useState(false);
  const [copiedCodes, setCopiedCodes] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!enrollData) return null;

  const handleCopySecret = () => {
    const secret = enrollData.secretKey || enrollData.secret || "";
    navigator.clipboard.writeText(secret);
    setCopiedSecret(true);
    setTimeout(() => setCopiedSecret(false), 2000);
  };

  const handleCopyCodes = () => {
    if (enrollData.backupCodes) {
      navigator.clipboard.writeText(enrollData.backupCodes.join("\n"));
      setCopiedCodes(true);
      setTimeout(() => setCopiedCodes(false), 2000);
    }
  };

  const handleConfirm = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!totpCode || totpCode.length !== 6) {
      setError("Insira o código de 6 dígitos gerado pela sua aplicação.");
      return;
    }

    try {
      setIsVerifying(true);
      setError(null);
      const res = await api.confirmMfaEnroll(tempToken, totpCode);
      if (res.token) {
        api.setToken(res.token);
        localStorage.setItem(
          "fincomp_user",
          JSON.stringify({
            email: res.email,
            role: res.role,
            fullName: res.fullName,
          })
        );
        onSuccess();
      }
    } catch (err: any) {
      setError(err.message || "Código inválido. Tente novamente.");
    } finally {
      setIsVerifying(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="mx-auto w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center text-primary mb-2">
            <Shield className="h-6 w-6" />
          </div>
          <DialogTitle className="text-center text-lg">
            Two-Factor Authentication
          </DialogTitle>
          <DialogDescription className="text-center text-xs">
            FinComp requires two-factor authentication for your account. Scan the QR code with an authenticator app.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleConfirm} className="space-y-4 text-xs">
          {/* QR Code Container */}
          <div className="flex flex-col items-center justify-center p-3 bg-muted/40 rounded-lg border">
            {enrollData.qrCodeBase64 ? (
              <img
                src={`data:image/png;base64,${enrollData.qrCodeBase64}`}
                alt="QR Code TOTP"
                className="w-44 h-44 rounded border bg-white p-2 shadow-sm"
              />
            ) : enrollData.qrCodeUri ? (
              <img
                src={enrollData.qrCodeUri}
                alt="QR Code TOTP"
                className="w-44 h-44 rounded border bg-white p-2 shadow-sm"
              />
            ) : (
              <div className="w-44 h-44 flex items-center justify-center border bg-white rounded">
                <QrCode className="h-16 w-16 text-muted-foreground" />
              </div>
            )}
            <p className="mt-2 text-muted-foreground text-center text-[11px]">
              Escaneie com o Google Authenticator ou Microsoft Authenticator.
            </p>
          </div>

          {/* Secret Manual Key */}
          <div className="space-y-1">
            <Label className="text-[11px] text-muted-foreground">
              Ou insira a chave manualmente:
            </Label>
            <div className="flex items-center gap-2">
              <code className="flex-1 p-2 bg-muted rounded font-mono text-[11px] select-all border">
                {enrollData.secretKey || enrollData.secret}
              </code>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleCopySecret}
                className="h-8 px-2.5"
              >
                {copiedSecret ? (
                  <Check className="h-3.5 w-3.5 text-emerald-600" />
                ) : (
                  <Copy className="h-3.5 w-3.5" />
                )}
              </Button>
            </div>
          </div>

          {/* Backup Codes */}
          {enrollData.backupCodes && enrollData.backupCodes.length > 0 && (
            <div className="space-y-1.5 p-3 bg-amber-500/10 border border-amber-500/20 rounded-lg">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-amber-900 flex items-center gap-1">
                  <Key className="h-3.5 w-3.5 text-amber-600" />
                  Códigos de Recuperação (Uso Único)
                </span>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={handleCopyCodes}
                  className="h-6 text-[10px] text-amber-800 hover:bg-amber-500/20 px-1.5"
                >
                  {copiedCodes ? "Copiados!" : "Copiar todos"}
                </Button>
              </div>
              <p className="text-[10px] text-amber-800">
                Guarde estes códigos em local seguro. Cada código só pode ser utilizado uma única vez caso perca o telemóvel.
              </p>
              <div className="grid grid-cols-2 gap-1 font-mono text-[11px] text-amber-950 pt-1">
                {enrollData.backupCodes.map((code, idx) => (
                  <span key={idx} className="bg-white/60 px-2 py-0.5 rounded border border-amber-200">
                    {code}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* TOTP Confirmation Input */}
          <div className="space-y-1.5 pt-2">
            <Label htmlFor="totpInput" className="font-semibold text-foreground">
              Código de Verificação de 6 Dígitos
            </Label>
            <Input
              id="totpInput"
              type="text"
              inputMode="numeric"
              maxLength={6}
              placeholder="000000"
              value={totpCode}
              onChange={(e) => setTotpCode(e.target.value.replace(/\D/g, ""))}
              className="text-center font-mono text-lg tracking-widest h-10"
              autoFocus
            />
          </div>

          {error && (
            <div className="p-2.5 bg-red-500/10 border border-red-500/30 rounded text-red-600 flex items-center gap-1.5 text-xs">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <DialogFooter className="gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onClose}
              disabled={isVerifying}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              size="sm"
              disabled={isVerifying || totpCode.length !== 6}
              className="gap-1.5"
            >
              {isVerifying && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
              Ativar 2FA e Iniciar Sessão
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
