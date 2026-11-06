# Service layer

The service layer is the application boundary for business workflows. Controllers call services, never repositories directly — including for reads, which is why the dashboards go through `DashboardMetricsService` rather than counting rows themselves.

| Service | Primary responsibilities | Rules enforced |
|---|---|---|
| UserService | Accounts and roles | Unique username and email; BCrypt password hashing; accounts are disabled, never deleted. |
| StudentService | Admission, enrollment, profiles | Admission number is unique; stream and academic year must be in the selected school; a stream is mandatory; a learner cannot be admitted before they were born. |
| AcademicService | Calendar, classes, subjects, teacher allocations | Transactional academic setup CRUD; resolves the active term. |
| AttendanceService | Daily learner attendance | One record per student in a stream on a date; no future-dated registers; a learner outside the stream cannot appear on its register; re-saving a register corrects it rather than duplicating it. |
| ExaminationService | Exams, mark entry, grading | Teacher must be assigned to that subject, stream and term; marks cannot exceed the paper maximum or be negative; published exams are immutable; grade and points are assigned automatically from the examination's grading scale. |
| FinanceService | Fees, invoices, receipts | Receipt uniqueness; no overpayment; no payment against a voided invoice; an invoice is `PAID` only at zero balance; bulk invoicing skips learners already billed for the term. |
| MpesaService | M-Pesa confirmations | Unique transaction code; transaction and payment amounts must match. |
| LibraryService | Copies and circulation | Only available copies can be issued; a loan cannot fall due before it is issued; copy status is updated atomically with the loan. |
| NotificationService | Announcements and recipient delivery | All addressed recipients must be valid users; notices carry an optional expiry. |
| DashboardAccessService | Learner-record authorization | Student self-access and parent/guardian child ownership only; raises `AccessDeniedException` so the refusal is a 403 and is audited. |
| DashboardMetricsService | Read-only aggregation | Whole-school counts, chart series and the audit feed for the dashboards. |

## Transactions

Services are annotated `@Transactional(readOnly = true)` at class level, with write methods overriding to a read-write transaction. This keeps reads cheap and makes every write an explicit decision.

Two writes that must not be separable share one transaction:

- **Admission** — the `Student` row and its `StudentEnrollment` are written together. A learner without a placement cannot be registered, marked present, or examined, so a half-completed admission is not a valid state.
- **Circulation** — the `BookLoan` and the `BookCopy` status change together, so a copy can never be both available and on loan.

## Input and output

Input DTOs are Java records with Jakarta Bean Validation. Read models are also records (`StudentProfile`, `DashboardSummary`, `MarkSheetRow`, `ChartPoint`) so a managed entity never escapes a transaction into a template.

Callers receive domain exceptions — `ResourceNotFoundException`, `DuplicateResourceException`, `BusinessRuleViolationException` — plus Spring Security's `AccessDeniedException` for ownership failures. The web layer turns these into a styled error page; the REST layer turns them into RFC 7807 `ProblemDetail` JSON.
