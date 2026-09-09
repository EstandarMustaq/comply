import urllib.request
import json
import base64
import hmac
import hashlib
import time
import struct

BASE_URL = "http://localhost:8080/api"

def request_json(url, method="GET", data=None, headers=None):
    if headers is None:
        headers = {}
    headers["Content-Type"] = "application/json"
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as response:
            return response.status, json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))

def get_totp_code(secret_base32):
    key = base64.b32decode(secret_base32, casefold=True)
    epoch_seconds = int(time.time())
    step = epoch_seconds // 30
    msg = struct.pack(">Q", step)
    h = hmac.new(key, msg, hashlib.sha1).digest()
    offset = h[-1] & 0x0F
    binary = struct.unpack(">I", h[offset:offset+4])[0] & 0x7fffffff
    otp = binary % 1000000
    return f"{otp:06d}"

print("1. Login com admin (MFA ativo):")
status, data = request_json(f"{BASE_URL}/auth/login", "POST", {
    "email": "admin@fincomp.co.mz",
    "password": "Admin@FinComp2024!"
})
print("HTTP Status:", status, "| Status:", data.get("status"))
challenge_token = data.get("challengeToken")

print("\n2. Testar autenticação com Código de Backup de Uso Único ('TQXAC438'):")
status, verify_data = request_json(f"{BASE_URL}/auth/mfa/verify", "POST", {
    "challengeToken": challenge_token,
    "code": "TQXAC438"
})
print("HTTP Status:", status, "| Status após verificação Backup Code:", verify_data.get("status"))
print("Access Token emitido?", bool(verify_data.get("accessToken")))
admin_token = verify_data.get("accessToken")

print("\n3. Testar reutilização do mesmo código de backup (deve ser rejeitado pois é uso único):")
# Novo login para novo challenge
status, data2 = request_json(f"{BASE_URL}/auth/login", "POST", {
    "email": "admin@fincomp.co.mz",
    "password": "Admin@FinComp2024!"
})
challenge_token2 = data2.get("challengeToken")

status_reused, res_reused = request_json(f"{BASE_URL}/auth/mfa/verify", "POST", {
    "challengeToken": challenge_token2,
    "code": "TQXAC438"
})
print("Resultado do reuso do mesmo código de backup:", status_reused, res_reused)

print("\n4. Testar chamada a endpoint protegido /api/settings/users com o token emitido:")
status_users, users_list = request_json(f"{BASE_URL}/settings/users", "GET", headers={"Authorization": f"Bearer {admin_token}"})
print("HTTP Status /api/settings/users:", status_users)
print("Número de utilizadores no sistema:", len(users_list))
for u in users_list:
    print(f" - {u['email']} ({u['role']}) | MFA: {u['mfaEnabled']}")

print("\n5. Testar tentativa de UPDATE direto na tabela audit_log (validar trigger de imutabilidade):")
