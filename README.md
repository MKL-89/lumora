# Lumora School Management System

A school ERP for Kenyan secondary schools, built with Java 17, Spring Boot 3.5,
Spring Security 6, Spring Data JPA, Thymeleaf, Flyway and MySQL/MariaDB.

---

## Running it

There are two ways to run Lumora. **Option A needs only Java** and is the right
choice for a presentation or any machine that does not already have MySQL. Option B
uses a real MySQL/MariaDB server.

### Option A — standalone, needs only Java (recommended)

The `dist` folder is a self-contained bundle: the application jar plus launchers.
It uses an embedded database, so **no MySQL, no XAMPP, no Maven, and no internet**
are required — only Java 17 or newer.

To run it on this or any other PC:

1. Copy the whole **`dist`** folder to the other computer.
2. Confirm Java is installed: open a terminal and run `java -version`. If it is
   missing, install Java 17+ from <https://adoptium.net> and tick *Add to PATH*.
3. **Windows:** double-click **`RUN-LUMORA.bat`**.
   **Mac/Linux:** run `bash RUN-LUMORA.sh`.
4. Wait for `Started LumoraApplication`, then open **http://localhost:8081**.

`dist/HOW-TO-RUN.txt` has the same steps in plain language for handing to someone else.

You can also run the jar directly:

```bash
java -jar dist/lumora.jar --spring.profiles.active=standalone,demo
```

The embedded database is a file in `./lumora-data` next to the jar; delete that
folder to start from an empty school. Add `--server.port=9090` if 8081 is taken.

From the project root, **`START-LUMORA.bat`** does the same thing and will build the
jar first (using the bundled Maven wrapper — still no Maven install) if it is missing.

### Option B — with a real MySQL / MariaDB server

Requires a server on `localhost:3306` with the database created once:

```bash
C:\xampp\mysql\bin\mysql.exe -u root < database/create_lumora_database.sql
```

Then start with the default profile (no `standalone`):

```bash
mvnw spring-boot:run           # or: mvn spring-boot:run
```

Or run the jar against MySQL:

```bash
java -jar dist/lumora.jar --spring.profiles.active=demo
```

### Several roles at once, in one browser

Per-tab sessions are **on by default** (`app.security.per-tab-sessions=true`), so each
browser tab holds its own independent login. Open four tabs, sign into a different
role in each, and they will not interfere: refreshing keeps a tab's own user, signing
out affects only that tab, and a brand-new tab always starts signed out.

Why this is needed: a `JSESSIONID` cookie belongs to the whole browser profile, not to
a tab, so by default every tab shares one session and one user. In this mode the
session id travels in the URL instead — an address bar belongs to exactly one tab.

That is why URLs look like `/students;jsessionid=…`. **Turn it off for any real
deployment** (`PER_TAB_SESSIONS=false`), because a session id in a URL can leak
through browser history, bookmarks and `Referer` headers. With the flag off the
application returns to ordinary cookie sessions and remember-me is re-enabled.

### Demo accounts

Every account uses the password **`Lumora@123`**.

| Role | Username | Lands on |
|---|---|---|
| Super Administrator | `superadmin` | Platform administration |
| Administrator | `admin` | School administration |
| Principal | `principal` | Executive dashboard |
| Deputy Principal | `deputy` | Academic operations |
| Registrar | `registrar` | Admissions and records |
| Bursar | `bursar` | Finance dashboard |
| Class Teacher | `grace.wanjiru` | Class management |
| Teacher | `mercy.chebet` | Teaching dashboard |
| Librarian | `librarian` | Library dashboard |
| Student | `student` | Student portal |
| Parent / Guardian | `parent` | Parent portal |

---

## Your school's logo and photos

When Lumora is handed over to a school, an **Administrator** or **Super Administrator**
opens **Administration → School photos** in the sidebar and uploads:

| Picture | Where it appears |
|---|---|
| School logo | Sidebar and sign-in page (until one is uploaded, the "L" mark is used) |
| Sign-in page photo | Beside the sign-in form |
| Dashboard banner | Top of every staff dashboard |
| Student and parent portal banner | Top of the student and parent portals |
| Academics banner | Top of the examinations and results pages |
| Library banner | Top of the circulation desk and catalogue |

JPEG, PNG or WebP, up to 5 MB each. Every upload is checked to be a real image, saved
under a fixed name in `lumora-data/media` (set `MEDIA_DIR` to move it), recorded in the
audit log, and can be reset to the built-in photo at any time. Until a school uploads
its own, built-in photos are shown — see [docs/photo-credits.md](docs/photo-credits.md).

---

## What is in the demo database

