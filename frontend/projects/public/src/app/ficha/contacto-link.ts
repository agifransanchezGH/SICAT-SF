import { Contacto } from 'shared';

export interface EnlaceContacto {
  icono: string;
  texto: string;
  href: string | null;
}

const sinEspacios = (v: string) => v.replace(/\s+/g, '');

function url(v: string): string {
  return /^https?:\/\//i.test(v) ? v : `https://${v}`;
}

/** Arma el enlace adecuado según el tipo de canal (web, correo, teléfono, WhatsApp, redes). */
export function enlaceDe(c: Contacto): EnlaceContacto {
  const tipo = c.tipo.toLowerCase();
  const v = c.valor.trim();
  if (tipo.includes('correo') || tipo.includes('mail')) {
    return { icono: 'mail', texto: v, href: `mailto:${v}` };
  }
  if (tipo.includes('whatsapp')) {
    return { icono: 'chat', texto: v, href: `https://wa.me/${sinEspacios(v).replace(/[^\d]/g, '')}` };
  }
  if (tipo.includes('tel') || tipo.includes('celular') || tipo.includes('fijo')) {
    return { icono: 'call', texto: v, href: `tel:${sinEspacios(v)}` };
  }
  if (tipo.includes('instagram')) {
    const usuario = sinEspacios(v).replace(/^@/, '');
    return { icono: 'photo_camera', texto: v, href: v.includes('/') || v.includes('.') ? url(v) : `https://instagram.com/${usuario}` };
  }
  if (tipo.includes('facebook')) {
    return { icono: 'groups', texto: v, href: v.includes('/') || v.includes('.') ? url(v) : `https://facebook.com/search/top?q=${encodeURIComponent(v)}` };
  }
  if (tipo.includes('web') || tipo.includes('sitio') || /^(https?:\/\/|www\.)/i.test(v)) {
    return { icono: 'language', texto: v.replace(/^https?:\/\//i, ''), href: url(v) };
  }
  return { icono: 'info', texto: v, href: null };
}
