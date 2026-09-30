package com.oms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Một dòng trong tệp nhập hàng loạt: số dòng Excel, các lỗi phát hiện được và kết quả sau khi nhập. */
public abstract class ImportRow implements Serializable {

    public enum Outcome {
        CREATED("Đã tạo mới"),
        UPDATED("Đã cập nhật"),
        UNCHANGED("Không thay đổi"),
        SKIPPED("Bỏ qua");

        private final String label;

        Outcome(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private int rowNumber;
    private final List<String> errors = new ArrayList<>();
    private Outcome outcome;

    public boolean isValid() {
        return errors.isEmpty();
    }

    public void addError(String message) {
        errors.add(message);
    }

    public String getErrorText() {
        return String.join("; ", errors);
    }

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
    public List<String> getErrors() { return errors; }
    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }
}
