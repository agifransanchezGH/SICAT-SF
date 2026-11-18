/**
 * Texto para comparar o buscar: sin acentos, minúsculas, sin puntuación, espacios simples. Es la misma regla
 * que `Texto.clave` del backend, así lo que el panel considera igual coincide con lo que considera la importación.
 */
export function normalizar(s: string | null | undefined): string {
  return (s ?? '')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9ñ]+/g, ' ')
    .trim();
}

/** Parecido entre dos textos ya normalizados, de 0 a 1 (distancia de Levenshtein relativa). */
export function similitud(a: string, b: string): number {
  if (a === b) {
    return 1;
  }
  if (!a.length || !b.length) {
    return 0;
  }
  let previa = Array.from({ length: b.length + 1 }, (_, j) => j);
  for (let i = 1; i <= a.length; i++) {
    const actual = [i];
    for (let j = 1; j <= b.length; j++) {
      actual[j] = Math.min(previa[j] + 1, actual[j - 1] + 1, previa[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1));
    }
    previa = actual;
  }
  return 1 - previa[b.length] / Math.max(a.length, b.length);
}

const MENORES = new Set(['de', 'del', 'la', 'las', 'los', 'el', 'y', 'e', 'en', 'a', 'al', 'con', 'por', 'para', 'o', 'u']);
const ROMANO = /^(?=[ivxlcdm]+$)m{0,3}(cm|cd|d?c{0,3})(xc|xl|l?x{0,3})(ix|iv|v?i{0,3})$/i;

function palabra(p: string, primera: boolean): string {
  const letras = p.replace(/[^\p{L}]/gu, '');
  // Siglas que conviene dejar como están: con puntos (C.S, S.A.), romanos (II, XX) o sin vocales (SF, CS).
  if (/\p{L}\.\p{L}/u.test(p) || (letras.length > 1 && ROMANO.test(letras)) || (letras.length > 1 && !/[aeiouáéíóú]/i.test(letras))) {
    return p;
  }
  const minuscula = p.toLocaleLowerCase('es-AR');
  if (!primera && MENORES.has(minuscula)) {
    return minuscula;
  }
  // La primera letra (después de comillas o paréntesis) en mayúscula.
  return minuscula.replace(/\p{L}/u, (l) => l.toLocaleUpperCase('es-AR'));
}

/**
 * Nombres cargados todo en mayúsculas (así vienen muchos de la importación) se muestran en tipo título:
 * "DEMOS – COMUNIDAD CULTURAL Y DE ESTUDIOS" → "Demos – Comunidad Cultural y de Estudios".
 * Solo cambia la presentación; si el nombre ya tiene minúsculas, se respeta tal cual.
 */
export function nombreLegible(nombre: string | null | undefined): string {
  const n = (nombre ?? '').trim();
  if (!/\p{Lu}/u.test(n) || /\p{Ll}/u.test(n)) {
    return n;
  }
  let primera = true;
  return n.replace(/\S+/g, (p) => {
    const r = palabra(p, primera);
    primera = false;
    return r;
  });
}
