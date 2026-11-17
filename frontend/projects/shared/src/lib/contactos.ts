/**
 * Cómo se arma el enlace de un canal de contacto a partir de lo que se cargó en "Valor".
 * Lo usan la ficha del sitio público (el enlace) y el editor del panel (vista previa, ayuda y aviso).
 * Las reglas de "a revisar" se repiten en SQL en el filtro del panel (EstablecimientoRepository): si se
 * cambia una, cambiar la otra.
 */

export interface EnlaceContacto {
  icono: string;
  /** Lo que se muestra: "@usuario", "facebook.com/pagina", el teléfono… */
  texto: string;
  /** null cuando no se puede armar un enlace confiable. */
  href: string | null;
}

/** Usuario de Instagram: letras, números, punto y guion bajo, hasta 30. */
const USUARIO_INSTAGRAM = /^@?[A-Za-z0-9._]{1,30}$/;
/** Usuario de Facebook (la parte final de facebook.com/…): letras, números y punto, 5 o más. */
const USUARIO_FACEBOOK = /^@?[A-Za-z0-9.]{5,}$/;
const URL_INSTAGRAM = /^(https?:\/\/)?(www\.)?instagram\.com\//i;
const URL_FACEBOOK = /^(https?:\/\/)?(www\.|m\.|web\.)?(facebook|fb)\.(com|me)\//i;

const sinEspacios = (v: string) => v.replace(/\s+/g, '');
const conProtocolo = (v: string) => (/^https?:\/\//i.test(v) ? v : `https://${v}`);
/** "https://www.facebook.com/club/" → "facebook.com/club" */
const corta = (v: string) => v.replace(/^https?:\/\//i, '').replace(/^www\./i, '').replace(/[/?#]+$/, '');

type Canal = 'correo' | 'whatsapp' | 'telefono' | 'instagram' | 'facebook' | 'web' | 'otro';

function canal(tipo: string, valor: string): Canal {
  const t = tipo.toLowerCase();
  if (t.includes('correo') || t.includes('mail')) return 'correo';
  if (t.includes('whatsapp')) return 'whatsapp';
  if (t.includes('tel') || t.includes('celular') || t.includes('fijo')) return 'telefono';
  if (t.includes('instagram')) return 'instagram';
  if (t.includes('facebook')) return 'facebook';
  if (t.includes('web') || t.includes('sitio') || /^(https?:\/\/|www\.)/i.test(valor)) return 'web';
  return 'otro';
}

/** Arma el enlace según el tipo de canal (correo, WhatsApp, teléfono, Instagram, Facebook, web). */
export function enlaceDe(tipo: string, valor: string): EnlaceContacto {
  const v = valor.trim();
  switch (canal(tipo, v)) {
    case 'correo':
      return { icono: 'mail', texto: v, href: `mailto:${v}` };
    case 'whatsapp':
      return { icono: 'chat', texto: v, href: `https://wa.me/${v.replace(/[^\d]/g, '')}` };
    case 'telefono':
      return { icono: 'call', texto: v, href: `tel:${sinEspacios(v)}` };
    case 'instagram': {
      if (URL_INSTAGRAM.test(v)) {
        const usuario = corta(v).split('/')[1];
        return { icono: 'photo_camera', texto: usuario ? `@${usuario}` : corta(v), href: conProtocolo(v) };
      }
      if (USUARIO_INSTAGRAM.test(v)) {
        const usuario = v.replace(/^@/, '');
        return { icono: 'photo_camera', texto: `@${usuario}`, href: `https://www.instagram.com/${usuario}` };
      }
      return { icono: 'photo_camera', texto: v, href: null };
    }
    case 'facebook': {
      if (URL_FACEBOOK.test(v)) {
        return { icono: 'groups', texto: corta(v), href: conProtocolo(v) };
      }
      if (USUARIO_FACEBOOK.test(v)) {
        const usuario = v.replace(/^@/, '');
        return { icono: 'groups', texto: `facebook.com/${usuario}`, href: `https://www.facebook.com/${usuario}` };
      }
      // Un nombre ("Hotel Bertaina") no alcanza para saber la dirección: se ofrece la búsqueda.
      return { icono: 'groups', texto: v, href: `https://www.facebook.com/search/top?q=${encodeURIComponent(v)}` };
    }
    case 'web':
      return /\s/.test(v)
        ? { icono: 'language', texto: v, href: null }
        : { icono: 'language', texto: corta(v), href: conProtocolo(v) };
    default:
      return { icono: 'info', texto: v, href: null };
  }
}

/** Qué conviene cargar en "Valor" para cada tipo de canal (ayuda del editor). */
export function ayudaDe(tipo: string): string {
  switch (canal(tipo, '')) {
    case 'instagram':
      return 'El usuario (@usuario) o la URL del perfil';
    case 'facebook':
      return 'La URL de la página: https://www.facebook.com/…';
    case 'web':
      return 'La dirección, p. ej. www.ejemplo.com.ar';
    case 'whatsapp':
      return 'Con código de área, p. ej. 342 155 123456';
    case 'correo':
      return 'p. ej. reservas@ejemplo.com';
    default:
      return '';
  }
}

/** Por qué un valor no sirve para armar un buen enlace, o null si está bien. */
export function problemaDe(tipo: string, valor: string): string | null {
  const v = valor.trim();
  if (!v) return null;
  switch (canal(tipo, v)) {
    case 'instagram':
      return URL_INSTAGRAM.test(v) || USUARIO_INSTAGRAM.test(v)
        ? null
        : 'No es un usuario de Instagram (sin espacios) ni la URL del perfil: no se va a mostrar enlace';
    case 'facebook':
      return URL_FACEBOOK.test(v) || USUARIO_FACEBOOK.test(v)
        ? null
        : 'Es un nombre, no la URL de la página: el sitio solo puede ofrecer una búsqueda en Facebook';
    case 'web':
      return /\s/.test(v) ? 'Una dirección web no lleva espacios: no se va a mostrar enlace' : null;
    default:
      return null;
  }
}
