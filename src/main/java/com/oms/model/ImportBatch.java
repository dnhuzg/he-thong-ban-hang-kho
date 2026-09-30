package com.oms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Kết quả đọc một tệp nhập hàng loạt. Giữ trong session giữa bước xem trước và bước nhập;
 * token để nút "Nhập" ở tab cũ không áp nhầm một bản xem trước khác.
 */
public class ImportBatch<T extends ImportRow> implements Serializable {

    private final String token = UUID.randomUUID().toString();
    private final String fileName;
    private final List<T> rows;
    private final List<String> notes = new ArrayList<>();
    private boolean applied;

    public ImportBatch(String fileName, List<T> rows) {
        this.fileName = fileName;
        this.rows = rows;
    }

    public List<T> getErrorRows() {
        return rows.stream().filter(r -> !r.isValid()).toList();
    }

    public int getValidCount() {
        return (int) rows.stream().filter(ImportRow::isValid).count();
    }

    public int getErrorCount() {
        return rows.size() - getValidCount();
    }

    public int count(ImportRow.Outcome outcome) {
        return (int) rows.stream().filter(r -> r.getOutcome() == outcome).count();
    }

    public int getCreatedCount() { return count(ImportRow.Outcome.CREATED); }
    public int getUpdatedCount() { return count(ImportRow.Outcome.UPDATED); }
    public int getUnchangedCount() { return count(ImportRow.Outcome.UNCHANGED); }
    public int getSkippedCount() { return count(ImportRow.Outcome.SKIPPED); }

    public String getToken() { return token; }
    public String getFileName() { return fileName; }
    public List<T> getRows() { return rows; }
    public List<String> getNotes() { return notes; }
    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }
}
