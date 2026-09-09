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

print("1. Login inicial sem MFA ativo:")
status, data = request_json(f"{BASE_URL}/auth/login", "POST", {
    "email": "admin@fincomp.co.mz",
    "password": "Admin@FinComp2024!"
})
print("HTTP Status:", status, "| Resposta Status:", data.get("status"))
token = data.get("accessToken")
enrollment_token = data.get("enrollmentToken")
headers = {"Authorization": f"Bearer {token}"} if token else {}

print("\n2. Iniciar Enrollment MFA:")
enroll_body = {"enrollmentToken": enrollment_token} if enrollment_token else {}
status, enroll_data = request_json(f"{BASE_URL}/auth/mfa/enroll", "POST", data=enroll_body, headers=headers)
secret_key = enroll_data.get("secretKey")
print("HTTP Status:", status, "| Chave secreta Base32 recebida:", secret_key)
print("QR Code Base64 presente?", bool(enroll_data.get("qrCodeBase64")))

print("\n3. Gerar código TOTP e confirmar enrollment:")
code = get_totp_code(secret_key)
print("Código TOTP gerado:", code)
confirm_body = {"code": code}
if enrollment_token:
    confirm_body["enrollmentToken"] = enrollment_token
status, confirm_data = request_json(f"{BASE_URL}/auth/mfa/confirm", "POST", data=confirm_body, headers=headers)
print("HTTP Status:", status, "| Mensagem:", confirm_data.get("message"))
print("Códigos de backup:", confirm_data.get("backupCodes"))

print("\n4. Login novamente (agora com MFA ativo):")
status, login_mfa_data = request_json(f"{BASE_URL}/auth/login", "POST", {
    "email": "admin@fincomp.co.mz",
    "password": "Admin@FinComp2024!"
})
print("HTTP Status:", status, "| Status do Login:", login_mfa_data.get("status"))
challenge_token = login_mfa_data.get("challengeToken")
print("Challenge Token recebido:", challenge_token)

print("\n5. Validar código TOTP no endpoint /api/auth/mfa/verify:")
totp_code_2 = get_totp_code(secret_key)
status, verify_data = request_json(f"{BASE_URL}/auth/mfa/verify", "POST", {
    "challengeToken": challenge_token,
    "code": totp_code_2
})
print("HTTP Status:", status, "| Status após verificação TOTP:", verify_data.get("status"))
print("Novo Access Token emitido?", bool(verify_data.get("accessToken")))
print("MFA Ativo no perfil?", verify_data.get("mfaEnabled"))

print("\n6. Testar prevenção de replay attack:")
status_replay, res_replay = request_json(f"{BASE_URL}/auth/mfa/verify", "POST", {
    "challengeToken": challenge_token,
    "code": totp_code_2
})
print("Resposta para replay/challenge reutilizado:", status_replay, res_replay)
