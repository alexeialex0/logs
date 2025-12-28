package ru.vkusvill.logs.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CsvLogWriter {

    private static final Logger log = LoggerFactory.getLogger(CsvLogWriter.class);

    private final Path filePath;
    private final char separator = ';';

    public CsvLogWriter(String filePath) {
        this.filePath = Paths.get(filePath);
    }

    public void appendRow(List<String> columns) {
        try {
            // проверяем, что файл существует (шапку создали вручную)
            if (!Files.exists(filePath)) {
                log.warn("CSV file not found at {}, skipping write", filePath.toAbsolutePath());
                return;
            }

            // открываем с флагом append=true, UTF-8
            // FileOutputStream может одновременно писать из разных потоков,
            // но для надёжности используем синхронизацию
            synchronized (CsvLogWriter.class) {
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(filePath.toFile(), true),
                                StandardCharsets.UTF_8))) {

                    String line = toCsvLine(columns);
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                }
            }

            log.debug("Row appended to CSV at {}", filePath.toAbsolutePath());

        } catch (IOException e) {
            log.error("Error writing CSV row to {}", filePath.toAbsolutePath(), e);
        }
    }

    private String toCsvLine(List<String> columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(escape(columns.get(i)));
        }
        return sb.toString();
    }

    private String escape(String value) {
        if (value == null) return "";
        boolean needQuotes = value.indexOf(separator) >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        String v = value.replace("\"", "\"\"");
        return needQuotes ? "\"" + v + "\"" : v;
    }
}

