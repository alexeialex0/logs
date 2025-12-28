package ru.vkusvill.logs.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ru.vkusvill.logs.logging.CsvLogWriter;
import ru.vkusvill.logs.logging.LogRecord;
import ru.vkusvill.logs.logging.LogRowBuilder;
import ru.vkusvill.logs.monitor.LogSseBroadcaster;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
public class LogProxyController {

    private static final Logger log = LoggerFactory.getLogger(LogProxyController.class);

    private static final String BACKEND_URL =
            "твой_урл_логов_ВВ";

    private final RestTemplate restTemplate = new RestTemplate();
    private final LogRowBuilder logRowBuilder = new LogRowBuilder();
    private final CsvLogWriter csvLogWriter = new CsvLogWriter("logs.csv");
    private final LogSseBroadcaster broadcaster;

    public LogProxyController(LogSseBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @PostMapping("/log-proxy")
    public ResponseEntity<String> handleLog(
            @RequestBody String body,
            @RequestHeader Map<String, String> headersMap) {

        log.info("Received log body length={}", body.length());

        try {
            List<String> row = logRowBuilder.buildRow(body, null);
            csvLogWriter.appendRow(row);
            broadcaster.broadcast(new LogRecord(row));
            log.info("Row appended and broadcast, columns={}", row.size());
        } catch (Exception e) {
            log.error("Error building/writing/broadcasting log row", e);
        }

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

            HttpEntity<String> backendRequest = new HttpEntity<String>(body, backendHeaders);
            ResponseEntity<byte[]> backendResponse =
                    restTemplate.exchange(BACKEND_URL, HttpMethod.POST, backendRequest, byte[].class);

            log.info("Backend status={}", backendResponse.getStatusCode().value());

            HttpHeaders respHeaders = new HttpHeaders();
            backendResponse.getHeaders().forEach(respHeaders::put);

            byte[] respBodyBytes = backendResponse.getBody();
            String respBody = respBodyBytes == null
                    ? ""
                    : new String(respBodyBytes, StandardCharsets.UTF_8);

            return new ResponseEntity<String>(respBody, respHeaders, backendResponse.getStatusCode());

        } catch (Exception ex) {
            log.error("Error forwarding to backend", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error forwarding log to backend: " + ex.getMessage());
        }
    }

    private void copyHeader(Map<String, String> from, HttpHeaders to, String nameLower) {
        for (Map.Entry<String, String> entry : from.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (k != null && k.equalsIgnoreCase(nameLower)) {
                to.add(k, v);
            }
        }
    }
}

