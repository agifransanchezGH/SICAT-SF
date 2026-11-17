/** Tipos de la API Admin (espejo de los DTO de sicat-admin-api). */
import { Coordenadas, Pagina } from 'shared';

export type { Coordenadas, Pagina };

export type EstadoPublicacion = 'BORRADOR' | 'PUBLICADO' | 'OCULTO';
export type EstadoGeo = 'PENDIENTE' | 'AUTOMATICO' | 'APROXIMADO' | 'NO_ENCONTRADO' | 'VERIFICADO';

export const ESTADOS_PUBLICACION: EstadoPublicacion[] = ['BORRADOR', 'PUBLICADO', 'OCULTO'];

export const ESTADOS_GEO: { valor: EstadoGeo; texto: string }[] = [
  { valor: 'PENDIENTE', texto: 'Pendiente' },
  { valor: 'AUTOMATICO', texto: 'Automático' },
  { valor: 'APROXIMADO', texto: 'Aproximado' },
  { valor: 'NO_ENCONTRADO', texto: 'No encontrado' },
  { valor: 'VERIFICADO', texto: 'Verificado' },
];

export const DIAS = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

export interface Ref {
  id: number;
  nombre: string;
}

export interface CatalogoItem {
  id: number;
  nombre: string;
  activo?: boolean;
  provincia?: string;
  organismoOtorgante?: string;
  grupo?: string;
  admiteCantidad?: boolean;
  /** Solo caracteristica: false = de uso interno, no sale en el sitio público. */
  esPublica?: boolean;
}

export interface CategoriaNodo {
  id: number;
  nombre: string;
  padreId: number | null;
  activo: boolean;
  hijos: CategoriaNodo[];
}

export interface EstablecimientoResumen {
  id: number;
  nombre: string;
  oferenteId: number;
  oferente: string;
  localidad: string | null;
  coordenadas: Coordenadas | null;
  estadoGeo: EstadoGeo | null;
  estadoPublicacion: EstadoPublicacion;
  destacado: boolean;
  activo: boolean;
  categorias: string[];
}

export interface Establecimiento {
  id: number;
  nombre: string;
  descripcion: string | null;
  notasInternas: string | null;
  anioInauguracion: number | null;
  estadoFuncionamiento: string | null;
  motivoEstado: string | null;
  fechaReapertura: string | null;
  estadoPublicacion: EstadoPublicacion;
  destacado: boolean;
  activo: boolean;
  oferente: Ref;
  lugar: { id: number; nombre: string; direccion: string | null; localidad: string | null; coordenadas: Coordenadas | null } | null;
  ofertas: { id: number; categoriaId: number; categoria: string; activo: boolean }[];
}

export interface EstadoPublicacionInfo {
  estado: EstadoPublicacion;
  puedePublicarse: boolean;
  faltantes: string[];
}

export interface CanalContacto {
  id: number | null;
  tipoCanalId: number;
  tipoCanal?: string;
  valor: string;
  uso: string | null;
  esPublico: boolean;
  orden: number | null;
}

export interface Referente {
  id: number | null;
  nombre: string;
  cargo: string | null;
  telefono: string | null;
  correo: string | null;
  activo: boolean;
}

export interface DistincionAsignada {
  distincionId: number;
  nombre?: string;
  organismoOtorgante?: string | null;
  anioOtorgamiento: number | null;
  urlVerificacion: string | null;
}

export interface CaracteristicaAsignada {
  caracteristicaId: number;
  grupo?: string;
  nombre?: string;
  cantidad: number | null;
}

export interface Horario {
  id: number | null;
  diaSemana: number;
  desde: string;
  hasta: string;
}

export interface Alojamiento {
  estrellas: number | null;
  cantHabitaciones: number | null;
  totalPlazas: number | null;
  habAccesibles: number | null;
  plazasAccesibles: number | null;
  personalPermanente: number | null;
  personalNoPermanente: number | null;
}

export interface Circuito {
  lugarPartidaId: number | null;
  lugarPartida?: string | null;
  duracionMin: number | null;
  trazado: unknown;
}

export interface Parada {
  orden: number;
  establecimientoId: number;
  establecimiento: string;
}

export interface Salon {
  id: number;
  ofertaId: number;
  nombre: string;
  capAuditorio: number | null;
  capBanquete: number | null;
  capCoctel: number | null;
  m2: number | null;
  estadoConservacion: string | null;
  estadoFuncionamiento: string | null;
  activo: boolean;
}