The `demo` profile (active by default) seeds a complete school on first start and
does nothing on later starts, so restarting never duplicates data.

- Lumora Secondary School, 2026 academic year, three terms with Term 2 current
- Form 1 to Form 4, two streams each — **96 learners** placed across 8 streams
- 10 subjects, 6 teachers, 80 teaching allocations
- KCSE 12-point grading scale (A to E)
- 16 examinations: the openers are marked and published, the mid-terms are left
  blank so mark entry can be demonstrated live
- 10 school days of attendance registers (960 records)
- Fee structures per class, 96 invoices, 65 payments including 22 M-Pesa
  confirmations
- A library of 8 titles, 32 copies, 6 live loans — one deliberately overdue
- 4 school announcements

To start from an empty database instead, run with `APP_PROFILE=prod`.

---

## Architecture

```
com.lumora.sms
├── entity        JPA entities; every table has audit columns and a @Version
├── repository    Spring Data interfaces — the only code that touches the database
├── service       Business rules and transaction boundaries (interface + impl)
├── controller    Thymeleaf page controllers; controller.api holds the REST writes
├── dto           Validated request records and read-only view models
├── security      Authentication, principal, lockout, audit, password reset
├── config        Security configuration, JPA auditing, demo data seeder
├── exception     Domain exceptions plus web and REST exception handlers
└── web           View helpers exposed to Thymeleaf
```

**Controllers never call repositories.** Every read and write goes through the
service layer, which owns the transaction boundary and the business rules.

### Layers in one request

`AttendanceController` → `AttendanceService.saveRegister(...)` →
`AttendanceSessionRepository` + `AttendanceRecordRepository` → MySQL.

---

## Business rules enforced in the service layer

| Area | Rule |
|---|---|
| Admissions | Admission numbers are unique; the stream and academic year must belong to the learner's school; a learner cannot be admitted before they were born; the learner record and class placement are written in one transaction |
| Attendance | One record per learner, per stream, per day; the register cannot be taken for a future date; a learner outside the stream cannot appear on its register; re-saving corrects rather than duplicates |
| Examinations | A teacher may only enter marks for subjects assigned to them in that stream and term; marks cannot exceed the paper maximum or be negative; grades and points are assigned automatically from the grading scale, computed as a percentage of the paper maximum; published results are immutable |
| Finance | Receipt numbers are unique; a payment can never exceed the invoice balance; a voided invoice cannot receive payments; an invoice is `PAID` only at a zero balance; the bulk invoice run skips anyone already billed, so it is safe to repeat |
| M-Pesa | Transaction codes are unique; the M-Pesa amount must equal its payment amount |
| Library | Only an available copy can be issued; a loan cannot fall due before it is issued; copy status and loan status change together in one transaction |
| Ownership | A learner can only open their own record; a guardian only the children linked to them — checked against the authenticated identity, never a request parameter |

---

## Security

- BCrypt password hashing, database-backed `UserDetailsService`
- Role-gated routes for all eleven roles, plus record-level ownership checks
- Account locks for 15 minutes after 5 failed sign-ins; success clears the lock
- CSRF on every state-changing request; session fixation protection; one
  concurrent session per account; 30-minute idle timeout
- Content-Security-Policy of `script-src 'self'` — no CDN script or stylesheet is
  used anywhere, and the charts are server-rendered SVG rather than a charting
  library, specifically so the policy can stay strict
- Sign-ins, failures, logouts, denied requests and password changes are written to
  `audit_logs`; no password, hash or reset token is ever logged

---

## Tests

```bash
mvn test
```

29 tests covering the grading scale, the payment balance rules, library
circulation, and chart geometry.

---

## Configuration

Everything is overridable by environment variable.

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8081` | HTTP port |
| `APP_PROFILE` | `demo` | Use `prod` for an empty database |
| `DB_URL` | `jdbc:mysql://localhost:3306/lumora_db` | Datasource |
| `DB_USERNAME` / `DB_PASSWORD` | `lumora_user` / `lumora_dev_pass` | Credentials |
| `REMEMBER_ME_KEY` | placeholder | **Replace before any real deployment** |
| `LOCKOUT_MAX_ATTEMPTS` / `LOCKOUT_MINUTES` | `5` / `15` | Account lockout |
| `MAIL_ENABLED` | `false` | When false, reset links are logged, not emailed |
| `PER_TAB_SESSIONS` | `true` | One login per browser tab. **Set `false` in production** |
| `MAX_SESSIONS_PER_USER` | `10` | Concurrent sessions allowed per account |

Flyway owns the schema (`src/main/resources/db/migration`); Hibernate is set to
`ddl-auto=none` and never alters a table.
