package com.lumora.sms.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;

/**
 * Restores URL rewriting for Thymeleaf link expressions.
 *
 * <p>Thymeleaf 2 and 3.0 passed every {@code @{...}} link through
 * {@code HttpServletResponse.encodeURL}, which is how a servlet container tracks a
 * session when it cannot use a cookie. Thymeleaf 3.1 removed that call while
 * decoupling itself from the servlet API, leaving {@code processLink} as an empty
 * hook. Without it, URL-based session tracking silently does nothing: the container
 * is willing to rewrite links, but nothing ever asks it to.
 *
 * <p>This puts the call back, in one place, for every link and form action in the
 * application — no template changes. Whether a link is actually rewritten stays the
 * container's decision: {@code encodeURL} returns the URL untouched whenever the
 * session is already tracked by a cookie.
 */
public class SessionAwareLinkBuilder extends StandardLinkBuilder {

    @Override
    protected String processLink(final IExpressionContext context, final String link) {
        if (link == null || link.isEmpty()) {
            return link;
        }
        HttpServletResponse response = currentResponse();
        if (response == null) {
            // Rendered outside a request, e.g. a scheduled job building an email body.
            return link;
        }
        return response.encodeURL(link);
    }

    private HttpServletResponse currentResponse() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getResponse();
        }
        return null;
    }
}
