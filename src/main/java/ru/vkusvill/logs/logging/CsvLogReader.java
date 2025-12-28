package ru.vkusvill.logs.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class CsvLogReader {

    private static final Logger log = LoggerFactory.getLogger(CsvLogReader.class);

    private final Path filePath;
    private final char separator = ';';

    public CsvLogReader(String filePath) {
        this.filePath = Paths.get(filePath);
    }

    public List<LogRecord> readLast(int limit) {
        List<LogRecord> result = new ArrayList<LogRecord>();
        Deque<String> queue = new ArrayDeque<String>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath.toFile()), StandardCharsets.UTF_8))) {

            String line;
            // пропускаем шапку (первая строка)
            boolean headerSkipped = false;

            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                if (queue.size() == limit) {
                    queue.removeFirst();
                }
                queue.addLast(line);
            }

            for (String l : queue) {
                List<String> cols = parseLine(l);
                result.add(new LogRecord(cols));
            }

        } catch (Exception e) {
            log.error("Error reading CSV file {}", filePath.toAbsolutePath(), e);
        }

        return result;
    }

    private List<String> parseLine(String line) {
        List<String> cols = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == separator && !inQuotes) {
                cols.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        cols.add(sb.toString());
        return cols;
    }
}
