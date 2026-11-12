# 0005 · Refresh token sin estado y ADMIN inicial por variables de entorno

- Estado: Aceptado
- Fecha: 2026-11-12

## Contexto
`ARQUITECTURA.md` pide login con JWT y refresh token, pero el modelo de datos no tiene una tabla para guardar refresh tokens. Además, sin un primer usuario ADMIN nadie puede entrar al panel.

## Decisión

### Tokens
- Access y refresh son JWT HS256 firmados con `SICAT_JWT_SECRET`, emisor `sicat-admin-api`.
- Se distinguen por el claim `tipo` (`access` | `refresh`):
  - el resource server solo acepta `tipo = access`, así un refresh token no sirve para llamar a la API;
  - `/auth/refresh` solo acepta `tipo = refresh`.
- Vida: access `SICAT_JWT_EXPIRACION_MINUTOS` (60), refresh `SICAT_JWT_REFRESH_EXPIRACION_DIAS` (7).
- El refresh no se guarda en la base. Al renovar se verifica que el usuario exista y esté activo.
- Claims del access: `sub` (id de usuario), `email`, `nombre`, `roles` (`[ADMIN|CARGADOR|CONSULTA]`).
- `/auth/login` y `/auth/refresh` son públicos (el refresh token es la credencial); `/auth/me` requiere access token.

### ADMIN inicial
- Al arrancar, si `seguridad.usuario` está vacía, la API Admin crea un ADMIN con `SICAT_ADMIN_INICIAL_EMAIL` / `SICAT_ADMIN_INICIAL_PASSWORD` (`AdminInicial`).
- Si ya hay usuarios no hace nada. Ninguna contraseña queda en el código ni en las migraciones.

### Reglas de usuarios
- Emails únicos, sin distinguir mayúsculas (se guardan en minúscula). Repetido → 409.
- En la etapa 1 solo se asignan ADMIN, CARGADOR y CONSULTA (OFERENTE → 400).
- Un ADMIN no puede desactivarse ni quitarse el rol ADMIN a sí mismo (→ 422).
- Contraseñas con BCrypt, entre 8 y 72 caracteres.

## Consecuencias
- No hace falta una tabla nueva.
- No se puede revocar un refresh token puntual antes de que venza; desactivar al usuario corta la renovación,
  y el access token vigente sigue sirviendo hasta su vencimiento (máximo 60 minutos).
- Cerrar sesión en el front borra los tokens del navegador.
- Si más adelante se necesita revocación (p. ej. "cerrar todas las sesiones"), agregar `seguridad.refresh_token`
  con una migración nueva.
