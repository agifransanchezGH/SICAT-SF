/** Modelos compartidos. Deben coincidir con los DTO de las APIs (ARQUITECTURA.md §7 y §9). */

export interface Coordenadas {
  lat: number;
  lng: number;
}

/** Respuesta paginada estándar (PaginaDto en sicat-core). */
export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface EstablecimientoResumen {
  id: number;
  nombre: string;
  localidad: string | null;
  categorias: string[];
  coordenadas: Coordenadas | null;
  destacado: boolean;
}

export interface Contacto {
  tipo: string;
  valor: string;
}

export interface Horario {
  diaSemana: number; // 1 = lunes … 7 = domingo
  desde: string; // HH:mm
  hasta: string;
}

export interface Caracteristica {
  grupo: string;
  nombre: string;
  cantidad: number | null;
}

export interface Distincion {
  nombre: string;
  anio: number | null;
}

export interface OfertaFicha {
  id: number;
  categoria: string;
  categoriasAdicionales: string[];
  horarios: Horario[];
  /** Solo los campos públicos de la especialización (alojamiento, circuito, salones, visitas). */
  detalle: Record<string, unknown>;
}

export interface EstablecimientoFicha {
  id: number;
  nombre: string;
  descripcion: string | null;
  direccion: string | null;
  localidad: string | null;
  coordenadas: Coordenadas | null;
  contactos: Contacto[];
  caracteristicas: Caracteristica[];
  distinciones: Distincion[];
  ofertas: OfertaFicha[];
}
