package com.fincomp.ingest.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ColumnMappingService {

    public Map<String, String> suggestMapping(List<String> detectedHeaders) {
        Map<String, String> suggestions = new HashMap<>();

        for (String header : detectedHeaders) {
            String norm = normalize(header);

            if (matches(norm, "doc", "nuit", "bi", "passaporte", "identificacao", "documento", "nr_doc")) {
                suggestions.putIfAbsent("clientDocument", header);
            } else if (matches(norm, "nome", "cliente", "titular", "mutuario", "razao_social", "nome_cliente")) {
                suggestions.putIfAbsent("clientName", header);
            } else if (matches(norm, "data", "data_operacao", "data_transaccao", "dt_operacao", "dt", "data_movimento")) {
                suggestions.putIfAbsent("transactionDate", header);
            } else if (matches(norm, "valor", "montante", "total", "quantia", "valor_operacao", "deposito", "saldo")) {
                suggestions.putIfAbsent("amount", header);
            } else if (matches(norm, "tipo", "operacao", "natureza", "tipo_operacao", "tipo_movimento", "credito_debito")) {
                suggestions.putIfAbsent("transactionType", header);
            } else if (matches(norm, "conta", "numero_conta", "no_conta", "nr_conta", "nuib", "iban")) {
                suggestions.putIfAbsent("accountNumber", header);
            }
        }

        return suggestions;
    }

    private boolean matches(String target, String... candidates) {
        for (String candidate : candidates) {
            if (target.equals(candidate) || target.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String str) {
        if (str == null) return "";
        return Normalizer.normalize(str, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}
