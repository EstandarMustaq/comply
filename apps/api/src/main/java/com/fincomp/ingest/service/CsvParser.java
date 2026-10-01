package com.fincomp.ingest.service;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class CsvParser {

    public record CsvParsedData(List<String> headers, List<List<String>> rows) {}

    public CsvParsedData parse(byte[] fileBytes) throws IOException {
        Charset charset = detectCharset(fileBytes);
        char separator = detectSeparator(fileBytes, charset);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileBytes), charset));
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                     .build()) {

            List<String[]> allLines = csvReader.readAll();
            if (allLines.isEmpty()) {
                return new CsvParsedData(List.of(), List.of());
            }

            // Identifica a primeira linha válida como cabeçalho (ignora possíveis linhas vazias no topo)
            int headerIdx = 0;
            while (headerIdx < allLines.size() && allLines.get(headerIdx).length == 0) {
                headerIdx++;
            }

            if (headerIdx >= allLines.size()) {
                return new CsvParsedData(List.of(), List.of());
            }

            List<String> headers = Arrays.stream(allLines.get(headerIdx))
                    .map(String::trim)
                    .toList();

            List<List<String>> rows = new ArrayList<>();
            for (int i = headerIdx + 1; i < allLines.size(); i++) {
                String[] line = allLines.get(i);
                // Ignora linhas totalmente em branco ou linhas de rodapé com somatórios se vazias
                boolean hasContent = Arrays.stream(line).anyMatch(cell -> cell != null && !cell.trim().isBlank());
                if (hasContent) {
                    rows.add(Arrays.stream(line).map(cell -> cell != null ? cell.trim() : "").toList());
                }
            }

            return new CsvParsedData(headers, rows);
        } catch (Exception e) {
            throw new IOException("Falha ao analisar arquivo CSV: " + e.getMessage(), e);
        }
    }

    private char detectSeparator(byte[] fileBytes, Charset charset) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileBytes), charset))) {
            String firstLine = reader.readLine();
            if (firstLine != null) {
                int semicolons = countOccurrences(firstLine, ';');
                int commas = countOccurrences(firstLine, ',');
                int tabs = countOccurrences(firstLine, '\t');

                if (semicolons >= commas && semicolons >= tabs) return ';';
                if (tabs > commas) return '\t';
                return ',';
            }
        } catch (Exception ignored) {}
        return ';';
    }

    private Charset detectCharset(byte[] fileBytes) {
        // Tenta UTF-8; se houver caracteres inválidos recorre a ISO-8859-1 (comum em exports moçambicanos legados)
        try {
            StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(fileBytes));
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            return StandardCharsets.ISO_8859_1;
        }
    }

    private int countOccurrences(String str, char ch) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == ch) count++;
        }
        return count;
    }
}
