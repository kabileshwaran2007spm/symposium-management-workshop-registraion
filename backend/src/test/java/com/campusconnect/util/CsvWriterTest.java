package com.campusconnect.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CsvWriterTest {

    private static final List<String> HEADERS = List.of("Student ID", "Name", "Status");

    @Test
    @DisplayName("writes a header row and one row per record")
    void writesHeaderAndRows() {
        String csv = CsvWriter.toCsv(HEADERS, List.of(
                List.of("CS001", "Arun", "CONFIRMED"),
                List.of("CS002", "Kumar", "ATTENDED")));

        assertThat(csv.lines().toList()).containsExactly(
                "Student ID,Name,Status",
                "CS001,Arun,CONFIRMED",
                "CS002,Kumar,ATTENDED");
    }

    @Test
    @DisplayName("an empty result set still produces the header")
    void emptyResultKeepsHeader() {
        String csv = CsvWriter.toCsv(HEADERS, List.of());
        assertThat(csv.lines().toList()).containsExactly("Student ID,Name,Status");
    }

    @Test
    @DisplayName("values containing a comma are quoted")
    void commasAreQuoted() {
        String csv = CsvWriter.toCsv(HEADERS, List.of(List.of("CS001", "Priya, Sharma", "CONFIRMED")));
        assertThat(csv).contains("\"Priya, Sharma\"");
    }

    @Test
    @DisplayName("embedded quotes are doubled, per RFC 4180")
    void quotesAreDoubled() {
        String csv = CsvWriter.toCsv(HEADERS, List.of(List.of("CS001", "Arun \"Ace\" Kumar", "CONFIRMED")));
        assertThat(csv).contains("\"Arun \"\"Ace\"\" Kumar\"");
    }

    @Test
    @DisplayName("newlines inside a value are quoted so they cannot break the row")
    void newlinesAreQuoted() {
        String csv = CsvWriter.toCsv(HEADERS, List.of(List.of("CS001", "Line1\nLine2", "CONFIRMED")));
        assertThat(csv).contains("\"Line1\nLine2\"");
    }

    @Test
    @DisplayName("null values become empty cells rather than the text 'null'")
    void nullsBecomeEmpty() {
        String csv = CsvWriter.toCsv(HEADERS, List.of(java.util.Arrays.asList("CS001", null, null)));
        assertThat(csv).contains("CS001,,");
        assertThat(csv).doesNotContain("null");
    }

    @Test
    @DisplayName("a value that looks like a spreadsheet formula is neutralised")
    void formulaInjectionIsNeutralised() {
        // A student could otherwise name themselves =1+1 and have Excel run it.
        String csv = CsvWriter.toCsv(HEADERS, List.of(List.of("CS001", "=1+1", "CONFIRMED")));
        assertThat(csv).contains("'=1+1");

        assertThat(CsvWriter.escape("+cmd")).startsWith("'+");
        assertThat(CsvWriter.escape("-2+3")).startsWith("'-");
        assertThat(CsvWriter.escape("@SUM(A1)")).startsWith("'@");
    }

    @Test
    @DisplayName("rows are separated by CRLF so Excel opens them as one table")
    void usesCrlfLineEndings() {
        String csv = CsvWriter.toCsv(HEADERS, List.of(List.of("CS001", "Arun", "CONFIRMED")));
        assertThat(csv).contains("\r\n");
    }
}
