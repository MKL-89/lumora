package com.lumora.sms.service.impl;

import com.lumora.sms.exception.BusinessRuleViolationException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A spreadsheet saved as CSV, read the way Excel and Google Sheets write it:
 * quoted fields may hold commas, quotes ("") and line breaks; the separator may be
 * a comma, a semicolon (Excel in some regions) or a tab; the file may be UTF-8,
 * with or without a byte-order mark, or Excel's older Windows-1252.
 *
 * <p>Column headings are matched loosely: case, spaces and punctuation are
 * ignored, so "Admission No.", "admission_number" and "ADMISSION NUMBER" are the
 * same column.
 */
final class CsvTable {

    /** A data row and its line number in the spreadsheet (the heading is row 1). */
    record Row(int number, Map<String, String> cells) {
        String get(String column) {
            String value = cells.get(column);
            return value == null ? "" : value.trim();
        }
    }

    private final List<String> headings;
    private final List<Row> rows;

    private CsvTable(List<String> headings, List<Row> rows) {
        this.headings = headings;
        this.rows = rows;
    }

    List<String> headings() {
        return headings;
    }

    List<Row> rows() {
        return rows;
    }

    /** "Admission No." becomes "admissionno". */
    static String key(String heading) {
        return heading.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Reads the file. {@code aliases} maps each loosely written heading to the
     * column name used in code; headings it does not know are ignored.
     */
    static CsvTable parse(byte[] bytes, Map<String, String> aliases, int maxRows) {
        String text = decode(bytes);
        List<List<String>> records = split(text);
        if (records.isEmpty() || records.get(0).stream().allMatch(String::isBlank)) {
            throw new BusinessRuleViolationException("The file is empty.");
        }
        List<String> rawHeadings = records.get(0);
        List<String> headings = new ArrayList<>();
        for (String heading : rawHeadings) {
            headings.add(aliases.getOrDefault(key(heading), ""));
        }

        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> record = records.get(i);
            if (record.stream().allMatch(String::isBlank)) {
                continue;
            }
            if (rows.size() >= maxRows) {
                throw new BusinessRuleViolationException(
                        "The file has more than " + maxRows + " learners. Split it into smaller files.");
            }
            Map<String, String> cells = new HashMap<>();
            for (int c = 0; c < record.size() && c < headings.size(); c++) {
                if (!headings.get(c).isEmpty()) {
                    cells.putIfAbsent(headings.get(c), record.get(c));
                }
            }
            rows.add(new Row(i + 1, cells));
        }
        return new CsvTable(headings, rows);
    }

    private static String decode(byte[] bytes) {
        int start = bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF ? 3 : 0;
        ByteBuffer buffer = ByteBuffer.wrap(bytes, start, bytes.length - start);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(buffer).toString();
        } catch (CharacterCodingException notUtf8) {
            return new String(bytes, start, bytes.length - start, Charset.forName("windows-1252"));
        }
    }

    /** The separator is whichever of comma, semicolon or tab appears most in the heading line. */
    static char separator(String text) {
        int end = text.indexOf('\n');
        String first = end < 0 ? text : text.substring(0, end);
        char best = ',';
        long bestCount = first.chars().filter(ch -> ch == ',').count();
        for (char candidate : new char[]{';', '\t'}) {
            long count = first.chars().filter(ch -> ch == candidate).count();
            if (count > bestCount) {
                best = candidate;
                bestCount = count;
            }
        }
        return best;
    }

    static List<List<String>> split(String text) {
        char separator = separator(text);
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == '"' && field.isEmpty()) {
                quoted = true;
            } else if (ch == separator) {
                record.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                record.add(field.toString());
                field.setLength(0);
                records.add(record);
                record = new ArrayList<>();
            } else {
                field.append(ch);
            }
        }
        if (!field.isEmpty() || !record.isEmpty()) {
            record.add(field.toString());
            records.add(record);
        }
        return records;
    }
}
