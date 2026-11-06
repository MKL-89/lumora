package com.lumora.sms.service.impl;

import com.lumora.sms.enums.Gender;
import org.junit.jupiter.api.Test;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** How spreadsheets exported from other systems are read. */
class StudentImportParsingTest {

    private static CsvTable parse(String text) {
        return CsvTable.parse(text.getBytes(StandardCharsets.UTF_8), StudentImportServiceImpl.ALIASES, 100);
    }

    @Test
    void headingsAreMatchedLooselyAndQuotedCellsKeepCommas() {
        CsvTable table = parse("Adm No.,SURNAME,First Name,Sex,D.O.B,Class,Stream\r\n"
                + "adm/1,Kamau,\"Brian, Jr\",M,14/03/2012,F1,East\r\n");
        CsvTable.Row row = table.rows().get(0);
        assertEquals(2, row.number());
        assertEquals("adm/1", row.get("admission_number"));
        assertEquals("Kamau", row.get("last_name"));
        assertEquals("Brian, Jr", row.get("first_name"));
        assertEquals("M", row.get("gender"));
        assertEquals("F1", row.get("form"));
    }

    @Test
    void semicolonFilesBomAndBlankLinesAreHandled() {
        CsvTable table = parse("﻿admission_number;first_name;last_name\n\nA1;Faith;Achieng\n;;\n");
        assertEquals(1, table.rows().size());
        assertEquals("Faith", table.rows().get(0).get("first_name"));
    }

    @Test
    void windows1252FilesFromOlderExcelAreRead() {
        byte[] bytes = "admission_number,first_name,last_name\nA1,Renée,Otieno\n".getBytes(Charset.forName("windows-1252"));
        CsvTable table = CsvTable.parse(bytes, StudentImportServiceImpl.ALIASES, 100);
        assertEquals("Renée", table.rows().get(0).get("first_name"));
    }

    @Test
    void oneNameColumnIsSplitAndShoutedNamesAreTidied() {
        CsvTable table = parse("name\nBRIAN KIPROTICH KAMAU\n");
        String[] names = StudentImportServiceImpl.names(table.rows().get(0));
        assertArrayEquals(new String[]{"Brian", "Kiprotich", "Kamau"}, names);
        assertEquals("Mary-Anne", StudentImportServiceImpl.titleCase("MARY-ANNE"));
        assertEquals("McDonald", StudentImportServiceImpl.titleCase("McDonald"));
    }

    @Test
    void datesAreReadDayFirstAndImpossibleDatesAreRefused() {
        assertEquals(LocalDate.of(2012, 3, 14), StudentImportServiceImpl.date("14/03/2012"));
        assertEquals(LocalDate.of(2012, 3, 14), StudentImportServiceImpl.date("2012-03-14"));
        assertEquals(LocalDate.of(2012, 3, 14), StudentImportServiceImpl.date("14-Mar-2012"));
        assertEquals(LocalDate.of(2012, 3, 14), StudentImportServiceImpl.date("14/3/12"));
        assertEquals(LocalDate.of(2012, 3, 14), StudentImportServiceImpl.date("14/03/2012 00:00"));
        assertNull(StudentImportServiceImpl.date(" "));
        assertThrows(java.time.format.DateTimeParseException.class, () -> StudentImportServiceImpl.date("31/02/2012"));
        assertThrows(java.time.format.DateTimeParseException.class, () -> StudentImportServiceImpl.date("soon"));
    }

    @Test
    void formsAndGendersAcceptTheUsualSpellings() {
        for (String form : List.of("Form 1", "F1", "1", "form one", "FORM I")) {
            assertEquals(1, StudentImportServiceImpl.formLevel(form), form);
        }
        assertEquals(4, StudentImportServiceImpl.formLevel("Form 4"));
        assertNull(StudentImportServiceImpl.formLevel("Form 5"));
        assertEquals(Gender.MALE, StudentImportServiceImpl.gender("Boy"));
        assertEquals(Gender.FEMALE, StudentImportServiceImpl.gender("F"));
        assertNull(StudentImportServiceImpl.gender("x"));
    }

    @Test
    void phoneNumbersWrittenDifferentlyAreTheSame() {
        assertTrue(Phones.same("0712 345 678", "+254712345678"));
        assertTrue(Phones.same("712345678", "254-712-345-678"));
        assertFalse(Phones.same("0712345678", "0712345679"));
        assertFalse(Phones.same("", ""));
    }

    @Test
    void temporaryPasswordsPassThePasswordRules() {
        for (int i = 0; i < 50; i++) {
            String password = UserServiceImpl.temporaryPassword();
            assertTrue(password.matches("[a-z]{4}-[2-9]{4}-[a-z]{4}"), password);
            com.lumora.sms.security.PasswordRules.check(password, "someone");
        }
    }
}
