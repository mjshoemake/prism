package com.prism.api.mcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import com.prism.common.logging.LogCategory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * MCP server wiring that isn't covered by Spring AI's auto-configuration (docs/MCP.md §4.4). Active only with
 * {@code prism.mcp.enabled=true}.
 * <p>
 * Until authentication exists (backlog #5), the MCP endpoint answers only requests from this machine.
 * TODO: replace with token authentication (MCP authorization spec) before the server is reachable by anyone else.
 */
@Configuration
@ConditionalOnProperty(name = "prism.mcp.enabled", havingValue = "true")
public class McpConfig {

    private static final Logger log = LogCategory.APPLICATION.logger(McpConfig.class);
    private static final Logger audit = LogCategory.AUDIT.logger();

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> mcpLocalOnlyFilter() {
        log.debug("Started");
        var registration = new FilterRegistrationBean<OncePerRequestFilter>(new LocalOnlyFilter());
        registration.addUrlPatterns("/mcp", "/mcp/*");
        registration.setName("mcpLocalOnly");
        log.info("MCP server on: /mcp (local requests only)");
        return registration;
    }

    /** Rejects requests that don't come from a loopback address (403). */
    static final class LocalOnlyFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            log.debug("Started");
            if (isLoopback(request.getRemoteAddr())) {
                chain.doFilter(request, response);
            } else {
                audit.warn("MCP_REQUEST_REJECTED remote={} reason=not_local", request.getRemoteAddr());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "The MCP server only accepts local requests.");
            }
        }

        static boolean isLoopback(String address) {
            log.debug("Started");
            try {
                return address != null && InetAddress.getByName(address).isLoopbackAddress();
            } catch (UnknownHostException e) {
                return false;
            }
        }
    }
}
