/** Modelos compartidos. Los de la API Pública deben coincidir con sus DTO (ARQUITECTURA.md §9). */

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

/** Ítem del listado público. */
export interface EstablecimientoResumen {
  id: number;
  nombre: string;
  direccion?: string;
  localidad?: string;
  coordenadas?: Coordenadas;
  categorias: string[];
  destacado: boolean;
  /** Solo en /cercanos */
  distanciaMetros?: number;
}

export interface CategoriaPublica {
  id: number;
  nombre: string;
  hijos: CategoriaPublica[];
}

export interface LocalidadPublica {
  id: number;
  nombre: string;
  provincia: string;
}

export interface Contacto {
  tipo: string;
  valor: string;
  uso?: string;
}

export interface Horario {
  diaSemana: number; // 1 = lunes … 7 = domingo
  desde: string; // HH:mm
  hasta: string;
}

export interface Caracteristica {
  grupo: string;
  nombre: string;
  cantidad?: number;
}

export interface Distincion {
  nombre: string;
  organismoOtorgante?: string;
  anio?: number;
  urlVerificacion?: string;
}

export interface AlojamientoPublico {
  estrellas?: number;
  cantHabitaciones?: number;
  totalPlazas?: number;
  habAccesibles?: number;
  plazasAccesibles?: number;
}

export interface SalonPublico {
  nombre: string;
  capAuditorio?: number;
  capBanquete?: number;
  capCoctel?: number;
  m2?: number;
}

export interface VisitaGuiadaPublica {
  modalidad?: string;
  requiereReserva: boolean;
  horarios?: Horario[];
}

export interface CircuitoPublico {
  duracionMin?: number;
  lugarPartida?: string;
  partida?: Coordenadas;
  trazado?: unknown;
  paradas?: { orden: number; establecimientoId: number; nombre: string; coordenadas?: Coordenadas }[];
}

/** Datos públicos de la especialización de una oferta (puede venir vacío). */
export interface DetalleOferta {
  alojamiento?: AlojamientoPublico;
  circuito?: CircuitoPublico;
  salones?: SalonPublico[];
  visitasGuiadas?: VisitaGuiadaPublica[];
}

export interface OfertaFicha {
  id: number;
  categoria: string;
  categoriasAdicionales?: string[];
  acceso?: string;
  publicoAdmitido?: string;
  frecuencia?: string;
  horarios?: Horario[];
  detalle?: DetalleOferta;
}

export interface EstablecimientoFicha {
  id: number;
  nombre: string;
  descripcion?: string;
  oferente?: string;
  direccion?: string;
  lugar?: string;
  localidad?: string;
  provincia?: string;
  coordenadas?: Coordenadas;
  anioInauguracion?: number;
  estadoFuncionamiento?: string;
  motivoEstado?: string;
  fechaReapertura?: string;
  destacado: boolean;
  contactos?: Contacto[];
  caracteristicas?: Caracteristica[];
  distinciones?: Distincion[];
  ofertas?: OfertaFicha[];
}

export const DIAS_SEMANA = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
