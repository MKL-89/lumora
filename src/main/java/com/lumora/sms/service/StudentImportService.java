package com.lumora.sms.service;

import com.lumora.sms.dto.StudentImportResult;

/**
 * Brings learners across from a school's previous system, or from any
 * spreadsheet, saved as CSV. Every row is checked first; if any row has a
 * problem, nothing is saved, so a school never ends up with half a list.
 */
public interface StudentImportService {

    /**
     * @param skipExisting when true, learners whose admission number is already in
     *                     Lumora are left untouched instead of being reported as problems,
     *                     so the same file can safely be uploaded twice
     */
    StudentImportResult importCsv(byte[] file, boolean skipExisting);

    /** A ready-to-fill spreadsheet with the expected headings and two example rows. */
    String templateCsv();
}
