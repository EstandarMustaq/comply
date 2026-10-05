package com.complyz.ingest.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ColumnMappingServiceTest {

    private final ColumnMappingService service = new ColumnMappingService();

    @Test
    void suggestsMappingsForEnglishHeaders() {
        Map<String, String> suggestions = service.suggestMapping(List.of(
                "Document number",
                "Full name",
                "Transaction date",
                "Transaction amount",
                "Transaction type",
                "Account number"
        ));

        assertThat(suggestions)
                .containsEntry("clientDocument", "Document number")
                .containsEntry("clientName", "Full name")
                .containsEntry("transactionDate", "Transaction date")
                .containsEntry("amount", "Transaction amount")
                .containsEntry("transactionType", "Transaction type")
                .containsEntry("accountNumber", "Account number");
    }

    @Test
    void keepsMappingExistingPortugueseHeaders() {
        Map<String, String> suggestions = service.suggestMapping(List.of(
                "NUIT",
                "Nome do Cliente",
                "Data de Operacao",
                "Montante",
                "Tipo de Operacao",
                "Numero da Conta"
        ));

        assertThat(suggestions)
                .containsEntry("clientDocument", "NUIT")
                .containsEntry("clientName", "Nome do Cliente")
                .containsEntry("transactionDate", "Data de Operacao")
                .containsEntry("amount", "Montante")
                .containsEntry("transactionType", "Tipo de Operacao")
                .containsEntry("accountNumber", "Numero da Conta");
    }
}
