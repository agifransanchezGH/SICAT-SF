package ar.sf.sicat.publica.config;

import java.io.IOException;
import java.util.Set;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** La API pública solo acepta GET (y HEAD/OPTIONS para CORS). Cualquier otro método → 405. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SoloLecturaFilter extends OncePerRequestFilter {

    private static final Set<String> PERMITIDOS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!PERMITIDOS.contains(request.getMethod())) {
            response.setHeader(HttpHeaders.ALLOW, "GET, HEAD, OPTIONS");
            response.setContentType("application/problem+json");
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
            response.getWriter().write("""
                    {"type":"about:blank","title":"Method Not Allowed","status":405,\
                    "detail":"La API pública es de solo lectura"}""");
            return;
        }
        chain.doFilter(request, response);
    }
}
