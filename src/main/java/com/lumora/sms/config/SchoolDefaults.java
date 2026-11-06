package com.lumora.sms.config;

import com.lumora.sms.enums.UserRoleType;
import java.util.List;
import java.util.Map;

/**
 * Reference data every Kenyan secondary school starts with: what each role is for,
 * the KCSE twelve-point grading scale, and the common KCSE subjects. Shared by the
 * demo seed and by first-time setup so both build a school the same way.
 */
public final class SchoolDefaults {

    private SchoolDefaults() {
    }

    public static final Map<UserRoleType, String> ROLE_DESCRIPTIONS = Map.ofEntries(
            Map.entry(UserRoleType.SUPER_ADMIN, "Platform governance, school setup and global configuration."),
            Map.entry(UserRoleType.ADMINISTRATOR, "Day-to-day school administration and operational reports."),
            Map.entry(UserRoleType.PRINCIPAL, "Whole-school academics, finance summaries and approvals."),
            Map.entry(UserRoleType.DEPUTY_PRINCIPAL, "Academic operations, examinations and attendance oversight."),
            Map.entry(UserRoleType.REGISTRAR, "Admissions, student records and class allocation."),
            Map.entry(UserRoleType.BURSAR, "Fee structures, invoices, receipts and arrears."),
            Map.entry(UserRoleType.TEACHER, "Assigned subjects: attendance, marks and progress."),
            Map.entry(UserRoleType.CLASS_TEACHER, "Teacher duties plus a stream roster and guardian contact."),
            Map.entry(UserRoleType.STUDENT, "Own timetable, attendance, results, fees and loans."),
            Map.entry(UserRoleType.PARENT, "Linked children only: attendance, results and fee balances."),
            Map.entry(UserRoleType.LIBRARIAN, "Catalogue, copies, circulation and overdue items."));

    /** One band of the grading scale: marks from {@code min} to {@code max} earn {@code points}. */
    public record GradeBand(String grade, int min, int max, int points, String remarks) {
    }

    /** The standard KCSE twelve-point scale. */
    public static final List<GradeBand> KCSE_GRADES = List.of(
            new GradeBand("A", 80, 100, 12, "Excellent"),
            new GradeBand("A-", 75, 79, 11, "Very good"),
            new GradeBand("B+", 70, 74, 10, "Very good"),
            new GradeBand("B", 65, 69, 9, "Good"),
            new GradeBand("B-", 60, 64, 8, "Good"),
            new GradeBand("C+", 55, 59, 7, "Above average"),
            new GradeBand("C", 50, 54, 6, "Average"),
            new GradeBand("C-", 45, 49, 5, "Average"),
            new GradeBand("D+", 40, 44, 4, "Below average"),
            new GradeBand("D", 35, 39, 3, "Weak"),
            new GradeBand("D-", 30, 34, 2, "Weak"),
            new GradeBand("E", 0, 29, 1, "Needs urgent support"));

    /** A subject offered at KCSE. {@code usual} subjects are ticked by default on the setup page. */
    public record SubjectOption(String code, String name, boolean compulsory, boolean usual) {
    }

    public static final List<SubjectOption> KCSE_SUBJECTS = List.of(
            new SubjectOption("ENG", "English", true, true),
            new SubjectOption("KIS", "Kiswahili", true, true),
            new SubjectOption("MAT", "Mathematics", true, true),
            new SubjectOption("BIO", "Biology", false, true),
            new SubjectOption("CHE", "Chemistry", false, true),
            new SubjectOption("PHY", "Physics", false, true),
            new SubjectOption("HIS", "History and Government", false, true),
            new SubjectOption("GEO", "Geography", false, true),
            new SubjectOption("CRE", "Christian Religious Education", false, true),
            new SubjectOption("IRE", "Islamic Religious Education", false, false),
            new SubjectOption("HRE", "Hindu Religious Education", false, false),
            new SubjectOption("BST", "Business Studies", false, true),
            new SubjectOption("AGR", "Agriculture", false, true),
            new SubjectOption("CMP", "Computer Studies", false, false),
            new SubjectOption("HSC", "Home Science", false, false),
            new SubjectOption("ART", "Art and Design", false, false),
            new SubjectOption("MUS", "Music", false, false),
            new SubjectOption("FRE", "French", false, false),
            new SubjectOption("GER", "German", false, false),
            new SubjectOption("ARB", "Arabic", false, false));
}
