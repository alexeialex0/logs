package ru.vkusvill.logs.logging;

import java.util.List;

public class LogRecord {

    private final List<String> columns;

    public LogRecord(List<String> columns) {
        this.columns = columns;
    }

    public List<String> getColumns() {
        return columns;
    }

    // удобные геттеры для важных полей
    public String getCaseName() {
        return columns.size() > 0 ? columns.get(0) : "";
    }

    public String getStrPar() {
        return columns.size() > 2 ? columns.get(2) : "";
    }
}
