package ar.sf.sicat.publica.config;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Límite de pedidos por IP (Bucket4j en memoria). Detrás de un proxy, configurar
 * {@code server.forward-headers-strategy=native} para que getRemoteAddr sea la IP real.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class LimitePedidosFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    private final long pedidosPorMinuto;

    public LimitePedidosFilter(@Value("${sicat.rate-limit.pedidos-por-minuto:120}") long pedidosPorMinuto) {
        this.pedidosPorMinuto = pedidosPorMinuto;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Bucket bucket = buckets.get(request.getRemoteAddr(), ip -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(pedidosPorMinuto)
                        .refillGreedy(pedidosPorMinuto, Duration.ofMinutes(1))
                        .build())
                .build());
        if (!bucket.tryConsume(1)) {
            response.setContentType("application/problem+json");
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("""
                    {"type":"about:blank","title":"Too Many Requests","status":429,\
                    "detail":"Demasiados pedidos, intente nuevamente en un minuto"}""");
            return;
        }
        chain.doFilter(request, response);
    }
}