export interface VisitaGuiada {
  id: number;
  ofertaId: number;
  modalidad: string | null;
  requiereReserva: boolean;
  habilitada: boolean;
  horarios: Horario[];
}

export interface Oferta {
  id: number;
  establecimientoId: number;
  establecimiento: string;
  categoriaId: number;
  categoria: string;
  categoriasAdicionales?: Ref[];
  acceso: string | null;
  publicoAdmitido: string | null;
  frecuencia: string | null;
  fechaRelevamiento: string | null;
  activo: boolean;
  horarios?: Horario[];
  alojamiento?: Alojamiento;
  circuito?: Circuito;
  salones?: Salon[];
  visitasGuiadas?: VisitaGuiada[];
}

export interface Oferente {
  id: number;
  nombre: string;
  tipoOferenteId: number;
  tipoOferente: string;
  activo: boolean;
  establecimientos?: { id: number; nombre: string; estadoPublicacion: EstadoPublicacion; activo: boolean }[];
}

export interface Lugar {
  id: number;
  nombre: string;
  padreId?: number;
  padre?: string;
  tipoLugarId?: number;
  tipoLugar?: string;
  localidadId?: number;
  localidad?: string;
  calle?: string;
  altura?: number;
  km?: number;
  calleCruce?: string;
  complemento?: string;
  coordenadas?: Coordenadas;
  geometria?: unknown;
  estadoGeo?: EstadoGeo;
  hijos?: Ref[];
  /** Establecimientos ubicados en este lugar. */
  establecimientos?: { id: number; nombre: string; estadoPublicacion: EstadoPublicacion; activo: boolean }[];
}

export type Rol = 'ADMIN' | 'CARGADOR' | 'CONSULTA';

export interface Usuario {
  id: number;
  nombreCompleto: string;
  email: string;
  rol: Rol;
  activo: boolean;
  ultimoAcceso: string | null;
}

export interface Auditoria {
  id: number;
  tabla: string;
  clave: string;
  accion: 'ALTA' | 'MODIFICACION' | 'ELIMINACION';
  usuario: { id: number; nombreCompleto: string; email: string } | null;
  fechaHora: string;
  origen: 'APP' | 'SISTEMA' | 'MIGRACION';
  datosAnteriores: Record<string, unknown> | null;
  datosNuevos: Record<string, unknown> | null;
}

export interface GrupoDuplicados {
  clave: string;
  mismaDireccion: boolean;
  establecimientos: { id: number; nombre: string; direccion: string | null; estadoPublicacion: EstadoPublicacion }[];
}

export interface PerfilImportacion {
  archivo: string;
  archivoEsperado: string;
  hojas: unknown[];
}

export interface ResultadoImportacion {
  archivo: string;
  ejecutado: boolean;
  total: number;
  nuevos: number;
  duplicadosEnBase: number;
  duplicadosEnArchivo: number;
  conErrores: number;
  creados: number;
  hojasIgnoradas: string[];
  items: {
    hoja: string;
    fila: number;
    nombre: string;
    categoria: string;
    direccion?: string;
    estado: 'NUEVO' | 'DUPLICADO_EN_BASE' | 'DUPLICADO_EN_ARCHIVO' | 'ERROR';
    establecimientoExistenteId?: number;
    establecimientoCreadoId?: number;
    contactos: number;
    caracteristicas: number;
    errores?: string[];
    advertencias?: string[];
  }[];
}

export interface EstadoGeocodificacion {
  enCurso: boolean;
  total: number;
  procesados: number;
  encontrados: number;
  aproximados: number;
  noEncontrados: number;
  pendientes?: number;
  inicio?: string;
  fin?: string;
  error?: string;
}

/** Aplana el árbol de categorías para usarlo en un select ("Padre › Hija"). */
export function aplanarCategorias(nodos: CategoriaNodo[], prefijo = ''): { id: number; nombre: string; activo: boolean }[] {
  return nodos.flatMap((n) => {
    const nombre = prefijo ? `${prefijo} › ${n.nombre}` : n.nombre;
    return [{ id: n.id, nombre, activo: n.activo }, ...aplanarCategorias(n.hijos ?? [], nombre)];
  });
}
