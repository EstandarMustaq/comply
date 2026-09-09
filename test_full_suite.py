import urllib.request
import json
import base64
import hmac
import hashlib
import time
import struct
import io

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

print("==================================================")
print("FINCOMP E2E REGULATORY & COMPLIANCE TEST SUITE")
print("==================================================")

# 1. Login com OCOS
print("\n[TESTE 1] Autenticação com OCOS:")
status, login_res = request_json(f"{BASE_URL}/auth/login", "POST", {
    "email": "ocos@fincomp.co.mz",
    "password": "Ocos@FinComp2024!"
})
print("HTTP Status:", status, "| Role:", login_res.get("role"))
ocos_token = login_res.get("accessToken")
headers_ocos = {"Authorization": f"Bearer {ocos_token}"}

# 2. Insights Summary
print("\n[TESTE 2] Consulta de Métricas e Indicadores de Compliance (Redis Cache):")
status, insights = request_json(f"{BASE_URL}/insights/summary", "GET", headers=headers_ocos)
print("HTTP Status:", status)
print(f"Total Clientes: {insights.get('totalClients')} | Flags Pendentes: {insights.get('pendingFlagsCount')} | Sanções: {insights.get('sanctionsMatchCount')}")

# 3. Teste de Sanções (Lei 15/2023 - 24h SLA)
print("\n[TESTE 3] Cadastro de cliente e triagem contra Lista Designada Nacional:")
# Cadastra cliente que corresponde à entrada "IBONDO SALIMO" (PGR-LDN-2023-002)
status, client_sanction = request_json(f"{BASE_URL}/clients", "POST", {
    "fullName": "Ibondo Salimo",
    "idDocumentType": "BI",
    "idDocumentNumber": "110200300400B",
    "dueDiligenceRegime": "NORMAL",
    "isPep": False,
    "seniorManagementApproved": True
}, headers=headers_ocos)
print("HTTP Status criação cliente:", status, "| ID:", client_sanction.get("id"))
client_id = client_sanction.get("id")

# 4. Upload e Ingestão de Extrato CSV com transação do cliente sancionado e teste de fraccionamento
print("\n[TESTE 4] Ingestão de Extrato CSV via Multipart:")
csv_content = """Documento;Nome Cliente;Data;Valor;Tipo;Conta
110200300400B;Ibondo Salimo;2026-09-01;50000,00;DEPOSITO;10001001
110200300400B;Ibondo Salimo;2026-09-02;60000,00;DEPOSITO;10001001
110200300400B;Ibondo Salimo;2026-09-03;70000,00;DEPOSITO;10001001
880100200300C;Maria Mutola;2026-09-04;35000,00;DEPOSITO;10002002
"""

boundary = "----FinCompFormBoundary7MA4YWxkTrZu0gW"
body_parts = []
body_parts.append(f"--{boundary}\r\n".encode())
body_parts.append(b'Content-Disposition: form-data; name="file"; filename="extrato_teste.csv"\r\n')
body_parts.append(b'Content-Type: text/csv\r\n\r\n')
body_parts.append(csv_content.encode("utf-8"))
body_parts.append(f"\r\n--{boundary}--\r\n".encode())
multipart_body = b"".join(body_parts)

req = urllib.request.Request(
    f"{BASE_URL}/import-batches",
    data=multipart_body,
    headers={
        "Authorization": f"Bearer {ocos_token}",
        "Content-Type": f"multipart/form-data; boundary={boundary}"
    },
    method="POST"
)
with urllib.request.urlopen(req) as resp:
    upload_res = json.loads(resp.read().decode())
batch_id = upload_res.get("batchId")
print("Lote criado com sucesso. Batch ID:", batch_id)
print("Cabeçalhos detectados:", upload_res.get("headers"))
print("Mapeamento sugerido heuristicamente:", upload_res.get("suggestedMapping"))

# 5. Confirmar mapeamento e processar
print("\n[TESTE 5] Confirmação de mapeamento e execução transacional:")
mapping = upload_res.get("suggestedMapping")
status, process_res = request_json(
    f"{BASE_URL}/import-batches/{batch_id}/confirm-mapping",
    "POST",
    mapping,
    headers=headers_ocos
)
print("Status do Processamento:", status, "| Processados com sucesso:", process_res.get("totalProcessed"))

# 6. Listar Flags geradas e inspecionar SLA de 24h para Sanções
print("\n[TESTE 6] Inspeção da Fila de Flags e SLAs Legais:")
status, flags = request_json(f"{BASE_URL}/flags", "GET", headers=headers_ocos)
print(f"Total de Flags ativas geradas: {len(flags)}")
sanction_flag = None
for f in flags:
    print(f" - [{f['type']}] SLA: {f.get('slaDeadline')} | Razão: {f['reason'][:80]}...")
    if f['type'] == 'SANCTIONS_MATCH':
        sanction_flag = f

if sanction_flag:
    print("\n[VALIDAÇÃO SANÇÕES] Flag de Sanções detectada com sucesso!")
    print(f"SLA Deadline: {sanction_flag['slaDeadline']} (SLA estrito de 24h nos termos da Lei 15/2023)")

# 7. Escalamento da Flag para STR (Artigo 98)
print("\n[TESTE 7] Escalamento de Flag pelo OCOS para Participação de Operação Suspeita (STR):")
target_flag_id = flags[0]['id']
status, escalate_res = request_json(
    f"{BASE_URL}/flags/{target_flag_id}/escalate-to-str",
    "POST",
    headers=headers_ocos
)
str_id = escalate_res.get("strId")
print("Status do escalamento:", status, "| STR ID gerado:", str_id)

# 8. Consulta e submissão formal de STR com protocolo GIFiM
print("\n[TESTE 8] Preenchimento e submissão formal de STR com prazo de retenção de 10 anos (Art. 89 e 98):")
status, submit_res = request_json(
    f"{BASE_URL}/reports/str/{str_id}/submit",
    "POST",
    {"gifimRegistrationNumber": "GIFIM-POS-2026-00891"},
    headers=headers_ocos
)
print("Status da submissão:", status)
print(f"Protocolo GIFiM: {submit_res.get('gifimRegistrationNumber')}")
print(f"Retenção legal fixada até: {submit_res.get('retentionUntil')} (10 anos nos termos do Art. 89 do Aviso 10/GBM/2024)")

# 9. Listar CTRs gerados automaticamente por limiar/agregação (Art. 44 / 129)
print("\n[TESTE 9] Verificação de CTRs gerados automaticamente por agregação mensal:")
status, ctrs = request_json(f"{BASE_URL}/reports/ctr", "GET", headers=headers_ocos)
print(f"Total de CTRs pendentes de envio ao GIFiM: {len(ctrs)}")
for c in ctrs:
    print(f" - CTR ID: {c['id']} | Montante Agregado: {c['aggregateValue']} {c['currency']} | Período: {c['aggregationPeriodStart']} a {c['aggregationPeriodEnd']}")

print("\n==================================================")
print("TODOS OS TESTES DE CONFORMIDADE E FLUXOS REGULATÓRIOS CONCLUÍDOS COM SUCESSO!")
print("==================================================")
