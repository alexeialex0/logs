package ru.vkusvill.logs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
public class LogProxyController {

    private static final Logger log = LoggerFactory.getLogger(LogProxyController.class);

    // ПРЯМОЙ echo‑URL Apps Script (без 302)
    private static final String GOOGLE_SCRIPT_URL =
"ТВОЙ_УРЛ_ВЕБ_ХУКА";
    private static final String BACKEND_URL =
            "УРЛ_ЛОГОВ_ВВ";

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/log-proxy")
    public ResponseEntity<String> handleLog(
            @RequestBody String body,
            @RequestHeader Map<String, String> headersMap) {

        log.info("Received log body length={}", body.length());

        String caseName = extractCaseName(body);

        String gsBody = body;
        if (caseName != null && !caseName.trim().isEmpty()) {
            try {
                gsBody = body + "&case_name=" + URLEncoder.encode(caseName, StandardCharsets.UTF_8.toString());
            } catch (Exception e) {
                gsBody = body + "&case_name=" + caseName.replace("&", "%26").replace("=", "%3D");
            }
        }

        log.info("Google Script body length={}, case_name={}", gsBody.length(), caseName);

        // 1. Отправка в Google Script БЕЗ редиректа
        sendToGoogleScript(gsBody);

        // 2. Как раньше — отправка в backend
        return forwardToBackend(body, headersMap);
    }

    private void sendToGoogleScript(String gsBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<String> request = new HttpEntity<>(gsBody, headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(
                    GOOGLE_SCRIPT_URL, request, String.class
            );

            log.info("Google Script status={}, body={}",
                    resp.getStatusCode().value(), resp.getBody());
        } catch (Exception ex) {
            log.error("Error sending to Google Script", ex);
        }
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

    private String extractCaseName(String formBody) {
        if (formBody == null || formBody.isEmpty()) return null;
        String[] parts = formBody.split("&");
        for (String part : parts) {
            if (part.startsWith("case_name=")) {
                try {
                    return URLDecoder.decode(part.substring(9), StandardCharsets.UTF_8.toString());
                } catch (Exception e) {
                    return part.substring(9);
                }
            }
        }
        return null;
    }

    private void copyHeader(Map<String, String> from, HttpHeaders to, String nameLower) {
        from.forEach((k, v) -> {
            if (k != null && k.equalsIgnoreCase(nameLower)) {
                to.add(k, v);
            }
        });
    }
}
