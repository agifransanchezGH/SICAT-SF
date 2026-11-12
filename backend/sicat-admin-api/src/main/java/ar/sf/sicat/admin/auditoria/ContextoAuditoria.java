package ar.sf.sicat.admin.auditoria;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ar.sf.sicat.core.auditoria.OrigenAuditoria;

/**
 * Quién y desde dónde se hace un cambio, sin que los servicios de negocio tengan que informarlo:
 * <ul>
 *   <li>usuario: el {@code sub} del JWT del pedido en curso (null si no hay usuario autenticado);</li>
 *   <li>origen: MIGRACION si el pedido es a /importacion/**, APP si hay usuario, SISTEMA si no
 *       (p. ej. el ADMIN inicial creado al arrancar).</li>
 * </ul>
 */
final class ContextoAuditoria {

    private ContextoAuditoria() {
    }

    static Long idUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            try {
                return Long.valueOf(jwt.getToken().getSubject());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    static OrigenAuditoria origenActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                && attrs.getRequest().getRequestURI().contains("/importacion/")) {
            return OrigenAuditoria.MIGRACION;
        }
        return idUsuarioActual() != null ? OrigenAuditoria.APP : OrigenAuditoria.SISTEMA;
    }
}
