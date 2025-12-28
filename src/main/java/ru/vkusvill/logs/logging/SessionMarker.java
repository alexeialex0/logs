package ru.vkusvill.logs.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class SessionMarker {

    private static final Logger log = LoggerFactory.getLogger(SessionMarker.class);

    private final CsvLogWriter csvLogWriter;

    public SessionMarker() {
        // тот же путь, что и в LogProxyController
        this.csvLogWriter = new CsvLogWriter("logs.csv");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String sessionText = "НОВАЯ_СЕССИЯ: " + timestamp;

        // первая колонка = Кейсы, остальные пустые (столько же, сколько и в обычной строке)
        List<String> row = new ArrayList<String>();
        row.add(sessionText);  // Кейсы
        row.add("");           // Действие
        row.add("");           // str_par

        // добиваем до нужного количества колонок, чтобы совпадало с остальными строками
        // у нас всего 69 колонок (по шапке), уже добавили 3, значит ещё 66 пустых
        for (int i = 0; i < 66; i++) {
            row.add("");
        }

        csvLogWriter.appendRow(row);
        log.info("Session marker written to logs.csv: {}", sessionText);
    }
}
