package com.campusconnect.util;

import java.util.List;

/**
 * Minimal CSV serialisation.
 *
 * <p>Deliberately hand-written rather than pulled in as a dependency: exports are a
 * handful of string columns, and the two rules that matter are easy to get right in
 * one place — quoting per RFC 4180, and neutralising values that a spreadsheet
 * would otherwise treat as a formula.
 */
public final class CsvWriter {

    private static final String LINE_SEPARATOR = "\r\n";
    private static final char[] FORMULA_PREFIXES = {'=', '+', '-', '@', '\t', '\r'};

    private CsvWriter() {
    }

    public static String toCsv(List<String> headers, List<List<String>> rows) {
        StringBuilder csv = new StringBuilder();

        appendRow(csv, headers);
        for (List<String> row : rows) {
            appendRow(csv, row);
        }
        return csv.toString();
    }

    private static void appendRow(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(values.get(i)));
        }
        csv.append(LINE_SEPARATOR);
    }

    /**
     * Quotes when the value contains a delimiter, quote or newline, and prefixes a
     * leading apostrophe when the value could be interpreted as a formula by a
     * spreadsheet application (CSV injection).
     */
    static String escape(String value) {
        if (value == null) {
            return "";
        }

        String sanitised = value;
        if (!sanitised.isEmpty() && isFormulaPrefix(sanitised.charAt(0))) {
            sanitised = "'" + sanitised;
        }

        boolean requiresQuotes = sanitised.indexOf(',') >= 0
                || sanitised.indexOf('"') >= 0
                || sanitised.indexOf('\n') >= 0
                || sanitised.indexOf('\r') >= 0;

        String escaped = sanitised.replace("\"", "\"\"");
        return requiresQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private static boolean isFormulaPrefix(char first) {
        for (char prefix : FORMULA_PREFIXES) {
            if (first == prefix) {
                return true;
            }
        }
        return false;
    }
}
