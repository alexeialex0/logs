package ru.vkusvill.logs.monitor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.vkusvill.logs.logging.CsvLogReader;
import ru.vkusvill.logs.logging.LogRecord;

import java.util.List;

@RestController
public class LogMonitorController {

    private final CsvLogReader csvLogReader;
    private final LogSseBroadcaster broadcaster;

    public LogMonitorController(LogSseBroadcaster broadcaster) {
        this.csvLogReader = new CsvLogReader("logs.csv");
        this.broadcaster = broadcaster;
    }

    // начальная загрузка последних N строк
    @GetMapping("/logs/last")
    public List<LogRecord> getLastLogs(@RequestParam(defaultValue = "50") int limit) {
        return csvLogReader.readLast(limit);
    }

    // SSE‑стрим для живых логов
    @GetMapping("/logs/stream")
    public SseEmitter streamLogs() {
        return broadcaster.registerClient();
    }
}
