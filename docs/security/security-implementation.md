# Lumora security implementation

## Authentication and account protection

Lumora uses Spring Security 6 with a database-backed `UserDetailsService`, BCrypt password hashes, session fixation protection, a 30-minute configurable idle session, one concurrent session per account, and persistent 14-day remember-me tokens.

Failed sign-ins are audited. A known account is temporarily locked for 15 minutes after five failed attempts; both values are configurable through environment variables. Successful login clears the temporary lock state. Logins, logout, failed login attempts, reset requests, password changes, and denied requests are written to `audit_logs` without passwords or reset tokens.

Password reset tokens are 256-bit random values. Only a SHA-256 hash is stored; tokens are one-use and expire in 30 minutes. Configure SMTP (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`) and set `MAIL_ENABLED=true` to deliver reset links. When mail is disabled or the SMTP server is unreachable, the link is written to the application log and the reset still succeeds — the availability of mail never decides whether a password can be recovered.

## Dashboard authorization

| Role | Login destination |
|---|---|
| Super Administrator | `/dashboard/super-admin` |
| Administrator | `/dashboard/administrator` |
| Principal | `/dashboard/principal` |
| Deputy Principal | `/dashboard/deputy-principal` |
| Bursar | `/dashboard/bursar` |
| Registrar | `/dashboard/registrar` |
| Class Teacher | `/dashboard/class-teacher` |
| Teacher | `/dashboard/teacher` |
| Librarian | `/dashboard/librarian` |
| Parent/Guardian | `/dashboard/parent` |
| Student | `/dashboard/student` |

An account holding several roles lands on the broadest dashboard it is entitled to; the precedence order lives in `DashboardAuthenticationSuccessHandler`.

Every dashboard URL is restricted by role in `SecurityConfig`. Module screens and the REST write endpoints are restricted by the role that owns each business operation:

| Surface | Permitted roles |
|---|---|
| `/students` (the roll) | All staff roles |
| `/students/new`, deactivate, reactivate | Super Admin, Administrator, Principal, Registrar |
| `/fees/**`, `/payments/**`, `/api/finance/**` | Super Admin, Administrator, Principal, Bursar |
| `/api/attendance/**`, `/api/results/**` | Super Admin, Administrator, Principal, Deputy Principal, Teacher, Class Teacher |
| `/library/**`, `/api/library/**` | Super Admin, Administrator, Principal, Deputy Principal, Librarian, Teacher, Class Teacher |
| `/books` (catalogue) | Every signed-in user |
| `/users/**`, `/roles/**` | Super Admin, Administrator |

The navigation is generated with `sec:authorize`, so a user is never shown a link that would refuse them.

## Record-level ownership

URL role rules alone are never used for record-level authorization. Student and Parent/Guardian access is proven in the service layer by `DashboardAccessService`:

- a learner may open only the student record their account is attached to;
- a guardian may open only learners explicitly linked to them in `student_guardians`.

The check that applies is decided from the **authenticated authorities**, not from a request parameter, so a guardian cannot widen their own access by editing the URL. A failure raises `AccessDeniedException`, which is reported as **403 Forbidden** and written to `audit_logs` as `ACCESS_DENIED`.

The learner roll and the circulation desk are staff-only, so a learner or guardian is never handed a list of other people's records to begin with.

## Session scope and per-tab logins

A servlet container identifies a session with the `JSESSIONID` cookie, and cookies are
stored per browser profile rather than per tab. Every tab of one browser therefore
sends the same id, shares one session, and shares one authenticated user. Signing in
on a second tab replaces the first tab's principal, rotates the CSRF token underneath
it and trips concurrent-session control — which is what produced a 403 on the sign-in
page.

With `app.security.per-tab-sessions=true` the container tracks sessions through the
URL instead. An address bar belongs to exactly one tab, so each tab carries its own
session and its own user. Three things have to change together for this to work:

| Change | Reason |
|---|---|
| `SessionTrackingConfig` sets the tracking mode to `URL` | Stops the session being identified by a browser-wide cookie |
| `SessionAwareLinkBuilder` restores `response.encodeURL` for Thymeleaf links | Thymeleaf 3.1 removed the automatic call, so nothing propagated the id |
| `DisableEncodeUrlFilter` is removed from the filter chain | Spring Security inserts it specifically to stop session ids appearing in URLs |
| `StrictHttpFirewall` permits semicolons | `;jsessionid=` would otherwise be rejected as a malicious URL |
| CSRF moves to `HttpSessionCsrfTokenRepository` | A cookie-stored token is browser-wide; the token must share the scope of the login it protects |
| Remember-me is disabled | Its token is a browser-wide cookie and would silently sign a brand-new tab in |

**This is a demonstration convenience, not a hardening measure.** A session id in a
URL can leak through browser history, bookmarks, shared links and `Referer` headers,
and Spring Security ships a filter whose only job is to prevent it. Leave
`PER_TAB_SESSIONS=false` for any real deployment; the application then returns to
ordinary cookie sessions with remember-me enabled, and no other behaviour changes.

## Web protections

- CSRF is enabled with a cookie token repository; all state-changing requests, including login/logout/reset, require a CSRF token.
- The Content-Security-Policy is `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'`. The application loads **no** CDN script, stylesheet, or font: charts are server-rendered inline SVG rather than a charting library, precisely so the policy can stay this strict.
- Session cookies are HTTP-only and `SameSite=Lax`; set `SESSION_COOKIE_SECURE=true` under HTTPS.
- Remember-me uses server-side persistent tokens and a secret supplied through `REMEMBER_ME_KEY`.
- No password, password hash, or raw reset token is placed in audit logs.
- Domain failures are rendered as a styled error page for browsers and as RFC 7807 `ProblemDetail` JSON for the REST endpoints; unexpected exceptions are logged in full but reported to the user in general terms.

## Demo and development accounts

The `demo` profile (active by default) seeds one account per role. The usernames are listed in the project README; the password for all of them is `Lumora@123`, stored only as a BCrypt hash.

The older `dev` profile creates a parallel set named `test_<role>` with the same password. Neither profile should be enabled outside a local environment; run with `APP_PROFILE=prod` for a clean database with no sample accounts.
