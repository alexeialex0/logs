package ru.vkusvill.logs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
public class LogProxyController {

    private static final Logger log = LoggerFactory.getLogger(LogProxyController.class);

    // только backend, без Google Script
    private static final String BACKEND_URL =
            "урл_ВВ_Логов";

    private final RestTemplate restTemplate = new RestTemplate();
    private final LogRowBuilder logRowBuilder = new LogRowBuilder();
    // logs.csv лежит в корне проекта рядом с pom.xml
    private final CsvLogWriter csvLogWriter = new CsvLogWriter("logs.csv");

    @PostMapping("/log-proxy")
    public ResponseEntity<String> handleLog(
            @RequestBody String body,
            @RequestHeader Map<String, String> headersMap) {

        log.info("Received log body length={}", body.length());

        // 1. Парсинг и запись строки в CSV
        try {
            List<String> row = logRowBuilder.buildRow(body, null);
            csvLogWriter.appendRow(row);
            log.info("Row appended to logs.csv, columns={}", row.size());
        } catch (Exception e) {
            log.error("Error building or writing CSV row", e);
        }

        // 2. Форвард в backend, как раньше
        return forwardToBackend(body, headersMap);
    }

    private ResponseEntity<String> forwardToBackend(String body, Map<String, String> headersMap) {
        try {
            HttpHeaders backendHeaders = new HttpHeaders();
            copyHeader(headersMap, backendHeaders, "content-type");
            copyHeader(headersMap, backendHeaders, "x-vkusvill-token-access");
            copyHeader(headersMap, backendHeaders, "x-vkusvill-token");
            copyHeader(headersMap, backendHeaders, "x-vkusvill-version");
            copyHeader(headersMap, backendHeaders, "x-vkusvill-source");
            copyHeader(headersMap, backendHeaders, "user-agent");
            copyHeader(headersMap, backendHeaders, "accept");
            copyHeader(headersMap, backendHeaders, "accept-language");
            copyHeader(headersMap, backendHeaders, "accept-encoding");
            copyHeader(headersMap, backendHeaders, "cookie");

            if (!backendHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
                backendHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            }

            HttpEntity<String> backendRequest = new HttpEntity<>(body, backendHeaders);
            ResponseEntity<byte[]> backendResponse =
                    restTemplate.exchange(BACKEND_URL, HttpMethod.POST, backendRequest, byte[].class);

            log.info("Backend status={}", backendResponse.getStatusCode().value());

            HttpHeaders respHeaders = new HttpHeaders();
            backendResponse.getHeaders().forEach(respHeaders::put);

            byte[] respBodyBytes = backendResponse.getBody();
            String respBody = respBodyBytes == null
                    ? ""
                    : new String(respBodyBytes, StandardCharsets.UTF_8);

            return new ResponseEntity<>(respBody, respHeaders, backendResponse.getStatusCode());

        } catch (Exception ex) {
            log.error("Error forwarding to backend", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error forwarding log to backend: " + ex.getMessage());
        }
    }

    private void copyHeader(Map<String, String> from, HttpHeaders to, String nameLower) {
        from.forEach((k, v) -> {
            if (k != null && k.equalsIgnoreCase(nameLower)) {
                to.add(k, v);
            }
        });
    }
}
