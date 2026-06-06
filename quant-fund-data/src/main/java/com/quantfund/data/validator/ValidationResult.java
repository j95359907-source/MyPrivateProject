package com.quantfund.data.validator;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void addError(String msg) { errors.add(msg); }
    public void addWarning(String msg) { warnings.add(msg); }

    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }

    public int errorCount() { return errors.size(); }
    public int warningCount() { return warnings.size(); }
    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean isValid() { return errors.isEmpty(); }

    @Override
    public String toString() {
        return String.format("ValidationResult(OK=%s, errors=%d, warnings=%d)",
                isValid(), errorCount(), warningCount());
    }
}
