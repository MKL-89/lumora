package com.lumora.sms.config;

import com.lumora.sms.security.CustomUserDetailsService;
import com.lumora.sms.security.handler.*;
import jakarta.servlet.Filter;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.rememberme.*;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;

/**
 * Spring Security 6 configuration covering authentication, role-based route access,
 * session policy, CSRF, and response hardening headers.
 *
 * <p>Route rules are layered deliberately: dashboards are gated per role, each write
 * API is gated by the role that owns that business operation, and everything else
 * simply requires an authenticated session.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final DashboardAuthenticationSuccessHandler successHandler;
    private final AccountLockoutFailureHandler failureHandler;
    private final AuditingLogoutSuccessHandler logoutSuccessHandler;
    private final LumoraAccessDeniedHandler accessDeniedHandler;
    private final DataSource dataSource;
    private final com.lumora.sms.security.AccountSecurityStateService accountState;

    @Value("${app.security.remember-me.key}")
    private String rememberMeKey;

    /** See {@link SessionTrackingConfig} for what this changes and why. */
    @Value("${app.security.per-tab-sessions:false}")
    private boolean perTabSessions;

    /** Concurrent sessions permitted for one account; per-tab mode needs more than one. */
    @Value("${app.security.max-sessions-per-user:1}")
    private int maxSessionsPerUser;

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    /**
     * Where the CSRF token is kept.
     *
     * <p>The cookie repository stores one token per browser. Under per-tab sessions
     * that is the wrong scope: signing in on one tab rotates the shared token and
     * invalidates the form another tab is still holding, which surfaces as a 403 on
     * the sign-in page. Keeping the token in the session puts it at exactly the same
     * scope as the login it protects.
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return perTabSessions
                ? new HttpSessionCsrfTokenRepository()
                : CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        repository.setCreateTableOnStartup(false);
        return repository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider provider) throws Exception {
        http.authenticationProvider(provider)
            .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository()))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; "
                    + "script-src 'self'; "
                    + "style-src 'self' 'unsafe-inline'; "
                    + "img-src 'self' data:; "
                    + "font-src 'self'; "
                    + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'"))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.migrateSession())
                .invalidSessionUrl("/login?expired")
                // One session per account is right when a browser holds one session.
                // Under per-tab sessions the same account may legitimately be open in
                // several tabs, so the limit is raised rather than silently expiring
                // the tab the user was just working in.
                .maximumSessions(maxSessionsPerUser)
                .maxSessionsPreventsLogin(false))
            .authorizeHttpRequests(auth -> auth
                // Public surface.
                .requestMatchers("/login", "/setup", "/password-reset/**", "/css/**", "/js/**", "/images/**",
                                 "/favicon.ico", "/error").permitAll()
                // School logo and photos: the sign-in page shows them before anyone
                // is signed in. Changing them is an administrator task (below).
                .requestMatchers(HttpMethod.GET, "/media/*").permitAll()
                .requestMatchers("/settings/**").hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR")

                // Role-gated dashboards.
                .requestMatchers("/dashboard/super-admin/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/dashboard/administrator/**").hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR")
                .requestMatchers("/dashboard/principal/**").hasAnyRole("SUPER_ADMIN", "PRINCIPAL")
                .requestMatchers("/dashboard/deputy-principal/**").hasAnyRole("SUPER_ADMIN", "PRINCIPAL", "DEPUTY_PRINCIPAL")
                .requestMatchers("/dashboard/bursar/**").hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "BURSAR")
                .requestMatchers("/dashboard/registrar/**").hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "REGISTRAR")
                .requestMatchers("/dashboard/class-teacher/**").hasAnyRole("SUPER_ADMIN", "CLASS_TEACHER")
                .requestMatchers("/dashboard/teacher/**").hasAnyRole("SUPER_ADMIN", "TEACHER", "CLASS_TEACHER")
                .requestMatchers("/dashboard/student/**").hasRole("STUDENT")
                .requestMatchers("/dashboard/parent/**").hasRole("PARENT")
                .requestMatchers("/dashboard/librarian/**").hasAnyRole("SUPER_ADMIN", "LIBRARIAN")

                // Write APIs are restricted to the roles that own each business operation.
                .requestMatchers("/api/attendance/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "PRINCIPAL", "DEPUTY_PRINCIPAL", "TEACHER", "CLASS_TEACHER")
                .requestMatchers("/api/results/**")
                    .hasAnyRole("PRINCIPAL", "TEACHER", "CLASS_TEACHER")
                .requestMatchers("/api/finance/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "BURSAR")
                .requestMatchers("/api/library/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "LIBRARIAN")

                // Results are seen only by teachers, the principal, and the learner
                // or their guardians. Mark sheets show every candidate's result, so
                // they belong to the people who enter marks (teachers) and the
                // principal, who alone publishes them.
                .requestMatchers(HttpMethod.POST, "/examinations/*/publish").hasRole("PRINCIPAL")
                .requestMatchers(HttpMethod.POST, "/examinations/**").hasAnyRole("PRINCIPAL", "TEACHER", "CLASS_TEACHER")
                // The deputy and administrators may read mark sheets, not enter marks.
                .requestMatchers("/examinations/**")
                    .hasAnyRole("PRINCIPAL", "DEPUTY_PRINCIPAL", "ADMINISTRATOR", "TEACHER", "CLASS_TEACHER")

                // Results lookup by admission number. Learners and guardians may only
                // open their own records; ExaminationController checks ownership.
                .requestMatchers("/results/**")
                    .hasAnyRole("PRINCIPAL", "DEPUTY_PRINCIPAL", "ADMINISTRATOR", "TEACHER", "CLASS_TEACHER",
                                "STUDENT", "PARENT")

                // Registers are entered by teaching staff and leadership.
                .requestMatchers(HttpMethod.POST, "/attendance/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "PRINCIPAL", "DEPUTY_PRINCIPAL", "TEACHER", "CLASS_TEACHER")

                // Registers and timetables name every learner, so they are staff-only.
                .requestMatchers("/attendance/**", "/subjects/**", "/classes/**", "/timetable/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "PRINCIPAL", "DEPUTY_PRINCIPAL",
                                "REGISTRAR", "TEACHER", "CLASS_TEACHER")

                .requestMatchers("/teachers/**", "/parents/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "PRINCIPAL")

                // Same roles that see the "Post an announcement" panel.
                .requestMatchers(HttpMethod.POST, "/announcements/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "PRINCIPAL", "DEPUTY_PRINCIPAL", "CLASS_TEACHER")

                // Admitting a learner (the form and the save itself) and deactivating
                // a record belong to the principal and the school administrators only.
                .requestMatchers(HttpMethod.POST, "/students", "/students/", "/students/import")
                    .hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "PRINCIPAL")
                .requestMatchers("/students/new", "/students/import", "/students/import/**",
                                 "/students/*/deactivate", "/students/*/reactivate")
                    .hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "PRINCIPAL")

                // The learner roll is staff-only. A guardian or learner reaching an
                // individual record still has to prove ownership in StudentController,
                // but they never get the list in the first place.
                .requestMatchers("/students").hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "PRINCIPAL",
                                                         "DEPUTY_PRINCIPAL", "REGISTRAR", "TEACHER",
                                                         "CLASS_TEACHER", "BURSAR")

                .requestMatchers("/fees/**", "/payments/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "PRINCIPAL", "BURSAR")
                .requestMatchers("/users/**", "/roles/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR")

                // The circulation desk lists every borrower, so it is staff-only.
                // The catalogue itself stays open to everyone who can sign in.
                .requestMatchers("/library/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMINISTRATOR", "PRINCIPAL", "DEPUTY_PRINCIPAL",
                                "LIBRARIAN", "TEACHER", "CLASS_TEACHER")

                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "remember-me", "XSRF-TOKEN")
                .logoutSuccessHandler(logoutSuccessHandler))
            .exceptionHandling(handling -> handling.accessDeniedHandler(accessDeniedHandler))
            // A temporary password must be replaced before anything else can be used.
            .addFilterBefore(new PasswordChangeRequiredFilter(accountState),
                    org.springframework.security.web.access.intercept.AuthorizationFilter.class);

        // Remember-me is deliberately off under per-tab sessions. Its token is a
        // browser-wide cookie, so a brand-new tab would silently be signed in as the
        // remembered user — the opposite of one independent login per tab.
        if (!perTabSessions) {
            http.rememberMe(remember -> remember
                    .key(rememberMeKey)
                    .tokenRepository(persistentTokenRepository())
                    .tokenValiditySeconds(1_209_600)
                    .userDetailsService(userDetailsService));
        }

        DefaultSecurityFilterChain chain = (DefaultSecurityFilterChain) http.build();
        return perTabSessions ? allowUrlSessionTracking(chain) : chain;
    }

    /**
     * Removes {@code DisableEncodeUrlFilter} from the chain.
     *
     * <p>Spring Security inserts that filter by default and it wraps the response so
     * that {@code encodeURL} can never append a session id. That is sound hardening —
     * a session id in a URL can leak through history, bookmarks and {@code Referer}
     * headers — and it is precisely why URL session tracking appears to do nothing
     * until the filter is taken out.
     *
     * <p>Removing it is therefore a deliberate, scoped trade: it happens only when
     * per-tab sessions are switched on, which should never be the case in production.
     */
    private DefaultSecurityFilterChain allowUrlSessionTracking(DefaultSecurityFilterChain chain) {
        List<Filter> filters = chain.getFilters().stream()
                .filter(filter -> !(filter instanceof DisableEncodeUrlFilter))
                .toList();
        return new DefaultSecurityFilterChain(chain.getRequestMatcher(), filters);
    }

    /**
     * The strict firewall rejects any URL containing a semicolon, which is the very
     * character that carries {@code ;jsessionid=}. Semicolons are permitted only
     * while per-tab sessions are enabled; every other firewall protection is left at
     * its default.
     */
    @Bean
    public HttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowSemicolon(perTabSessions);
        return firewall;
    }
}
