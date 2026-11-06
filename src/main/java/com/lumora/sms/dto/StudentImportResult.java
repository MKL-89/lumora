package com.lumora.sms.dto;

import java.util.List;

/**
 * What a learner import did. When {@code problems} is not empty nothing at all was
 * saved: the school fixes the listed rows and uploads the file again.
 */
public record StudentImportResult(int rowsRead, int imported, int skipped, int guardiansAdded,
                                  List<RowProblem> problems) {

    /** One thing wrong with one row of the spreadsheet. */
    public record RowProblem(int row, String admissionNumber, String message) {
    }

    public boolean ok() {
        return problems.isEmpty();
    }
}
