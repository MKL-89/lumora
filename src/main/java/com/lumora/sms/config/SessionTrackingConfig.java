package com.lumora.sms.config;

import jakarta.servlet.SessionTrackingMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import jakarta.annotation.PostConstruct;
import java.util.EnumSet;

/**
 * Decides how a request is tied to its HTTP session.
 *
 * <p><strong>Why this exists.</strong> By default a servlet container identifies the
 * session with the {@code JSESSIONID} cookie. Cookies are stored per browser
 * profile, not per tab, so every tab of the same browser sends the same
 * {@code JSESSIONID} and therefore shares one session and one authenticated user.
 * Signing in on a second tab replaces the first tab's principal, rotates the CSRF
 * token underneath it, and trips concurrent-session control — which is what
 * produced the 403 on the sign-in page.
 *
 * <p><strong>What per-tab mode does.</strong> With {@code app.security.per-tab-sessions}
 * enabled the container tracks sessions through the URL instead of a cookie. A
 * session id lives in the address bar, and an address bar belongs to exactly one
 * tab, so each tab carries its own session and its own authenticated user. Nothing
 * is shared, so nothing collides.
 *
 * <p>Thymeleaf runs every {@code @{...}} link through
 * {@code HttpServletResponse.encodeURL}, and Spring's redirects go through
 * {@code encodeRedirectURL}, so the id propagates across the whole application
 * without a single template change.
 *
 * <p><strong>The trade-off.</strong> A session id in a URL can leak through the
 * address bar, browser history, bookmarks and {@code Referer} headers. That is an
 * acceptable price for a local demonstration and a poor one on the public internet,
 * which is why this is a flag and why it defaults to cookie tracking. Leave it off
 * for any real deployment and put the system behind HTTPS.
 */
@Slf4j
@Configuration
public class SessionTrackingConfig {

    @Value("${app.security.per-tab-sessions:false}")
    private boolean perTabSessions;

    private final SpringTemplateEngine templateEngine;

    public SessionTrackingConfig(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Teaches Thymeleaf to rewrite link expressions again. Only needed under per-tab
     * sessions, but harmless either way: {@code encodeURL} is a no-op when the
     * container is tracking sessions by cookie.
     */
    @PostConstruct
    void enableLinkRewriting() {
        if (perTabSessions) {
            templateEngine.setLinkBuilder(new SessionAwareLinkBuilder());
        }
    }

    @Bean
    public ServletContextInitializer sessionTrackingModeInitializer() {
        return servletContext -> {
            if (perTabSessions) {
                servletContext.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.URL));
                log.info("Per-tab sessions ENABLED: sessions are tracked in the URL, so every "
                        + "browser tab holds its own independent login.");
            } else {
                servletContext.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE));
                log.info("Per-tab sessions disabled: sessions are tracked by cookie and are "
                        + "shared across the tabs of one browser.");
            }
        };
    }
}
