# Lumora dashboard and access design

Every signed-in Lumora user is routed to a dedicated dashboard determined by their active role. A user may hold multiple roles; the security layer will require an explicit active role where dashboard scope differs.

| Role | Dedicated dashboard | Permitted scope |
|---|---|---|
| Super Administrator | Platform Administration | School setup, global roles, users, configurations, audits, all reports. |
| Administrator | School Administration | School users, classes, subjects, calendar, operational reports; no platform-level configuration. |
| Principal | Executive Dashboard | Whole-school academics, discipline, attendance, finance summaries, approvals, strategic reports. |
| Deputy Principal | Academic Operations | Academic calendar, examinations oversight, attendance and teacher-performance summaries. |
| Bursar | Finance Dashboard | Fee structures, invoices, receipts, M-Pesa records, balances, arrears, finance reports. |
| Registrar | Admissions & Records | Admissions, student profiles, parent links, enrolment, class allocation and record exports. |
| Teacher | Teaching Dashboard | Assigned subjects/classes, attendance, marks, student progress and class notices. |
| Class Teacher | Class Management Dashboard | Teacher permissions plus assigned stream roster, daily attendance, class progress and guardian communication. |
| Student | Student Portal | Own timetable, attendance, results, fee balance, library loans and announcements. |
| Parent/Guardian | Parent Portal | Linked children only: attendance, results, fee balances, notices and payment history. |
| Librarian | Library Dashboard | Catalogue, copies, loans, returns, fines, overdue items and library reports. |

## Permission principles

- Authentication is required for every dashboard and module route.
- Permissions are role-based and enforced in Spring Security and service methods.
- Parent/Guardian and Student roles are ownership-scoped: they can never access another learner's records.
- Teacher and Class Teacher access is restricted to their explicit teaching and stream assignments.
- Finance data entry is restricted to Bursar and authorized administrators; executive roles receive summary/read permissions.
- Super Administrator is reserved for platform governance; Administrator is the operational school-administration role.

The `PARENT` role is the technical representation of the Parent/Guardian dashboard. The UI will label it “Parent/Guardian”.
