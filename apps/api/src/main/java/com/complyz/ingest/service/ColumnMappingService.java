package com.complyz.ingest.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ColumnMappingService {

    public Map<String, String> suggestMapping(List<String> detectedHeaders) {
        Map<String, String> suggestions = new HashMap<>();

        for (String header : detectedHeaders) {
            String norm = normalize(header);

            if (matches(norm,
                    "document", "document_number", "id_number", "tax_id", "doc", "nuit",
                    "bi", "passaporte", "identificacao", "documento", "nr_doc")) {
                suggestions.putIfAbsent("clientDocument", header);
            } else if (matches(norm,
                    "name", "full_name", "customer_name", "client_name", "account_holder",
                    "holder", "borrower", "company_name", "nome", "cliente", "titular",
                    "mutuario", "razao_social", "nome_cliente")) {
                suggestions.putIfAbsent("clientName", header);
            } else if (matches(norm,
                    "date", "transaction_date", "operation_date", "data", "data_operacao",
                    "data_transaccao", "dt_operacao", "dt", "data_movimento")) {
                suggestions.putIfAbsent("transactionDate", header);
            } else if (matches(norm,
                    "amount", "value", "total", "transaction_amount", "operation_value",
                    "balance", "valor", "montante", "quantia", "valor_operacao", "deposito",
                    "saldo")) {
                suggestions.putIfAbsent("amount", header);
            } else if (matches(norm,
                    "type", "transaction_type", "operation_type", "kind", "tipo", "operacao",
                    "natureza", "tipo_operacao", "tipo_movimento", "credito_debito")) {
                suggestions.putIfAbsent("transactionType", header);
            } else if (matches(norm,
                    "account", "account_number", "account_no", "acct", "conta", "numero_conta",
                    "no_conta", "nr_conta", "nuib", "iban")) {
                suggestions.putIfAbsent("accountNumber", header);
            }
        }

        return suggestions;
    }

    private boolean matches(String target, String... candidates) {
        String paddedTarget = "_" + target + "_";
        for (String candidate : candidates) {
            if (paddedTarget.contains("_" + candidate + "_")) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String str) {
        if (str == null) return "";
        return Normalizer.normalize(str, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}
