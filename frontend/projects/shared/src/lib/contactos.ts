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

/** Característica que se asume cuando el número se cargó sin ella (la base es de la ciudad de Santa Fe). */
const CARACTERISTICA_LOCAL = '342';

const digitos = (v: string) => v.replace(/\D/g, '');

/** Número nacional de 10 dígitos (característica + abonado), y si es celular (venía con el 15). */
interface NumeroArgentino {
  nacional: string;
  celular: boolean;
}

function nacionalDe(d: string): NumeroArgentino | null {
  if (d.length === 10) {
    return { nacional: d, celular: false };
  }
  // Con el 15 de celular después de la característica (2 a 4 dígitos): 342 15 5034137.
  if (d.length === 12) {
    for (const largo of [3, 4, 2]) {
      if (d.substring(largo, largo + 2) === '15') {
        return { nacional: d.substring(0, largo) + d.substring(largo + 2), celular: true };
      }
    }
    return null;
  }
  // Sin característica: se asume la de Santa Fe.
  if (d.length === 7) {
    return { nacional: CARACTERISTICA_LOCAL + d, celular: false };
  }
  if (d.length === 9 && d.startsWith('15')) {
    return { nacional: CARACTERISTICA_LOCAL + d.substring(2), celular: true };
  }
  return null;
}

/**
 * Pasa un teléfono como se carga en Argentina a formato internacional, sin el "+": "(0342) 15 503-4137" →
 * "5493425034137". Toma el primer número del texto (puede traer otro, un interno o una aclaración), quita el 0
 * de la característica y el 15 del celular. Con `celular` agrega el 9 (WhatsApp siempre lo necesita).
 * Devuelve null si no se puede saber el número con seguridad.
 */
export function telefonoInternacional(valor: string, celular = false): string | null {
  // Primer tramo con aspecto de número: dígitos con espacios, guiones, puntos o paréntesis en el medio.
  const tramo = valor.match(/\+?[\d(][\d\s().\-–]*\d/)?.[0];
  if (!tramo) {
    return null;
  }
  const directo = digitos(tramo);
  // 0800, 0810, 0600…: números de servicio que solo se marcan dentro del país.
  if (/^0[6-9]\d0/.test(directo)) {
    return null;
  }
  // Ya internacional: +54 9 342 …, 0054 …, 549…
  const internacional = directo.replace(/^00/, '');
  if ((tramo.startsWith('+') || directo.startsWith('00') || directo.startsWith('54')) && /^54\d{10,11}$/.test(internacional)) {
    const resto = internacional.substring(2).replace(/^9/, '');
    const n = nacionalDe(resto);
    return n ? '54' + (celular || n.celular || internacional.startsWith('549') ? '9' : '') + n.nacional : null;
  }
  // Varios números separados por guiones ("4602921-4603053"): se suman partes hasta tener uno válido.
  let acumulado = '';
  for (const parte of tramo.split(/[-–]/)) {
    acumulado += digitos(parte);
    const n = nacionalDe(acumulado.replace(/^0/, ''));
    if (n) {
      return '54' + (celular || n.celular ? '9' : '') + n.nacional;
    }
  }
  return null;
}

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
    case 'whatsapp': {
      // wa.me necesita el número internacional completo: 54 9 + característica sin 0 + número sin 15.
      const numero = telefonoInternacional(v, true);
      return { icono: 'chat', texto: v, href: numero ? `https://wa.me/${numero}` : null };
    }
    case 'telefono': {
      // Con +54 la llamada funciona también desde un celular extranjero.
      const numero = telefonoInternacional(v);
      // Si no se reconoce (0800, número incompleto) se marca tal cual, siempre que parezca un teléfono.
      const crudo = digitos(v.match(/[\d(][\d\s().\-]*\d/)?.[0] ?? '');
      return { icono: 'call', texto: v, href: numero ? `tel:+${numero}` : crudo.length >= 7 ? `tel:${crudo}` : null };
    }
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
    // Solo aviso en el editor (no entra en el filtro "Contactos a revisar", que se arma en SQL).
    case 'whatsapp':
      return telefonoInternacional(v, true)
        ? null
        : 'No se reconoce el número: cargalo con característica, p. ej. 342 15 5123456. Sin eso no hay enlace';
    default:
      return null;
  }
}
